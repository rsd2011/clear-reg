package com.example.audit.infra.siem;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

final class SiemTestUtil {

    private static final String PEM_PRIVATE_KEY = """
-----BEGIN PRIVATE KEY-----
MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQC1yUirerGL9T3f
iM4FJrUwGy2Hq4eUXRPPY0/L7sHNf2CB7wNjNEIIWsAe31APazBih17IOFBZEBY9
KzGsqUfylweRAHF5Bg2pznMV+pzu9otRmHyzfuBR5z7bDvCfq5ilA7Xqt7V8gwx9
D+WnaiuJqleHW5XXhm2iX12nLFrX7RP0cTDCrPUcsQozzZZkcjqxcmUrVEhIEA5/
843kZBMR/sXOwbjoVLzgujjO73+0IHZ4RKvFjRbm+ucrpc4K4o6Xcbr98xihAEKg
F0E+WeERNfPGeJVjEIkD6Yi5QxCt+/8g3D2tHuQ7laPaR/Lr2421XC9xDIXKRJqS
g7c3BE6dAgMBAAECggEAKcDVcuN/t7aIs1jT/x1kpOu9Ian9fk+BnxL/r0bD10wg
Mb/g7o6AczRK59xhYmU7jAntNH4wwEAgZ4TOCEsqZhcX6w9nwMUp6rxrNRtmo8FX
fxKP49dqtQ0w4/YywO3UJeSsCkEPS427FteJp1IMtXY9W+7mhAYRlOekFmNq6yia
kZ0/DmZGypmU12Fixhg7WSp4NRgBL5CLQbV2oC3K+hGGJB+MxscyEWfCOFSr87Lq
+ilz2iBWmTTdTVfcHreeTNSVBvrrQWTaws7VM7bktd/aCcL2OCl0kd6hDG2vk4/1
QmuC6tcD6EJvfRDhp+sNjW0tpOyRAXILk81hlnfq2QKBgQDv8KZ/3PivMSToN8C/
WSBe6MJD00b2HYrNp8NgkH6wl3bZggYXfazlZwCEIxdXqQmGQ5VfSe33rm+CJRe5
JmtX5QJviG7vLdjmzb+w3HsN4dbQN215krdU9muKKQGVfNK6cuwN9M+UvQ2S7iCu
tCNHNqzCxtWzcm+SymLmsTUl1QKBgQDB9CySMoouoZ3SJYJO2obofEzwnS1Tzls/
2GSNcIdJjrSP/yt+rtuc+orlgC1FXHCbtLNqB+3eTybJQZmd/NhfoQ9bc/PZt2Im
FNqx+WvRPMn7Uqbr66ILRNvekGnSR6o5z1SyCZy7+whr0Mu6P6iR7dwC0RRp6Yqz
l4X14GaBqQKBgAsqnfURaZltphQQlC3TjggcuP3DFWha05zik9sonlzFj8hrnrD1
Lli2xHVh1LJFsaXH+2ml3SdLvPDHnSUGvrQmekiKhu8mqROEu8kVWby5p++kxLmF
tHaFyVdytRop5vy6YHX90LNRYRJXdrnqtEGaL8wYyNVDHCIuvQ+Ta6/JAoGAbCNz
5l9/Y9iAYfa/3K9CoMUNMOqxpxRbFBHWnnWrOHv0eITQ/2UG1VNrteHtW8rajjYG
AoxBN73x5ixJNikPiuIF6fuQbkcs1gaymKB0WMkH5LSElO1f/+B4pnLmTXPfmTPc
CRgK51RjsZlMvZ9YoWR5Uqi4NQUdbFyDOD/RZ2ECgYAI2gpbEDml5EVdXpovwR3p
x0yo8KgabQTN2EHfIFkJYfS3SJvlDqVaW8+UihM4qfBj6aaJshvXy/pjunnthlNK
ZX5XHYvALHe8qaUia4cqGfA421+E/o+7n8HKXmbPiVQRIG6iEToL5OK3dkRIwbXb
DYTSTWC8gkQhSIxyWyw/9Q==
-----END PRIVATE KEY-----
""";

    private static final String PEM_CERTIFICATE = """
-----BEGIN CERTIFICATE-----
MIIDCTCCAfGgAwIBAgIUfwTvR1MXGwqLWA3BFW1CvLeWSIYwDQYJKoZIhvcNAQEL
BQAwFDESMBAGA1UEAwwJc2llbS10ZXN0MB4XDTI1MTEyNDExMjExNFoXDTI2MTEy
NDExMjExNFowFDESMBAGA1UEAwwJc2llbS10ZXN0MIIBIjANBgkqhkiG9w0BAQEF
AAOCAQ8AMIIBCgKCAQEAtclIq3qxi/U934jOBSa1MBsth6uHlF0Tz2NPy+7BzX9g
ge8DYzRCCFrAHt9QD2swYodeyDhQWRAWPSsxrKlH8pcHkQBxeQYNqc5zFfqc7vaL
UZh8s37gUec+2w7wn6uYpQO16re1fIMMfQ/lp2oriapXh1uV14Ztol9dpyxa1+0T
9HEwwqz1HLEKM82WZHI6sXJlK1RISBAOf/ON5GQTEf7FzsG46FS84Lo4zu9/tCB2
eESrxY0W5vrnK6XOCuKOl3G6/fMYoQBCoBdBPlnhETXzxniVYxCJA+mIuUMQrfv/
INw9rR7kO5Wj2kfy69uNtVwvcQyFykSakoO3NwROnQIDAQABo1MwUTAdBgNVHQ4E
FgQU5LgiJ7sHokut7+sONymkSMqPoLgwHwYDVR0jBBgwFoAU5LgiJ7sHokut7+sO
NymkSMqPoLgwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAdWOh
cHvInld3TlQxBH//UW5hTu1lKigEFHHCZ+tiSCkF6Sjf2n6IUxU1qgYsOQB5tUAc
VcsnEslCkuy8VLpYektTu9/9VNu9x7vDbmfQ7bT2gUM455qZ3o0Dft6aWWXRvgtV
SAN5KMBgAtbrFlokfatj4sNWHGLsYjgW4GIMC+NOsP3y+9hfXS/8NAPB8eBMUdTS
bdoQItuhbRQWkUMsaSP4HijVTO62OIASOqiRl9DnLCKWFzFZrbYtTCHjRwTivvx4
jgXTCUwBvr/AXGAPDsYNhN7mryvUmgjwEhIjHW6mqLtIuGAJT3eKuvUQ+ZLo/qkO
yER+vBpmr1PJE40ypA==
-----END CERTIFICATE-----
""";

    private SiemTestUtil() {
    }

    static PrivateKey readPrivateKey() throws Exception {
        String normalized = PEM_PRIVATE_KEY.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(normalized);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    static X509Certificate readCertificate() throws Exception {
        String normalized = PEM_CERTIFICATE.replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
        byte[] certBytes = Base64.getDecoder().decode(normalized);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));
    }

    static File writePkcs12(char[] password) throws Exception {
        PrivateKey privateKey = readPrivateKey();
        X509Certificate cert = readCertificate();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, password);
        ks.setKeyEntry("siem", privateKey, password, new Certificate[]{cert});
        File temp = File.createTempFile("siem-keystore", ".p12");
        try (FileOutputStream fos = new FileOutputStream(temp)) {
            ks.store(fos, password);
        }
        return temp;
    }
}
