package com.wellpag.notificacao.model;

public enum BancoIntegracao {
    PIX_GENERICO,   // Formato PIX padrão Bacen (maioria dos bancos)
    ASAAS,          // Asaas / PJBank
    INTER,          // Banco Inter
    SICOOB,         // Sicoob
    EFIPAY,         // Gerencianet / Efi Bank
    GENERICO        // Fallback quando o formato não é reconhecido
}
