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
    @DisplayName("syslogHost가 null이면 forward를 건너뛴다")
    void skipsForwardWhenHostNull() {
        SiemProperties props = new SiemProperties();
        props.setEnabled(true);
        props.setMode("syslog");
        props.setSyslogHost(null); // null host

        SSLSocketFactory factory = Mockito.mock(SSLSocketFactory.class);
        SyslogSiemForwarder forwarder = new SyslogSiemForwarder(props, new ObjectMapper().findAndRegisterModules());
        forwarder.setSocketFactory(factory);

        // null host면 아무것도 하지 않고 리턴해야 함
        forwarder.forward(AuditEvent.builder().eventType("TEST").build());

        // 소켓 생성 시도 없이 리턴됨을 확인
        Mockito.verifyNoInteractions(factory);
    }

    @Test
    @DisplayName("소켓 연결 실패 시 경고 로그만 남기고 예외를 삼킨다")
    void logsWarningOnSocketFailure() throws Exception {
        SiemProperties props = new SiemProperties();
        props.setEnabled(true);
        props.setMode("syslog");
        props.setSyslogHost("localhost");
        props.setSyslogPort(6514);

        SSLSocketFactory factory = Mockito.mock(SSLSocketFactory.class);
        Mockito.when(factory.createSocket("localhost", 6514))
                .thenThrow(new java.io.IOException("Connection refused"));

        SyslogSiemForwarder forwarder = new SyslogSiemForwarder(props, new ObjectMapper().findAndRegisterModules());
        forwarder.setSocketFactory(factory);

        // 예외가 발생해도 throw 없이 정상 리턴
        forwarder.forward(AuditEvent.builder().eventType("TEST").build());

        verify(factory).createSocket("localhost", 6514);
    }

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
