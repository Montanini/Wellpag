package com.wellpag.controller;

import com.wellpag.dto.WhatsAppConfiguracaoRequest;
import com.wellpag.dto.WhatsAppConfiguracaoResponse;
import com.wellpag.dto.WhatsAppConexaoResponse;
import com.wellpag.service.WhatsAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/professor/whatsapp")
@RequiredArgsConstructor
@Tag(name = "WhatsApp")
public class WhatsAppController {

    private final WhatsAppService whatsAppService;

    @PostMapping("/conectar")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inicia conexão — retorna QR code para escanear")
    public WhatsAppConexaoResponse conectar(@AuthenticationPrincipal String professorId) {
        return whatsAppService.conectar(professorId);
    }

    @GetMapping("/status")
    @Operation(summary = "Estado da conexão (open | connecting | close | desconectado)")
    public WhatsAppConexaoResponse status(@AuthenticationPrincipal String professorId) {
        return whatsAppService.status(professorId);
    }

    @DeleteMapping("/desconectar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a instância e desconecta o WhatsApp")
    public void desconectar(@AuthenticationPrincipal String professorId) {
        whatsAppService.desconectar(professorId);
    }

    @GetMapping("/configuracao")
    @Operation(summary = "Configurações de envio automático")
    public WhatsAppConfiguracaoResponse obterConfiguracao(@AuthenticationPrincipal String professorId) {
        return whatsAppService.obterConfiguracao(professorId);
    }

    @PutMapping("/configuracao")
    @Operation(summary = "Atualiza configurações de envio automático")
    public WhatsAppConfiguracaoResponse salvarConfiguracao(@AuthenticationPrincipal String professorId,
                                                            @Valid @RequestBody WhatsAppConfiguracaoRequest request) {
        return whatsAppService.salvarConfiguracao(professorId, request);
    }

    @PostMapping("/lembretes/enviar")
    @Operation(summary = "Dispara lembretes manualmente para todos os alunos elegíveis")
    public Map<String, Integer> enviarLembretes(@AuthenticationPrincipal String professorId) {
        int enviados = whatsAppService.enviarLembretes(professorId);
        return Map.of("enviados", enviados);
    }

    @PostMapping("/lembretes/aluno/{alunoId}")
    @Operation(summary = "Envia lembrete para um aluno específico")
    public Map<String, Boolean> enviarParaAluno(@PathVariable String alunoId,
                                                 @AuthenticationPrincipal String professorId) {
        boolean ok = whatsAppService.enviarParaAluno(professorId, alunoId);
        return Map.of("enviado", ok);
    }
}
