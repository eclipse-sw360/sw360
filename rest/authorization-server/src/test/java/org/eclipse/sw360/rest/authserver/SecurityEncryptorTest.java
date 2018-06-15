/*
 * Copyright Siemens AG, 2018. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.authserver;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import javax.crypto.SealedObject;

import static org.eclipse.sw360.rest.authserver.security.Sw360SecurityEncryptor.decrypt;
import static org.eclipse.sw360.rest.authserver.security.Sw360SecurityEncryptor.encrypt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
public class SecurityEncryptorTest {

    @Test
    public void testPasswordEncryptor() throws Exception {
        String password = "test password for security encryption!";
        SealedObject encrypted = encrypt(password);
        assertTrue(!password.equals(encrypted));
        String decrypted = decrypt(encrypted);
        assertEquals(password, decrypted);
    }
}
