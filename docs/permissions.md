# 권한 및 데이터 정책 가이드

## 전반 구조

- `FeatureCode`/`ActionCode`는 `backend/auth` 모듈에서 Java Enum으로 관리된다.
- `PermissionGroup` 엔티티는 Feature+Action 조합과 RowScope, FieldMaskRule을 보유한다.
- 모든 사용자는 `UserAccount.permissionGroupCode`와 `organizationCode`를 가진다.
- `OrganizationPolicy` 는 조직별 기본 권한그룹과 결재선 PermissionGroup 목록을 유지한다.

## 접근 제어

- 컨트롤러나 서비스 메서드에 `@RequirePermission(feature, action)`을 선언한다.
- Aspect가 실행돼 `PermissionEvaluator` → `PermissionGroupService` → `UserAccountService` 순으로 권한을 검증한다.
- 성공 시 `AuthContextHolder`에 ThreadLocal로 Feature/Action/RowScope/MaskRule 정보를 저장하고 감사 로그를 남긴다.
- 실패 시 `PermissionDeniedException`을 발생시키고 감사 로그를 WARN 레벨로 남긴다.

## 데이터 정책

- DTO/Record 필드는 `@Sensitive("TAG")`로 마킹한다. 예: `@Sensitive("ORG_NAME") String name`.
- `SensitiveDataMaskingModule`이 Jackson SerializerModifier로 등록되어 `DataPolicyEvaluator`를 통해 마스킹을 일괄 수행한다.
- 현재 요청의 `AuthContext` Action이 `FieldMaskRule.requiredAction`을 만족하지 못하면 기본 마스킹 문자열(`***`) 혹은 Rule별 mask 값을 반환한다.
- Action이 `UNMASK`이거나 Rule이 허용할 경우 원문을 그대로 직렬화한다. 감사 플래그가 참이면 허용/차단 이벤트를 로그로 남긴다.

## Row Scope

- `RowScope`는 `OWN`, `ORG`, `ALL`, `CUSTOM`을 지원하며 `com.example.common.security`에 공통 Enum으로 정의한다.
- 서비스 계층(`HrOrganizationQueryService`)은 RowScope와 사용자 조직코드를 입력받아 Repository 조건을 강제한다. 누락되면 실패로 간주한다.
- `CUSTOM` 스코프는 `OrganizationRowScopeStrategy` 빈을 통해 행 필터 전략을 주입해 확장한다.

## 예시 적용 (HR 조직 조회)

1. `HrOrganizationController.organizations`에 `@RequirePermission(feature = FeatureCode.ORGANIZATION, action = ActionCode.READ)` 선언.
2. Aspect가 권한 검사 후 `AuthContext`를 설정.
3. 컨트롤러에서 `AuthContextHolder.current()` 값을 가져와 RowScope와 조직코드를 `HrOrganizationQueryService`에 전달.
4. Service는 Scope에 따라 Repository 메서드를 호출하여 행 단위 필터링을 수행.
5. DTO `HrOrganizationResponse.name` 필드는 `@Sensitive("ORG_NAME")`로 태깅되어 권한에 따라 마스킹된다.

## 확장 포인트

- `PermissionGroup.maskRules` 컬렉션에 필요한 태그, UNMASK 요구 여부, 감사 플래그를 저장.
- `OrganizationPolicyService`를 통해 조직 온보딩 시 기본 PermissionGroup 및 결재선 템플릿을 적용.
- Repository 계층에서 `RowScope.CUSTOM`일 때 별도 전략 빈을 주입해 세부 비즈니스 룰을 삽입할 수 있다.
- 메뉴 API(`/api/menus/me`) 구현 시 PermissionGroup 기반으로 접근 가능한 메뉴만 구성하도록 동일한 RequirePermission/RowScope 컨텍스트를 활용한다.
