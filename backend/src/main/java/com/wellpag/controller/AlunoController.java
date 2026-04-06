package com.wellpag.controller;

import com.wellpag.dto.AlunoAutoCadastroRequest;
import com.wellpag.dto.AlunoComplementoRequest;
import com.wellpag.dto.AlunoResponse;
import com.wellpag.service.AlunoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Alunos")
public class AlunoController {

    private final AlunoService alunoService;

    /** Endpoint público: o próprio aluno se cadastra informando o ID do professor. */
    @PostMapping("/alunos/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Auto-cadastro do aluno (público)")
    public AlunoResponse autoCadastrar(@Valid @RequestBody AlunoAutoCadastroRequest request) {
        return alunoService.autoCadastrar(request);
    }

    @GetMapping("/professor/alunos")
    @Operation(summary = "Listar alunos do professor autenticado")
    public List<AlunoResponse> listar(@AuthenticationPrincipal String professorId) {
        return alunoService.listarPorProfessor(professorId);
    }

    @GetMapping("/professor/alunos/{alunoId}")
    @Operation(summary = "Buscar aluno por ID")
    public AlunoResponse buscar(@PathVariable String alunoId,
                                @AuthenticationPrincipal String professorId) {
        return alunoService.buscar(alunoId, professorId);
    }

    @PatchMapping("/professor/alunos/{alunoId}/complemento")
    @Operation(summary = "Professor define mensalidade e vencimento do aluno")
    public AlunoResponse complementar(@PathVariable String alunoId,
                                      @AuthenticationPrincipal String professorId,
                                      @Valid @RequestBody AlunoComplementoRequest request) {
        return alunoService.complementar(alunoId, professorId, request);
    }

    @DeleteMapping("/professor/alunos/{alunoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover aluno")
    public void remover(@PathVariable String alunoId,
                        @AuthenticationPrincipal String professorId) {
        alunoService.remover(alunoId, professorId);
    }
}
