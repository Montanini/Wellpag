package com.wellpag.notificacao.model;

public enum StatusNotificacao {
    PENDENTE,   // Aguarda revisão do professor
    VINCULADA,  // Professor vinculou a uma mensalidade
    IGNORADA    // Professor descartou
}
