package com.wellpag.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * URLs base dos 7 microsservicos ja extraidos, usadas pelo RouteConfig para
 * montar as rotas do gateway. Cada uma vem de uma property configuravel (com
 * default de dev sem Docker) - ver application.yml/application-dev.yml.
 *
 * O monolito (backend/) nao e mais roteado por aqui - ver comentario em
 * RouteConfig sobre a remocao da rota /webhook/**.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "wellpag")
public class GatewayProperties {

    private Services services = new Services();

    @Getter
    @Setter
    public static class Services {
        private String relatorio;
        private String auth;
        private String aluno;
        private String agenda;
        private String financeiro;
        private String pagamento;
        private String notificacao;
    }
}
