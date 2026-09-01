package com.wellpag.relatorio.repository;

import com.wellpag.relatorio.model.Mensalidade;
import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * Repositorio read-only temporario sobre a collection "mensalidades" (ver Mensalidade.java).
 * Estende Repository (interface marcadora, sem CRUD) para nao herdar save/delete.
 * Expoe apenas o metodo de query realmente usado por Dashboard/Relatorio.
 */
public interface MensalidadeRepository extends Repository<Mensalidade, String> {
    List<Mensalidade> findByProfessorIdAndMesReferencia(String professorId, String mesReferencia);
}
