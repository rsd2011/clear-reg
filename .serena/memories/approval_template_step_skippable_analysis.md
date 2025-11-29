# ApprovalTemplateStep Skippable 필드 추가 계획 분석

## 1. 엔티티 개요

### ApprovalTemplateStep (현재 레거시)
- **위치**: `backend/admin/src/main/java/com/example/admin/approval/domain/ApprovalTemplateStep.java`
- **상태**: 레거시 엔티티 (하위 호환성 유지, 추후 제거 예정)
- **현재 필드**:
  - `template`: ApprovalLineTemplate (ManyToOne)
  - `stepOrder`: int (승인 단계 순서)
  - `approvalGroup`: ApprovalGroup (ManyToOne)
- **테이블**: `approval_template_steps`

### ApprovalTemplateStepVersion (활성 버전)
- **위치**: `backend/admin/src/main/java/com/example/admin/approval/domain/ApprovalTemplateStepVersion.java`
- **상태**: 현재 활용 중 (SCD Type 2 이력관리)
- **현재 필드**:
  - `templateVersion`: ApprovalLineTemplateVersion (ManyToOne)
  - `stepOrder`: int
  - `approvalGroup`: ApprovalGroup (ManyToOne)
  - `approvalGroupCode`: String (비정규화)
  - `approvalGroupName`: String (비정규화)
- **테이블**: `approval_template_step_versions`

---

## 2. 관계 엔티티

### ApprovalLineTemplate
- **역할**: 승인선 템플릿 메인 엔티티
- **버전 관리**: `currentVersion`, `previousVersion`, `nextVersion`로 SCD Type 2 구현
- **레거시 필드**: `steps` (List<ApprovalTemplateStep>) - 추후 제거 예정

### ApprovalLineTemplateVersion
- **역할**: 템플릿 버전 저장 (SCD Type 2)
- **관계**: `steps` (List<ApprovalTemplateStepVersion>)
- **상태**: DRAFT, PUBLISHED, HISTORICAL

### ApprovalGroup
- **역할**: 승인 그룹
- **필드**: `groupCode`, `name` (ApprovalTemplateStepVersion에서 비정규화)

---

## 3. DTO 클래스

### ApprovalTemplateStepRequest
- **위치**: `backend/admin/src/main/java/com/example/admin/approval/dto/ApprovalTemplateStepRequest.java`
- **현재 필드**:
  - `stepOrder`: int (@Min(1))
  - `approvalGroupCode`: String (@NotBlank @Size(max=64))
- **변경 필요**: `skippable` boolean 필드 추가

### ApprovalTemplateStepResponse
- **위치**: `backend/admin/src/main/java/com/example/admin/approval/dto/ApprovalTemplateStepResponse.java`
- **현재 필드**:
  - `id`: UUID
  - `stepOrder`: int
  - `approvalGroupCode`: String
  - `approvalGroupName`: String
- **변경 필요**: `skippable` boolean 필드 추가
- **Mapper 메서드**: `from(ApprovalTemplateStep step)` 변경 필요

---

## 4. Service 클래스 영향 범위

### ApprovalLineTemplateService
- **핵심 메서드**:
  - `addStepsToTemplate(ApprovalLineTemplate, List<ApprovalTemplateStepRequest>)` - DTO → 엔티티 변환
  - `copyTemplate()` - Step 복사 로직
- **변경 필요**: 
  - Step 복사 시 `skippable` 필드도 함께 복사
  - Request → ApprovalTemplateStep 변환 시 skippable 필드 처리

### ApprovalLineTemplateVersionService
- **핵심 메서드**:
  - `addStepsToVersion()` - DTO → 버전 엔티티 변환
  - `addStepsToDraft()` - Draft에 Step 추가
  - `copyFrom(ApprovalTemplateStepVersion)` - Step 버전 복사
- **변경 필요**:
  - ApprovalTemplateStepVersion.copyFrom() 메서드에서 skippable 필드 복사
  - Step 버전 생성 메서드에서 skippable 필드 처리

---

## 5. 테스트 파일 영향 범위

