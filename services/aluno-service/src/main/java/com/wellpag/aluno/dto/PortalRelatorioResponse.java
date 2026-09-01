package com.wellpag.aluno.dto;

public record PortalRelatorioResponse(
    int totalMeses,
    double totalPago,
    double totalAPagar,
    double totalAtrasado,
    double totalGeral
) {}
