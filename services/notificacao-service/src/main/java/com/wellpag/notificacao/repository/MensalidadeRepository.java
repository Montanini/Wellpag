package com.wellpag.notificacao.repository;

import com.wellpag.notificacao.model.Mensalidade;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/**
 * Repositorio read-only temporario sobre a collection "mensalidades" (ver
 * Mensalidade.java). Estende Repository (interface marcadora, sem CRUD) em vez
 * de MongoRepository para garantir em nivel de tipo que nenhum metodo de
 * escrita (save/delete) fica disponivel por acidente — financeiro-service e o
 * dono real desta entidade. Expoe apenas o metodo de query usado por
 * WhatsAppService para decidir se um lembrete e devido.
 */
public interface MensalidadeRepository extends Repository<Mensalidade, String> {
    Optional<Mensalidade> findByAlunoIdAndMesReferencia(String alunoId, String mesReferencia);
}
