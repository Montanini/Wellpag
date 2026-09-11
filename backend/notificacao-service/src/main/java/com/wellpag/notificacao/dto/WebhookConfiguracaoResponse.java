package com.wellpag.notificacao.dto;

import java.util.Map;

public record WebhookConfiguracaoResponse(
    String token,
    Map<String, String> urls
) {}
