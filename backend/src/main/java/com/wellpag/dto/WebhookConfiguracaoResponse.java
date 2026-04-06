package com.wellpag.dto;

import java.util.Map;

public record WebhookConfiguracaoResponse(
    String token,
    Map<String, String> urls
) {}
