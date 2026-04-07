package com.wellpag.dto;

import com.wellpag.model.BancoConfiguracaoInter;

public record ConfiguracaoInterResponse(
    String clientId,
    String chavePix,
    boolean temCertificado,
    boolean temChavePrivada,
    boolean webhookRegistrado,
    String webhookUrl
) {
    public static ConfiguracaoInterResponse from(BancoConfiguracaoInter c) {
        return new ConfiguracaoInterResponse(
            c.getClientId(),
            c.getChavePix(),
            c.getCertificadoPem() != null && !c.getCertificadoPem().isBlank(),
            c.getChavePrivadaPem() != null && !c.getChavePrivadaPem().isBlank(),
            c.isWebhookRegistrado(),
            c.getWebhookUrl()
        );
    }

    public static ConfiguracaoInterResponse vazio() {
        return new ConfiguracaoInterResponse(null, null, false, false, false, null);
    }
}
