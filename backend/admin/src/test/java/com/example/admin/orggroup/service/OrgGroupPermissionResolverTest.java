package com.example.admin.orggroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import com.example.admin.orggroup.domain.OrgGroup;
import com.example.admin.orggroup.domain.OrgGroupRolePermission;
import com.example.admin.orggroup.properties.OrgGroupSettingsProperties;
import com.example.common.orggroup.OrgGroupRoleType;
import com.example.admin.orggroup.repository.OrgGroupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("OrgGroupPermissionResolver 기본/폴백 분기")
class OrgGroupPermissionResolverTest {

    private static final OffsetDateTime NOW = OffsetDateTime.now();

    OrgGroupRepository repository = Mockito.mock(OrgGroupRepository.class);
    OrgGroupSettingsProperties props = new OrgGroupSettingsProperties();

    private OrgGroup createGroupWithRolePermissions(String code, String name, int sort,
                                                     String leaderCode, String managerCode, String memberCode) {
        OrgGroup group = OrgGroup.builder()
                .code(code)
                .name(name)
                .sort(sort)
                .build();

        if (leaderCode != null) {
            group.addRolePermission(OrgGroupRolePermission.create(group, OrgGroupRoleType.LEADER, leaderCode, NOW));
        }
        if (managerCode != null) {
            group.addRolePermission(OrgGroupRolePermission.create(group, OrgGroupRoleType.MANAGER, managerCode, NOW));
        }
        if (memberCode != null) {
            group.addRolePermission(OrgGroupRolePermission.create(group, OrgGroupRoleType.MEMBER, memberCode, NOW));
        }
        return group;
    }

    @Test
    void resolvesLeaderAndMemberCodes() {
        OrgGroup leaderGroup = createGroupWithRolePermissions("G1", "영업그룹", 1, "L1", null, "M1");
        OrgGroup memberOnlyGroup = createGroupWithRolePermissions("G2", "지원그룹", 2, null, null, "M2");

        given(repository.findByMemberOrgIdsOrderBySortAsc(any()))
                .willReturn(List.of(leaderGroup, memberOnlyGroup));

        OrgGroupPermissionResolver resolver = new OrgGroupPermissionResolver(repository, props);
        Set<String> leader = resolver.resolvePermGroups(List.of("O1", "O2"), true);
        Set<String> member = resolver.resolvePermGroups(List.of("O1", "O2"), false);

        assertThat(leader).containsExactly("L1");
        assertThat(member).containsExactly("M1", "M2");
    }

    @Test
    void fallbackToDefaultGroupsWhenEmpty() {
        given(repository.findByMemberOrgIdsOrderBySortAsc(any())).willReturn(List.of());
        props.setDefaultGroups(List.of("DEF1", "DEF2"));
        OrgGroupPermissionResolver resolver = new OrgGroupPermissionResolver(repository, props);
        Set<String> groups = resolver.resolvePermGroups(List.of("O1"), false);
        assertThat(groups).containsExactly("DEF1", "DEF2");
    }

    @Test
    void managerUsesGroupSpecificCode() {
        OrgGroup group = createGroupWithRolePermissions("G1", "영업그룹", 1, null, "RESP-MANAGER", null);

        given(repository.findByMemberOrgIdsOrderBySortAsc(any())).willReturn(List.of(group));
        OrgGroupPermissionResolver resolver = new OrgGroupPermissionResolver(repository, props);
        assertThat(resolver.resolveManagerPermGroups(List.of("O1"))).containsExactly("RESP-MANAGER");
    }

    @Test
    void managerUsesConfiguredDefaults() {
        given(repository.findByMemberOrgIdsOrderBySortAsc(any())).willReturn(List.of());
        props.setDefaultManagerGroups(List.of("RESP1", "RESP2"));
        OrgGroupPermissionResolver resolver = new OrgGroupPermissionResolver(repository, props);
        Set<String> groups = resolver.resolveManagerPermGroups(List.of("O1"));
        assertThat(groups).containsExactly("RESP1", "RESP2");
    }

    @Test
    void managerFallsBackToDefaultGroupsWhenManagerGroupsEmpty() {
        given(repository.findByMemberOrgIdsOrderBySortAsc(any())).willReturn(List.of());
        props.setDefaultManagerGroups(List.of());
        props.setDefaultGroups(List.of("DEF-R1"));
        OrgGroupPermissionResolver resolver = new OrgGroupPermissionResolver(repository, props);
        assertThat(resolver.resolveManagerPermGroups(List.of("O1"))).containsExactly("DEF-R1");
    }

    @Test
    void noFallbackWhenDefaultGroupsEmpty() {
        props.setDefaultGroups(List.of());
        given(repository.findByMemberOrgIdsOrderBySortAsc(any())).willReturn(List.of());
        OrgGroupPermissionResolver resolver = new OrgGroupPermissionResolver(repository, props);
        assertThat(resolver.resolvePermGroups(List.of("O1"), false)).isEmpty();
    }

    @Test
    void ignoresEmptyCodesFromGroups() {
        OrgGroup blankGroup = OrgGroup.builder().code("G").name("빈그룹").sort(1).build();
        given(repository.findByMemberOrgIdsOrderBySortAsc(any())).willReturn(List.of(blankGroup));
        OrgGroupPermissionResolver resolver = new OrgGroupPermissionResolver(repository, props);
        assertThat(resolver.resolvePermGroups(List.of("O1"), true)).isEmpty();
    }

    @Test
    void emptyOrgIdsReturnsEmpty() {
        OrgGroupPermissionResolver resolver = new OrgGroupPermissionResolver(repository, props);
        assertThat(resolver.resolvePermGroups(List.of(), false)).isEmpty();
        assertThat(resolver.resolveManagerPermGroups(List.of())).isEmpty();
        assertThat(resolver.resolveManagerPermGroups(null)).isEmpty();
    }

    @Test
    void nullOrgIdsReturnsEmpty() {
        OrgGroupPermissionResolver resolver = new OrgGroupPermissionResolver(repository, props);
        assertThat(resolver.resolvePermGroups(null, false)).isEmpty();
    }
}
