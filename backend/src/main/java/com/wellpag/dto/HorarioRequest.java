package com.wellpag.dto;

import com.wellpag.model.DiaSemana;
import com.wellpag.model.TipoHorario;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record HorarioRequest(

    @NotNull(message = "Aluno é obrigatório")
    String alunoId,

    @NotNull(message = "Dia da semana é obrigatório")
    DiaSemana diaSemana,

    @NotNull(message = "Hora de início é obrigatória")
    LocalTime horaInicio,

    @NotNull(message = "Hora de fim é obrigatória")
    LocalTime horaFim,

    @NotNull(message = "Tipo é obrigatório")
    TipoHorario tipo
) {}
