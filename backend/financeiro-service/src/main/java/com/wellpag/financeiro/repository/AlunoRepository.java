package com.wellpag.financeiro.repository;

import com.wellpag.financeiro.model.Aluno;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio read-only temporario sobre a collection "alunos" (ver Aluno.java).
 * Estende Repository (interface marcadora, sem CRUD) em vez de MongoRepository
 * para garantir em nivel de tipo que nenhum metodo de escrita (save/delete) fica
 * disponivel por acidente. Expoe apenas os metodos de query realmente usados por
 * MensalidadeService: busca por aluno+professor (criacao individual), findAll
 * (geracao em lote pelo scheduler, precisa de @Query("{}") pois nao e uma query
 * derivada por nome de propriedade) e findByUsuarioId (endpoints /portal/mensalidades).
 */
public interface AlunoRepository extends Repository<Aluno, String> {
    Optional<Aluno> findByIdAndProfessorId(String id, String professorId);
    List<Aluno> findByUsuarioId(String usuarioId);

    @Query("{}")
    List<Aluno> findAll();
}
