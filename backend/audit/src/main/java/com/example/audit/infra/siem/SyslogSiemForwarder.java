package com.example.audit.infra.siem;

import com.example.audit.AuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLSocketFactory;
import java.io.OutputStream;
import java.net.Socket;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "audit.siem", name = "mode", havingValue = "syslog")
@Slf4j
public class SyslogSiemForwarder implements SiemForwarder {

    private final SiemProperties props;
    private final ObjectMapper mapper;
    private SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();

    @Override
    public void forward(AuditEvent event) {
        if (props.getSyslogHost() == null) return;
        try {
            try (Socket socket = socketFactory.createSocket(props.getSyslogHost(), props.getSyslogPort());
                 OutputStream out = socket.getOutputStream()) {
                String body = mapper.writeValueAsString(event);
                String line = "<134>1 " + java.time.Instant.now() + " audit " + " - - - " + body + "\n";
                out.write(line.getBytes());
                out.flush();
            }
        } catch (Exception e) {
            log.warn("SIEM syslog forward failed: {}", e.getMessage());
        }
    }

    // test용 주입
    void setSocketFactory(SSLSocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }
}
