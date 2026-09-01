package com.wellpag.agenda.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Modelo read-only temporario: subconjunto dos campos de Aluno realmente lidos
 * por HorarioService (apenas para validar que o aluno pertence ao professor
 * autenticado antes de criar um horario) e pelo endpoint /portal/horarios
 * (usuarioId, para achar os alunos vinculados ao aluno autenticado). Aponta
 * para a mesma collection "alunos" do monolito. Ponte transitoria ate o futuro
 * aluno-service existir de verdade e isso virar uma chamada REST — este
 * servico nunca escreve nesta collection.
 */
@Data
@Document(collection = "alunos")
public class Aluno {

    @Id
    private String id;

    /** ID do professor responsavel por este aluno. */
    private String professorId;

    /** ID da conta Usuario do aluno (preenchido quando ele cria o acesso ao portal). */
    private String usuarioId;
}
