# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Wellpag is a SaaS platform for autonomous teachers to manage students, schedules, and monthly fees. It is a full-stack monorepo with a Spring Boot backend and a Next.js frontend.

- **Backend**: Java 17 + Spring Boot 3.2.4 (monolith, `backend/`) plus 7 Java 21 + Spring Boot 3.5.5 microservices (`services/`) already extracted from it, MongoDB, JWT + Google OAuth2
- **Frontend**: React 19 + Next.js 15 (App Router) + TypeScript + Tailwind CSS
- **Messaging**: WhatsApp via Evolution API
- **Deployment**: self-hosted on the owner's local machine (fixed IP), no cloud PaaS — intended to run there until traffic outgrows it

> **Architecture status**: this is a strangler-pattern migration in progress, not a design-only document anymore. 7 of the 8 target microservices (see "Target Architecture") have already been extracted into `services/` and are live on `main`, each its own Maven module, each still pointing at the same physical MongoDB instance as the monolith (database-per-service is logical, not physical, in this transitional phase). A `services/gateway/` (Spring Cloud Gateway) is also live and is now the single HTTP entry point for the frontend, on port 8080. The original `backend/` monolith is still present and still runs, but has been trimmed down to only the one flow that has not been extracted yet: bank payment webhooks (`/webhook/**`), on port 8098 in dev. The student self-service portal (`/aluno/portal/**`), previously the monolith's other remaining flow, has since been migrated to `aluno-service`, which now orchestrates it via REST against `agenda-service` and `financeiro-service`. "Current Architecture" below describes what's left in the monolith; the extracted services are summarized in "Services & Ports" and "Target Architecture".

## Commands

### Frontend (`frontend/`)
```bash
npm run dev       # Dev server on http://localhost:3000
npm run build     # Production build
npm run lint      # ESLint
```

### Backend (`backend/`)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev     # Run with the dev profile (default day-to-day)
mvn spring-boot:run -Dspring-boot.run.profiles=teste    # Run with the teste profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod     # Run with the prod profile (needs real env vars, see below)
mvn verify -B                     # Build + run all tests
mvn clean package -DskipTests     # Build JAR without tests
```

### Local Infrastructure
```bash
docker-compose up -d   # Start MongoDB (27017) + Evolution API (8081)
```

## Environments

The monolith (`backend/`) runs as three Spring profiles, all intended to run side by side on the same physical machine (the owner's, fixed IP — see Project Overview). The 7 extracted services and the gateway currently only define a `dev` profile (see "Services & Ports" below) — `teste`/`prod` profiles for them are future work, not yet needed since only `dev` is exercised so far.

| Profile | Config file | Port | MongoDB database | Secrets |
|---|---|---|---|---|
| `dev` | `application-dev.yml` | 8098 | `wellpag_dev` | Fake, hardcoded in the file — safe to commit |
| `teste` | `application-teste.yml` | 8090 | `wellpag_teste` | Fake, hardcoded in the file — safe to commit |
| `prod` | `application-prod.yml` | 8082 (or `SERVER_PORT`) | via `MONGODB_URI` | Real, **only** from env vars — the app fails to start if one is missing |

Note the `dev` port: it moved from 8080 to **8098** — `services/gateway/` now owns 8080 as the single entry point for the frontend (see "Services & Ports"). `teste` (8090) and `prod` (8082/`SERVER_PORT`) are unchanged; nothing yet routes to them through a gateway.

All three share the same `docker-compose` MongoDB container (27017) and Evolution API (8081) — they're separated by database name and port, not by infrastructure. Google OAuth credentials are no longer read by the monolith (auth/OAuth2 login moved to `auth-service` — see below); `EVOLUTION_API_URL`/`EVOLUTION_API_KEY` are likewise no longer read by the monolith (WhatsApp moved to `notificacao-service`). Only JWT secret is still profile-specific for the monolith now (`frontend-url`/`webhook.base-url` were dropped along with the code that read them — see "Current Architecture").

Select a profile with `-Dspring-boot.run.profiles=<dev|teste|prod>` (see Commands) or `SPRING_PROFILES_ACTIVE=<profile>`.

## Services & Ports

Single entry point for the frontend is the gateway on **8080** — `NEXT_PUBLIC_API_URL` stays `http://localhost:8080` unchanged, only what's listening there changed (previously the monolith directly, now the gateway). The gateway (`services/gateway/`, Spring Cloud Gateway, MVC/servlet variant — `spring-cloud-starter-gateway-server-webmvc` on `spring-cloud-dependencies` 2025.0.2, the release train compatible with Spring Boot 3.5.5) is a pure reverse proxy: it does **not** validate JWTs itself, it just routes by `Path` predicate and forwards the request (including the `Authorization` header) unchanged — each downstream service validates its own JWT independently.

