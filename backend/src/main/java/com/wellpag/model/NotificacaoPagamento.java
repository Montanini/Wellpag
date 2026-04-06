package com.wellpag.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "notificacoes_pagamento")
public class NotificacaoPagamento {

    @Id
    private String id;

    private String professorId;
    private BancoIntegracao banco;

    /** Payload bruto recebido do banco (JSON como string). */
    private String payloadBruto;

    /** Campos extraídos do payload para exibição. */
    private Double valor;
    private String nomePagador;
    private String documentoPagador;
    private LocalDateTime dataTransacao;
    private String endToEndId;       // Identificador único PIX
    private String descricao;

    private StatusNotificacao status;

    /** Preenchido quando o professor vincula à uma mensalidade. */
    private String mensalidadeId;
    private String alunoId;

    @CreatedDate
    private LocalDateTime recebidaEm;
}
