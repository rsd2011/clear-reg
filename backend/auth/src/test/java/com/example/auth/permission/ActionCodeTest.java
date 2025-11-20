package com.example.auth.permission;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.testing.bdd.Scenario;

@DisplayName("ActionCode 열거형 테스트")
class ActionCodeTest {

    @Test
    @DisplayName("Given UNMASK 권한 When satisfies 검사 Then READ 액션을 포함한다")
    void givenActionHierarchy_whenSatisfies_thenMatchesUnmask() {
        Scenario.given("UNMASK 권한", () -> ActionCode.UNMASK)
                .when("검증", action -> action.satisfies(ActionCode.READ))
                .then("모든 액션을 포함", result -> assertThat(result).isTrue());
    }

    @Test
    @DisplayName("Given READ 액션 When 데이터 조회 여부 검사 Then true를 반환한다")
    void givenReadAction_whenCheckingDataFetch_thenTrue() {
        Scenario.given("READ 액션", () -> ActionCode.READ)
                .when("데이터 조회 여부", ActionCode::isDataFetch)
                .then("데이터 접근 액션", result -> assertThat(result).isTrue());
    }

    @Test
    @DisplayName("Given CREATE 액션 When satisfies READ 호출 Then false를 반환한다")
    void givenCreateAction_whenCheckingSatisfies_thenExactMatchRequired() {
        Scenario.given("CREATE 액션", () -> ActionCode.CREATE)
                .when("READ 허용 여부", action -> action.satisfies(ActionCode.READ))
                .then("다른 액션은 거부", result -> assertThat(result).isFalse());
    }
}