### 주요 테스트 클래스
1. `ApprovalGroupTest.java` - ApprovalTemplateStep 생성 테스트
2. `ApprovalLineTemplateTest.java` - Template Step 추가/변경 테스트
3. `ApprovalTemplateStepVersionTest.java` - Step 버전 생성/복사 테스트
4. `ApprovalLineTemplateServiceTest.java` - Service 레벨 통합 테스트
5. `ApprovalLineTemplateVersionServiceTest.java` - 버전 서비스 테스트

**변경 필요**:
- 모든 ApprovalTemplateStepRequest 생성 구문에 skippable 필드 추가
- ApprovalTemplateStepVersion.create() 호출 시 skippable 필드 전달
- 검증 테스트 추가 (valid/invalid skippable 값)

---

## 6. 데이터베이스 마이그레이션 계획

### 패턴 분석
- **최근 마이그레이션 예시**: `2025-11-27-approval-scd-type2.sql`
  - ALTER TABLE로 컬럼 추가 (NOT NULL DEFAULT 지정)
  - 인덱스 생성
  - 기존 데이터 초기화
  - 롤백 SQL 주석으로 제공

### 필요한 마이그레이션 스크립트
```sql
-- 1. approval_template_steps 테이블에 skippable 컬럼 추가
ALTER TABLE approval_template_steps
ADD COLUMN IF NOT EXISTS skippable BOOLEAN NOT NULL DEFAULT false;

-- 2. approval_template_step_versions 테이블에 skippable 컬럼 추가
ALTER TABLE approval_template_step_versions
ADD COLUMN IF NOT EXISTS skippable BOOLEAN NOT NULL DEFAULT false;

-- 3. 인덱스 고려 (선택)
-- 스킵 가능한 단계를 자주 쿼리하는 경우만
```

### 롤백 계획
```sql
ALTER TABLE approval_template_steps DROP COLUMN IF NOT EXISTS skippable;
ALTER TABLE approval_template_step_versions DROP COLUMN IF NOT EXISTS skippable;
```

---

## 7. 컨트롤러/API 영향

### ApprovalLineTemplateController (추정)
- **GET** /templates/{id} - ApprovalLineTemplateResponse 반환
  - Response DTO에 skippable 필드 포함 필요
  
- **POST/PUT** /templates - ApprovalLineTemplateRequest 처리
  - Request DTO의 steps[]에 skippable 필드 필요

---

## 8. 변경 요약

| 항목 | 파일 | 변경 내용 |
|------|------|---------|
| **엔티티** | ApprovalTemplateStep.java | boolean skippable 필드 추가 |
| | ApprovalTemplateStepVersion.java | boolean skippable 필드 추가 + copyFrom() 메서드 수정 |
| **DTO** | ApprovalTemplateStepRequest.java | boolean skippable 필드 추가 |
| | ApprovalTemplateStepResponse.java | boolean skippable 필드 추가 + from() 메서드 수정 |
| **Service** | ApprovalLineTemplateService.java | addStepsToTemplate() 메서드 수정 |
| | ApprovalLineTemplateVersionService.java | addStepsToVersion(), copyFrom() 메서드 수정 |
| **테스트** | 모든 테스트 클래스 | Step 생성 시 skippable 값 지정 |
| **Migration** | docs/migrations/YYYY-MM-DD-*.sql | 새로운 마이그레이션 스크립트 생성 |

---

## 9. 구현 순서 (권장)

1. **엔티티 수정**:
   - ApprovalTemplateStep에 skippable 필드 추가
   - ApprovalTemplateStepVersion에 skippable 필드 추가

2. **Service 메서드 수정**:
   - ApprovalLineTemplateService.addStepsToTemplate()
   - ApprovalLineTemplateVersionService 메서드들

3. **DTO 수정**:
   - ApprovalTemplateStepRequest에 필드 추가
   - ApprovalTemplateStepResponse에 필드 추가 및 mapper 수정

4. **마이그레이션 스크립트 생성**:
   - docs/migrations/2025-11-29-approval-template-step-skippable.sql

5. **테스트 수정**:
   - 모든 테스트에서 skippable 값 지정

6. **통합 테스트**:
   - E2E 테스트 실행 (build, test coverage 확인)

