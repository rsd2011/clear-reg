# Jacoco 커버리지 정렬 계획 (2025-11-22 기준)

## 목표
- 전역 BUNDLE 최소 라인 90%, 브랜치 80% 달성(현재 적용)
- CLASS/PACKAGE 룰 활성 상태 유지, 필요 시 테스트 보강 후 exclude 최소화
- 테스트는 JUnit 5 + Spring 슬라이스 중심, BDD 스타일과 한국어 `@DisplayName` 유지

## 현황 요약 (2025-11-22 전체 재검증 결과)
- `./gradlew test jacocoTestCoverageVerification` 전체 green. 전역 BUNDLE 90/80, CLASS 0.80, PACKAGE 0.75 적용 중.
- 모든 모듈(server, draft, file-core, policy, platform, auth, dw-ingestion-core, dw-gateway-client, dw-gateway, dw-integration, batch-app, dw-worker) 전역 임계 충족.
- auth: PermissionEvaluator/AccountStatusPolicy/SensitiveSerializer 분기 추가 후 전역 임계로 복귀(별도 임시 임계 해제).
- dw-gateway: FileController 단위 분기(목록 빈값, 스토리지 예외, 삭제 실패, contentType null, originalName null) + DwOrganizationRecord 값 보존 테스트로 브랜치 0.50→0.75 달성.

## 최근 추가 테스트 (2025-11-22)
- auth
  - PermissionEvaluator 체인/기본그룹 null/없음 분기, PermissionChecks null 처리, ActionCode isDataFetch 추가 분기.
  - AccountStatusPolicy 락 on/off + passwordHistory on/off + inactive 계정 분기, AuthContextPropagator 컨텍스트 복원.
  - SensitiveSerializerModifier 마스킹 null/same/다름 + null serializer 경로.
- dw-gateway
  - FileController 단위: 목록 빈 배열, 스토리지 예외 500, 삭제 실패, contentType null 기본값, originalName null→attachment.
  - DwOrganizationRecord 값 보존 테스트.
- 기타: ActionCode 추가 분기(true/false)로 브랜치 확보.

## 다음 단계 / Roadmap
- 단기: PACKAGE exclude(commoncode/cache 등) 점진 축소, 전역 임계 90/80 유지.
- 필요 시 CLASS/PACKAGE를 더 엄격히(예: BRANCH 0.80↑) 상향 여부 검토 – 현재는 유지.

## Jacoco 설정 메모
- 전역 exclude: `**/*Application*.class`, `**/config/**`, `**/dto/**`
- 전역 BUNDLE: 라인 0.90 / 브랜치 0.80
- CLASS 룰: on (라인 ≥0.80), DTO/Request/Response 등 기본 exclude만 유지
- PACKAGE 룰: on (브랜치 ≥0.75), `com.example.common.jpa*`, `com.example.server.cache*`, `com.example.server.commoncode*` 임시 exclude

## 모듈 체크리스트
- [x] server — BUNDLE 0.90/0.80, CLASS on. commoncode PACKAGE exclude는 추후 제거 예정.
- [x] file-core — 업로드/삭제 실패, 다운로드 허용 경로, AuditRelay 쿼리 실패 분기 보강.
- [x] policy — DatabasePolicySettingsProvider/PolicyDocument 보강 완료.
- [x] auth — DeclarativePermissionConfiguration·PasswordPolicyValidator 분기 보강.
- [x] draft — TemplateAdminService 리스트, DTO 보존, 노티/감사 퍼블리셔 스모크.
- [x] platform — RowScope/파일 DTO 경계 테스트 완료.
- [x] dw-ingestion-core — queue 데드레터/재시도, parser/validator 경계, Kafka publisher send 예외 분기.
- [x] dw-gateway-client — FileManagementPortClient 500/헤더 분기 및 null 응답 처리, Properties 헤더 테스트 추가.
- [x] dw-gateway — MockMvc 분기 커버로 통과.
- [x] dw-integration — outbox 재시도/데드레터, import batch 전이, ingestion properties 테스트.
- [x] batch-app, dw-worker — 고브랜치 유지.

## 임계 상향/유지 메모
- 현재: BUNDLE 90/80, CLASS on, PACKAGE on(브랜치 0.75). 전체 green.
- 향후: PACKAGE exclude 축소 → 필요 시 브랜치 기준 상향 검토.
