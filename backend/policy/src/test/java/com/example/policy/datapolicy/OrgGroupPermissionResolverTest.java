package com.example.policy.datapolicy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@DataJpaTest
@EntityScan(basePackageClasses = {OrgGroup.class, OrgGroupMember.class})
@Import({OrgGroupPermissionResolver.class, OrgGroupPermissionResolverTest.TestConfig.class, OrgGroupPermissionResolverTest.TestApp.class})
class OrgGroupPermissionResolverTest {

    @Autowired
    OrgGroupRepository repository;

    @Autowired
    OrgGroupPermissionResolver resolver;

    @TestConfiguration
    @EnableJpaRepositories(basePackageClasses = OrgGroupRepository.class)
    static class TestConfig {
        @Bean
        OrgGroupSettingsProperties orgGroupSettingsProperties() {
            OrgGroupSettingsProperties props = new OrgGroupSettingsProperties();
            props.setDefaultGroups(java.util.List.of("DEFAULT_GRP"));
            return props;
        }
    }

    @SpringBootApplication
    static class TestApp {}

    @Test
    @DisplayName("리더/멤버에 따라 다른 권한그룹을 반환한다")
    void resolve() {
        repository.save(OrgGroupMember.builder()
                .groupCode("GRP_A")
                .orgId("ORG1")
                .leaderPermGroupCode("LEADER_ADMIN")
                .memberPermGroupCode("MEMBER_READ")
                .priority(50)
                .build());

        Set<String> leader = resolver.resolvePermGroups(List.of("ORG1"), true);
        Set<String> member = resolver.resolvePermGroups(List.of("ORG1"), false);

        assertThat(leader).containsExactly("LEADER_ADMIN");
        assertThat(member).containsExactly("MEMBER_READ");
    }

    @Test
    @DisplayName("우선순위가 낮은 값이 뒤에 온다")
    void priorityOrdering() {
        repository.save(OrgGroupMember.builder()
                .groupCode("GRP_A")
                .orgId("ORG1")
                .memberPermGroupCode("LOW")
                .priority(200)
                .build());
        repository.save(OrgGroupMember.builder()
                .groupCode("GRP_B")
                .orgId("ORG1")
                .memberPermGroupCode("HIGH")
                .priority(10)
                .build());

        Set<String> member = resolver.resolvePermGroups(List.of("ORG1"), false);
        assertThat(member.stream().toList()).containsExactly("HIGH", "LOW");
    }

    @Test
    @DisplayName("맵핑이 없으면 기본 그룹을 반환한다")
    void defaultGroupWhenEmpty() {
        Set<String> member = resolver.resolvePermGroups(List.of("UNKNOWN"), false);
        assertThat(member).containsExactly("DEFAULT_GRP");
    }

    @Test
    @DisplayName("맵핑 없을 때 기본 그룹 혹은 최하위 priority 그룹으로 대체")
    void defaultOrLowestPriority() {
        Set<String> member = resolver.resolvePermGroups(List.of("ORG2"), false);
        assertThat(member).containsExactly("DEFAULT_GRP");
    }
}
