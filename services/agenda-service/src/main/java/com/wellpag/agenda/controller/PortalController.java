package com.wellpag.agenda.controller;

import com.wellpag.agenda.dto.HorarioResponse;
import com.wellpag.agenda.service.HorarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint próprio do aluno para ver seus horários — chamado por aluno-service
 * (AlunoPortalService), que reenvia o header Authorization original do aluno
 * sem gerar token novo. Substitui a leitura que antes era feita diretamente
 * pelo monolito em AlunoPortalService.horarios().
 */
@RestController
@RequestMapping("/portal")
@RequiredArgsConstructor
@Tag(name = "Portal do Aluno")
public class PortalController {

    private final HorarioService horarioService;

    @GetMapping("/horarios")
    @Operation(summary = "Horários do aluno autenticado")
    public List<HorarioResponse> horarios(@AuthenticationPrincipal String usuarioId) {
        return horarioService.listarParaUsuario(usuarioId);
    }
}
