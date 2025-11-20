# 권한 및 데이터 정책 가이드

## 전반 구조

- `FeatureCode`/`ActionCode`는 `backend/auth` 모듈에서 Java Enum으로 관리된다.
- `PermissionGroup` 엔티티는 Feature+Action 조합과 RowScope, FieldMaskRule을 보유한다.
- 모든 사용자는 `UserAccount.permissionGroupCode`와 `organizationCode`를 가진다.
- `OrganizationPolicy` 는 조직별 기본 권한그룹과 결재선 PermissionGroup 목록을 유지한다.
- 권한 그룹은 `backend/auth/src/main/resources/permission-groups.yml`에 선언형으로 기술할 수 있으며
  애플리케이션 기동 시 `security.permission.declarative.location` 위치에서 자동 반영된다.
- 공용 파일 관리 서비스(`FeatureCode.FILE`)는 업로드/다운로드/삭제 권한을 중앙화해 모든 모듈이
  동일한 첨부 정책·감사 로직을 사용한다.
- 통합 기안 시스템은 `FeatureCode.DRAFT`와 전용 Action(`DRAFT_CREATE`, `DRAFT_SUBMIT`, `DRAFT_APPROVE`,
  `DRAFT_READ`, `DRAFT_CANCEL`, `DRAFT_AUDIT`)을 사용한다. 실제 업무별 기안 작성 권한은 해당 업무 Feature
  (예: `NOTICE`)에 `DRAFT_CREATE`를 매핑하여 제어하고, 공통 기능(조회/감사)은 `FeatureCode.DRAFT`에 매핑한다.
  - 예정된 확장(Action 추가): `DRAFT_WITHDRAW`, `DRAFT_RESUBMIT`, `DRAFT_DELEGATE`, `DRAFT_REFERENCE_VIEW` (see `docs/draft-approval-todo.md` T5/T14).
  - 결재자 검증: ApprovalGroupMember 기반 단계별 승인/반려 권한 검증을 추가(T4).
  - 참조자 권한: DraftReference로 열람/알림만 허용, 수정/결재 권한은 부여하지 않음(T2/T14).
  - RowScope 확장: 작성자/결재자/참조자 스코프 필터를 지원하도록 list API 스펙을 확장(T14/T15).
- 공통 코드 서비스(`FeatureCode.COMMON_CODE`)는 시스템/ DW 양쪽 테이블을 통합 조회하며,
  관리자만이 시스템 코드 값을 생성/수정(`ActionCode.UPDATE`)할 수 있다. 일반 사용자는 `READ`만 부여된다.

### 선언형 권한 정의

```yaml
permissionGroups:
  - code: DEFAULT
    name: 기본 사용자
    defaultRowScope: OWN
    assignments:
      - feature: ORGANIZATION
        action: READ
        rowScope: ORG
        condition: "roles.contains('HR_VIEWER')"
    maskRules:
      - tag: ORG_NAME
        maskWith: "***"
        requiredAction: UNMASK
        audit: true
```

- `condition` 필드는 SpEL(Spring Expression Language) 표현식으로, `username`, `organizationCode`,
  `roles`, `permissionGroupCode`, `feature`, `action`, `defaultRowScope` 속성을 참조할 수 있다.
- `security.permission.declarative.enabled=false` 로 비활성화하거나
  `security.permission.declarative.location=file:/path/groups.yml` 로 외부 파일을 로드할 수 있다.

## 접근 제어

- 컨트롤러나 서비스 메서드에 `@RequirePermission(feature, action)`을 선언한다.
- Aspect가 실행돼 `PermissionEvaluator` → `PermissionGroupService` → `UserAccountService` 순으로 권한을 검증한다.
- 성공 시 `AuthContextHolder`에 ThreadLocal로 Feature/Action/RowScope/MaskRule 정보를 저장하고 감사 로그를 남긴다.
- 실패 시 `PermissionDeniedException`을 발생시키고 감사 로그를 WARN 레벨로 남긴다.
- `PermissionEvaluator`는 선언된 `condition` 표현식을 검사하며, 조건이 거짓이면 권한이 거부된다.

## 데이터 정책

- DTO/Record 필드는 `@Sensitive("TAG")`로 마킹한다. 예: `@Sensitive("ORG_NAME") String name`.
- `SensitiveDataMaskingModule`이 Jackson SerializerModifier로 등록되어 `DataPolicyEvaluator`를 통해 마스킹을 일괄 수행한다.
- 현재 요청의 `AuthContext` Action이 `FieldMaskRule.requiredAction`을 만족하지 못하면 기본 마스킹 문자열(`***`) 혹은 Rule별 mask 값을 반환한다.
- Action이 `UNMASK`이거나 Rule이 허용할 경우 원문을 그대로 직렬화한다. 감사 플래그가 참이면 허용/차단 이벤트를 로그로 남긴다.

## Row Scope