| Service | Dev port | Routes | Module |
|---|---|---|---|
| `gateway` | 8080 | routes everything below by path | `services/gateway/` |
| `relatorio-service` | 8091 | `GET /professor/dashboard`, `/professor/relatorios/**` | `services/relatorio-service/` |
| `auth-service` | 8092 | `/auth/**`, `/oauth2/**`, `/login/oauth2/**` | `services/auth-service/` |
| `aluno-service` | 8093 | `POST /alunos/cadastro`, `/professor/alunos/**`, `/aluno/portal/**` | `services/aluno-service/` |
| `agenda-service` | 8094 | `/professor/horarios/**` (+ internal `GET /portal/horarios`, called by `aluno-service`) | `services/agenda-service/` |
| `financeiro-service` | 8095 | `/professor/mensalidades/**` (+ internal `GET /portal/mensalidades`, `GET /portal/mensalidades/{mes}`, called by `aluno-service`) | `services/financeiro-service/` |
| `pagamento-service` | 8096 | `/professor/banco/**` | `services/pagamento-service/` |
| `notificacao-service` | 8097 | `/professor/notificacoes/**`, `/professor/whatsapp/**` | `services/notificacao-service/` |
| monolith (`backend/`) | 8098 (dev only) | `/webhook/**` | `backend/` |

