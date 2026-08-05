/*
 * Copyright Taanvi Khevaria, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.archival;

import java.io.InputStream;
import java.util.List;

import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.archival.ArchivalClient;
import org.eclipse.sw360.datahandler.archival.ArchivalClients;
import org.eclipse.sw360.datahandler.services.archival.ArchivalRecord;
import org.eclipse.sw360.datahandler.services.archival.ArchivePreview;
import org.eclipse.sw360.datahandler.services.archival.ArchiveRequest;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.stereotype.Service;

@Service
public class Sw360ArchivalService {

    private ArchivalClient client() {
        return ArchivalClients.get();
    }

    public ArchivePreview preview(ArchiveRequest request, User sw360User) {
        return client().preview(request, UserConverter.fromThrift(sw360User));
    }

    public InputStream archive(ArchiveRequest request, User sw360User) {
        return client().archive(request, UserConverter.fromThrift(sw360User));
    }

    public List<ArchivalRecord> listRecords() {
        return client().listRecords();
    }

    public ArchivalRecord getRecord(String id) {
        return client().getRecord(id);
    }

    public void deleteRecord(String id) {
        client().deleteRecord(id);
    }
}
