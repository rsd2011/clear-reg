package com.example.admin.maskingpolicy.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MaskingPolicyRootNotFoundException")
class MaskingPolicyRootNotFoundExceptionTest {

    @Nested
    @DisplayName("생성자")
    class Constructor {

        @Test
        @DisplayName("Given: 메시지 / When: 메시지로 생성 / Then: 메시지가 설정됨")
        void createsWithMessage() {
            String message = "마스킹 정책을 찾을 수 없습니다.";

            MaskingPolicyRootNotFoundException exception =
                    new MaskingPolicyRootNotFoundException(message);

            assertThat(exception.getMessage()).isEqualTo(message);
        }

        @Test
        @DisplayName("Given: 메시지와 원인 / When: 메시지와 원인으로 생성 / Then: 메시지와 원인이 설정됨")
        void createsWithMessageAndCause() {
            String message = "마스킹 정책을 찾을 수 없습니다.";
            Throwable cause = new RuntimeException("원인");

            MaskingPolicyRootNotFoundException exception =
                    new MaskingPolicyRootNotFoundException(message, cause);

            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCause()).isEqualTo(cause);
        }
    }
}
