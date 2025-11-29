package com.example.admin.menu.dto;

import com.example.admin.menu.domain.Menu;
import com.example.admin.menu.domain.MenuCode;

import java.util.List;
import java.util.UUID;

/**
 * 메뉴 응답 DTO.
 *
 * @param id 메뉴 ID
 * @param code 메뉴 코드
 * @param path 메뉴 경로
 * @param name 메뉴 이름
 * @param icon 표시 아이콘 (DB 오버라이드 또는 enum 기본값)
 * @param sortOrder 정렬 순서
 * @param description 메뉴 설명
 * @param active 활성화 여부
 * @param capabilities 메뉴 접근 권한 목록
 */
public record MenuResponse(
        UUID id,
        MenuCode code,
        String path,
        String name,
        String icon,
        Integer sortOrder,
        String description,
        boolean active,
        List<MenuCapabilityResponse> capabilities
) {
    /**
     * Menu 엔티티를 응답 DTO로 변환한다.
     */
    public static MenuResponse from(Menu menu) {
        List<MenuCapabilityResponse> capabilityResponses = menu.getRequiredCapabilities().stream()
                .map(MenuCapabilityResponse::from)
                .toList();

        return new MenuResponse(
                menu.getId(),
                menu.getCode(),
                menu.getPath(),
                menu.getName(),
                menu.getDisplayIcon(),
                menu.getSortOrder(),
                menu.getDescription(),
                menu.isActive(),
                capabilityResponses
        );
    }
}
