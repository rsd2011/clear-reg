package com.example.admin.orggroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.admin.approval.domain.ApprovalTemplateRoot;
import com.example.admin.approval.repository.ApprovalTemplateRootRepository;
import com.example.admin.orggroup.domain.OrgGroup;
import com.example.admin.orggroup.domain.OrgGroupApprovalMapping;
import com.example.admin.orggroup.repository.OrgGroupApprovalMappingRepository;
import com.example.common.orggroup.WorkType;
import com.example.admin.orggroup.repository.OrgGroupRepository;

@DisplayName("OrgGroupApprovalMappingService")
class OrgGroupApprovalMappingServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2025-01-15T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    private static final OffsetDateTime NOW = OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);

    private OrgGroupApprovalMappingRepository mappingRepository;
    private OrgGroupRepository orgGroupRepository;
    private ApprovalTemplateRootRepository templateRootRepository;
    private OrgGroupApprovalMappingService service;

    @BeforeEach
    void setUp() {
        mappingRepository = Mockito.mock(OrgGroupApprovalMappingRepository.class);
        orgGroupRepository = Mockito.mock(OrgGroupRepository.class);
        templateRootRepository = Mockito.mock(ApprovalTemplateRootRepository.class);
        service = new OrgGroupApprovalMappingService(
                mappingRepository, orgGroupRepository, templateRootRepository, FIXED_CLOCK);
    }

    private OrgGroup createOrgGroup(String code) {
        return OrgGroup.builder()
                .code(code)
                .name(code + " 그룹")
                .build();
    }

    private ApprovalTemplateRoot createTemplateRoot() {
        return ApprovalTemplateRoot.create(NOW);
    }

    @Nested
    @DisplayName("resolveTemplate")
    class ResolveTemplate {

        @Test
        @DisplayName("Given: 정확한 매핑이 존재하면 When: resolveTemplate 호출 Then: 해당 템플릿 반환")
        void returnsExactMatch() {
            OrgGroup orgGroup = createOrgGroup("SALES");
            ApprovalTemplateRoot template = createTemplateRoot();
            OrgGroupApprovalMapping mapping = OrgGroupApprovalMapping.create(
                    orgGroup, WorkType.FILE_EXPORT, template, NOW);

            given(mappingRepository.findByOrgGroupCodeAndWorkType("SALES", WorkType.FILE_EXPORT))
                    .willReturn(Optional.of(mapping));

            Optional<ApprovalTemplateRoot> result = service.resolveTemplate("SALES", WorkType.FILE_EXPORT);

            assertThat(result).contains(template);
        }

        @Test
        @DisplayName("Given: 정확한 매핑이 없고 기본 매핑이 있으면 When: resolveTemplate 호출 Then: 기본 템플릿 반환")
        void returnsDefaultWhenNoExactMatch() {
            OrgGroup orgGroup = createOrgGroup("SALES");
            ApprovalTemplateRoot defaultTemplate = createTemplateRoot();
            OrgGroupApprovalMapping defaultMapping = OrgGroupApprovalMapping.createDefault(
                    orgGroup, defaultTemplate, NOW);

            given(mappingRepository.findByOrgGroupCodeAndWorkType("SALES", WorkType.FILE_EXPORT))
                    .willReturn(Optional.empty());
            given(mappingRepository.findByOrgGroupCodeAndWorkType("SALES", null))
                    .willReturn(Optional.of(defaultMapping));

            Optional<ApprovalTemplateRoot> result = service.resolveTemplate("SALES", WorkType.FILE_EXPORT);

            assertThat(result).contains(defaultTemplate);
        }

        @Test
        @DisplayName("Given: 매핑이 전혀 없으면 When: resolveTemplate 호출 Then: empty 반환")
        void returnsEmptyWhenNoMapping() {
            given(mappingRepository.findByOrgGroupCodeAndWorkType("SALES", WorkType.FILE_EXPORT))
                    .willReturn(Optional.empty());
            given(mappingRepository.findByOrgGroupCodeAndWorkType("SALES", null))
                    .willReturn(Optional.empty());

            Optional<ApprovalTemplateRoot> result = service.resolveTemplate("SALES", WorkType.FILE_EXPORT);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given: workType이 null이면 When: resolveTemplate 호출 Then: 기본 템플릿만 조회")
        void returnsDefaultWhenWorkTypeNull() {
            OrgGroup orgGroup = createOrgGroup("SALES");
            ApprovalTemplateRoot defaultTemplate = createTemplateRoot();
            OrgGroupApprovalMapping defaultMapping = OrgGroupApprovalMapping.createDefault(
                    orgGroup, defaultTemplate, NOW);

            given(mappingRepository.findByOrgGroupCodeAndWorkType("SALES", null))
                    .willReturn(Optional.of(defaultMapping));

            Optional<ApprovalTemplateRoot> result = service.resolveTemplate("SALES", null);

            assertThat(result).contains(defaultTemplate);
        }

        @Test
        @DisplayName("Given: orgGroupCode가 null이면 When: resolveTemplate 호출 Then: empty 반환")
        void returnsEmptyWhenOrgGroupCodeNull() {
            Optional<ApprovalTemplateRoot> result = service.resolveTemplate(null, WorkType.GENERAL);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given: orgGroupCode가 빈 문자열이면 When: resolveTemplate 호출 Then: empty 반환")
        void returnsEmptyWhenOrgGroupCodeBlank() {
            Optional<ApprovalTemplateRoot> result = service.resolveTemplate("   ", WorkType.GENERAL);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createMapping")
    class CreateMapping {

        @Test
        @DisplayName("Given: 조직그룹과 템플릿이 존재하면 When: createMapping 호출 Then: 매핑 생성")
        void createsMapping() {
            OrgGroup orgGroup = createOrgGroup("SALES");
            ApprovalTemplateRoot template = createTemplateRoot();
            UUID templateId = template.getId();

            given(orgGroupRepository.findByCode("SALES")).willReturn(Optional.of(orgGroup));
            given(templateRootRepository.findById(templateId)).willReturn(Optional.of(template));
            given(mappingRepository.existsByOrgGroupAndWorkType(orgGroup, WorkType.FILE_EXPORT))
                    .willReturn(false);
            given(mappingRepository.save(any(OrgGroupApprovalMapping.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            OrgGroupApprovalMapping result = service.createMapping("SALES", WorkType.FILE_EXPORT, templateId);

            assertThat(result.getOrgGroup()).isEqualTo(orgGroup);
            assertThat(result.getWorkType()).isEqualTo(WorkType.FILE_EXPORT);
            assertThat(result.getApprovalTemplateRoot()).isEqualTo(template);
            verify(mappingRepository).save(any(OrgGroupApprovalMapping.class));
        }

        @Test
        @DisplayName("Given: 조직그룹이 없으면 When: createMapping 호출 Then: 예외 발생")
        void throwsWhenOrgGroupNotFound() {
            given(orgGroupRepository.findByCode("UNKNOWN")).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.createMapping("UNKNOWN", WorkType.GENERAL, UUID.randomUUID()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("조직그룹을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("Given: 템플릿이 없으면 When: createMapping 호출 Then: 예외 발생")
        void throwsWhenTemplateNotFound() {
            OrgGroup orgGroup = createOrgGroup("SALES");
            UUID unknownTemplateId = UUID.randomUUID();

            given(orgGroupRepository.findByCode("SALES")).willReturn(Optional.of(orgGroup));
            given(templateRootRepository.findById(unknownTemplateId)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.createMapping("SALES", WorkType.GENERAL, unknownTemplateId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("승인선 템플릿을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("Given: 이미 매핑이 존재하면 When: createMapping 호출 Then: 예외 발생")
        void throwsWhenMappingExists() {
            OrgGroup orgGroup = createOrgGroup("SALES");
            ApprovalTemplateRoot template = createTemplateRoot();
            UUID templateId = template.getId();

            given(orgGroupRepository.findByCode("SALES")).willReturn(Optional.of(orgGroup));
            given(templateRootRepository.findById(templateId)).willReturn(Optional.of(template));
            given(mappingRepository.existsByOrgGroupAndWorkType(orgGroup, WorkType.FILE_EXPORT))
                    .willReturn(true);

            assertThatThrownBy(() ->
                    service.createMapping("SALES", WorkType.FILE_EXPORT, templateId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 존재하는 매핑입니다");
        }
    }

    @Nested
    @DisplayName("createOrUpdateMapping")
    class CreateOrUpdateMapping {

        @Test
        @DisplayName("Given: 매핑이 없으면 When: createOrUpdateMapping 호출 Then: 새 매핑 생성")
        void createsNewMapping() {
            OrgGroup orgGroup = createOrgGroup("SALES");
            ApprovalTemplateRoot template = createTemplateRoot();
            UUID templateId = template.getId();

            given(orgGroupRepository.findByCode("SALES")).willReturn(Optional.of(orgGroup));
            given(templateRootRepository.findById(templateId)).willReturn(Optional.of(template));
            given(mappingRepository.findByOrgGroupAndWorkType(orgGroup, WorkType.FILE_EXPORT))
                    .willReturn(Optional.empty());
            given(mappingRepository.save(any(OrgGroupApprovalMapping.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            OrgGroupApprovalMapping result = service.createOrUpdateMapping("SALES", WorkType.FILE_EXPORT, templateId);

            assertThat(result.getOrgGroup()).isEqualTo(orgGroup);
            assertThat(result.getWorkType()).isEqualTo(WorkType.FILE_EXPORT);
            verify(mappingRepository).save(any(OrgGroupApprovalMapping.class));
        }

        @Test
        @DisplayName("Given: 매핑이 있으면 When: createOrUpdateMapping 호출 Then: 기존 매핑 업데이트")
        void updatesExistingMapping() {
            OrgGroup orgGroup = createOrgGroup("SALES");
            ApprovalTemplateRoot oldTemplate = createTemplateRoot();
            ApprovalTemplateRoot newTemplate = createTemplateRoot();
            UUID newTemplateId = newTemplate.getId();
            OrgGroupApprovalMapping existingMapping = OrgGroupApprovalMapping.create(
                    orgGroup, WorkType.FILE_EXPORT, oldTemplate, NOW.minusDays(1));

            given(orgGroupRepository.findByCode("SALES")).willReturn(Optional.of(orgGroup));
            given(templateRootRepository.findById(newTemplateId)).willReturn(Optional.of(newTemplate));
            given(mappingRepository.findByOrgGroupAndWorkType(orgGroup, WorkType.FILE_EXPORT))
                    .willReturn(Optional.of(existingMapping));

            OrgGroupApprovalMapping result = service.createOrUpdateMapping("SALES", WorkType.FILE_EXPORT, newTemplateId);

            assertThat(result.getApprovalTemplateRoot()).isEqualTo(newTemplate);
            assertThat(result.getUpdatedAt()).isEqualTo(NOW);
        }
    }

    @Nested
    @DisplayName("resolveTemplateById")
    class ResolveTemplateById {

        @Test
        @DisplayName("Given: 정확한 매핑이 존재하면 When: resolveTemplateById 호출 Then: 해당 템플릿 반환")
        void returnsExactMatch() {
            OrgGroup orgGroup = createOrgGroup("SALES");
            ApprovalTemplateRoot template = createTemplateRoot();
            OrgGroupApprovalMapping mapping = OrgGroupApprovalMapping.create(
                    orgGroup, WorkType.FILE_EXPORT, template, NOW);
            UUID orgGroupId = orgGroup.getId();

            given(mappingRepository.findByOrgGroupIdAndWorkType(orgGroupId, WorkType.FILE_EXPORT))
                    .willReturn(Optional.of(mapping));

            Optional<ApprovalTemplateRoot> result = service.resolveTemplateById(orgGroupId, WorkType.FILE_EXPORT);

            assertThat(result).contains(template);
        }

        @Test
        @DisplayName("Given: 정확한 매핑이 없고 기본 매핑이 있으면 When: resolveTemplateById 호출 Then: 기본 템플릿 반환")
        void returnsDefaultWhenNoExactMatch() {
            OrgGroup orgGroup = createOrgGroup("SALES");
            ApprovalTemplateRoot defaultTemplate = createTemplateRoot();
            OrgGroupApprovalMapping defaultMapping = OrgGroupApprovalMapping.createDefault(
                    orgGroup, defaultTemplate, NOW);
            UUID orgGroupId = orgGroup.getId();

            given(mappingRepository.findByOrgGroupIdAndWorkType(orgGroupId, WorkType.FILE_EXPORT))
                    .willReturn(Optional.empty());
            given(mappingRepository.findDefaultByOrgGroupId(orgGroupId))
                    .willReturn(Optional.of(defaultMapping));

            Optional<ApprovalTemplateRoot> result = service.resolveTemplateById(orgGroupId, WorkType.FILE_EXPORT);

            assertThat(result).contains(defaultTemplate);
        }

        @Test
        @DisplayName("Given: orgGroupId가 null이면 When: resolveTemplateById 호출 Then: empty 반환")
        void returnsEmptyWhenOrgGroupIdNull() {
            Optional<ApprovalTemplateRoot> result = service.resolveTemplateById(null, WorkType.GENERAL);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given: workType이 null이면 When: resolveTemplateById 호출 Then: 기본 템플릿만 조회")
        void returnsDefaultWhenWorkTypeNull() {
            OrgGroup orgGroup = createOrgGroup("SALES");
            ApprovalTemplateRoot defaultTemplate = createTemplateRoot();
            OrgGroupApprovalMapping defaultMapping = OrgGroupApprovalMapping.createDefault(
                    orgGroup, defaultTemplate, NOW);
            UUID orgGroupId = orgGroup.getId();

            given(mappingRepository.findDefaultByOrgGroupId(orgGroupId))
                    .willReturn(Optional.of(defaultMapping));

            Optional<ApprovalTemplateRoot> result = service.resolveTemplateById(orgGroupId, null);

            assertThat(result).contains(defaultTemplate);
        }
    }

    @Nested
    @DisplayName("findByOrgGroupCode")
    class FindByOrgGroupCode {

        @Test
        @DisplayName("Given: 조직그룹에 매핑이 있으면 When: findByOrgGroupCode 호출 Then: 매핑 목록 반환")
        void returnsMappings() {
            OrgGroup orgGroup = createOrgGroup("SALES");
            ApprovalTemplateRoot template = createTemplateRoot();
            OrgGroupApprovalMapping mapping = OrgGroupApprovalMapping.create(
                    orgGroup, WorkType.GENERAL, template, NOW);

            given(mappingRepository.findByOrgGroupCode("SALES")).willReturn(List.of(mapping));

            List<OrgGroupApprovalMapping> result = service.findByOrgGroupCode("SALES");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getWorkType()).isEqualTo(WorkType.GENERAL);
        }
    }

    @Nested
    @DisplayName("findByOrgGroupId")
    class FindByOrgGroupId {

        @Test
        @DisplayName("Given: 조직그룹 ID에 매핑이 있으면 When: findByOrgGroupId 호출 Then: 매핑 목록 반환")
        void returnsMappings() {
            OrgGroup orgGroup = createOrgGroup("SALES");
            ApprovalTemplateRoot template = createTemplateRoot();
            OrgGroupApprovalMapping mapping = OrgGroupApprovalMapping.create(
                    orgGroup, WorkType.GENERAL, template, NOW);
            UUID orgGroupId = orgGroup.getId();

            given(mappingRepository.findByOrgGroupIdWithTemplate(orgGroupId)).willReturn(List.of(mapping));

            List<OrgGroupApprovalMapping> result = service.findByOrgGroupId(orgGroupId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getWorkType()).isEqualTo(WorkType.GENERAL);
        }
    }

    @Nested
    @DisplayName("deleteMapping")
    class DeleteMapping {

        @Test
        @DisplayName("Given: 매핑이 존재하면 When: deleteMapping 호출 Then: 삭제 성공")
        void deletesMapping() {
            UUID mappingId = UUID.randomUUID();
            given(mappingRepository.existsById(mappingId)).willReturn(true);

            service.deleteMapping(mappingId);

            verify(mappingRepository).deleteById(mappingId);
        }

        @Test
        @DisplayName("Given: 매핑이 없으면 When: deleteMapping 호출 Then: 예외 발생")
        void throwsWhenMappingNotFound() {
            UUID unknownId = UUID.randomUUID();
            given(mappingRepository.existsById(unknownId)).willReturn(false);

            assertThatThrownBy(() -> service.deleteMapping(unknownId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("매핑을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("deleteMappingByOrgGroupAndWorkType")
    class DeleteMappingByOrgGroupAndWorkType {

        @Test
        @DisplayName("Given: 매핑이 존재하면 When: deleteMappingByOrgGroupAndWorkType 호출 Then: 삭제 성공")
        void deletesMapping() {
            OrgGroup orgGroup = createOrgGroup("SALES");
            ApprovalTemplateRoot template = createTemplateRoot();
            OrgGroupApprovalMapping mapping = OrgGroupApprovalMapping.create(
                    orgGroup, WorkType.FILE_EXPORT, template, NOW);

            given(orgGroupRepository.findByCode("SALES")).willReturn(Optional.of(orgGroup));
            given(mappingRepository.findByOrgGroupAndWorkType(orgGroup, WorkType.FILE_EXPORT))
                    .willReturn(Optional.of(mapping));

            service.deleteMappingByOrgGroupAndWorkType("SALES", WorkType.FILE_EXPORT);

            verify(mappingRepository).delete(mapping);
        }

        @Test
        @DisplayName("Given: 조직그룹이 없으면 When: deleteMappingByOrgGroupAndWorkType 호출 Then: 예외 발생")
        void throwsWhenOrgGroupNotFound() {
            given(orgGroupRepository.findByCode("UNKNOWN")).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.deleteMappingByOrgGroupAndWorkType("UNKNOWN", WorkType.GENERAL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("조직그룹을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("Given: 매핑이 없으면 When: deleteMappingByOrgGroupAndWorkType 호출 Then: 예외 발생")
        void throwsWhenMappingNotFound() {
            OrgGroup orgGroup = createOrgGroup("SALES");

            given(orgGroupRepository.findByCode("SALES")).willReturn(Optional.of(orgGroup));
            given(mappingRepository.findByOrgGroupAndWorkType(orgGroup, WorkType.FILE_EXPORT))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.deleteMappingByOrgGroupAndWorkType("SALES", WorkType.FILE_EXPORT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("매핑을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("isTemplateInUse")
    class IsTemplateInUse {

        @Test
        @DisplayName("Given: 템플릿을 사용하는 매핑이 있으면 When: isTemplateInUse 호출 Then: true 반환")
        void returnsTrueWhenInUse() {
            UUID templateRootId = UUID.randomUUID();
            given(mappingRepository.existsByApprovalTemplateRootId(templateRootId)).willReturn(true);

            assertThat(service.isTemplateInUse(templateRootId)).isTrue();
        }

        @Test
        @DisplayName("Given: 템플릿을 사용하는 매핑이 없으면 When: isTemplateInUse 호출 Then: false 반환")
        void returnsFalseWhenNotInUse() {
            UUID templateRootId = UUID.randomUUID();
            given(mappingRepository.existsByApprovalTemplateRootId(templateRootId)).willReturn(false);

            assertThat(service.isTemplateInUse(templateRootId)).isFalse();
        }
    }
}
