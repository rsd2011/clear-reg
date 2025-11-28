package com.example.admin.codegroup.dto;

import com.example.admin.codegroup.domain.CodeGroupSource;
import com.example.admin.codegroup.domain.CodeItem;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * 코드 항목 통합 응답 DTO.
 *
 * <p>모든 소스(Static Enum, Dynamic DB, DW 등)의 코드를 통일된 형식으로 표현합니다.</p>
 *
 * @param id            DB ID (Static Enum은 오버라이드 없으면 null)
 * @param groupCode     그룹 코드
 * @param itemCode      아이템 코드
 * @param itemName      아이템명 (Static Enum은 null일 수 있음)
 * @param displayOrder  표시 순서
 * @param active        활성 상태
 * @param source        소스 타입
 * @param description   설명
 * @param metadataJson  메타데이터 (JSON)
 * @param editable      라벨 수정 가능 여부
 * @param deletable     삭제 가능 여부
 * @param hasDbOverride Static Enum의 DB 오버라이드 존재 여부
 * @param builtIn       ISO 표준 제공 항목 여부 (Locale 소스용)
 * @param updatedAt     수정일시
 * @param updatedBy     수정자
 */
public record CodeGroupItemResponse(
        UUID id,
        String groupCode,
        String itemCode,
        String itemName,
        Integer displayOrder,
        boolean active,
        CodeGroupSource source,
        String description,
        String metadataJson,
        boolean editable,
        boolean deletable,
        boolean hasDbOverride,
        boolean builtIn,
        OffsetDateTime updatedAt,
        String updatedBy
) {

    /**
     * CodeGroupItem에서 변환 (기본 응답용)
     */
    public static CodeGroupItemResponse from(CodeGroupItem item) {
        return from(item, null, null, null, false);
    }

    /**
     * CodeGroupItem에서 변환 (마스킹 지원)
     */
    public static CodeGroupItemResponse from(CodeGroupItem item, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new CodeGroupItemResponse(
                null,
                item.groupCode(),
                fn.apply(item.itemCode()),
                fn.apply(item.itemName()),
                item.displayOrder(),
                item.active(),
                item.source(),
                fn.apply(item.description()),
                fn.apply(item.metadataJson()),
                isEditable(item.source()),
                isDeletable(item.source()),
                false,
                false,
                null,
                null
        );
    }

    /**
     * CodeGroupItem + DB 정보로 변환 (상세 응답용)
     */
    public static CodeGroupItemResponse from(
            CodeGroupItem item,
            UUID id,
            OffsetDateTime updatedAt,
            String updatedBy,
            boolean hasDbOverride
    ) {
        return new CodeGroupItemResponse(
                id,
                item.groupCode(),
                item.itemCode(),
                item.itemName(),
                item.displayOrder(),
                item.active(),
                item.source(),
                item.description(),
                item.metadataJson(),
                isEditable(item.source()),
                isDeletable(item.source()),
                hasDbOverride,
                false,
                updatedAt,
                updatedBy
        );
    }

    /**
     * CodeItem (DB 엔티티)에서 변환 (소스 자동 추론)
     */
    public static CodeGroupItemResponse fromEntity(CodeItem entity) {
        return fromEntity(entity, entity.getSource());
    }

    /**
     * CodeItem (DB 엔티티)에서 변환
     */
    public static CodeGroupItemResponse fromEntity(CodeItem entity, CodeGroupSource source) {
        return new CodeGroupItemResponse(
                entity.getId(),
                entity.getGroupCode(),
                entity.getItemCode(),
                entity.getItemName(),
                entity.getDisplayOrder(),
                entity.isActive(),
                source,
                entity.getDescription(),
                entity.getMetadataJson(),
                isEditable(source),
                isDeletable(source),
                source == CodeGroupSource.STATIC_ENUM || source.isLocale(), // Static Enum 또는 Locale의 DB 레코드는 오버라이드
                entity.isBuiltIn(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy()
        );
    }

    /**
     * Locale ISO 항목용 응답 생성 (DB 오버라이드 없음)
     */
    public static CodeGroupItemResponse forLocaleIso(
            String groupCode,
            String itemCode,
            String itemName,
            CodeGroupSource source,
            String metadataJson
    ) {
        return new CodeGroupItemResponse(
                null,
                groupCode,
                itemCode,
                itemName,
                0,
                true,
                source,
                null,
                metadataJson,
                isEditable(source),
                isDeletable(source),
                false,
                true, // builtIn = true for ISO items
                null,
                null
        );
    }

    /**
     * 라벨 수정 가능 여부 판단 (enum 위임)
     */
    private static boolean isEditable(CodeGroupSource source) {
        return source.isEditable();
    }

    /**
     * 삭제 가능 여부 판단 (enum 위임)
     */
    private static boolean isDeletable(CodeGroupSource source) {
        return source.isDeletable();
    }
}
