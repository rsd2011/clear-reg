package com.example.admin.orggroup.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import com.example.admin.orggroup.domain.OrgGroup;
import com.example.admin.orggroup.domain.OrgGroupMember;
import com.example.admin.orggroup.domain.OrgGroupRolePermission;
import com.example.admin.orggroup.properties.OrgGroupSettingsProperties;
import com.example.common.orggroup.OrgGroupRoleType;
import com.example.common.orggroup.WorkCategory;
import com.example.admin.orggroup.repository.OrgGroupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrgGroupCoverageTest {

    private static final OffsetDateTime NOW = OffsetDateTime.now();

    @Test
    @DisplayName("OrgGroup/Member builder 기본값과 필드 세터 검증")
    void orgGroupDefaults() {
        // OrgGroup: sort 및 RolePermission 필드
        OrgGroup group =
                OrgGroup.builder()
                        .code("SALES")
                        .name("영업")
                        .description("desc")
                        .sort(10)
                        .build();

        // 역할별 권한 추가
        group.addRolePermission(OrgGroupRolePermission.create(group, OrgGroupRoleType.LEADER, "LEADER", NOW));
        group.addRolePermission(OrgGroupRolePermission.create(group, OrgGroupRoleType.MANAGER, "RESP", NOW));
        group.addRolePermission(OrgGroupRolePermission.create(group, OrgGroupRoleType.MEMBER, "MEMBER", NOW));

        assertThat(group.getSort()).isEqualTo(10);
        assertThat(group.getPermGroupCodeByRole(OrgGroupRoleType.LEADER)).isEqualTo("LEADER");
        assertThat(group.getPermGroupCodeByRole(OrgGroupRoleType.MANAGER)).isEqualTo("RESP");
        assertThat(group.getPermGroupCodeByRole(OrgGroupRoleType.MEMBER)).isEqualTo("MEMBER");

        // WorkCategory 추가/조회 테스트
        group.addWorkCategory(WorkCategory.SALES);
        group.addWorkCategory(WorkCategory.COMPLIANCE);
        assertThat(group.getWorkCategories()).containsExactlyInAnyOrder(
                WorkCategory.SALES, WorkCategory.COMPLIANCE);
        assertThat(group.hasWorkCategory(WorkCategory.SALES)).isTrue();

        // OrgGroupMember: displayOrder 필드 검증
        OrgGroupMember member =
                OrgGroupMember.builder()
                        .orgGroup(group)
                        .orgId("001")
                        .displayOrder(5)
                        .build();
        assertThat(member.getDisplayOrder()).isEqualTo(5);
        assertThat(member.getGroupCode()).isEqualTo("SALES");
        assertThat(member.getOrgId()).isEqualTo("001");
        assertThat(member.getOrgGroup()).isEqualTo(group);
    }

    @Test
    @DisplayName("OrgGroupSettingsProperties는 YAML 기본값을 보존한다")
    void orgGroupSettingsPropertiesDefaults() {
        OrgGroupSettingsProperties props = new OrgGroupSettingsProperties();
        assertThat(props.getDefaultGroups()).isEmpty();
        assertThat(props.getDefaultManagerGroups()).isEmpty();
    }

    @Test
    @DisplayName("OrgGroupPermissionResolver는 폴백 그룹을 반환한다")
    void permissionResolverFallback() {
        OrgGroupRepository repo = org.mockito.Mockito.mock(OrgGroupRepository.class);
        OrgGroupSettingsProperties props = new OrgGroupSettingsProperties();
        props.setDefaultGroups(java.util.List.of("DEFAULT-G"));
        org.mockito.BDDMockito.given(repo.findByMemberOrgIdsOrderBySortAsc(java.util.List.of("ORG1")))
                .willReturn(java.util.List.of());
        OrgGroupPermissionResolver resolver = new OrgGroupPermissionResolver(repo, props);
        assertThat(resolver.resolvePermGroups(java.util.List.of("ORG1"), true)).contains("DEFAULT-G");
    }
}
