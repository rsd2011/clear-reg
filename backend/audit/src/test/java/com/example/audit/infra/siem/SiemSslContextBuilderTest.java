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

    @Test
    @DisplayName("null 패스워드는 빈 문자열로 처리된다")
    void nullPasswordsTreatedAsEmpty() throws Exception {
        // 빈 패스워드로 생성된 키스토어
        File ks = SiemTestUtil.writePkcs12(new char[0]);

        // null 패스워드 전달 시 빈 char[]로 변환됨
        SSLContext ctx = SiemSslContextBuilder.build(
                ks.getAbsolutePath(), null,
                ks.getAbsolutePath(), null
        );

        assertThat(ctx.getSocketFactory()).isNotNull();
        ks.delete();
    }

    @Test
    @DisplayName("키스토어 패스워드만 null인 경우")
    void keyStorePasswordNullOnly() throws Exception {
        // 키스토어는 빈 패스워드로 생성 (null → new char[0]으로 변환됨)
        File ksFile = SiemTestUtil.writePkcs12(new char[0]);
        // 트러스트스토어는 별도로 "changeit" 패스워드로 생성
        File tsFile = SiemTestUtil.writePkcs12("changeit".toCharArray());

        SSLContext ctx = SiemSslContextBuilder.build(
                ksFile.getAbsolutePath(), null,
                tsFile.getAbsolutePath(), "changeit"
        );

        assertThat(ctx.getSocketFactory()).isNotNull();
        ksFile.delete();
        tsFile.delete();
    }

    @Test
    @DisplayName("트러스트스토어 패스워드만 null인 경우")
    void trustStorePasswordNullOnly() throws Exception {
        // 키스토어는 "changeit" 패스워드로 생성
        File ksFile = SiemTestUtil.writePkcs12("changeit".toCharArray());
        // 트러스트스토어는 빈 패스워드로 생성 (null → new char[0]으로 변환됨)
        File tsFile = SiemTestUtil.writePkcs12(new char[0]);

        SSLContext ctx = SiemSslContextBuilder.build(
                ksFile.getAbsolutePath(), "changeit",
                tsFile.getAbsolutePath(), null
        );

        assertThat(ctx.getSocketFactory()).isNotNull();
        ksFile.delete();
        tsFile.delete();
    }
}

