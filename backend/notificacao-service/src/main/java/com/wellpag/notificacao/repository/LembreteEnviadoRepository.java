package com.wellpag.notificacao.repository;

import com.wellpag.notificacao.model.LembreteEnviado;
import com.wellpag.notificacao.model.LembreteEnviado.TipoLembrete;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;

public interface LembreteEnviadoRepository extends MongoRepository<LembreteEnviado, String> {
    boolean existsByAlunoIdAndMesReferenciaAndTipoAndEnviadoEm(
        String alunoId, String mesReferencia, TipoLembrete tipo, LocalDate enviadoEm
    );
}
