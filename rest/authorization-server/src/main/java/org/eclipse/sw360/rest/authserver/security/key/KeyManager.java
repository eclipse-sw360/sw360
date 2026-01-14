/*
SPDX-FileCopyrightText: © 2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.authserver.security.key;

import java.io.FileInputStream;
import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.jwk.RSAKey;

@Component
public class KeyManager {

    private static final Logger log = LogManager.getLogger(KeyManager.class);

    @Value("${jwt.secretkey:sw360SecretKey}")
    private String secretKey;
    private final String keystore = CommonUtils.SYSTEM_CONFIGURATION_PATH+"/jwt-keystore.jks";

    public RSAKey rsaKey() throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore keyStore = loadKeyStore(keystore, secretKey);
        Certificate certificate = keyStore.getCertificate("jwt");
        RSAPublicKey publicKey = (RSAPublicKey) certificate.getPublicKey();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyStore.getKey("jwt", secretKey.toCharArray());
        Calendar cl = Calendar.getInstance();
        cl.add(Calendar.DATE, 30);
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey).expirationTime(cl.getTime())
                .build();
    }

    private static KeyStore loadKeyStore(String keystoreFile, String password) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        try (FileInputStream fileInputStream = new FileInputStream(keystoreFile)) {
            keyStore.load(fileInputStream, password.toCharArray());
        } catch (Exception e) {
            log.error("Error loading keystore from {}", keystoreFile, e);
        }
        return keyStore;
    }

}
