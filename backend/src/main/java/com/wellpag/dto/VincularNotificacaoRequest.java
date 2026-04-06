package com.wellpag.dto;

import jakarta.validation.constraints.NotBlank;

public record VincularNotificacaoRequest(
    @NotBlank(message = "ID do aluno é obrigatório")
    String alunoId,

    @NotBlank(message = "ID da mensalidade é obrigatório")
    String mensalidadeId
) {}
