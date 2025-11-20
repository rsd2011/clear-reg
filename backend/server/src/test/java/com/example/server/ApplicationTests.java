package com.example.server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {Application.class, TestDraftAuditConfig.class})
@DisplayName("서버 애플리케이션 컨텍스트 테스트")
class ApplicationTests {

    @Test
    @DisplayName("Given 애플리케이션 When 시작하면 Then 컨텍스트가 정상 로드된다")
    void contextLoads() {
    }
}
