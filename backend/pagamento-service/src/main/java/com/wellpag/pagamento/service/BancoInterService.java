package com.wellpag.pagamento.service;

import com.wellpag.pagamento.dto.ConfiguracaoInterResponse;
import com.wellpag.pagamento.model.BancoConfiguracaoInter;
import com.wellpag.pagamento.repository.BancoConfiguracaoInterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BancoInterService {

    private final BancoConfiguracaoInterRepository repository;

    // ───── Consulta ─────

    public ConfiguracaoInterResponse buscarCredenciais(String professorId) {
        return repository.findByProfessorId(professorId)
            .map(ConfiguracaoInterResponse::from)
            .orElse(ConfiguracaoInterResponse.vazio());
    }

    // ───── Salvar credenciais e certificados ─────

    public ConfiguracaoInterResponse salvarCredenciais(
        String professorId,
        String clientId,
        String clientSecret,
        String chavePix,
        String certificadoPem,
        String chavePrivadaPem
    ) {
        BancoConfiguracaoInter config = repository.findByProfessorId(professorId)
            .orElseGet(() -> {
                BancoConfiguracaoInter novo = new BancoConfiguracaoInter();
                novo.setProfessorId(professorId);
                return novo;
            });

        if (hasText(clientId))        config.setClientId(clientId);
        if (hasText(clientSecret))    config.setClientSecret(clientSecret);
        if (hasText(chavePix))        config.setChavePix(chavePix);
        if (hasText(certificadoPem))  config.setCertificadoPem(certificadoPem);
        if (hasText(chavePrivadaPem)) config.setChavePrivadaPem(chavePrivadaPem);

        return ConfiguracaoInterResponse.from(repository.save(config));
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
