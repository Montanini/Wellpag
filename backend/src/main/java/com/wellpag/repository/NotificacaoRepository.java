package com.wellpag.repository;

import com.wellpag.model.NotificacaoPagamento;
import com.wellpag.model.StatusNotificacao;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface NotificacaoRepository extends MongoRepository<NotificacaoPagamento, String> {
    List<NotificacaoPagamento> findByProfessorIdOrderByRecebidaEmDesc(String professorId);
    List<NotificacaoPagamento> findByProfessorIdAndStatusOrderByRecebidaEmDesc(String professorId, StatusNotificacao status);
    Optional<NotificacaoPagamento> findByIdAndProfessorId(String id, String professorId);
    boolean existsByEndToEndId(String endToEndId);
}
