package com.inha.coinkaraoke.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.inha.coinkaraoke.users.impl.HFCAServiceImpl;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Locale;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcECContentSignerBuilder;
import org.hyperledger.fabric.gateway.Identities;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.hyperledger.fabric.gateway.X509Identity;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.identity.X509Enrollment;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HFCAServiceTest {

    private final HFCAService hfcaService = new HFCAServiceImpl();

    @Mock
    private HFCAClient hfcaClient;


    @DisplayName("ca에 관리자를 등록한다.")
    @Test
    public void enrollAdminToCATest()
            throws InvalidArgumentException, EnrollmentException, IOException {

        //given
        X509Credentials x509Credentials = new X509Credentials();
        Enrollment expectedEnrollment =
                new X509Enrollment(x509Credentials.getPrivateKey(),x509Credentials.getCertificatePem());
        given(hfcaClient.enroll(any(), any())).willReturn(expectedEnrollment);
        Wallet wallet = Wallets.newInMemoryWallet();

        //when
        hfcaService.enrollAdmin(hfcaClient, wallet, "org1");

        //then
        then(hfcaClient).should(times(1)).enroll(any(), any());
        assertThat(((X509Identity)wallet.get("admin")).getCertificate())
                .isEqualTo(x509Credentials.certificate);
    }

    @DisplayName("지갑에 관리자 정보가 등록되어 있으면 실패한다.")
    @Test
    public void failToEnrollAdmin()
            throws CertificateException, IOException, InvalidArgumentException, EnrollmentException {

        //given
        Wallet wallet = Wallets.newInMemoryWallet();
        X509Credentials x509Credentials = new X509Credentials();
        wallet.put("admin", Identities.newX509Identity("org1",
                new X509Enrollment(x509Credentials.getPrivateKey(),
                        x509Credentials.getCertificatePem())));
        //when
        hfcaService.enrollAdmin(hfcaClient, wallet, "org1");

        //then
        then(hfcaClient).should(times(0)).enroll(any(), any());
    }

    public static class X509Credentials {
        private static final Provider BC_PROVIDER = new BouncyCastleProvider();

        private final X509Certificate certificate;
        private final PrivateKey privateKey;

        public X509Credentials() {
            KeyPair keyPair = generateKeyPair();
            certificate = generateCertificate(keyPair);
            privateKey = keyPair.getPrivate();
        }

        private KeyPair generateKeyPair() {
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", BC_PROVIDER);
                generator.initialize(256);
                return generator.generateKeyPair();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        private X509Certificate generateCertificate(KeyPair keyPair) {
            X500Name dnName = new X500Name("CN=John Doe");
            Date validityBeginDate = new Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000); // Yesterday
            Date validityEndDate = new Date(System.currentTimeMillis() + 2L * 365 * 24 * 60 * 60 * 1000); // 2 years from now
            SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
            X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                    dnName,
                    BigInteger.valueOf(System.currentTimeMillis()),
                    validityBeginDate,
                    validityEndDate,
                    Locale.getDefault(),
                    dnName,
                    subPubKeyInfo);

            AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256WithRSAEncryption");
            AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);

            try {
                ContentSigner contentSigner = new BcECContentSignerBuilder(sigAlgId, digAlgId)
                        .build(PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded()));
                X509CertificateHolder holder = builder.build(contentSigner);
                return new JcaX509CertificateConverter().getCertificate(holder);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (OperatorCreationException | CertificateException e) {
                throw new RuntimeException(e);
            }
        }

        public X509Certificate getCertificate() {
            return certificate;
        }

        public String getCertificatePem() {
            return Identities.toPemString(certificate);
        }

        public PrivateKey getPrivateKey() {
            return privateKey;
        }

        public String getPrivateKeyPem() {
            return Identities.toPemString(privateKey);
        }
    }
}

