package com.wellpag.cobrancapix.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint minimo, sem valor de negocio, existente apenas para verificar
 * de ponta a ponta (issue #34) que SecurityConfig/JwtAuthFilter estao
 * corretamente aplicando hasRole("PROFESSOR") em /professor/** — sem ele,
 * uma rota /professor/** sem controller nenhum mapeado devolveria 404 antes
 * mesmo do Spring Security entrar em acao, o que não provaria nada sobre a
 * cadeia de autenticacao/autorizacao. Sera substituido por controllers reais
 * nos proximos tickets (#35+).
 */
@RestController
public class PingController {

    @GetMapping("/professor/cobranca-pix/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok");
    }
}
