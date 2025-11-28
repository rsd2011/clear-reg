package com.example.admin.datapolicy.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import com.example.admin.datapolicy.service.DataPolicyProviderAdapter;
import com.example.admin.datapolicy.service.DataPolicyService;
import com.example.admin.orggroup.domain.OrgGroup;
import com.example.admin.orggroup.domain.OrgGroupCategoryMap;
import com.example.admin.orggroup.domain.OrgGroupMember;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.common.policy.DataPolicyMatch;
import com.example.common.policy.DataPolicyQuery;

class DataPolicyDomainTest {

    @Test
    void dataPolicyBuilderPopulatesFields() {
        Instant now = Instant.now();
        DataPolicy policy = DataPolicy.builder()
                .featureCode("DRAFT")
                .actionCode("READ")
                .permGroupCode("PG")
                .orgPolicyId(1L)
                .orgGroupCode("ORG-G")
                .businessType("BT")
                .rowScope("ORG")
                .rowScopeExpr("org_code = :org")
                .defaultMaskRule("PARTIAL")
                .maskParams("{\"p\":1}")
                .priority(10)
                .active(true)
                .effectiveFrom(now)
                .effectiveTo(now.plusSeconds(10))
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertThat(policy.getFeatureCode()).isEqualTo("DRAFT");
        assertThat(policy.getRowScope()).isEqualTo("ORG");
        assertThat(policy.getPriority()).isEqualTo(10);
    }

    @Test
    void orgGroupAndMemberDefaults() {
        // OrgGroup: sort는 nullable, PermGroupCode 필드는 OrgGroup에 위치
        OrgGroup group = OrgGroup.builder()
                .code("SALES")
                .name("영업")
                .description("desc")
                .leaderPermGroupCode("LEADER")
                .memberPermGroupCode("MEMBER")
                .build();
        assertThat(group.getSort()).isNull();
        assertThat(group.getLeaderPermGroupCode()).isEqualTo("LEADER");
        assertThat(group.getMemberPermGroupCode()).isEqualTo("MEMBER");

        // OrgGroupMember: PermGroupCode 필드 없음, sort만 보유
        OrgGroupMember member = OrgGroupMember.builder()
                .groupCode("SALES")
                .orgId("BR001")
                .orgName("브랜치")
                .sort(5)
                .build();
        assertThat(member.getGroupCode()).isEqualTo("SALES");
        assertThat(member.getSort()).isEqualTo(5);
    }

    @Test
    void orgGroupCategoryMapping() {
        OrgGroupCategoryMap map = OrgGroupCategoryMap.builder()
                .categoryCode("SALES")
                .groupCode("SALES-KR")
                .build();
        assertThat(map.getCategoryCode()).isEqualTo("SALES");
    }

    @Test
    void providerAdapterDelegatesToService() {
        DataPolicyService service = Mockito.mock(DataPolicyService.class);
        DataPolicyProviderAdapter adapter = new DataPolicyProviderAdapter(service);
        DataPolicyQuery query = new DataPolicyQuery("F","A","PG",1L, java.util.List.of("ORG"),"BT", null, Instant.now());
        DataPolicyMatch match = DataPolicyMatch.builder().rowScope("ALL").build();
        Mockito.when(service.evaluate(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyList(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(java.util.Optional.of(match));

        var result = adapter.evaluate(query);
        assertThat(result).contains(match);
    }
}
