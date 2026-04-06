package com.wellpag.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Dados preenchidos pelo professor após o auto-cadastro do aluno:
 * valor da mensalidade e dia de vencimento.
 */
public record AlunoComplementoRequest(

    @NotNull(message = "Valor da mensalidade é obrigatório")
    @Positive(message = "Valor deve ser positivo")
    Double valorMensalidade,

    @NotNull(message = "Dia de vencimento é obrigatório")
    @Min(value = 1, message = "Dia deve ser entre 1 e 28")
    @Max(value = 28, message = "Dia deve ser entre 1 e 28")
    Integer diaVencimento
) {}
