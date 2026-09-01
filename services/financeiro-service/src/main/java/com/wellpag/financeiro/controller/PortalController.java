package com.wellpag.financeiro.controller;

import com.wellpag.financeiro.dto.MensalidadeResponse;
import com.wellpag.financeiro.service.MensalidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

/**
 * Endpoints próprios do aluno para ver/gerar suas mensalidades — chamados por
 * aluno-service (AlunoPortalService), que reenvia o header Authorization
 * original do aluno sem gerar token novo. Substitui a leitura + lazy-creation
 * que antes era feita diretamente pelo monolito em AlunoPortalService.
 */
@RestController
@RequestMapping("/portal")
@RequiredArgsConstructor
@Tag(name = "Portal do Aluno")
public class PortalController {

    private final MensalidadeService mensalidadeService;

    @GetMapping("/mensalidades")
    @Operation(summary = "Histórico de mensalidades do aluno autenticado (cria a do mês atual se não existir)")
    public List<MensalidadeResponse> mensalidades(@AuthenticationPrincipal String usuarioId) {
        return mensalidadeService.mensalidadesPortal(usuarioId);
    }

    @GetMapping("/mensalidades/{mes}")
    @Operation(summary = "Mensalidade de um mês específico do aluno autenticado (formato: yyyy-MM)")
    public List<MensalidadeResponse> mensalidadesPorMes(@AuthenticationPrincipal String usuarioId,
                                                          @PathVariable String mes) {
        return mensalidadeService.mensalidadesPortalPorMes(usuarioId, YearMonth.parse(mes));
    }
}
