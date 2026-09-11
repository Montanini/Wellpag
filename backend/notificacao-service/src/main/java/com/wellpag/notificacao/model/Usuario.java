package com.wellpag.notificacao.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Modelo read-only temporario: apenas os campos "id" e "webhookToken", os
 * unicos realmente necessarios para montar as URLs de webhook por banco
 * (ver NotificacaoService.configuracao()). Aponta para a mesma collection
 * "usuarios" do monolito. Ponte transitoria ate o futuro auth-service existir
 * de verdade e isso virar uma chamada REST — este servico nunca escreve nesta
 * collection.
 */
@Data
@Document(collection = "usuarios")
public class Usuario {

    @Id
    private String id;

    private String webhookToken;
}
