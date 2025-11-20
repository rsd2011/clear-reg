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
