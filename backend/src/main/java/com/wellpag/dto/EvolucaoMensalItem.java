package com.wellpag.dto;

public record EvolucaoMensalItem(
    String mes,
    double esperado,
    double recebido
) {}
