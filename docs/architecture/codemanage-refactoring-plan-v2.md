# CodeManage 리팩토링 계획 v2

> **작성일**: 2025-01-28
> **버전**: 2.0 (개선된 요건 반영)
> **상태**: 계획 수립 완료

---

## 1. 개요

### 1.1 배경
현재 `codemanage` 패키지는 시스템 공통코드를 관리하는 역할을 수행합니다. 사용자 요청에 따라 다음 주요 변경사항을 반영한 리팩토링을 수행합니다.

### 1.2 주요 변경 요건

| 요건 | 설명 |
|------|------|
| **CodeManageKind 병합** | `CodeManageKind`를 없애고 `CodeManageSource`로 기능 병합 |
| **모듈 위치 검토** | server 모듈로 이관 필요성 검토 (enum 스캐닝 문제) |
| **SystemCommonCodeType 분리** | 정적 코드목록과 동적 코드로 분리 |
| **전체 Enum 수집** | 어노테이션 기반이 아닌 전체 Enum 자동 수집 (DB 우선순위 최고) |
| **베스트 프랙티스** | 더 나은 방향 검토 |

---

## 2. 현황 분석

### 2.1 현재 패키지 구조

```
backend/admin/src/main/java/com/example/admin/codemanage/
├── CodeManageQueryService.java      # 코드 조회 서비스 (캐싱)
├── SystemCommonCodeService.java     # DB 코드 CRUD
├── SystemCommonCodeRepository.java  # JPA Repository
└── model/
    ├── CodeManageSource.java        # SYSTEM, DW (소스 타입)
    ├── CodeManageKind.java          # STATIC, DYNAMIC, FEDERATED (종류)
    ├── SystemCommonCodeType.java    # 시스템 코드 타입 Enum
    ├── SystemCommonCode.java        # JPA Entity
    └── CodeManageItem.java          # DTO Record
```

### 2.2 현재 Enum 정의

**CodeManageSource.java** (현재)
```java
public enum CodeManageSource {
    SYSTEM,
    DW
}
```

**CodeManageKind.java** (현재)
```java
public enum CodeManageKind {
    STATIC,       // 정적 Enum 기반 (읽기 전용)
    DYNAMIC,      // DB 기반 (CRUD 가능)
    FEDERATED     // 외부 시스템 연동
}
```

**SystemCommonCodeType.java** (현재)
```java
public enum SystemCommonCodeType {
    NOTICE_CATEGORY("NOTICE_CATEGORY", CodeManageKind.DYNAMIC),
    FILE_CLASSIFICATION("FILE_CLASSIFICATION", CodeManageKind.STATIC),
    ALERT_CHANNEL("ALERT_CHANNEL", CodeManageKind.DYNAMIC),
    CUSTOM("CUSTOM", CodeManageKind.DYNAMIC);

    private final String code;           // ← 중복 (name()과 동일)
    private final CodeManageKind defaultKind;
    // ...
}
```

### 2.3 모듈 의존성 분석

```
server (main entry point)
├── admin (codemanage 포함)
│   ├── platform
│   ├── audit
│   └── data-integration
├── auth (Enum 포함: FeatureCode, ActionCode 등)
├── draft (Enum 포함: DraftStatus 등)
├── approval (ApprovalGroup Entity 포함)
├── audit (Enum 포함: AuditEventType 등)
└── platform (Enum 포함: YesNo 등)
```

**Server Application.java의 scanBasePackages:**
```java
@SpringBootApplication(scanBasePackages = {
    "com.example.server",
    "com.example.auth",
    "com.example.admin",
    "com.example.common",
    "com.example.dw",
    "com.example.draft",
    "com.example.file",
    "com.example.platform"
})
```

### 2.4 프로젝트 전체 Enum 목록 (27개)

