package com.wellpag.aluno.client;

/**
 * Subconjunto do response body devolvido por financeiro-service em
 * GET /portal/mensalidades e GET /portal/mensalidades/{mes}. status vem como
 * String em vez do enum StatusMensalidade e dataPagamento como String em vez
 * de LocalDate (mesmo padrão de notificacao-service/client/MensalidadeResponse)
 * — AlunoPortalService.relatorio() compara status por nome (PAGO/A_PAGAR/
 * ATRASADO) sem precisar do enum real, e o JSON de saída para o frontend fica
 * idêntico ao que o monolito devolvia.
 */
public record MensalidadeResponse(
    String id,
    String alunoId,
    String mesReferencia,
    Double valor,
    Integer diaVencimento,
    String status,
    String dataPagamento,
    String observacao
) {}
