/*
 * Copyright Bosch.IO GmbH 2020.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.attachment;

import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentService;
import org.eclipse.sw360.rest.resourceserver.core.ThriftServiceProvider;
import org.springframework.stereotype.Component;

@Component
public class ThriftAttachmentServiceProvider implements ThriftServiceProvider<AttachmentService.Iface> {
    @Override
    public AttachmentService.Iface getService(String thriftServerUrl) {
        return ThriftClients.makeAttachmentClient();
    }
}
