package com.example.server.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.example.auth.security.JwtTokenProvider;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import java.io.IOException;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter 테스트")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }

    @Test
    @DisplayName("Given 유효한 토큰 When 필터링하면 Then 인증 정보가 설정된다")
    void givenValidTokenWhenFilterThenAuthenticate() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer abc.def");
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserDetails userDetails = User.withUsername("tester").password("pw").roles("USER").build();

        given(jwtTokenProvider.isValid("abc.def")).willReturn(true);
        given(jwtTokenProvider.extractUsername("abc.def")).willReturn("tester");
        given(userDetailsService.loadUserByUsername("tester")).willReturn(userDetails);

        filter.doFilterInternal(request, response, filterChain);

        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("tester");
    }

    @Test
    @DisplayName("Given 토큰이 없을 때 When 필터링하면 Then 인증을 생략한다")
    void givenNoTokenWhenFilterThenSkip() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Given 유효하지 않은 토큰 When 필터링하면 Then 인증을 설정하지 않는다")
    void givenInvalidTokenWhenFilterThenNoAuth() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtTokenProvider.isValid("invalid")).willReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Given 이미 인증된 컨텍스트 When 필터링하면 Then 기존 인증을 유지한다")
    void givenAuthenticatedContextWhenFilterThenKeepAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserDetails userDetails = User.withUsername("tester").password("pw").roles("USER").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        given(jwtTokenProvider.isValid("token")).willReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("tester");
    }

    @Test
    @DisplayName("Given Bearer 접두사가 없는 헤더 When 필터링하면 Then 인증을 시도하지 않는다")
    void givenNonBearerHeaderWhenFilterThenSkip() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Token abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Given Bearer 헤더가 비어 있으면 When 필터링 Then 인증을 설정하지 않는다")
    void givenEmptyBearerWhenFilterThenNoAuth() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Given 만료된 토큰 When 필터링하면 Then 인증을 설정하지 않는다")
    void givenExpiredTokenWhenFilterThenNoAuth() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer expired");
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtTokenProvider.isValid("expired")).willReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Given 서명 오류 토큰 When 필터링하면 Then 인증을 설정하지 않는다")
    void givenSignatureErrorTokenWhenFilterThenNoAuth() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer badsig");
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtTokenProvider.isValid("badsig")).willReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
