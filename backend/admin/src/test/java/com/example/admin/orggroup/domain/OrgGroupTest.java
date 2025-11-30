package com.example.admin.orggroup.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.common.orggroup.OrgGroupRoleType;
import com.example.common.orggroup.WorkCategory;

@DisplayName("OrgGroup/Member 엔터티 테스트")
class OrgGroupTest {

    @Nested
    @DisplayName("OrgGroup 기본 기능")
    class OrgGroupBasics {

        @Test
        @DisplayName("Given: 빌더로 OrgGroup 생성 When: getter 호출 Then: 설정된 값 반환")
        void buildAndGetters() {
            OrgGroup group = OrgGroup.builder()
                    .code("GRP1")
                    .name("영업그룹")
                    .description("desc")
                    .sort(10)
                    .build();

            assertThat(group.getCode()).isEqualTo("GRP1");
            assertThat(group.getSort()).isEqualTo(10);
            assertThat(group.getName()).isEqualTo("영업그룹");
            assertThat(group.getDescription()).isEqualTo("desc");
            assertThat(group.getRolePermissions()).isEmpty();
        }

        @Test
        @DisplayName("Given: sort 없이 생성 When: getSort 호출 Then: null 반환")
        void sortNullable() {
            OrgGroup group = OrgGroup.builder().code("GRP2").name("기본").build();
            assertThat(group.getSort()).isNull();
        }
    }

    @Nested
    @DisplayName("WorkCategory 관리")
    class WorkCategoryManagement {

        @Test
        @DisplayName("Given: OrgGroup 생성 When: addWorkCategory Then: 카테고리 추가됨")
        void addWorkCategory() {
            OrgGroup group = OrgGroup.builder().code("GRP1").name("테스트").build();

            group.addWorkCategory(WorkCategory.COMPLIANCE);
            group.addWorkCategory(WorkCategory.SALES);

            assertThat(group.getWorkCategories()).containsExactlyInAnyOrder(
                    WorkCategory.COMPLIANCE, WorkCategory.SALES);
        }

        @Test
        @DisplayName("Given: 카테고리가 있는 OrgGroup When: removeWorkCategory Then: 카테고리 제거됨")
        void removeWorkCategory() {
            OrgGroup group = OrgGroup.builder().code("GRP1").name("테스트").build();
            group.addWorkCategory(WorkCategory.COMPLIANCE);
            group.addWorkCategory(WorkCategory.SALES);

            group.removeWorkCategory(WorkCategory.COMPLIANCE);

            assertThat(group.getWorkCategories()).containsExactly(WorkCategory.SALES);
        }

        @Test
        @DisplayName("Given: OrgGroup When: setWorkCategories Then: 카테고리 일괄 설정됨")
        void setWorkCategories() {
            OrgGroup group = OrgGroup.builder().code("GRP1").name("테스트").build();
            group.addWorkCategory(WorkCategory.OPERATIONS);

            group.setWorkCategories(Set.of(WorkCategory.TRADING, WorkCategory.RISK_MANAGEMENT));

            assertThat(group.getWorkCategories()).containsExactlyInAnyOrder(
                    WorkCategory.TRADING, WorkCategory.RISK_MANAGEMENT);
        }

        @Test
        @DisplayName("Given: null 카테고리 When: setWorkCategories(null) Then: 카테고리 비워짐")
        void setWorkCategoriesNull() {
            OrgGroup group = OrgGroup.builder().code("GRP1").name("테스트").build();
            group.addWorkCategory(WorkCategory.COMPLIANCE);

            group.setWorkCategories(null);

            assertThat(group.getWorkCategories()).isEmpty();
        }

        @Test
        @DisplayName("Given: 카테고리가 있는 OrgGroup When: hasWorkCategory Then: 포함 여부 반환")
        void hasWorkCategory() {
            OrgGroup group = OrgGroup.builder().code("GRP1").name("테스트").build();
            group.addWorkCategory(WorkCategory.COMPLIANCE);

            assertThat(group.hasWorkCategory(WorkCategory.COMPLIANCE)).isTrue();
            assertThat(group.hasWorkCategory(WorkCategory.SALES)).isFalse();
        }

        @Test
        @DisplayName("Given: null 카테고리 When: addWorkCategory(null) Then: 무시됨")
        void addNullCategory() {
            OrgGroup group = OrgGroup.builder().code("GRP1").name("테스트").build();

            group.addWorkCategory(null);

            assertThat(group.getWorkCategories()).isEmpty();
        }
    }

