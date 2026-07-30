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
import org.eclipse.sw360.datahandler.attachments.AttachmentClient;
import org.eclipse.sw360.datahandler.attachments.AttachmentClients;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SW360AttachmentBackendServiceTest {

    @Mock
    private AttachmentClient attachmentClient;

    @Mock
    private AttachmentTypeBridge attachmentTypeBridge;

    private SW360AttachmentBackendService attachmentBackendService;

    @BeforeEach
    public void setUp() {
        AttachmentClients.set(attachmentClient);
        attachmentBackendService = new SW360AttachmentBackendService(attachmentTypeBridge);
    }

    @AfterEach
    public void tearDown() {
        AttachmentClients.set(null);
    }

    @Test
    public void getSha1FromAttachmentContentId_returnsSha1FromBackend() throws TException {
        when(attachmentClient.getSha1FromAttachmentContentId("content-id-1")).thenReturn("abc123sha1");

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
        when(attachmentClient.makeAttachmentContent(pojoInput)).thenReturn(pojoResponse);
        when(attachmentTypeBridge.toThrift(pojoResponse)).thenReturn(thriftResponse);

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

        when(attachmentClient.getAttachmentContent("content-1")).thenReturn(pojo);
        when(attachmentTypeBridge.toThrift(pojo)).thenReturn(thrift);

        AttachmentContent result = attachmentBackendService.getAttachmentContent("content-1");

        assertThat(result).isEqualTo(thrift);
    }

    @Test
    public void getAttachmentContent_throwsSw360ExceptionWhenNotFound() {
        when(attachmentClient.getAttachmentContent("missing-id"))
                .thenThrow(new org.eclipse.sw360.datahandler.services.common.SW360Exception("attachment not found", 404));

        assertThatThrownBy(() -> attachmentBackendService.getAttachmentContent("missing-id"))
                .isInstanceOf(SW360Exception.class)
                .satisfies(ex -> assertThat(((SW360Exception) ex).getErrorCode()).isEqualTo(404));
    }
}
