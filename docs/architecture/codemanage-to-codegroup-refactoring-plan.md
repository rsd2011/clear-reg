# CodeManage → CodeGroup/CodeItem 분리 리팩터링 계획

> **문서 버전**: 1.0
> **작성일**: 2025-01-28
> **목적**: CodeManage 단일 테이블을 CodeGroup + CodeItem 두 테이블로 분리하고, 네이밍을 CodeGroup으로 통일

---

## 1. 개요

### 1.1 현재 구조
```
system_common_codes (CodeManage)
├── code_type      (코드 그룹/타입)
├── code_value     (코드 값)
├── code_name      (코드명)
├── display_order  (표시 순서)
├── code_kind      (STATIC/DYNAMIC)
├── active         (활성 상태)
├── description    (설명)
├── metadata       (JSON 메타데이터)
├── updated_at     (수정일시)
└── updated_by     (수정자)
```

### 1.2 목표 구조
```
code_groups (CodeGroup)
├── id             (PK)
├── group_code     (이전 code_type, UNIQUE)
├── group_name     (그룹 표시명)
├── description    (설명)
├── group_kind     (STATIC/DYNAMIC)
├── active         (활성 상태)
├── metadata       (JSON 메타데이터)
├── display_order  (표시 순서)
├── updated_at     (수정일시)
└── updated_by     (수정자)

code_items (CodeItem)
├── id             (PK)
├── group_id       (FK → code_groups)
├── item_code      (이전 code_value)
├── item_name      (이전 code_name)
├── display_order  (표시 순서)
├── active         (활성 상태)
├── description    (설명)
├── metadata       (JSON 메타데이터)
├── updated_at     (수정일시)
└── updated_by     (수정자)
└── UNIQUE(group_id, item_code)
```

### 1.3 관계
```
CodeGroup (1) ←─── (N) CodeItem
```

---

## 2. 영향 범위 분석

### 2.1 영향받는 파일 (총 41개)

#### Domain Layer (7개 파일)
| 현재 파일 | 변경 후 | 작업 |
|----------|--------|------|
| `domain/CodeManage.java` | `domain/CodeGroup.java` | 리네이밍 + 필드 수정 |
| (새로 생성) | `domain/CodeItem.java` | 신규 생성 |
| `domain/CodeManageKind.java` | `domain/CodeGroupKind.java` | 리네이밍 |
| `domain/CodeManageSource.java` | `domain/CodeGroupSource.java` | 리네이밍 |
| `domain/CodeManageType.java` | `domain/CodeGroupType.java` | 리네이밍 |
| `domain/DynamicCodeType.java` | 유지 또는 `DynamicCodeGroupType.java` | 검토 필요 |

#### Repository Layer (2개 파일)
| 현재 파일 | 변경 후 | 작업 |
|----------|--------|------|
| `repository/CodeManageRepository.java` | `repository/CodeGroupRepository.java` | 리네이밍 + 메서드 수정 |
| (새로 생성) | `repository/CodeItemRepository.java` | 신규 생성 |

#### Service Layer (2개 파일)
| 현재 파일 | 변경 후 | 작업 |
|----------|--------|------|
| `CodeManageService.java` | `CodeGroupService.java` | 리네이밍 + 로직 수정 |
| `CodeManageQueryService.java` | `CodeGroupQueryService.java` | 리네이밍 + 로직 수정 |

#### DTO Layer (8개 파일)
| 현재 파일 | 변경 후 | 작업 |
|----------|--------|------|
| `dto/CodeManageItem.java` | `dto/CodeGroupItem.java` | 리네이밍 |
| `dto/CodeManageItemRequest.java` | `dto/CodeGroupItemRequest.java` | 리네이밍 |
| `dto/CodeManageItemResponse.java` | `dto/CodeGroupItemResponse.java` | 리네이밍 |
| `dto/CodeManageRequest.java` | `dto/CodeGroupRequest.java` | 리네이밍 |
| `dto/CodeManageResponse.java` | `dto/CodeGroupResponse.java` | 리네이밍 |
| `dto/CodeManageAggregateResponse.java` | `dto/CodeGroupAggregateResponse.java` | 리네이밍 |
| `dto/CodeTypeInfo.java` | 유지 | 참조 업데이트 |
| (새로 생성) | `dto/CodeItemRequest.java` | 신규 생성 |
| (새로 생성) | `dto/CodeItemResponse.java` | 신규 생성 |

