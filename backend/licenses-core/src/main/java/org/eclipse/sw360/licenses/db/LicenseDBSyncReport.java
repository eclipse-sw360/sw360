/*
 * Copyright Sandip Mandal, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses.db;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LicenseDBSyncReport {
    private String type = "licensedb-sync-report";
    private String syncType;
    private String startDate;
    private String endDate;
    private int processingSeconds;
    private String status;
    private String message;
    private int totalElements;
    private int totalAffectedElements;
}