| 모듈 | Enum | 코드 관리 적합성 |
|------|------|-----------------|
| platform | YesNo | ✅ |
| platform | AuditPartitionType | ⚠️ (내부용) |
| auth | FeatureCode | ✅ |
| auth | ActionCode | ✅ |
| auth | RowScope | ✅ |
| auth | PermissionLevel | ✅ |
| auth | SsoType | ✅ |
| auth | UserStatus | ✅ |
| auth | AuthEventType | ✅ |
| admin | SystemCommonCodeType | ✅ (분리 대상) |
| admin | CodeManageSource | ⚠️ (내부용) |
| admin | CodeManageKind | ❌ (삭제 예정) |
| admin | MaskingType | ✅ |
| admin | MenuType | ✅ |
| draft | DraftStatus | ✅ |
| draft | DraftType | ✅ |
| draft | DraftAttachmentType | ✅ |
| approval | ApprovalStatus | ✅ |
| approval | ApprovalActionType | ✅ |
| approval | DelegationType | ✅ |
| audit | AuditEventType | ✅ |
| audit | AuditCategory | ✅ |
| audit | AuditSeverity | ✅ |
| audit | HashAlgorithm | ⚠️ (내부용) |
| file-core | FileStorageType | ✅ |
| file-core | UploadStatus | ✅ |
| data-integration | ExportFormat | ✅ |

---

## 3. 설계 결정사항

### 3.1 모듈 위치: admin 유지 (server 이관 불필요)

**분석 결과:**
- Server 모듈이 이미 `scanBasePackages`로 모든 모듈 패키지를 스캔
- 런타임에 `ClassPathScanningCandidateComponentProvider`로 전체 Enum 스캔 가능
- Enum들은 각 모듈의 classpath에 있으며, server 실행 시점에 모두 접근 가능

**결론:** `codemanage`는 **admin 모듈에 유지**하며, Enum 스캐너만 server에서 실행

### 3.2 CodeManageSource 통합 설계

**새로운 CodeManageSource** (CodeManageKind 병합):
```java
@Getter
@RequiredArgsConstructor
public enum CodeManageSource {
    // 정적 소스 (읽기 전용)
    STATIC_ENUM("프로젝트 Enum", false, true),

    // 동적 소스 (CRUD 가능)
    DYNAMIC_DB("시스템 공통코드 DB", true, false),

    // 외부 소스
    DW("데이터웨어하우스", false, true),
    APPROVAL_GROUP("승인 그룹", false, true),  // ApprovalGroup Entity 기반

    // 페더레이션 (외부 API 연동)
    FEDERATED("외부 시스템 연동", false, true);

    private final String description;
    private final boolean editable;      // DB CRUD 가능 여부
    private final boolean readOnly;      // 읽기 전용 여부
}
```

### 3.3 SystemCommonCodeType 분리 전략

기존 `SystemCommonCodeType`을 두 가지 용도로 분리:

#### 3.3.1 DynamicCodeType (동적 DB 코드)
```java
/**
 * DB에서 관리되는 동적 공통코드 타입.
 * 관리자가 CRUD 가능하며, metadataJson을 통해 확장 가능.
 */
@Getter
@RequiredArgsConstructor
public enum DynamicCodeType {
    NOTICE_CATEGORY("공지사항 카테고리"),
    ALERT_CHANNEL("알림 채널"),
    CUSTOM("사용자 정의");

    private final String description;

    public CodeManageSource getSource() {
        return CodeManageSource.DYNAMIC_DB;
    }
}
```

#### 3.3.2 StaticCodeRegistry (정적 Enum 레지스트리)
```java
/**
 * 정적 Enum 코드 목록 - 자동 수집된 Enum 관리.
 * DB 설정으로 표시 여부, 순서, 라벨 오버라이드 가능.
 */
public interface StaticCodeRegistry {

    /**
     * 등록된 모든 정적 Enum 타입 조회
     */
    Set<Class<? extends Enum<?>>> getRegisteredEnums();

    /**
     * 특정 Enum 타입의 코드 아이템 조회
     */
    List<CodeManageItem> getCodeItems(Class<? extends Enum<?>> enumClass);

    /**
     * 코드 타입명으로 Enum 조회
     */
    Optional<Class<? extends Enum<?>>> findByCodeType(String codeType);
}
```

