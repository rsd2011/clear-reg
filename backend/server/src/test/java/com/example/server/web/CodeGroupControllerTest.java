package com.example.server.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.admin.codegroup.domain.CodeGroupSource;
import com.example.admin.codegroup.domain.CodeItem;
import com.example.admin.codegroup.dto.CodeGroupInfo;
import com.example.admin.codegroup.dto.CodeGroupItem;
import com.example.admin.codegroup.dto.CodeGroupItemResponse;
import com.example.admin.codegroup.dto.MigrationResult;
import com.example.admin.codegroup.dto.MigrationStatusResponse;
import com.example.admin.codegroup.registry.StaticCodeRegistry;
import com.example.admin.codegroup.service.CodeGroupQueryService;
import com.example.admin.codegroup.service.CodeGroupService;
import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.PermissionEvaluator;
import com.example.admin.permission.RequirePermissionAspect;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.admin.permission.context.PermissionDecision;
import com.example.file.audit.FileAuditOutboxRelay;
import com.example.server.config.CurrentUserArgumentResolver;
import com.example.server.config.JpaConfig;
import com.example.server.config.SecurityConfig;
import com.example.server.security.JwtAuthenticationFilter;
import com.example.server.security.RestAccessDeniedHandler;
import com.example.server.security.RestAuthenticationEntryPoint;

