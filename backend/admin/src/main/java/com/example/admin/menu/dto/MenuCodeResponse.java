package com.example.admin.menu.dto;

import com.example.admin.menu.domain.MenuCode;

/**
 * 메뉴 코드 응답 DTO.
 *
 * <p>MenuCode enum 정보와 DB 등록 여부를 포함한다.</p>
 *
 * @param code 메뉴 코드
 * @param path 메뉴 경로
 * @param defaultIcon 기본 아이콘
 * @param registered DB 등록 여부
 */
public record MenuCodeResponse(
        MenuCode code,
        String path,
        String defaultIcon,
        boolean registered
) {
    /**
     * MenuCode enum을 응답 DTO로 변환한다.
     *
     * @param menuCode 메뉴 코드 enum
     * @param registered DB 등록 여부
     */
    public static MenuCodeResponse from(MenuCode menuCode, boolean registered) {
        return new MenuCodeResponse(
                menuCode,
                menuCode.getPath(),
                menuCode.getDefaultIcon(),
                registered
        );
    }
}
