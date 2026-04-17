/*
 * Copyright Aman Kumar, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.license;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Sw360LicenseServiceTest {

    @Mock
    private LicenseService.Iface licenseClient;

    private Sw360LicenseService licenseService;

    @Before
    public void setUp() throws TException {
        licenseService = spy(new Sw360LicenseService());
        doReturn(licenseClient).when(licenseService).getThriftLicenseClient();
    }

    @Test
    public void updateLicense_throwsBadRequestClientException_whenClientReturnsFailure() throws TException {
        when(licenseClient.updateLicense(any(), any(), any())).thenReturn(RequestStatus.FAILURE);

        assertThatThrownBy(() -> licenseService.updateLicense(new License(), new User()))
                .isInstanceOf(BadRequestClientException.class)
                .hasMessageContaining(RequestStatus.FAILURE.toString());
    }

    @Test
    public void updateLicense_returnsSuccess_whenClientReturnsSuccess() throws TException {
        when(licenseClient.updateLicense(any(), any(), any())).thenReturn(RequestStatus.SUCCESS);

        RequestStatus result = licenseService.updateLicense(new License(), new User());

        assertThat(result).isEqualTo(RequestStatus.SUCCESS);
    }
}
