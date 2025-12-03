package com.example.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 인코더 설정.
 *
 * <p>SecurityConfig에서 분리하여 순환 참조를 방지합니다.
 * SecurityConfig → JwtAuthenticationFilter → UserAccountDetailsService → UserAccountProvider
 * → UserAccountService → PasswordEncoder 경로의 순환 참조 문제 해결.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
