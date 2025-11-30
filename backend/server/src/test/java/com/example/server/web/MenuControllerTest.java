package com.example.server.web;

import static org.mockito.ArgumentMatchers.any;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

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

import com.example.admin.menu.domain.Menu;
import com.example.admin.menu.domain.MenuCapability;
import com.example.admin.menu.domain.MenuCode;
import com.example.admin.menu.service.MenuService;
import com.example.admin.permission.aop.RequirePermissionAspect;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.admin.permission.context.PermissionDecision;
import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.admin.permission.service.PermissionEvaluator;
import com.example.file.audit.FileAuditOutboxRelay;
import com.example.server.config.CurrentUserArgumentResolver;
import com.example.server.config.JpaConfig;
import com.example.server.config.SecurityConfig;
import com.example.server.security.JwtAuthenticationFilter;
import com.example.server.security.RestAccessDeniedHandler;
import com.example.server.security.RestAuthenticationEntryPoint;

@WebMvcTest(controllers = MenuController.class,
        properties = "spring.task.scheduling.enabled=false",
        excludeAutoConfiguration = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class,
                        RestAccessDeniedHandler.class, JpaConfig.class}))
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.context.annotation.Import({GlobalExceptionHandler.class, CurrentUserArgumentResolver.class})
@DisplayName("MenuController 테스트")
class MenuControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    MenuService menuService;

    @MockBean
    PermissionEvaluator permissionEvaluator;

    @MockBean
    RequirePermissionAspect requirePermissionAspect;

    @MockBean
    FileAuditOutboxRelay fileAuditOutboxRelay;

    private AuthContext authContext;

    @BeforeEach
    void setUpAuth() throws Throwable {
        authContext = AuthContext.of("tester", "ORG", "PG", FeatureCode.MENU, ActionCode.READ, java.util.List.of());
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
    @DisplayName("메뉴 코드 조회 API")
    class MenuCodeApiTests {

        @Test
        @DisplayName("GET /api/admin/menus/codes - Given 등록된 메뉴 존재 When 조회 Then 모든 MenuCode 반환")
        void listMenuCodes_returnsAllCodes() throws Exception {
            given(menuService.findByCode(MenuCode.DASHBOARD)).willReturn(Optional.of(new Menu(MenuCode.DASHBOARD, "대시보드")));
            given(menuService.findByCode(any(MenuCode.class))).willReturn(Optional.empty());
            given(menuService.findByCode(MenuCode.DASHBOARD)).willReturn(Optional.of(new Menu(MenuCode.DASHBOARD, "대시보드")));

            mockMvc.perform(get("/api/admin/menus/codes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(MenuCode.values().length));
        }

        @Test
        @DisplayName("GET /api/admin/menus/codes - Given DB 등록 메뉴 When 조회 Then registered 플래그 true")
        void listMenuCodes_showsRegisteredFlag() throws Exception {
            given(menuService.findByCode(MenuCode.DASHBOARD)).willReturn(Optional.of(new Menu(MenuCode.DASHBOARD, "대시보드")));

            mockMvc.perform(get("/api/admin/menus/codes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].code").value("DASHBOARD"))
                    .andExpect(jsonPath("$[0].registered").value(true));
        }

        @Test
        @DisplayName("GET /api/admin/menus/codes/{code} - Given 유효한 코드 When 조회 Then 해당 코드 정보 반환")
        void getMenuCode_returnsCodeInfo() throws Exception {
            given(menuService.findByCode(MenuCode.DASHBOARD)).willReturn(Optional.of(new Menu(MenuCode.DASHBOARD, "대시보드")));

            mockMvc.perform(get("/api/admin/menus/codes/DASHBOARD"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("DASHBOARD"))
                    .andExpect(jsonPath("$.path").value("/dashboard"))
                    .andExpect(jsonPath("$.defaultIcon").value("home"))
                    .andExpect(jsonPath("$.registered").value(true));
        }

        @Test
        @DisplayName("GET /api/admin/menus/codes/{code} - Given 잘못된 코드 When 조회 Then 400 반환")
        void getMenuCode_invalidCode_returns400() throws Exception {
            mockMvc.perform(get("/api/admin/menus/codes/INVALID_CODE"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("메뉴 조회 API")
    class MenuReadApiTests {

        @Test
        @DisplayName("GET /api/admin/menus - Given 활성 메뉴 존재 When 조회 Then 목록 반환")
        void listActiveMenus_returnsMenus() throws Exception {
            Menu menu1 = new Menu(MenuCode.DASHBOARD, "대시보드");
            menu1.updateDetails("대시보드", "home", 1, null);
            Menu menu2 = new Menu(MenuCode.DRAFT, "기안");
            menu2.updateDetails("기안", "edit", 2, null);

            given(menuService.findAllActive()).willReturn(List.of(menu1, menu2));

            mockMvc.perform(get("/api/admin/menus"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].code").value("DASHBOARD"))
                    .andExpect(jsonPath("$[1].code").value("DRAFT"));
        }

        @Test
        @DisplayName("GET /api/admin/menus/{code} - Given 존재하는 메뉴 When 조회 Then 200 반환")
        void getMenu_exists_returns200() throws Exception {
            Menu menu = new Menu(MenuCode.DASHBOARD, "대시보드");
            menu.updateDetails("대시보드", "home", 1, "메인 화면");

            given(menuService.findByCode(MenuCode.DASHBOARD)).willReturn(Optional.of(menu));

            mockMvc.perform(get("/api/admin/menus/DASHBOARD"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("DASHBOARD"))
                    .andExpect(jsonPath("$.name").value("대시보드"))
                    .andExpect(jsonPath("$.icon").value("home"))
                    .andExpect(jsonPath("$.path").value("/dashboard"));
        }

        @Test
        @DisplayName("GET /api/admin/menus/{code} - Given 미등록 메뉴 When 조회 Then 404 반환")
        void getMenu_notExists_returns404() throws Exception {
            given(menuService.findByCode(MenuCode.DASHBOARD)).willReturn(Optional.empty());

            mockMvc.perform(get("/api/admin/menus/DASHBOARD"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("메뉴 수정 API")
    class MenuUpdateApiTests {

        @Test
        @DisplayName("PUT /api/admin/menus/{code} - Given 유효한 요청 When 수정 Then 200 반환")
        void updateMenu_validRequest_returns200() throws Exception {
            Menu updatedMenu = new Menu(MenuCode.DASHBOARD, "새 대시보드");
            updatedMenu.updateDetails("새 대시보드", "new-icon", 10, "새 설명");

            given(menuService.createOrUpdateMenu(
                    eq(MenuCode.DASHBOARD),
                    eq("새 대시보드"),
                    eq("new-icon"),
                    eq(10),
                    eq("새 설명"),
                    any()
            )).willReturn(updatedMenu);

            mockMvc.perform(put("/api/admin/menus/DASHBOARD")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "새 대시보드",
                                        "icon": "new-icon",
                                        "sortOrder": 10,
                                        "description": "새 설명"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("새 대시보드"))
                    .andExpect(jsonPath("$.icon").value("new-icon"));
        }

        @Test
        @DisplayName("PUT /api/admin/menus/{code} - Given capabilities 포함 요청 When 수정 Then capabilities 처리")
        void updateMenu_withCapabilities_processesCapabilities() throws Exception {
            Menu updatedMenu = new Menu(MenuCode.DASHBOARD, "대시보드");
            updatedMenu.addCapability(new MenuCapability(FeatureCode.MENU, ActionCode.READ));

            given(menuService.createOrUpdateMenu(
                    eq(MenuCode.DASHBOARD),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
            )).willReturn(updatedMenu);

            mockMvc.perform(put("/api/admin/menus/DASHBOARD")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "대시보드",
                                        "capabilities": [
                                            {"feature": "MENU", "action": "READ"}
                                        ]
                                    }
                                    """))
                    .andExpect(status().isOk());

            verify(menuService).createOrUpdateMenu(
                    eq(MenuCode.DASHBOARD),
                    eq("대시보드"),
                    any(),
                    any(),
                    any(),
                    any(Set.class)
            );
        }

        @Test
        @DisplayName("PUT /api/admin/menus/{code} - Given capabilities null When 수정 Then null 전달")
        void updateMenu_nullCapabilities_passesNull() throws Exception {
            Menu updatedMenu = new Menu(MenuCode.DASHBOARD, "대시보드");

            given(menuService.createOrUpdateMenu(
                    eq(MenuCode.DASHBOARD),
                    any(),
                    any(),
                    any(),
                    any(),
                    eq(null)
            )).willReturn(updatedMenu);

            mockMvc.perform(put("/api/admin/menus/DASHBOARD")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "대시보드"
                                    }
                                    """))
                    .andExpect(status().isOk());

            verify(menuService).createOrUpdateMenu(
                    eq(MenuCode.DASHBOARD),
                    eq("대시보드"),
                    eq(null),
                    eq(null),
                    eq(null),
                    eq(null)
            );
        }
    }

    @Nested
    @DisplayName("메뉴 활성화/비활성화 API")
    class MenuActivationApiTests {

        @Test
        @DisplayName("DELETE /api/admin/menus/{code} - Given 활성 메뉴 When 비활성화 Then 204 반환")
        void deactivateMenu_returns204() throws Exception {
            mockMvc.perform(delete("/api/admin/menus/DASHBOARD"))
                    .andExpect(status().isNoContent());

            verify(menuService).deactivateMenu(MenuCode.DASHBOARD);
        }

        @Test
        @DisplayName("POST /api/admin/menus/{code}/activate - Given 비활성 메뉴 When 활성화 Then 204 반환")
        void activateMenu_returns204() throws Exception {
            mockMvc.perform(post("/api/admin/menus/DASHBOARD/activate"))
                    .andExpect(status().isNoContent());

            verify(menuService).activateMenu(MenuCode.DASHBOARD);
        }
    }

    @Nested
    @DisplayName("메뉴 동기화 API")
    class MenuSyncApiTests {

        @Test
        @DisplayName("POST /api/admin/menus/sync - Given enum 동기화 When 호출 Then 생성 개수 반환")
        void syncMenus_returnsCreatedCount() throws Exception {
            given(menuService.syncMenusFromEnum()).willReturn(5);

            mockMvc.perform(post("/api/admin/menus/sync"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.createdCount").value(5));
        }

        @Test
        @DisplayName("POST /api/admin/menus/sync - Given 이미 동기화됨 When 호출 Then 0 반환")
        void syncMenus_alreadySynced_returnsZero() throws Exception {
            given(menuService.syncMenusFromEnum()).willReturn(0);

            mockMvc.perform(post("/api/admin/menus/sync"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.createdCount").value(0));
        }
    }
}
