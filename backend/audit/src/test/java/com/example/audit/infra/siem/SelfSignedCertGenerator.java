package com.example.audit.infra.siem;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Date;

import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateIssuerName;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateSubjectName;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

/**
 * 테스트용 self-signed 인증서 생성기 (sun.security.* 사용, 테스트 전용).
 */
class SelfSignedCertGenerator {
    static X509Certificate generate(String dn, KeyPair keyPair) throws Exception {
        long now = System.currentTimeMillis();
        Date from = new Date(now - 1000L);
        Date to = new Date(now + 365L * 24 * 60 * 60 * 1000);
        BigInteger sn = new BigInteger(64, new java.security.SecureRandom());
        X500Name owner = new X500Name(dn);

        X509CertInfo info = new X509CertInfo();
        info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
        info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
        info.set(X509CertInfo.SUBJECT, new CertificateSubjectName(owner));
        info.set(X509CertInfo.ISSUER, new CertificateIssuerName(owner));
        info.set(X509CertInfo.VALIDITY, new CertificateValidity(from, to));
        info.set(X509CertInfo.KEY, new CertificateX509Key(keyPair.getPublic()));
        info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(AlgorithmId.get("SHA256withRSA")));

        X509CertImpl cert = new X509CertImpl(info);
        cert.sign(keyPair.getPrivate(), "SHA256withRSA");
        return cert;
    }
}
