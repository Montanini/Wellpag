package com.wellpag.notificacao.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Dono real deste model neste microsservico (notificacao-service e a fonte
 * canonica das notificacoes de pagamento recebidas via webhook bancario).
 * Aponta para a mesma collection "notificacoes_pagamento" que o monolito ainda
 * usa durante esta wave — mesma URI/collection do monolito (wellpag_dev) ate a
 * etapa futura de corte de banco por servico.
 */
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
