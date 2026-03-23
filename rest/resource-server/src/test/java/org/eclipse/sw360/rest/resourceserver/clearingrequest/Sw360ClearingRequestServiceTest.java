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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// Unit tests for the thrift client resource leak fix in Sw360ClearingRequestService.
// Before the fix, a new THttpClient was created on every method call.
// After the fix, all calls go through a single shared ThriftClients instance.
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
        // inject our mock in place of the real ThriftClients bean
        ReflectionTestUtils.setField(service, "thriftClients", thriftClients);

        when(thriftClients.makeModerationClient()).thenReturn(moderationClient);
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

        // if the old per-call THttpClient code were still there, thriftClients
        // would never be touched and this verify would fail
        verify(thriftClients, atLeastOnce()).makeModerationClient();
        verify(moderationClient).getClearingRequestById("CR-42", u);
    }

    @Test
    public void testSameThriftClientsInstanceReusedAcrossMultipleCalls() throws TException {
        // this is the actual regression test for the leak --
        // calling the service multiple times must go through the same shared
        // ThriftClients field each time, not spin up a new client per call
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

        // 2 calls, both hitting the same injected instance -- not 0, not 2 separate ones
        verify(thriftClients, times(2)).makeModerationClient();
        verifyNoMoreInteractions(thriftClients);
    }
}
