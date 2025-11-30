package com.example.admin.orggroup.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.approval.domain.ApprovalTemplateRoot;
import com.example.admin.approval.repository.ApprovalTemplateRootRepository;
import com.example.admin.orggroup.domain.OrgGroup;
import com.example.admin.orggroup.domain.OrgGroupApprovalMapping;
import com.example.admin.orggroup.repository.OrgGroupApprovalMappingRepository;
import com.example.common.orggroup.WorkType;
import com.example.admin.orggroup.repository.OrgGroupRepository;

import lombok.RequiredArgsConstructor;

/**
 * 조직그룹별 승인선 템플릿 매핑 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrgGroupApprovalMappingService {

    private final OrgGroupApprovalMappingRepository mappingRepository;
    private final OrgGroupRepository orgGroupRepository;
    private final ApprovalTemplateRootRepository templateRootRepository;
    private final Clock clock;

    /**
     * 조직그룹과 업무유형에 해당하는 승인선 템플릿을 조회한다.
     * <p>
     * 조회 우선순위:
     * <ol>
     *   <li>조직그룹 + 업무유형 정확히 일치하는 매핑</li>
     *   <li>조직그룹의 기본 템플릿 (workType = null)</li>
     * </ol>
     *
     * @param orgGroupCode 조직그룹 코드
     * @param workType     업무유형 (nullable)
     * @return 승인선 템플릿 (없으면 empty)
     */
    public Optional<ApprovalTemplateRoot> resolveTemplate(String orgGroupCode, WorkType workType) {
        if (orgGroupCode == null || orgGroupCode.isBlank()) {
            return Optional.empty();
        }

        // 1. 정확한 매핑 조회
        if (workType != null) {
            Optional<OrgGroupApprovalMapping> exactMatch =
                    mappingRepository.findByOrgGroupCodeAndWorkType(orgGroupCode, workType);
            if (exactMatch.isPresent()) {
                return Optional.of(exactMatch.get().getApprovalTemplateRoot());
            }
        }

        // 2. 기본 템플릿 조회 (workType = null)
        return mappingRepository.findByOrgGroupCodeAndWorkType(orgGroupCode, null)
                .map(OrgGroupApprovalMapping::getApprovalTemplateRoot);
    }

    /**
     * 조직그룹 ID와 업무유형에 해당하는 승인선 템플릿을 조회한다.
     *
     * @param orgGroupId 조직그룹 ID
     * @param workType   업무유형 (nullable)
     * @return 승인선 템플릿 (없으면 empty)
     */
    public Optional<ApprovalTemplateRoot> resolveTemplateById(UUID orgGroupId, WorkType workType) {
        if (orgGroupId == null) {
            return Optional.empty();
        }

        // 1. 정확한 매핑 조회
        if (workType != null) {
            Optional<OrgGroupApprovalMapping> exactMatch =
                    mappingRepository.findByOrgGroupIdAndWorkType(orgGroupId, workType);
            if (exactMatch.isPresent()) {
                return Optional.of(exactMatch.get().getApprovalTemplateRoot());
            }
        }

        // 2. 기본 템플릿 조회
        return mappingRepository.findDefaultByOrgGroupId(orgGroupId)
                .map(OrgGroupApprovalMapping::getApprovalTemplateRoot);
    }

    /**
     * 조직그룹의 모든 매핑을 조회한다.
     *
     * @param orgGroupCode 조직그룹 코드
     * @return 매핑 목록
     */
    public List<OrgGroupApprovalMapping> findByOrgGroupCode(String orgGroupCode) {
        return mappingRepository.findByOrgGroupCode(orgGroupCode);
    }

    /**
     * 조직그룹 ID의 모든 매핑을 조회한다.
     *
     * @param orgGroupId 조직그룹 ID
     * @return 매핑 목록
     */
    public List<OrgGroupApprovalMapping> findByOrgGroupId(UUID orgGroupId) {
        return mappingRepository.findByOrgGroupIdWithTemplate(orgGroupId);
    }

    /**
     * 매핑을 생성한다.
     *
     * @param orgGroupCode   조직그룹 코드
     * @param workType       업무유형 (nullable)
     * @param templateRootId 템플릿 루트 ID
     * @return 생성된 매핑
     */
    @Transactional
    public OrgGroupApprovalMapping createMapping(
            String orgGroupCode,
            WorkType workType,
            UUID templateRootId) {
        OrgGroup orgGroup = orgGroupRepository.findByCode(orgGroupCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "조직그룹을 찾을 수 없습니다: " + orgGroupCode));

        ApprovalTemplateRoot templateRoot = templateRootRepository.findById(templateRootId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "승인선 템플릿을 찾을 수 없습니다: " + templateRootId));

        if (mappingRepository.existsByOrgGroupAndWorkType(orgGroup, workType)) {
            throw new IllegalStateException(
                    "이미 존재하는 매핑입니다: orgGroup=" + orgGroupCode + ", workType=" + workType);
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        OrgGroupApprovalMapping mapping =
                OrgGroupApprovalMapping.create(orgGroup, workType, templateRoot, now);

        return mappingRepository.save(mapping);
    }

    /**
     * 매핑을 생성하거나 업데이트한다.
     *
     * @param orgGroupCode   조직그룹 코드
     * @param workType       업무유형 (nullable)
     * @param templateRootId 템플릿 루트 ID
     * @return 생성 또는 업데이트된 매핑
     */
    @Transactional
    public OrgGroupApprovalMapping createOrUpdateMapping(
            String orgGroupCode,
            WorkType workType,
            UUID templateRootId) {
        OrgGroup orgGroup = orgGroupRepository.findByCode(orgGroupCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "조직그룹을 찾을 수 없습니다: " + orgGroupCode));

        ApprovalTemplateRoot templateRoot = templateRootRepository.findById(templateRootId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "승인선 템플릿을 찾을 수 없습니다: " + templateRootId));

        OffsetDateTime now = OffsetDateTime.now(clock);

        Optional<OrgGroupApprovalMapping> existing =
                mappingRepository.findByOrgGroupAndWorkType(orgGroup, workType);

        if (existing.isPresent()) {
            OrgGroupApprovalMapping mapping = existing.get();
            mapping.changeTemplate(templateRoot, now);
            return mapping;
        } else {
            OrgGroupApprovalMapping mapping =
                    OrgGroupApprovalMapping.create(orgGroup, workType, templateRoot, now);
            return mappingRepository.save(mapping);
        }
    }

    /**
     * 매핑을 삭제한다.
     *
     * @param mappingId 매핑 ID
     */
    @Transactional
    public void deleteMapping(UUID mappingId) {
        if (!mappingRepository.existsById(mappingId)) {
            throw new IllegalArgumentException("매핑을 찾을 수 없습니다: " + mappingId);
        }
        mappingRepository.deleteById(mappingId);
    }

    /**
     * 조직그룹과 업무유형으로 매핑을 삭제한다.
     *
     * @param orgGroupCode 조직그룹 코드
     * @param workType     업무유형 (nullable)
     */
    @Transactional
    public void deleteMappingByOrgGroupAndWorkType(String orgGroupCode, WorkType workType) {
        OrgGroup orgGroup = orgGroupRepository.findByCode(orgGroupCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "조직그룹을 찾을 수 없습니다: " + orgGroupCode));

        OrgGroupApprovalMapping mapping = mappingRepository.findByOrgGroupAndWorkType(orgGroup, workType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "매핑을 찾을 수 없습니다: orgGroup=" + orgGroupCode + ", workType=" + workType));

        mappingRepository.delete(mapping);
    }

    /**
     * 특정 템플릿을 사용하는 매핑이 존재하는지 확인한다.
     *
     * @param templateRootId 템플릿 루트 ID
     * @return 사용 중이면 true
     */
    public boolean isTemplateInUse(UUID templateRootId) {
        return mappingRepository.existsByApprovalTemplateRootId(templateRootId);
    }
}
