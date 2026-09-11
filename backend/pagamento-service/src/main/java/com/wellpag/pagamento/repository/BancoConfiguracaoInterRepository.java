package com.wellpag.pagamento.repository;

import com.wellpag.pagamento.model.BancoConfiguracaoInter;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface BancoConfiguracaoInterRepository extends MongoRepository<BancoConfiguracaoInter, String> {
    Optional<BancoConfiguracaoInter> findByProfessorId(String professorId);
}
