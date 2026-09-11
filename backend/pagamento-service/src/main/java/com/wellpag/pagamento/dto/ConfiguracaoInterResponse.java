package com.wellpag.pagamento.dto;

import com.wellpag.pagamento.model.BancoConfiguracaoInter;

public record ConfiguracaoInterResponse(
    String clientId,
    String chavePix,
    boolean temCertificado,
    boolean temChavePrivada
) {
    public static ConfiguracaoInterResponse from(BancoConfiguracaoInter c) {
        return new ConfiguracaoInterResponse(
            c.getClientId(),
            c.getChavePix(),
            c.getCertificadoPem() != null && !c.getCertificadoPem().isBlank(),
            c.getChavePrivadaPem() != null && !c.getChavePrivadaPem().isBlank()
        );
    }

    public static ConfiguracaoInterResponse vazio() {
        return new ConfiguracaoInterResponse(null, null, false, false);
    }
}
