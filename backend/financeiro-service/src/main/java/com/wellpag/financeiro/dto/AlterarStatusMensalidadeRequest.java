package com.wellpag.financeiro.dto;

import com.wellpag.financeiro.model.StatusMensalidade;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusMensalidadeRequest(
    @NotNull StatusMensalidade status
) {}
