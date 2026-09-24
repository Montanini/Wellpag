package com.wellpag.cobrancapix.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
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
 * {@code @Profile("prod")} e proposital: MP_CLIENT_ID/MP_CLIENT_SECRET/
 * MP_REDIRECT_URI nao tem default em NENHUM profile (nao da pra fakear
 * credencial real de app OAuth de terceiro — mesmo padrao ja adotado, ainda
 * que nao validado, por GOOGLE_CLIENT_ID em auth-service), mas o fail-fast
 * em si so e' exigido (issue #34) para o profile prod — exigir as 3 variaveis
 * tambem em dev quebraria `mvn clean verify`/`spring-boot:run -Dspring-boot.run.profiles=dev`
 * para qualquer um que nao tenha uma app Mercado Pago configurada, sem
 * ganho nenhum nesta fase de scaffold (nenhum controller le esses campos
 * ainda). Isso sera revisitado quando o fluxo OAuth Connect (issue #35+)
 * precisar de credenciais reais tambem em dev.
 * <p>
 * Nao ha logica de negocio aqui ainda — clientId/clientSecret/redirectUri
 * serao lidos pelo fluxo OAuth Connect implementado em issue #35+.
 */
@Getter
@Setter
@Component
@Profile("prod")
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
