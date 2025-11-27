package com.example.draft.application.response;

import com.example.draft.application.dto.*;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftAttachmentResponseEqualityTest {

    @Test
    @DisplayName("DraftAttachmentResponse equals/hashCode 분기 커버")
    void equalsAndHashCode() {
        OffsetDateTime now = OffsetDateTime.now();
        DraftAttachmentResponse r1 = new DraftAttachmentResponse(UUID.randomUUID(), "n", "t", 10L, now, "user");
        DraftAttachmentResponse r2 = new DraftAttachmentResponse(r1.fileId(), "n", "t", 10L, now, "user");
        DraftAttachmentResponse rDiff = new DraftAttachmentResponse(UUID.randomUUID(), "n", "t", 10L, now, "user");

        assertThat(r1).isEqualTo(r2).hasSameHashCodeAs(r2);
        assertThat(r1).isNotEqualTo(rDiff);
    }
}
