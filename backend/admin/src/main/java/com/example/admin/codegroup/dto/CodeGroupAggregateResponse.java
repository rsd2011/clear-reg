package com.example.admin.codegroup.dto;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * 코드 그룹 집계 응답 DTO.
 *
 * <p>특정 그룹의 코드 항목 목록을 마스킹 기능과 함께 반환합니다.</p>
 *
 * @param groupCode 그룹 코드
 * @param items     코드 항목 목록
 */
public record CodeGroupAggregateResponse(
        String groupCode,
        List<CodeGroupItemResponse> items
) {

    /**
     * CodeGroupItem 목록에서 응답 생성
     *
     * @param groupCode 그룹 코드
     * @param items     코드 항목 목록
     * @param masker    마스킹 함수 (nullable)
     * @return 응답 DTO
     */
    public static CodeGroupAggregateResponse of(
            String groupCode,
            List<CodeGroupItem> items,
            UnaryOperator<String> masker
    ) {
        List<CodeGroupItemResponse> responseItems = items.stream()
                .map(item -> CodeGroupItemResponse.from(item, masker))
                .toList();
        return new CodeGroupAggregateResponse(groupCode, responseItems);
    }

    /**
     * CodeGroupItem 목록에서 응답 생성 (마스킹 없음)
     */
    public static CodeGroupAggregateResponse of(String groupCode, List<CodeGroupItem> items) {
        return of(groupCode, items, null);
    }
}
