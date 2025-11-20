package com.example.server.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

@DisplayName("RestAuthenticationEntryPoint 테스트")
class RestAuthenticationEntryPointTest {

    private final RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint();

    @Test
    @DisplayName("Given 인증되지 않은 요청 When commence 호출 Then 401 상태를 반환한다")
    void givenUnauthorizedWhenCommenceThen401() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        entryPoint.commence(new MockHttpServletRequest(), response, new AuthenticationException("denied") {});

        assertThat(response.getStatus()).isEqualTo(401);
    }
}
