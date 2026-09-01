package com.wellpag.notificacao.model;

/**
 * Espelha o enum equivalente em financeiro-service (dono real de Mensalidade).
 * Usado apenas como tipo de campo no bridge read-only Mensalidade.java.
 */
public enum StatusMensalidade {
    PAGO,
    A_PAGAR,
    ATRASADO
}
