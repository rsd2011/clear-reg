package com.example.audit.infra.siem;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SiemSslContextBuilderTest {

    @Test
    @DisplayName("키스토어/트러스트스토어로 SSLContext를 생성한다")
    void buildsContext() throws Exception {
        File ks = SiemTestUtil.writePkcs12("changeit".toCharArray());

        SSLContext ctx = SiemSslContextBuilder.build(ks.getAbsolutePath(), "changeit", ks.getAbsolutePath(), "changeit");

        assertThat(ctx.getSocketFactory()).isNotNull();
        ks.delete();
    }
}

