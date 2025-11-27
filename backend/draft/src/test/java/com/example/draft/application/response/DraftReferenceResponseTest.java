package com.example.draft.application.response;

import com.example.draft.application.dto.*;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.draft.domain.DraftReference;

class DraftReferenceResponseTest {

    @Test
    @DisplayName("DraftReferenceResponse.from은 참조 정보를 그대로 매핑한다")
    void mapsReferenceFields() {
        DraftReference reference = DraftReference.create("user1", "ORG1", "creator", OffsetDateTime.now());

        DraftReferenceResponse response = DraftReferenceResponse.from(reference);

        assertThat(response.id()).isEqualTo(reference.getId());
        assertThat(response.referencedUserId()).isEqualTo("user1");
        assertThat(response.referencedOrgCode()).isEqualTo("ORG1");
        assertThat(response.addedBy()).isEqualTo("creator");
        assertThat(response.addedAt()).isEqualTo(reference.getAddedAt());
    }
}
