package com.example.dw.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HrOrganizationStagingEntityTest {

    @Test
    @DisplayName("조직 스테이징 엔티티는 필드 값을 보존한다")
    void storesFields() {
        HrOrganizationStagingEntity entity = new HrOrganizationStagingEntity();
        entity.setOrganizationCode("ORG");
        entity.setParentOrganizationCode("PARENT");
        assertThat(entity.getOrganizationCode()).isEqualTo("ORG");
        assertThat(entity.getParentOrganizationCode()).isEqualTo("PARENT");
    }
}
