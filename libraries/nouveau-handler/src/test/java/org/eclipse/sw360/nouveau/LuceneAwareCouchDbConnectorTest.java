/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.nouveau;

import com.google.gson.Gson;
import com.ibm.cloud.cloudant.security.CouchDbSessionAuthenticator;
import com.ibm.cloud.cloudant.v1.Cloudant;
import com.ibm.cloud.sdk.core.security.Authenticator;
import junit.framework.TestCase;

import java.lang.reflect.Field;

public class LuceneAwareCouchDbConnectorTest extends TestCase {

    public void testEnsureDesignIdMissing() {
        assert (LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX + "lucene").equals(LuceneAwareCouchDbConnector.ensureDesignId("lucene"));
    }

    public void testEnsureDesignIdContaining() {
        assert (LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX + "lucene").equals(LuceneAwareCouchDbConnector.ensureDesignId(LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX + "lucene"));
    }

    public void testConnectorConstructionDoesNotMutateSharedAuthenticator() throws Exception {
        Authenticator auth = CouchDbSessionAuthenticator.newAuthenticator("sw360", "sw360");
        Cloudant client = new Cloudant("sw360-couchdb", auth);
        client.setServiceUrl("http://couchdb:5984");

        Field sessionUrlField = CouchDbSessionAuthenticator.class.getDeclaredField("sessionUrl");
        sessionUrlField.setAccessible(true);
        assertEquals("http://couchdb:5984/_session", sessionUrlField.get(auth).toString());

        Field tokenDataField = auth.getClass().getSuperclass().getSuperclass().getDeclaredField("tokenData");
        tokenDataField.setAccessible(true);

        Class<?> tokenClass = Class.forName("com.ibm.cloud.cloudant.security.CouchDbSessionAuthenticator$CouchDbSessionToken");
        java.lang.reflect.Constructor<?> tokenCtor = tokenClass.getDeclaredConstructor(long.class);
        tokenCtor.setAccessible(true);
        Object mockToken = tokenCtor.newInstance(System.currentTimeMillis() + 3600_000L);
        tokenDataField.set(auth, mockToken);
        assertNotNull(tokenDataField.get(auth));

        new LuceneAwareCouchDbConnector(client, "ddoc", "sw360db", new Gson());

        assertEquals("http://couchdb:5984/_session", sessionUrlField.get(auth).toString());
        assertNotNull("Active session token must not be invalidated by connector construction", tokenDataField.get(auth));
    }
}
