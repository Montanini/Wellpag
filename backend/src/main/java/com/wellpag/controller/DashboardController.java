package com.wellpag.controller;

import com.wellpag.dto.DashboardAlunoItem;
import com.wellpag.model.DiaSemana;
import com.wellpag.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/professor/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Retorna alunos com status de mensalidade.
     * Se diaSemana não for informado, usa o dia atual.
     */
    @GetMapping
    @Operation(summary = "Dashboard: alunos do dia com status de mensalidade")
    public List<DashboardAlunoItem> dashboard(
            @AuthenticationPrincipal String professorId,
            @RequestParam(required = false) DiaSemana diaSemana) {

        DiaSemana dia = diaSemana != null ? diaSemana : diaAtual();
        return dashboardService.listar(professorId, dia);
    }

    private DiaSemana diaAtual() {
        return switch (LocalDate.now().getDayOfWeek()) {
            case MONDAY -> DiaSemana.SEGUNDA;
            case TUESDAY -> DiaSemana.TERCA;
            case WEDNESDAY -> DiaSemana.QUARTA;
            case THURSDAY -> DiaSemana.QUINTA;
            case FRIDAY -> DiaSemana.SEXTA;
            case SATURDAY -> DiaSemana.SABADO;
            case SUNDAY -> DiaSemana.DOMINGO;
        };
    }
}
