package com.wellpag.notificacao.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

/**
 * Registro de lembrete enviado — evita reenvio para o mesmo aluno no mesmo dia.
 * Dono real deste model neste microsservico. Aponta para a mesma collection
 * "lembretes_enviados" que o monolito ainda usa durante esta wave.
 */
@Data
@Document(collection = "lembretes_enviados")
public class LembreteEnviado {

    @Id
    private String id;

    private String professorId;
    private String alunoId;
    private String mesReferencia;
    private TipoLembrete tipo;
    private LocalDate enviadoEm;

    public enum TipoLembrete {
        PRE_VENCIMENTO,
        ATRASADO
    }
}
