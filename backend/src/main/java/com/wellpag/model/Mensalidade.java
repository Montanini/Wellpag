package com.wellpag.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

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