#### Controller Layer - Server Module (3개 파일)
| 현재 파일 | 변경 후 | 작업 |
|----------|--------|------|
| `CodeManageController.java` | `CodeGroupController.java` | 리네이밍 |
| `CommonCodeAdminController.java` | 유지 | 참조 업데이트 |
| `CommonCodeController.java` | 유지 | 참조 업데이트 |

#### Infrastructure (4개 파일)
| 현재 파일 | 변경 후 | 작업 |
|----------|--------|------|
| `registry/StaticCodeRegistry.java` | 유지 | 참조 업데이트 |
| `registry/EnumCodeRegistry.java` | 유지 | 참조 업데이트 |
| `validation/EnumConsistencyValidator.java` | 유지 | 참조 업데이트 |
| `event/CodeChangedEvent.java` | 유지 | 참조 업데이트 |
| `event/CodeChangedEventListener.java` | 유지 | 참조 업데이트 |
| `CacheMaintenanceService.java` | 유지 | 참조 업데이트 |

#### Test Files (15개 파일)
| 현재 파일 | 변경 후 |
|----------|--------|
| `CodeManageControllerTest.java` | `CodeGroupControllerTest.java` |
| `CodeManageQueryServiceUnifiedApiTest.java` | `CodeGroupQueryServiceUnifiedApiTest.java` |
| `CodeManageQueryServiceTest.java` | `CodeGroupQueryServiceTest.java` |
| `CodeManageQueryServiceBranchTest.java` | `CodeGroupQueryServiceBranchTest.java` |
| `CodeManageServiceTest.java` | `CodeGroupServiceTest.java` |
| `CodeManageServiceLineCoverTest.java` | `CodeGroupServiceLineCoverTest.java` |
| `CodeManageTest.java` | `CodeGroupTest.java` |
| `CodeManageTypeTest.java` | `CodeGroupTypeTest.java` |
| `CodeManageSourceTest.java` | `CodeGroupSourceTest.java` |
| `DynamicCodeTypeTest.java` | 유지 또는 리네이밍 |
| `EnumCodeRegistryTest.java` | 유지 (참조 업데이트) |
| `EnumConsistencyValidatorTest.java` | 유지 (참조 업데이트) |
| `CodeChangedEventTest.java` | 유지 (참조 업데이트) |
| `CodeChangedEventListenerTest.java` | 유지 (참조 업데이트) |
| `CommonCodeAdminControllerTest.java` | 유지 (참조 업데이트) |
| `CommonCodeControllerTest.java` | 유지 (참조 업데이트) |
| `CacheMaintenanceServiceTest.java` | 유지 (참조 업데이트) |

---

## 3. 단계별 리팩터링 계획

### Phase 1: 도메인 모델 분리 (예상 2-3일)

#### 1.1 CodeGroup 엔티티 생성
```java
@Entity
@Table(name = "code_groups",
    indexes = @Index(name = "idx_code_group_code", columnList = "group_code"))
@Getter
public class CodeGroup extends PrimaryKeyEntity {

    @Column(name = "group_code", nullable = false, unique = true, length = 64)
    private String groupCode;  // 이전 codeType

    @Column(name = "group_name", nullable = false, length = 255)
    private String groupName;

    @Column(name = "description", length = 512)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_kind", nullable = false, length = 16)
    private CodeGroupKind groupKind = CodeGroupKind.DYNAMIC;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadataJson;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by", nullable = false, length = 128)
    private String updatedBy;

    @OneToMany(mappedBy = "codeGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, itemCode ASC")
    private List<CodeItem> items = new ArrayList<>();

    // Factory methods, business logic...
}
```

#### 1.2 CodeItem 엔티티 생성
```java
@Entity
@Table(name = "code_items",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_code_item_group_code",
        columnNames = {"group_id", "item_code"}
    ),
    indexes = @Index(name = "idx_code_item_group", columnList = "group_id, display_order"))
@Getter
public class CodeItem extends PrimaryKeyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private CodeGroup codeGroup;

    @Column(name = "item_code", nullable = false, length = 128)
    private String itemCode;  // 이전 codeValue

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;  // 이전 codeName

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadataJson;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by", nullable = false, length = 128)
    private String updatedBy;

    // Factory methods, business logic...
}
```

#### 1.3 Enum 리네이밍
| 현재 | 변경 후 | 비고 |
|-----|--------|-----|
| `CodeManageKind` | `CodeGroupKind` | STATIC, DYNAMIC 유지 |
| `CodeManageSource` | `CodeGroupSource` | STATIC_ENUM, DYNAMIC_DB, DW, APPROVAL_GROUP 유지 |
| `CodeManageType` | `CodeGroupType` | 기존 타입 매핑 유지 |

