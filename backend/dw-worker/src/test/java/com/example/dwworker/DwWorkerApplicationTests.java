package com.example.dwworker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.example.audit.AuditPort;

@SpringBootTest
class DwWorkerApplicationTests {

    @MockBean
    AuditPort auditPort;

    @Test
    void contextLoads() {
    }
}
