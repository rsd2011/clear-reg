package com.example.draft.architecture;

import org.junit.jupiter.api.DisplayName;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.common.security.RequiresRowScope;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "com.example.draft")
@DisplayName("RowScope Repository 아키텍처 규칙")
class RowScopeRepositoryArchTest {

    @ArchTest
    static final ArchRule row_scope_repositories_should_implement_spec_executor =
            classes().that().areAnnotatedWith(RequiresRowScope.class)
                    .should().beAssignableTo(JpaSpecificationExecutor.class)
                    .because("RowScope 적용 Repository는 Specification 기반 필터링을 지원해야 합니다.");
}
