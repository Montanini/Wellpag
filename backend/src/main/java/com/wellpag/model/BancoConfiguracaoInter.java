package com.wellpag.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "banco_configuracao_inter")
public class BancoConfiguracaoInter {

    @Id
    private String id;

    @Indexed(unique = true)
    private String professorId;

    /** OAuth2 credentials do portal Inter Empresas */
    private String clientId;
    private String clientSecret;

    /** Chave PIX cadastrada no Inter para associar o webhook */
    private String chavePix;

    /** Certificado mTLS em formato PEM (.crt) */
    private String certificadoPem;

    /** Chave privada mTLS em formato PEM (.key) */
    private String chavePrivadaPem;

    /** true quando o webhook foi registrado com sucesso na API do Inter */
    private boolean webhookRegistrado;

    /** URL efetivamente registrada no Inter */
    private String webhookUrl;

    @LastModifiedDate
    private LocalDateTime atualizadoEm;
}
