package com.wellpag.repository;

import com.wellpag.model.Mensalidade;
import com.wellpag.model.StatusMensalidade;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MensalidadeRepository extends MongoRepository<Mensalidade, String> {
    List<Mensalidade> findByProfessorId(String professorId);
    List<Mensalidade> findByAlunoId(String alunoId);
    List<Mensalidade> findByAlunoIdAndProfessorId(String alunoId, String professorId);
    Optional<Mensalidade> findByAlunoIdAndMesReferencia(String alunoId, String mesReferencia);
    List<Mensalidade> findByProfessorIdAndMesReferencia(String professorId, String mesReferencia);
    List<Mensalidade> findByProfessorIdAndStatus(String professorId, StatusMensalidade status);
}
