/*
 * Copyright TOSHIBA CORPORATION, 2025. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.common;

public class SW360ConfigKeys {

    // boolean configuration
    public static final String SPDX_DOCUMENT_ENABLED = "spdx.document.enabled";
    public static final String IS_COMPONENT_VISIBILITY_RESTRICTION_ENABLED = "component.visibility.restriction.enabled";
    public static final String USE_LICENSE_INFO_FROM_FILES = "licenseinfo.spdxparser.use-license-info-from-files";
    public static final String MAINLINE_STATE_ENABLED_FOR_USER = "mainline.state.enabled.for.user";
    public static final String IS_STORE_ATTACHMENT_TO_FILE_SYSTEM_ENABLED = "enable.attachment.store.to.file.system";
    public static final String AUTO_SET_ECC_STATUS = "auto.set.ecc.status";
    public static final String MAIL_REQUEST_FOR_PROJECT_REPORT = "send.project.spreadsheet.export.to.mail.enabled";
    public static final String MAIL_REQUEST_FOR_COMPONENT_REPORT = "send.component.spreadsheet.export.to.mail.enabled";
    public static final String IS_BULK_RELEASE_DELETING_ENABLED = "bulk.release.deleting.enabled";
    public static final String DISABLE_CLEARING_FOSSOLOGY_REPORT_DOWNLOAD = "disable.clearing.fossology.report.download";
    public static final String IS_FORCE_UPDATE_ENABLED = "rest.force.update.enabled";

    // string configuration
    public static final String ATTACHMENT_STORE_FILE_SYSTEM_LOCATION = "attachment.store.file.system.location";
    public static final String SBOM_IMPORT_EXPORT_ACCESS_USER_ROLE = "sbom.import.export.access.usergroup";

    // number configuration
    public static final String ATTACHMENT_DELETE_NO_OF_DAYS = "attachment.delete.no.of.days";

    // unchangeable config
    public static final String ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP = "enable.flexible.project.release.relationship";

    private SW360ConfigKeys() {
    }
}
