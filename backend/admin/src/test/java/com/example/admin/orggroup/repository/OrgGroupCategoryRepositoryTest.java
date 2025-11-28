package com.example.admin.orggroup.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import com.example.admin.orggroup.domain.OrgGroup;
import com.example.admin.orggroup.domain.OrgGroupCategory;
import com.example.admin.orggroup.domain.OrgGroupCategoryMap;
import com.example.admin.orggroup.domain.OrgGroupMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
@EntityScan(basePackageClasses = { OrgGroup.class, OrgGroupMember.class, OrgGroupCategory.class, OrgGroupCategoryMap.class})
@EnableJpaRepositories(basePackageClasses = { OrgGroupRepository.class, OrgGroupCategoryRepository.class})
class OrgGroupCategoryRepositoryTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestConfig {
    }

    @Autowired
    TestEntityManager em;
    @Autowired
    OrgGroupCategoryRepository categoryRepo;

    @Test
    @DisplayName("그룹 코드로 카테고리 코드를 조회할 수 있다")
    void findCategoryCodes() {
        em.persist(OrgGroup.builder().code("GRP_A").name("Group A").sort(50).build());
        em.persist(OrgGroupCategory.builder().code("COMPLIANCE").label("준법").build());
        em.persist(OrgGroupCategory.builder().code("SALES").label("영업").build());
        categoryRepo.save(OrgGroupCategoryMap.builder().groupCode("GRP_A").categoryCode("COMPLIANCE").build());
        categoryRepo.save(OrgGroupCategoryMap.builder().groupCode("GRP_A").categoryCode("SALES").build());

        Set<String> codes = categoryRepo.findCategoryCodesByGroupCodes(List.of("GRP_A"));
        assertThat(codes).containsExactlyInAnyOrder("COMPLIANCE", "SALES");
    }
}
