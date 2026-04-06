package com.wellpag.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wellpag.model.BancoIntegracao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Parser para webhooks do Asaas.
 *
 * Exemplo de payload:
 * {
 *   "event": "PAYMENT_RECEIVED",
 *   "payment": {
 *     "id": "pay_...",
 *     "value": 150.00,
 *     "paymentDate": "2024-01-15",
 *     "description": "Mensalidade",
 *     "billingType": "PIX"
 *   }
 * }
 */
@Component
@RequiredArgsConstructor
public class AsaasParser implements BancoParser {

    private final ObjectMapper objectMapper;

    @Override
    public BancoIntegracao banco() {
        return BancoIntegracao.ASAAS;
    }

    @Override
    public PayloadExtraido extrair(String payloadJson) {
        try {
            JsonNode root = objectMapper.readTree(payloadJson);
            JsonNode payment = root.path("payment");

            Double valor = payment.path("value").isNull() ? null : payment.path("value").asDouble();
            String descricao = payment.path("description").asText(null);
            String paymentId = payment.path("id").asText(null);

            LocalDateTime data = null;
            String dateStr = payment.path("paymentDate").asText(null);
            if (dateStr != null && !dateStr.isBlank()) {
                data = LocalDate.parse(dateStr).atTime(LocalTime.NOON);
            }

            return new PayloadExtraido(valor, null, null, data, paymentId, descricao);
        } catch (Exception e) {
            return new PayloadExtraido(null, null, null, null, null, null);
        }
    }
}
