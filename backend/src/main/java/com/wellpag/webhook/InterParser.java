package com.wellpag.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wellpag.model.BancoIntegracao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Banco Inter usa o formato PIX padrão Bacen para webhooks.
 *
 * Exemplo de payload:
 * {
 *   "pix": [{
 *     "endToEndId": "E00000000202401010000AA123456789",
 *     "txid": "fc9a31b4346f43c99d2e069a9d5d3a2e",
 *     "chave": "pix@inter.com",
 *     "valor": "110.00",
 *     "horario": "2021-01-18T10:00:00.000Z",
 *     "infoPagador": "Mensalidade",
 *     "pagador": { "cpf": "12345678909", "nome": "Fulano de Tal" }
 *   }]
 * }
 */
@Component
@RequiredArgsConstructor
public class InterParser implements BancoParser {

    private final ObjectMapper objectMapper;

    @Override
    public BancoIntegracao banco() {
        return BancoIntegracao.INTER;
    }

    @Override
    public PayloadExtraido extrair(String payloadJson) {
        try {
            JsonNode root = objectMapper.readTree(payloadJson);
            JsonNode pix  = root.path("pix");
            JsonNode item = pix.isArray() && pix.size() > 0 ? pix.get(0) : pix;

            Double valor       = parseValor(item.path("valor").asText(null));
            String endToEnd    = item.path("endToEndId").asText(null);
            String descricao   = item.path("infoPagador").asText(null);
            LocalDateTime data = parseData(item.path("horario").asText(null));

            JsonNode pagador = item.path("pagador");
            String nome = pagador.path("nome").asText(null);
            String cpf  = pagador.path("cpf").asText(pagador.path("cnpj").asText(null));

            return new PayloadExtraido(valor, nome, cpf, data, endToEnd, descricao);
        } catch (Exception e) {
            return new PayloadExtraido(null, null, null, null, null, null);
        }
    }

    private Double parseValor(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return null; }
    }

    private LocalDateTime parseData(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDateTime.parse(s.replace("Z", ""), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) { return null; }
    }
}
