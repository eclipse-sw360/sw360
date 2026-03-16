/*
SPDX-FileCopyrightText: © 2026 Contributors to the Eclipse SW360 project
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents.handler;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMComponent;
import org.eclipse.sw360.vmcomponents.AbstractJSONMockTest;
import org.eclipse.sw360.vmcomponents.db.VMDatabaseHandler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * Standalone unit tests for the getVulIdsPerComponentVmId fix (GitHub issue #2726).
 *
 * These tests do NOT need CouchDB or SVM credentials — they use WireMock to fake
 * the SVM HTTP endpoint and a Mockito mock for the DB handler so no infrastructure
 * is required.
 */
@RunWith(MockitoJUnitRunner.class)
public class SVMSyncHandlerVulIdsTest extends AbstractJSONMockTest {

    // WireMock runs on port 8090 (started by the @Rule in AbstractJSONMockTest).
    private static final String VUL_URL_TEMPLATE =
            "http://localhost:8090/portal/api/v1/public/components/#compVmId#/notifications";

    private SVMSyncHandler<VMComponent> handler;
    private Method getVulIdsMethod;

    @Before
    public void setUp() throws Exception {
        // Build the handler via the private (Class, VMDatabaseHandler) constructor so
        // we never touch CouchDB. A Mockito mock satisfies the non-null check.
        Constructor<SVMSyncHandler> ctor = SVMSyncHandler.class
                .getDeclaredConstructor(Class.class, VMDatabaseHandler.class);
        ctor.setAccessible(true);
        @SuppressWarnings("unchecked")
        SVMSyncHandler<VMComponent> h =
                (SVMSyncHandler<VMComponent>) ctor.newInstance(VMComponent.class,
                        mock(VMDatabaseHandler.class));
        handler = h;

        getVulIdsMethod = SVMSyncHandler.class
                .getDeclaredMethod("getVulIdsPerComponentVmId", String.class, String.class);
        getVulIdsMethod.setAccessible(true);
    }

    @SuppressWarnings("unchecked")
    private Set<String> invoke(String componentVmId, String urlTemplate) throws Exception {
        return (Set<String>) getVulIdsMethod.invoke(handler, componentVmId, urlTemplate);
    }

    /**
     * Happy path: SVM returns objects that each have an "id" field.
     * Expected: the three vulnerability IDs are extracted and returned.
     */
    @Test
    public void testGetVulIdsPerComponentVmId_ObjectsWithIdField_ReturnsVulIds() throws Exception {
        staticJSONResponse("/portal/api/v1/public/components/70/notifications",
                "[{\"id\": \"19936\"}, {\"id\": \"20705\"}, {\"id\": \"22955\"}]");

        Set<String> result = invoke("70", VUL_URL_TEMPLATE);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("19936"));
        assertTrue(result.contains("20705"));
        assertTrue(result.contains("22955"));
    }

    /**
     * NPE scenario (GitHub issue #2726):
     * SVM returns objects that have NO "id" field.
     * Before fix: json.get("id") == null → .toString() throws NullPointerException
     *             (caught silently → empty set returned, notifications never updated).
     * After fix:  null check skips the element gracefully → empty set, no exception.
     */
    @Test
    public void testGetVulIdsPerComponentVmId_ObjectWithMissingIdField_ReturnsEmptyGracefully()
            throws Exception {
        staticJSONResponse("/portal/api/v1/public/components/71/notifications",
                "[{\"publish_date\": \"2014-01-01\", \"last_update\": null}]");

        Set<String> result = invoke("71", VUL_URL_TEMPLATE);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    /**
     * ClassCastException scenario (GitHub issue #2726):
     * SVM returns plain integer IDs instead of JSON objects.
     * Before fix: (JsonObject) id throws ClassCastException
     *             (caught silently → empty set returned, notifications never updated).
     * After fix:  primitives fall into the else branch → id.toString() → IDs returned.
     */
    @Test
    public void testGetVulIdsPerComponentVmId_PlainIntegerIds_ReturnsVulIds() throws Exception {
        staticJSONResponse("/portal/api/v1/public/components/72/notifications",
                "[19936, 20705, 22955]");

        Set<String> result = invoke("72", VUL_URL_TEMPLATE);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("19936"));
        assertTrue(result.contains("20705"));
        assertTrue(result.contains("22955"));
    }

    // -----------------------------------------------------------------------
    // Root-cause verification tests — prove the bug scenarios exist at the
    // json-simple library level, independent of SVMSyncHandler.
    // These make the fix undeniable: they confirm exactly WHY the old code broke.
    // -----------------------------------------------------------------------

    /**
     * Proves the NPE root cause: json-simple returns null for a missing key,
     * so calling .toString() on that null throws NullPointerException.
     * This is exactly what the old line `json.get(VULNERABILITY_ID).toString()`
     * did when the "id" field was absent from an SVM API response object.
     */
    @Test
    public void rootCause_JsonObjectMissingKey_GetReturnsNull_ToStringThrowsNPE() {
        JsonObject obj = new JsonObject();
        obj.put("publish_date", "2014-01-01");

        Object vulIdObj = obj.get("id");
        assertNull("json-simple returns null for a missing key", vulIdObj);

        try {
            vulIdObj.toString(); // replicates the old buggy line
            fail("Expected NullPointerException — this is the bug from issue #2726");
        } catch (NullPointerException e) {
            // confirmed: the old code threw NPE here
        }
    }

    /**
     * Proves the ClassCastException root cause: json-simple parses plain integers
     * as Long values, not as JsonObject instances, so the old cast `(JsonObject) id`
     * throws ClassCastException.
     */
    @Test
    public void rootCause_PlainIntegerInJsonArray_CastToJsonObjectThrowsClassCastException() {
        JsonArray ids = Jsoner.deserialize("[19936, 20705, 22955]", new JsonArray());
        Object firstId = ids.get(0);

        assertFalse("plain integer is NOT a JsonObject", firstId instanceof JsonObject);
        assertEquals("json-simple 4.x parses integers as BigDecimal", java.math.BigDecimal.class, firstId.getClass());

        try {
            @SuppressWarnings("unused")
            JsonObject json = (JsonObject) firstId; // replicates the old buggy cast
            fail("Expected ClassCastException — this is the bug from issue #2726");
        } catch (ClassCastException e) {
            // confirmed: the old code threw ClassCastException here
        }
    }
}
