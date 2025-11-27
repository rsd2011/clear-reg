package com.example.draft.application.response;

import com.example.draft.application.dto.*;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Draft 응답 DTO 값 보존")
class DraftResponseValueTest {

    @Test
    @DisplayName("DraftReferenceResponse는 전달한 필드를 보존한다")
    void referenceResponsePreservesValues() {
        OffsetDateTime now = OffsetDateTime.now();
        UUID id = UUID.randomUUID();
        DraftReferenceResponse response = new DraftReferenceResponse(id, "user1", "ORG", "adder", now);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.referencedUserId()).isEqualTo("user1");
        assertThat(response.referencedOrgCode()).isEqualTo("ORG");
        assertThat(response.addedBy()).isEqualTo("adder");
        assertThat(response.addedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("DraftAttachmentResponse는 전달한 필드를 보존한다")
    void attachmentResponsePreservesValues() {
        OffsetDateTime now = OffsetDateTime.now();
        UUID fileId = UUID.randomUUID();
        DraftAttachmentResponse response = new DraftAttachmentResponse(fileId, "name", "text/plain", 2L, now, "creator");

        assertThat(response.fileId()).isEqualTo(fileId);
        assertThat(response.fileName()).isEqualTo("name");
        assertThat(response.attachedBy()).isEqualTo("creator");
        assertThat(response.fileSize()).isEqualTo(2L);
        assertThat(response.contentType()).isEqualTo("text/plain");
        assertThat(response.attachedAt()).isEqualTo(now);
    }
}
