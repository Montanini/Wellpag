package com.wellpag.notificacao.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Modelo read-only temporario: subconjunto dos campos de Aluno realmente lidos
 * por WhatsAppService (nome/telefone para o envio, professorId para o lote do
 * scheduler, valorMensalidade/diaVencimento para o template da mensagem e
 * calculo de vencimento). Aponta para a mesma collection "alunos" do monolito.
 * Ponte transitoria ate o futuro aluno-service existir de verdade e isso virar
 * uma chamada REST — este servico nunca escreve nesta collection.
 *
 * Mantida como leitura direta no banco (nao REST) mesmo apos aluno-service ter
 * sido extraido: WhatsAppService.enviarLembretes() e chamado tambem pelo
 * LembreteScheduler (job de fundo, sem JWT de professor disponivel), que nao
 * tem como se autenticar contra aluno-service sem um mecanismo de autenticacao
 * servico-a-servico — fora do escopo desta wave.
 */
@Data
@Document(collection = "alunos")
public class Aluno {

    @Id
    private String id;

    private String nome;
    private String telefone;

    /** ID do professor responsavel por este aluno. */
    private String professorId;

    /** Valor padrao da mensalidade definido pelo professor. */
    private Double valorMensalidade;

    /** Dia do mes em que a mensalidade vence (1-28). */
    private Integer diaVencimento;
}
