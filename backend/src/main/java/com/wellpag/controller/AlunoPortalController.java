package com.wellpag.controller;

import com.wellpag.dto.HorarioResponse;
import com.wellpag.dto.MensalidadeResponse;
import com.wellpag.dto.PortalPerfilResponse;
import com.wellpag.dto.PortalRelatorioResponse;
import com.wellpag.service.AlunoPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

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
    public List<HorarioResponse> horarios(@AuthenticationPrincipal String usuarioId) {
        return portalService.horarios(usuarioId);
    }

    @GetMapping("/mensalidades")
    @Operation(summary = "Histórico de mensalidades")
    public List<MensalidadeResponse> mensalidades(@AuthenticationPrincipal String usuarioId) {
        return portalService.mensalidades(usuarioId);
    }

    @GetMapping("/mensalidades/{mes}")
    @Operation(summary = "Mensalidade de um mês específico (formato: yyyy-MM)")
    public List<MensalidadeResponse> mensalidadesPorMes(@AuthenticationPrincipal String usuarioId,
                                                         @PathVariable String mes) {
        return portalService.mensalidadesPorMes(usuarioId, YearMonth.parse(mes));
    }

    @GetMapping("/relatorio")
    @Operation(summary = "Relatório financeiro consolidado do aluno")
    public PortalRelatorioResponse relatorio(@AuthenticationPrincipal String usuarioId) {
        return portalService.relatorio(usuarioId);
    }
}
