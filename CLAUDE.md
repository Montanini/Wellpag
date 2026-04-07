# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Wellpag is a SaaS platform for autonomous teachers to manage students, schedules, and monthly fees. It is a full-stack monorepo with a Spring Boot backend and a Next.js frontend.

- **Backend**: Java 17 + Spring Boot 3.2.4, MongoDB, JWT + Google OAuth2
- **Frontend**: React 19 + Next.js 15 (App Router) + TypeScript + Tailwind CSS
- **Messaging**: WhatsApp via Evolution API
- **Deployment**: Railway (backend), Vercel (frontend)

## Commands

### Frontend (`frontend/`)
```bash
npm run dev       # Dev server on http://localhost:3000
npm run build     # Production build
npm run lint      # ESLint
```

### Backend (`backend/`)
```bash
mvn spring-boot:run               # Run locally
mvn verify -B                     # Build + run all tests
mvn clean package -DskipTests     # Build JAR without tests
```

### Local Infrastructure
```bash
docker-compose up -d   # Start MongoDB (27017) + Evolution API (8081)
```

## Environment Variables

**Backend** (required to run locally — use `application-local.yml` or env vars):
```
MONGODB_URI=mongodb://localhost:27017/wellpag
JWT_SECRET=<min 256-bit secret>
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
FRONTEND_URL=http://localhost:3000
WEBHOOK_BASE_URL=http://localhost:8080
EVOLUTION_API_URL=http://localhost:8081
EVOLUTION_API_KEY=...
```

**Frontend** (copy from `.env.local.example`):
```
NEXT_PUBLIC_API_URL=http://localhost:8080
```

## Architecture

### Backend (`backend/src/main/java/com/wellpag/`)

Layered Spring Boot application:
- `controller/` — 13 REST controllers; API docs available at `http://localhost:8080/swagger-ui`
- `service/` — Business logic. `LembreteScheduler` runs a cron job daily at 9am for WhatsApp reminders
- `model/` — MongoDB documents: `Usuario`, `Aluno`, `Horario`, `Mensalidade`, `ConfiguracaoWhatsApp`, `LembreteEnviado`, `BancoIntegracao`, `NotificacaoPagamento`
- `dto/` — 37+ request/response DTOs (separate from domain models)
- `repository/` — Spring Data MongoDB repositories
- `security/` — JWT filter, OAuth2 handlers, `JwtService`
- `config/` — Security config, CORS, exception handling
- `webhook/` — Bank payment webhook parsers
- `whatsapp/` — Evolution API HTTP client

**Key flows:**
- Auth: Google OAuth2 → `AuthController` → JWT issued to frontend
- Student onboarding: Teacher generates a link → student self-registers via `AlunoPortalController` → teacher completes data via `AlunoController`
- Payments: Bank webhook → `WebhookController` → CPF match → `MensalidadeService` marks fee as PAGO
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

## CI/CD

GitHub Actions workflow at `.github/workflows/backend-ci.yml` runs `mvn verify -B` on push/PR to `main`/`develop` for `backend/**` paths. Requires Java 17 and a local MongoDB 7 service spun up in the workflow.

Railway deployment uses the multi-stage `backend/Dockerfile` and health-checks `/actuator/health`.
