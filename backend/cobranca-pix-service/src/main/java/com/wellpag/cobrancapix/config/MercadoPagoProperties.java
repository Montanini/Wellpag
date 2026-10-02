package com.wellpag.cobrancapix.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Credenciais da aplicacao OAuth do Mercado Pago (Mercado Pago Connect) —
 * ver wellpag.mercadopago.* em application.yml.
 * <p>
 * Registrada como {@code @Component} + {@code @ConfigurationProperties}
 * (mesmo padrao ja usado por {@code GatewayProperties} em backend/gateway/),
 * o Spring instancia e faz o bind desta classe durante o carregamento do
 * contexto. Isso sozinho, porem, NAO basta para fail-fast: ao contrario de
 * {@code @Value("${...}")} (que falha imediatamente se o placeholder nao for
 * resolvivel — ver JwtService/wellpag.jwt.secret), o {@code Binder} usado
 * por {@code @ConfigurationProperties} NAO deixa o campo {@code null} nem
 * lanca excecao quando um placeholder como {@code ${MP_CLIENT_ID}} nao tem
 * valor nem default: o {@code PropertySourcesPlaceholdersResolver} interno
 * do Spring Boot usa {@code ignoreUnresolvablePlaceholders = true}
 * deliberadamente (ao contrario de {@code @Value}), entao o campo acaba
 * igual ao texto literal nao resolvido — {@code "${MP_CLIENT_ID}"} — uma
 * string nao-vazia que passaria por um {@code @NotBlank} sozinho sem
 * detectar nada (confirmado empiricamente com um log temporario durante o
 * desenvolvimento desta classe). Por isso, alem de {@code @NotBlank}, cada
 * campo tem {@code @Pattern} rejeitando qualquer valor que ainda comece com
 * {@code ${} — e' esse padrao, nao o {@code @NotBlank} isoladamente, que de
 * fato detecta "variavel de ambiente ausente" aqui. {@code @Validated} na
 * classe faz o {@code ConfigurationPropertiesBindingPostProcessor} rodar o
 * Bean Validation logo apos o bind, e uma violacao derruba o contexto com
 * {@code ConfigurationPropertiesBindException}.
 * <p>
 * Ativa em TODOS os profiles (correcao feita na issue #35, ver historico de
 * commits): a issue #34 (scaffold puro) tinha isto como {@code @Profile("prod")}
 * porque nenhum controller lia o bean ainda, e exigir as 3 variaveis tambem em
 * dev teria bloqueado `mvn clean verify`/`spring-boot:run -Dspring-boot.run.profiles=dev`
 * sem ganho nenhum naquela fase. A partir da issue #35, o fluxo OAuth Connect
 * (GET /professor/cobranca-pix/connect + GET /cobranca-pix/oauth/callback) le
 * clientId/clientSecret/redirectUri de verdade para montar a URL de autorizacao
 * e trocar o code por token — entao o bean precisa estar ativo (e validado) em
 * todo profile, exatamente como GOOGLE_CLIENT_ID/GOOGLE_CLIENT_SECRET ja
 * funcionam em auth-service: MP_CLIENT_ID/MP_CLIENT_SECRET/MP_REDIRECT_URI nao
 * tem default em NENHUM profile (nem dev) — quem roda localmente precisa
 * exportar manualmente qualquer valor fake nao-vazio (ex.: `MP_CLIENT_ID=dev-fake-client-id`).
 * <p>
 * clientId/clientSecret/redirectUri sao lidos por OAuthStateService (redirectUri
 * na URL de autorizacao) e por MercadoPagoClient (troca do code por token).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "wellpag.mercadopago")
@Validated
public class MercadoPagoProperties {

    private static final String NAO_RESOLVIDO_MSG =
        "propriedade ausente (variavel de ambiente nao definida)";

    @NotBlank(message = NAO_RESOLVIDO_MSG)
    @Pattern(regexp = "^(?!\\$\\{).*$", message = NAO_RESOLVIDO_MSG)
    private String clientId;

    @NotBlank(message = NAO_RESOLVIDO_MSG)
    @Pattern(regexp = "^(?!\\$\\{).*$", message = NAO_RESOLVIDO_MSG)
    private String clientSecret;

    @NotBlank(message = NAO_RESOLVIDO_MSG)
    @Pattern(regexp = "^(?!\\$\\{).*$", message = NAO_RESOLVIDO_MSG)
    private String redirectUri;
}
