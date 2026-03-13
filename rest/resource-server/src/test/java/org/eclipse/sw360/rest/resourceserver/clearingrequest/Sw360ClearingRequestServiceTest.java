/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.clearingrequest;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.ClearingRequestState;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// Tests that verify the thrift client resource leak fix.
// Before the fix, getThriftModerationClient() was creating a new THttpClient on every call.
// Now we delegate to an injected ThriftClients instance instead.
// TODO: clean this up later, some of the reflection stuff is a bit messy -GM
@RunWith(MockitoJUnitRunner.class)
public class Sw360ClearingRequestServiceTest {

    @Mock
    private ThriftClients thriftClients;

    @Mock
    private ModerationService.Iface moderationClient;

    private Sw360ClearingRequestService service;

    @Before
    public void setUp() throws Exception {
        service = new Sw360ClearingRequestService();
        // Use ReflectionTestUtils to override the thriftClients field with our mock.
        // The field is final so we can't just set it directly, but ReflectionTestUtils handles that.
        ReflectionTestUtils.setField(service, "thriftClients", thriftClients);

        when(thriftClients.makeModerationClient()).thenReturn(moderationClient);
    }

    @Test
    public void testOldPrivateFactoryMethodIsGone() {
        // The whole point of the fix -- getThriftModerationClient() used to build a raw
        // THttpClient on every invocation which leaks connections. It must not exist anymore.

        // getDeclaredMethod throws NoSuchMethodException if the method isn't there, which would
        // mean we'd have to wrap this in try/catch to assert the method is absent. Using
        // stream instead is cleaner for negative checks.
        Method[] methods = Sw360ClearingRequestService.class.getDeclaredMethods();
        boolean hasOldFactory = Arrays.stream(methods)
                .anyMatch(m -> m.getName().equals("getThriftModerationClient"));

        // private Method oldMethod = null; // keeping for now in case we need to rollback

        assertFalse("getThriftModerationClient should have been removed as part of the resource leak fix",
                hasOldFactory);
    }

    @Test
    public void testThriftClientsFieldIsPresent() throws Exception {
        // Sanity check -- make sure the field actually exists and is the right type.
        // If someone refactors this away we want to know immediately.
        boolean fieldFound = false;
        Field[] allFields = Sw360ClearingRequestService.class.getDeclaredFields();
        for (Field f : allFields) {
            if (f.getName().equals("thriftClients") && f.getType().equals(ThriftClients.class)) {
                fieldFound = true;
                break;
            }
        }
        assertTrue("Expected a ThriftClients field named 'thriftClients' in service class", fieldFound);
    }

    @Test
    public void testGetClearingRequestByIdDelegatesToThriftClients() throws TException {
        ClearingRequest cr = new ClearingRequest();
        cr.setId("CR-42");
        User u = new User();
        u.setEmail("user@example.org");

        when(moderationClient.getClearingRequestById(anyString(), any(User.class))).thenReturn(cr);

        ClearingRequest result = service.getClearingRequestById("CR-42", u);

        assertNotNull(result);
        assertEquals("CR-42", result.getId());

        // This is the key assertion -- proves we used the injected client, not a locally
        // constructed THttpClient. If the old code were still there, thriftClients would
        // never be touched and this verify would fail.
        verify(thriftClients, atLeastOnce()).makeModerationClient();
        verify(moderationClient).getClearingRequestById("CR-42", u);
    }

    @Test
    public void testGetClearingRequestByProjectIdDelegatesToThriftClients() throws TException {
        ClearingRequest cr = new ClearingRequest();
        cr.setId("CR-99");
        cr.setProjectId("P-7");
        User sw360User = new User();
        sw360User.setEmail("admin@sw360.org");

        when(moderationClient.getClearingRequestByProjectId(anyString(), any())).thenReturn(cr);

        ClearingRequest returned = service.getClearingRequestByProjectId("P-7", sw360User);

        assertEquals("P-7", returned.getProjectId());
        verify(thriftClients).makeModerationClient();
    }

    @Test
    public void testGetMyClearingRequestsCallsModerationClientTwice() throws TException {
        // getMyClearingRequests calls makeModerationClient() twice:
        // once for getMyClearingRequests and once for getClearingRequestsByBU
        User u = new User();
        u.setEmail("someone@sw360.org");
        u.setDepartment("DEPT-A");

        when(moderationClient.getMyClearingRequests(any())).thenReturn(new HashSet<ClearingRequest>());
        when(moderationClient.getClearingRequestsByBU(anyString())).thenReturn(new HashSet<ClearingRequest>());

        Set<ClearingRequest> result = service.getMyClearingRequests(u, null);

        assertNotNull(result);
        // System.out.println("DEBUG result size: " + result.size());
        verify(thriftClients, times(2)).makeModerationClient();
    }

    @Test
    public void testSameThriftClientsInstanceReusedAcrossMultipleCalls() throws TException {
        // This is the actual regression test for the leak fix.
        // Before the fix, each method call constructed its own THttpClient internally --
        // meaning a new connection was opened and never closed on every request.
        // After the fix, all calls go through the same shared ThriftClients field.
        // If that ever regresses, thriftClients here would never be called (it'd use a locally
        // constructed client instead), and these verifies would fail.
        ClearingRequest cr1 = new ClearingRequest();
        cr1.setId("CR-1");
        ClearingRequest cr2 = new ClearingRequest();
        cr2.setId("CR-2");
        User u = new User();
        u.setEmail("admin@sw360.org");

        when(moderationClient.getClearingRequestById("CR-1", u)).thenReturn(cr1);
        when(moderationClient.getClearingRequestById("CR-2", u)).thenReturn(cr2);

        service.getClearingRequestById("CR-1", u);
        service.getClearingRequestById("CR-2", u);

        // both calls must go through the same injected thriftClients -- 2 total, not 0
        verify(thriftClients, times(2)).makeModerationClient();
        verifyNoMoreInteractions(thriftClients);
    }

    @Test
    public void testMakeModerationClientNotCalledForPureLocalMethods() {
        // convertTimestampToDateTime is static and doesn't need a thrift call.
        // Just verifying nothing weird happens when we call local utility methods.
        String formatted = Sw360ClearingRequestService.convertTimestampToDateTime(0L);
        assertNotNull(formatted);
        assertTrue(formatted.contains("1970")); // epoch in UTC should be 1970
        verifyNoInteractions(thriftClients);
    }
}
