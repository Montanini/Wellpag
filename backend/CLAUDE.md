# CLAUDE.md — backend

Convenções específicas deste diretório. Visão geral do projeto, arquitetura, camadas, fluxos e comandos de alto nível estão no `CLAUDE.md` da raiz — não duplicar aqui.

## Versões

- Java 21 (LTS), Spring Boot 3.5.5.
- `jjwt` 0.12.5 e `springdoc-openapi-starter-webmvc-ui` 2.3.0 são pinados explicitamente no `pom.xml` — não são gerenciados pelo BOM do `spring-boot-starter-parent`, então não sobem sozinhos quando o Spring Boot é atualizado.

## Testes

- Rodar uma classe de teste isolada: `mvn test -Dtest=NomeDaClasse`
- Cobertura hoje é essencialmente zero — só existe `WellpagApplicationTests.contextLoads()` (smoke test do contexto Spring). Isso é o estado atual conhecido, não um TODO implícito: não assuma que testes existentes cobrem uma regra de negócio antes de alterá-la.
- CI (`mvn verify -B`) sobe um `mongo:7` real como service container do GitHub Actions.

## Particularidades

- Lombok é excluído explicitamente do `spring-boot-maven-plugin` (`<excludes>`) para não ser reprocessado no build do JAR final.
- Lombok 1.18.46 (a versão mais recente no Maven Central) não gera getters/setters sob JDK 25 — por isso o `java.version` está fixado em 21, não é escolha arbitrária. Se algum dia cogitar subir pra 25, teste primeiro (`mvn clean compile` e confira se os getters/setters foram gerados via `javap`) — o erro é silencioso (compila com ~100 "cannot find symbol", não um erro claro sobre o Lombok).
- Este diretório hoje só contém o fluxo de webhook bancário (`/webhook/**` — `WebhookController`/`WebhookService`/`webhook/*Parser`) — todo o resto foi extraído pros microsserviços em `services/` (ver `CLAUDE.md` da raiz, seção "Current Architecture"). `config/MongoConfig.java` (converter `LocalTime↔String`) foi removido junto com `Horario` quando o portal do aluno foi migrado — não existe mais neste módulo. Não roda em nenhum ambiente orquestrado (nem dev nem prod) desde que o dono decidiu não usar webhook por enquanto; só serve pra rodar standalone (`mvn spring-boot:run`) se alguém for trabalhar na extração futura do `webhook-service`.
