# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

### Backend (Spring Boot)
```bash
# Build project
mvn clean package

# Run application
mvn spring-boot:run

# Run specific tests
mvn test -Dtest="fan.summer.hmoneta.service.unifi.*"

# Run with dev profile (disables DnsUpdateTask scheduled job)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Frontend (Vue 3/Vite)
```bash
cd HMfront/hm-front

# Install dependencies (use Yarn 4.x, not npm)
yarn install

# Development mode (port 3000)
yarn dev

# Production build
yarn build

# Lint and fix
yarn lint
```

## Project Structure

```
HMoneta/
├── src/main/java/fan/summer/hmoneta/
│   ├── controller/          # REST API endpoints (acme/, dns/, log/, network/, plugin/, task/, unifi/, user/, waf/)
│   ├── service/            # Business logic (acme/, dns/, log/, network/, plugin/, unifi/, user/, waf/)
│   ├── database/           # JPA entities and repositories (entity/, repository/)
│   ├── task/               # Scheduled tasks (dns/DnsUpdateTask, acme/AcmeUpdateTask)
│   ├── util/               # Utilities (JwtUtil, Md5Util, IpUtil, WebApiUtil, WebUtil)
│   ├── websocket/          # WebSocket handlers
│   └── common/             # AOP aspects, configs, interceptors, enums, exceptions
├── HMfront/hm-front/       # Vue 3 frontend
│   ├── src/pages/          # Page components (dns.vue, network.vue, logPage.vue, etc.)
│   ├── src/stores/         # Pinia stores (app.js exports useUserStore, userNotificationStore)
│   ├── src/router/         # Vue Router with auto-routes
│   ├── src/layouts/        # vite-plugin-vue-layouts layouts
│   └── src/common/         # request.js (HTTP client with HMToken header)
├── plugins/                # PF4J plugin directory
└── certs/                  # ACME certificate storage
```

## Architecture Overview

### Backend Architecture

**API Prefix**: All endpoints use `/hm` prefix (e.g., `/hm/dns/query_all`, `/hm/user/login`)

**Layered Structure**:
- `controller/` - REST endpoints
- `service/` - Business logic
- `database/` - JPA entities (under `entity/`) and repositories (under `repository/`)
- `common/` - Cross-cutting concerns

### Authentication System

- **JWT-based** with MD5+Salt password hashing
- `JwtUtil` - Token generation/validation with `@JwtExclude` annotation for sensitive fields
- `ApiInterceptor` - Validates tokens on protected endpoints
- Token sent via `HMToken` header (not `Authorization`)
- Protected endpoints exclude: `/hm/user/login`, `/hm/user/valid`, `/hm/user/login/status`

### Plugin System (PF4J)

- DNS providers loaded dynamically via PF4J
- Plugin interface: `fan.summer.HmDnsProviderPlugin` (published to Maven Local as `fan.summer:HMoneta-Official-Plugin-Api:0.1.0`)
- Plugins directory: `plugins/`
- `PluginService` manages plugin lifecycle (load, unload, start, stop)

### Scheduled Tasks

**Task Management System**:
- `TaskScheduleService` - Centralized task scheduler that manages all tasks dynamically
- `@ScheduledTask` annotation marks task classes with metadata (name, description, defaultCron, methodName)
- Tasks auto-register on application startup via `ApplicationListener<ApplicationReadyEvent>`
- `TaskConfigEntity` persists task configuration (enabled, custom cron) to database
- `TaskController` exposes REST APIs at `/hm/task/*`

**Task REST APIs**:
- `GET /hm/task/list` - List all registered tasks with status and next execution time
- `POST /hm/task/trigger/{taskName}` - Manually trigger a task
- `PUT /hm/task/cron` - Update task cron expression (persisted to DB)
- `PUT /hm/task/enabled` - Enable/disable a task (persisted to DB)

**Default Tasks**:
- `DnsUpdateTask` - DDNS update (`0 0/10 * * * ?` = every 10 minutes)
- `AcmeUpdateTask` - ACME certificate renewal check (`0 0 0 * * ?` = daily at midnight), renews certs expiring within 30 days

**Frontend**: `taskManager.vue` page for task management UI

### ACME Certificate Management

- Uses `acme4j-client:3.5.0` for Let's Encrypt integration
- DNS-01 challenge for domain validation
- `@Async` operations with `AcmeAsyncLogEntity` tracking
- `AcmeTaskContext` manages task context across async boundaries
- Retry logic: login retry (max 3), DNS propagation verification (max 10 attempts, 10s interval)
- Certificate files stored in `certs/{domain}/` directory
- ZIP download with .key, .crt, .pem, .fullchain.pem formats

### WebSocket Log Push

- `DbLogAppender` queues logs to `BlockingQueue` (capacity: 10000)
- Frontend connects to `/ws/logs` for real-time log streaming
- `userNotificationStore.showError()` / `showSuccess()` for snackbar notifications

### UniFi Network Integration

- Uses Spring WebFlux `WebClient` via `WebApiUtil` for async HTTP
- Supports local and remote API authorization modes
- `unifiIsAccessible()` checks connectivity before operations
- DTOs under `service/unifi/dto/SiteManager/`

### LeiChi WAF Integration

- `LeiChiWafService` manages SSL certificates and sites via REST API
- `NetworkService` correlates UniFi clients with LeiChi WAF sites
- `LeiChiWafAspect` provides AOP logging for WAF API calls

### Frontend Architecture

**State Management (Pinia)**:
- `useUserStore` - Auth state with `login(token)`, `logout()`, `checkAuth()` via `/hm/user/valid`
- `userNotificationStore` - Global snackbar via `showError(msg)`, `showSuccess(msg)`

**Routing**:
- `unplugin-vue-router` for file-based routing
- Public routes: `/login`, `/register`, `/forgot-password`
- Default route `/` redirects to `/dns`

**Request Handling**:
- `src/common/request.js` wraps fetch with `HMToken` header auto-injection
- Blob/arraybuffer response types supported

**VUETIFY THEMES**: `hmonetaDark` and `hmonetaLight` (green primary `#10b981`)

## Configuration

**Backend**: `application.yml` with Spring Profiles
- `application-dev.yml` - Dev profile (disables DnsUpdateTask)
- Environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `jwt.secret`, `jwt.expiration`

**Frontend**: Environment files in `HMfront/hm-front/`:
- `.env.development`: `VITE_API_BASE_URL=http://localhost:8080/hm`, `VITE_WS_BASE_URL=ws://localhost:8080/ws/logs`
- `.env.production`: `VITE_API_BASE_URL=http://10.5.20.80:8080/hm`, `VITE_WS_BASE_URL=http://10.5.20.80:8080/ws/logs`

## Key Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 4.0.5 | Framework |
| Java | 25 | Runtime |
| acme4j-client | 3.5.0 | ACME protocol |
| dnsjava | 3.6.3 | DNS operations |
| pf4j-spring | 0.10.0 | Plugin framework |
| jjwt-api | 0.12.3 | JWT handling |
| hypersistence-utils | 3.14.1 | PostgreSQL enum support |
| Vue | 3.5.22 | Frontend |
| Vuetify | 3.12.0 | UI components |
| Pinia | 3.0.3 | State management |
