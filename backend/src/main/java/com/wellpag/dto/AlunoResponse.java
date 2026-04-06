package com.wellpag.dto;

import com.wellpag.model.Aluno;

import java.time.LocalDateTime;

public record AlunoResponse(
    String id,
    String nome,
    String email,
    String telefone,
    String nomeResponsavel,
    String telefoneResponsavel,
    String cpfPagador,
    Double valorMensalidade,
    Integer diaVencimento,
    LocalDateTime dataCadastro
) {
    public static AlunoResponse from(Aluno aluno) {
        return new AlunoResponse(
            aluno.getId(),
            aluno.getNome(),
            aluno.getEmail(),
            aluno.getTelefone(),
            aluno.getNomeResponsavel(),
            aluno.getTelefoneResponsavel(),
            aluno.getCpfPagador(),
            aluno.getValorMensalidade(),
            aluno.getDiaVencimento(),
            aluno.getDataCadastro()
        );
    }
}
