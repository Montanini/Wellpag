package com.wellpag.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ConfirmarPagamentoRequest(

    @NotNull(message = "Data de pagamento é obrigatória")
    LocalDate dataPagamento,

    String observacao
) {}
