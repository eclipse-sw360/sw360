/*
 * Copyright Siemens AG, 2023-2024.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.admin.attachment;

import java.util.Set;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentService;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class Sw360AttachmentCleanUpService {

    @NonNull
    private final RestControllerHelper restControllerHelper;

    ComponentService.Iface componentClient;
    AttachmentService.Iface attachmentClient;

    private ComponentService.Iface getThriftComponentClient() {
        ComponentService.Iface componentClient = new ThriftClients().makeComponentClient();
        return componentClient;
    }

    private AttachmentService.Iface getThriftAttachmentClient() {
        AttachmentService.Iface attachmentClient = new ThriftClients().makeAttachmentClient();
        return attachmentClient;
    }

    public RequestSummary cleanUpAttachments(User sw360User) throws TException {
        componentClient = getThriftComponentClient();
        attachmentClient = getThriftAttachmentClient();
        final Set<String> usedAttachmentIds = componentClient.getUsedAttachmentContentIds();
        return attachmentClient.vacuumAttachmentDB(sw360User, usedAttachmentIds);
    }
}
