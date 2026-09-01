# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Wellpag is a SaaS platform for autonomous teachers to manage students, schedules, and monthly fees. It is a full-stack monorepo with a Spring Boot backend and a Next.js frontend.

- **Backend**: Java 17 + Spring Boot 3.2.4, MongoDB, JWT + Google OAuth2
- **Frontend**: React 19 + Next.js 15 (App Router) + TypeScript + Tailwind CSS
- **Messaging**: WhatsApp via Evolution API
- **Deployment**: self-hosted on the owner's local machine (fixed IP), no cloud PaaS — intended to run there until traffic outgrows it

> **Architecture status**: the codebase today is a single Spring Boot monolith (described in "Current Architecture" below). A target microservices architecture is documented under "Target Architecture" for future migration — it is a design only, not yet implemented. Do not assume service boundaries from that section exist in the code.

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

The backend runs as three Spring profiles, all intended to run side by side on the same physical machine (the owner's, fixed IP — see Project Overview):

| Profile | Config file | Port | MongoDB database | Secrets |
|---|---|---|---|---|
| `dev` | `application-dev.yml` | 8080 | `wellpag_dev` | Fake, hardcoded in the file — safe to commit |
| `teste` | `application-teste.yml` | 8090 | `wellpag_teste` | Fake, hardcoded in the file — safe to commit |
| `prod` | `application-prod.yml` | 8082 (or `SERVER_PORT`) | via `MONGODB_URI` | Real, **only** from env vars — the app fails to start if one is missing |

All three share the same `docker-compose` MongoDB container (27017) and Evolution API (8081) — they're separated by database name and port, not by infrastructure. `EVOLUTION_API_URL`/`EVOLUTION_API_KEY` and Google OAuth credentials are common across profiles and still come from `application.yml`'s env var placeholders (see below); only JWT secret, frontend URL and webhook base URL are profile-specific.

Select a profile with `-Dspring-boot.run.profiles=<dev|teste|prod>` (see Commands) or `SPRING_PROFILES_ACTIVE=<profile>`.

## Environment Variables

**Backend** — shared across all profiles, read by `application.yml`:
```
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
EVOLUTION_API_URL=http://localhost:8081
EVOLUTION_API_KEY=...
```

**Backend** — only required for the `prod` profile (no defaults, the app won't start without them):
```
MONGODB_URI=mongodb://<host>:27017/wellpag_prod
JWT_SECRET=<min 256-bit secret>
FRONTEND_URL=https://...
WEBHOOK_BASE_URL=https://...
SERVER_PORT=8082   # optional, defaults to 8082
```

`dev` and `teste` need none of the above beyond the shared ones — their Mongo URI, JWT secret, frontend URL and webhook base URL are hardcoded (fake values) in `application-dev.yml`/`application-teste.yml`.

Bank integration credentials (Banco Inter OAuth2 client id/secret, mTLS certificate/key, PIX key) are **not** environment variables in any profile — they are configured per-professor at runtime and stored in the `banco_configuracao_inter` MongoDB collection via `BancoController`.

**Frontend** (copy from `.env.local.example`):
```
NEXT_PUBLIC_API_URL=http://localhost:8080
```

## Current Architecture (Monolith)

### Backend (`backend/src/main/java/com/wellpag/`)

Layered Spring Boot application:
- `controller/` — 11 REST controllers; API docs available at `http://localhost:8080/swagger-ui`
- `service/` — Business logic. `LembreteScheduler` runs a cron job daily at 9am for WhatsApp reminders
- `model/` — MongoDB documents: `Usuario`, `Aluno`, `Horario`, `Mensalidade`, `ConfiguracaoWhatsApp`, `LembreteEnviado`, `BancoConfiguracaoInter`, `NotificacaoPagamento`. Also includes enums (`BancoIntegracao`, `Role`, `DiaSemana`, `TipoHorario`, `StatusMensalidade`, `StatusNotificacao`, `AuthProvider`) — `BancoIntegracao` identifies which bank parser to use, it is not a persisted document
- `dto/` — 23 request/response DTOs (separate from domain models)
- `repository/` — Spring Data MongoDB repositories
- `security/` — JWT filter, OAuth2 handlers, `JwtService`
- `config/` — Security config, CORS, exception handling
- `webhook/` — Bank payment webhook parsers, one per format (`InterParser`, `AsaasParser`, `PixGenericoParser`, `GenericoParser`) behind the `BancoParser` interface
- `whatsapp/` — Evolution API HTTP client

**Key flows:**
- Auth: Google OAuth2 → `AuthController` → JWT issued to frontend
- Student onboarding: Teacher generates a link → student self-registers via `AlunoPortalController` → teacher completes data via `AlunoController`
- Payments (generic): Bank webhook → `WebhookController` → parser selected by `BancoIntegracao` → CPF match → `MensalidadeService` marks fee as PAGO
- Payments (Banco Inter): professor registers OAuth2 credentials + mTLS certificate/key via `BancoController` → `BancoInterService` registers the webhook with the Inter API → Inter calls back through the same generic webhook flow, parsed by `InterParser`
- Dashboard: `DashboardController` returns students grouped by the current time slot (`Horario`)

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
- `Aluno` has a `professorId` (ref to `Usuario`) and a `cpf` used for automated payment matching
- `Horario` belongs to a professor, has `DiaSemana` + start/end time + type (`FIXO`/`AVULSO`)
- `Mensalidade` tracks monthly fee per student with status: `A_PAGAR`, `PAGO`, `ATRASADO`
- `LembreteEnviado` prevents duplicate WhatsApp reminders per student per period
- `BancoConfiguracaoInter` holds one professor's Banco Inter OAuth2 + mTLS credentials and webhook registration state

## Target Architecture — Microservices (planned, design-only)

Goal: split the monolith above into independently deployable services, keeping MongoDB (database-per-service, no relational migration). This is a learning/portfolio-driven redesign, not a response to a current scaling problem — treat it as a future direction, and migrate incrementally (strangler pattern: extract one service at a time behind the gateway, starting with a low-risk one such as `relatorio-service`) rather than a rewrite.

| Service | Responsibility | Sourced from (current monolith) |
|---|---|---|
| `auth-service` | Login, JWT issuance, Google OAuth2, user registration | `AuthController/Service`, `security/`, `Usuario` |
| `aluno-service` | Student CRUD, self-registration portal | `AlunoController/Service`, `AlunoPortalController/Service`, `Aluno` |
| `agenda-service` | Class schedules (fixed/one-off) | `HorarioController/Service`, `Horario` |
| `financeiro-service` | Monthly fees and their status (`A_PAGAR`/`PAGO`/`ATRASADO`) | `MensalidadeController/Service`, `Mensalidade` |
| `webhook-service` | Receives each bank's webhook (Inter, Asaas, generic PIX) on its own endpoint, validates mTLS/signature, parses the payload into a normalized payload | `WebhookController`, `InterParser`, `AsaasParser`, `PixGenericoParser`, `GenericoParser` |
| `pagamento-service` | Owns per-bank integration credentials (Inter OAuth2 + mTLS config), matches normalized payments to a student by CPF, decides when a fee is settled | `BancoController`, `BancoInterService`, `BancoConfiguracaoInter`, `BancoIntegracao` |
| `notificacao-service` | WhatsApp reminders + payment notifications | `NotificacaoController/Service`, `WhatsAppController/Service`, `LembreteScheduler`, `EvolutionApiClient` |
| `relatorio-service` | Dashboard and financial reports (read-only aggregation) | `DashboardController/Service`, `RelatorioController/Service` |

> `relatorio-service`'s dashboard also reads `Horario` (agenda-service), not just aluno/financeiro data — the table above only lists controller/service origin, not every read dependency, and that omission is a real source of coupling worth calling out explicitly.

**Support patterns:**
- **Database-per-service**: each service keeps its own MongoDB database (e.g. `wellpag_aluno`, `wellpag_financeiro`) — no cross-service collection access.
- **API Gateway** (e.g. Spring Cloud Gateway): single entry point for the frontend, routes by path prefix, validates the JWT before routing.
- **Synchronous REST** for direct reads — e.g. `relatorio-service` calls `financeiro-service` and `aluno-service` to build the dashboard.
- **Asynchronous events** (RabbitMQ) for flows currently coupled in-process in the monolith:
  1. Bank calls `webhook-service` (bank-specific endpoint, e.g. `/webhook/inter`).
  2. `webhook-service` validates and parses → publishes `PagamentoRecebido`.
  3. `pagamento-service` consumes it, matches the student by CPF against `aluno-service`.
  4. Publishes `MensalidadeQuitada` → `financeiro-service` updates the fee status → `notificacao-service` consumes it and sends the WhatsApp confirmation.
- **Local orchestration**: `docker-compose` extended to run all services + MongoDB + RabbitMQ, replacing today's single-container setup.

## CI

GitHub Actions workflow at `.github/workflows/backend-ci.yml` runs `mvn verify -B` on push/PR to `main`/`develop` for `backend/**` paths. Requires Java 17 and a local MongoDB 7 service spun up in the workflow. This pipeline covers the current monolith; a microservices migration would need a per-service pipeline and image.

There is no cloud deployment target — the app runs on the owner's own machine (fixed IP), reachable directly without a PaaS. The multi-stage `backend/Dockerfile` (health-checks `/actuator/health`) can still be used to run it locally under Docker if preferred over `mvn spring-boot:run`.
