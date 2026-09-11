package com.wellpag.auth.repository;

import com.wellpag.auth.model.Aluno;
import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * Repositorio read-only temporario sobre a collection "alunos" (ver Aluno.java).
 * Estende Repository (interface marcadora, sem CRUD) em vez de MongoRepository
 * para garantir em nivel de tipo que nenhum metodo de escrita (save/delete) fica
 * disponivel por acidente. A escrita pontual de usuarioId e feita via
 * MongoTemplate diretamente em AuthService/OAuth2SuccessHandler.
 */
public interface AlunoRepository extends Repository<Aluno, String> {
    List<Aluno> findByEmail(String email);
}
