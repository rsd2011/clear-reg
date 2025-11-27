package com.example.admin.masking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrgGroupCoverageTest {

  @Test
  @DisplayName("OrgGroup/Category/Member builder 기본값과 필드 세터 검증")
  void orgGroupDefaults() {
    // OrgGroup: sort 및 PermGroupCode 필드
    OrgGroup group =
        OrgGroup.builder()
            .code("SALES")
            .name("영업")
            .description("desc")
            .sort(10)
            .leaderPermGroupCode("LEADER")
            .managerPermGroupCode("RESP")
            .memberPermGroupCode("MEMBER")
            .build();
    assertThat(group.getSort()).isEqualTo(10);
    assertThat(group.getLeaderPermGroupCode()).isEqualTo("LEADER");
    assertThat(group.getManagerPermGroupCode()).isEqualTo("RESP");
    assertThat(group.getMemberPermGroupCode()).isEqualTo("MEMBER");

    // OrgGroupCategory
    OrgGroupCategory category =
        OrgGroupCategory.builder().code("SALES").label("영업본부").description("desc").build();
    assertThat(category.getLabel()).isEqualTo("영업본부");

    // OrgGroupCategoryMap
    OrgGroupCategoryMap map =
        OrgGroupCategoryMap.builder().categoryCode("SALES").groupCode("SALES-KR").build();
    assertThat(map.getGroupCode()).isEqualTo("SALES-KR");

    // OrgGroupMember: sort만 있고 PermGroupCode 필드 없음
    OrgGroupMember member =
        OrgGroupMember.builder()
            .groupCode("SALES")
            .orgId("001")
            .orgName("강남지점")
            .sort(5)
            .build();
    assertThat(member.getSort()).isEqualTo(5);
    assertThat(member.getGroupCode()).isEqualTo("SALES");
    assertThat(member.getOrgId()).isEqualTo("001");
    assertThat(member.getOrgName()).isEqualTo("강남지점");
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
