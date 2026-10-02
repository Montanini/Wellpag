package com.wellpag.cobrancapix.model;

/**
 * Estado da conexao OAuth de um professor com o Mercado Pago.
 * <p>
 * {@code PENDING} cobre conceitualmente "linha ainda nao existe" — nesta
 * issue (#35) o unico caminho de escrita e o callback OAuth bem-sucedido, que
 * grava direto como {@code CONNECTED}; nenhuma linha {@code PENDING} chega a
 * ser persistida ainda. Incluido aqui de qualquer forma por completude, para
 * ticket futuro (ex.: registrar a tentativa antes do redirect para o Mercado
 * Pago). Sem nenhuma regra de precedencia por ordinal aqui — essa regra (ver
 * spec da issue #33) se aplica a {@code CobrancaPix.status}, um modelo
 * diferente de um ticket futuro diferente, e nao se aplica a este enum.
 */
public enum StatusContaMercadoPago {
    PENDING,
    CONNECTED,
    REVOKED,
    EXPIRED
}
