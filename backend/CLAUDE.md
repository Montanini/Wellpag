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

- `config/MongoConfig.java` registra converters customizados `LocalTime↔String` — o driver do MongoDB não serializa `java.time.LocalTime` nativamente, então campos desse tipo (ex. horários de aula) dependem desses converters para persistir/ler corretamente.
- Lombok é excluído explicitamente do `spring-boot-maven-plugin` (`<excludes>`) para não ser reprocessado no build do JAR final.
