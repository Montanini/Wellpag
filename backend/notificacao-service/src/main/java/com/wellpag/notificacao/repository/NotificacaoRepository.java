package com.wellpag.notificacao.repository;

import com.wellpag.notificacao.model.NotificacaoPagamento;
import com.wellpag.notificacao.model.StatusNotificacao;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Dono real deste repositorio. Porta apenas os metodos usados por
 * NotificacaoController/NotificacaoService neste servico. NAO porta
 * existsByEndToEndId — usado apenas por WebhookService, que permanece no
 * monolito (escopo de wave futura).
 */
public interface NotificacaoRepository extends MongoRepository<NotificacaoPagamento, String> {
    List<NotificacaoPagamento> findByProfessorIdOrderByRecebidaEmDesc(String professorId);
    List<NotificacaoPagamento> findByProfessorIdAndStatusOrderByRecebidaEmDesc(String professorId, StatusNotificacao status);
    Optional<NotificacaoPagamento> findByIdAndProfessorId(String id, String professorId);
}
