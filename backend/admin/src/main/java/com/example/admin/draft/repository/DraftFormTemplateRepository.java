package com.example.admin.draft.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.admin.draft.domain.DraftFormTemplate;
import com.example.admin.draft.domain.TemplateScope;

/**
 * 기안 양식 템플릿 리포지토리.
 */
public interface DraftFormTemplateRepository extends JpaRepository<DraftFormTemplate, UUID> {

    /**
     * 활성화된 템플릿을 ID로 조회한다.
     *
     * @param id 템플릿 ID
     * @return 활성화된 템플릿 (없으면 empty)
     */
    Optional<DraftFormTemplate> findByIdAndActiveTrue(UUID id);

    /**
     * 템플릿 코드로 조회한다.
     *
     * @param templateCode 템플릿 코드
     * @return 템플릿 (없으면 empty)
     */
    Optional<DraftFormTemplate> findByTemplateCode(String templateCode);

    /**
     * 활성화된 템플릿을 템플릿 코드로 조회한다.
     *
     * @param templateCode 템플릿 코드
     * @return 활성화된 템플릿 (없으면 empty)
     */
    Optional<DraftFormTemplate> findByTemplateCodeAndActiveTrue(String templateCode);

    /**
     * 비즈니스 유형과 조직코드로 활성화된 템플릿 목록을 조회한다.
     *
     * @param businessType     비즈니스 유형
     * @param organizationCode 조직 코드
     * @return 활성화된 템플릿 목록
     */
    List<DraftFormTemplate> findByBusinessTypeAndOrganizationCodeAndActiveTrue(
            String businessType, String organizationCode);

    /**
     * 비즈니스 유형으로 전역 템플릿 목록을 조회한다.
     *
     * @param businessType 비즈니스 유형
     * @return 활성화된 전역 템플릿 목록
     */
    List<DraftFormTemplate> findByBusinessTypeAndScopeAndActiveTrue(String businessType, TemplateScope scope);

    /**
     * 조직에서 사용 가능한 모든 템플릿을 조회한다.
     * (전역 템플릿 + 해당 조직 템플릿)
     *
     * @param businessType     비즈니스 유형
     * @param organizationCode 조직 코드
     * @return 사용 가능한 템플릿 목록
     */
    @Query("SELECT t FROM DraftFormTemplate t WHERE t.active = true " +
            "AND t.businessType = :businessType " +
            "AND (t.scope = 'GLOBAL' OR t.organizationCode = :organizationCode) " +
            "ORDER BY t.scope, t.name")
    List<DraftFormTemplate> findAvailableTemplates(
            @Param("businessType") String businessType,
            @Param("organizationCode") String organizationCode);

    /**
     * 템플릿 코드 존재 여부를 확인한다.
     *
     * @param templateCode 템플릿 코드
     * @return 존재하면 true
     */
    boolean existsByTemplateCode(String templateCode);
}
