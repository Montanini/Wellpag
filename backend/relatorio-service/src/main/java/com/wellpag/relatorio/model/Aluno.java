package com.wellpag.relatorio.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Modelo read-only temporario: subconjunto dos campos de Aluno realmente lidos
 * por Dashboard/Relatorio. Aponta para a mesma collection "alunos" do monolito.
 * Ponte transitoria ate o futuro aluno-service existir de verdade e isso virar
 * uma chamada REST — este servico nunca escreve nesta collection.
 */
@Data
@Document(collection = "alunos")
public class Aluno {

    @Id
    private String id;

    private String nome;
    private String telefone;

    /** ID do professor responsavel por este aluno. */
    private String professorId;

    /** Dia do mes em que a mensalidade vence (1-28). Usado para calcular status default no dashboard. */
    private Integer diaVencimento;
}
