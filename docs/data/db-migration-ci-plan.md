# CI-managed Database Migration Strategy

## 1. 목적
- PostgreSQL 스키마 변경을 CI 파이프라인에서 자동 검증하고, Rollback 가능한 상태로 배포하기 위한 전략을 정의한다.
- `docs/migrations/*.sql` 규칙(`YYYY-MM-DD-description.sql`)을 유지하되, Flyway/Liquibase 기반 실행 파이프라인을 추가한다.

## 2. 선택지 비교
| 항목 | Flyway | Liquibase |
| --- | --- | --- |
| 장점 | Gradle 플러그인 단순, SQL/Java 혼합 지원 | YAML/JSON 포맷, diff-based auto generation |
| 단점 | Rollback 스크립트 수동 | 러닝커브, light-weight SQL 실행 시 과함 |
| 결정 | **Flyway** (Gradle plugin + SQL 기반) |

## 3. 파이프라인 개요
1. `./gradlew flywayValidate` : CI에서 실행, checksum 검증.
2. `./gradlew flywayMigrate -Dflyway.url=jdbc:postgresql://ci-db ...` : Temp DB로 리허설.
3. `./gradlew flywayUndo` : Rollback 스크립트 검증 (Flyway Pro 기능 없을 시, `docs/migrations/*_rollback.sql` 실행 스크립트 작성).
4. GitHub Actions Workflow 예시
```
name: db-migration
on:
  pull_request:
    paths:
      - 'docs/migrations/**'
  push:
    branches: [main]
jobs:
  lint:
    steps:
      - run: ./scripts/check-migration-naming.sh
  validate:
    steps:
      - run: ./gradlew flywayValidate
  rehearsal:
    steps:
      - run: ./gradlew flywayMigrate -Dflyway.url=${{ secrets.CI_DB_URL }}
  rollback-check:
    steps:
      - run: ./scripts/run-rollback.sh docs/migrations/2024-*.sql
```

## 4. 운영 플로우
1. 개발자는 `docs/migrations/2024-xx-yy-feature.sql` 과 `...-rollback.sql`을 작성.
2. PR 에서 CI가 Validate/Rehearsal/rollback 체크 수행.
3. Merge 시 CD 파이프라인이 Flyway migrate를 staging → prod 순으로 실행.
4. 장애 시 `flywayUndo` 또는 rollback 스크립트를 적용 후, `docs/migrations/rollback-log.md` 업데이트.

## 5. 환경 구성
- CI DB: docker-compose (Postgres) 혹은 RDS snapshot 복제본.
- App config: `backend/*/src/main/resources/db/flyway.conf` + Gradle task 공유.
- Secret 관리: GitHub OIDC + AWS Secrets Manager.

## 6. 관측/감사
- Flyway Schema History 테이블(`flyway_schema_history`)을 CloudWatch/Prometheus exporter로 노출.
- Migration ID + commit hash 기록 (`docs/migrations/migration-index.json`).
- 실패 알람: GitHub Actions -> Slack.

## 7. 단계별 TODO
- [ ] Flyway Gradle plugin 추가 및 기본 config 공유.
- [ ] CI workflow(`.github/workflows/db-migration.yml`) 작성.
- [ ] Rollback 스크립트 템플릿/검증 스크립트 추가.
- [ ] 운영/스테이징 DB 접근 권한 자동화 (IAM Role/SSM).
- [ ] `docs/migrations/rollback-log.md` 생성.
