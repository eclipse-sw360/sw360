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

    // This property is used to enable the tab SPDX Document feature
    public static final String SPDX_DOCUMENT_ENABLED = "spdx.document.enabled";

    // This property is used to enable the component visibility restriction feature
    public static final String IS_COMPONENT_VISIBILITY_RESTRICTION_ENABLED = "component.visibility.restriction.enabled";

    public static final String USE_LICENSE_INFO_FROM_FILES = "licenseinfo.spdxparser.use-license-info-from-files";
    public static final String MAINLINE_STATE_ENABLED_FOR_USER = "mainline.state.enabled.for.user";

    // Attachment storage configuration
    public static final String IS_STORE_ATTACHMENT_TO_FILE_SYSTEM_ENABLED = "enable.attachment.store.to.file.system";
    public static final String ATTACHMENT_DELETE_NO_OF_DAYS = "attachment.delete.no.of.days";
    public static final String ATTACHMENT_STORE_FILE_SYSTEM_LOCATION = "attachment.store.file.system.location";

    // Enable auto set ECC status
    public static final String AUTO_SET_ECC_STATUS = "auto.set.ecc.status";

    // This property is used to enable mail request for projects report
    public static final String MAIL_REQUEST_FOR_PROJECT_REPORT = "send.project.spreadsheet.export.to.mail.enabled";
    // This property is used to enable mail request for components report
    public static final String MAIL_REQUEST_FOR_COMPONENT_REPORT = "send.component.spreadsheet.export.to.mail.enabled";
    // This property is used to enable the bulk release deleting feature
    public static final String IS_BULK_RELEASE_DELETING_ENABLED = "bulk.release.deleting.enabled";
    // This property is used to disable the ISR generation in fossology process
    public static final String DISABLE_CLEARING_FOSSOLOGY_REPORT_DOWNLOAD = "disable.clearing.fossology.report.download";

    // This property enable force update feature
    public static final String IS_FORCE_UPDATE_ENABLED = "rest.force.update.enabled";

    // This property is used to control the user role for SBOM import and export
    public static final String SBOM_IMPORT_EXPORT_ACCESS_USER_ROLE = "sbom.import.export.access.usergroup";

    // This property is used to set the tool name in exported CycloneDx SBOM
    public static final String TOOL_NAME = "sw360.tool.name";
    // This property is used to set the tool vendor in exported CycloneDx SBOM
    public static final String TOOL_VENDOR = "sw360.tool.vendor";
    // This property is used to enable/disable the package portlet feature.
    public static final String IS_PACKAGE_PORTLET_ENABLED = "package.portlet.enabled";
    // This property is used to control the write access user role for Packages.
    public static final String PACKAGE_PORTLET_WRITE_ACCESS_USER_ROLE = "package.portlet.write.access.usergroup";

    public static final String IS_ADMIN_PRIVATE_ACCESS_ENABLED = "admin.private.project.access.enabled";

    public static final String SKIP_DOMAINS_FOR_VALID_SOURCE_CODE = "release.sourcecodeurl.skip.domains";

    private SW360ConfigKeys() {
    }
}
