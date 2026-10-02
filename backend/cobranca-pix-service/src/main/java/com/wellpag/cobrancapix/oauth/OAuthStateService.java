package com.wellpag.cobrancapix.oauth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Assina e valida o parametro {@code state} do fluxo OAuth Connect
 * (GET /professor/cobranca-pix/connect -> Mercado Pago -> GET
 * /cobranca-pix/oauth/callback).
 * <p>
 * Implementado como um JWT compacto (reaproveitando jjwt 0.12.5, ja
 * dependencia deste modulo para JwtService) com o {@code professorId} como
 * subject e um {@code exp} curto — assinado com uma chave HMAC derivada de
 * {@code wellpag.mercadopago.oauth-state-secret}, um dominio de assinatura
 * deliberadamente separado de {@code wellpag.jwt.secret} (exigencia explicita
 * do ticket #35: um "state" forjado nao pode ser um JWT de login valido nem
 * vice-versa, mesmo que ambos usem jjwt). Nao reaproveita JwtService por isso.
 * <p>
 * <b>Protecao contra replay</b>: esta implementacao usa APENAS a janela curta
 * de expiracao ({@link #STATE_TTL}) — nao ha um registro persistido de
 * "state ja usado". Isso rejeita um state expirado, mas NAO impede um mesmo
 * state valido de ser usado duas vezes dentro da janela de 10 minutos (ex.: o
 * professor abre o link de callback duas vezes, ou um atacante intercepta o
 * redirect e o replaya antes de expirar). Dado que o callback e idempotente
 * por professorId (upsert por {@code professorId} unico — reconectar so
 * atualiza a linha existente, nunca duplica nem quebra), o pior caso de um
 * replay dentro da janela e' uma segunda troca de code por token (que por sua
 * vez falharia no lado do Mercado Pago, ja que um {@code code} de
 * authorization_code so e valido uma vez la) — nao um estado inconsistente
 * localmente. Julgamento de engenharia: para o escopo desta issue, a janela
 * curta + a troca de code (que o Mercado Pago mesmo invalida apos o primeiro
 * uso) foi considerada suficiente; um store dedicado de "state usado"
 * (ex.: colecao Mongo com TTL index) e a alternativa mais robusta se um
 * revisor entender que a garantia acima nao e forte o bastante.
 */
@Slf4j
@Service
public class OAuthStateService {

    private static final Duration STATE_TTL = Duration.ofMinutes(10);

    private final SecretKey key;

    public OAuthStateService(@Value("${wellpag.mercadopago.oauth-state-secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Gera um state assinado contendo o professorId, valido por {@link #STATE_TTL}. */
    public String gerar(String professorId) {
        Instant agora = Instant.now();
        return Jwts.builder()
            .subject(professorId)
            .issuedAt(Date.from(agora))
            .expiration(Date.from(agora.plus(STATE_TTL)))
            .signWith(key)
            .compact();
    }

    /**
     * Valida a assinatura e a expiracao do state e retorna o professorId nele
     * contido. Assinatura invalida e state expirado sao tratados da mesma
     * forma (ambos lancam {@link IllegalArgumentException}) — deliberado: o
     * callback e uma rota publica chamada pelo navegador do professor apos o
     * redirect do Mercado Pago, e distinguir "invalido" de "expirado" na
     * resposta nao e exigido pelo ticket e vazaria detalhe desnecessario para
     * quem quer que bata nesse endpoint.
     */
    public String validarEExtrairProfessorId(String state) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(state)
                .getPayload();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("State OAuth invalido ou expirado: {}", e.getMessage());
            throw new IllegalArgumentException("state invalido ou expirado", e);
        }
    }
}
