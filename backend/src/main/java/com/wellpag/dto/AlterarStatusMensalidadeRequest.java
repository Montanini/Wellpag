package com.wellpag.dto;

import com.wellpag.model.StatusMensalidade;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusMensalidadeRequest(
    @NotNull StatusMensalidade status
) {}
