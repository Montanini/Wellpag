package com.wellpag.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Dados preenchidos pelo próprio aluno no auto-cadastro.
 * O professor complementa com horário e mensalidade depois.
 */
public record AlunoAutoCadastroRequest(

    @NotBlank(message = "Nome é obrigatório")
    String nome,

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    String email,

    String telefone,
    String nomeResponsavel,
    String telefoneResponsavel,

    /** CPF de quem faz o PIX (pode ser do responsável). Usado para vincular pagamentos automaticamente. */
    String cpfPagador,

    @NotBlank(message = "Código do professor é obrigatório")
    String professorId
) {}
