/*
 * Copyright Kavya Popat, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.vendor;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class Sw360VendorServiceTest {

    @Mock
    private VendorService.Iface vendorClient;

    private Sw360VendorService vendorService;

    @BeforeEach
    public void setUp() throws TException {
        vendorService = spy(new Sw360VendorService());
        doReturn(vendorClient).when(vendorService).getThriftVendorClient();
    }

    @Test
    public void vendorUpdate_throwsResourceNotFoundException_whenVendorDoesNotExist() throws TException {
        when(vendorClient.getByID("nonexistent-id")).thenReturn(null);

        Vendor patch = new Vendor();
        patch.setFullname("New Name");

        assertThatThrownBy(() -> vendorService.vendorUpdate(patch, new User(), "nonexistent-id"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("nonexistent-id");
    }

    @Test
    public void vendorUpdate_onlyPatchesNonNullFields_whenVendorExists() throws TException {
        Vendor existing = new Vendor();
        existing.setId("existing-id");
        existing.setFullname("Old Fullname");
        existing.setShortname("OldShort");
        existing.setUrl("http://old.example.com");

        when(vendorClient.getByID("existing-id")).thenReturn(existing);
        when(vendorClient.updateVendor(any(), any())).thenReturn(RequestStatus.SUCCESS);

        Vendor patch = new Vendor();
        patch.setFullname("New Fullname");
        // shortname and url intentionally left null - should NOT be overwritten

        RequestStatus result = vendorService.vendorUpdate(patch, new User(), "existing-id");

        assertThat(result).isEqualTo(RequestStatus.SUCCESS);
        assertThat(existing.getFullname()).isEqualTo("New Fullname");
        assertThat(existing.getShortname()).isEqualTo("OldShort");
        assertThat(existing.getUrl()).isEqualTo("http://old.example.com");
    }

    @Test
    public void vendorUpdate_wrapsThriftException_asRuntimeException() throws TException {
        when(vendorClient.getByID("id")).thenThrow(new TException("thrift connection failed"));

        assertThatThrownBy(() -> vendorService.vendorUpdate(new Vendor(), new User(), "id"))
                .isInstanceOf(RuntimeException.class);
    }
}
