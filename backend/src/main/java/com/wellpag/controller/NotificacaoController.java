package com.wellpag.controller;

import com.wellpag.dto.NotificacaoResponse;
import com.wellpag.dto.VincularNotificacaoRequest;
import com.wellpag.dto.WebhookConfiguracaoResponse;
import com.wellpag.model.StatusNotificacao;
import com.wellpag.service.NotificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/professor/notificacoes")
@RequiredArgsConstructor
@Tag(name = "Notificações de Pagamento")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    @GetMapping("/webhook-configuracao")
    @Operation(summary = "URLs do webhook por banco para configurar no seu banco")
    public WebhookConfiguracaoResponse configuracao(@AuthenticationPrincipal String professorId) {
        return notificacaoService.configuracao(professorId);
    }

    @GetMapping
    @Operation(summary = "Listar notificações (filtre por status: PENDENTE, VINCULADA, IGNORADA)")
    public List<NotificacaoResponse> listar(
            @AuthenticationPrincipal String professorId,
            @RequestParam(required = false) StatusNotificacao status) {
        return notificacaoService.listar(professorId, status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de uma notificação (inclui payload bruto)")
    public NotificacaoResponse buscar(@PathVariable String id,
                                      @AuthenticationPrincipal String professorId) {
        return notificacaoService.buscar(id, professorId);
    }

    @PatchMapping("/{id}/vincular")
    @Operation(summary = "Vincular notificação a um aluno/mensalidade e confirmar pagamento")
    public NotificacaoResponse vincular(@PathVariable String id,
                                        @AuthenticationPrincipal String professorId,
                                        @Valid @RequestBody VincularNotificacaoRequest request) {
        return notificacaoService.vincular(id, professorId, request);
    }

    @PatchMapping("/{id}/ignorar")
    @Operation(summary = "Descartar notificação")
    public NotificacaoResponse ignorar(@PathVariable String id,
                                       @AuthenticationPrincipal String professorId) {
        return notificacaoService.ignorar(id, professorId);
    }
}
