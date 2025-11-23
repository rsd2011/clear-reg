package com.example.policy.datapolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("OrgGroupPermissionResolver 기본/폴백 분기")
class OrgGroupPermissionResolverTest {

    OrgGroupRepository repository = Mockito.mock(OrgGroupRepository.class);
    OrgGroupSettingsProperties props = new OrgGroupSettingsProperties();

    @Test
    void resolvesLeaderAndMemberAndFallback() {
        OrgGroupMember leaderMember = OrgGroupMember.builder().groupCode("G1").orgId("O1").leaderPermGroupCode("L1").memberPermGroupCode("M1").priority(1).build();
        OrgGroupMember onlyMember = OrgGroupMember.builder().groupCode("G2").orgId("O2").memberPermGroupCode("M2").priority(2).build();
        given(repository.findByOrgIdInOrderByPriorityAsc(any())).willReturn(List.of(leaderMember, onlyMember));

        OrgGroupPermissionResolver resolver = new OrgGroupPermissionResolver(repository, props);
        Set<String> leader = resolver.resolvePermGroups(List.of("O1", "O2"), true);
        Set<String> member = resolver.resolvePermGroups(List.of("O1", "O2"), false);

        assertThat(leader).containsExactly("L1");
        assertThat(member).containsExactly("M1", "M2");
    }

    @Test
    void fallbackToDefaultGroupsWhenEmpty() {
        given(repository.findByOrgIdInOrderByPriorityAsc(any())).willReturn(List.of());
        props.setDefaultGroups(List.of("DEF1", "DEF2"));
        OrgGroupPermissionResolver resolver = new OrgGroupPermissionResolver(repository, props);
        Set<String> groups = resolver.resolvePermGroups(List.of("O1"), false);
        assertThat(groups).containsExactly("DEF1", "DEF2");
    }

    @Test
    void fallbackToLowestPriorityGroup() {
        given(repository.findByOrgIdInOrderByPriorityAsc(any())).willReturn(List.of());
        props.setDefaultGroups(List.of());
        props.setFallbackToLowestPriorityGroup(true);
        given(repository.findTopByOrderByPriorityDesc()).willReturn(OrgGroup.builder().code("LOW").priority(200).name("low").build());

        OrgGroupPermissionResolver resolver = new OrgGroupPermissionResolver(repository, props);
        Set<String> groups = resolver.resolvePermGroups(List.of("O1"), false);
        assertThat(groups).containsExactly("LOW");
    }

    @Test
    void emptyOrgIdsReturnsEmpty() {
        OrgGroupPermissionResolver resolver = new OrgGroupPermissionResolver(repository, props);
        assertThat(resolver.resolvePermGroups(List.of(), false)).isEmpty();
    }
}
