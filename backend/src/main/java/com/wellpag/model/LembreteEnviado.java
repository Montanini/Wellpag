package com.wellpag.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

/**
 * Registro de lembrete enviado — evita reenvio para o mesmo aluno no mesmo dia.
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
