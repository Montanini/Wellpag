package com.wellpag.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class EvolutionApiClient {

    private final RestClient restClient;
    private final ObjectMapper mapper;

    public EvolutionApiClient(
            @Value("${wellpag.whatsapp.evolution-api-url}") String baseUrl,
            @Value("${wellpag.whatsapp.evolution-api-key}") String apiKey,
            ObjectMapper mapper) {
        this.mapper = mapper;
        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("apikey", apiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    /** Cria a instância do professor na Evolution API. */
    public JsonNode criarInstancia(String instanceName) {
        ObjectNode body = mapper.createObjectNode();
        body.put("instanceName", instanceName);
        body.put("qrcode", true);
        body.put("integration", "WHATSAPP-BAILEYS");

        return restClient.post()
            .uri("/instance/create")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body.toString())
            .retrieve()
            .body(JsonNode.class);
    }

    /** Retorna o QR code da instância para scan. */
    public JsonNode obterQrCode(String instanceName) {
        return restClient.get()
            .uri("/instance/connect/{instance}", instanceName)
            .retrieve()
            .body(JsonNode.class);
    }

    /** Estado da conexão: open | close | connecting */
    public String estadoConexao(String instanceName) {
        try {
            JsonNode response = restClient.get()
                .uri("/instance/connectionState/{instance}", instanceName)
                .retrieve()
                .body(JsonNode.class);
            return response != null ? response.path("instance").path("state").asText("close") : "close";
        } catch (Exception e) {
            log.warn("Erro ao consultar estado da instância {}: {}", instanceName, e.getMessage());
            return "close";
        }
    }

    /** Envia mensagem de texto. */
    public boolean enviarTexto(String instanceName, String numero, String mensagem) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("number", formatarNumero(numero));
            body.putObject("options").put("delay", 1000);
            body.putObject("textMessage").put("text", mensagem);

            restClient.post()
                .uri("/message/sendText/{instance}", instanceName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body.toString())
                .retrieve()
                .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.error("Falha ao enviar WhatsApp para {}: {}", numero, e.getMessage());
            return false;
        }
    }

    /** Remove a instância. */
    public void deletarInstancia(String instanceName) {
        try {
            restClient.delete()
                .uri("/instance/delete/{instance}", instanceName)
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Erro ao deletar instância {}: {}", instanceName, e.getMessage());
        }
    }

    /** Garante formato internacional sem símbolos: 5511999999999 */
    private String formatarNumero(String telefone) {
        if (telefone == null) return "";
        String digits = telefone.replaceAll("\\D", "");
        if (!digits.startsWith("55")) digits = "55" + digits;
        return digits + "@s.whatsapp.net";
    }
}
