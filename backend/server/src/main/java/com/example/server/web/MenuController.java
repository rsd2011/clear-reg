package com.example.server.web;

import com.example.admin.menu.domain.Menu;
import com.example.admin.menu.domain.MenuCapability;
import com.example.admin.menu.domain.MenuCode;
import com.example.admin.menu.dto.MenuCodeResponse;
import com.example.admin.menu.dto.MenuResponse;
import com.example.admin.menu.dto.MenuUpdateRequest;
import com.example.admin.menu.service.MenuService;
import com.example.admin.permission.annotation.RequirePermission;
import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 메뉴 관리 API.
 *
 * <p>관리자가 메뉴를 조회/수정하고, 메뉴 코드 목록을 확인할 수 있다.</p>
 */
@RestController
@Validated
@RequestMapping("/api/admin/menus")
@Tag(name = "Menu Admin", description = "메뉴 관리 API")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    // ========================================
    // 메뉴 코드 조회
    // ========================================

    @GetMapping("/codes")
    @RequirePermission(feature = FeatureCode.MENU, action = ActionCode.READ)
    @Operation(summary = "메뉴 코드 목록 조회", description = "MenuCode enum에 정의된 모든 메뉴 코드와 DB 등록 여부를 조회한다.")
    public List<MenuCodeResponse> listMenuCodes() {
        return Arrays.stream(MenuCode.values())
                .map(code -> MenuCodeResponse.from(code, menuService.findByCode(code).isPresent()))
                .toList();
    }

    @GetMapping("/codes/{code}")
    @RequirePermission(feature = FeatureCode.MENU, action = ActionCode.READ)
    @Operation(summary = "메뉴 코드 단일 조회", description = "특정 메뉴 코드의 상세 정보를 조회한다.")
    public MenuCodeResponse getMenuCode(@PathVariable MenuCode code) {
        return MenuCodeResponse.from(code, menuService.findByCode(code).isPresent());
    }

    // ========================================
    // 메뉴 CRUD
    // ========================================

    @GetMapping
    @RequirePermission(feature = FeatureCode.MENU, action = ActionCode.READ)
    @Operation(summary = "활성 메뉴 목록 조회", description = "활성화된 모든 메뉴를 정렬 순서대로 조회한다.")
    public List<MenuResponse> listActiveMenus() {
        return menuService.findAllActive().stream()
                .map(MenuResponse::from)
                .toList();
    }

    @GetMapping("/{code}")
    @RequirePermission(feature = FeatureCode.MENU, action = ActionCode.READ)
    @Operation(summary = "메뉴 단일 조회", description = "메뉴 코드로 메뉴를 조회한다.")
    public ResponseEntity<MenuResponse> getMenu(@PathVariable MenuCode code) {
        return menuService.findByCode(code)
                .map(MenuResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{code}")
    @RequirePermission(feature = FeatureCode.MENU, action = ActionCode.UPDATE)
    @Operation(summary = "메뉴 수정", description = "메뉴의 이름, 아이콘, 정렬순서, 설명, 접근 권한을 수정한다.")
    public MenuResponse updateMenu(
            @PathVariable MenuCode code,
            @Valid @RequestBody MenuUpdateRequest request) {

        Set<MenuCapability> capabilities = null;
        if (request.capabilities() != null) {
            capabilities = request.capabilities().stream()
                    .map(cap -> new MenuCapability(cap.feature(), cap.action()))
                    .collect(Collectors.toSet());
        }

        Menu menu = menuService.createOrUpdateMenu(
                code,
                request.name(),
                request.icon(),
                request.sortOrder(),
                request.description(),
                capabilities
        );

        return MenuResponse.from(menu);
    }

    @DeleteMapping("/{code}")
    @RequirePermission(feature = FeatureCode.MENU, action = ActionCode.DELETE)
    @Operation(summary = "메뉴 비활성화", description = "메뉴를 비활성화한다 (소프트 삭제).")
    public ResponseEntity<Void> deactivateMenu(@PathVariable MenuCode code) {
        menuService.deactivateMenu(code);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{code}/activate")
    @RequirePermission(feature = FeatureCode.MENU, action = ActionCode.UPDATE)
    @Operation(summary = "메뉴 활성화", description = "비활성화된 메뉴를 다시 활성화한다.")
    public ResponseEntity<Void> activateMenu(@PathVariable MenuCode code) {
        menuService.activateMenu(code);
        return ResponseEntity.noContent().build();
    }

    // ========================================
    // 동기화
    // ========================================

    @PostMapping("/sync")
    @RequirePermission(feature = FeatureCode.MENU, action = ActionCode.CREATE)
    @Operation(summary = "메뉴 동기화", description = "MenuCode enum에 정의된 메뉴를 DB에 동기화한다.")
    public SyncResponse syncMenus() {
        int created = menuService.syncMenusFromEnum();
        return new SyncResponse(created);
    }

    /**
     * 동기화 결과 응답.
     */
    public record SyncResponse(int createdCount) {}
}
