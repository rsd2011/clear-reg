# Approval 패키지 개선 계획서

> **작성일**: 2025-11-27
> **대상 패키지**: `backend/admin/src/main/java/com/example/admin/approval`

---

## 목차

1. [DTO 간소화 검토](#1-dto-간소화-검토)
2. [Javers 의존성 추가 및 마이그레이션](#2-javers-의존성-추가-및-마이그레이션)
3. [ApprovalLineTemplateAdminService 이름 변경](#3-approvallinetemplateadminservice-이름-변경)

---

## 1. DTO 간소화 검토

### 분석 대상

| DTO | 필드 수 | 검증 어노테이션 | 권장 |
|-----|--------|----------------|------|
| `TemplateCopyRequest` | 2 (name, description) | `@NotBlank`, `@Size` | **유지** |
| `RollbackRequest` | 2 (targetVersion, changeReason) | `@NotNull`, `@Size` | **유지** |
| `ApprovalGroupPriorityRequest` | 1 (priorities - 중첩 record) | `@NotNull`, `@Valid` | **유지** |
| `ApprovalTemplateStepRequest` | 2 (groupCode, stepOrder) | `@NotBlank`, `@NotNull` | **유지** |
| `GroupCodeExistsResponse` | 1 (exists - boolean) | 없음 | **검토 필요** |

### 결론 및 권장사항

**대부분의 DTO 유지 권장**:
- 검증 어노테이션(`@NotBlank`, `@Size`, `@Valid`)이 적용된 DTO는 타입 안전성과 입력 검증을 위해 유지
- Record 클래스의 간결한 문법으로 추가 코드 오버헤드 최소화

**검토 대상 - `GroupCodeExistsResponse`**:
```java
// 현재 구조
public record GroupCodeExistsResponse(boolean exists) {}

// 대안: Controller에서 직접 boolean 반환
@GetMapping("/exists")
public boolean checkExists(@RequestParam String code) {
    return service.exists(code);
}
```

**권장**: API 일관성과 향후 확장성(추가 필드 가능성)을 위해 **현행 유지**

### 조치 사항
- [ ] 즉시 조치 필요 없음 (현행 유지)

---

## 2. Javers 의존성 추가 및 마이그레이션

### 현재 상태

`ApprovalLineTemplateVersionService.compareVersions()` 메서드에서 수동으로 필드 비교 수행:

```java
// 현재 코드 (lines 155-172)
private List<FieldDiff> compareFields(ApprovalLineTemplateVersion v1,
                                       ApprovalLineTemplateVersion v2) {
    List<FieldDiff> diffs = new ArrayList<>();
    addFieldDiff(diffs, "name", "이름", v1.getName(), v2.getName());
    addFieldDiff(diffs, "displayOrder", "표시순서", v1.getDisplayOrder(), v2.getDisplayOrder());
    addFieldDiff(diffs, "description", "설명", v1.getDescription(), v2.getDescription());
    addFieldDiff(diffs, "active", "활성화", v1.isActive(), v2.isActive());
    return diffs;
}
```

### 문제점

1. **새 필드 추가 시 수동 업데이트 필요** - 누락 위험
2. **중첩 객체(Steps) 비교 로직 복잡** - 약 40줄의 수동 비교 코드
3. **타입별 비교 로직 중복** - addFieldDiff 메서드

### Javers 마이그레이션 계획

#### Step 1: 의존성 추가

**gradle/libs.versions.toml**:
```toml
[versions]
javers = "7.6.2"

[libraries]
javers-core = { module = "org.javers:javers-core", version.ref = "javers" }
```

**backend/admin/build.gradle**:
```groovy
dependencies {
    implementation libs.javers.core
}
```

#### Step 2: 서비스 코드 리팩토링

```java
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.diff.changetype.container.ListChange;

@Service
public class ApprovalLineTemplateVersionService {

    private final Javers javers = JaversBuilder.javers().build();

    // 비교 로직 간소화
    private List<FieldDiff> compareFields(ApprovalLineTemplateVersion v1,
                                          ApprovalLineTemplateVersion v2) {
        Diff diff = javers.compare(v1, v2);

        return diff.getChangesByType(ValueChange.class).stream()
            .filter(change -> FIELD_LABELS.containsKey(change.getPropertyName()))
            .map(change -> new FieldDiff(
                change.getPropertyName(),
                FIELD_LABELS.get(change.getPropertyName()),
                change.getLeft(),
                change.getRight(),
                DiffType.MODIFIED
            ))
            .toList();
    }

    private static final Map<String, String> FIELD_LABELS = Map.of(
        "name", "이름",
        "displayOrder", "표시순서",
        "description", "설명",
        "active", "활성화"
    );
}
```

#### Step 3: Step 비교 로직 개선

```java
private List<StepDiff> compareSteps(ApprovalLineTemplateVersion v1,
                                    ApprovalLineTemplateVersion v2) {
    Diff diff = javers.compareCollections(
        v1.getSteps(),
        v2.getSteps(),
        ApprovalTemplateStepVersion.class
    );

    List<StepDiff> result = new ArrayList<>();

    diff.getChanges().forEach(change -> {
        if (change instanceof ListChange listChange) {
            listChange.getChanges().forEach(elementChange -> {
                // Added, Removed, Modified 처리
            });
        }
    });

    return result;
}
```

### 예상 효과

| 항목 | Before | After |
|------|--------|-------|
| 필드 비교 코드 | 20줄 | 10줄 |
| Step 비교 코드 | 40줄 | 15줄 |
| 신규 필드 추가 시 | 수동 업데이트 | 자동 감지 |
| 테스트 용이성 | 낮음 | 높음 (Javers 자체 테스트됨) |

### 조치 사항

- [ ] `gradle/libs.versions.toml`에 javers 버전 추가
- [ ] `backend/admin/build.gradle`에 의존성 추가
- [ ] `ApprovalLineTemplateVersionService` 리팩토링
- [ ] 기존 테스트 업데이트 및 검증
- [ ] 성능 테스트 (대용량 Step 비교 시)

---

## 3. ApprovalLineTemplateAdminService 이름 변경

### 변경 사유

- `Admin` 접두어가 불필요하게 구체적임
- 실제로는 승인선 템플릿 관리 전반을 담당하는 서비스
- `FeatureCode.APPROVAL_MANAGE` 권한으로 접근 제어됨 (Admin 여부와 무관)

### 변경 대상 파일

| 파일 | 변경 내용 |
|------|----------|
| `ApprovalLineTemplateAdminService.java` | 클래스명 → `ApprovalLineTemplateService` |
| `ApprovalLineTemplateAdminServiceTest.java` | 클래스명 및 import 변경 |
| `ApprovalLineTemplateController.java` | 의존성 주입 필드명, import 변경 |
| `ApprovalLineTemplateControllerTest.java` | Mock 필드명, import 변경 |

### 변경 상세

#### 3.1 서비스 클래스 리네이밍

```bash
# 파일명 변경
mv ApprovalLineTemplateAdminService.java ApprovalLineTemplateService.java
```

```java
// Before
public class ApprovalLineTemplateAdminService { ... }

// After
public class ApprovalLineTemplateService { ... }
```

#### 3.2 테스트 클래스 리네이밍

```bash
mv ApprovalLineTemplateAdminServiceTest.java ApprovalLineTemplateServiceTest.java
```

#### 3.3 Controller 수정

```java
// Before
private final ApprovalLineTemplateAdminService templateService;

public ApprovalLineTemplateController(ApprovalLineTemplateAdminService templateService, ...) {

// After
private final ApprovalLineTemplateService templateService;

public ApprovalLineTemplateController(ApprovalLineTemplateService templateService, ...) {
```

#### 3.4 Controller 테스트 수정

```java
// Before
@MockBean
private ApprovalLineTemplateAdminService templateService;

// After
@MockBean
private ApprovalLineTemplateService templateService;
```

### 조치 사항

- [ ] `ApprovalLineTemplateAdminService.java` → `ApprovalLineTemplateService.java` 리네이밍
- [ ] `ApprovalLineTemplateAdminServiceTest.java` → `ApprovalLineTemplateServiceTest.java` 리네이밍
- [ ] `ApprovalLineTemplateController.java` import 및 필드 수정
- [ ] `ApprovalLineTemplateControllerTest.java` import 및 mock 수정
- [ ] 전체 빌드 및 테스트 실행

---

## 실행 우선순위

| 순서 | 항목 | 난이도 | 위험도 | 권장 시점 |
|------|------|--------|--------|----------|
| 1 | 서비스 이름 변경 | 낮음 | 낮음 | 즉시 |
| 2 | DTO 간소화 | - | - | 조치 불필요 |
| 3 | Javers 마이그레이션 | 중간 | 낮음 | 다음 스프린트 |

### 참고사항

- 모든 변경 후 `./gradlew :backend:admin:test :backend:server:test` 실행하여 검증
- Javers 마이그레이션은 별도 PR로 진행 권장
- 서비스 이름 변경은 단순 리팩토링이므로 즉시 진행 가능
