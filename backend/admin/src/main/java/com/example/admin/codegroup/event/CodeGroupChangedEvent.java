package com.example.admin.codegroup.event;

import com.example.admin.codegroup.domain.CodeGroupSource;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 코드 그룹/아이템 변경 이벤트.
 *
 * <p>코드 그룹이나 아이템이 추가, 수정, 삭제될 때 발행되어 캐시 무효화를 트리거합니다.</p>
 *
 * <h3>사용 예시:</h3>
 * <pre>{@code
 * // 아이템 저장 후 이벤트 발행
 * applicationEventPublisher.publishEvent(
 *     CodeGroupChangedEvent.itemCreated(this, CodeGroupSource.DYNAMIC_DB, "NOTICE_CATEGORY", "NOTICE_001")
 * );
 * }</pre>
 */
@Getter
public class CodeGroupChangedEvent extends ApplicationEvent {

    /**
     * 변경 유형
     */
    public enum ChangeType {
        GROUP_CREATED,
        GROUP_UPDATED,
        GROUP_DELETED,
        ITEM_CREATED,
        ITEM_UPDATED,
        ITEM_DELETED,
        MIGRATED
    }

    private final CodeGroupSource source;
    private final String groupCode;
    private final ChangeType changeType;
    private final String itemCode;  // nullable - 아이템 변경 시에만
    private final String newGroupCode;  // nullable - 마이그레이션 시 새 그룹 코드

    private CodeGroupChangedEvent(Object eventSource, CodeGroupSource source, String groupCode,
                                   ChangeType changeType, String itemCode, String newGroupCode) {
        super(eventSource);
        this.source = source;
        this.groupCode = groupCode;
        this.changeType = changeType;
        this.itemCode = itemCode;
        this.newGroupCode = newGroupCode;
    }

    // ========== 그룹 이벤트 ==========

    /**
     * 그룹 생성 이벤트
     */
    public static CodeGroupChangedEvent groupCreated(Object source, CodeGroupSource codeSource, String groupCode) {
        return new CodeGroupChangedEvent(source, codeSource, groupCode,
                ChangeType.GROUP_CREATED, null, null);
    }

    /**
     * 그룹 수정 이벤트
     */
    public static CodeGroupChangedEvent groupUpdated(Object source, CodeGroupSource codeSource, String groupCode) {
        return new CodeGroupChangedEvent(source, codeSource, groupCode,
                ChangeType.GROUP_UPDATED, null, null);
    }

    /**
     * 그룹 삭제 이벤트
     */
    public static CodeGroupChangedEvent groupDeleted(Object source, CodeGroupSource codeSource, String groupCode) {
        return new CodeGroupChangedEvent(source, codeSource, groupCode,
                ChangeType.GROUP_DELETED, null, null);
    }

    // ========== 아이템 이벤트 ==========

    /**
     * 아이템 생성 이벤트
     */
    public static CodeGroupChangedEvent itemCreated(Object source, CodeGroupSource codeSource,
                                                     String groupCode, String itemCode) {
        return new CodeGroupChangedEvent(source, codeSource, groupCode,
                ChangeType.ITEM_CREATED, itemCode, null);
    }

    /**
     * 아이템 수정 이벤트
     */
    public static CodeGroupChangedEvent itemUpdated(Object source, CodeGroupSource codeSource,
                                                     String groupCode, String itemCode) {
        return new CodeGroupChangedEvent(source, codeSource, groupCode,
                ChangeType.ITEM_UPDATED, itemCode, null);
    }

    /**
     * 아이템 삭제 이벤트
     */
    public static CodeGroupChangedEvent itemDeleted(Object source, CodeGroupSource codeSource,
                                                     String groupCode, String itemCode) {
        return new CodeGroupChangedEvent(source, codeSource, groupCode,
                ChangeType.ITEM_DELETED, itemCode, null);
    }

    // ========== 마이그레이션 ==========

    /**
     * 그룹 코드 마이그레이션 이벤트
     *
     * @param source       이벤트 소스
     * @param codeSource   코드 소스
     * @param oldGroupCode 기존 그룹 코드
     * @param newGroupCode 새 그룹 코드
     */
    public static CodeGroupChangedEvent migrated(Object source, CodeGroupSource codeSource,
                                                  String oldGroupCode, String newGroupCode) {
        return new CodeGroupChangedEvent(source, codeSource, oldGroupCode,
                ChangeType.MIGRATED, null, newGroupCode);
    }

    /**
     * 그룹 관련 이벤트인지 확인
     */
    public boolean isGroupEvent() {
        return changeType == ChangeType.GROUP_CREATED
                || changeType == ChangeType.GROUP_UPDATED
                || changeType == ChangeType.GROUP_DELETED;
    }

    /**
     * 아이템 관련 이벤트인지 확인
     */
    public boolean isItemEvent() {
        return changeType == ChangeType.ITEM_CREATED
                || changeType == ChangeType.ITEM_UPDATED
                || changeType == ChangeType.ITEM_DELETED;
    }

    @Override
    public String toString() {
        return String.format("CodeGroupChangedEvent[source=%s, groupCode=%s, itemCode=%s, changeType=%s]",
                source, groupCode, itemCode, changeType);
    }
}
