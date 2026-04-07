package com.wellpag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wellpag.dto.ConfiguracaoInterResponse;
import com.wellpag.model.BancoConfiguracaoInter;
import com.wellpag.repository.BancoConfiguracaoInterRepository;
import com.wellpag.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BancoInterService {

    private static final String INTER_BASE_URL = "https://cdpj.partners.bancointer.com.br";

    private final BancoConfiguracaoInterRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    @Value("${wellpag.webhook.base-url}")
    private String webhookBaseUrl;

    // ───── Consulta ─────

    public ConfiguracaoInterResponse buscarCredenciais(String professorId) {
        return repository.findByProfessorId(professorId)
            .map(ConfiguracaoInterResponse::from)
            .orElse(ConfiguracaoInterResponse.vazio());
    }

    // ───── Salvar credenciais e certificados ─────

    public ConfiguracaoInterResponse salvarCredenciais(
        String professorId,
        String clientId,
        String clientSecret,
        String chavePix,
        String certificadoPem,
        String chavePrivadaPem
    ) {
        BancoConfiguracaoInter config = repository.findByProfessorId(professorId)
            .orElseGet(() -> {
                BancoConfiguracaoInter novo = new BancoConfiguracaoInter();
                novo.setProfessorId(professorId);
                return novo;
            });

        if (hasText(clientId))        config.setClientId(clientId);
        if (hasText(clientSecret))    config.setClientSecret(clientSecret);
        if (hasText(chavePix))        config.setChavePix(chavePix);
        if (hasText(certificadoPem))  config.setCertificadoPem(certificadoPem);
        if (hasText(chavePrivadaPem)) config.setChavePrivadaPem(chavePrivadaPem);

        // Credenciais alteradas → webhook precisa ser re-registrado
        if (hasText(clientId) || hasText(clientSecret) || hasText(chavePix)) {
            config.setWebhookRegistrado(false);
            config.setWebhookUrl(null);
        }

        return ConfiguracaoInterResponse.from(repository.save(config));
    }

    // ───── Registrar webhook na API do Inter ─────

    public ConfiguracaoInterResponse registrarWebhook(String professorId) {
        BancoConfiguracaoInter config = repository.findByProfessorId(professorId)
            .orElseThrow(() -> new IllegalArgumentException("Credenciais Inter não configuradas"));

        validarConfigCompleta(config);

        String professorToken = usuarioRepository.findById(professorId)
            .orElseThrow(() -> new IllegalArgumentException("Professor não encontrado"))
            .getWebhookToken();

        String webhookUrl = webhookBaseUrl + "/webhook/" + professorToken + "/inter";

        try {
            HttpClient client = criarHttpClientMtls(config);
            String token = obterTokenOAuth(client, config);
            registrarWebhookNoInter(client, token, config.getChavePix(), webhookUrl);

            config.setWebhookRegistrado(true);
            config.setWebhookUrl(webhookUrl);
            return ConfiguracaoInterResponse.from(repository.save(config));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao registrar webhook no Inter para professor {}", professorId, e);
            throw new IllegalArgumentException("Erro ao registrar webhook no Inter: " + e.getMessage());
        }
    }

    // ───── Remover webhook da API do Inter ─────

    public void deletarWebhook(String professorId) {
        BancoConfiguracaoInter config = repository.findByProfessorId(professorId)
            .orElseThrow(() -> new IllegalArgumentException("Credenciais Inter não configuradas"));

        validarConfigCompleta(config);

        try {
            HttpClient client = criarHttpClientMtls(config);
            String token = obterTokenOAuth(client, config);
            deletarWebhookNoInter(client, token, config.getChavePix());

            config.setWebhookRegistrado(false);
            config.setWebhookUrl(null);
            repository.save(config);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao deletar webhook no Inter para professor {}", professorId, e);
            throw new IllegalArgumentException("Erro ao deletar webhook no Inter: " + e.getMessage());
        }
    }

    // ───── Chamadas à API do Inter ─────

    private String obterTokenOAuth(HttpClient client, BancoConfiguracaoInter config) throws Exception {
        String body = "client_id="     + URLEncoder.encode(config.getClientId(),     StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(config.getClientSecret(), StandardCharsets.UTF_8)
                    + "&grant_type=client_credentials"
                    + "&scope=webhook.read%20webhook.write";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(INTER_BASE_URL + "/oauth/v2/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(Duration.ofSeconds(30))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IllegalArgumentException(
                "Falha na autenticação Inter (HTTP " + response.statusCode() + "): " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        return json.path("access_token").asText();
    }

    private void registrarWebhookNoInter(HttpClient client, String token, String chavePix, String webhookUrl)
        throws Exception {

        String body = objectMapper.writeValueAsString(Map.of("webhookUrl", webhookUrl));
        String chaveEncoded = URLEncoder.encode(chavePix, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(INTER_BASE_URL + "/pix/v2/webhook/" + chaveEncoded))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .timeout(Duration.ofSeconds(30))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalArgumentException(
                "Falha ao registrar webhook no Inter (HTTP " + response.statusCode() + "): " + response.body());
        }
    }

    private void deletarWebhookNoInter(HttpClient client, String token, String chavePix) throws Exception {
        String chaveEncoded = URLEncoder.encode(chavePix, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(INTER_BASE_URL + "/pix/v2/webhook/" + chaveEncoded))
            .header("Authorization", "Bearer " + token)
            .DELETE()
            .timeout(Duration.ofSeconds(30))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalArgumentException(
                "Falha ao deletar webhook no Inter (HTTP " + response.statusCode() + "): " + response.body());
        }
    }

    // ───── mTLS helpers ─────

    private HttpClient criarHttpClientMtls(BancoConfiguracaoInter config) throws Exception {
        X509Certificate cert = parseCertificado(config.getCertificadoPem());
        PrivateKey privateKey = parseChavePrivada(config.getChavePrivadaPem());

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("inter-client", privateKey, new char[0], new X509Certificate[]{cert});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, new char[0]);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);

        return HttpClient.newBuilder()
            .sslContext(sslContext)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    private X509Certificate parseCertificado(String pem) throws Exception {
        String base64 = pem
            .replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "")
            .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(base64);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(decoded));
    }

    private PrivateKey parseChavePrivada(String pem) throws Exception {
        String base64 = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(base64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private void validarConfigCompleta(BancoConfiguracaoInter config) {
        if (!hasText(config.getClientId()))
            throw new IllegalArgumentException("Client ID Inter não configurado");
        if (!hasText(config.getClientSecret()))
            throw new IllegalArgumentException("Client Secret Inter não configurado");
        if (!hasText(config.getChavePix()))
            throw new IllegalArgumentException("Chave PIX Inter não configurada");
        if (!hasText(config.getCertificadoPem()))
            throw new IllegalArgumentException("Certificado (.crt) não enviado");
        if (!hasText(config.getChavePrivadaPem()))
            throw new IllegalArgumentException("Chave privada (.key) não enviada");
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