`aluno-service`'s `/aluno/portal/**` orchestrates rather than owning all the data itself: `GET /perfil` is 100% local (aluno-service owns `Aluno`), but `GET /horarios` calls agenda-service's `GET /portal/horarios`, and `GET /mensalidades`, `GET /mensalidades/{mes}` and `GET /relatorio` call financeiro-service's `GET /portal/mensalidades[/{mes}]` (relatorio aggregates that same response locally) — all via `RestClient`, forwarding the caller's `Authorization` header unchanged (same pattern as `notificacao-service`'s `FinanceiroServiceClient`, see `services/aluno-service/src/main/java/com/wellpag/aluno/client/`). The two `/portal/**` endpoints on agenda-service/financeiro-service are protected by `hasRole("ALUNO")`, same as the `aluno-service` ones — they're not meant to be called by anything but `aluno-service`, but nothing currently enforces that beyond role-based JWT auth.

All 7 services + the monolith still point at the same physical MongoDB (`wellpag_dev`) in this transitional phase — logical database-per-service, not physical isolation yet. Running everything locally without Docker: start each module with its own `mvn spring-boot:run -Dspring-boot.run.profiles=dev` (each has its own `pom.xml`); `docker-compose up -d` (root) runs the same set as containers, wired to each other by service hostname (e.g. `http://financeiro-service:8095`) instead of `localhost`.

## Environment Variables

**Backend monolith (`backend/`)** — now that auth/OAuth2 login and WhatsApp were extracted to `auth-service`/`notificacao-service`, the monolith no longer reads `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET`/`EVOLUTION_API_URL`/`EVOLUTION_API_KEY` at all (those are now `auth-service`'s and `notificacao-service`'s concern respectively — see their own env vars). The monolith's only remaining required variable, across every profile, is the JWT secret (still validation-only — see "Current Architecture"):
```
JWT_SECRET=...   # dev/teste: hardcoded fake value in application-{dev,teste}.yml; prod: real value, env-only
```

**Backend monolith** — only required for the `prod` profile (no defaults, the app won't start without it):
```
MONGODB_URI=mongodb://<host>:27017/wellpag_prod
JWT_SECRET=<min 256-bit secret>
SERVER_PORT=8082   # optional, defaults to 8082
```

`dev` and `teste` need none of the above — their Mongo URI and JWT secret are hardcoded (fake values) in `application-dev.yml`/`application-teste.yml`.

**Gateway (`services/gateway/`)** — `dev` profile has working localhost defaults for all 7 service URLs + the monolith URL (see "Services & Ports"); override via `RELATORIO_SERVICE_URL`, `AUTH_SERVICE_URL`, `ALUNO_SERVICE_URL`, `AGENDA_SERVICE_URL`, `FINANCEIRO_SERVICE_URL`, `PAGAMENTO_SERVICE_URL`, `NOTIFICACAO_SERVICE_URL`, `MONOLITH_BASE_URL` (as done in `docker-compose.yml`, pointing at each container's hostname).

**Extracted services (`services/*`)** — each still needs its own `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` (`auth-service`) or `EVOLUTION_API_URL`/`EVOLUTION_API_KEY` (`notificacao-service`) as applicable; unchanged by this migration step. `aluno-service`'s `dev` profile has working localhost defaults for the two services it calls to orchestrate the portal: `AGENDA_SERVICE_URL` (default `http://localhost:8094`) and `FINANCEIRO_SERVICE_URL` (default `http://localhost:8095`) — same `wellpag.<service>.base-url` pattern as `notificacao-service`'s `FINANCEIRO_SERVICE_URL`; overridden to container hostnames in `docker-compose.yml`.

Bank integration credentials (Banco Inter OAuth2 client id/secret, mTLS certificate/key, PIX key) are **not** environment variables in any profile — they are configured per-professor at runtime and stored in the `banco_configuracao_inter` MongoDB collection, now owned by `pagamento-service`'s `BancoController` (moved out of the monolith).

**Frontend** (copy from `.env.local.example`):
```
NEXT_PUBLIC_API_URL=http://localhost:8080
```

## Current Architecture (Monolith)

### Backend (`backend/src/main/java/com/wellpag/`)

The monolith no longer serves the frontend directly — it now only handles the one flow not yet extracted to a microservice, reached through the gateway (see "Services & Ports"):
- **Bank payment webhooks** (`/webhook/**`, public route) — `WebhookController` → `WebhookService` → parser selected via `BancoIntegracao` (`InterParser`/`AsaasParser`/`PixGenericoParser`/`GenericoParser`, behind the `BancoParser` interface) → CPF match against `Aluno` → marks the matching `Mensalidade` as `PAGO` → writes a `NotificacaoPagamento`.

The student self-service portal (`/aluno/portal/**`, previously `AlunoPortalController`/`AlunoPortalService` here) has been migrated to `aluno-service` — see "Services & Ports" for the new orchestration (aluno-service calls agenda-service and financeiro-service via REST for schedules/fees) and the `aluno-service` row in "Target Architecture".

Everything else (auth/login, student CRUD, schedules CRUD, fee CRUD, bank credential config, notifications, WhatsApp, and now the student portal) was extracted — its controllers/services/DTOs were deleted from the monolith and now live only in the corresponding `services/*` module (see "Services & Ports" / "Target Architecture"). What's left under `backend/src/main/java/com/wellpag/`:
- `controller/` — `WebhookController` (1 REST controller; API docs at `http://localhost:8098/swagger-ui` in dev)
- `service/` — `WebhookService`
- `model/`, `repository/` — trimmed to what that one flow still touches directly: `Usuario`, `Aluno`, `Mensalidade`, `NotificacaoPagamento`, plus enums (`BancoIntegracao`, `Role`, `StatusMensalidade`, `StatusNotificacao`, `AuthProvider`). `Horario`/`DiaSemana`/`TipoHorario`/`HorarioRepository` were deleted along with the portal migration (confirmed via grep that `WebhookService` never touched them) — along with `config/MongoConfig.java` (its `LocalTime↔String` Mongo converter existed only for `Horario`; `AsaasParser`'s unrelated `LocalTime.NOON` usage doesn't need it, since it's combined into a `LocalDateTime`, not persisted as a bare field). Models/repositories that only other extracted flows used (`ConfiguracaoWhatsApp`, `LembreteEnviado`, `BancoConfiguracaoInter`) were deleted earlier along with those.
- `security/` — `JwtService` (validation/claim extraction only now — token issuance moved to `auth-service`), `JwtAuthFilter`
- `config/` — `SecurityConfig` (now `permitAll()` on every request — `/webhook/**` was already public and, after the portal migration, no route in this module needs authentication or a role check anymore; `JwtAuthFilter`/`JwtService` are kept wired but have no practical effect), CORS, exception handling
- `webhook/` — bank payment webhook parsers, one per format (`InterParser`, `AsaasParser`, `PixGenericoParser`, `GenericoParser`) behind the `BancoParser` interface — unchanged, still monolith-only pending a future `webhook-service` extraction (see "Target Architecture")

### Frontend (`frontend/src/`)

Next.js App Router with two main role-based areas enforced by `middleware.ts`:

**Teacher (PROFESSOR role):**
- `/dashboard` — Hourly student view with payment status
- `/alunos` — Student list; generates auto-registration links
- `/alunos/[id]` — Student details/edit
- `/horarios` — Schedule management
- `/relatorios` — Financial reports
- `/notificacoes`, `/whatsapp` — Notification and WhatsApp API config

**Student (ALUNO role):**
- `/portal`, `/portal/horarios`, `/portal/historico`, `/portal/relatorio`

**Shared utilities in `lib/`:**
- `api.ts` — Typed fetch wrapper for all backend calls (uses `NEXT_PUBLIC_API_URL`)
- `auth.ts` — Auth helpers (JWT storage, user info)
- `types.ts` and `*-types.ts` files — TypeScript interfaces mirroring backend DTOs

### Data model key relationships
- `Aluno` has a `professorId` (ref to `Usuario`), a `usuarioId` (ref to the student's own `Usuario`, used to resolve the portal's `hasRole("ALUNO")` caller to their `Aluno` record(s) across `aluno-service`/`agenda-service`/`financeiro-service`) and a `cpf` used for automated payment matching
- `Horario` belongs to a professor, has `DiaSemana` + start/end time + type (`FIXO`/`AVULSO`) — now owned entirely by `agenda-service`, no longer present in the monolith
- `Mensalidade` tracks monthly fee per student with status: `A_PAGAR`, `PAGO`, `ATRASADO`

`LembreteEnviado` (dedupes WhatsApp reminders) and `BancoConfiguracaoInter` (Banco Inter OAuth2 + mTLS credentials) no longer live in the monolith — they moved to `notificacao-service` and `pagamento-service` respectively, along with the code that used them.

## Target Architecture — Microservices

Goal: split the original monolith into independently deployable services, keeping MongoDB (database-per-service, no relational migration). This is a learning/portfolio-driven redesign, not a response to a current scaling problem, migrated incrementally (strangler pattern: extract one service at a time behind the gateway). **7 of the 8 rows below are already extracted and live in `services/`; only `webhook-service` remains — see the note under the table.**

| Service | Responsibility | Sourced from (original monolith) | Status |
|---|---|---|---|
| `gateway` | Single HTTP entry point for the frontend (port 8080); routes by `Path` predicate to the service below or, for what's not extracted yet, to the monolith; pure reverse proxy — does **not** validate JWT, forwards `Authorization` unchanged | n/a (new) | **Done** — `services/gateway/` |
| `auth-service` | Login, JWT issuance, Google OAuth2, user registration | `AuthController/Service`, `security/`, `Usuario` | **Done** — `services/auth-service/` |
| `aluno-service` | Student CRUD, self-registration, student self-service portal (orchestrated via REST against agenda-service/financeiro-service) | `AlunoController/Service`, `Aluno`, `AlunoPortalController/Service` | **Done** — `services/aluno-service/` |
| `agenda-service` | Class schedules (fixed/one-off) | `HorarioController/Service`, `Horario` | **Done** — `services/agenda-service/` |
| `financeiro-service` | Monthly fees and their status (`A_PAGAR`/`PAGO`/`ATRASADO`) | `MensalidadeController/Service`, `Mensalidade` | **Done** — `services/financeiro-service/` |
| `pagamento-service` | Owns per-bank integration credentials (Inter OAuth2 + mTLS config), matches normalized payments to a student by CPF, decides when a fee is settled | `BancoController`, `BancoInterService`, `BancoConfiguracaoInter`, `BancoIntegracao` | **Done** — `services/pagamento-service/` |
| `notificacao-service` | WhatsApp reminders + payment notifications | `NotificacaoController/Service`, `WhatsAppController/Service`, `LembreteScheduler`, `EvolutionApiClient` | **Done** — `services/notificacao-service/` |
| `relatorio-service` | Dashboard and financial reports (read-only aggregation) | `DashboardController/Service`, `RelatorioController/Service` | **Done** — `services/relatorio-service/` |
| `webhook-service` | Receives each bank's webhook (Inter, Asaas, generic PIX) on its own endpoint, validates mTLS/signature, parses the payload into a normalized payload | `WebhookController`, `InterParser`, `AsaasParser`, `PixGenericoParser`, `GenericoParser` | **Not extracted** — still in `backend/` (`/webhook/**`) |

> `relatorio-service`'s dashboard also reads `Horario` (agenda-service), not just aluno/financeiro data — the table above only lists controller/service origin, not every read dependency, and that omission is a real source of coupling worth calling out explicitly.
>
> The student self-service portal (`AlunoPortalController/Service`, `/aluno/portal/**`) used to be stuck in `backend/` because it read across `aluno`/`agenda`/`financeiro` domains directly (`AlunoRepository`, `HorarioRepository`, `MensalidadeRepository`) and splitting it properly needed those three services to expose a real REST API to each other first. Now that `agenda-service` and `financeiro-service` are real (not just read-only Mongo bridges), that precondition is met: the portal was migrated into `aluno-service`, which owns `GET /perfil` locally and calls `GET /portal/horarios` (agenda-service) / `GET /portal/mensalidades[/{mes}]` (financeiro-service, including the lazy-creation of a month's fee) via `RestClient`, forwarding the caller's JWT. Those two `/portal/**` endpoints are internal to this orchestration (protected by `hasRole("ALUNO")`, same as everything else reachable with a student's own token) — they are not meant to be called by anything other than `aluno-service`, though nothing besides role-based JWT auth currently enforces that.

**Support patterns:**
- **Database-per-service**: each service keeps its own MongoDB database (e.g. `wellpag_aluno`, `wellpag_financeiro`) — planned, not real yet: all 7 services + the monolith currently still point at the same physical `wellpag_dev` MongoDB instance (transitional phase, see "Services & Ports").
- **API Gateway** — implemented (`services/gateway/`, Spring Cloud Gateway MVC/servlet variant): single entry point for the frontend, routes by `Path` predicate. It does **not** validate the JWT before routing (each service validates its own independently) — this was a deliberate choice to avoid redundant validation, not a gap; revisit only if a cross-cutting concern (rate limiting, centralized auth) actually needs it.
- **Synchronous REST** for direct reads — e.g. `relatorio-service` calls `financeiro-service` and `aluno-service` to build the dashboard.
- **Asynchronous events** (RabbitMQ) for flows currently coupled in-process in the monolith:
  1. Bank calls `webhook-service` (bank-specific endpoint, e.g. `/webhook/inter`).
  2. `webhook-service` validates and parses → publishes `PagamentoRecebido`.
  3. `pagamento-service` consumes it, matches the student by CPF against `aluno-service`.
  4. Publishes `MensalidadeQuitada` → `financeiro-service` updates the fee status → `notificacao-service` consumes it and sends the WhatsApp confirmation.
- **Local orchestration**: `docker-compose` extended to run all services + MongoDB + RabbitMQ, replacing today's single-container setup.

## CI

GitHub Actions workflow at `.github/workflows/backend-ci.yml` runs `mvn verify -B` on push/PR to `main`/`develop` for `backend/**` paths. Requires Java 17 and a local MongoDB 7 service spun up in the workflow. This pipeline covers only the monolith (`backend/`) — the 7 extracted services and `services/gateway/` each have their own `pom.xml` (Java 21 / Spring Boot 3.5.5) but no CI workflow of their own yet; that's still open work, not something this migration step added.

There is no cloud deployment target — the app runs on the owner's own machine (fixed IP), reachable directly without a PaaS. The multi-stage `backend/Dockerfile` (health-checks `/actuator/health`) can still be used to run it locally under Docker if preferred over `mvn spring-boot:run`.
