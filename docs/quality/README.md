# Quality Guide

## Testing Guidelines

# 테스트 코드 컨벤션

## `@DisplayName` 표준
- 모든 단위/통합 테스트 클래스와 테스트 메서드는 `@DisplayName`을 명시한다.
- 설명은 **한국어로** 작성하며, 테스트 시나리오를 “Given/When/Then” 형태로 짧게 표현한다.
  - 예) `@DisplayName("Given 관리자 When 계정을 잠금 해제하면 Then 정상 처리된다")`
- 파라미터화된 테스트나 동적 테스트도 동일하게 한국어 설명을 제공한다.
- 신규 테스트를 작성할 때는 IDE 템플릿 또는 팀 기준 템플릿을 활용해 `@DisplayName` 누락을 방지한다.

## 적용 범위 및 우선순위
1. **`backend/server` 모듈**
   - API·서비스 테스트 수가 많고, QA/리뷰 빈도가 가장 높으므로 최우선으로 정비한다.
   - 컨트롤러/서비스 테스트부터 `@DisplayName`을 적용하고, 이후 Repository/Component 순으로 확장한다.
2. **`backend/auth` 모듈**
   - 권한/인증 로직은 보안 관점에서 중요하므로 두 번째 단계에서 적용한다.
   - 기존 `AuthServiceTests`, `PermissionEvaluatorTest`, `RequirePermissionAspectTest` 등을 우선 업데이트한다.
3. **기타 모듈(`draft`, `policy`, `dw-integration`, `batch-app`)**
   - 상위 모듈 적용이 끝나면 나머지 모듈을 순차적으로 정비한다.
   - 새로 추가되는 테스트는 즉시 컨벤션을 따라야 하며, 기존 테스트는 리팩터링 시 함께 반영한다.
   - Draft/Approval 모듈 백로그(T13) 진행 시 상태 전이/동시성/권한/RowScope/알림(outbox) 통합 테스트를 우선 추가한다 (`docs/draft-approval-todo.md` 참조).

## 운영 가이드
- PR 템플릿에 “테스트 `@DisplayName` 한글 적용 여부” 체크박스를 추가하면 누락을 예방할 수 있다.
- 코드 리뷰에서 `@DisplayName` 누락이 발견되면 반드시 수정하도록 가이드한다.
- IDE 검사 또는 정적 분석(예: ArchUnit, custom lint)을 통해 `@DisplayName`이 없는 테스트를 탐지하는 것도 고려한다.
\n---\n
## CI Pipeline

# CI/CD 가이드

본 문서는 `clear-reg` 프로젝트의 최소 CI 파이프라인 구성을 정의한다. GitHub Actions 기준으로 작성되었으나 Jenkins 등 다른 CI에도 동일 단계 적용 가능하다.

## 필수 빌드 단계
1. **Checkout + JDK 21 설치**
2. **Gradle 캐시 설정** (`~/.gradle/caches`)
3. **정적 검사**: `./gradlew checkstyleMain` (향후 추가 예정)
4. **테스트 + 커버리지**: `./gradlew test jacocoTestCoverageVerification`
5. **아티팩트/리포트 업로드**: `build/reports/tests`, `build/reports/jacoco/test/html`

## GitHub Actions 워크플로 예시
```yaml
name: CI

on:
  pull_request:
    branches: [ "main" ]
  push:
    branches: [ "main" ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Cache Gradle
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
      - name: Build & Test
        run: ./gradlew test jacocoTestCoverageVerification --stacktrace
      - name: Publish Test Report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-reports
          path: '**/build/reports/tests/test'
      - name: Publish Coverage Report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: jacoco-report
          path: '**/build/reports/jacoco/test/html'
```

## 추가 권장 사항
- **Branch Protection**: `main` 브랜치에 대해 위 Workflow 성공 + Code Owners 승인 필수.
- **Dependency Cache**: Gradle remote cache (e.g., Build Cache node) 도입 검토.
- **Security Checks**: 추후 `./gradlew dependencyCheckAnalyze` or Snyk 스캔 추가.
- **Deployment**: CI 성공 시 staging 배포 자동화 (ArgoCD/Helm hook).

CI 관련 변경이나 파이프라인 고도화 시 본 문서를 업데이트하고, PR 템플릿에 CI 로그 링크를 첨부하도록 유지한다.
\n---\n
## Coverage Targets

세부 임계와 모듈 현황은 docs/quality/coverage-plan.md 를 참조.
