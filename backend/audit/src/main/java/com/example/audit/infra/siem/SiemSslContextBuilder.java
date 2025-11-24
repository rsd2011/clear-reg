package com.example.audit.infra.siem;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

final class SiemSslContextBuilder {

    private SiemSslContextBuilder() {}

    static SSLContext build(String keyStorePath, String keyStorePassword,
                            String trustStorePath, String trustStorePassword) throws Exception {
        char[] kp = keyStorePassword != null ? keyStorePassword.toCharArray() : new char[0];
        char[] tp = trustStorePassword != null ? trustStorePassword.toCharArray() : new char[0];

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream fis = new FileInputStream(keyStorePath)) {
            ks.load(fis, kp);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, kp);

        KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream fis = new FileInputStream(trustStorePath)) {
            ts.load(fis, tp);
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return ctx;
    }
}
