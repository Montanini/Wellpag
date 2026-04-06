package com.wellpag.controller;

import com.wellpag.dto.EvolucaoMensalItem;
import com.wellpag.dto.InadimplenteItem;
import com.wellpag.dto.RelatorioResumoResponse;
import com.wellpag.service.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/professor/relatorios")
@RequiredArgsConstructor
@Tag(name = "Relatórios")
public class RelatorioController {

    private final RelatorioService relatorioService;

    @GetMapping("/resumo")
    @Operation(summary = "Resumo financeiro do mês (padrão: mês atual)")
    public RelatorioResumoResponse resumo(
            @AuthenticationPrincipal String professorId,
            @RequestParam(required = false) String mes) {

        YearMonth yearMonth = mes != null ? YearMonth.parse(mes) : YearMonth.now();
        return relatorioService.resumo(professorId, yearMonth);
    }

    @GetMapping("/evolucao")
    @Operation(summary = "Evolução mensal da receita (padrão: últimos 6 meses)")
    public List<EvolucaoMensalItem> evolucao(
            @AuthenticationPrincipal String professorId,
            @RequestParam(defaultValue = "6") int meses) {

        return relatorioService.evolucao(professorId, Math.min(meses, 12));
    }

    @GetMapping("/inadimplentes")
    @Operation(summary = "Alunos com mensalidade atrasada no mês (padrão: mês atual)")
    public List<InadimplenteItem> inadimplentes(
            @AuthenticationPrincipal String professorId,
            @RequestParam(required = false) String mes) {

        YearMonth yearMonth = mes != null ? YearMonth.parse(mes) : YearMonth.now();
        return relatorioService.inadimplentes(professorId, yearMonth);
    }
}
