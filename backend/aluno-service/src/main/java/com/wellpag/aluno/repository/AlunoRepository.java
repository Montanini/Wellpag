package com.wellpag.aluno.repository;

import com.wellpag.aluno.model.Aluno;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * aluno-service e o dono real de Aluno — extends MongoRepository normalmente,
 * escrita completa (save/delete) e esperada. So os metodos realmente usados
 * por AlunoService/AlunoPortalService foram portados.
 */
public interface AlunoRepository extends MongoRepository<Aluno, String> {
    List<Aluno> findByProfessorId(String professorId);
    Optional<Aluno> findByIdAndProfessorId(String id, String professorId);
    boolean existsByEmailAndProfessorId(String email, String professorId);
    List<Aluno> findByUsuarioId(String usuarioId);
}
