package com.wellpag.dto;

import com.wellpag.model.StatusMensalidade;

import java.time.LocalTime;

/**
 * Representa um aluno no dashboard do professor,
 * com horário e status de mensalidade do mês atual.
 */
public record DashboardAlunoItem(
    String alunoId,
    String nome,
    String telefone,
    LocalTime horaInicio,
    LocalTime horaFim,
    StatusMensalidade statusMensalidade,
    String mesReferencia
) {}
