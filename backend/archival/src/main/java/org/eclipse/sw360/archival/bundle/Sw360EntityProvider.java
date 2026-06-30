/*
 * Copyright Taanvi Khevaria, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.archival.bundle;

import org.eclipse.sw360.services.archival.ArchivalEntityType;

/**
 * Production EntityProvider that reads from the live SW360 databases.
 * Not wired yet — the DB handler integration is still to come.
 *
 * Planned wiring:
 *   PROJECT   -> ProjectDatabaseHandler.getProjectByIdIgnoringVisibility(id)
 *                + ChangeLogsDatabaseHandler.getChangeLogsByDocumentId(id)
 *                + ProjectObligationRepository, AttachmentUsageRepository
 *   COMPONENT -> ComponentDatabaseHandler.getComponent(id, user) (+ all linked Releases)
 *   RELEASE   -> ComponentDatabaseHandler.getRelease(id, user) (+ SpdxDocument if present)
 *   PACKAGE   -> PackageDatabaseHandler.getPackageById(id)
 *
 * Attachments: walk entity.getAttachments() and build an AttachmentSource for each,
 * backed by AttachmentConnector.unsafeGetAttachmentStream(...).
 *
 * Project entities additionally persist:
 *   - clearing-requests.json (ProjectVulnerabilityRating etc.)
 *   - obligations.json
 *   - attachment-usage.json (so clearing decisions survive a restore)
 */
public class Sw360EntityProvider implements EntityProvider {

    private final boolean includeAttachments;
    private final boolean includeChangelogs;

    public Sw360EntityProvider(boolean includeAttachments, boolean includeChangelogs) {
        this.includeAttachments = includeAttachments;
        this.includeChangelogs = includeChangelogs;
    }

    @Override
    public boolean includeAttachments() { return includeAttachments; }

    @Override
    public boolean includeChangelogs() { return includeChangelogs; }

    @Override
    public CollectedEntity collect(ArchivalEntityType type, String entityId) {
        throw new UnsupportedOperationException(
                "Sw360EntityProvider is not wired to the live SW360 databases yet. "
                        + "Requested " + type + "/" + entityId);
    }
}
