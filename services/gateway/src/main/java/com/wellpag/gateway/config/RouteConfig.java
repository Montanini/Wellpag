package com.wellpag.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

/**
 * Roteamento por Path predicate: um bean por servico, encaminhando o prefixo
 * de rota correspondente para cada um dos 7 microsservicos ja extraidos do
 * monolito, mais uma rota para o que restou no monolito (backend/): hoje so
 * /webhook/** (webhooks bancarios) e /aluno/portal/** (portal do aluno).
 *
 * O gateway e' um proxy reverso puro: nao valida JWT (cada servico ja valida
 * o seu proprio token de forma independente - ver JwtAuthFilter/JwtService em
 * cada modulo) e nao altera headers - HandlerFunctions.http() repassa a
 * requisicao original (incluindo o header Authorization) tal como recebida.
 */
@Configuration
@RequiredArgsConstructor
public class RouteConfig {

    private final GatewayProperties properties;

    @Bean
    public RouterFunction<ServerResponse> relatorioServiceRoute() {
        return route("relatorio_service")
            .route(path("/professor/dashboard", "/professor/dashboard/**", "/professor/relatorios/**"), http())
            .before(uri(properties.getServices().getRelatorio()))
            .build();
    }

    @Bean
    public RouterFunction<ServerResponse> authServiceRoute() {
        return route("auth_service")
            .route(path("/auth/**", "/oauth2/**", "/login/oauth2/**"), http())
            .before(uri(properties.getServices().getAuth()))
            .build();
    }

    @Bean
    public RouterFunction<ServerResponse> alunoServiceRoute() {
        return route("aluno_service")
            .route(path("/alunos/cadastro", "/professor/alunos/**"), http())
            .before(uri(properties.getServices().getAluno()))
            .build();
    }

    @Bean
    public RouterFunction<ServerResponse> agendaServiceRoute() {
        return route("agenda_service")
            .route(path("/professor/horarios/**"), http())
            .before(uri(properties.getServices().getAgenda()))
            .build();
    }

    @Bean
    public RouterFunction<ServerResponse> financeiroServiceRoute() {
        return route("financeiro_service")
            .route(path("/professor/mensalidades/**"), http())
            .before(uri(properties.getServices().getFinanceiro()))
            .build();
    }

    @Bean
    public RouterFunction<ServerResponse> pagamentoServiceRoute() {
        return route("pagamento_service")
            .route(path("/professor/banco/**"), http())
            .before(uri(properties.getServices().getPagamento()))
            .build();
    }

    @Bean
    public RouterFunction<ServerResponse> notificacaoServiceRoute() {
        return route("notificacao_service")
            .route(path("/professor/notificacoes/**", "/professor/whatsapp/**"), http())
            .before(uri(properties.getServices().getNotificacao()))
            .build();
    }

    @Bean
    public RouterFunction<ServerResponse> monolithRoute() {
        return route("monolith")
            .route(path("/webhook/**", "/aluno/portal/**"), http())
            .before(uri(properties.getMonolith().getBaseUrl()))
            .build();
    }
}
