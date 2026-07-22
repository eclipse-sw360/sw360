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

import org.eclipse.sw360.datahandler.services.archival.ArchivalEntityType;

/**
 * Retrieves an entity and its associated data from the live SW360 databases.
 * Implementations use the existing Project, Component, Release, and Package
 * database handlers. Keeping this behind an interface allows the archive
 * workflow to be tested without requiring a running CouchDB instance.
 */
public interface EntityProvider {

    boolean includeAttachments();

    boolean includeChangelogs();

    CollectedEntity collect(ArchivalEntityType type, String entityId) throws Exception;
}
