package com.wellpag.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "configuracoes_whatsapp")
public class ConfiguracaoWhatsApp {

    @Id
    private String id;

    @Indexed(unique = true)
    private String professorId;

    /** Nome da instância na Evolution API (wellpag-{professorId[:8]}). */
    private String instanceName;

    private boolean conectado;

    /** Número do professor no formato internacional: 5511999999999 */
    private String telefone;

    /** Quantos dias antes do vencimento enviar o lembrete (padrão: 3). */
    private int diasAntesVencimento = 3;

    /** Enviar lembretes para alunos com mensalidade atrasada (padrão: true). */
    private boolean enviarAtrasados = true;
}