@WebMvcTest(controllers = CodeGroupController.class,
        properties = "spring.task.scheduling.enabled=false",
        excludeAutoConfiguration = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class,
                        RestAccessDeniedHandler.class, JpaConfig.class}))
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.context.annotation.Import({GlobalExceptionHandler.class, CurrentUserArgumentResolver.class})
@DisplayName("CodeGroupController 테스트")
class CodeGroupControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    CodeGroupQueryService queryService;

    @MockBean
    CodeGroupService codeGroupService;

    @MockBean
    StaticCodeRegistry staticCodeRegistry;

    @MockBean
    PermissionEvaluator permissionEvaluator;

    @MockBean
    RequirePermissionAspect requirePermissionAspect;

    @MockBean
    FileAuditOutboxRelay fileAuditOutboxRelay;

    private AuthContext authContext;

    @BeforeEach
    void setUpAuth() throws Throwable {
        authContext = AuthContext.of("tester", "ORG", "PG", FeatureCode.COMMON_CODE, ActionCode.READ, com.example.common.security.RowScope.ALL);
        PermissionDecision allowDecision = org.mockito.Mockito.mock(PermissionDecision.class);
        given(allowDecision.toContext()).willReturn(authContext);
        given(permissionEvaluator.evaluate(any(), any())).willReturn(allowDecision);
        willAnswer(invocation -> null).given(requirePermissionAspect).enforce(any(ProceedingJoinPoint.class));
        AuthContextHolder.set(authContext);
    }

    @AfterEach
    void clearAuth() {
        AuthContextHolder.clear();
    }

    @Nested
    @DisplayName("조회 API")
    class ReadApiTests {

        @Test
        @DisplayName("GET /api/code-groups - 전체 코드 조회 성공")
        void getAllCodes_returns200() throws Exception {
            given(queryService.aggregateAll()).willReturn(Map.of(
                    "USER_STATUS", List.of(
                            new CodeGroupItem("USER_STATUS", "ACTIVE", "활성", 0, true, CodeGroupSource.STATIC_ENUM, null, null)
                    )
            ));

            mockMvc.perform(get("/api/code-groups"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.USER_STATUS").isArray())
                    .andExpect(jsonPath("$.USER_STATUS[0].itemCode").value("ACTIVE"));
        }

        @Test
        @DisplayName("GET /api/code-groups/items - 통합 코드 항목 조회 성공")
        void getAllItems_returns200() throws Exception {
            CodeGroupItem item = new CodeGroupItem("USER_STATUS", "ACTIVE", "활성", 0, true, CodeGroupSource.STATIC_ENUM, null, null);
            given(queryService.findAllItems(any(), any(), any(), any())).willReturn(List.of(
                    CodeGroupItemResponse.from(item)
            ));

            mockMvc.perform(get("/api/code-groups/items"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].itemCode").value("ACTIVE"));
        }

        @Test
        @DisplayName("GET /api/code-groups/items - 필터링 파라미터 전달")
        void getAllItems_withFilters_returns200() throws Exception {
            CodeGroupItem item = new CodeGroupItem("USER_STATUS", "ACTIVE", "활성", 0, true, CodeGroupSource.STATIC_ENUM, null, null);
            given(queryService.findAllItems(any(), eq("USER_STATUS"), eq(true), eq("활성"))).willReturn(List.of(
                    CodeGroupItemResponse.from(item)
            ));

            mockMvc.perform(get("/api/code-groups/items")
                            .param("source", "STATIC_ENUM")
                            .param("groupCode", "USER_STATUS")
                            .param("active", "true")
                            .param("search", "활성"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/code-groups/{groupCode} - 특정 그룹 코드 조회 성공")
        void getByGroupCode_returns200() throws Exception {
            given(queryService.findByGroupCode("USER_STATUS")).willReturn(List.of(
                    new CodeGroupItem("USER_STATUS", "ACTIVE", "활성", 0, true, CodeGroupSource.STATIC_ENUM, null, null)
            ));

            mockMvc.perform(get("/api/code-groups/USER_STATUS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].itemCode").value("ACTIVE"));
        }

        @Test
        @DisplayName("GET /api/code-groups/{groupCode} - 존재하지 않으면 404")
        void getByGroupCode_notFound_returns404() throws Exception {
            given(queryService.findByGroupCode("UNKNOWN")).willReturn(List.of());

            mockMvc.perform(get("/api/code-groups/UNKNOWN"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /api/code-groups/batch - 여러 그룹 코드 일괄 조회")
        void getByGroupCodes_returns200() throws Exception {
            given(queryService.findByGroupCodes(any())).willReturn(Map.of(
                    "USER_STATUS", List.of(
                            new CodeGroupItem("USER_STATUS", "ACTIVE", "활성", 0, true, CodeGroupSource.STATIC_ENUM, null, null)
                    )
            ));

            mockMvc.perform(get("/api/code-groups/batch")
                            .param("groups", "USER_STATUS", "ORDER_STATUS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.USER_STATUS").isArray());
        }

        @Test
        @DisplayName("GET /api/code-groups/meta - 메타정보 조회")
        void getCodeGroupInfos_returns200() throws Exception {
            given(queryService.getCodeGroupInfos()).willReturn(List.of(
                    CodeGroupInfo.ofStaticEnum("USER_STATUS", "사용자 상태", "설명", "GENERAL", 3)
            ));

            mockMvc.perform(get("/api/code-groups/meta"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].groupCode").value("USER_STATUS"));
        }
    }

    @Nested
    @DisplayName("생성 API")
    class CreateApiTests {

        @Test
        @DisplayName("POST /api/code-groups/items - 동적 코드 생성 성공")
        void createItem_returns201() throws Exception {
            UUID id = UUID.randomUUID();
            CodeItem savedItem = createMockCodeItem(id, "NOTICE_CATEGORY", "NC01", "카테고리1");

            given(staticCodeRegistry.hasGroup("NOTICE_CATEGORY")).willReturn(false);
            given(codeGroupService.createItem(any(), anyString(), anyString(), anyString(),
                    any(Integer.class), any(Boolean.class), any(), any(), anyString()))
                    .willReturn(savedItem);

            mockMvc.perform(post("/api/code-groups/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"groupCode":"NOTICE_CATEGORY","itemCode":"NC01","itemName":"카테고리1"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.itemCode").value("NC01"));
        }

        @Test
        @DisplayName("POST /api/code-groups/items - Static Enum에 생성 시도하면 403")
        void createItem_staticEnum_returns403() throws Exception {
            given(staticCodeRegistry.hasGroup("USER_STATUS")).willReturn(true);

            mockMvc.perform(post("/api/code-groups/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"groupCode":"USER_STATUS","itemCode":"NEW","itemName":"신규"}
                                    """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /api/code-groups/items - 등록되지 않은 동적 타입이면 400")
        void createItem_unknownDynamicType_returns400() throws Exception {
            given(staticCodeRegistry.hasGroup("UNKNOWN_TYPE")).willReturn(false);

            mockMvc.perform(post("/api/code-groups/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"groupCode":"UNKNOWN_TYPE","itemCode":"NEW","itemName":"신규"}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("수정 API")
    class UpdateApiTests {

        @Test
        @DisplayName("PUT /api/code-groups/items/{id} - ID로 코드 수정")
        void updateItemById_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            CodeItem existing = createMockCodeItem(id, "NOTICE_CATEGORY", "NC01", "카테고리1");
            CodeItem updated = createMockCodeItem(id, "NOTICE_CATEGORY", "NC01", "수정된 카테고리");

            given(codeGroupService.findItemById(id)).willReturn(Optional.of(existing));
            given(codeGroupService.updateItem(eq(id), anyString(), any(Integer.class),
                    any(Boolean.class), any(), any(), anyString()))
                    .willReturn(updated);

            mockMvc.perform(put("/api/code-groups/items/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"groupCode":"NOTICE_CATEGORY","itemCode":"NC01","itemName":"수정된 카테고리"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.itemName").value("수정된 카테고리"));
        }

        @Test
        @DisplayName("PUT /api/code-groups/items/{id} - 존재하지 않으면 400")
        void updateItemById_notFound_returns400() throws Exception {
            UUID id = UUID.randomUUID();
            given(codeGroupService.findItemById(id)).willReturn(Optional.empty());

            mockMvc.perform(put("/api/code-groups/items/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"groupCode":"NOTICE_CATEGORY","itemCode":"NC01","itemName":"수정된 카테고리"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PUT /api/code-groups/items - groupCode+itemCode로 수정 (Dynamic)")
        void updateItemByGroupAndCode_dynamic_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            CodeItem updated = createMockCodeItem(id, "NOTICE_CATEGORY", "NC01", "수정된 카테고리");

            given(staticCodeRegistry.hasGroup("NOTICE_CATEGORY")).willReturn(false);
            given(codeGroupService.updateItemByGroupAndCode(anyString(), anyString(), anyString(),
                    any(), any(Boolean.class), any(), any(), anyString()))
                    .willReturn(updated);

            mockMvc.perform(put("/api/code-groups/items")
                            .param("groupCode", "NOTICE_CATEGORY")
                            .param("itemCode", "NC01")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"groupCode":"NOTICE_CATEGORY","itemCode":"NC01","itemName":"수정된 카테고리"}
                                    """))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PUT /api/code-groups/items - Static Enum 오버라이드 수정")
        void updateItemByGroupAndCode_staticOverride_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            CodeItem overridden = createMockCodeItem(id, "USER_STATUS", "ACTIVE", "활성화됨");

            given(staticCodeRegistry.hasGroup("USER_STATUS")).willReturn(true);
            given(staticCodeRegistry.getItems("USER_STATUS")).willReturn(List.of(
                    new CodeGroupItem("USER_STATUS", "ACTIVE", "활성", 0, true, CodeGroupSource.STATIC_ENUM, null, null)
            ));
            given(codeGroupService.createOrUpdateOverride(anyString(), anyString(), anyString(),
                    any(), any(), any(), anyString()))
                    .willReturn(overridden);

            mockMvc.perform(put("/api/code-groups/items")
                            .param("groupCode", "USER_STATUS")
                            .param("itemCode", "ACTIVE")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"groupCode":"USER_STATUS","itemCode":"ACTIVE","itemName":"활성화됨"}
                                    """))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PUT /api/code-groups/items - Static Enum에 존재하지 않는 코드면 400")
        void updateItemByGroupAndCode_staticNotExists_returns400() throws Exception {
            given(staticCodeRegistry.hasGroup("USER_STATUS")).willReturn(true);
            given(staticCodeRegistry.getItems("USER_STATUS")).willReturn(List.of(
                    new CodeGroupItem("USER_STATUS", "ACTIVE", "활성", 0, true, CodeGroupSource.STATIC_ENUM, null, null)
            ));

            mockMvc.perform(put("/api/code-groups/items")
                            .param("groupCode", "USER_STATUS")
                            .param("itemCode", "NOT_EXISTS")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"groupCode":"USER_STATUS","itemCode":"NOT_EXISTS","itemName":"없는값"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PUT /api/code-groups/items - 읽기 전용 소스 수정 시 403")
        void updateItemByGroupAndCode_readOnlySource_returns403() throws Exception {
            mockMvc.perform(put("/api/code-groups/items")
                            .param("groupCode", "DW_CODE")
                            .param("itemCode", "CODE1")
                            .param("source", "DW")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"groupCode":"DW_CODE","itemCode":"CODE1","itemName":"변경"}
                                    """))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("삭제 API")
    class DeleteApiTests {

        @Test
        @DisplayName("DELETE /api/code-groups/items/{id} - 동적 코드 삭제 성공")
        void deleteItem_returns204() throws Exception {
            UUID id = UUID.randomUUID();
            CodeItem existing = createMockCodeItem(id, "NOTICE_CATEGORY", "NC01", "카테고리1");

            given(codeGroupService.findItemById(id)).willReturn(Optional.of(existing));
            given(staticCodeRegistry.hasGroup("NOTICE_CATEGORY")).willReturn(false);

            mockMvc.perform(delete("/api/code-groups/items/{id}", id))
                    .andExpect(status().isNoContent());

            verify(codeGroupService).deleteItem(id);
        }

        @Test
        @DisplayName("DELETE /api/code-groups/items/{id} - 존재하지 않으면 400")
        void deleteItem_notFound_returns400() throws Exception {
            UUID id = UUID.randomUUID();
            given(codeGroupService.findItemById(id)).willReturn(Optional.empty());

            mockMvc.perform(delete("/api/code-groups/items/{id}", id))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("DELETE /api/code-groups/items/{id} - Static Enum 삭제 시 403")
        void deleteItem_staticEnum_returns403() throws Exception {
            UUID id = UUID.randomUUID();
            CodeItem existing = createMockCodeItem(id, "USER_STATUS", "ACTIVE", "활성");

            given(codeGroupService.findItemById(id)).willReturn(Optional.of(existing));
            given(staticCodeRegistry.hasGroup("USER_STATUS")).willReturn(true);

            mockMvc.perform(delete("/api/code-groups/items/{id}", id))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /api/code-groups/items/{id}/override - 오버라이드 삭제")
        void deleteOverride_returns204() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/api/code-groups/items/{id}/override", id))
                    .andExpect(status().isNoContent());

            verify(codeGroupService).deleteOverride(id);
        }
    }

    @Nested
    @DisplayName("마이그레이션 API")
    class MigrationApiTests {

        @Test
        @DisplayName("GET /api/code-groups/migration/status - 상태 조회")
        void getMigrationStatus_returns200() throws Exception {
            given(queryService.getMigrationStatus()).willReturn(
                    new MigrationStatusResponse(List.of(), List.of(), List.of())
            );

            mockMvc.perform(get("/api/code-groups/migration/status"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /api/code-groups/migration - 마이그레이션 성공")
        void migrate_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            given(staticCodeRegistry.hasGroup("NEW_GROUP")).willReturn(true);
            given(codeGroupService.migrate(id, "NEW_GROUP"))
                    .willReturn(new MigrationResult(5, id, "OLD_GROUP", "NEW_GROUP"));

            mockMvc.perform(post("/api/code-groups/migration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"id\":\"" + id + "\",\"newGroupCode\":\"NEW_GROUP\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.migratedCount").value(5));
        }

        @Test
        @DisplayName("POST /api/code-groups/migration - Enum에 없는 그룹코드로 마이그레이션 시 400")
        void migrate_invalidNewGroupCode_returns400() throws Exception {
            UUID id = UUID.randomUUID();
            given(staticCodeRegistry.hasGroup("NOT_IN_ENUM")).willReturn(false);

            mockMvc.perform(post("/api/code-groups/migration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"id\":\"" + id + "\",\"newGroupCode\":\"NOT_IN_ENUM\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/code-groups/migration - 충돌 발생 시 409")
        void migrate_conflict_returns409() throws Exception {
            UUID id = UUID.randomUUID();
            given(staticCodeRegistry.hasGroup("NEW_GROUP")).willReturn(true);
            given(codeGroupService.migrate(id, "NEW_GROUP"))
                    .willThrow(new IllegalStateException("이미 레코드가 존재합니다"));

            mockMvc.perform(post("/api/code-groups/migration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"id\":\"" + id + "\",\"newGroupCode\":\"NEW_GROUP\"}"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("POST /api/code-groups/migration - 잘못된 인자 시 400")
        void migrate_illegalArgument_returns400() throws Exception {
            UUID id = UUID.randomUUID();
            given(staticCodeRegistry.hasGroup("NEW_GROUP")).willReturn(true);
            given(codeGroupService.migrate(id, "NEW_GROUP"))
                    .willThrow(new IllegalArgumentException("그룹을 찾을 수 없습니다"));

            mockMvc.perform(post("/api/code-groups/migration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"id\":\"" + id + "\",\"newGroupCode\":\"NEW_GROUP\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("DELETE /api/code-groups/migration/{groupCode} - DB 전용 그룹 삭제")
        void deleteDbOnlyGroupCode_returns204() throws Exception {
            given(staticCodeRegistry.hasGroup("DB_ONLY_GROUP")).willReturn(false);

            mockMvc.perform(delete("/api/code-groups/migration/DB_ONLY_GROUP"))
                    .andExpect(status().isNoContent());

            verify(codeGroupService).deleteByGroupCode("DB_ONLY_GROUP");
        }

        @Test
        @DisplayName("DELETE /api/code-groups/migration/{groupCode} - Enum 그룹 삭제 시도하면 400")
        void deleteDbOnlyGroupCode_enumGroup_returns400() throws Exception {
            given(staticCodeRegistry.hasGroup("USER_STATUS")).willReturn(true);

            mockMvc.perform(delete("/api/code-groups/migration/USER_STATUS"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("유틸리티 API")
    class UtilityApiTests {

        @Test
        @DisplayName("POST /api/code-groups/cache/evict - 전체 캐시 무효화")
        void evictCache_all_returns200() throws Exception {
            mockMvc.perform(post("/api/code-groups/cache/evict"))
                    .andExpect(status().isOk());

            verify(queryService).evictCache(null);
        }

        @Test
        @DisplayName("POST /api/code-groups/cache/evict - 특정 그룹 캐시 무효화")
        void evictCache_specific_returns200() throws Exception {
            mockMvc.perform(post("/api/code-groups/cache/evict")
                            .param("groupCode", "USER_STATUS"))
                    .andExpect(status().isOk());

            verify(queryService).evictCache("USER_STATUS");
        }
    }

    /**
     * CodeItem mock 생성 헬퍼
     */
    private CodeItem createMockCodeItem(UUID id, String groupCode, String itemCode, String itemName) {
        CodeItem mockItem = org.mockito.Mockito.mock(CodeItem.class);
        given(mockItem.getId()).willReturn(id);
        given(mockItem.getGroupCode()).willReturn(groupCode);
        given(mockItem.getItemCode()).willReturn(itemCode);
        given(mockItem.getItemName()).willReturn(itemName);
        given(mockItem.getDisplayOrder()).willReturn(0);
        given(mockItem.isActive()).willReturn(true);
        given(mockItem.getSource()).willReturn(CodeGroupSource.DYNAMIC_DB);
        given(mockItem.getDescription()).willReturn(null);
        given(mockItem.getMetadataJson()).willReturn(null);
        given(mockItem.isBuiltIn()).willReturn(false);
        given(mockItem.getUpdatedAt()).willReturn(OffsetDateTime.now());
        given(mockItem.getUpdatedBy()).willReturn("tester");
        return mockItem;
    }
}
