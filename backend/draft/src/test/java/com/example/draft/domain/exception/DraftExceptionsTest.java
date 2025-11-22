package com.example.draft.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftExceptionsTest {

    @Test
    @DisplayName("DraftNotFoundException은 전달된 메시지를 보존한다")
    void draftNotFoundMessage() {
        DraftNotFoundException ex = new DraftNotFoundException("not found");
        assertThat(ex).hasMessage("not found");
    }

    @Test
    @DisplayName("DraftWorkflowException은 메시지를 보존한다")
    void draftWorkflowMessage() {
        DraftWorkflowException ex = new DraftWorkflowException("wf error");
        assertThat(ex).hasMessage("wf error");
    }

    @Test
    @DisplayName("DraftTemplateNotFoundException은 메시지를 보존한다")
    void draftTemplateNotFoundMessage() {
        DraftTemplateNotFoundException ex = new DraftTemplateNotFoundException("template missing");
        assertThat(ex).hasMessage("template missing");
    }
}
