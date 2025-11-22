# 정책/마스킹 디버그 및 E2E 시나리오

## 1. 디버그 API
- 엔드포인트: `GET /api/admin/policies/effective`
- 파라미터: `featureCode`, `actionCode`(optional), `permGroupCode`(optional), `orgGroupCodes`(optional, 반복), `businessType`(optional)
- 응답: `PolicyToggleSettings` + `DataPolicyMatch`(rowScope/maskRule 등) 반환.
- 용도: UI에서 정책 토글/매치 실시간 확인, e2e 테스트에서 정책 적용 여부 검증.

### e2e 검증 예시
1) 정책이 없는 경우
   - 요청: `featureCode=NOTICE`, `actionCode=READ`
   - 기대: `dataPolicyMatch`가 `null` 이거나 `rowScope=ALL`, `maskRule` 기본값
2) 특정 permGroup/조직 그룹에 정책 등록 후
   - 요청: `featureCode=NOTICE`, `actionCode=READ`, `permGroupCode=PG_AUDITOR`
   - 기대: `dataPolicyMatch.rowScope=ORG`, `maskRule=PARTIAL`
3) secure-by-default 확인
   - 정책 미정의 엔드포인트 호출 전 디버그 API로 `maskRule` 기본값 확인 후, 마스킹 필드가 PARTIAL/HASH 등으로 응답되는지 UI 단에서 비교.

## 2. 공지·알림 이력 마스킹 점검
- 현재 시스템에는 별도 공지 이력 엔드포인트가 없으나, 향후 추가될 경우 다음을 준수:
  - 응답 DTO에 `UnaryOperator<String> masker` 오버로드 추가 후 `MaskingFunctions.masker(DataPolicyMatch)` 적용
  - 타임라인/이력 텍스트(내용, 제목, 메타데이터)에 PARTIAL/HASH/TOKENIZE 규칙 적용
  - 필수 필드(작성자, 조직 코드)는 마스킹하지 않되, 정책상 MASK_RULE=FULL 시 `[MASKED]` 처리

## 3. 캐시/secure-by-default e2e 체크
- 필터(`DataPolicyMaskingFilter`)는 정책 미정의 시 기본 ON/마스킹 활성화 → e2e에서 확인
  1) 정책 없음 상태에서 SENSITIVE API 호출 → reason 누락 시 400, 마스킹 기본 적용 확인
  2) 정책에 `maskRule=NONE` 설정 후 동일 호출 → 마스킹 해제 응답 비교
- 정책 캐시 무효화 테스트(필요 시): 정책 변경 → 디버그 API로 변경 반영 확인 → 동일 API 응답 마스킹 변화 확인

## 4. UI 적용 가이드(요약)
- 화면에서 민감 필드 표시 전 디버그 API를 호출하거나, 서버 응답의 마스킹 결과를 그대로 렌더링(서버가 결정)
- “마스킹 상태 배지” 표시: `maskRule` / `rowScope` 값을 함께 노출해 운영자가 즉시 확인 가능
- 정책 토글 화면에서 저장 직후 디버그 API를 호출해 반영 여부 e2e 단위테스트에 포함

이 문서는 테스트/운영팀이 정책·마스킹 상태를 빠르게 확인하고 e2e 시나리오를 작성하기 위한 참고용이다.