---

### Phase 2: Repository 계층 (예상 1-2일)

#### 2.1 CodeGroupRepository
```java
public interface CodeGroupRepository extends JpaRepository<CodeGroup, UUID> {

    Optional<CodeGroup> findByGroupCode(String groupCode);

    @Query("SELECT DISTINCT g.groupCode FROM CodeGroup g ORDER BY g.groupCode")
    List<String> findDistinctGroupCodes();

    long countByGroupCode(String groupCode);

    boolean existsByGroupCode(String groupCode);
}
```

#### 2.2 CodeItemRepository
```java
public interface CodeItemRepository extends JpaRepository<CodeItem, UUID> {

    List<CodeItem> findByCodeGroupOrderByDisplayOrderAscItemCodeAsc(CodeGroup group);

    List<CodeItem> findByCodeGroup_GroupCodeOrderByDisplayOrderAsc(String groupCode);

    Optional<CodeItem> findByCodeGroup_GroupCodeAndItemCode(String groupCode, String itemCode);

    long countByCodeGroup_GroupCode(String groupCode);

    @Query("SELECT i FROM CodeItem i JOIN FETCH i.codeGroup WHERE i.codeGroup.groupCode = :groupCode")
    List<CodeItem> findAllByGroupCodeWithGroup(@Param("groupCode") String groupCode);
}
```

---

### Phase 3: Service 계층 (예상 2-3일)

#### 3.1 CodeGroupService (기존 CodeManageService 대체)
- 그룹 CRUD 로직
- 그룹 + 아이템 일괄 관리

#### 3.2 CodeGroupQueryService (기존 CodeManageQueryService 대체)
- 통합 조회 API
- Static Enum + Dynamic DB 통합 쿼리

**주요 메서드 매핑:**
| 현재 메서드 | 변경 후 |
|-----------|--------|
| `findAll(codeType)` | `findAllItems(groupCode)` |
| `findByCodeTypeAndCodeValue()` | `findItem(groupCode, itemCode)` |
| `getCodeTypeInfos()` | `getGroupInfos()` |
| `findAllItems(sources, codeType, active, search)` | `findAllItems(sources, groupCode, active, search)` |

---

### Phase 4: DTO 계층 (예상 1-2일)

#### 4.1 네이밍 매핑
| 현재 DTO | 변경 후 |
|---------|--------|
| `CodeManageItem` | `CodeGroupItem` |
| `CodeManageItemRequest` | `CodeGroupItemRequest` (그룹 아이템 생성/수정) |
| `CodeManageItemResponse` | `CodeGroupItemResponse` |
| `CodeManageRequest` | `CodeGroupRequest` (그룹 생성/수정) |
| `CodeManageResponse` | `CodeGroupResponse` |
| `CodeManageAggregateResponse` | `CodeGroupAggregateResponse` |

#### 4.2 신규 DTO
```java
// 코드 아이템 전용 요청 DTO
public record CodeItemRequest(
    String itemCode,
    String itemName,
    Integer displayOrder,
    Boolean active,
    String description,
    String metadataJson
) {}

// 코드 아이템 전용 응답 DTO
public record CodeItemResponse(
    UUID id,
    String groupCode,
    String itemCode,
    String itemName,
    Integer displayOrder,
    boolean active,
    String description,
    String metadataJson,
    OffsetDateTime updatedAt,
    String updatedBy
) {}
```

---

### Phase 5: Controller 계층 (예상 1-2일)

#### 5.1 CodeGroupController (기존 CodeManageController 대체)

**API 경로 결정:**
- **옵션 A**: 기존 유지 `/api/codes` (하위 호환성)
- **옵션 B**: 새 경로 `/api/code-groups` (명확한 네이밍)

**권장**: 옵션 A (기존 경로 유지) - 프론트엔드 변경 최소화

```java
@RestController
@RequestMapping("/api/codes")
public class CodeGroupController {

    // 기존 API 시그니처 유지
    @GetMapping("/types")
    public ResponseEntity<List<CodeTypeInfo>> getCodeTypes() { ... }

    @GetMapping("/items")
    public ResponseEntity<List<CodeGroupItemResponse>> getItems(...) { ... }

    @PostMapping("/items")
    public ResponseEntity<CodeGroupItemResponse> createItem(...) { ... }

    @PutMapping("/items")
    public ResponseEntity<CodeGroupItemResponse> updateItem(...) { ... }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(...) { ... }
}
```

