package com.example.draft.application.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftCreateRequestTest {

    @Test
    @DisplayName("attachments/templateVariables가 null이면 빈 컬렉션으로 초기화된다")
    void initializesDefaults() {
        DraftCreateRequest req = new DraftCreateRequest("t", "c", "NOTICE",
                null, null, "{}", null, null, null);

        assertThat(req.attachments()).isEmpty();
        assertThat(req.templateVariables()).isEmpty();
    }
}