### 3.4 전체 Enum 자동 수집 시스템

#### 3.4.1 설계 원칙

| 원칙 | 설명 |
|------|------|
| **전체 수집** | 프로젝트 내 모든 Enum 자동 감지 |
| **DB 우선순위** | DB 설정이 Enum 기본값보다 우선 |
| **Optional 어노테이션** | `@ManagedCode`로 추가 메타데이터 제공 가능 |
| **제외 목록** | 내부용/시스템용 Enum 제외 가능 |

#### 3.4.2 우선순위 체계

```
┌─────────────────────────────────────────────────────────────┐
│                    우선순위 (높음 → 낮음)                      │
├─────────────────────────────────────────────────────────────┤
│ 1. DB 설정 (code_manage_config 테이블)                       │
│    - 표시 여부, 순서, 라벨 오버라이드, 그룹핑                   │
├─────────────────────────────────────────────────────────────┤
│ 2. @ManagedCode 어노테이션 (Optional)                        │
│    - displayName, description, group, displayOrder          │
├─────────────────────────────────────────────────────────────┤
│ 3. Enum 기본값                                               │
│    - name(), ordinal()                                       │
└─────────────────────────────────────────────────────────────┘
```

#### 3.4.3 어노테이션 설계 (Optional)

```java
/**
 * Enum을 코드 관리 시스템에 등록할 때 추가 메타데이터 제공.
 * 이 어노테이션이 없어도 Enum은 자동 수집됨.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ManagedCode {
    /** 표시명 (기본: Enum 클래스명) */
    String displayName() default "";

    /** 설명 */
    String description() default "";

    /** 그룹 (카테고리) */
    String group() default "GENERAL";

    /** 표시 순서 */
    int displayOrder() default 0;

    /** 관리 화면에서 숨김 여부 */
    boolean hidden() default false;
}

/**
 * Enum 필드(상수)에 추가 메타데이터 제공.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CodeValue {
    /** 표시 라벨 */
    String label() default "";

    /** 추가 설명 */
    String description() default "";

    /** 표시 순서 (기본: ordinal) */
    int order() default -1;

    /** 비활성화 여부 */
    boolean deprecated() default false;
}
```

#### 3.4.4 DB 설정 테이블

```sql
-- Enum 타입별 설정 (우선순위 최고)
CREATE TABLE code_manage_config (
    id              BIGINT PRIMARY KEY,
    code_type       VARCHAR(100) NOT NULL UNIQUE,  -- Enum 클래스명 또는 DynamicCodeType
    source          VARCHAR(50) NOT NULL,          -- CodeManageSource
    display_name    VARCHAR(200),                  -- 표시명 오버라이드
    description     VARCHAR(500),
    group_name      VARCHAR(100) DEFAULT 'GENERAL',
    display_order   INT DEFAULT 0,
    visible         BOOLEAN DEFAULT TRUE,          -- 관리 화면 표시 여부
    editable        BOOLEAN DEFAULT FALSE,         -- CRUD 가능 여부
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL
);

-- Enum 값별 설정 (라벨 오버라이드 등)
CREATE TABLE code_manage_value_config (
    id              BIGINT PRIMARY KEY,
    code_type       VARCHAR(100) NOT NULL,
    code_value      VARCHAR(100) NOT NULL,         -- Enum name()
    label           VARCHAR(200),                  -- 표시 라벨 오버라이드
    description     VARCHAR(500),
    display_order   INT,                           -- 순서 오버라이드
    active          BOOLEAN DEFAULT TRUE,          -- 활성화 여부
    metadata_json   JSONB,                         -- 추가 메타데이터
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,
    UNIQUE (code_type, code_value)
);
```

---

