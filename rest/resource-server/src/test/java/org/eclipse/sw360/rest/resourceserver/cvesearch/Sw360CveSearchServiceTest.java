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

import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.cvesearch.VulnerabilityUpdateStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Sw360CveSearchServiceTest {

    @Mock
    private RestClient restClient;

    private Sw360CveSearchService cveSearchService;

    @Before
    public void setUp() {
        cveSearchService = new Sw360CveSearchService(restClient);
    }

    @Test
    public void updateForRelease_returnsResponseFromBackend() {
        VulnerabilityUpdateStatus expected = new VulnerabilityUpdateStatus()
                .setRequestStatus(RequestStatus.SUCCESS);
        mockPostForBody(expected, VulnerabilityUpdateStatus.class);

        VulnerabilityUpdateStatus result = cveSearchService.updateForRelease("release123");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void update_returnsRequestStatusFromBackend() {
        mockPostForBody(RequestStatus.SUCCESS, RequestStatus.class);

        RequestStatus result = cveSearchService.update();

        assertThat(result).isEqualTo(RequestStatus.SUCCESS);
    }

    @Test
    public void findCpes_returnsCpeSetFromBackend() {
        Set<String> expected = Set.of("cpe:2.3:a:apache:httpd:2.4.1:*:*:*:*:*:*:*");
        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(Function.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(expected);

        Set<String> result = cveSearchService.findCpes("apache", "httpd", "2.4.1");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void postForBody_rethrowsRestClientException() {
        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenThrow(new RestClientException("Connection refused"));

        assertThatThrownBy(() -> cveSearchService.fullUpdate())
                .isInstanceOf(RestClientException.class)
                .hasMessageContaining("Connection refused");
    }

    private <T> void mockPostForBody(T response, Class<T> responseType) {
        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(responseType))).thenReturn(response);
    }
}
