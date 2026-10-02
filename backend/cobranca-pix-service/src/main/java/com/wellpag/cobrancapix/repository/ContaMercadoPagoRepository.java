package com.wellpag.cobrancapix.repository;

import com.wellpag.cobrancapix.model.ContaMercadoPago;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * {@code ContaMercadoPago} e dado de propriedade deste servico (nao uma copia
 * read-only de outro dominio) — por isso um {@code MongoRepository} normal e
 * correto aqui, diferente do padrao de bridge read-only (interface
 * {@code Repository<T, ID>} pura, sem save/delete) usado quando o dado
 * pertence a outro servico (ver CLAUDE.md raiz, "Read-only cross-domain bridge").
 */
public interface ContaMercadoPagoRepository extends MongoRepository<ContaMercadoPago, String> {

    Optional<ContaMercadoPago> findByProfessorId(String professorId);
}
