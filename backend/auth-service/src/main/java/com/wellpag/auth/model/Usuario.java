package com.wellpag.auth.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

/**
 * auth-service e o dono real desta entidade (collection "usuarios") — diferente
 * de Aluno.java neste mesmo modulo, que e apenas uma ponte de leitura/escrita
 * pontual para o futuro aluno-service.
 */
@Data
@Document(collection = "usuarios")
public class Usuario {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String nome;
    private Role role;

    /** Senha com hash BCrypt. Nulo para usuários OAuth2. */
    private String senha;

    /** Origem do cadastro: LOCAL ou GOOGLE. */
    private AuthProvider provider;

    /** Token único usado na URL do webhook bancário. Gerado automaticamente no cadastro. */
    private String webhookToken;
}
