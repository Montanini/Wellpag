package com.wellpag.relatorio.repository;

import com.wellpag.relatorio.model.DiaSemana;
import com.wellpag.relatorio.model.Horario;
import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * Repositorio read-only temporario sobre a collection "horarios" (ver Horario.java).
 * Estende Repository (interface marcadora, sem CRUD) para nao herdar save/delete.
 * Expoe apenas o metodo de query realmente usado pelo Dashboard.
 */
public interface HorarioRepository extends Repository<Horario, String> {
    List<Horario> findByProfessorIdAndDiaSemana(String professorId, DiaSemana diaSemana);
}
