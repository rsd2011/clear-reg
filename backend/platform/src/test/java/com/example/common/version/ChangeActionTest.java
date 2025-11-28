package com.example.common.version;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChangeAction 열거형")
class ChangeActionTest {

    @Test
    @DisplayName("Given: ChangeAction 열거형 / When: values() 호출 / Then: 모든 액션 타입 포함")
    void allValuesExist() {
        ChangeAction[] values = ChangeAction.values();

        assertThat(values).containsExactly(
                ChangeAction.CREATE,
                ChangeAction.UPDATE,
                ChangeAction.DELETE,
                ChangeAction.COPY,
                ChangeAction.RESTORE,
                ChangeAction.ROLLBACK,
                ChangeAction.DRAFT,
                ChangeAction.PUBLISH
        );
    }

    @Test
    @DisplayName("Given: 각 문자열 / When: valueOf() 호출 / Then: 대응하는 열거형 값 반환")
    void valueOfWorksForAllValues() {
        assertThat(ChangeAction.valueOf("CREATE")).isEqualTo(ChangeAction.CREATE);
        assertThat(ChangeAction.valueOf("UPDATE")).isEqualTo(ChangeAction.UPDATE);
        assertThat(ChangeAction.valueOf("DELETE")).isEqualTo(ChangeAction.DELETE);
        assertThat(ChangeAction.valueOf("COPY")).isEqualTo(ChangeAction.COPY);
        assertThat(ChangeAction.valueOf("RESTORE")).isEqualTo(ChangeAction.RESTORE);
        assertThat(ChangeAction.valueOf("ROLLBACK")).isEqualTo(ChangeAction.ROLLBACK);
        assertThat(ChangeAction.valueOf("DRAFT")).isEqualTo(ChangeAction.DRAFT);
        assertThat(ChangeAction.valueOf("PUBLISH")).isEqualTo(ChangeAction.PUBLISH);
    }

    @Test
    @DisplayName("Given: 각 열거형 값 / When: name() 호출 / Then: 올바른 문자열 반환")
    void nameReturnsCorrectString() {
        assertThat(ChangeAction.CREATE.name()).isEqualTo("CREATE");
        assertThat(ChangeAction.UPDATE.name()).isEqualTo("UPDATE");
        assertThat(ChangeAction.DELETE.name()).isEqualTo("DELETE");
        assertThat(ChangeAction.COPY.name()).isEqualTo("COPY");
        assertThat(ChangeAction.RESTORE.name()).isEqualTo("RESTORE");
        assertThat(ChangeAction.ROLLBACK.name()).isEqualTo("ROLLBACK");
        assertThat(ChangeAction.DRAFT.name()).isEqualTo("DRAFT");
        assertThat(ChangeAction.PUBLISH.name()).isEqualTo("PUBLISH");
    }

    @Test
    @DisplayName("Given: 각 열거형 값 / When: ordinal() 호출 / Then: 올바른 순서 반환")
    void ordinalReturnsCorrectOrder() {
        assertThat(ChangeAction.CREATE.ordinal()).isZero();
        assertThat(ChangeAction.UPDATE.ordinal()).isEqualTo(1);
        assertThat(ChangeAction.DELETE.ordinal()).isEqualTo(2);
        assertThat(ChangeAction.COPY.ordinal()).isEqualTo(3);
        assertThat(ChangeAction.RESTORE.ordinal()).isEqualTo(4);
        assertThat(ChangeAction.ROLLBACK.ordinal()).isEqualTo(5);
        assertThat(ChangeAction.DRAFT.ordinal()).isEqualTo(6);
        assertThat(ChangeAction.PUBLISH.ordinal()).isEqualTo(7);
    }
}
