/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.attachment;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_SELF;

@ExtendWith(MockitoExtension.class)
public class SW360AttachmentBackendServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private AttachmentTypeBridge attachmentTypeBridge;

    private SW360AttachmentBackendService attachmentBackendService;

    @BeforeEach
    public void setUp() {
        attachmentBackendService = new SW360AttachmentBackendService(restClient, attachmentTypeBridge);
    }

    @Test
    public void getSha1FromAttachmentContentId_returnsSha1FromBackend() throws TException {
        mockGetForBody("content-id-1", "abc123sha1", String.class);

        String result = attachmentBackendService.getSha1FromAttachmentContentId("content-id-1");

        assertThat(result).isEqualTo("abc123sha1");
    }

    @Test
    public void makeAttachmentContent_postsAndReturnsThriftAttachment() throws TException {
        AttachmentContent thriftInput = new AttachmentContent().setFilename("test.txt");
        org.eclipse.sw360.datahandler.services.attachments.AttachmentContent pojoInput =
                new org.eclipse.sw360.datahandler.services.attachments.AttachmentContent().setFilename("test.txt");
        org.eclipse.sw360.datahandler.services.attachments.AttachmentContent pojoResponse =
                new org.eclipse.sw360.datahandler.services.attachments.AttachmentContent()
                        .setId("content-1")
                        .setFilename("test.txt");
        AttachmentContent thriftResponse = new AttachmentContent().setId("content-1").setFilename("test.txt");

        when(attachmentTypeBridge.toPojo(thriftInput)).thenReturn(pojoInput);
        when(attachmentTypeBridge.toThrift(pojoResponse)).thenReturn(thriftResponse);
        mockPostForBody(pojoResponse, org.eclipse.sw360.datahandler.services.attachments.AttachmentContent.class);

        AttachmentContent result = attachmentBackendService.makeAttachmentContent(thriftInput);

        assertThat(result).isEqualTo(thriftResponse);
        verify(attachmentTypeBridge).toPojo(thriftInput);
        verify(attachmentTypeBridge).toThrift(pojoResponse);
    }

    @Test
    public void getAttachmentContent_returnsThriftAttachment() throws SW360Exception {
        org.eclipse.sw360.datahandler.services.attachments.AttachmentContent pojo =
                new org.eclipse.sw360.datahandler.services.attachments.AttachmentContent().setId("content-1");
        AttachmentContent thrift = new AttachmentContent().setId("content-1");

        when(attachmentTypeBridge.toThrift(pojo)).thenReturn(thrift);
        mockGetForBody("content-1", pojo, org.eclipse.sw360.datahandler.services.attachments.AttachmentContent.class);

        AttachmentContent result = attachmentBackendService.getAttachmentContent("content-1");

        assertThat(result).isEqualTo(thrift);
    }

    @Test
    public void getAttachmentContent_throwsSw360ExceptionWhenNotFound() {
        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), eq("missing-id"))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(org.eclipse.sw360.datahandler.services.attachments.AttachmentContent.class))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.NOT_FOUND,
                        "Not Found",
                        HttpHeaders.EMPTY,
                        "attachment not found".getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8));

        assertThatThrownBy(() -> attachmentBackendService.getAttachmentContent("missing-id"))
                .isInstanceOf(SW360Exception.class)
                .satisfies(ex -> assertThat(((SW360Exception) ex).getErrorCode()).isEqualTo(404));
    }

    private <T> void mockGetForBody(String pathVariable, T response, Class<T> responseType) {
        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), eq(pathVariable))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(responseType))).thenReturn(response);
    }

    private <T> void mockPostForBody(T response, Class<T> responseType) {
        RestClient.RequestBodyUriSpec requestBodyUriSpec =
                mock(RestClient.RequestBodyUriSpec.class, RETURNS_SELF);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(responseType))).thenReturn(response);
    }
}
