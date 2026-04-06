package com.wellpag.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wellpag.model.BancoIntegracao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Formato PIX padrão Bacen — usado por BB, Bradesco, Sicoob, Itaú, etc.
 *
 * Exemplo de payload:
 * {
 *   "pix": [{
 *     "endToEndId": "E...",
 *     "txid": "...",
 *     "valor": "150.00",
 *     "horario": "2024-01-15T10:30:00.000Z",
 *     "pagador": { "nome": "João Silva", "cpf": "12345678901" },
 *     "infoPagador": "Mensalidade janeiro"
 *   }]
 * }
 */
@Component
@RequiredArgsConstructor
public class PixGenericoParser implements BancoParser {

    private final ObjectMapper objectMapper;

    @Override
    public BancoIntegracao banco() {
        return BancoIntegracao.PIX_GENERICO;
    }

    @Override
    public PayloadExtraido extrair(String payloadJson) {
        try {
            JsonNode root = objectMapper.readTree(payloadJson);
            JsonNode pix = root.path("pix");

            JsonNode item = pix.isArray() && pix.size() > 0 ? pix.get(0) : pix;

            Double valor = parseValor(item.path("valor").asText(null));
            String endToEnd = item.path("endToEndId").asText(null);
            String descricao = item.path("infoPagador").asText(null);
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
