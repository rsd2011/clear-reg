package com.example.server.web;

import com.example.admin.codegroup.service.CodeGroupQueryService;
import com.example.admin.codegroup.service.CodeGroupService;
import com.example.admin.codegroup.domain.CodeGroupSource;
import com.example.admin.codegroup.domain.DynamicCodeType;
import com.example.admin.codegroup.domain.CodeItem;
import com.example.admin.codegroup.dto.CodeGroupItem;
import com.example.admin.codegroup.dto.CodeGroupItemRequest;
import com.example.admin.codegroup.dto.CodeGroupItemResponse;
import com.example.admin.codegroup.dto.CodeGroupInfo;
import com.example.admin.codegroup.dto.MigrationRequest;
import com.example.admin.codegroup.dto.MigrationResponse;
import com.example.admin.codegroup.dto.MigrationResult;
import com.example.admin.codegroup.dto.MigrationStatusResponse;
import com.example.admin.codegroup.registry.StaticCodeRegistry;
import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.RequirePermission;
import com.example.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 코드 그룹 관리 통합 REST API 컨트롤러.
 *
 * <p>정적 Enum, 동적 DB 코드, DW 연동 코드, 승인 그룹 등
 * 모든 소스의 코드를 통합 조회하는 엔드포인트를 제공합니다.</p>
 */
@RestController
@RequestMapping("/api/code-groups")
@Tag(name = "Code Group Management", description = "코드 그룹 관리 통합 API")
@RequiredArgsConstructor
public class CodeGroupController {

    private final CodeGroupQueryService queryService;
    private final CodeGroupService codeGroupService;
    private final StaticCodeRegistry staticCodeRegistry;

    // ========== 조회 API ==========

