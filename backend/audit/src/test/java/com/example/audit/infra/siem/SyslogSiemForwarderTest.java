package com.example.audit.infra.siem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.net.Socket;

import javax.net.ssl.SSLSocketFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.example.audit.AuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

class SyslogSiemForwarderTest {

    @Test
    @DisplayName("syslog 모드에서 TLS 소켓으로 JSON 라인을 전송한다")
    void writesSyslogLine() throws Exception {
        SiemProperties props = new SiemProperties();
        props.setEnabled(true);
        props.setMode("syslog");
        props.setSyslogHost("localhost");
        props.setSyslogPort(6514);

        // 소켓과 팩토리를 모킹하여 실제 네트워크 없이 write 캡처
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Socket mockSocket = Mockito.mock(Socket.class);
        Mockito.when(mockSocket.getOutputStream()).thenReturn(baos);
        SSLSocketFactory factory = Mockito.mock(SSLSocketFactory.class);
        Mockito.when(factory.createSocket("localhost", 6514)).thenReturn(mockSocket);

        SyslogSiemForwarder forwarder = new SyslogSiemForwarder(props, new ObjectMapper().findAndRegisterModules());
        forwarder.setSocketFactory(factory);

        forwarder.forward(AuditEvent.builder().eventType("TEST").build());

        verify(factory).createSocket("localhost", 6514);
        String sent = baos.toString();
        assertThat(sent).contains("TEST");
        assertThat(sent).contains("<134>");
    }
}
