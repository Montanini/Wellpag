package com.wellpag.notificacao.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Modelo read-only temporario: subconjunto dos campos de Mensalidade realmente
 * lidos por WhatsAppService (status para decidir se o lembrete e devido).
 * Aponta para a mesma collection "mensalidades" que financeiro-service ja
 * possui de verdade (extraido/mergeado, porta 8095). Ponte transitoria mantida
 * como leitura direta no banco (nao REST) pelo mesmo motivo do bridge de
 * Aluno: WhatsAppService.enviarLembretes() e chamado pelo LembreteScheduler
 * (job de fundo sem JWT de professor disponivel) — fora do escopo desta wave
 * inventar autenticacao servico-a-servico so para essa leitura em lote.
 * Este servico nunca escreve nesta collection (a escrita de confirmacao de
 * pagamento, quando necessaria, vai via chamada REST — ver
 * client/FinanceiroServiceClient.java).
 */
@Data
@Document(collection = "mensalidades")
public class Mensalidade {

    @Id
    private String id;

    private String alunoId;

    /** Formato: "2025-04" (ano-mês). */
    private String mesReferencia;

    private StatusMensalidade status;
}
