package com.example.draft.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.domain.Specification;

import com.example.common.security.RowScope;
import com.example.common.security.RowScopeSpecifications;
import com.example.draft.domain.Draft;

@DataJpaTest
@DisplayName("DraftRepository RowScope 적용 테스트")
class DraftRepositoryRowScopeTest {

    @Autowired
    private DraftRepository draftRepository;

    private static final OffsetDateTime NOW = OffsetDateTime.now(ZoneOffset.UTC);

    @Test
    void givenOwnScope_whenFilter_thenOnlyOwnOrganizationReturned() {
        Draft mine = Draft.create("내 기안", "내용", "NOTICE", "ORG-A", "TPL-1", "writer", NOW);
        Draft other = Draft.create("다른 기안", "내용", "NOTICE", "ORG-B", "TPL-1", "writer", NOW);
        draftRepository.saveAll(List.of(mine, other));

        Specification<Draft> spec = RowScopeSpecifications.organizationScoped("organizationCode",
                RowScope.OWN,
                "ORG-A",
                List.of());

        List<Draft> results = draftRepository.findAll(spec);

        assertThat(results).extracting(Draft::getOrganizationCode)
                .containsExactly("ORG-A");
    }

    @Test
    void givenOrgScope_whenFilter_thenHierarchyReturned() {
        Draft root = Draft.create("ROOT", "내용", "NOTICE", "ORG-A", "TPL-1", "writer", NOW);
        Draft child = Draft.create("Child", "내용", "NOTICE", "ORG-A-1", "TPL-1", "writer", NOW);
        Draft other = Draft.create("Other", "내용", "NOTICE", "ORG-B", "TPL-1", "writer", NOW);
        draftRepository.saveAll(List.of(root, child, other));

        Specification<Draft> spec = RowScopeSpecifications.organizationScoped("organizationCode",
                RowScope.ORG,
                "ORG-A",
                List.of("ORG-A", "ORG-A-1"));

        List<Draft> results = draftRepository.findAll(spec);

        assertThat(results).extracting(Draft::getOrganizationCode)
                .containsExactlyInAnyOrder("ORG-A", "ORG-A-1");
    }

    @Test
    void givenAllScope_whenFilter_thenEveryDraftReturned() {
        Draft first = Draft.create("ROOT", "내용", "NOTICE", "ORG-A", "TPL-1", "writer", NOW);
        Draft second = Draft.create("Other", "내용", "NOTICE", "ORG-B", "TPL-1", "writer", NOW);
        draftRepository.saveAll(List.of(first, second));

        Specification<Draft> spec = RowScopeSpecifications.organizationScoped("organizationCode",
                RowScope.ALL,
                null,
                List.of());

        List<Draft> results = draftRepository.findAll(spec);

        assertThat(results).hasSize(2);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan({"com.example.draft", "com.example.approval"})
    @EnableJpaRepositories({"com.example.draft.domain.repository", "com.example.approval.domain.repository"})
    public static class TestConfig {
    }
}
