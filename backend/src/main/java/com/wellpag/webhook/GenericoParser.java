package com.wellpag.webhook;

import com.wellpag.model.BancoIntegracao;
import org.springframework.stereotype.Component;

/**
 * Fallback: salva o payload bruto sem tentar extrair campos.
 * O professor verifica manualmente.
 */
@Component
public class GenericoParser implements BancoParser {

    @Override
    public BancoIntegracao banco() {
        return BancoIntegracao.GENERICO;
    }

    @Override
    public PayloadExtraido extrair(String payloadJson) {
        return new PayloadExtraido(null, null, null, null, null, "Revisar payload manualmente");
    }
}
