package com.example.admin.codegroup.registry;

import com.example.admin.codegroup.dto.CodeGroupItem;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 정적 Enum 코드 레지스트리 인터페이스.
 *
 * <p>프로젝트에서 {@link com.example.admin.codegroup.annotation.ManagedCode} 어노테이션이
 * 적용된 Enum들을 스캔하여 등록하고, 통합 조회 API를 제공합니다.</p>
 *
 * <p>구현체는 애플리케이션 시작 시 모든 Enum을 스캔하여 캐시합니다.</p>
 */
public interface StaticCodeRegistry {

    /**
     * 모든 등록된 그룹 코드 목록 조회.
     *
     * @return 그룹 코드 Set
     */
    Set<String> getGroupCodes();

    /**
     * 특정 그룹의 모든 코드 항목 조회.
     *
     * @param groupCode 그룹 코드
     * @return 코드 항목 목록 (없으면 빈 목록)
     */
    List<CodeGroupItem> getItems(String groupCode);

    /**
     * 모든 그룹의 코드 항목 조회.
     *
     * @return 그룹 코드 -> 코드 항목 목록 맵
     */
    Map<String, List<CodeGroupItem>> getAllItems();

    /**
     * 특정 코드 항목 조회.
     *
     * @param groupCode 그룹 코드
     * @param itemCode  항목 코드
     * @return 코드 항목 (없으면 empty)
     */
    Optional<CodeGroupItem> getItem(String groupCode, String itemCode);

    /**
     * 그룹 코드 존재 여부 확인.
     *
     * @param groupCode 그룹 코드
     * @return 존재하면 true
     */
    boolean hasGroup(String groupCode);

    /**
     * 특정 그룹의 Enum 클래스 조회.
     *
     * @param groupCode 그룹 코드
     * @return Enum 클래스 (없으면 empty)
     */
    Optional<Class<? extends Enum<?>>> getEnumClass(String groupCode);

    /**
     * 등록된 모든 Enum 클래스 조회.
     *
     * @return Enum 클래스 Set
     */
    Set<Class<? extends Enum<?>>> getRegisteredEnums();

    /**
     * Enum 클래스에서 코드 항목 목록 추출.
     *
     * @param enumClass Enum 클래스
     * @return 코드 항목 목록
     */
    List<CodeGroupItem> getCodeGroupItems(Class<? extends Enum<?>> enumClass);

    /**
     * 그룹 코드로 Enum 클래스 조회 (getEnumClass 별칭).
     *
     * @param groupCode 그룹 코드
     * @return Enum 클래스 (없으면 empty)
     */
    default Optional<Class<? extends Enum<?>>> findByCodeType(String groupCode) {
        return getEnumClass(groupCode);
    }

    /**
     * 캐시 무효화.
     *
     * @param groupCode 무효화할 그룹 코드 (null이면 전체)
     */
    void invalidateCache(String groupCode);
}
