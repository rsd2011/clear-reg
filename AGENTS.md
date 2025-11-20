# clear-reg Project Instructions

## Stack & Tooling
- Multi-module Gradle build (`build.gradle`) shared across every backend subproject. Java toolchain is fixed to version 21, sources/javadoc jars are generated, and JaCoCo 0.8.11 is applied automatically with a **minimum 80% LINE coverage** gate on every module.
- Spring Boot 3.3.2 with dependency management stems from `gradle/libs.versions.toml`. Edit the catalog instead of hardcoding versions in module build files. Common bundles exist for Spring Boot core/security/test scopes.
- Lombok is used pervasively and wired as `compileOnly/annotationProcessor` in each module. Prefer Lombok annotations over hand-written getters/setters to stay aligned with the existing style.

## Module Layout
- `backend/platform`: shared domain/core utilities (e.g., `com.example.common.security.RowScope`) plus reusable test fixtures (`java-test-fixtures`). Consume these abstractions instead of duplicating infra code.
- `backend/auth`: authentication + SSO integration (LDAP/AD clients, JWT issuance, permission enforcement). Depends on `platform` and provides enums/entities such as `FeatureCode`, `ActionCode`, `PermissionGroup`.
- `backend/dw-integration`: DW ingestion hub that now owns HR plus future connectors (YAML-driven policies, JPA aggregates) backing both the API server and batch jobs.
- `backend/policy`: policy configuration & persistence layer for permission/data masking metadata.
- `backend/server`: the main Spring Boot API that composes `auth`, `dw-integration`, `policy`, and `platform`. Enable controllers/services here.
- `backend/batch-app`: Spring Boot batch entry point using `dw-integration` + `platform` for offline jobs.

## Coding & Testing Guidelines
- Keep packages under `com.example` and reuse shared types from `backend/platform` rather than redefining equivalents in feature modules.
- New permission/data-policy behaviors must declare `@RequirePermission` and leverage `AuthContextHolder`, `PermissionEvaluator`, and `RowScope` enforcement patterns already implemented in `backend/auth` (see `docs/permissions.md`).
- Sensitive DTO fields should continue to use `@Sensitive("TAG")` so `SensitiveDataMaskingModule` can mask/unmask automatically according to mask rules.
- Integration logic that needs organization-scoped filtering must consult `RowScope` and delegate to the relevant service helper (e.g., `DwOrganizationQueryService`) to guarantee repository filtering.
- Tests run on JUnit 5 with Spring test starters and (where needed) Testcontainers. Each module registers `testFixtures` from `backend/platform`; prefer using those fixtures over custom stubs. CI gates expect `./gradlew test jacocoTestCoverageVerification` to pass.

## Dependency & Configuration Management
- `gradle/libs.versions.toml` is the single source of truth for library and plugin versions (Spring Boot, SpringDoc, Jackson, JJWT, Testcontainers, etc.). When adding libs, declare them there and consume via aliases (e.g., `libs.spring.boot.starter.web`).
- Subprojects inherit caching/parallelization flags from `gradle.properties`. Avoid overriding those unless strictly necessary.

## Database & Migration Notes
- SQL migrations live under `docs/migrations/` (PostgreSQL) and already set expectations for permission-related schema objects (see `2024-09-01-permission-upgrade.sql`). Follow the same directory + naming convention (`YYYY-MM-DD-description.sql`) and include rollback instructions/comments inside each script.

## Documentation References
- `docs/permissions.md` captures the canonical authorization/data-policy workflow (Feature/Action enums, `PermissionGroup` assignments, row-scope filtering, masking tags). Review and update it whenever security-sensitive behavior changes to keep the doc in sync with the implementation.
