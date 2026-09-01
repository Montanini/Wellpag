package com.wellpag.financeiro.repository;

import com.wellpag.financeiro.model.Mensalidade;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Dono real deste repositorio (ver Mensalidade.java). Porta apenas os metodos
 * realmente usados por MensalidadeService/MensalidadeScheduler — save/findById
 * vem herdados de MongoRepository.
 */
public interface MensalidadeRepository extends MongoRepository<Mensalidade, String> {
    Optional<Mensalidade> findByAlunoIdAndMesReferencia(String alunoId, String mesReferencia);
    List<Mensalidade> findByAlunoIdAndProfessorId(String alunoId, String professorId);
    List<Mensalidade> findByProfessorIdAndMesReferencia(String professorId, String mesReferencia);
}
