package com.wellpag.agenda.dto;

import com.wellpag.agenda.model.DiaSemana;
import com.wellpag.agenda.model.Horario;
import com.wellpag.agenda.model.TipoHorario;

import java.time.LocalTime;

public record HorarioResponse(
    String id,
    String alunoId,
    DiaSemana diaSemana,
    LocalTime horaInicio,
    LocalTime horaFim,
    TipoHorario tipo
) {
    public static HorarioResponse from(Horario horario) {
        return new HorarioResponse(
            horario.getId(),
            horario.getAlunoId(),
            horario.getDiaSemana(),
            horario.getHoraInicio(),
            horario.getHoraFim(),
            horario.getTipo()
        );
    }
}
