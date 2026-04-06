package com.wellpag.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "alunos")
public class Aluno {

    @Id
    private String id;

    private String nome;
    private String email;
    private String telefone;

    /** Preenchido quando o aluno é menor de idade. */
    private String nomeResponsavel;
    private String telefoneResponsavel;

    /** ID do professor responsável por este aluno. */
    private String professorId;

    /**
     * CPF do titular da conta que faz o PIX.
     * Usado para vincular automaticamente o pagamento à mensalidade do aluno.
     * Pode ser diferente do CPF do aluno (ex: pai paga pelo CPF dele).
     */
    private String cpfPagador;

    /** Valor padrão da mensalidade definido pelo professor. */
    private Double valorMensalidade;

    /** Dia do mês em que a mensalidade vence (1–28). */
    private Integer diaVencimento;

    /** ID da conta Usuario do aluno (preenchido quando ele cria o acesso ao portal). */
    private String usuarioId;

    @CreatedDate
    private LocalDateTime dataCadastro;
}
