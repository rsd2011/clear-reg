package com.example.admin.codegroup.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.admin.codegroup.domain.CodeGroup;
import com.example.admin.codegroup.domain.CodeGroupSource;

/**
 * 코드 그룹 리포지토리.
 */
public interface CodeGroupRepository extends JpaRepository<CodeGroup, UUID> {

    /**
     * 소스와 그룹 코드로 조회
     */
    Optional<CodeGroup> findBySourceAndGroupCode(CodeGroupSource source, String groupCode);

    /**
     * 그룹 코드로 조회 (소스 무관, 첫 번째 매칭)
     */
    Optional<CodeGroup> findFirstByGroupCode(String groupCode);

    /**
     * 그룹 코드로 모든 소스의 그룹 조회
     */
    List<CodeGroup> findAllByGroupCode(String groupCode);

    /**
     * 소스로 모든 그룹 조회
     */
    List<CodeGroup> findAllBySourceOrderByDisplayOrderAscGroupCodeAsc(CodeGroupSource source);

    /**
     * 활성화된 그룹만 조회
     */
    List<CodeGroup> findAllByActiveTrue();

    /**
     * 소스와 활성화 상태로 조회
     */
    List<CodeGroup> findAllBySourceAndActiveOrderByDisplayOrderAsc(CodeGroupSource source, boolean active);

    /**
     * 소스와 그룹 코드 존재 여부 확인
     */
    boolean existsBySourceAndGroupCode(CodeGroupSource source, String groupCode);

    /**
     * 모든 고유 그룹 코드 조회
     */
    @Query("SELECT DISTINCT g.groupCode FROM CodeGroup g ORDER BY g.groupCode")
    List<String> findDistinctGroupCodes();

    /**
     * 소스별 고유 그룹 코드 조회
     */
    @Query("SELECT DISTINCT g.groupCode FROM CodeGroup g WHERE g.source = :source ORDER BY g.groupCode")
    List<String> findDistinctGroupCodesBySource(@Param("source") CodeGroupSource source);

    /**
     * 그룹 코드별 아이템 개수 조회
     */
    @Query("SELECT COUNT(i) FROM CodeItem i WHERE i.codeGroup.groupCode = :groupCode")
    long countItemsByGroupCode(@Param("groupCode") String groupCode);

    /**
     * 소스와 그룹 코드별 아이템 개수 조회
     */
    @Query("SELECT COUNT(i) FROM CodeItem i WHERE i.codeGroup.source = :source AND i.codeGroup.groupCode = :groupCode")
    long countItemsBySourceAndGroupCode(@Param("source") CodeGroupSource source, @Param("groupCode") String groupCode);

    /**
     * 그룹과 아이템을 함께 조회 (N+1 방지)
     */
    @Query("SELECT DISTINCT g FROM CodeGroup g LEFT JOIN FETCH g.items WHERE g.source = :source AND g.groupCode = :groupCode")
    Optional<CodeGroup> findBySourceAndGroupCodeWithItems(@Param("source") CodeGroupSource source, @Param("groupCode") String groupCode);

    /**
     * 모든 그룹과 아이템을 함께 조회
     */
    @Query("SELECT DISTINCT g FROM CodeGroup g LEFT JOIN FETCH g.items ORDER BY g.source, g.displayOrder, g.groupCode")
    List<CodeGroup> findAllWithItems();

    /**
     * 소스별 그룹과 아이템을 함께 조회
     */
    @Query("SELECT DISTINCT g FROM CodeGroup g LEFT JOIN FETCH g.items WHERE g.source = :source ORDER BY g.displayOrder, g.groupCode")
    List<CodeGroup> findAllBySourceWithItems(@Param("source") CodeGroupSource source);
}
