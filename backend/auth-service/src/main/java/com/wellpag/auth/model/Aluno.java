package com.wellpag.auth.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Modelo read/write pontual temporario: subconjunto dos campos de Aluno
 * realmente usados por AuthService/OAuth2SuccessHandler (id, email, usuarioId).
 * Aponta para a mesma collection "alunos" do monolito. Ponte transitoria ate o
 * futuro aluno-service existir de verdade e isso virar uma chamada REST.
 *
 * auth-service LE este model para decidir se um novo usuario e PROFESSOR ou
 * ALUNO (existe algum Aluno com esse e-mail?) e ESCREVE o campo usuarioId para
 * vincular o(s) registro(s) de Aluno encontrados a conta recem-criada — essa
 * escrita cruzada de dominio e feita via MongoTemplate (ver AuthService /
 * OAuth2SuccessHandler), nunca via AlunoRepository (que e read-only, ver
 * repository/AlunoRepository.java).
 */
@Data
@Document(collection = "alunos")
public class Aluno {

    @Id
    private String id;

    private String email;

    /** ID do Usuario vinculado a este Aluno. Nulo ate o self-registro completar o cadastro de conta. */
    private String usuarioId;
}
