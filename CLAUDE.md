# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Full build with tests and coverage verification
./gradlew test jacocoTestCoverageVerification

# Run tests only (single module)
./gradlew :backend:server:test
./gradlew :backend:auth:test

# Run specific test class
./gradlew :backend:server:test --tests "com.example.server.SomeTest"

# Build without tests
./gradlew build -x test

# Static analysis (SpotBugs, Checkstyle, PMD - enabled for strictModules)
./gradlew check

# SonarQube analysis (CI only, requires SONAR_TOKEN)
./gradlew sonarqube
```

## Project Architecture

### Multi-Module Gradle Build
- **Java 21** toolchain with JaCoCo 0.8.11 (80% line, 75% branch coverage gates)
- Dependencies managed via `gradle/libs.versions.toml` - always add new libraries there
- Spring Boot 3.3.2 with Spring Security, JPA, Kafka, Redis

### Module Hierarchy

```
backend/
├── platform/      # Core domain utilities, shared types, test fixtures (java-test-fixtures)
├── auth/          # Authentication, JWT, SSO (LDAP/AD), permission enums (FeatureCode, ActionCode)
├── policy/        # Policy configuration, YAML-driven permission/masking rules
├── draft/         # Draft/approval domain logic, state machine
├── approval/      # Approval workflow engine, delegation, defer/hold actions
├── audit/         # Audit logging, SIEM integration, hash-chain verification
├── file-core/     # File storage abstraction, MIME validation (Tika)
├── server/        # Main Spring Boot API (composes all modules above)
├── batch/         # Spring Batch entry point for scheduled jobs
├── dw-gateway/    # DW query API gateway service
├── dw-gateway-api/# API contracts for DW gateway
├── dw-gateway-client/ # Client library for DW gateway consumers
├── dw-worker/     # Background DW ingestion worker
├── dw-ingestion-core/ # Core DW ingestion logic
└── data-integration/  # HR and external data connectors
```

### Key Design Patterns

**Permission System** (`backend/auth`, `backend/policy`):
- `@RequirePermission(feature, action)` annotation triggers AOP-based authorization
- `PermissionEvaluator` → `PermissionGroupService` → `AuthContextHolder` (ThreadLocal)
- `RowScope` (OWN/ORG/ALL/CUSTOM) for row-level filtering via `RowScopeSpecifications`
- `@Sensitive("TAG")` + `SensitiveDataMaskingModule` for field-level masking

**Declarative Permissions** (`permission-groups.yml`):
- SpEL conditions for dynamic permission evaluation
- Configure via `security.permission.declarative.location`

**Async/Batch Context**:
- `AuthContextPropagator` / `AuthContextTaskDecorator` for thread context propagation
- Batch jobs use `DwBatchAuthContext.systemContext()`

### Entry Points
- **API Server**: `backend/server` → `com.example.server.Application`
- **Batch**: `backend/batch` → `com.example.batch.BatchApplication`
- **DW Gateway**: `backend/dw-gateway` → `com.example.dwgateway.DwGatewayApplication`
- **DW Worker**: `backend/dw-worker` → `com.example.dwworker.DwWorkerApplication`

## Testing Conventions

- Use `@DisplayName` with Korean Given/When/Then format
- Consume test fixtures from `backend/platform` (java-test-fixtures)
- Testcontainers for integration tests
- Tests disable scheduling: `spring.task.scheduling.enabled=false`

## Database Migrations

- SQL migrations in `docs/migrations/` (PostgreSQL)
- Naming: `YYYY-MM-DD-description.sql` with rollback comments

## Documentation Structure

- `docs/security/permissions/` - Authorization, RowScope, masking
- `docs/architecture/` - Bounded contexts, module decoupling
- `docs/drafts/` - Draft/approval API specs
- `docs/audit/` - Audit module design, E2E tests
- `docs/operations/` - Observability, SIEM, alerting