## 4. 구현 계획

### 4.1 Phase 1: 패키지 구조 및 기본 리팩토링

**목표:** model → domain 패키지 변경, Lombok 적용, 기본 정리

#### 4.1.1 패키지 구조 변경

```
backend/admin/src/main/java/com/example/admin/codemanage/
├── domain/                              # model → domain
│   ├── CodeManageSource.java           # 통합된 소스 타입
│   ├── DynamicCodeType.java            # 동적 DB 코드 타입
│   ├── SystemCommonCode.java           # JPA Entity (Lombok 적용)
│   └── CodeManageItem.java             # DTO Record (유지)
├── annotation/                          # 신규
│   ├── ManagedCode.java                # 클래스 레벨 어노테이션
│   └── CodeValue.java                  # 필드 레벨 어노테이션
├── registry/                            # 신규
│   ├── StaticCodeRegistry.java         # 인터페이스
│   ├── EnumCodeRegistry.java           # Enum 수집/관리 구현체
│   └── CodeManageConfigRepository.java # DB 설정 Repository
├── service/
│   ├── CodeManageQueryService.java     # 조회 서비스 (개선)
│   └── SystemCommonCodeService.java    # 동적 코드 CRUD
└── config/
    └── CodeManageAutoConfiguration.java # 자동 설정
```

#### 4.1.2 삭제 대상

- `CodeManageKind.java` - `CodeManageSource`로 병합
- `SystemCommonCodeType.java` - `DynamicCodeType` + DB 설정으로 대체

### 4.2 Phase 2: CodeManageSource 통합

**작업 내용:**

```java
// 파일: backend/admin/src/main/java/com/example/admin/codemanage/domain/CodeManageSource.java

package com.example.admin.codemanage.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 코드 관리 소스 타입.
 * 기존 CodeManageKind를 통합하여 소스와 특성을 함께 정의.
 */
@Getter
@RequiredArgsConstructor
public enum CodeManageSource {

    // === 정적 소스 (읽기 전용) ===
    STATIC_ENUM("프로젝트 Enum", false, true, false),

    // === 동적 소스 (CRUD 가능) ===
    DYNAMIC_DB("시스템 공통코드", true, false, true),

    // === 외부 연동 소스 ===
    DW("데이터웨어하우스", false, true, false),
    APPROVAL_GROUP("승인 그룹", false, true, false),

    // === 페더레이션 ===
    FEDERATED("외부 시스템 연동", false, true, false);

    private final String description;
    private final boolean editable;           // DB CRUD 가능
    private final boolean readOnly;           // 읽기 전용
    private final boolean requiresDbStorage;  // DB 저장 필요

    /**
     * 정적 Enum 기반 소스인지 확인
     */
    public boolean isStaticEnum() {
        return this == STATIC_ENUM;
    }

    /**
     * 외부 소스인지 확인
     */
    public boolean isExternal() {
        return this == DW || this == FEDERATED;
    }
}
```

### 4.3 Phase 3: Enum 자동 수집 시스템 구현

#### 4.3.1 EnumCodeRegistry 구현

