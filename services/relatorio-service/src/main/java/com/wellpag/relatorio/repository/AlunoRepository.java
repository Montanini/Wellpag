package com.wellpag.relatorio.repository;

import com.wellpag.relatorio.model.Aluno;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Repositorio read-only temporario sobre a collection "alunos" (ver Aluno.java).
 * Estende Repository (interface marcadora, sem CRUD) em vez de MongoRepository
 * para garantir em nivel de tipo que nenhum metodo de escrita (save/delete) fica
 * disponivel por acidente. Expoe apenas os metodos de query realmente usados
 * por Dashboard/Relatorio.
 */
public interface AlunoRepository extends Repository<Aluno, String> {
    List<Aluno> findByProfessorId(String professorId);
    List<Aluno> findByIdIn(Collection<String> ids);
}
