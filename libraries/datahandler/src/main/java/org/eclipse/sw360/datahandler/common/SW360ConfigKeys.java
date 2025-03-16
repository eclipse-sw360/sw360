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

    // This property is used to find correlation key for combined CLI
    public static final String COMBINED_CLI_PARSER_EXTERNAL_ID_CORRELATION_KEY = "combined.cli.parser.external.id.correlation.key";

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
    // This property is used to create URLs in Doc reports
    public static final String RELEASE_FRIENDLY_URL = "release.friendly.url";

    public static final String IS_ADMIN_PRIVATE_ACCESS_ENABLED = "admin.private.project.access.enabled";

    public static final String SKIP_DOMAINS_FOR_VALID_SOURCE_CODE = "release.sourcecodeurl.skip.domains";

    //Properties used by the RepositoryURL class to handle VCS from SBOM
    public static final String VCS_HOSTS = "vcs.hosts";
    public static final String VCS_REDIRECTION_LIMIT = "vcs.redirection.limit";
    public static final String VCS_REDIRECTION_TIMEOUT_LIMIT = "vcs.redirection.timeout.limit";
    public static final String NON_PKG_MANAGED_COMPS_PROP = "non.pkg.managed.comps.prop";

    // Properties purely used by UI
    // This property is used in Project Administration
    public static final String UI_CLEARING_TEAMS = "ui.clearing.teams";
    // This property add "Unknown" team to the clearing teams list
    public static final String UI_CLEARING_TEAM_UNKNOWN_ENABLED = "ui.clearing.team.unknown.enabled";
    // This property is used to create Component Categories
    public static final String UI_COMPONENT_CATEGORIES = "ui.component.categories";
    // This property is used to create external keys for Components
    public static final String UI_COMPONENT_EXTERNALKEYS = "ui.component.externalkeys";
    // This property is used to create Roles for Components
    public static final String UI_CUSTOMMAP_COMPONENT_ROLES = "ui.custommap.component.roles";
    // This property is used to create Roles for Projects
    public static final String UI_CUSTOMMAP_PROJECT_ROLES = "ui.custommap.project.roles";
    // This property is used to create Roles for Releases
    public static final String UI_CUSTOMMAP_RELEASE_ROLES = "ui.custommap.release.roles";
    // This property is used to enable or disable the custom welcome page
    public static final String UI_CUSTOM_WELCOME_PAGE_GUIDELINE = "ui.custom.welcome.page.guideline";
    // This property is used to create Domains for Components
    public static final String UI_DOMAINS = "ui.domains";
    // This property is used to control enable or disable the licenseInfoToRelease button on project page.
    public static final String UI_ENABLE_ADD_LICENSE_INFO_TO_RELEASE_BUTTON = "ui.enable.add.license.info.to.release.button";
    // This property is used to indicate whether SVM tracking is enabled or not
    public static final String UI_ENABLE_SECURITY_VULNERABILITY_MONITORING = "ui.enable.security.vulnerability.monitoring";
    // This property is used to create Project Operating Systems
    public static final String UI_OPERATING_SYSTEMS = "ui.operating.systems";
    // This property is used to disable the Clearing Request feature for the projects based on project Business Unit (BU) / Group.
    // Add the list of BU for which you want to disable the Clearing Request feature.
    public static final String UI_ORG_ECLIPSE_SW360_DISABLE_CLEARING_REQUEST_FOR_PROJECT_GROUP = "ui.org.eclipse.sw360.disable.clearing.request.for.project.group";
    // This property is used to create Project Programming Languages
    public static final String UI_PROGRAMMING_LANGUAGES = "ui.programming.languages";
    // This property is used to create Project External Keys
    public static final String UI_PROJECT_EXTERNALKEYS = "ui.project.externalkeys";
    // This property is used to create Project External URLs
    public static final String UI_PROJECT_EXTERNALURLS = "ui.project.externalurls";
    // This property is used to create Project Tags
    public static final String UI_PROJECT_TAG = "ui.project.tag";
    // This property is used to create Project Types
    public static final String UI_PROJECT_TYPE = "ui.project.type";
    // This property is used to create Release External Keys
    public static final String UI_RELEASE_EXTERNALKEYS = "ui.release.externalkeys";
    // This property is used to enable or disable API Token Generation feature in UI
    public static final String UI_REST_APITOKEN_GENERATOR_ENABLE = "ui.rest.apitoken.generator.enable";
    // This property is used to create Release Software Platforms
    public static final String UI_SOFTWARE_PLATFORMS = "ui.software.platforms";
    // This property is used to create State of Projects
    public static final String UI_STATE = "ui.state";

    private SW360ConfigKeys() {
    }
}