```java
// 파일: backend/admin/src/main/java/com/example/admin/codemanage/registry/EnumCodeRegistry.java

package com.example.admin.codemanage.registry;

import com.example.admin.codemanage.annotation.ManagedCode;
import com.example.admin.codemanage.annotation.CodeValue;
import com.example.admin.codemanage.domain.CodeManageItem;
import com.example.admin.codemanage.domain.CodeManageSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 프로젝트 내 모든 Enum을 자동 수집하고 관리하는 레지스트리.
 *
 * 수집 방식:
 * 1. ClassPathScanning으로 com.example 패키지 내 모든 Enum 탐지
 * 2. 제외 목록에 없는 Enum 자동 등록
 * 3. @ManagedCode 어노테이션이 있으면 메타데이터 추출
 * 4. DB 설정이 있으면 오버라이드 적용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnumCodeRegistry implements StaticCodeRegistry {

    private final CodeManageConfigRepository configRepository;

    // 수집된 Enum 캐시
    private final Map<String, Class<? extends Enum<?>>> enumTypeMap = new ConcurrentHashMap<>();
    private final Map<String, List<CodeManageItem>> enumItemsCache = new ConcurrentHashMap<>();

    // 제외할 Enum 목록 (내부/시스템용)
    private static final Set<String> EXCLUDED_ENUMS = Set.of(
        "CodeManageSource",
        "AuditPartitionType",
        "HashAlgorithm"
    );

    // 스캔 대상 패키지
    private static final String[] SCAN_PACKAGES = {
        "com.example.common",
        "com.example.auth",
        "com.example.admin",
        "com.example.draft",
        "com.example.approval",
        "com.example.audit",
        "com.example.file",
        "com.example.dw"
    };

    @PostConstruct
    public void init() {
        scanAndRegisterEnums();
        log.info("EnumCodeRegistry initialized. Registered {} enum types", enumTypeMap.size());
    }

    /**
     * 모든 패키지에서 Enum 스캔 및 등록
     */
    @SuppressWarnings("unchecked")
    private void scanAndRegisterEnums() {
        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Enum.class));

        for (String basePackage : SCAN_PACKAGES) {
            Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);

            for (BeanDefinition bd : candidates) {
                try {
                    Class<?> clazz = Class.forName(bd.getBeanClassName());
                    if (clazz.isEnum() && !EXCLUDED_ENUMS.contains(clazz.getSimpleName())) {
                        Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) clazz;
                        String codeType = enumClass.getSimpleName();
                        enumTypeMap.put(codeType, enumClass);
                        log.debug("Registered enum: {}", codeType);
                    }
                } catch (ClassNotFoundException e) {
                    log.warn("Failed to load enum class: {}", bd.getBeanClassName(), e);
                }
            }
        }
    }

    @Override
    public Set<Class<? extends Enum<?>>> getRegisteredEnums() {
        return new HashSet<>(enumTypeMap.values());
    }

    @Override
    public List<CodeManageItem> getCodeItems(Class<? extends Enum<?>> enumClass) {
        String codeType = enumClass.getSimpleName();
        return enumItemsCache.computeIfAbsent(codeType, k -> buildCodeItems(enumClass));
    }

    @Override
    public Optional<Class<? extends Enum<?>>> findByCodeType(String codeType) {
        return Optional.ofNullable(enumTypeMap.get(codeType));
    }

    /**
     * Enum 클래스로부터 CodeManageItem 목록 생성
     * 우선순위: DB 설정 > @CodeValue 어노테이션 > Enum 기본값
     */
    private List<CodeManageItem> buildCodeItems(Class<? extends Enum<?>> enumClass) {
        String codeType = enumClass.getSimpleName();
        Enum<?>[] constants = enumClass.getEnumConstants();

        // DB에서 값별 설정 조회
        Map<String, CodeValueConfig> dbConfigs = configRepository
            .findValueConfigsByCodeType(codeType)
            .stream()
            .collect(Collectors.toMap(
                CodeValueConfig::getCodeValue,
                config -> config
            ));

        List<CodeManageItem> items = new ArrayList<>();

        for (Enum<?> constant : constants) {
            String code = constant.name();

            // 1. DB 설정 확인 (최우선)
            CodeValueConfig dbConfig = dbConfigs.get(code);
            if (dbConfig != null && !dbConfig.isActive()) {
                continue; // 비활성화된 코드 제외
            }

            // 2. 어노테이션 확인
            CodeValue annotation = getCodeValueAnnotation(enumClass, code);
            if (annotation != null && annotation.deprecated()) {
                continue; // deprecated 코드 제외
            }

            // 3. 값 결정 (우선순위 적용)
            String label = resolveLabel(code, dbConfig, annotation);
            String description = resolveDescription(dbConfig, annotation);
            int order = resolveOrder(constant.ordinal(), dbConfig, annotation);
            String metadataJson = dbConfig != null ? dbConfig.getMetadataJson() : null;

            items.add(new CodeManageItem(
                code,
                label,
                description,
                codeType,
                CodeManageSource.STATIC_ENUM,
                order,
                true,  // active
                null,  // parentCode
                metadataJson
            ));
        }

        // 순서 정렬
        items.sort(Comparator.comparingInt(CodeManageItem::displayOrder));
        return items;
    }

    private CodeValue getCodeValueAnnotation(Class<? extends Enum<?>> enumClass, String code) {
        try {
            Field field = enumClass.getField(code);
            return field.getAnnotation(CodeValue.class);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private String resolveLabel(String code, CodeValueConfig dbConfig, CodeValue annotation) {
        if (dbConfig != null && dbConfig.getLabel() != null) {
            return dbConfig.getLabel();
        }
        if (annotation != null && !annotation.label().isEmpty()) {
            return annotation.label();
        }
        return code; // 기본값: Enum name
    }

    private String resolveDescription(CodeValueConfig dbConfig, CodeValue annotation) {
        if (dbConfig != null && dbConfig.getDescription() != null) {
            return dbConfig.getDescription();
        }
        if (annotation != null && !annotation.description().isEmpty()) {
            return annotation.description();
        }
        return null;
    }

    private int resolveOrder(int ordinal, CodeValueConfig dbConfig, CodeValue annotation) {
        if (dbConfig != null && dbConfig.getDisplayOrder() != null) {
            return dbConfig.getDisplayOrder();
        }
        if (annotation != null && annotation.order() >= 0) {
            return annotation.order();
        }
        return ordinal;
    }

    /**
     * 캐시 무효화 (DB 설정 변경 시 호출)
     */
    public void invalidateCache(String codeType) {
        if (codeType != null) {
            enumItemsCache.remove(codeType);
        } else {
            enumItemsCache.clear();
        }
    }
}
```

