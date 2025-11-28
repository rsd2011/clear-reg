package com.example.admin.codegroup.dto;

import com.example.admin.codegroup.domain.CodeGroupSource;

/**
 * 코드 그룹 항목 DTO.
 *
 * <p>정적 Enum, 동적 DB 코드, DW 연동 코드 등 모든 소스의 코드 아이템을 통일된 형식으로 표현합니다.</p>
 *
 * @param groupCode    그룹 코드 (Enum 클래스명 또는 DynamicCodeType)
 * @param itemCode     아이템 코드 (Enum name() 또는 DB 코드값)
 * @param itemName     표시 라벨/이름
 * @param displayOrder 표시 순서
 * @param active       활성화 여부
 * @param source       코드 소스 타입
 * @param description  설명
 * @param metadataJson 추가 메타데이터 (JSON)
 */
public record CodeGroupItem(
        String groupCode,
        String itemCode,
        String itemName,
        Integer displayOrder,
        boolean active,
        CodeGroupSource source,
        String description,
        String metadataJson
) {

    /**
     * 정적 Enum용 간편 생성자
     */
    public static CodeGroupItem ofStaticEnum(String groupCode, String itemCode, String itemName,
                                              int displayOrder, String description) {
        return new CodeGroupItem(groupCode, itemCode, itemName, displayOrder, true,
                CodeGroupSource.STATIC_ENUM, description, null);
    }

    /**
     * 동적 DB 코드용 간편 생성자
     */
    public static CodeGroupItem ofDynamicDb(String groupCode, String itemCode, String itemName,
                                             int displayOrder, boolean active,
                                             String description, String metadataJson) {
        return new CodeGroupItem(groupCode, itemCode, itemName, displayOrder, active,
                CodeGroupSource.DYNAMIC_DB, description, metadataJson);
    }

    /**
     * DW 연동 코드용 간편 생성자
     */
    public static CodeGroupItem ofDw(String groupCode, String itemCode, String itemName,
                                      int displayOrder, String description, String metadataJson) {
        return new CodeGroupItem(groupCode, itemCode, itemName, displayOrder, true,
                CodeGroupSource.DW, description, metadataJson);
    }

    /**
     * 승인 그룹용 간편 생성자
     */
    public static CodeGroupItem ofApprovalGroup(String itemCode, String itemName,
                                                 int displayOrder, boolean active, String description) {
        return new CodeGroupItem("APPROVAL_GROUP", itemCode, itemName, displayOrder, active,
                CodeGroupSource.APPROVAL_GROUP, description, null);
    }
}
