package com.wellpag.notificacao.client;

import java.time.LocalDate;

/**
 * Espelha o request body esperado por financeiro-service em
 * PATCH /professor/mensalidades/{id}/confirmar. Vive em client/ (nao em dto/)
 * porque nao e um contrato exposto pela API deste servico — e apenas o payload
 * de saida para a chamada REST feita por FinanceiroServiceClient.
 */
public record ConfirmarPagamentoRequest(
    LocalDate dataPagamento,
    String observacao
) {}
