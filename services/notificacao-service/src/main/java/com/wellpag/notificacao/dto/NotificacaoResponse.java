package com.wellpag.notificacao.dto;

import com.wellpag.notificacao.model.BancoIntegracao;
import com.wellpag.notificacao.model.NotificacaoPagamento;
import com.wellpag.notificacao.model.StatusNotificacao;

import java.time.LocalDateTime;

public record NotificacaoResponse(
    String id,
    BancoIntegracao banco,
    Double valor,
    String nomePagador,
    String documentoPagador,
    LocalDateTime dataTransacao,
    String endToEndId,
    String descricao,
    StatusNotificacao status,
    String mensalidadeId,
    String alunoId,
    LocalDateTime recebidaEm
) {
    public static NotificacaoResponse from(NotificacaoPagamento n) {
        return new NotificacaoResponse(
            n.getId(), n.getBanco(), n.getValor(),
            n.getNomePagador(), n.getDocumentoPagador(),
            n.getDataTransacao(), n.getEndToEndId(), n.getDescricao(),
            n.getStatus(), n.getMensalidadeId(), n.getAlunoId(),
            n.getRecebidaEm()
        );
    }
}
