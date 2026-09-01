package com.wellpag.auth.repository;

import com.wellpag.auth.model.Usuario;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByWebhookToken(String webhookToken);
    boolean existsById(String id);
}
