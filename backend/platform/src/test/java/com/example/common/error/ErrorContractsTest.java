package com.example.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ErrorContractsTest {

    @Test
    void commonErrorCodeHoldsValues() {
        assertThat(CommonErrorCode.NOT_FOUND.code()).isEqualTo("NOT_FOUND");
        assertThat(CommonErrorCode.NOT_FOUND.message()).contains("없습니다");
    }

    @Test
    void businessExceptionExposesCode() {
        BusinessException ex = new BusinessException(CommonErrorCode.INVALID_REQUEST, "bad");
        assertThat(ex.errorCode()).isEqualTo(CommonErrorCode.INVALID_REQUEST);
        assertThat(ex.getMessage()).isEqualTo("bad");
    }

    @Test
    void businessExceptionStoresCause() {
        var cause = new IllegalArgumentException("boom");
        BusinessException ex = new BusinessException(CommonErrorCode.INTERNAL_ERROR, "fail", cause);
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
