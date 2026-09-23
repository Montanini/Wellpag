# CLAUDE.md — backend

Convenções específicas deste diretório. Visão geral do projeto, arquitetura, camadas, fluxos e comandos de alto nível estão no `CLAUDE.md` da raiz — não duplicar aqui.

`backend/` agrupa os 8 módulos Maven independentes (7 microsserviços + gateway) — não existe mais um único monolito nesta pasta, nem um POM pai agregando os módulos. Cada subpasta (`backend/<servico>/`) tem seu próprio `pom.xml`, `Dockerfile` e `application*.yml`, e é buildada/rodada isoladamente (`cd backend/<servico>` antes de qualquer comando `mvn`).

## Versões

- Java 21 (LTS), Spring Boot 3.5.5 em todos os 8 módulos.
- `jjwt` 0.12.5 e `springdoc-openapi-starter-webmvc-ui` são pinados explicitamente no `pom.xml` de cada módulo — não são gerenciados pelo BOM do `spring-boot-starter-parent`, então não sobem sozinhos quando o Spring Boot é atualizado. Se atualizar a versão em um módulo, considere se os outros também precisam.
- Lombok 1.18.46 em todos os módulos, excluído explicitamente do `spring-boot-maven-plugin` (`<excludes>`) em cada `pom.xml` para não ser reprocessado no build do JAR final.

## Particularidades

- Lombok 1.18.46 (a versão mais recente no Maven Central) não gera getters/setters sob JDK 25 — por isso `java.version` está fixado em 21 em todo módulo, não é escolha arbitrária. Se algum dia cogitar subir pra 25, teste primeiro (`mvn clean compile` e confira se os getters/setters foram gerados via `javap`) — o erro é silencioso (compila com ~100 "cannot find symbol", não um erro claro sobre o Lombok).
- Cobertura de testes hoje é essencialmente zero na maioria dos módulos — normalmente só o smoke test do contexto Spring (`*ApplicationTests.contextLoads()`). Não assuma que testes existentes cobrem uma regra de negócio antes de alterá-la.
- Não há workflow de CI configurado hoje para nenhum dos 8 módulos (ver seção "CI" no `CLAUDE.md` da raiz) — rodar `mvn verify` localmente antes de abrir PR é a única rede de segurança no momento.
- O monolito original (que ocupava esta pasta `backend/` como módulo único antes da reorganização) foi removido do repositório — o fluxo de webhooks bancários que só existia nele foi descontinuado, não extraído para um `webhook-service`. Não existe mais nenhuma rota `/webhook/**` em nenhum lugar do projeto.
- `pagamento-service` (integração Banco Inter) também foi removido do repositório por completo — não só o fluxo de webhook, a integração inteira (credenciais, mTLS, `/professor/banco/**`) foi descontinuada. Se precisar reconstruir, o código antigo é recuperável no histórico do git antes da remoção.
- `cobranca-pix-service` (issue #33, scaffold em #34) é o 8º módulo, criado do zero (não extraído do monolito) — substitui em espírito, não em código, a integração Banco Inter removida. Ainda não tem rota no gateway nem no `docker-compose*.yml` (isso é escopo da issue #35+); rode-o isoladamente com `mvn spring-boot:run -Dspring-boot.run.profiles=dev` a partir de `backend/cobranca-pix-service/` enquanto isso.
