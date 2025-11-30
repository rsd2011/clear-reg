package com.example.admin.draft.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.admin.draft.domain.DraftTemplatePreset;

/**
 * 기안 템플릿 프리셋 리포지토리.
 */
public interface DraftTemplatePresetRepository extends JpaRepository<DraftTemplatePreset, UUID> {

    /**
     * 활성화된 프리셋을 ID로 조회한다.
     *
     * @param id 프리셋 ID
     * @return 활성화된 프리셋 (없으면 empty)
     */
    Optional<DraftTemplatePreset> findByIdAndActiveTrue(UUID id);

    /**
     * 프리셋 코드로 조회한다.
     *
     * @param presetCode 프리셋 코드
     * @return 프리셋 (없으면 empty)
     */
    Optional<DraftTemplatePreset> findByPresetCode(String presetCode);

    /**
     * 활성화된 프리셋을 프리셋 코드로 조회한다.
     *
     * @param presetCode 프리셋 코드
     * @return 활성화된 프리셋 (없으면 empty)
     */
    Optional<DraftTemplatePreset> findByPresetCodeAndActiveTrue(String presetCode);

    /**
     * 비즈니스 기능 코드와 조직코드로 활성화된 프리셋 목록을 조회한다.
     *
     * @param businessFeatureCode 비즈니스 기능 코드
     * @param organizationCode    조직 코드
     * @return 활성화된 프리셋 목록
     */
    List<DraftTemplatePreset> findByBusinessFeatureCodeAndOrganizationCodeAndActiveTrue(
            String businessFeatureCode, String organizationCode);

    /**
     * 비즈니스 기능 코드로 전역 프리셋 목록을 조회한다.
     *
     * @param businessFeatureCode 비즈니스 기능 코드
     * @return 활성화된 전역 프리셋 목록
     */
    List<DraftTemplatePreset> findByBusinessFeatureCodeAndOrganizationCodeIsNullAndActiveTrue(
            String businessFeatureCode);

    /**
     * 조직에서 사용 가능한 모든 프리셋을 조회한다.
     * (전역 프리셋 + 해당 조직 프리셋)
     *
     * @param businessFeatureCode 비즈니스 기능 코드
     * @param organizationCode    조직 코드
     * @return 사용 가능한 프리셋 목록
     */
    @Query("SELECT p FROM DraftTemplatePreset p WHERE p.active = true " +
            "AND p.businessFeatureCode = :businessFeatureCode " +
            "AND (p.scope = 'GLOBAL' OR p.organizationCode = :organizationCode) " +
            "ORDER BY p.scope, p.name")
    List<DraftTemplatePreset> findAvailablePresets(
            @Param("businessFeatureCode") String businessFeatureCode,
            @Param("organizationCode") String organizationCode);

    /**
     * 특정 양식 템플릿을 사용하는 프리셋이 존재하는지 확인한다.
     *
     * @param formTemplateId 양식 템플릿 ID
     * @return 사용 중이면 true
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
            "FROM DraftTemplatePreset p WHERE p.formTemplate.id = :formTemplateId")
    boolean existsByFormTemplateId(@Param("formTemplateId") UUID formTemplateId);

    /**
     * 프리셋 코드 존재 여부를 확인한다.
     *
     * @param presetCode 프리셋 코드
     * @return 존재하면 true
     */
    boolean existsByPresetCode(String presetCode);
}
