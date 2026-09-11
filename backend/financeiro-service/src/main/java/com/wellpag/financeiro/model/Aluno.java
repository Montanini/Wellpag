package com.wellpag.financeiro.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Modelo read-only temporario: subconjunto dos campos de Aluno realmente lidos
 * por MensalidadeService (valorMensalidade/diaVencimento para calcular a
 * mensalidade, professorId para o lote do scheduler, usuarioId para os
 * endpoints /portal/mensalidades). Aponta para a mesma collection "alunos" do
 * monolito. Ponte transitoria ate o futuro aluno-service existir de verdade e
 * isso virar uma chamada REST — este servico nunca escreve nesta collection.
 */
@Data
@Document(collection = "alunos")
public class Aluno {

    @Id
    private String id;

    /** ID do professor responsavel por este aluno. */
    private String professorId;

    /** Valor padrao da mensalidade definido pelo professor. */
    private Double valorMensalidade;

    /** Dia do mes em que a mensalidade vence (1-28). */
    private Integer diaVencimento;

    /** ID da conta Usuario do aluno (preenchido quando ele cria o acesso ao portal). */
    private String usuarioId;
}
