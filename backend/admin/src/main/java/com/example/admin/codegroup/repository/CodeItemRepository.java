package com.example.admin.codegroup.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.admin.codegroup.domain.CodeGroup;
import com.example.admin.codegroup.domain.CodeGroupSource;
import com.example.admin.codegroup.domain.CodeItem;

/**
 * 코드 아이템 리포지토리.
 */
public interface CodeItemRepository extends JpaRepository<CodeItem, UUID> {

    /**
     * 그룹 엔티티로 아이템 목록 조회
     */
    List<CodeItem> findByCodeGroupOrderByDisplayOrderAscItemCodeAsc(CodeGroup codeGroup);

    /**
     * 그룹 코드로 아이템 목록 조회
     */
    List<CodeItem> findByCodeGroup_GroupCodeOrderByDisplayOrderAscItemCodeAsc(String groupCode);

    /**
     * 소스와 그룹 코드로 아이템 목록 조회
     */
    @Query("SELECT i FROM CodeItem i WHERE i.codeGroup.source = :source AND i.codeGroup.groupCode = :groupCode ORDER BY i.displayOrder, i.itemCode")
    List<CodeItem> findBySourceAndGroupCode(@Param("source") CodeGroupSource source, @Param("groupCode") String groupCode);

    /**
     * 그룹 코드와 아이템 코드로 조회
     */
    Optional<CodeItem> findByCodeGroup_GroupCodeAndItemCode(String groupCode, String itemCode);

    /**
     * 소스, 그룹 코드, 아이템 코드로 조회
     */
    @Query("SELECT i FROM CodeItem i WHERE i.codeGroup.source = :source AND i.codeGroup.groupCode = :groupCode AND i.itemCode = :itemCode")
    Optional<CodeItem> findBySourceAndGroupCodeAndItemCode(
            @Param("source") CodeGroupSource source,
            @Param("groupCode") String groupCode,
            @Param("itemCode") String itemCode);

    /**
     * 아이템과 그룹 함께 조회 (N+1 방지)
     */
    @Query("SELECT i FROM CodeItem i JOIN FETCH i.codeGroup WHERE i.id = :id")
    Optional<CodeItem> findByIdWithGroup(@Param("id") UUID id);

    /**
     * 그룹 ID로 아이템 목록 조회 (그룹 함께)
     */
    @Query("SELECT i FROM CodeItem i JOIN FETCH i.codeGroup WHERE i.codeGroup.id = :groupId ORDER BY i.displayOrder, i.itemCode")
    List<CodeItem> findByGroupIdWithGroup(@Param("groupId") UUID groupId);

    /**
     * 그룹 코드별 아이템 개수 조회
     */
    long countByCodeGroup_GroupCode(String groupCode);

    /**
     * 소스와 그룹 코드별 아이템 개수 조회
     */
    @Query("SELECT COUNT(i) FROM CodeItem i WHERE i.codeGroup.source = :source AND i.codeGroup.groupCode = :groupCode")
    long countBySourceAndGroupCode(@Param("source") CodeGroupSource source, @Param("groupCode") String groupCode);

    /**
     * 아이템 코드 존재 여부 확인
     */
    boolean existsByCodeGroup_GroupCodeAndItemCode(String groupCode, String itemCode);

    /**
     * 그룹 ID와 아이템 코드로 존재 여부 확인
     */
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM CodeItem i WHERE i.codeGroup.id = :groupId AND i.itemCode = :itemCode")
    boolean existsByGroupIdAndItemCode(@Param("groupId") UUID groupId, @Param("itemCode") String itemCode);

    /**
     * 그룹 ID와 아이템 코드로 조회
     */
    @Query("SELECT i FROM CodeItem i WHERE i.codeGroup.id = :groupId AND i.itemCode = :itemCode")
    Optional<CodeItem> findByGroupIdAndItemCode(@Param("groupId") UUID groupId, @Param("itemCode") String itemCode);

    /**
     * 소스, 그룹 코드, 아이템 코드 존재 여부 확인
     */
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM CodeItem i WHERE i.codeGroup.source = :source AND i.codeGroup.groupCode = :groupCode AND i.itemCode = :itemCode")
    boolean existsBySourceAndGroupCodeAndItemCode(
            @Param("source") CodeGroupSource source,
            @Param("groupCode") String groupCode,
            @Param("itemCode") String itemCode);

    /**
     * 그룹의 모든 아이템 삭제
     */
    @Modifying
    @Query("DELETE FROM CodeItem i WHERE i.codeGroup.id = :groupId")
    int deleteAllByGroupId(@Param("groupId") UUID groupId);

    /**
     * 활성화된 아이템만 조회
     */
    @Query("SELECT i FROM CodeItem i WHERE i.codeGroup.source = :source AND i.codeGroup.groupCode = :groupCode AND i.active = true ORDER BY i.displayOrder, i.itemCode")
    List<CodeItem> findActiveBySourceAndGroupCode(@Param("source") CodeGroupSource source, @Param("groupCode") String groupCode);

    /**
     * 검색어로 아이템 검색
     */
    @Query("SELECT i FROM CodeItem i WHERE " +
            "(LOWER(i.itemCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(i.itemName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(i.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY i.codeGroup.groupCode, i.displayOrder, i.itemCode")
    List<CodeItem> searchByKeyword(@Param("search") String search);
}
