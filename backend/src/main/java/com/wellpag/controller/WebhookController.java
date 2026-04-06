package com.wellpag.controller;

import com.wellpag.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
@Tag(name = "Webhook Bancário")
public class WebhookController {

    private final WebhookService webhookService;

    /**
     * Endpoint público chamado pelo banco ao receber um pagamento.
     *
     * URL: POST /webhook/{professorToken}/{banco}
     *
     * Exemplos por banco:
     *   /webhook/{token}/pix_generico  → PIX padrão Bacen (BB, Bradesco, Itaú, Sicoob...)
     *   /webhook/{token}/asaas         → Asaas
     *   /webhook/{token}/efipay        → Gerencianet / Efi Bank
     *   /webhook/{token}/generico      → Qualquer outro (payload salvo bruto)
     */
    @PostMapping("/{professorToken}/{banco}")
    @Operation(summary = "Recebe notificação de pagamento do banco (endpoint público)")
    public ResponseEntity<Void> receber(
            @PathVariable String professorToken,
            @PathVariable String banco,
            @RequestBody String payload) {

        try {
            webhookService.processar(professorToken, banco, payload);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            // Token inválido → 404 para não revelar que o endpoint existe
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erro ao processar webhook banco={} erro={}", banco, e.getMessage(), e);
            // Retorna 200 mesmo em erro interno para o banco não reenviar em loop
            return ResponseEntity.ok().build();
        }
    }

    /**
     * Alguns bancos enviam um GET de validação antes de ativar o webhook.
     */
    @GetMapping("/{professorToken}/{banco}")
    @Operation(summary = "Validação do endpoint pelo banco (handshake)")
    public ResponseEntity<String> validar(
            @PathVariable String professorToken,
            @PathVariable String banco,
            @RequestParam(required = false) String challenge) {

        if (challenge != null) return ResponseEntity.ok(challenge);
        return ResponseEntity.ok("OK");
    }
}
