package com.example.auth.organization;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OrganizationPolicy 도메인 테스트")
class OrganizationPolicyTest {

    @Test
    @DisplayName("Given 정책 객체 When 컬렉션 접근 Then 불변 컬렉션으로 반환된다")
    void givenPolicy_whenAccessingCollections_thenUnmodifiable() throws Exception {
        OrganizationPolicy policy = new OrganizationPolicy("ORG", "DEFAULT");

        Field groupsField = OrganizationPolicy.class.getDeclaredField("additionalPermissionGroups");
        groupsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        LinkedHashSet<String> groups = (LinkedHashSet<String>) groupsField.get(policy);
        groups.add("AUDIT");

        Field flowField = OrganizationPolicy.class.getDeclaredField("approvalFlow");
        flowField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> flow = (List<String>) flowField.get(policy);
        flow.add("APPROVER");

        assertThat(policy.getAdditionalPermissionGroups()).containsExactly("AUDIT");
        assertThat(policy.getApprovalFlow()).containsExactly("APPROVER");
    }
}
