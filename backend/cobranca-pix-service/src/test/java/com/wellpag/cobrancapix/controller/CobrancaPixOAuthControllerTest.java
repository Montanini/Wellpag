package com.wellpag.cobrancapix.controller;

import com.wellpag.cobrancapix.client.MercadoPagoClient;
import com.wellpag.cobrancapix.client.MercadoPagoTokenResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Primeiro teste de integracao do projeto Wellpag (issue #35) — precedente
 * para os proximos: uma unica costura via MockMvc entrando pela camada
 * HTTP/controller, rodando contra um MongoDB real via Testcontainers (nao um
 * fake em memoria, nao um repositorio mockado) — cobre controller -> service
 * -> Mongo de verdade. A unica fronteira mockada e a chamada de terceiro:
 * {@link MercadoPagoClient} (via {@code @MockitoBean}, o substituto do
 * {@code @MockBean} descontinuado a partir do Spring Boot 3.4+).
 * <p>
 * Perfil {@code dev} e ativado deliberadamente (assim como o smoke test
 * {@code CobrancaPixServiceApplicationTests}) para poder assinar/validar JWTs
 * de teste com o mesmo segredo hardcoded conhecido de application-dev.yml
 * (wellpag.jwt.secret) e o mesmo oauth-state-secret conhecido, sem precisar
 * reinjetar essas properties aqui — so spring.data.mongodb.uri e sobrescrita
 * (para o container Testcontainers) via @DynamicPropertySource.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Testcontainers
class CobrancaPixOAuthControllerTest {

    // Mesmo valor hardcoded de application-dev.yml (wellpag.jwt.secret) — ver Javadoc da classe.
    private static final String JWT_SECRET_DEV =
        "dev-fake-secret-nao-usar-em-producao-min-256-bits-0000000000000000";

    // Mesmo valor hardcoded de application-dev.yml (wellpag.mercadopago.oauth-state-secret).
    private static final String OAUTH_STATE_SECRET_DEV = "dhyrRZFd+FnrLEoFtHB+VgmGDp4eCxRI";

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> mongoDBContainer.getReplicaSetUrl("wellpag_test"));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongoTemplate;

    @MockitoBean
    private MercadoPagoClient mercadoPagoClient;

    // ---- GET /professor/cobranca-pix/connect ----

    @Test
    void connect_semJwt_retorna401ou403() throws Exception {
        mockMvc.perform(get("/professor/cobranca-pix/connect"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assertThat(status).isIn(401, 403);
            });
    }

    @Test
    void connect_comJwtValido_retorna200ComStateDecodificavel() throws Exception {
        String professorId = "professor-123";
        String jwt = gerarJwtProfessor(professorId);

        mockMvc.perform(get("/professor/cobranca-pix/connect")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authorizationUrl").exists())
            .andExpect(result -> {
                String body = result.getResponse().getContentAsString();
                String url = body.replaceAll(".*\"authorizationUrl\":\"([^\"]+)\".*", "$1")
                    .replace("\\u0026", "&");
                String state = extrairQueryParam(url, "state");
                assertThat(state).isNotBlank();

                String professorIdDecodificado = validarStateEExtrairSubject(state);
                assertThat(professorIdDecodificado).isEqualTo(professorId);
            });
    }

    // ---- GET /cobranca-pix/oauth/callback ----

    @Test
    void callback_naoExigeAuthorizationHeader_eRedirecionaEmSucesso() throws Exception {
        String professorId = "professor-callback-sucesso";
        String state = gerarStateValido(professorId);

        when(mercadoPagoClient.trocarCodePorToken(anyString())).thenReturn(new MercadoPagoTokenResponse(
            "access-token-plaintext-xyz",
            "refresh-token-plaintext-abc",
            "bearer",
            21600L,
            "mp-user-999",
            "public-key-999"
        ));

        mockMvc.perform(get("/cobranca-pix/oauth/callback")
                .param("code", "auth-code-fake")
                .param("state", state))
            // sem header Authorization nenhum -> se essa rota exigisse JWT, isso daria 401/403
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "http://localhost:3000/dashboard?mercadopago=connected"));

        Document raw = mongoTemplate.getCollection("contas_mercadopago")
            .find(new Document("professorId", professorId))
            .first();
        assertThat(raw).isNotNull();
        assertThat(raw.getString("status")).isEqualTo("CONNECTED");
        assertThat(raw.getString("mpUserId")).isEqualTo("mp-user-999");

        // Prova de criptografia em repouso: le o documento CRU (bypassando o
        // AesGcmStringConverter, que so decifra quando se passa pelo
        // repositorio Spring Data) e confirma que o valor armazenado NAO e o
        // texto plano original — asserir so via o repositorio decifraria
        // silenciosamente e esconderia um bug real aqui.
        String accessTokenCru = raw.getString("accessToken");
        String refreshTokenCru = raw.getString("refreshToken");
        assertThat(accessTokenCru).isNotEqualTo("access-token-plaintext-xyz");
        assertThat(refreshTokenCru).isNotEqualTo("refresh-token-plaintext-abc");
        assertThat(accessTokenCru).doesNotContain("access-token-plaintext-xyz");
        assertThat(refreshTokenCru).doesNotContain("refresh-token-plaintext-abc");
    }

    @Test
    void callback_stateInvalido_naoPersisteERedirecionaParaErro() throws Exception {
        mockMvc.perform(get("/cobranca-pix/oauth/callback")
                .param("code", "auth-code-fake")
                .param("state", "isto-nao-e-um-jwt-valido"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "http://localhost:3000/dashboard?mercadopago=error"));

        long total = mongoTemplate.getCollection("contas_mercadopago").countDocuments();
        assertThat(total).isZero();
    }

    @Test
    void callback_stateExpirado_naoPersisteERedirecionaParaErro() throws Exception {
        String professorId = "professor-state-expirado";
        String stateExpirado = gerarStateExpirado(professorId);

        mockMvc.perform(get("/cobranca-pix/oauth/callback")
                .param("code", "auth-code-fake")
                .param("state", stateExpirado))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "http://localhost:3000/dashboard?mercadopago=error"));

        Document raw = mongoTemplate.getCollection("contas_mercadopago")
            .find(new Document("professorId", professorId))
            .first();
        assertThat(raw).isNull();
    }

    @Test
    void callback_codeAusente_naoPersisteERedirecionaParaErro() throws Exception {
        String professorId = "professor-sem-code";
        String state = gerarStateValido(professorId);

        mockMvc.perform(get("/cobranca-pix/oauth/callback")
                .param("state", state))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "http://localhost:3000/dashboard?mercadopago=error"));

        Document raw = mongoTemplate.getCollection("contas_mercadopago")
            .find(new Document("professorId", professorId))
            .first();
        assertThat(raw).isNull();
    }

    // ---- helpers ----

    private String gerarJwtProfessor(String professorId) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET_DEV.getBytes(StandardCharsets.UTF_8));
        Instant agora = Instant.now();
        return Jwts.builder()
            .subject(professorId)
            .claim("role", "PROFESSOR")
            .issuedAt(Date.from(agora))
            .expiration(Date.from(agora.plus(Duration.ofHours(1))))
            .signWith(key)
            .compact();
    }

    private String gerarStateValido(String professorId) {
        SecretKey key = Keys.hmacShaKeyFor(OAUTH_STATE_SECRET_DEV.getBytes(StandardCharsets.UTF_8));
        Instant agora = Instant.now();
        return Jwts.builder()
            .subject(professorId)
            .issuedAt(Date.from(agora))
            .expiration(Date.from(agora.plus(Duration.ofMinutes(10))))
            .signWith(key)
            .compact();
    }

    /** Constroi um state ja expirado diretamente (mesmo mecanismo de assinatura), sem esperar o TTL real. */
    private String gerarStateExpirado(String professorId) {
        SecretKey key = Keys.hmacShaKeyFor(OAUTH_STATE_SECRET_DEV.getBytes(StandardCharsets.UTF_8));
        Instant passado = Instant.now().minus(Duration.ofHours(1));
        return Jwts.builder()
            .subject(professorId)
            .issuedAt(Date.from(passado.minus(Duration.ofMinutes(10))))
            .expiration(Date.from(passado))
            .signWith(key)
            .compact();
    }

    private String validarStateEExtrairSubject(String state) {
        SecretKey key = Keys.hmacShaKeyFor(OAUTH_STATE_SECRET_DEV.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(state)
            .getPayload()
            .getSubject();
    }

    private String extrairQueryParam(String url, String param) {
        for (String parte : url.split("[?&]")) {
            if (parte.startsWith(param + "=")) {
                return parte.substring((param + "=").length());
            }
        }
        return null;
    }
}
