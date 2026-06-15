/*
 * Copyright Taanvi Khevaria, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.archival;

import org.eclipse.sw360.datahandler.services.archival.ArchivalRecord;
import org.eclipse.sw360.datahandler.services.archival.ArchiveManifest;

import java.util.List;

/** What ArchivalHandler.archive returns alongside the streamed TAR. */
public record ArchiveResult(String bundleId,
                            ArchiveManifest manifest,
                            List<ArchivalRecord> records) {}
