package com.example.draft.application.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ResponseMaskingUtilTest {

    @Test
    @DisplayName("actor가 null/blank이면 UNKNOWN, 두 글자 이하면 그대로, 그 외에는 앞 한 글자 + ***")
    void maskActorBranches() {
        assertThat(ResponseMaskingUtil.maskActor(null)).isEqualTo("UNKNOWN");
        assertThat(ResponseMaskingUtil.maskActor(" ")).isEqualTo("UNKNOWN");
        assertThat(ResponseMaskingUtil.maskActor("AB")).isEqualTo("AB");
        assertThat(ResponseMaskingUtil.maskActor("Alice")).isEqualTo("A***");
    }
}
