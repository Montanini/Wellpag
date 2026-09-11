# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Wellpag is a SaaS platform for autonomous teachers to manage students, schedules, and monthly fees. It is a full-stack monorepo with a Spring Boot backend and a Next.js frontend.

- **Backend**: Java 21 + Spring Boot 3.5.5 throughout — 7 independent Maven modules (6 microservices + a gateway) under `backend/`, MongoDB, JWT + Google OAuth2
- **Frontend**: React 19 + Next.js 15 (App Router) + TypeScript + Tailwind CSS
- **Messaging**: WhatsApp via Evolution API
- **Deployment**: self-hosted on the owner's local machine (fixed IP), no cloud PaaS — intended to run there until traffic outgrows it

> **Architecture status**: the strangler-pattern migration off the original monolith is done. The remaining target microservices plus the gateway (see "Architecture history & patterns" below for how they got here) are live on `main`, each its own Maven module under `backend/`, each still pointing at the same physical MongoDB instance (database-per-service is logical, not physical, in this transitional phase). The gateway is the single HTTP entry point for the frontend, on port 8080. The original monolith (which used to live at `backend/` as a single module, before this directory was repurposed) has been **deleted from the repository**: its last remaining flow, bank payment webhooks (`/webhook/**`), was never extracted into its own `webhook-service` — the owner decided to discontinue that flow entirely rather than migrate it, since webhook-based payment matching wasn't in use. Later, the owner discontinued the Banco Inter integration **entirely** (not just the webhook piece) — `pagamento-service` (credential storage, mTLS config, the `/professor/banco/**` API) was removed from the repository too, along with its frontend UI and gateway route. If either of these is ever wanted again, they need to be rebuilt from scratch (or recovered from git history prior to removal), not resumed from a half-finished extraction.

## Repository Layout

Top-level split is backend vs. frontend:

```
backend/            # 7 independent Maven modules (Java 21 / Spring Boot 3.5.5), no parent POM
  gateway/
  auth-service/
  aluno-service/
  agenda-service/
  financeiro-service/
  notificacao-service/
  relatorio-service/
frontend/            # Next.js 15 app
docker-compose.yml            # dev: MongoDB + Evolution API + all 7 backend modules
docker-compose.prod.yml       # prod overlay, see "Production Deployment"
docs/
```

Each module under `backend/` is fully self-contained (own `pom.xml`, own `Dockerfile`, own `application*.yml`) — there is no multi-module parent POM aggregating them. `cd` into the specific module you're working on.

## Commands

### Backend (`backend/<servico>/`, one of: `gateway`, `auth-service`, `aluno-service`, `agenda-service`, `financeiro-service`, `notificacao-service`, `relatorio-service`)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev     # Run with the dev profile (default day-to-day)
mvn spring-boot:run -Dspring-boot.run.profiles=prod     # Run with the prod profile (needs real env vars, see below)
mvn verify -B                     # Build + run all tests
mvn clean package -DskipTests     # Build JAR without tests
```
Run this from inside the module's own directory (e.g. `cd backend/auth-service`) — each has its own `pom.xml`, there's no root-level Maven command that builds all 7 at once.

### Frontend (`frontend/`)
```bash
npm run dev       # Dev server on http://localhost:3000
npm run build     # Production build
npm run lint      # ESLint
```

### Local Infrastructure
```bash
docker-compose up -d --build   # Start MongoDB (27017) + Evolution API (8081) + all 7 backend modules + gateway
```

## Environments

Each of the 7 `backend/` modules defines `dev` and `prod` Spring profiles; `teste` is future work, not yet needed since only `dev`/`prod` are exercised so far.

| Profile | Config file | Secrets |
|---|---|---|
| `dev` | `application-dev.yml` | Fake, hardcoded in the file — safe to commit |
| `prod` | `application-prod.yml` | Real, **only** from env vars — the app fails to start if one is missing |

Select a profile with `-Dspring-boot.run.profiles=<dev|prod>` (see Commands) or `SPRING_PROFILES_ACTIVE=<profile>`. Dev ports are listed in "Services & Ports" below. All modules share the same `docker-compose` MongoDB container (27017) and Evolution API (8081) in dev — they're separated by database name and port, not by infrastructure.

## Services & Ports

Single entry point for the frontend is the gateway on **8080** — `NEXT_PUBLIC_API_URL` stays `http://localhost:8080`. The gateway (`backend/gateway/`, Spring Cloud Gateway, MVC/servlet variant — `spring-cloud-starter-gateway-server-webmvc` on `spring-cloud-dependencies` 2025.0.2, the release train compatible with Spring Boot 3.5.5) is a pure reverse proxy: it does **not** validate JWTs itself, it just routes by `Path` predicate and forwards the request (including the `Authorization` header) unchanged — each downstream service validates its own JWT independently.

