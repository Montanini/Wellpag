package com.wellpag.dto;

import com.wellpag.model.ConfiguracaoWhatsApp;

public record WhatsAppConfiguracaoResponse(
    boolean conectado,
    int diasAntesVencimento,
    boolean enviarAtrasados
) {
    public static WhatsAppConfiguracaoResponse from(ConfiguracaoWhatsApp c) {
        return new WhatsAppConfiguracaoResponse(
            c.isConectado(),
            c.getDiasAntesVencimento(),
            c.isEnviarAtrasados()
        );
    }
}