- `RowScope`는 `OWN`, `ORG`, `ALL`, `CUSTOM`을 지원하며 `com.example.common.security`에 공통 Enum으로 정의한다.
- 서비스 계층(`DwOrganizationQueryService`)은 RowScope와 사용자 조직코드를 입력받아 캐시된 조직 트리를 필터링하고 필요 시 Repository 조건을 강제한다.
- Repository 계층에서는 `com.example.common.security.RowScopeSpecifications` 를 사용해 `organization_code` 필드를 필수적으로 필터링하고, `@RequiresRowScope` 로 마킹된 Repository는 ArchUnit 규칙으로 감시된다.
- `CUSTOM` 스코프는 `OrganizationRowScopeStrategy` 빈을 통해 행 필터 전략을 주입해 확장한다.
- RowScope 조건식(`condition`)을 통해 조직코드나 역할 등에 따라 스코프를 세밀하게 제한할 수 있다
  (예: `"organizationCode == 'ROOT' && roles.contains('ROLE_AUDITOR')"`).

## 비동기/배치 컨텍스트 전파

- `AuthContextPropagator`를 사용하면 현재 스레드의 권한 컨텍스트를 `Runnable`/`Callable`에 캡처해 @Async, CompletableFuture 등 비동기 작업으로 전파할 수 있다.
- `AuthContextTaskDecorator`는 Spring `TaskDecorator` 구현체로, `AsyncConfig`에서 등록하면 `applicationTaskExecutor`가 자동으로 AuthContext를 복제한다.
- Quartz Job(`DwIngestionQuartzJob`)과 Spring Batch Tasklet은 `DwBatchAuthContext.systemContext()`와 `AuthContextPropagator`를 이용해 시스템 권한으로 실행된다. 배치/스케줄 작업에서 사용자 권한을 명시적으로 넘기려면 `AuthContext`를 JobDataMap 또는 실행 컨텍스트에 보관한 뒤 동일 유틸리티로 복원한다.

## 예시 적용 (HR 조직 조회)

1. `DwOrganizationController.organizations`에 `@RequirePermission(feature = FeatureCode.ORGANIZATION, action = ActionCode.READ)` 선언.
2. Aspect가 권한 검사 후 `AuthContext`를 설정.
3. 컨트롤러에서 `AuthContextHolder.current()` 값을 가져와 RowScope와 조직코드를 `DwOrganizationQueryService`에 전달.
4. Service는 Scope에 따라 캐싱된 조직 트리(`DwOrganizationTreeService`)에서 가시 범위를 산출한 뒤 필요 시 Repository/커스텀 전략을 호출한다.
5. DTO `DwOrganizationResponse.name` 필드는 `@Sensitive("ORG_NAME")`로 태깅되어 권한에 따라 마스킹된다.

## 파일 관리 서비스

- `/api/files` 업로드/다운로드/삭제 API는 `FeatureCode.FILE`과 `ActionCode.UPLOAD/DOWNLOAD/DELETE`(목록 조회는 `READ`)를 사용한다.
- 메타데이터(`stored_files`), 버전(`stored_file_versions`), 감사(`file_access_logs`)가 분리돼 있어
  보존/만료 정책과 감사 로그를 중앙화할 수 있다.
- 실제 바이너리는 `file.storage.root-path` 아래에 저장되며, 미사용 환경에서도 로컬 스토리지로 동작한다.
- 모듈에서 첨부파일을 사용하려면 파일 서비스가 돌려주는 `fileId`만 저장하고, 필요한 경우 다운로드 API로 역참조한다.
- 업로드 보안 정책(`maxFileSizeBytes`, `allowedFileExtensions`, `strictMimeValidation`, `fileRetentionDays`)은 정책 관리 API를 통해 조정되며,
  Apache Tika 를 이용한 MIME 검증으로 확장자 위장 파일을 차단한다. 보관 기한을 넘긴 파일은 백그라운드에서 정리하도록 연동할 수 있다.

## 확장 포인트

- `PermissionGroup.maskRules` 컬렉션에 필요한 태그, UNMASK 요구 여부, 감사 플래그를 저장.
- `OrganizationPolicyService`를 통해 조직 온보딩 시 기본 PermissionGroup 및 결재선 템플릿을 적용.
- Repository 계층에서 `RowScope.CUSTOM`일 때 별도 전략 빈을 주입해 세부 비즈니스 룰을 삽입할 수 있다.
- 메뉴 API(`/api/menus/me`) 구현 시 PermissionGroup 기반으로 접근 가능한 메뉴만 구성하도록 동일한 RequirePermission/RowScope 컨텍스트를 활용한다.
- 공통 코드 API(`/api/common-codes/{type}`)는 `FeatureCode.COMMON_CODE` 권한을 요구하며
  시스템 코드(`system_common_codes`)와 DW 코드(`dw_common_codes`)를 병합한 결과를 제공한다.
  시스템 관리자는 `/api/admin/common-codes/{type}`를 통해 코드 값을 동적으로 추가하거나 이름/순서를 조정할 수 있다.
