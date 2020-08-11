/*
 * Copyright Siemens AG, 2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.authserver.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class Sw360SecurityEncryptor {

    private static final Logger log = LogManager.getLogger(Sw360SecurityEncryptor.class);

    private static final byte[] PUBLIC_SPEC_KEY = "sw360-rest-super-secret-password".getBytes();
    private static final String SECRET_SPEC_TRANSFORMATION = "AES";

    public static SealedObject encrypt(String password) throws IOException {
        if (password != null) {
            Cipher cipher = getCipher(Cipher.ENCRYPT_MODE);
            try {
                return new SealedObject(password, cipher);
            } catch (IllegalBlockSizeException e) {
                log.error("could not encrypt to sealed object", e);
            }
        } else {
            log.warn("could not encrypt to sealed object because the password is null");
        }
        return null;
    }

    public static String decrypt(SealedObject password) throws IOException {
        try {
            return password.getObject(getCipher(Cipher.DECRYPT_MODE)).toString();
        } catch (ClassNotFoundException | IllegalBlockSizeException | BadPaddingException e) {
            log.error("could not decrypt sealed object", e);
            return null;
        }
    }

    private static Cipher getCipher(int mode) {
        SecretKeySpec sks = new SecretKeySpec(PUBLIC_SPEC_KEY, SECRET_SPEC_TRANSFORMATION);
        try {
            Cipher cipher = Cipher.getInstance(SECRET_SPEC_TRANSFORMATION);
            cipher.init(mode, sks);
            return cipher;
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("error occured by initializing cipher object", e);
            return null;
        }
    }
}
