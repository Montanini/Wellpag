package com.wellpag.notificacao.dto;

import com.wellpag.notificacao.model.ConfiguracaoWhatsApp;

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
