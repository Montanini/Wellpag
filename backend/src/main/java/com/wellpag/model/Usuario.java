package com.wellpag.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

@Data
@Document(collection = "usuarios")
public class Usuario {

    @Id
    private String id;

    @Indexed(unique = true, sparse = true)
    private String username;

    @Indexed(unique = true, sparse = true)
    private String email;

    private String nome;
    private Role role;

    /** Senha com hash BCrypt. Nulo para usuários OAuth2. */
    private String senha;

    /** Origem do cadastro: LOCAL, GOOGLE ou GITHUB. */
    private AuthProvider provider;

    /** Token único usado na URL do webhook bancário. Gerado automaticamente no cadastro. */
    private String webhookToken;
}