### 4.4 Phase 4: CodeManageQueryService 개선

```java
// 파일: backend/admin/src/main/java/com/example/admin/codemanage/service/CodeManageQueryService.java

package com.example.admin.codemanage.service;

import com.example.admin.codemanage.domain.CodeManageItem;
import com.example.admin.codemanage.domain.CodeManageSource;
import com.example.admin.codemanage.domain.DynamicCodeType;
import com.example.admin.codemanage.registry.EnumCodeRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 코드 관리 조회 서비스.
 *
 * 역할:
 * - 모든 소스(STATIC_ENUM, DYNAMIC_DB, DW, APPROVAL_GROUP)에서 코드 집계
 * - 캐싱을 통한 성능 최적화
 * - 화면 초기 로드용 전체 코드 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodeManageQueryService {

    private final EnumCodeRegistry enumCodeRegistry;
    private final SystemCommonCodeService systemCommonCodeService;
    private final ApprovalGroupRepository approvalGroupRepository;
    private final DwCodeClient dwCodeClient; // DW 연동 클라이언트

    /**
     * 전체 코드 집계 (화면 초기 로드용)
     *
     * @return 소스별로 그룹핑된 코드 목록
     */
    @Cacheable(cacheNames = CacheNames.COMMON_CODE_AGGREGATES, key = "'ALL'")
    public Map<String, List<CodeManageItem>> aggregateAll() {
        Map<String, List<CodeManageItem>> result = new LinkedHashMap<>();

        // 1. 정적 Enum 코드
        enumCodeRegistry.getRegisteredEnums().forEach(enumClass -> {
            String codeType = enumClass.getSimpleName();
            List<CodeManageItem> items = enumCodeRegistry.getCodeItems(enumClass);
            if (!items.isEmpty()) {
                result.put(codeType, items);
            }
        });

        // 2. 동적 DB 코드
        for (DynamicCodeType type : DynamicCodeType.values()) {
            List<CodeManageItem> items = systemCommonCodeService.findByType(type);
            if (!items.isEmpty()) {
                result.put(type.name(), items);
            }
        }

        // 3. 승인 그룹
        List<CodeManageItem> approvalGroups = approvalGroupRepository.findAllActive()
            .stream()
            .map(this::toCodeManageItem)
            .collect(Collectors.toList());
        if (!approvalGroups.isEmpty()) {
            result.put("APPROVAL_GROUP", approvalGroups);
        }

        // 4. DW 코드 (필요시)
        // Map<String, List<CodeManageItem>> dwCodes = dwCodeClient.fetchAllCodes();
        // result.putAll(dwCodes);

        return result;
    }

    /**
     * 특정 코드 타입 조회
     */
    @Cacheable(cacheNames = CacheNames.COMMON_CODE_AGGREGATES, key = "#codeType")
    public List<CodeManageItem> findByCodeType(String codeType) {
        // 1. 정적 Enum 확인
        Optional<Class<? extends Enum<?>>> enumClass = enumCodeRegistry.findByCodeType(codeType);
        if (enumClass.isPresent()) {
            return enumCodeRegistry.getCodeItems(enumClass.get());
        }

        // 2. 동적 코드 확인
        try {
            DynamicCodeType dynamicType = DynamicCodeType.valueOf(codeType);
            return systemCommonCodeService.findByType(dynamicType);
        } catch (IllegalArgumentException ignored) {
        }

        // 3. 승인 그룹 확인
        if ("APPROVAL_GROUP".equals(codeType)) {
            return approvalGroupRepository.findAllActive()
                .stream()
                .map(this::toCodeManageItem)
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    /**
     * 코드 타입 메타데이터 조회 (관리 화면용)
     */
    public List<CodeTypeInfo> getCodeTypeInfos() {
        List<CodeTypeInfo> infos = new ArrayList<>();

        // 정적 Enum
        enumCodeRegistry.getRegisteredEnums().forEach(enumClass -> {
            infos.add(CodeTypeInfo.builder()
                .codeType(enumClass.getSimpleName())
                .source(CodeManageSource.STATIC_ENUM)
                .editable(false)
                .itemCount(enumCodeRegistry.getCodeItems(enumClass).size())
                .build());
        });

        // 동적 코드
        for (DynamicCodeType type : DynamicCodeType.values()) {
            infos.add(CodeTypeInfo.builder()
                .codeType(type.name())
                .displayName(type.getDescription())
                .source(CodeManageSource.DYNAMIC_DB)
                .editable(true)
                .itemCount(systemCommonCodeService.countByType(type))
                .build());
        }

        // 승인 그룹
        infos.add(CodeTypeInfo.builder()
            .codeType("APPROVAL_GROUP")
            .displayName("승인 그룹")
            .source(CodeManageSource.APPROVAL_GROUP)
            .editable(false)
            .itemCount(approvalGroupRepository.countActive())
            .build());

        return infos;
    }

    private CodeManageItem toCodeManageItem(ApprovalGroup group) {
        return new CodeManageItem(
            group.getGroupCode(),
            group.getName(),
            group.getDescription(),
            "APPROVAL_GROUP",
            CodeManageSource.APPROVAL_GROUP,
            group.getDisplayOrder(),
            group.isActive(),
            null,
            null
        );
    }
}
```

