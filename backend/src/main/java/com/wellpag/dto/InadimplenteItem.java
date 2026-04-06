package com.wellpag.dto;

public record InadimplenteItem(
    String alunoId,
    String nome,
    String telefone,
    int mesesAtrasados,
    double totalAtrasado
) {}
