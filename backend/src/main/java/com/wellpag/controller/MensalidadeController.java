package com.wellpag.controller;

import com.wellpag.dto.AlterarStatusMensalidadeRequest;
import com.wellpag.dto.ConfirmarPagamentoRequest;
import com.wellpag.dto.MensalidadeResponse;
import com.wellpag.service.MensalidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/professor/mensalidades")
@RequiredArgsConstructor
@Tag(name = "Mensalidades")
public class MensalidadeController {

    private final MensalidadeService mensalidadeService;

    @GetMapping("/aluno/{alunoId}")
    @Operation(summary = "Listar todas as mensalidades de um aluno")
    public List<MensalidadeResponse> listarPorAluno(@PathVariable String alunoId,
                                                     @AuthenticationPrincipal String professorId) {
        return mensalidadeService.listarPorAluno(alunoId, professorId);
    }

    @GetMapping("/mes/{mes}")
    @Operation(summary = "Listar mensalidades de um mês (formato: yyyy-MM)")
    public List<MensalidadeResponse> listarPorMes(
            @PathVariable String mes,
            @AuthenticationPrincipal String professorId) {
        return mensalidadeService.listarPorProfessorEMes(professorId, YearMonth.parse(mes));
    }

    @GetMapping("/aluno/{alunoId}/mes/{mes}")
    @Operation(summary = "Buscar ou criar mensalidade de um aluno em um mês")
    public MensalidadeResponse buscarOuCriar(@PathVariable String alunoId,
                                              @PathVariable String mes,
                                              @AuthenticationPrincipal String professorId) {
        return mensalidadeService.buscarOuCriar(alunoId, professorId, YearMonth.parse(mes));
    }

    @PatchMapping("/{mensalidadeId}/status")
    @Operation(summary = "Alterar status de uma mensalidade manualmente")
    public MensalidadeResponse alterarStatus(@PathVariable String mensalidadeId,
                                              @AuthenticationPrincipal String professorId,
                                              @Valid @RequestBody AlterarStatusMensalidadeRequest request) {
        return mensalidadeService.alterarStatus(mensalidadeId, professorId, request);
    }

    @PatchMapping("/{mensalidadeId}/confirmar")
    @Operation(summary = "Confirmar pagamento de uma mensalidade")
    public MensalidadeResponse confirmarPagamento(@PathVariable String mensalidadeId,
                                                   @AuthenticationPrincipal String professorId,
                                                   @Valid @RequestBody ConfirmarPagamentoRequest request) {
        return mensalidadeService.confirmarPagamento(mensalidadeId, professorId, request);
    }
}