### 4.5 Phase 5: 마이그레이션 및 정리

#### 4.5.1 삭제 대상 파일

```
- backend/admin/src/main/java/com/example/admin/codemanage/model/CodeManageKind.java
- backend/admin/src/main/java/com/example/admin/codemanage/model/SystemCommonCodeType.java
```

#### 4.5.2 마이그레이션 SQL

```sql
-- 기존 SystemCommonCodeType 기반 데이터를 새 구조로 마이그레이션

-- 1. 코드 타입 설정 테이블 생성
CREATE TABLE IF NOT EXISTS code_manage_config (
    id              BIGSERIAL PRIMARY KEY,
    code_type       VARCHAR(100) NOT NULL UNIQUE,
    source          VARCHAR(50) NOT NULL,
    display_name    VARCHAR(200),
    description     VARCHAR(500),
    group_name      VARCHAR(100) DEFAULT 'GENERAL',
    display_order   INT DEFAULT 0,
    visible         BOOLEAN DEFAULT TRUE,
    editable        BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 2. 코드 값 설정 테이블 생성
CREATE TABLE IF NOT EXISTS code_manage_value_config (
    id              BIGSERIAL PRIMARY KEY,
    code_type       VARCHAR(100) NOT NULL,
    code_value      VARCHAR(100) NOT NULL,
    label           VARCHAR(200),
    description     VARCHAR(500),
    display_order   INT,
    active          BOOLEAN DEFAULT TRUE,
    metadata_json   JSONB,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (code_type, code_value)
);

-- 3. 인덱스 생성
CREATE INDEX idx_code_manage_config_source ON code_manage_config(source);
CREATE INDEX idx_code_manage_value_config_code_type ON code_manage_value_config(code_type);

-- 4. 기존 system_common_codes 데이터는 유지 (DynamicCodeType용)
-- NOTICE_CATEGORY, ALERT_CHANNEL, CUSTOM 타입은 계속 사용
```

