package com.wellpag.agenda.repository;

import com.wellpag.agenda.model.Aluno;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/**
 * Repositorio read-only temporario sobre a collection "alunos" (ver Aluno.java).
 * Estende Repository (interface marcadora, sem CRUD) em vez de MongoRepository
 * para garantir em nivel de tipo que nenhum metodo de escrita (save/delete) fica
 * disponivel por acidente. Expoe apenas o metodo de query realmente usado por
 * HorarioService.criar().
 */
public interface AlunoRepository extends Repository<Aluno, String> {
    Optional<Aluno> findByIdAndProfessorId(String id, String professorId);
}
