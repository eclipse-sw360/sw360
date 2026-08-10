/*
 * Copyright Taanvi Khevaria, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.archival;

import java.io.InputStream;
import java.util.List;

import org.eclipse.sw360.datahandler.services.archival.ArchivalRecord;
import org.eclipse.sw360.datahandler.services.archival.ArchivePreview;
import org.eclipse.sw360.datahandler.services.archival.ArchiveRequest;
import org.eclipse.sw360.datahandler.services.users.User;

/**
 * Server-to-server view of the standalone archival service. The resource server
 * calls these methods; the identity travels as X-User-* headers so no browser
 * ever talks to the archival service directly.
 */
public interface ArchivalClient {

    /** Dry run: what an archive would do, without changing anything. */
    ArchivePreview preview(ArchiveRequest request, User user);

    /**
     * Runs the archive and returns the TAR.GZ bundle as a stream. The caller owns
     * the stream and must close it (it holds an open HTTP connection).
     */
    InputStream archive(ArchiveRequest request, User user);

    List<ArchivalRecord> listRecords();

    ArchivalRecord getRecord(String id);

    void deleteRecord(String id);
}
