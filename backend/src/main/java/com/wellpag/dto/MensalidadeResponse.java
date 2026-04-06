package com.wellpag.dto;

import com.wellpag.model.Mensalidade;
import com.wellpag.model.StatusMensalidade;

import java.time.LocalDate;

public record MensalidadeResponse(
    String id,
    String alunoId,
    String mesReferencia,
    Double valor,
    Integer diaVencimento,
    StatusMensalidade status,
    LocalDate dataPagamento,
    String observacao
) {
    public static MensalidadeResponse from(Mensalidade m) {
        return new MensalidadeResponse(
            m.getId(),
            m.getAlunoId(),
            m.getMesReferencia(),
            m.getValor(),
            m.getDiaVencimento(),
            m.getStatus(),
            m.getDataPagamento(),
            m.getObservacao()
        );
    }
}
