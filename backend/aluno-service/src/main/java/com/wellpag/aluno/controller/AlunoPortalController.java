package com.wellpag.aluno.controller;

import com.wellpag.aluno.client.HorarioResponse;
import com.wellpag.aluno.client.MensalidadeResponse;
import com.wellpag.aluno.dto.PortalPerfilResponse;
import com.wellpag.aluno.dto.PortalRelatorioResponse;
import com.wellpag.aluno.service.AlunoPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Porte fiel de AlunoPortalController do monolito (mesmas 5 rotas). perfil()
 * usa apenas o usuarioId autenticado (dado 100% local). As demais rotas
 * precisam do header Authorization bruto para repassar a agenda-service /
 * financeiro-service via RestClient — ver AlunoPortalService.
 */
@RestController
@RequestMapping("/aluno/portal")
@RequiredArgsConstructor
@Tag(name = "Portal do Aluno")
public class AlunoPortalController {

    private final AlunoPortalService portalService;

    @GetMapping("/perfil")
    @Operation(summary = "Dados do aluno autenticado (pode ter mais de um vínculo)")
    public List<PortalPerfilResponse> perfil(@AuthenticationPrincipal String usuarioId) {
        return portalService.perfis(usuarioId);
    }

    @GetMapping("/horarios")
    @Operation(summary = "Horários do aluno autenticado")
    public List<HorarioResponse> horarios(@RequestHeader("Authorization") String authorization) {
        return portalService.horarios(authorization);
    }

    @GetMapping("/mensalidades")
    @Operation(summary = "Histórico de mensalidades")
    public List<MensalidadeResponse> mensalidades(@RequestHeader("Authorization") String authorization) {
        return portalService.mensalidades(authorization);
    }

    @GetMapping("/mensalidades/{mes}")
    @Operation(summary = "Mensalidade de um mês específico (formato: yyyy-MM)")
    public List<MensalidadeResponse> mensalidadesPorMes(@PathVariable String mes,
                                                          @RequestHeader("Authorization") String authorization) {
        return portalService.mensalidadesPorMes(mes, authorization);
    }

    @GetMapping("/relatorio")
    @Operation(summary = "Relatório financeiro consolidado do aluno")
    public PortalRelatorioResponse relatorio(@RequestHeader("Authorization") String authorization) {
        return portalService.relatorio(authorization);
    }
}
