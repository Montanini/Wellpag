package com.wellpag.dto;

public record WhatsAppConexaoResponse(
    String instanceName,
    /** open | connecting | close | desconectado */
    String estado,
    /** Base64 do QR code (presente apenas quando estado = "connecting") */
    String qrCodeBase64
) {}
