package com.wellpag.relatorio.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Modelo read-only temporario: subconjunto dos campos de Mensalidade realmente lidos
 * (e recalculados em memoria, sem persistir) por Dashboard/Relatorio. Aponta para a
 * mesma collection "mensalidades" do monolito. Ponte transitoria ate o futuro
 * financeiro-service existir de verdade e isso virar uma chamada REST — este
 * servico nunca escreve nesta collection.
 */
@Data
@Document(collection = "mensalidades")
public class Mensalidade {

    @Id
    private String id;

    private String alunoId;
    private String professorId;

    /** Formato: "2025-04" (ano-mes). */
    private String mesReferencia;

    private Double valor;

    /** Dia do mes em que esta mensalidade vence. */
    private Integer diaVencimento;

    private StatusMensalidade status;
}
