package com.wellpag.relatorio.dto;

import com.wellpag.relatorio.model.StatusMensalidade;

import java.time.LocalTime;

/**
 * Representa um aluno no dashboard do professor,
 * com horario e status de mensalidade do mes atual.
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
