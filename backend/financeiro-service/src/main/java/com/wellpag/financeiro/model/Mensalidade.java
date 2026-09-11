package com.wellpag.financeiro.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

/**
 * Dono real deste model neste microsservico (financeiro-service e a fonte
 * canonica do calculo de status de mensalidade). Aponta para a mesma collection
 * "mensalidades" que o monolito ainda usa durante a Wave 2 — ver decisao de
 * arquitetura no prompt de extracao: mesma URI/collection do monolito
 * (wellpag_dev) ate a etapa futura de corte de banco por servico.
 */
@Data
@Document(collection = "mensalidades")
public class Mensalidade {

    @Id
    private String id;

    private String alunoId;
    private String professorId;

    /** Formato: "2025-04" (ano-mês). */
    private String mesReferencia;

    private Double valor;

    /** Dia do mês em que esta mensalidade vence. */
    private Integer diaVencimento;

    private StatusMensalidade status;

    /** Preenchido quando o professor confirma o pagamento. */
    private LocalDate dataPagamento;

    private String observacao;
}
