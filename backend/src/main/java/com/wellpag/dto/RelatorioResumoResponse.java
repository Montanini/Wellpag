package com.wellpag.dto;

public record RelatorioResumoResponse(
    String mesReferencia,
    int totalAlunos,
    double totalEsperado,
    double totalRecebido,
    double totalAPagar,
    double totalAtrasado,
    double percentualRecebido
) {}