| Service | Dev port | Routes | Module |
|---|---|---|---|
| `gateway` | 8080 | routes everything below by path | `backend/gateway/` |
| `relatorio-service` | 8091 | `GET /professor/dashboard`, `/professor/relatorios/**` | `backend/relatorio-service/` |
| `auth-service` | 8092 | `/auth/**`, `/oauth2/**`, `/login/oauth2/**` | `backend/auth-service/` |
| `aluno-service` | 8093 | `POST /alunos/cadastro`, `/professor/alunos/**`, `/aluno/portal/**` | `backend/aluno-service/` |
| `agenda-service` | 8094 | `/professor/horarios/**` (+ internal `GET /portal/horarios`, called by `aluno-service`) | `backend/agenda-service/` |
| `financeiro-service` | 8095 | `/professor/mensalidades/**` (+ internal `GET /portal/mensalidades`, `GET /portal/mensalidades/{mes}`, called by `aluno-service`) | `backend/financeiro-service/` |
| `notificacao-service` | 8097 | `/professor/whatsapp/**` | `backend/notificacao-service/` |

Port 8096 (`pagamento-service`) is retired — that module was removed entirely along with the Banco Inter integration (see "Architecture status"). Nothing occupies that port anymore. `notificacao-service` also no longer serves `/professor/notificacoes/**` (payment notifications received via bank webhook, manually linked by the professor to a fee) — that feature depended entirely on the bank webhook flow, and once that was removed (see "Architecture status"), the notification list had no data source left (always empty) and was discontinued too. `notificacao-service` today is WhatsApp reminders only.