    @Nested
    @DisplayName("ApprovalMapping 관리")
    class ApprovalMappingManagement {

        @Test
        @DisplayName("Given: OrgGroup 생성 When: getApprovalMappings Then: 빈 리스트 반환")
        void initialEmptyMappings() {
            OrgGroup group = OrgGroup.builder().code("GRP1").name("테스트").build();

            assertThat(group.getApprovalMappings()).isEmpty();
        }

        @Test
        @DisplayName("Given: null 매핑 When: addApprovalMapping(null) Then: 무시됨")
        void addNullMapping() {
            OrgGroup group = OrgGroup.builder().code("GRP1").name("테스트").build();

            group.addApprovalMapping(null);

            assertThat(group.getApprovalMappings()).isEmpty();
        }
    }

    @Nested
    @DisplayName("RolePermission 관리")
    class RolePermissionManagement {

        @Test
        @DisplayName("Given: OrgGroup 생성 When: getRolePermissions Then: 빈 리스트 반환")
        void initialEmptyRolePermissions() {
            OrgGroup group = OrgGroup.builder().code("GRP1").name("테스트").build();

            assertThat(group.getRolePermissions()).isEmpty();
        }

        @Test
        @DisplayName("Given: null 역할권한 When: addRolePermission(null) Then: 무시됨")
        void addNullRolePermission() {
            OrgGroup group = OrgGroup.builder().code("GRP1").name("테스트").build();

            group.addRolePermission(null);

            assertThat(group.getRolePermissions()).isEmpty();
        }

        @Test
        @DisplayName("Given: 역할권한이 없는 OrgGroup When: getPermGroupCodeByRole Then: null 반환")
        void getPermGroupCodeByRoleWhenEmpty() {
            OrgGroup group = OrgGroup.builder().code("GRP1").name("테스트").build();

            assertThat(group.getPermGroupCodeByRole(OrgGroupRoleType.LEADER)).isNull();
            assertThat(group.getPermGroupCodeByRole(OrgGroupRoleType.MANAGER)).isNull();
            assertThat(group.getPermGroupCodeByRole(OrgGroupRoleType.MEMBER)).isNull();
        }
    }

    @Nested
    @DisplayName("OrgGroupMember 기본 기능")
    class OrgGroupMemberBasics {

        @Test
        @DisplayName("Given: 빌더로 OrgGroupMember 생성 When: getter 호출 Then: 설정된 값 반환")
        void buildAndGetters() {
            OrgGroup orgGroup = OrgGroup.builder()
                    .code("GRP1")
                    .name("테스트그룹")
                    .build();

            OrgGroupMember member = OrgGroupMember.builder()
                    .orgGroup(orgGroup)
                    .orgId("ORG1")
                    .displayOrder(5)
                    .build();

            assertThat(member.getGroupCode()).isEqualTo("GRP1");
            assertThat(member.getDisplayOrder()).isEqualTo(5);
            assertThat(member.getOrgId()).isEqualTo("ORG1");
            assertThat(member.getOrgGroup()).isEqualTo(orgGroup);
        }

        @Test
        @DisplayName("Given: displayOrder 없이 생성 When: getDisplayOrder 호출 Then: null 반환")
        void displayOrderNullable() {
            OrgGroup orgGroup = OrgGroup.builder()
                    .code("GRP1")
                    .name("테스트그룹")
                    .build();

            OrgGroupMember member = OrgGroupMember.builder()
                    .orgGroup(orgGroup)
                    .orgId("ORG2")
                    .build();

            assertThat(member.getDisplayOrder()).isNull();
        }

        @Test
        @DisplayName("Given: orgGroup이 null인 경우 When: getGroupCode 호출 Then: null 반환")
        void getGroupCodeWhenOrgGroupNull() {
            OrgGroupMember member = OrgGroupMember.builder()
                    .orgId("ORG1")
                    .build();

            assertThat(member.getGroupCode()).isNull();
        }

        @Test
        @DisplayName("Given: NoArgsConstructor When: 생성 Then: 정상 생성")
        void noArgsConstructor() {
            OrgGroupMember emptyMember = new OrgGroupMember();
            assertThat(emptyMember).isNotNull();
        }
    }
}
