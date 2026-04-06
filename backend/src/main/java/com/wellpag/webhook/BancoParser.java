package com.wellpag.webhook;

import com.wellpag.model.BancoIntegracao;

public interface BancoParser {
    BancoIntegracao banco();
    PayloadExtraido extrair(String payloadJson);
}
