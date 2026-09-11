package com.wellpag.aluno.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Modelo read-only temporario: apenas o campo "id", unico realmente necessario
 * para validar (via existsById) que o professor informado no auto-cadastro
 * publico existe. Aponta para a mesma collection "usuarios" do monolito.
 * Ponte transitoria ate o futuro auth-service existir de verdade e isso virar
 * uma chamada REST — este servico nunca escreve nesta collection.
 */
@Data
@Document(collection = "usuarios")
public class Usuario {

    @Id
    private String id;
}
