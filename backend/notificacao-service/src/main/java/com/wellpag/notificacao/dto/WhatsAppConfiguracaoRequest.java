package com.wellpag.notificacao.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record WhatsAppConfiguracaoRequest(

    @Min(1) @Max(15)
    int diasAntesVencimento,

    boolean enviarAtrasados
) {}
