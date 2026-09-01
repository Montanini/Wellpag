package com.wellpag.agenda.repository;

import com.wellpag.agenda.model.DiaSemana;
import com.wellpag.agenda.model.Horario;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface HorarioRepository extends MongoRepository<Horario, String> {
    List<Horario> findByProfessorId(String professorId);
    List<Horario> findByAlunoId(String alunoId);
    List<Horario> findByProfessorIdAndDiaSemana(String professorId, DiaSemana diaSemana);
}