---

### Phase 6: 데이터 마이그레이션 (예상 1일)

#### 6.1 마이그레이션 SQL 스크립트
```sql
-- 2025-01-XX-migrate-codemanage-to-codegroup.sql

-- 1. 새 테이블 생성
CREATE TABLE code_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_code VARCHAR(64) NOT NULL UNIQUE,
    group_name VARCHAR(255) NOT NULL,
    description VARCHAR(512),
    group_kind VARCHAR(16) NOT NULL DEFAULT 'DYNAMIC',
    active BOOLEAN NOT NULL DEFAULT true,
    metadata JSONB,
    display_order INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(128) NOT NULL
);

CREATE TABLE code_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES code_groups(id) ON DELETE CASCADE,
    item_code VARCHAR(128) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT true,
    description VARCHAR(512),
    metadata JSONB,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(128) NOT NULL,
    UNIQUE(group_id, item_code)
);

CREATE INDEX idx_code_groups_code ON code_groups(group_code);
CREATE INDEX idx_code_items_group ON code_items(group_id, display_order);

-- 2. 기존 데이터 마이그레이션
-- 2.1 그룹 생성 (중복 제거된 code_type 기준)
INSERT INTO code_groups (group_code, group_name, description, group_kind, active, updated_at, updated_by)
SELECT DISTINCT
    code_type,
    COALESCE(
        (SELECT code_name FROM system_common_codes s2
         WHERE s2.code_type = s.code_type
         ORDER BY display_order LIMIT 1),
        code_type
    ),
    MIN(description),
    code_kind,
    true,
    MAX(updated_at),
    MAX(updated_by)
FROM system_common_codes s
GROUP BY code_type, code_kind;

-- 2.2 아이템 생성
INSERT INTO code_items (group_id, item_code, item_name, display_order, active, description, metadata, updated_at, updated_by)
SELECT
    g.id,
    s.code_value,
    s.code_name,
    s.display_order,
    s.active,
    s.description,
    s.metadata,
    s.updated_at,
    s.updated_by
FROM system_common_codes s
JOIN code_groups g ON g.group_code = s.code_type;

-- 3. 검증 쿼리
SELECT
    (SELECT COUNT(*) FROM system_common_codes) as old_count,
    (SELECT COUNT(*) FROM code_items) as new_item_count,
    (SELECT COUNT(DISTINCT code_type) FROM system_common_codes) as old_type_count,
    (SELECT COUNT(*) FROM code_groups) as new_group_count;

-- 4. 기존 테이블 백업 후 삭제 (별도 스크립트로 분리 권장)
-- ALTER TABLE system_common_codes RENAME TO system_common_codes_backup;
-- DROP TABLE system_common_codes_backup; -- 마이그레이션 검증 완료 후
```

#### 6.2 롤백 스크립트
```sql
-- rollback-codemanage-migration.sql

-- 새 테이블에서 기존 테이블로 복원
INSERT INTO system_common_codes (id, code_type, code_value, code_name, display_order, code_kind, active, description, metadata, updated_at, updated_by)
SELECT
    i.id,
    g.group_code,
    i.item_code,
    i.item_name,
    i.display_order,
    g.group_kind,
    i.active,
    i.description,
    i.metadata,
    i.updated_at,
    i.updated_by
FROM code_items i
JOIN code_groups g ON i.group_id = g.id;

DROP TABLE code_items;
DROP TABLE code_groups;
```

---

### Phase 7: 테스트 업데이트 (예상 2-3일)

#### 7.1 테스트 파일 리네이밍
모든 `*CodeManage*Test.java` → `*CodeGroup*Test.java`

#### 7.2 신규 테스트 케이스 추가
- CodeGroup-CodeItem 1:N 관계 테스트
- 그룹 삭제 시 아이템 cascade 삭제 테스트
- 아이템 unique constraint 테스트

---

### Phase 8: 정리 및 검증 (예상 1일)

#### 8.1 삭제 대상
- 기존 `CodeManage*.java` 파일들 (deprecation 기간 후)
- `system_common_codes` 테이블 (백업 후)

#### 8.2 최종 검증 체크리스트
- [ ] 모든 API 엔드포인트 정상 동작
- [ ] 기존 프론트엔드 호환성 확인
- [ ] 데이터 마이그레이션 무결성 검증
- [ ] 테스트 커버리지 80% 이상 유지
- [ ] JaCoCo 빌드 통과

