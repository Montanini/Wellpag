package com.wellpag.repository;

import com.wellpag.model.Aluno;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AlunoRepository extends MongoRepository<Aluno, String> {
    List<Aluno> findByProfessorId(String professorId);
    Optional<Aluno> findByIdAndProfessorId(String id, String professorId);
    boolean existsByEmailAndProfessorId(String email, String professorId);
    List<Aluno> findByEmail(String email);
    List<Aluno> findByUsuarioId(String usuarioId);
    Optional<Aluno> findByProfessorIdAndCpfPagador(String professorId, String cpfPagador);
}