    /**
     * 전체 코드 집계 조회 (화면 초기 로드용)
     *
     * <p>모든 소스(정적 Enum, 동적 DB, DW, 승인 그룹)에서 코드를 수집하여
     * 그룹 코드별로 그룹핑하여 반환합니다.</p>
     */
    @GetMapping
    @RequirePermission(feature = FeatureCode.COMMON_CODE, action = ActionCode.READ)
    @Operation(
            summary = "전체 코드 조회",
            description = "화면 초기 로드용. 모든 소스의 코드를 그룹 코드별로 그룹핑하여 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Map.class)))
    })
    public ResponseEntity<Map<String, List<CodeGroupItem>>> getAllCodes() {
        Map<String, List<CodeGroupItem>> codes = queryService.aggregateAll();
        return ResponseEntity.ok(codes);
    }

    /**
     * 통합 코드 항목 목록 조회 (메인 API v2)
     *
     * <p>모든 소스의 코드를 플랫 리스트로 반환합니다. 페이징 없음.</p>
     */
    @GetMapping("/items")
    @RequirePermission(feature = FeatureCode.COMMON_CODE, action = ActionCode.READ)
    @Operation(
            summary = "통합 코드 항목 조회",
            description = "모든 소스의 코드를 플랫 리스트로 반환합니다. 소스, 그룹코드, 활성상태, 검색어로 필터링 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<List<CodeGroupItemResponse>> getAllItems(
            @Parameter(description = "소스 필터 (쉼표 구분: STATIC_ENUM, DYNAMIC_DB, DW 등)")
            @RequestParam(required = false) List<CodeGroupSource> source,

            @Parameter(description = "그룹 코드 필터")
            @RequestParam(required = false) String groupCode,

            @Parameter(description = "활성 상태 필터")
            @RequestParam(required = false) Boolean active,

            @Parameter(description = "검색어 (항목코드/항목명)")
            @RequestParam(required = false) String search
    ) {
        List<CodeGroupItemResponse> items = queryService.findAllItems(source, groupCode, active, search);
        return ResponseEntity.ok(items);
    }

    /**
     * 특정 그룹 코드의 코드 목록 조회
     */
    @GetMapping("/{groupCode}")
    @RequirePermission(feature = FeatureCode.COMMON_CODE, action = ActionCode.READ)
    @Operation(
            summary = "특정 그룹 코드 조회",
            description = "지정된 그룹 코드의 코드 목록을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "그룹 코드를 찾을 수 없음")
    })
    public ResponseEntity<List<CodeGroupItem>> getByGroupCode(
            @Parameter(description = "그룹 코드명 (예: USER_STATUS, NOTICE_CATEGORY)")
            @PathVariable String groupCode) {

        List<CodeGroupItem> items = queryService.findByGroupCode(groupCode);
        if (items.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(items);
    }

    /**
     * 여러 그룹 코드 일괄 조회
     */
    @GetMapping("/batch")
    @RequirePermission(feature = FeatureCode.COMMON_CODE, action = ActionCode.READ)
    @Operation(
            summary = "여러 그룹 코드 일괄 조회",
            description = "여러 그룹 코드를 한 번에 조회합니다. 쉼표로 구분된 그룹 코드명을 전달합니다."
    )
    public ResponseEntity<Map<String, List<CodeGroupItem>>> getByGroupCodes(
            @Parameter(description = "그룹 코드명 목록 (쉼표 구분)")
            @RequestParam List<String> groups) {

        Map<String, List<CodeGroupItem>> result = queryService.findByGroupCodes(groups);
        return ResponseEntity.ok(result);
    }

    /**
     * 코드 그룹 메타정보 목록 조회 (관리 화면용)
     */
    @GetMapping("/meta")
    @RequirePermission(feature = FeatureCode.COMMON_CODE, action = ActionCode.READ)
    @Operation(
            summary = "코드 그룹 메타정보 조회",
            description = "관리 화면에서 사용할 코드 그룹 목록과 메타정보를 반환합니다."
    )
    public ResponseEntity<List<CodeGroupInfo>> getCodeGroupInfos() {
        List<CodeGroupInfo> infos = queryService.getCodeGroupInfos();
        return ResponseEntity.ok(infos);
    }

    // ========== 수정 API ==========

    /**
     * 코드 생성 (DYNAMIC_DB, LOCALE_COUNTRY, LOCALE_LANGUAGE)
     *
     * <p>생성 가능한 소스 타입:</p>
     * <ul>
     *   <li>DYNAMIC_DB: 동적 공통코드</li>
     *   <li>LOCALE_COUNTRY: 커스텀 국가 코드 추가</li>
     *   <li>LOCALE_LANGUAGE: 커스텀 언어 코드 추가</li>
     * </ul>
     *
     * <p>STATIC_ENUM, DW, APPROVAL_GROUP은 생성 불가</p>
     */
    @PostMapping("/items")
    @RequirePermission(feature = FeatureCode.COMMON_CODE, action = ActionCode.CREATE)
    @Operation(
            summary = "코드 생성",
            description = """
                    새로운 코드를 생성합니다.
                    - DYNAMIC_DB: 동적 공통코드 생성
                    - LOCALE_COUNTRY: 커스텀 국가 코드 추가
                    - LOCALE_LANGUAGE: 커스텀 언어 코드 추가
                    - STATIC_ENUM, DW, APPROVAL_GROUP: 생성 불가
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "유효성 검사 실패"),
            @ApiResponse(responseCode = "403", description = "생성 불가능한 소스 (STATIC_ENUM, DW, APPROVAL_GROUP)"),
            @ApiResponse(responseCode = "409", description = "중복 코드")
    })
    public ResponseEntity<CodeGroupItemResponse> createItem(
            @Valid @RequestBody CodeGroupItemRequest request,
            CurrentUser currentUser
    ) {
        String groupCode = request.groupCode();
        String itemCode = request.itemCode();

        // 소스 결정: 요청에 명시 > 기존 그룹에서 추론 > 기본값(DYNAMIC_DB)
        CodeGroupSource source = determineSourceForCreate(request.source(), groupCode);

        // 생성 가능 여부 체크
        if (!source.isCreatable()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // DYNAMIC_DB인 경우 DynamicCodeType 검증
        if (source.isDynamic() && !DynamicCodeType.isDynamicType(groupCode)) {
            throw new IllegalArgumentException("등록되지 않은 동적 코드 타입입니다: " + groupCode);
        }

        CodeItem savedItem = codeGroupService.createItem(
                source,
                groupCode,
                itemCode,
                request.itemName(),
                request.getDisplayOrderOrDefault(),
                request.isActiveOrDefault(),
                request.description(),
                request.metadataJson(),
                currentUser.username()
        );

        CodeGroupItemResponse response = CodeGroupItemResponse.fromEntity(savedItem);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 생성 시 소스 타입 결정.
     *
     * @param requestedSource 요청에 명시된 소스 (nullable)
     * @param groupCode       그룹 코드
     * @return 결정된 소스 타입
     */
    private CodeGroupSource determineSourceForCreate(CodeGroupSource requestedSource, String groupCode) {
        // 1. 요청에 명시된 소스가 있으면 사용
        if (requestedSource != null) {
            return requestedSource;
        }

        // 2. Static Enum에 등록된 그룹이면 생성 불가 (STATIC_ENUM은 creatable=false)
        if (staticCodeRegistry.hasGroup(groupCode)) {
            return CodeGroupSource.STATIC_ENUM;
        }

        // 3. 기존 그룹이 있으면 해당 소스 사용
        return codeGroupService.findFirstGroupByCode(groupCode)
                .map(group -> group.getSource())
                .orElse(CodeGroupSource.DYNAMIC_DB);  // 4. 기본값
    }

    /**
     * 코드 수정 (ID로)
     */
    @PutMapping("/items/{id}")
    @RequirePermission(feature = FeatureCode.COMMON_CODE, action = ActionCode.UPDATE)
    @Operation(
            summary = "코드 수정",
            description = "기존 코드의 라벨, 순서, 설명 등을 수정합니다. Static Enum은 오버라이드 레코드가 생성/수정됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "유효성 검사 실패"),
            @ApiResponse(responseCode = "403", description = "읽기 전용 소스 수정 시도"),
            @ApiResponse(responseCode = "404", description = "코드 없음")
    })
    public ResponseEntity<CodeGroupItemResponse> updateItemById(
            @PathVariable UUID id,
            @Valid @RequestBody CodeGroupItemRequest request,
            CurrentUser currentUser
    ) {
        CodeItem existing = codeGroupService.findItemById(id)
                .orElseThrow(() -> new IllegalArgumentException("코드가 존재하지 않습니다: " + id));

        CodeItem savedItem = codeGroupService.updateItem(
                id,
                request.itemName(),
                request.displayOrder() != null ? request.displayOrder() : existing.getDisplayOrder(),
                request.active() != null ? request.active() : existing.isActive(),
                request.description(),
                request.metadataJson(),
                currentUser.username()
        );

        CodeGroupItemResponse response = CodeGroupItemResponse.fromEntity(savedItem);
        return ResponseEntity.ok(response);
    }

    /**
     * 코드 수정 (groupCode + itemCode로)
     *
     * <p>소스 타입에 따른 수정 동작:</p>
     * <ul>
     *   <li>STATIC_ENUM: 오버라이드 레코드 생성/수정</li>
     *   <li>DYNAMIC_DB: 기존 레코드 수정</li>
     *   <li>LOCALE_COUNTRY/LANGUAGE: 기존 레코드 수정 (builtIn 항목도 오버라이드 가능)</li>
     *   <li>DW, APPROVAL_GROUP: 수정 불가 (읽기 전용)</li>
     * </ul>
     */
    @PutMapping("/items")
    @RequirePermission(feature = FeatureCode.COMMON_CODE, action = ActionCode.UPDATE)
    @Operation(
            summary = "코드 수정 (그룹+항목코드로)",
            description = """
                    groupCode와 itemCode로 코드를 찾아 수정합니다.
                    - STATIC_ENUM: 오버라이드 레코드 생성/수정
                    - DYNAMIC_DB, LOCALE_*: 기존 레코드 수정
                    - DW, APPROVAL_GROUP: 수정 불가
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "유효성 검사 실패 또는 필수 파라미터 누락"),
            @ApiResponse(responseCode = "403", description = "읽기 전용 소스 수정 시도")
    })
    public ResponseEntity<CodeGroupItemResponse> updateItemByGroupAndCode(
            @Parameter(description = "그룹 코드", required = true)
            @RequestParam String groupCode,

            @Parameter(description = "항목 코드", required = true)
            @RequestParam String itemCode,

            @Parameter(description = "소스 타입 (미지정 시 자동 추론)")
            @RequestParam(required = false) CodeGroupSource source,

            @Valid @RequestBody CodeGroupItemRequest request,
            CurrentUser currentUser
    ) {
        // 소스 결정: 요청 파라미터 > 기존 그룹에서 추론
        CodeGroupSource resolvedSource = determineSourceForUpdate(source, groupCode);

        // 읽기 전용 소스는 수정 불가
        if (!resolvedSource.isEditable()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Static Enum 오버라이드 처리
        if (resolvedSource.isStaticEnum()) {
            // Static Enum에 해당 itemCode가 실제로 존재하는지 검증
            boolean enumValueExists = staticCodeRegistry.getItems(groupCode).stream()
                    .anyMatch(item -> item.itemCode().equals(itemCode));
            if (!enumValueExists) {
                throw new IllegalArgumentException("Static Enum에 존재하지 않는 코드 값입니다: " + itemCode);
            }

            CodeItem savedItem = codeGroupService.createOrUpdateOverride(
                    groupCode,
                    itemCode,
                    request.itemName(),
                    request.displayOrder(),
                    request.description(),
                    request.metadataJson(),
                    currentUser.username()
            );
            CodeGroupItemResponse response = CodeGroupItemResponse.fromEntity(savedItem);
            return ResponseEntity.ok(response);
        }

        // DYNAMIC_DB, LOCALE_* 코드 수정
        CodeItem savedItem = codeGroupService.updateItemByGroupAndCode(
                resolvedSource,
                groupCode,
                itemCode,
                request.itemName(),
                request.displayOrder(),
                request.active() != null ? request.active() : true,
                request.description(),
                request.metadataJson(),
                currentUser.username()
        );

        CodeGroupItemResponse response = CodeGroupItemResponse.fromEntity(savedItem);
        return ResponseEntity.ok(response);
    }

    /**
     * 수정 시 소스 타입 결정.
     */
    private CodeGroupSource determineSourceForUpdate(CodeGroupSource requestedSource, String groupCode) {
        // 1. 요청에 명시된 소스가 있으면 사용
        if (requestedSource != null) {
            return requestedSource;
        }

        // 2. Static Enum에 등록된 그룹이면 STATIC_ENUM
        if (staticCodeRegistry.hasGroup(groupCode)) {
            return CodeGroupSource.STATIC_ENUM;
        }

        // 3. 기존 그룹에서 소스 추론
        return codeGroupService.findFirstGroupByCode(groupCode)
                .map(group -> group.getSource())
                .orElse(CodeGroupSource.DYNAMIC_DB);
    }

    /**
     * 코드 삭제 (통합 API)
     *
     * <p>소스 타입에 따른 삭제 동작:</p>
     * <ul>
     *   <li>STATIC_ENUM: DB 오버라이드 레코드 삭제 → Enum 원본값 복원</li>
     *   <li>DYNAMIC_DB: 영구 삭제</li>
     *   <li>LOCALE_COUNTRY/LOCALE_LANGUAGE (builtIn=true): ISO 원본 복원</li>
     *   <li>LOCALE_COUNTRY/LOCALE_LANGUAGE (builtIn=false): 영구 삭제 (커스텀 항목)</li>
     *   <li>DW, APPROVAL_GROUP: 삭제 불가 (읽기 전용)</li>
     * </ul>
     */
    @DeleteMapping("/items/{id}")
    @RequirePermission(feature = FeatureCode.COMMON_CODE, action = ActionCode.DELETE)
    @Operation(
            summary = "코드 삭제",
            description = """
                    소스 타입에 따라 자동으로 적절한 삭제 동작을 수행합니다.
                    - STATIC_ENUM: 오버라이드 삭제 후 Enum 원본값 복원
                    - DYNAMIC_DB: 영구 삭제
                    - LOCALE_COUNTRY/LANGUAGE (builtIn): ISO 원본 복원
                    - LOCALE_COUNTRY/LANGUAGE (custom): 영구 삭제
                    - DW, APPROVAL_GROUP: 삭제 불가
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "400", description = "삭제할 수 없는 소스 (DW, APPROVAL_GROUP)"),
            @ApiResponse(responseCode = "404", description = "코드 없음")
    })
    public ResponseEntity<Void> deleteItem(@PathVariable UUID id) {
        codeGroupService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Static Enum 오버라이드 삭제
     *
     * @deprecated 대신 DELETE /api/code-groups/items/{id} 를 사용하세요.
     *             통합 삭제 API가 소스 타입에 따라 자동으로 적절한 동작을 수행합니다.
     */
    @Deprecated(since = "2025.01", forRemoval = true)
    @DeleteMapping("/items/{id}/override")
    @RequirePermission(feature = FeatureCode.COMMON_CODE, action = ActionCode.DELETE)
    @Operation(
            summary = "[Deprecated] Static Enum 오버라이드 삭제",
            description = "⚠️ Deprecated: DELETE /api/code-groups/items/{id} 를 사용하세요.",
            deprecated = true
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "오버라이드 레코드 없음")
    })
    public ResponseEntity<Void> deleteOverride(@PathVariable UUID id) {
        codeGroupService.deleteItem(id);  // 통합 API로 위임
        return ResponseEntity.noContent().build();
    }

    // ========== 마이그레이션 API ==========

    /**
     * 마이그레이션 상태 조회
     */
    @GetMapping("/migration/status")
    @RequirePermission(feature = FeatureCode.COMMON_CODE, action = ActionCode.READ)
    @Operation(
            summary = "마이그레이션 상태 조회",
            description = "Enum과 DB 간의 groupCode 불일치 상태를 조회합니다."
    )
    public ResponseEntity<MigrationStatusResponse> getMigrationStatus() {
        MigrationStatusResponse status = queryService.getMigrationStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * 마이그레이션 실행
     */
    @PostMapping("/migration")
    @RequirePermission(feature = FeatureCode.COMMON_CODE, action = ActionCode.UPDATE)
    @Operation(
            summary = "마이그레이션 실행",
            description = "DB의 groupCode를 일괄 변경합니다. 그룹 ID로 대상을 식별합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "마이그레이션 성공"),
            @ApiResponse(responseCode = "400", description = "그룹을 찾을 수 없음 또는 newGroupCode가 Enum에 없음"),
            @ApiResponse(responseCode = "409", description = "newGroupCode에 이미 DB 레코드 존재")
    })
    public ResponseEntity<MigrationResponse> migrate(
            @Valid @RequestBody MigrationRequest request
    ) {
        String newGroupCode = request.newGroupCode();

        // newGroupCode가 Enum에 존재하는지 확인
        if (!staticCodeRegistry.hasGroup(newGroupCode)) {
            return ResponseEntity.badRequest()
                    .body(MigrationResponse.failure(request.id(), newGroupCode,
                            "새 그룹 코드가 Enum에 존재하지 않습니다: " + newGroupCode));
        }

        try {
            MigrationResult result = codeGroupService.migrate(request.id(), newGroupCode);
            return ResponseEntity.ok(MigrationResponse.success(
                    result.migratedCount(),
                    result.groupId(),
                    result.oldGroupCode(),
                    result.newGroupCode()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(MigrationResponse.failure(request.id(), newGroupCode, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(MigrationResponse.failure(request.id(), newGroupCode, e.getMessage()));
        }
    }

    /**
     * DB 전용 그룹 코드 삭제
     */
    @DeleteMapping("/migration/{groupCode}")
    @RequirePermission(feature = FeatureCode.COMMON_CODE, action = ActionCode.DELETE)
    @Operation(
            summary = "DB 전용 그룹 코드 삭제",
            description = "Enum에 없는 DB 전용 그룹 코드를 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "400", description = "Enum에 존재하는 그룹 코드 삭제 시도")
    })
    public ResponseEntity<Void> deleteDbOnlyGroupCode(@PathVariable String groupCode) {
        // Enum에 존재하는 그룹 코드는 삭제 불가
        if (staticCodeRegistry.hasGroup(groupCode)) {
            return ResponseEntity.badRequest().build();
        }

        codeGroupService.deleteByGroupCode(groupCode);
        return ResponseEntity.noContent().build();
    }

    // ========== 유틸리티 API ==========

    /**
     * 캐시 무효화 (관리자 전용)
     */
    @PostMapping("/cache/evict")
    @RequirePermission(feature = FeatureCode.COMMON_CODE, action = ActionCode.UPDATE)
    @Operation(
            summary = "캐시 무효화",
            description = "코드 그룹 관리 캐시를 무효화합니다. 특정 그룹 코드를 지정하거나 전체 캐시를 무효화할 수 있습니다."
    )
    public ResponseEntity<Void> evictCache(
            @Parameter(description = "무효화할 그룹 코드 (미지정 시 전체)")
            @RequestParam(required = false) String groupCode) {

        queryService.evictCache(groupCode);
        return ResponseEntity.ok().build();
    }
}
