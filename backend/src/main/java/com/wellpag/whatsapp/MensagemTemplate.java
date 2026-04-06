package com.wellpag.whatsapp;

import com.wellpag.model.Aluno;
import com.wellpag.model.LembreteEnviado.TipoLembrete;

public class MensagemTemplate {

    public static String gerar(Aluno aluno, String mesReferencia, TipoLembrete tipo) {
        String nome       = aluno.getNome().split(" ")[0]; // primeiro nome
        String mes        = mesReferencia.replace("-", "/");
        String valor      = aluno.getValorMensalidade() != null
            ? String.format("R$ %.2f", aluno.getValorMensalidade()).replace(".", ",")
            : "—";
        int diaVenc       = aluno.getDiaVencimento() != null ? aluno.getDiaVencimento() : 0;

        return switch (tipo) {
            case PRE_VENCIMENTO -> """
                Olá, %s! 👋

                Passando para lembrar que sua mensalidade de *%s* no valor de *%s* vence no *dia %d*.

                Qualquer dúvida, estou à disposição. 😊
                """.formatted(nome, mes, valor, diaVenc).strip();

            case ATRASADO -> """
                Olá, %s! 👋

                Sua mensalidade de *%s* no valor de *%s* está em *atraso*.

                Por favor, entre em contato para regularizar. 🙏
                """.formatted(nome, mes, valor).strip();
        };
    }

    private MensagemTemplate() {}
}
