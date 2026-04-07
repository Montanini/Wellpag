package com.wellpag.repository;

import com.wellpag.model.BancoConfiguracaoInter;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface BancoConfiguracaoInterRepository extends MongoRepository<BancoConfiguracaoInter, String> {
    Optional<BancoConfiguracaoInter> findByProfessorId(String professorId);
}
