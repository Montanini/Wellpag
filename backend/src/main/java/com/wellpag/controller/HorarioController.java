package com.wellpag.controller;

import com.wellpag.dto.HorarioRequest;
import com.wellpag.dto.HorarioResponse;
import com.wellpag.model.DiaSemana;
import com.wellpag.service.HorarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/professor/horarios")
@RequiredArgsConstructor
@Tag(name = "Horários")
public class HorarioController {

    private final HorarioService horarioService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adicionar horário para um aluno")
    public HorarioResponse criar(@AuthenticationPrincipal String professorId,
                                 @Valid @RequestBody HorarioRequest request) {
        return horarioService.criar(professorId, request);
    }

    @GetMapping
    @Operation(summary = "Listar todos os horários do professor")
    public List<HorarioResponse> listar(@AuthenticationPrincipal String professorId) {
        return horarioService.listarPorProfessor(professorId);
    }

    @GetMapping("/dia/{diaSemana}")
    @Operation(summary = "Listar horários por dia da semana")
    public List<HorarioResponse> listarPorDia(@AuthenticationPrincipal String professorId,
                                               @PathVariable DiaSemana diaSemana) {
        return horarioService.listarPorDia(professorId, diaSemana);
    }

    @DeleteMapping("/{horarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover horário")
    public void remover(@PathVariable String horarioId,
                        @AuthenticationPrincipal String professorId) {
        horarioService.remover(horarioId, professorId);
    }
}