---

## 5. 구현 일정

| Phase | 작업 내용 | 예상 범위 |
|-------|----------|----------|
| **Phase 1** | 패키지 구조 변경, Lombok 적용 | 기본 리팩토링 |
| **Phase 2** | CodeManageSource 통합, CodeManageKind 삭제 | Enum 정리 |
| **Phase 3** | EnumCodeRegistry 구현, 어노테이션 추가 | 핵심 기능 |
| **Phase 4** | CodeManageQueryService 개선, 캐싱 적용 | 서비스 레이어 |
| **Phase 5** | 마이그레이션, 기존 코드 삭제 | 정리 |

---

## 6. 베스트 프랙티스 적용

### 6.1 Spring Framework 권장사항

1. **ClassPathScanningCandidateComponentProvider 활용**
   - Spring의 표준 컴포넌트 스캔 메커니즘 활용
   - 커스텀 필터로 Enum만 선택적 스캔

2. **캐싱 전략**
   - `@Cacheable`로 조회 성능 최적화
   - 설정 변경 시 `@CacheEvict`로 무효화

3. **트랜잭션 관리**
   - 읽기 전용 트랜잭션 (`@Transactional(readOnly = true)`)

### 6.2 디자인 패턴

1. **Registry 패턴**
   - `StaticCodeRegistry` 인터페이스로 추상화
   - 확장 가능한 구조

2. **Strategy 패턴**
   - `CodeManageSource`별로 다른 조회 전략

3. **Decorator 패턴**
   - DB 설정이 어노테이션/기본값을 오버라이드

### 6.3 확장 고려사항

1. **새 Enum 추가 시**: 별도 등록 불필요 (자동 수집)
2. **라벨 변경 시**: DB 설정으로 오버라이드
3. **새 소스 추가 시**: `CodeManageSource` Enum에 추가

---

## 7. 참고사항

### 7.1 기존 SystemCommonCode Entity 유지

`SystemCommonCode` JPA Entity는 **동적 코드**(NOTICE_CATEGORY, ALERT_CHANNEL 등)용으로 계속 사용됩니다. `metadataJson` 필드는 유연한 확장을 위해 유지합니다.

### 7.2 ApprovalGroup 통합

`ApprovalGroup` Entity를 `CodeManageSource.APPROVAL_GROUP`으로 통합하여 코드 관리 화면에서 함께 조회할 수 있습니다.

### 7.3 하위 호환성

- 기존 API 엔드포인트 유지
- 기존 캐시 키 구조 호환
- 점진적 마이그레이션 지원
