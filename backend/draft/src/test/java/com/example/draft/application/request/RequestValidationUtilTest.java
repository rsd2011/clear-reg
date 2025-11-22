package com.example.draft.application.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RequestValidationUtilTest {

    @Test
    @DisplayName("organizationCode가 null/blank면 false, 값이 있으면 true")
    void hasOrganizationBranches() {
        assertThat(RequestValidationUtil.hasOrganization(null)).isFalse();
        assertThat(RequestValidationUtil.hasOrganization(" ")).isFalse();
        assertThat(RequestValidationUtil.hasOrganization("ORG")).isTrue();
    }
}
