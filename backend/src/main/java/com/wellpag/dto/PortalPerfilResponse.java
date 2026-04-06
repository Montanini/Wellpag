package com.wellpag.dto;

import com.wellpag.model.Aluno;

public record PortalPerfilResponse(
    String id,
    String nome,
    String email,
    String telefone,
    String nomeResponsavel,
    String telefoneResponsavel,
    Double valorMensalidade,
    Integer diaVencimento
) {
    public static PortalPerfilResponse from(Aluno aluno) {
        return new PortalPerfilResponse(
            aluno.getId(),
            aluno.getNome(),
            aluno.getEmail(),
            aluno.getTelefone(),
            aluno.getNomeResponsavel(),
            aluno.getTelefoneResponsavel(),
            aluno.getValorMensalidade(),
            aluno.getDiaVencimento()
        );
    }
}
