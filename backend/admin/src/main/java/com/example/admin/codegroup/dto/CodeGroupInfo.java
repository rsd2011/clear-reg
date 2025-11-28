package com.example.admin.codegroup.dto;

import com.example.admin.codegroup.domain.CodeGroupSource;
import lombok.Builder;

/**
 * 코드 그룹 메타정보 DTO.
 *
 * <p>관리 화면에서 코드 그룹 목록을 표시할 때 사용합니다.</p>
 *
 * @param groupCode   그룹 코드명
 * @param displayName 표시명
 * @param description 설명
 * @param group       그룹/카테고리
 * @param source      코드 소스 타입
 * @param editable    수정 가능 여부
 * @param itemCount   코드 항목 수
 */
@Builder
public record CodeGroupInfo(
        String groupCode,
        String displayName,
        String description,
        String group,
        CodeGroupSource source,
        boolean editable,
        int itemCount
) {

    /**
     * 정적 Enum용 간편 생성자
     */
    public static CodeGroupInfo ofStaticEnum(String groupCode, String displayName,
                                              String description, String group, int itemCount) {
        return CodeGroupInfo.builder()
                .groupCode(groupCode)
                .displayName(displayName != null ? displayName : groupCode)
                .description(description)
                .group(group != null ? group : "GENERAL")
                .source(CodeGroupSource.STATIC_ENUM)
                .editable(false)
                .itemCount(itemCount)
                .build();
    }

    /**
     * 동적 DB 코드용 간편 생성자
     */
    public static CodeGroupInfo ofDynamicDb(String groupCode, String displayName,
                                             String description, int itemCount) {
        return CodeGroupInfo.builder()
                .groupCode(groupCode)
                .displayName(displayName != null ? displayName : groupCode)
                .description(description)
                .group("SYSTEM")
                .source(CodeGroupSource.DYNAMIC_DB)
                .editable(true)
                .itemCount(itemCount)
                .build();
    }
}
