package com.wellpag.relatorio.dto;

public record EvolucaoMensalItem(
    String mes,
    double esperado,
    double recebido
) {}
