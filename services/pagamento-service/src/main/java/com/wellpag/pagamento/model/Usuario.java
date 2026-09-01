package com.wellpag.pagamento.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Modelo read-only temporario: subconjunto dos campos de Usuario realmente lidos
 * por BancoInterService (webhookToken, usado para montar a URL do webhook PIX).
 * Aponta para a mesma collection "usuarios" do monolito/auth-service. Ponte
 * transitoria ate o dia em que essa leitura vire uma chamada REST ao
 * auth-service — este servico nunca escreve nesta collection.
 */
@Data
@Document(collection = "usuarios")
public class Usuario {

    @Id
    private String id;

    /** Token unico usado na URL do webhook bancario. */
    private String webhookToken;
}
