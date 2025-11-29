package com.example.admin.menu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 메뉴 수정 요청 DTO.
 *
 * @param name 메뉴 이름 (필수)
 * @param icon 아이콘 (선택, null이면 enum 기본값 사용)
 * @param sortOrder 정렬 순서
 * @param description 메뉴 설명
 * @param capabilities 메뉴 접근 권한 목록
 */
public record MenuUpdateRequest(
        @NotBlank(message = "메뉴 이름은 필수입니다")
        @Size(max = 200, message = "메뉴 이름은 200자를 초과할 수 없습니다")
        String name,

        @Size(max = 50, message = "아이콘은 50자를 초과할 수 없습니다")
        String icon,

        Integer sortOrder,

        @Size(max = 1000, message = "설명은 1000자를 초과할 수 없습니다")
        String description,

        @Valid
        List<MenuCapabilityRequest> capabilities
) {
}
