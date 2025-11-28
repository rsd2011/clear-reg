package com.example.admin.codegroup.dto;

import jakarta.validation.constraints.Size;

/**
 * 코드 항목 생성/수정 요청 DTO.
 *
 * <p>동적 코드 생성 및 Static Enum 오버라이드 수정에 사용합니다.</p>
 *
 * @param groupCode    그룹 코드 (생성 시 필수, 수정 시 무시)
 * @param itemCode     항목 코드 (생성 시 필수, 수정 시 무시)
 * @param itemName     코드 표시명
 * @param displayOrder 표시 순서
 * @param active       활성 상태 (Dynamic DB만)
 * @param description  설명
 * @param metadataJson 메타데이터 (JSON)
 */
public record CodeGroupItemRequest(
        @Size(max = 64, message = "그룹 코드는 64자 이하여야 합니다")
        String groupCode,

        @Size(max = 128, message = "항목 코드는 128자 이하여야 합니다")
        String itemCode,

        @Size(max = 255, message = "항목명은 255자 이하여야 합니다")
        String itemName,

        Integer displayOrder,

        Boolean active,

        @Size(max = 512, message = "설명은 512자 이하여야 합니다")
        String description,

        @Size(max = 2000, message = "메타데이터는 2000자 이하여야 합니다")
        String metadataJson
) {

    /**
     * 기본값이 적용된 displayOrder 반환
     */
    public int getDisplayOrderOrDefault() {
        return displayOrder != null ? displayOrder : 0;
    }

    /**
     * 기본값이 적용된 active 반환
     */
    public boolean isActiveOrDefault() {
        return active != null ? active : true;
    }
}
