package com.wellpag.notificacao.repository;

import com.wellpag.notificacao.model.ConfiguracaoWhatsApp;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ConfiguracaoWhatsAppRepository extends MongoRepository<ConfiguracaoWhatsApp, String> {
    Optional<ConfiguracaoWhatsApp> findByProfessorId(String professorId);
    List<ConfiguracaoWhatsApp> findByConectadoTrue();
}
