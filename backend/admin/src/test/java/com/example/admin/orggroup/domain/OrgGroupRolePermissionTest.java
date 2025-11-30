package com.example.admin.orggroup.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.common.orggroup.OrgGroupRoleType;

@DisplayName("OrgGroupRolePermission 엔티티")
class OrgGroupRolePermissionTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2025, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC);

    private OrgGroup createOrgGroup() {
        return OrgGroup.builder()
                .code("SALES")
                .name("영업팀")
                .build();
    }

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("Given: 조직그룹, 역할유형, 권한그룹코드가 주어지면 When: 생성하면 Then: 정상 생성된다")
        void createsSuccessfully() {
            OrgGroup orgGroup = createOrgGroup();

            OrgGroupRolePermission rolePermission = OrgGroupRolePermission.create(
                    orgGroup, OrgGroupRoleType.LEADER, "PERM_LEADER", NOW);

            assertThat(rolePermission.getOrgGroup()).isEqualTo(orgGroup);
            assertThat(rolePermission.getRoleType()).isEqualTo(OrgGroupRoleType.LEADER);
            assertThat(rolePermission.getPermGroupCode()).isEqualTo("PERM_LEADER");
            assertThat(rolePermission.getCreatedAt()).isEqualTo(NOW);
            assertThat(rolePermission.getUpdatedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("Given: 조직그룹이 null이면 When: 생성하면 Then: 예외가 발생한다")
        void throwsWhenOrgGroupNull() {
            assertThatThrownBy(() ->
                    OrgGroupRolePermission.create(null, OrgGroupRoleType.LEADER, "PERM", NOW))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("조직그룹");
        }

        @Test
        @DisplayName("Given: 역할유형이 null이면 When: 생성하면 Then: 예외가 발생한다")
        void throwsWhenRoleTypeNull() {
            OrgGroup orgGroup = createOrgGroup();

            assertThatThrownBy(() ->
                    OrgGroupRolePermission.create(orgGroup, null, "PERM", NOW))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("역할 유형");
        }

        @Test
        @DisplayName("Given: 권한그룹코드가 null이면 When: 생성하면 Then: 예외가 발생한다")
        void throwsWhenPermGroupCodeNull() {
            OrgGroup orgGroup = createOrgGroup();

            assertThatThrownBy(() ->
                    OrgGroupRolePermission.create(orgGroup, OrgGroupRoleType.LEADER, null, NOW))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("권한그룹 코드");
        }

        @Test
        @DisplayName("Given: 권한그룹코드가 빈 문자열이면 When: 생성하면 Then: 예외가 발생한다")
        void throwsWhenPermGroupCodeBlank() {
            OrgGroup orgGroup = createOrgGroup();

            assertThatThrownBy(() ->
                    OrgGroupRolePermission.create(orgGroup, OrgGroupRoleType.LEADER, "  ", NOW))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("권한그룹 코드");
        }
    }

    @Nested
    @DisplayName("권한그룹 변경")
    class ChangePermGroupCode {

        @Test
        @DisplayName("Given: 역할권한이 있으면 When: 권한그룹을 변경하면 Then: 권한그룹과 수정일시가 변경된다")
        void changesPermGroupCode() {
            OrgGroup orgGroup = createOrgGroup();
            OrgGroupRolePermission rolePermission = OrgGroupRolePermission.create(
                    orgGroup, OrgGroupRoleType.LEADER, "PERM_OLD", NOW);

            OffsetDateTime later = NOW.plusHours(1);
            rolePermission.changePermGroupCode("PERM_NEW", later);

            assertThat(rolePermission.getPermGroupCode()).isEqualTo("PERM_NEW");
            assertThat(rolePermission.getUpdatedAt()).isEqualTo(later);
            assertThat(rolePermission.getCreatedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("Given: 새 권한그룹코드가 null이면 When: 변경하면 Then: 예외가 발생한다")
        void throwsWhenNewPermGroupCodeNull() {
            OrgGroup orgGroup = createOrgGroup();
            OrgGroupRolePermission rolePermission = OrgGroupRolePermission.create(
                    orgGroup, OrgGroupRoleType.LEADER, "PERM", NOW);

            assertThatThrownBy(() -> rolePermission.changePermGroupCode(null, NOW))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("권한그룹 코드");
        }
    }

    @Nested
    @DisplayName("OrgGroupRoleType enum")
    class RoleTypeEnum {

        @Test
        @DisplayName("Given: 문자열이 주어지면 When: fromString Then: 해당 enum 반환")
        void fromStringValid() {
            assertThat(OrgGroupRoleType.fromString("LEADER")).isEqualTo(OrgGroupRoleType.LEADER);
            assertThat(OrgGroupRoleType.fromString("leader")).isEqualTo(OrgGroupRoleType.LEADER);
            assertThat(OrgGroupRoleType.fromString("Manager")).isEqualTo(OrgGroupRoleType.MANAGER);
            assertThat(OrgGroupRoleType.fromString("MEMBER")).isEqualTo(OrgGroupRoleType.MEMBER);
        }

        @Test
        @DisplayName("Given: 잘못된 문자열이면 When: fromString Then: null 반환")
        void fromStringInvalid() {
            assertThat(OrgGroupRoleType.fromString("INVALID")).isNull();
        }

        @Test
        @DisplayName("Given: null이면 When: fromString Then: null 반환")
        void fromStringNull() {
            assertThat(OrgGroupRoleType.fromString(null)).isNull();
        }

        @Test
        @DisplayName("Given: 빈 문자열이면 When: fromString Then: null 반환")
        void fromStringBlank() {
            assertThat(OrgGroupRoleType.fromString("")).isNull();
            assertThat(OrgGroupRoleType.fromString("  ")).isNull();
        }
    }
}