`aluno-service`'s `/aluno/portal/**` orchestrates rather than owning all the data itself: `GET /perfil` is 100% local (aluno-service owns `Aluno`), but `GET /horarios` calls agenda-service's `GET /portal/horarios`, and `GET /mensalidades`, `GET /mensalidades/{mes}` and `GET /relatorio` call financeiro-service's `GET /portal/mensalidades[/{mes}]` (relatorio aggregates that same response locally) — all via `RestClient`, forwarding the caller's `Authorization` header unchanged (same pattern first established for `notificacao-service`'s own inter-service client, since removed along with the payment-notification feature). The two `/portal/**` endpoints on agenda-service/financeiro-service are protected by `hasRole("ALUNO")`, same as the `aluno-service` ones — they're not meant to be called by anything but `aluno-service`, but nothing currently enforces that beyond role-based JWT auth.

All 7 modules still point at the same physical MongoDB (`wellpag_dev` in dev, `wellpag_prod` in production — see "Production Deployment") in this transitional phase — logical database-per-service, not physical isolation yet. Running everything locally without Docker: start each module with its own `mvn spring-boot:run -Dspring-boot.run.profiles=dev` (each has its own `pom.xml`, run from inside `backend/<servico>/`); `docker-compose up -d` (root) runs the same set as containers, wired to each other by service hostname (e.g. `http://financeiro-service:8095`) instead of `localhost`.

## Environment Variables

**Gateway (`backend/gateway/`)** — `dev` profile has working localhost defaults for all 6 service URLs (see "Services & Ports"); override via `RELATORIO_SERVICE_URL`, `AUTH_SERVICE_URL`, `ALUNO_SERVICE_URL`, `AGENDA_SERVICE_URL`, `FINANCEIRO_SERVICE_URL`, `NOTIFICACAO_SERVICE_URL` (as done in `docker-compose.yml`/`docker-compose.prod.yml`, pointing at each container's hostname). There is no monolith/webhook route, and no `pagamento-service` route (see "Architecture status").

**Each service (`backend/*`)** needs its own `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` (`auth-service`) or `EVOLUTION_API_URL`/`EVOLUTION_API_KEY` (`notificacao-service`) as applicable. `aluno-service`'s `dev` profile has working localhost defaults for the two services it calls to orchestrate the portal: `AGENDA_SERVICE_URL` (default `http://localhost:8094`) and `FINANCEIRO_SERVICE_URL` (default `http://localhost:8095`) — same `wellpag.<service>.base-url` pattern as `notificacao-service`'s `FINANCEIRO_SERVICE_URL`; overridden to container hostnames in `docker-compose.yml`.

Every service requires `JWT_SECRET` in `prod` (no default): dev has a hardcoded fake value in `application-dev.yml`.

**Frontend** (copy from `.env.local.example`):
```
NEXT_PUBLIC_API_URL=http://localhost:8080
```

## Production Deployment (self-hosted)

The first real production environment is a Docker Compose overlay on top of the local dev setup, meant to run on the owner's machine (fixed IP):

```bash
cp .env.example .env    # fill in real secrets — never commit .env
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

- `docker-compose.prod.yml` (root) is an **overlay**, not a standalone file — it's always used together with the base `docker-compose.yml`. Each service in the overlay repeats its **complete** production `environment` list rather than relying on partial merge with the base file's list.
- Runs all 7 `backend/` modules (`relatorio-service` … `notificacao-service` + `gateway`), all with `SPRING_PROFILES_ACTIVE=prod`, pointing at their `application-prod.yml`.
- All 7 modules share one physical MongoDB database in production too (`wellpag_prod`, same transitional database-per-service-is-logical-not-physical phase as dev's `wellpag_dev`), via one `MONGODB_URI` built from `${MONGO_ROOT_USER}`/`${MONGO_ROOT_PASSWORD}` in `.env`.
- Unlike the dev `docker-compose.yml`, the production `mongodb` service enables authentication (`MONGO_INITDB_ROOT_USERNAME`/`MONGO_INITDB_ROOT_PASSWORD`) and does **not** expose port 27017 to the host — only the services on the internal Compose network can reach it (`mongodb:27017`). Removing an inherited port mapping needs the Compose `!reset` tag (`ports: !reset []`); a plain `ports: []` would not remove it, since Compose merges (appends) sequence-typed attributes like `ports` across files by default.
- Runtime validation of this overlay (`docker compose -f docker-compose.yml -f docker-compose.prod.yml config`, and an actual `up -d` with a real `.env`) is pending — it needs to be run on a machine with Docker available.

## Data model

Key relationships (each field now owned by whichever service's model actually persists it — several services keep their own read-only trimmed copy of a model they don't own, see "Read-only cross-domain bridge" below):
- `Aluno` has a `professorId` (ref to `Usuario`), a `usuarioId` (ref to the student's own `Usuario`, used to resolve the portal's `hasRole("ALUNO")` caller to their `Aluno` record(s) across `aluno-service`/`agenda-service`/`financeiro-service`) and a `cpf` used for automated payment matching
- `Horario` belongs to a professor, has `DiaSemana` + start/end time + type (`FIXO`/`AVULSO`) — owned by `agenda-service`
- `Mensalidade` tracks monthly fee per student with status: `A_PAGAR`, `PAGO`, `ATRASADO` — owned by `financeiro-service`
- `LembreteEnviado` (dedupes WhatsApp reminders) — owned by `notificacao-service`

`BancoConfiguracaoInter` (Banco Inter OAuth2 + mTLS credentials) no longer exists — it was owned by `pagamento-service`, which was removed entirely (see "Architecture status"). The `banco_configuracao_inter` MongoDB collection is likewise gone from any freshly-provisioned database; on an existing one it may need manual cleanup.

## Frontend (`frontend/src/`)

Next.js App Router with two main role-based areas enforced by `middleware.ts`:

**Teacher (PROFESSOR role):**
- `/dashboard` — Hourly student view with payment status
- `/alunos` — Student list; generates auto-registration links
- `/alunos/[id]` — Student details/edit
- `/horarios` — Schedule management
- `/relatorios` — Financial reports
- `/whatsapp` — WhatsApp connection and reminder config

**Student (ALUNO role):**
- `/portal`, `/portal/horarios`, `/portal/historico`, `/portal/relatorio`

**Shared utilities in `lib/`:**
- `api.ts` — Typed fetch wrapper for all backend calls (uses `NEXT_PUBLIC_API_URL`)
- `auth.ts` — Auth helpers (JWT storage, user info)
- `types.ts` and `*-types.ts` files — TypeScript interfaces mirroring backend DTOs

## Architecture history & patterns

The system was split out of an original monolith into independently deployable services, keeping MongoDB (database-per-service, no relational migration). This was a learning/portfolio-driven redesign, not a response to a current scaling problem, migrated incrementally (strangler pattern: extract one service at a time behind the gateway). 7 of the originally-planned 8 services were extracted this way (everything except bank webhooks); the monolith itself has since been deleted from the repo (see "Architecture status"). One of those 7 extracted services, `pagamento-service`, was later removed too — not because the extraction was wrong, but because the owner discontinued the Banco Inter integration it existed for. The table below is kept for historical context — responsibility and origin of each currently-live service:

| Service | Responsibility | Sourced from (original monolith) |
|---|---|---|
| `gateway` | Single HTTP entry point for the frontend (port 8080); routes by `Path` predicate to each of the services below; pure reverse proxy — does **not** validate JWT, forwards `Authorization` unchanged | n/a (new) |
| `auth-service` | Login, JWT issuance, Google OAuth2, user registration | `AuthController/Service`, `security/`, `Usuario` |
| `aluno-service` | Student CRUD, self-registration, student self-service portal (orchestrated via REST against agenda-service/financeiro-service) | `AlunoController/Service`, `Aluno`, `AlunoPortalController/Service` |
| `agenda-service` | Class schedules (fixed/one-off) | `HorarioController/Service`, `Horario` |
| `financeiro-service` | Monthly fees and their status (`A_PAGAR`/`PAGO`/`ATRASADO`) | `MensalidadeController/Service`, `Mensalidade` |
| `notificacao-service` | WhatsApp reminders (payment notifications via bank webhook were also extracted here originally, later discontinued — see note below) | `WhatsAppController/Service`, `LembreteScheduler`, `EvolutionApiClient` |
| `relatorio-service` | Dashboard and financial reports (read-only aggregation) | `DashboardController/Service`, `RelatorioController/Service` |

> `relatorio-service`'s dashboard also reads `Horario` (agenda-service), not just aluno/financeiro data — the table above only lists controller/service origin, not every read dependency, and that omission is a real source of coupling worth calling out explicitly.
>
> Bank payment webhooks (Inter, Asaas, generic PIX — receiving the bank's callback, validating it, parsing it into a normalized payload, then matching by CPF and settling a `Mensalidade`) were the one flow **never extracted**. The owner decided to discontinue that functionality rather than build it out as its own `webhook-service`, and the monolith that hosted it was deleted from the repo entirely. There is no `/webhook/**` route anywhere anymore.
>
> `notificacao-service` originally also owned the professor-facing half of that flow: `NotificacaoPagamento` records that a (never-built) `webhook-service` would have created, which the professor could review and manually link to a student/fee (`NotificacaoController/Service`, `/professor/notificacoes/**`). Since nothing ever fed that collection (the webhook side was never extracted), the list was permanently empty and the feature was discontinued — removed along with `BancoIntegracao`, `NotificacaoPagamento`, `StatusNotificacao`, and the inter-service client it used to confirm payments against `financeiro-service`. `notificacao-service` now exists solely for WhatsApp reminders.
>
> `pagamento-service` **was** extracted (it owned per-bank integration credentials — Inter OAuth2 + mTLS config, `BancoController`, `BancoInterService`, `BancoConfiguracaoInter`, `BancoIntegracao`) but has since been removed from the repository entirely, along with its frontend credential-configuration UI and its gateway route (`/professor/banco/**`). The owner discontinued the Banco Inter integration altogether, not just the webhook registration flow within it (which had already been found dead and removed earlier). If bank payment integration is wanted again in the future, it needs to be designed and built fresh (recoverable from git history before removal as a reference, not as a resumable branch).

**Extraction patterns established across the extracted services** (useful precedent if a new service is ever split out of an existing one):
- **Read-only cross-domain bridge**: when a service needs data owned by a domain it doesn't itself own (in this shared-DB phase), it gets its own trimmed-down copy of that model (only the fields it actually reads) with a repository interface extending `org.springframework.data.repository.Repository<T, ID>` (the bare marker interface — **not** `MongoRepository`/`CrudRepository`), declaring only the specific derived-query methods it calls. This is a compile-time guarantee against accidental writes to data the service doesn't own — a real bug caught during review (Wave 1 of the original extraction) was a bridge repository extending `MongoRepository` and inheriting `save`/`delete` it had no business having.
- **Real inter-service calls, once both sides exist**: when a genuine cross-service write or an ALUNO-scoped read is needed and the target service already exists as a real deployable (not just a shared-DB read), use a `RestClient`-based client class that forwards the caller's original `Authorization` header unchanged (never mints a new token — token issuance is `auth-service`'s job alone) and translates the callee's errors sensibly (`RestClientResponseException` → a local `IllegalArgumentException`/400; `ResourceAccessException`, i.e. the callee being unreachable, → `IllegalStateException`/422, not left to leak as a misleading blank error). First established in `notificacao-service`'s `FinanceiroServiceClient`, reused by `aluno-service`'s `AgendaServiceClient`/`FinanceiroServiceClient` for the portal migration.
- **Shared JWT secret, independent validation**: every service validates its own JWT independently (`JwtService` there only has `isValid`/`extractUserId`/`extractRole`, no `generate` — that's `auth-service`-only); the gateway does not validate JWT at all, it's a pure reverse proxy that forwards `Authorization` unchanged. This means any service's own `SecurityConfig` is the actual enforcement point, not the gateway.
- **Migration/build methodology**: each extraction was done by a pair of subagents in an isolated git worktree — one "builder" ports the code (reading the real monolith source first, verifying via `grep` before deleting anything from the monolith that another still-live flow might depend on) and self-verifies (`mvn clean verify`, a real `mvn spring-boot:run` smoke test against live MongoDB), then a fresh "reviewer" subagent (no context from the builder) re-verifies independently — re-running the tests itself rather than trusting the builder's report, with extra scrutiny on whatever's riskiest for that particular change. Only after PASS does the branch get committed and PR'd.

**Support patterns:**
- **Database-per-service**: each service keeps its own MongoDB database (e.g. `wellpag_aluno`, `wellpag_financeiro`) — planned, not real yet: all 7 modules currently still point at the same physical `wellpag_dev` MongoDB instance (transitional phase, see "Services & Ports").
- **API Gateway** — implemented (`backend/gateway/`, Spring Cloud Gateway MVC/servlet variant): single entry point for the frontend, routes by `Path` predicate. It does **not** validate the JWT before routing (each service validates its own independently) — this was a deliberate choice to avoid redundant validation, not a gap; revisit only if a cross-cutting concern (rate limiting, centralized auth) actually needs it.
- **Synchronous REST** for direct reads — e.g. `relatorio-service` calls `financeiro-service` and `aluno-service` to build the dashboard.
- **Asynchronous events** (RabbitMQ) — never implemented; was originally planned to decouple a bank-webhook flow (`webhook-service` → `pagamento-service` → `financeiro-service` → `notificacao-service`) that has since been abandoned entirely, both the webhook piece and the bank-integration service itself (see above). Not on the roadmap.

## CI

There is no CI workflow in this repository today. `.github/workflows/backend-ci.yml` used to run `mvn verify -B` against the old monolith at `backend/` — it was deleted along with the monolith, since it no longer pointed at anything real. Setting up CI for the 7 current `backend/*` modules (each its own `pom.xml`, needing a real MongoDB service container like the old workflow had) is open work, not yet done.

There is no cloud deployment target — the app runs on the owner's own machine (fixed IP), reachable directly without a PaaS.

## Agent skills

### Issue tracker

Issues live as GitHub Issues (`github.com/Montanini/Wellpag`), managed via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Domain docs

Single-context layout: `CONTEXT.md` + `docs/adr/` at the repo root (not yet created — read lazily/silently when present, per `docs/agents/domain.md`; the multi-service split under `backend/` doesn't get its own per-service `CONTEXT.md`). See `docs/agents/domain.md`.
