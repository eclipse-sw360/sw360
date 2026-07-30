/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.cvesearch;

import org.eclipse.sw360.datahandler.cvesearch.CveSearchClient;
import org.eclipse.sw360.datahandler.cvesearch.CveSearchClients;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.cvesearch.VulnerabilityUpdateStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class Sw360CveSearchServiceTest {

    @Mock
    private CveSearchClient cveSearchClient;

    private Sw360CveSearchService cveSearchService;

    @BeforeEach
    public void setUp() {
        CveSearchClients.set(cveSearchClient);
        cveSearchService = new Sw360CveSearchService();
    }

    @AfterEach
    public void tearDown() {
        CveSearchClients.set(null);
    }

    @Test
    public void updateForRelease_returnsResponseFromBackend() {
        VulnerabilityUpdateStatus expected = new VulnerabilityUpdateStatus()
                .setRequestStatus(RequestStatus.SUCCESS);
        when(cveSearchClient.updateForRelease("release123")).thenReturn(expected);

        VulnerabilityUpdateStatus result = cveSearchService.updateForRelease("release123");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void update_returnsRequestStatusFromBackend() {
        when(cveSearchClient.update()).thenReturn(RequestStatus.SUCCESS);

        RequestStatus result = cveSearchService.update();

        assertThat(result).isEqualTo(RequestStatus.SUCCESS);
    }

    @Test
    public void findCpes_returnsCpeSetFromBackend() {
        Set<String> expected = Set.of("cpe:2.3:a:apache:httpd:2.4.1:*:*:*:*:*:*:*");
        when(cveSearchClient.findCpes("apache", "httpd", "2.4.1")).thenReturn(expected);

        Set<String> result = cveSearchService.findCpes("apache", "httpd", "2.4.1");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void postForBody_rethrowsRestClientException() {
        when(cveSearchClient.fullUpdate()).thenThrow(new SW360Exception("Connection refused", 503));

        assertThatThrownBy(() -> cveSearchService.fullUpdate())
                .isInstanceOf(SW360Exception.class)
                .hasMessageContaining("Connection refused");
    }
}
