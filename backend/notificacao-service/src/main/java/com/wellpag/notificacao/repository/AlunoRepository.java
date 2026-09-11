package com.wellpag.notificacao.repository;

import com.wellpag.notificacao.model.Aluno;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio read-only temporario sobre a collection "alunos" (ver Aluno.java).
 * Estende Repository (interface marcadora, sem CRUD) em vez de MongoRepository
 * para garantir em nivel de tipo que nenhum metodo de escrita (save/delete) fica
 * disponivel por acidente — aluno-service e o dono real desta entidade. Expoe
 * apenas os metodos de query realmente usados por WhatsAppService.
 */
public interface AlunoRepository extends Repository<Aluno, String> {
    List<Aluno> findByProfessorId(String professorId);
    Optional<Aluno> findByIdAndProfessorId(String id, String professorId);
}
