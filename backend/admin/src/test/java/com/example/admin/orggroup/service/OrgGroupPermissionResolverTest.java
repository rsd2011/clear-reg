package com.example.admin.orggroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Set;

import com.example.admin.orggroup.domain.OrgGroup;
import com.example.admin.orggroup.properties.OrgGroupSettingsProperties;
import com.example.admin.orggroup.repository.OrgGroupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("OrgGroupPermissionResolver 기본/폴백 분기")
class OrgGroupPermissionResolverTest {

  OrgGroupRepository repository = Mockito.mock(OrgGroupRepository.class);
  OrgGroupSettingsProperties props = new OrgGroupSettingsProperties();

  @Test
  void resolvesLeaderAndMemberCodes() {
    OrgGroup leaderGroup =
        OrgGroup.builder()
            .code("G1")
            .name("영업그룹")
            .leaderPermGroupCode("L1")
            .memberPermGroupCode("M1")
            .sort(1)
            .build();
    OrgGroup memberOnlyGroup =
        OrgGroup.builder()
            .code("G2")
            .name("지원그룹")
            .memberPermGroupCode("M2")
            .sort(2)
            .build();
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
    OrgGroup group =
        OrgGroup.builder()
            .code("G1")
            .name("영업그룹")
            .managerPermGroupCode("RESP-MANAGER")
            .sort(1)
            .build();
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
