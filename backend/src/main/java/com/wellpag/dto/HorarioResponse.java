package com.wellpag.dto;

import com.wellpag.model.DiaSemana;
import com.wellpag.model.Horario;
import com.wellpag.model.TipoHorario;

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
