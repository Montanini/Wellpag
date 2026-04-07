package com.wellpag.controller;

import com.wellpag.dto.ConfiguracaoInterResponse;
import com.wellpag.service.BancoInterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/professor/banco")
@RequiredArgsConstructor
@Tag(name = "Configuração Bancária")
public class BancoController {

    private final BancoInterService bancoInterService;

    @GetMapping("/inter")
    @Operation(summary = "Retorna as credenciais Inter salvas (sem expor segredos)")
    public ResponseEntity<ConfiguracaoInterResponse> buscar(
        @AuthenticationPrincipal String professorId
    ) {
        return ResponseEntity.ok(bancoInterService.buscarCredenciais(professorId));
    }

    @PostMapping(value = "/inter", consumes = "multipart/form-data")
    @Operation(summary = "Salva/atualiza credenciais Inter e faz upload dos certificados mTLS")
    public ResponseEntity<ConfiguracaoInterResponse> salvar(
        @AuthenticationPrincipal String professorId,
        @RequestParam(required = false) String clientId,
        @RequestParam(required = false) String clientSecret,
        @RequestParam(required = false) String chavePix,
        @RequestParam(name = "certificado",  required = false) MultipartFile certificado,
        @RequestParam(name = "chavePrivada", required = false) MultipartFile chavePrivada
    ) throws IOException {
        String certPem = toText(certificado);
        String keyPem  = toText(chavePrivada);

        return ResponseEntity.ok(
            bancoInterService.salvarCredenciais(professorId, clientId, clientSecret, chavePix, certPem, keyPem)
        );
    }

    @PostMapping("/inter/registrar-webhook")
    @Operation(summary = "Registra a URL de webhook na API do Banco Inter via mTLS")
    public ResponseEntity<ConfiguracaoInterResponse> registrarWebhook(
        @AuthenticationPrincipal String professorId
    ) {
        return ResponseEntity.ok(bancoInterService.registrarWebhook(professorId));
    }

    @DeleteMapping("/inter/webhook")
    @Operation(summary = "Remove o webhook registrado na API do Banco Inter")
    public ResponseEntity<Void> deletarWebhook(
        @AuthenticationPrincipal String professorId
    ) {
        bancoInterService.deletarWebhook(professorId);
        return ResponseEntity.noContent().build();
    }

    private static String toText(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }
}