---

## 4. 패키지 구조 변경

### 4.1 현재 구조
```
com.example.admin.codemanage/
├── domain/
│   ├── CodeManage.java
│   ├── CodeManageKind.java
│   ├── CodeManageSource.java
│   ├── CodeManageType.java
│   └── DynamicCodeType.java
├── repository/
│   └── CodeManageRepository.java
├── dto/
│   ├── CodeManageItem.java
│   ├── CodeManageItemRequest.java
│   ├── CodeManageItemResponse.java
│   └── ...
├── CodeManageService.java
└── CodeManageQueryService.java
```

### 4.2 변경 후 구조
```
com.example.admin.codegroup/  (패키지명도 변경 권장)
├── domain/
│   ├── CodeGroup.java
│   ├── CodeItem.java
│   ├── CodeGroupKind.java
│   ├── CodeGroupSource.java
│   ├── CodeGroupType.java
│   └── DynamicCodeType.java (또는 DynamicGroupType.java)
├── repository/
│   ├── CodeGroupRepository.java
│   └── CodeItemRepository.java
├── dto/
│   ├── CodeGroupItem.java
│   ├── CodeGroupItemRequest.java
│   ├── CodeGroupItemResponse.java
│   ├── CodeItemRequest.java
│   ├── CodeItemResponse.java
│   └── ...
├── CodeGroupService.java
└── CodeGroupQueryService.java
```

---

## 5. 의사결정 필요 사항

### 5.1 확인 필요
1. **패키지 경로**: `codemanage` → `codegroup` 변경 여부
2. **API 경로**: `/api/codes` 유지 vs `/api/code-groups` 변경
3. **하위 호환성 기간**: deprecated 코드 유지 기간
4. **DynamicCodeType**: 리네이밍 여부 (DynamicGroupType?)

### 5.2 권장 결정
| 항목 | 권장 | 이유 |
|-----|-----|-----|
| 패키지 경로 | `codegroup`으로 변경 | 일관성 |
| API 경로 | `/api/codes` 유지 | 프론트엔드 변경 최소화 |
| 하위 호환성 | 1 스프린트 (2주) | 안전한 전환 |
| DynamicCodeType | 유지 | 영향 범위 최소화 |

---

## 6. 리스크 및 완화 전략

### 6.1 리스크
| 리스크 | 영향 | 완화 전략 |
|-------|-----|----------|
| 데이터 마이그레이션 실패 | 높음 | 롤백 스크립트 준비, 백업 테이블 유지 |
| API 호환성 깨짐 | 중간 | 기존 API 시그니처 유지 |
| 테스트 실패 | 중간 | 단계별 마이그레이션, CI 검증 |
| 성능 저하 | 낮음 | 인덱스 최적화, N+1 방지 |

### 6.2 롤백 계획
1. 코드 롤백: Git revert
2. DB 롤백: 롤백 SQL 스크립트 실행
3. 백업 테이블 복원

---

## 7. 일정 (예상)

| Phase | 기간 | 담당 |
|-------|-----|-----|
| Phase 1: 도메인 모델 | 2-3일 | - |
| Phase 2: Repository | 1-2일 | - |
| Phase 3: Service | 2-3일 | - |
| Phase 4: DTO | 1-2일 | - |
| Phase 5: Controller | 1-2일 | - |
| Phase 6: 데이터 마이그레이션 | 1일 | - |
| Phase 7: 테스트 | 2-3일 | - |
| Phase 8: 정리 | 1일 | - |
| **총 예상** | **약 2주** | - |

---

## 8. 체크리스트

### 착수 전
- [ ] 의사결정 사항 확정 (패키지, API 경로 등)
- [ ] 프론트엔드 팀 공지
- [ ] 백업 전략 확인

### 진행 중
- [ ] Phase 1 완료: 도메인 모델 분리
- [ ] Phase 2 완료: Repository 계층
- [ ] Phase 3 완료: Service 계층
- [ ] Phase 4 완료: DTO 계층
- [ ] Phase 5 완료: Controller 계층
- [ ] Phase 6 완료: 데이터 마이그레이션
- [ ] Phase 7 완료: 테스트 업데이트
- [ ] Phase 8 완료: 정리

### 완료 후
- [ ] API 문서 업데이트
- [ ] 프론트엔드 호환성 확인
- [ ] 프로덕션 배포 승인
- [ ] deprecated 코드 삭제 일정 확정
