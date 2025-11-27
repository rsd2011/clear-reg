package com.example.draft.architecture;

import org.junit.jupiter.api.DisplayName;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.common.security.RequiresRowScope;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * @deprecated @RequiresRowScope 마커 어노테이션이 deprecated 되어 이 테스트도 비활성화됨.
 *     RowScope 기반 필터링은 AuthContextHolder.rowScopeSpec() 헬퍼를 통해 명시적으로 적용하므로
 *     Repository에 마커 어노테이션을 강제하지 않습니다.
 *
 *     <p>allowEmptyShould(true)로 설정하여 매칭되는 클래스가 없어도 테스트가 실패하지 않도록 함.
 */
@Deprecated(since = "2024.1", forRemoval = true)
@SuppressWarnings("removal")
@AnalyzeClasses(packages = "com.example.draft")
@DisplayName("RowScope Repository 아키텍처 규칙 (deprecated)")
class RowScopeRepositoryArchTest {

    @Deprecated
    @ArchTest
    static final ArchRule row_scope_repositories_should_implement_spec_executor =
            classes().that().areAnnotatedWith(RequiresRowScope.class)
                    .should().beAssignableTo(JpaSpecificationExecutor.class)
                    .because("RowScope 적용 Repository는 Specification 기반 필터링을 지원해야 합니다.")
                    .allowEmptyShould(true);
}
