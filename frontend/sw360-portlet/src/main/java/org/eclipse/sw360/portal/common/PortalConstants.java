/*
 * Copyright Siemens AG, 2013-2019. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.common;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;

import java.util.Properties;
import java.util.Set;

/**
 * Constants definitions for portlets
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author gerrit.grenzebach@tngtech.com
 * @author andreas.reichel@tngtech.com
 * @author stefan.jaeger@evosoft.com
 * @author alex.borodin@evosoft.com
 */

public class PortalConstants {
    public static final String PROPERTIES_FILE_PATH = "/sw360.properties";
    public static final String PROGRAMMING_LANGUAGES;
    public static final Set<String> DOMAIN;
    public static final String SOFTWARE_PLATFORMS;
    public static final String OPERATING_SYSTEMS;
    public static final Set<String> SET_CLEARING_TEAMS_STRING;
    public static final String LICENSE_IDENTIFIERS;
    public static final String PREFERRED_COUNTRY_CODES;
    public static final Boolean MAINLINE_STATE_ENABLED_FOR_USER;
    public static final Boolean IS_CLEARING_TEAM_UNKNOWN_ENABLED;
    public static final Set<String> PROJECT_OBLIGATIONS_ACTION_SET;
    public static final Boolean IS_PROJECT_OBLIGATIONS_ENABLED;
    public static final Boolean CUSTOM_WELCOME_PAGE_GUIDELINE;
    public static final UserGroup USER_ROLE_ALLOWED_TO_MERGE_OR_SPLIT_COMPONENT;
    public static final String CLEARING_REPORT_TEMPLATE_TO_FILENAMEMAPPING;
    public static final String CLEARING_REPORT_TEMPLATE_FORMAT;
    public static final String DISABLE_CLEARING_REQUEST_FOR_PROJECT_WITH_GROUPS;
    public static final Boolean SEND_PROJECT_SPREADSHEET_EXPORT_TO_MAIL_ENABLED;
    public static final Boolean SEND_COMPONENT_SPREADSHEET_EXPORT_TO_MAIL_ENABLED;
    public static final String LOAD_OPEN_MODERATION_REQUEST = "loadOpenModerationRequest";
    public static final String LOAD_CLOSED_MODERATION_REQUEST = "loadClosedModerationRequest";
    public static final String LICENSE_INFO_HEADER_TEXT_FILE_NAME_BY_PROJECT_GROUP;
    public static final String DEFAULT_COUNTRY_NAME;

    // DO NOT CHANGE THIS UNLESS YOU KNOW WHAT YOU ARE DOING !!!
    // - friendly url mapping files must be changed
    // - configured portlets in liferay would not be found anymore
    public static final String PORTLET_NAME_PREFIX = "sw360_portlet_";

    //! Role names
    // Keep this in sync with configuration/portal-ext.properties#system.site.roles
    public static final String ROLENAME_ADMIN = "Administrator";
    public static final String ROLENAME_CLEARING_ADMIN = "Clearing Admin";
    public static final String ROLENAME_CLEARING_EXPERT = "Clearing Expert";
    public static final String ROLENAME_ECC_ADMIN = "ECC Admin";
    public static final String ROLENAME_SECURITY_ADMIN = "Security Admin";
    public static final String ROLENAME_SW360_ADMIN = "SW360 Admin";

    //! Key for Exact Match CheckBox
    public static final String EXACT_MATCH_CHECKBOX = "exactMatchCheckBox";

    //! Standard keys for Lists and their size
    public static final String KEY_SUMMARY = "documents";
    public static final String RDF_FILE_EXTENSION = ".rdf";
    public static final String XML_FILE_EXTENSION = ".xml";

    public static final String KEY_LIST_SIZE = "documentssize";

    public static final String NO_FILTER = "noFilter";
    public static final String KEY_SEARCH_TEXT = "searchtext";
    public static final String KEY_SEARCH_FILTER_TEXT = "searchfilter";
    public static final String SUBMIT_SEARCH = "submitSearch";
    public static final String DOCUMENT_ID = "documentID";
    public static final String PAGENAME = "pagename";
    public static final String PAGENAME_DETAIL = "detail";
    public static final String PAGENAME_VIEW = "view";
    public static final String PAGENAME_IMPORT = "import";
    public static final String PAGENAME_ADD = "add";
    public static final String PAGENAME_EDIT = "edit";
    public static final String PAGENAME_ACTION = "action";
    public static final String PAGENAME_DUPLICATE = "duplicate";
    public static final String SELECTED_TAB = "selectedTab";
    public static final String IS_USER_ALLOWED_TO_MERGE = "isUserAllowedToMerge";
    public static final String DOCUMENT_TYPE = "documentType";
    public static final String IS_SPDX_DOCUMENT = "isSpdxDocument";
    public static final String VIEW_SIZE = "viewSize";
    public static final String TOTAL_ROWS = "totalRows";
    public static final String RESULT = "result";
    public static final String DATE_RANGE = "dateRange";
    public static final String END_DATE = "endDate";
    public static final String EPOCH_DATE = "1970-01-01";
    public static final String TO = "TO";

    public static final String IS_USER_AT_LEAST_CLEARING_ADMIN = "isUserAtLeastClearingAdmin";
    public static final String IS_USER_AT_LEAST_ECC_ADMIN = "isUserAtLeastECCAdmin";
    public static final String IS_USER_ADMIN = "isUserAdmin";
    public static final String IS_CLEARING_EXPERT = "isClearingExpert";
    public static final String IS_PROJECT_MEMBER = "isProjectMember";
    public static final String IS_ERROR_IN_UPDATE_OR_CREATE = "isErrorInUpdateOrCreate";

    //! Specialized keys for packages
    public static final String PACKAGE = "Package";
    public static final String PKG = "pkg";
    public static final String PACKAGES = "packages";
    public static final String PACKAGES_PORTLET_NAME = PORTLET_NAME_PREFIX + PACKAGES;
    public static final String PACKAGE_ID = "packageId";
    public static final String PACKAGE_IDS = "packageIds";
    public static final String IMPORT_CYCLONEDX_SBOM = "importCycloneDxSBoM";
    public static final String PACKAGE_LIST = "packageList";
    public static final String LOAD_PACKAGE_LIST = "loadPackageList";
    public static final String DELETE_PACKAGE = "delete_package";
    public static final String PAGENAME_PACKAGE_DETAIL = "detailPackage";
    public static final String PAGENAME_EDIT_PACKAGE = "editPackage";
    public static final String LOAD_RELEASE_INFO = "loadReleaseInfo";
    public static final String ORPHAN_PACKAGE_CHECKBOX = "orphanPackageCheckBox";

    //! Specialized keys for licenses
    public static final String LICENSES_PORTLET_NAME = PORTLET_NAME_PREFIX + "licenses";
    public static final String KEY_LICENSE_DETAIL = "licenseDetail";
    public static final String KEY_OBLIGATION_LIST = "obligationList";
    public static final String LICENSE_ID = "licenseid";
    public static final String LICENSE_TEXT = "licenseText";
    public static final String LICENSE_LIST = "licenseList";
    public static final String ACTUAL_LICENSE = "actual_license";
    public static final String ADDED_TODOS_FROM_MODERATION_REQUEST = "added_obligations_from_moderation_request";
    public static final String DB_TODOS_FROM_MODERATION_REQUEST = "db_obligations_from_moderation_request";
    public static final String MODERATION_LICENSE_DETAIL = "moderationLicenseDetail";
    public static final String LICENSE_TYPE_CHOICE = "licenseTypeChoice";
    public static final String LICENSE_TYPE_GLOBAL = "global";
    public static final String LICENSE_TYPE_OTHERS = "Others";
    public static final String LICENSE_IDS = "licenseIds";
    public static final String MAIN_LICENSE_FILES = "LICENSE.*|License.*|license|license.txt|license.html|COPYING.*|Copying.*|copying|copying.txt|copying.html";

    //! Specialized keys for moderation
    public static final String MODERATION_PORTLET_NAME = PORTLET_NAME_PREFIX + "moderations";
    public static final String MODERATION_ID = "moderationId";
    public static final String MODERATION_REQUEST = "moderationRequest";
    public static final String MODERATION_REQUESTS = "moderationRequests";
    public static final String CLOSED_MODERATION_REQUESTS = "closedModerationRequests";
    public static final String OPEN_MODERATION_REQUESTS = "openModerationRequests";
    public static final String DELETE_MODERATION_REQUEST = "deleteModerationRequest";
    public static final String MODERATION_ACTIONS_ALLOWED = "moderationAllowed";
    public static final String MODERATION_REQUESTING_USER_DEPARTMENTS = "requestingUserDepartments";

    //! Specialized keys for clearing
    public static final String CLEARING_REQUEST = "clearingRequest";
    public static final String CLEARING_REQUESTS = "clearingRequests";
    public static final String CLEARING_REQUEST_ID = "clearingId";
    public static final String CLOSED_CLEARING_REQUESTS = "closedClearingRequests";
    public static final String CREATE_CLEARING_REQUEST = "create_clearing_request";
    public static final String VIEW_CLEARING_REQUEST = "view_clearing_request";
    public static final String AGREED_CLEARING_DATE = "agreedClearingDate";
    public static final String PAGENAME_DETAIL_CLEARING_REQUEST = "detailClearingRequest";
    public static final String PAGENAME_EDIT_CLEARING_REQUEST = "editClearingRequest";
    public static final String ADD_COMMENT = "addComment";
    public static final String CLEARING_REQUEST_COMMENT = "clearingRequestComment";
    public static final String RE_OPEN_REQUEST = "reOpenRequest";
    public static final String LOAD_PROJECT_INFO = "loadProjectInfo";
    public static final String APPROVED_RELEASE_COUNT = "approvedReleaseCount";
    public static final String CRITICAL_CR_COUNT = "criticalCrCount";
    public static final String IS_CLEARING_REQUEST_DISABLED_FOR_PROJECT_BU = "isCrDisabledForProjectBU";

    //! Specialized keys for components
    public static final String COMPONENT_PORTLET_NAME = PORTLET_NAME_PREFIX + "components";
    public static final String COMPONENT_ID = "componentid";
    public static final String COMPONENT = "component";
    public static final String COMPONENT_PURL = "componentpurl";
    public static final String COMPONENT_NAME = "componentname";
    public static final String ACTUAL_COMPONENT = "actual_component";
    public static final String COMPONENT_LIST = "componentList";
    public static final String COMPONENT_TYPE_LIST = "componentTypeList";
    public static final String COMPONENT_CATEGORIES;
    public static final String COMPONENT_ROLES;
    public static final String PAGENAME_MERGE_COMPONENT = "mergeComponent";
    public static final String PAGENAME_SPLIT_COMPONENT = "splitComponent";
    public static final String COMPONENT_SELECTION = "componentSelection";
    public static final String COMPONENT_SOURCE_ID = "componentSourceId";
    public static final String COMPONENT_TARGET_ID = "componentTargetId";
    public static final Set<String> COMPONENT_EXTERNAL_ID_KEYS;
    public static final String SOURCE_COMPONENT = "srcComponent";
    public static final String TARGET_COMPONENT = "targetComponent";
    public static final String COMPONENT_VISIBILITY_RESTRICTION = "componentVisibilityRestriction";

    //! Specialized keys for releases
    public static final String RELEASE_ID = "releaseId";
    public static final String RELEASE_IDS = "releaseIds";
    public static final String CLEARING_TEAM = "clearingTeam";
    public static final String RELEASE = "release";
    public static final String ACTUAL_RELEASE = "actual_release";
    public static final String PAGENAME_RELEASE_DETAIL = "detailRelease";
    public static final String PAGENAME_EDIT_RELEASE = "editRelease";
    public static final String PAGENAME_DUPLICATE_RELEASE = "duplicateRelease";
    public static final String PAGENAME_DELETE_BULK_RELEASE = "deleteBulkRelease";
    public static final String PAGENAME_DELETE_BULK_RELEASE_PREVIEW = "deleteBulkReleasePreview";
    public static final String RELEASE_ROLES;
    public static final String RELEASE_EXTERNAL_IDS;
    public static final Set<String> RELEASE_EXTERNAL_ID_KEYS;
    public static final String RELEASE_LINK_TO_PROJECT = "releaseLinkToProject";
    public static final String PAGENAME_MERGE_RELEASE = "mergeRelease";
    public static final String RELEASE_SELECTION = "releaseSelection";
    public static final String RELEASE_SOURCE_ID = "releaseSourceId";
    public static final String RELEASE_TARGET_ID = "releaseTargetId";
    public static final String EVALUATE_CLI_ATTACHMENTS = "evaluateCLIAttachments";
    public static final String BULK_OPERATION_RESULT_LIST = "bulkOperationResultList";
    public static final String BULK_RELEASE_DELETING = "bulkReleaseDeleting";

    //! Specialized keys for vendors
    public static final String VENDOR_PORTLET_NAME = PORTLET_NAME_PREFIX + "vendors";
    public static final String VENDOR = "vendor";
    public static final String VENDOR_ID = "vendorId";
    public static final String VENDOR_LIST = "vendorList";
    public static final String VENDOR_SELECTION = "vendorSelection";
    public static final String VENDOR_SOURCE_ID = "vendorSourceId";
    public static final String VENDOR_TARGET_ID = "vendorTargetId";
    public static final String PAGENAME_MERGE_VENDOR = "mergeVendor";

    //! Specialized keys for obligations
    public static final String TODO_LIST = "obligList";
    public static final String TODO_ID = "obligId";

    //! Specialized keys for obligations
    public static final String OBLIGATION_ID = "obligationId";
    public static final String OBLIGATION_TOPIC = "obligationTopic";
    public static final String OBLIGATION_ACTION = "obligationAction";
    public static final String OBLIGATION_STATUS = "obligationStatus";
    public static final String OBLIGATION_COMMENT = "obligationComment";
    public static final String OBLIGATION_DATA = "obligationData";
    public static final String DELETE_ALL_ORPHAN_OBLIGATIONS = "deleteAllOrphanObligations";
    public static final String LICENSE_OBLIGATION_DATA = "licenseObligationData";
    public static final String LOAD_OBLIGATIONS_VIEW = "load_obligations_view";
    public static final String LOAD_OBLIGATIONS_EDIT = "load_obligations_edit";
    public static final String LOAD_LICENSE_OBLIGATIONS = "load_license_obligations";
    public static final String UNUSED_RELEASES = "unusedReleases";
    public static final String OBLIGATION_EDIT = "obligationEdit";
    public static final String OBLIGATION_CHANGELOG = "obligation_changelog";

    //! Specialized keys for license types
    public static final String LICENSE_TYPE_LIST = "licenseTypeList";

    //! Specialized keys for attachments
    public static final String ATTACHMENTS = "attachments";
    public static final String ATTACHMENT_NAME = "attachmentName";
    public static final String SPDX_ATTACHMENTS = "spdxAttachments";
    public static final String ADDED_ATTACHMENTS = "added_attachments";
    public static final String REMOVED_ATTACHMENTS = "removed_attachments";
    public static final String ATTACHMENT_ID = "attachmentId";
    public static final String ATTACHMENT_ID_TO_FILENAMES = "attachmentIdToFileNames[]";
    public static final String SHOW_ATTACHMENT_MISSING_ERROR = "showSessionError";
    public static final String ATTACHMENT_CONTENT_ID = "attachmentContentId";
    public static final String ALL_ATTACHMENTS = "all_attachments";
    public static final String CONTEXT_TYPE = "context_type";
    public static final String CONTEXT_ID = "context_id";
    public static final String ATTACHMENT_USAGE_COUNT_MAP = "attachmenUsageCountMap";
    public static final String ATTACHMENT_USAGES = "attachmentUsages";
    public static final String ATTACHMENT_USAGES_RESTRICTED_COUNTS = "attachmentUsagesRestrictedCounts";
    public static final String SPDX_LICENSE_INFO = "spdxLicenseInfo";
    public static final String SELECTED_ATTACHMENTS_WITH_FULL_PATH = "selectedAttachmentIdsWithFullPath[]";
    public static final String INCLUDE_CONCLUDED_LICENSE = "includeConcludedLicense";
    public static final String INCLUDE_CONCLUDED_LICENSE_SHADOWS = "includeConcludedLicenseShadows";
    public static final String ENABLE_CONCLUDED_LICENSE = "enableConcludedLicense";

    // ! Specialized keys for changelog
    public static final String LOAD_CHANGE_LOGS = "load_change_logs";
    public static final String VIEW_CHANGE_LOGS = "view_change_logs";

    //! Specialized keys for projects
    public static final String PROJECTS = "projects";
    public static final String PROJECT_PORTLET_NAME = PORTLET_NAME_PREFIX + PROJECTS;
    public static final String PROJECT_BDPIMPORT_PORTLET_NAME = PORTLET_NAME_PREFIX + "projectbdpimport";
    public static final String PROJECT_WSIMPORT_PORTLET_NAME = PORTLET_NAME_PREFIX + "projectwsimport";
    public static final String PROJECT_ID = "projectid";
    public static final String ACTUAL_PROJECT_ID = "actualprojectid";
    public static final String PROJECT_LINK_TO_PROJECT = "projectLinkToProject";
    public static final String LINKED_PROJECT_ID = "linkedProjectId";
    public static final String PROJECT = "project";
    public static final String ACTUAL_PROJECT = "actual_project";
    public static final String USING_PROJECTS = "usingProjects";
    public static final String USING_COMPONENTS = "usingComponents";
    public static final String USING_RELEASES = "usingReleases";
    public static final String ALL_USING_PROJECTS_COUNT = "allUsingProjectsCount";
    public static final String USING_RELEASE = "usingRelease";
    public static final String PROJECT_LIST = "projectList";
    public static final String ALL_SUB_PROJECT_LINK = "allSubProjectLink";
    public static final String RELEASE_LIST = "releaseList";
    public static final String TOTAL_INACCESSIBLE_ROWS = "totalInaccessibleRows";
    public static final String PROJECT_SEARCH = "projectSearch";
    public static final String RELEASE_SEARCH = "releaseSearch";
    public static final String PACKAGE_SEARCH = "packageSearch";
    public static final String RELEASE_SEARCH_BY_VENDOR = "releaseSearchByVendor";
    public static final String OBLIGATION_ELEMENT_SEARCH = "obligationElementSearch";
    public static final String OBLIGATION_ELEMENT_ID = "obligationElementId";
    public static final String LOAD_LINKED_PACKAGES = "loadLinkedPackages";
    public static final String LOAD_SBOM_IMPORT_INFO = "loadSbomImportInfo";

    public static final String RELEASE_LIST_FROM_LINKED_PROJECTS = "releaseListFromLinkedProjects";
    public static final String STATE;
    public static final String PROJECT_TYPE;
    public static final String EXTENDED_EXCEL_EXPORT = "extendedExcelExport";
    public static final String PROJECT_WITH_SUBPROJECT = "projectWithSubproject";
    public static final String PREPARE_LICENSEINFO_OBL_TAB = "prepareLicenseinfoOblTab";
    public static final String PROJECT_NOT_FOUND = "projectNotFound";
    public static final String PAGENAME_LICENSE_INFO = "generateLicenseInfo";
    public static final String PAGENAME_SOURCE_CODE_BUNDLE = "generateSourceCodeBundle";
    public static final String PROJECT_ROLES;
    public static final String DEFAULT_LICENSE_INFO_HEADER_TEXT = "defaultLicenseInfoHeaderText";
    public static final String DEFAULT_OBLIGATIONS_TEXT = "defaultObligationsText";
    public static final String DEFAULT_LICENSE_INFO_HEADER_TEXT_FOR_DISPALY = "--default text--";
    public static final String DEFAULT_OBLIGATIONS_TEXT_FOR_DISPALY = "--default text--";
    public static final String PROJECT_OBLIGATIONS = "projectLevelObligations";
    public static final String ORGANISATION_OBLIGATIONS = "organisationLevelObligations";
    public static final String COMPONENT_OBLIGATIONS = "componentLevelObligations";
    public static final String LICENSE_OBLIGATIONS = "licenseLevelObligations";
    public static final Set<String> PROJECT_EXTERNAL_ID_KEYS;
    public static final Set<String> PROJECT_EXTERNAL_URL_KEYS;
    public static final String PROJECT_SELECTED_ATTACHMENT_USAGES = "selectedAttachmentUsages";
    public static final String PROJECT_SELECTED_ATTACHMENT_USAGES_SHADOWS = "selectedAttachmentUsagesShadows";
    public static final String LICENSE_INFO_ATTACHMENT_USAGES = "licInfoAttUsages";
    public static final String SOURCE_CODE_ATTACHMENT_USAGES = "sourceAttUsages";
    public static final String MANUAL_ATTACHMENT_USAGES = "manualAttUsages";
    public static final String PROJECT_PATH = "projectPath";
    public static final String PROJECT_PATHS = "projectPaths";
    public static final String PARENT_PROJECT_PATH = "parentProjectPath";
    public static final String SOURCE_PROJECT_ID = "sourceProjectId";
    public static final String PROJECT_OBLIGATIONS_INFO_BY_RELEASE = "projectObligationsInfoByRelease";
    public static final String LINKED_OBLIGATIONS = "linkedObligations";
    public static final String APPROVED_OBLIGATIONS_COUNT = "approvedObligationsCount";
    public static final String OBLIGATION_FROM_README_OSS = "obligationFromReadmeOSS";
    public static final String EXCLUDED_RELEASES = "excludedReleases";
    public static final String RELATIONSHIPS = "relations";
    public static final String PROJECT_RELEASE_TO_RELATION = "projectReleaseToRelation";
    public static final String PROJECT_USED_RELEASE_RELATIONS = "usedProjectReleaseRelations";
    public static final String SELECTED_PROJECT_RELEASE_RELATIONS = "selectedProjectReleaseRelations";
    public static final String LINKED_PROJECT_RELATION = "linkedProjectRelation";
    public static final String USED_LINKED_PROJECT_RELATION = "usedLinkedProjectRelation";
    public static final String SELECTED_PROJECT_RELATIONS = "selectedProjectRelations";
    public static final String IS_LINKED_PROJECT_PRESENT = "isLinkedProjectPresent";
    public static final String PROJECT_URL = "projectUrl";
    public static final String PROECT_MODERATION_REQUEST = "projModReq";


    public static final String FOSSOLOGY_PORTLET_NAME = PORTLET_NAME_PREFIX + "fossology";
    public static final String USER_LIST = "userList";
    public static final String SECONDARY_GROUPS_LIST = "secGrpsKeys";
    public static final String EDIT_SECONDARY_GROUP_FOR_USER = "editSecondaryGroupForUser";
    public static final String SECONDARY_ROLES_OPTIONS = "secondaryRolesOptions";
    public static final String MISSING_USER_LIST = "missingUserList";
    public static final String GET_CLEARING_STATE_SUMMARY = "getClearingStateSummary";
    public static final String PROJECT_LINK_TABLE_MODE = "projectLinkTableMode";
    public static final String PROJECT_LINK_TABLE_MODE_LICENSE_INFO = "licenseInfo";
    public static final String PROJECT_LINK_TABLE_MODE_SOURCE_BUNDLE = "sourceBundle";
    public static final String COUCH_DB_USER_COUNT = "couchDbUserCount";

    //! Specialized keys for database Sanitation
    public static final String DUPLICATE_RELEASES = "duplicateReleases";
    public static final String DUPLICATE_RELEASE_SOURCES = "duplicateReleaseSources";
    public static final String DUPLICATE_COMPONENTS = "duplicateComponents";
    public static final String DUPLICATE_PROJECTS = "duplicateProjects";
    public static final String ACTION_DELETE_ALL_LICENSE_INFORMATION = "deleteAllLicenseInformation";
    public static final String ACTION_IMPORT_SPDX_LICENSE_INFORMATION = "importSpdxLicenseInformation";
    public static final String ACTION_IMPORT_OSADL_LICENSE_INFORMATION = "importOSADLLicenseInformation";


    //! Specialized keys for vulnerability management
    public static final String VULNERABILITIES_PORTLET_NAME = PORTLET_NAME_PREFIX + "vulnerabilitites";
    public static final String VULNERABILITY = "vulnerability";
    public static final String VULNERABILITY_LIST = "vulnerabilityList";
    public static final String TOTAL_VULNERABILITY_COUNT = "totalVulnerabilityCount";
    public static final String VULNERABILITY_RATINGS = "vulnerabilityRatings";
    public static final String VULNERABILITY_ACTIONS = "vulnerabilityActions";
    public static final String VULNERABILITY_ID = "vulnerabilityId";
    public static final String VULNERABILITY_IDS = "vulnerabilityIds";
    public static final String VULNERABILITY_RATING_VALUE = "vulnerabilityRatingValue";
    public static final String VULNERABILITY_RATING_COMMENT = "vulnerabilityRatingComment";
    public static final String VULNERABILITY_RATING_ACTION = "vulnerabilityRatingAction";
    public static final String NUMBER_OF_VULNERABILITIES = "numberOfVulnerabilities";
    public static final String NUMBER_OF_UNCHECKED_VULNERABILITIES = "numberOfUncheckedVulnerabilities";
    public static final String NUMBER_OF_INCORRECT_VULNERABILITIES = "numberOfIncorrectVulnerabilities";
    public static final String NUMBER_OF_CHECKED_OR_UNCHECKED_VULNERABILITIES = "numberOfCheckedOrUncheckedVulnerabilities";
    public static final String VULNERABILITY_CHECKSTATUS_TOOLTIPS = "vulnerabilityCheckstatusTooltips";
    public static final String VULNERABILITY_VERIFICATION_VALUE = "vulnerabilityVerificationValue";
    public static final String VULNERABILITY_VERIFICATION_COMMENT = "vulnerabilityVerificationComment";
    public static final String VULNERABILITY_VERIFICATION_EDITABLE = "vulnerabilityVerificationEditable";
    public static final String VULNERABILITY_VERIFICATION_TOOLTIPS = "vulnerabilityVerificationTooltips";
    public static final String VULNERABILITY_VERIFICATIONS = "vulnerabilityVerifications";
    public static final String VULNERABILITY_MATCHED_BY_HISTOGRAM = "vulnerabilityMatchedByHistogram";
    public static final String VULNERABILITY_IMPACT_KEY = "impactKey";
    public static final String VULNERABILITY_IMPACT_VALUE = "impactValue";
    public static final String VULNERABILITY_ACCESS_KEY = "accessKey";
    public static final String VULNERABILITY_ACCESS_VALUE = "accessValue";
    public static final String VULNERABILITY_CONFIG_KEY = "configKey";
    public static final String VULNERABILITY_CONFIG_VALUE = "configValue";
    public static final String VULNERABILITY_ADVISORY_VENDOR = "advisoryVendor";
    public static final String VULNERABILITY_ADVISORY_NAME = "advisoryName";
    public static final String VULNERABILITY_ADVISORY_URL = "advisoryUrl";
    public static final String VULNERABILITY_CVE_YEAR= "cveYear";
    public static final String VULNERABILITY_CVE_NUMBER= "cveNumber";
    public static final String VULNERABILITY_EXTERNAL_ID= "vulnerabilityExternalId";
    public static final String CVSS_DATE= "cvssDate";
    public static final String CVSS_TIME= "cvssTime";
    public static final String EXTERNAL_UPDATE_DATE= "externalUpdateDate";
    public static final String EXTERNAL_UPDATE_TIME= "externalUpdateTime";
    public static final String FIND_BY_EXTERNAL_ID = "findByExternalId";
    public static final String PUBLISH_DATE = "publishDate";
    public static final String PUBLISH_TIME = "publishTime";
    public static final String VM_MESSAGE = "vmMessage";
    public static final String VM_PROCESS = "vmProcess";
    public static final String VM_PROCESS_ID = "vmProcessId";
    public static final String VM_PROCESS_LIST = "vmProcessList";
    public static final String VM_MATCH_ID = "vmMatchId";
    public static final String VM_MATCH_STATE = "vmMatchState";
    public static final String VM_MATCH_LIST = "vmMatchList";

    //! Specialized keys for account sign-up
    public static final String PASSWORD = "password";
    public static final String PASSWORD_REPEAT = "password_repeat";
    public static final String USER_GROUPS = "usergroups";
    public static final String USER = "newuser";
    public static final String ORGANIZATIONS = "organizations";
    public static final String PAGENAME_SUCCESS = "success";

    //! Specialized keys for users
    public static final String CUSTOM_FIELD_PROJECT_GROUP_FILTER = "ProjectGroupFilter";
    public static final String CUSTOM_FIELD_COMPONENTS_VIEW_SIZE = "ComponentsViewSize";
    public static final String CUSTOM_FIELD_VULNERABILITIES_VIEW_SIZE = "VulnerabilitiesViewSize";
    public static final String CUSTOM_FIELD_PREFERRED_CLEARING_DATE_LIMIT = "PreferredClearingDateLimit";
    public static final String CUSTOM_FIELD_BANNER_MESSAGE = "BannerMessage";

    //! Specialized keys for scheduling
    public static final String CVESEARCH_IS_SCHEDULED = "cveSearchIsScheduled";
    public static final String ANY_SERVICE_IS_SCHEDULED = "anyServiceIsScheduled";
    public static final String CVESEARCH_OFFSET = "cvesearchOffset";
    public static final String CVESEARCH_INTERVAL = "cvesearchInterval";
    public static final String CVESEARCH_NEXT_SYNC = "cvesearchNextSync";

    public static final String SVMSYNC_IS_SCHEDULED = "svmSyncIsScheduled";
    public static final String SVMSYNC_OFFSET = "svmSyncOffset";
    public static final String SVMSYNC_INTERVAL = "svmSyncInterval";
    public static final String SVMSYNC_NEXT_SYNC = "svmSyncNextSync";
    public static final String SVMMATCH_IS_SCHEDULED = "svmMatchIsScheduled";
    public static final String SVMMATCH_OFFSET = "svmMatchOffset";
    public static final String SVMMATCH_INTERVAL = "svmMatchInterval";
    public static final String SVMMATCH_NEXT_SYNC = "svmMatchNextSync";
    public static final String SVM_LIST_UPDATE_IS_SCHEDULED = "svmListUpdateIsScheduled";
    public static final String SVM_LIST_UPDATE_OFFSET = "svmListUpdateOffset";
    public static final String SVM_LIST_UPDATE_INTERVAL = "svmListUpdateInterval";
    public static final String SVM_LIST_UPDATE_NEXT_SYNC = "svmListUpdateNextSync";
    public static final String SVM_TRACKING_FEEDBACK_IS_SCHEDULED = "trackingFeedbackIsScheduled";
    public static final String SVM_TRACKING_FEEDBACK_OFFSET = "trackingFeedbackOffset";
    public static final String SVM_TRACKING_FEEDBACK_INTERVAL = "trackingFeedbackInterval";
    public static final String SVM_TRACKING_FEEDBACK_NEXT_SYNC = "trackingFeedbackNextSync";
    public static final String SVM_MONITORINGLIST_ID = "svmMonitoringListID";
    public static final String DELETE_ATTACHMENT_IS_SCHEDULED = "deleteAttachmentIsScheduled";
    public static final String DELETE_ATTACHMENT_OFFSET = "deleteAttachmentOffset";
    public static final String DELETE_ATTACHMENT_INTERVAL = "deleteAttachmentInterval";
    public static final String DELETE_ATTACHMENT_NEXT_SYNC = "deleteAttachmentNextSync";

    public static final String DEPARTMENT_IS_SCHEDULED = "departmentIsScheduled";
    public static final String DEPARTMENT_OFFSET = "departmentOffset";
    public static final String DEPARTMENT_INTERVAL = "departmentInterval";
    public static final String DEPARTMENT_NEXT_SYNC = "departmentNextSync";
    public static final String DEPARTMENT_LIST = "departmentList";

    public static final String DEPARTMENT_KEY = "departmentKey";
    public static final String EMAIL_BY_DEPARTMENT_JSON = "emailByDepartment";
    public static final String EMAIL_OTHER_DEPARTMENT_JSON = "emailOtherDepartment";
    public static final String DEPARTMENT_ROLE = "departmentRole";

    public static final String DELETE_LIST_EMAIL = "deleteEmail";
    public static final String ADD_LIST_EMAIL = "listEmail";

    public static final String REMOVE_DEPARTMENT_BY_EMAIL = "remove_department";
    public static final String DEPARTMENT_URL = "departmentUrl";
    public static final String LIST_CONTENT_FILE_LOG = "listContentFileLog";
    public static final String LAST_FILE_NAME = "lastFileName";
    public static final String LAST_RUNNING_TIME = "lastRunningTime";
    public static final String PATH_CONFIG_FOLDER_DEPARTMENT = "pathConfigFolderDepartment";
    public static final String IMPORT_DEPARTMENT_MANUALLY = "importdepartmentmanually";

    //! Specialized keys for licenseInfo
    public static final String LICENSE_INFO_OUTPUT_FORMATS = "licenseInfoOutputFormats";
    public static final String LICENSE_INFO_SELECTED_OUTPUT_FORMAT = "licenseInfoSelectedOutputFormat";
    public static final String LICENSE_INFO_RELEASE_TO_ATTACHMENT = "licenseInfoAttachmentSelected";
    public static final String LICENSE_INFO_EMPTY_FILE = "isEmptyFile";
    public static final String SW360_USER = "sw360User";

    //! Specialized keys for obligation node
    public static final String OBLIGATION_NODE_LIST = "obligationNodeList";

    //! Specialized keys for obligation element
    public static final String OBLIGATION_ELEMENT_LIST = "obligationElementList";

    //! Serve resource generic keywords
    public static final String ACTION = "action";
    public static final String ACTION_CANCEL = "action_cancel";
    public static final String ACTION_ACCEPT = "action_accept";
    public static final String ACTION_POSTPONE = "action_postpone";
    public static final String ACTION_DECLINE = "action_decline";
    public static final String ACTION_REMOVEME = "action_removeme";
    public static final String ACTION_RENDER_NEXT_AFTER_UNSUBSCRIBE = "action_render_next";
    public static final String MODERATION_REQUEST_COMMENT = "moderation_request_comment";
    public static final String MODERATION_DECISION_COMMENT = "moderation_decision_comment";
    public static final String WHAT = "what";
    public static final String WHERE = "where";
    public static final String WHERE_ARRAY = "where[]";
    public static final String HOW = "how";

    //! Keys for ECC
    public static final String ECC_PORTLET_NAME = PORTLET_NAME_PREFIX + "ecc";

    //! Keys for Search
    public static final String TYPE_MASK = "typeMask";
    public static final String SEARCH_PORTLET_NAME = PORTLET_NAME_PREFIX + "search";
    public static final String IS_SEARCH_TRUNCATED = "isSearchTruncated";

    //! Keys for Preferences
    public static final String PREFERENCES_PORTLET_NAME = PORTLET_NAME_PREFIX + "preferences";

    //! Keys for Admin portlets
    public static final String ADMIN_PORTLET_NAME = PORTLET_NAME_PREFIX + "admin";
    public static final String ATTACHMENT_CLEANUP_PORTLET_NAME = PORTLET_NAME_PREFIX + "attachmentcleanup";
    public static final String BULK_RELEASE_EDIT_PORTLET_NAME = PORTLET_NAME_PREFIX + "bulkreleaseedit";
    public static final String IMPORT_EXPORT_PORTLET_NAME = PORTLET_NAME_PREFIX + "importexport";
    public static final String DATABASE_SANITATION_PORTLET_NAME = PORTLET_NAME_PREFIX + "databasesanitation";
    public static final String LICENSE_ADMIN_PORTLET_NAME = PORTLET_NAME_PREFIX + "licenseadmin";
    public static final String SCHEDULE_ADMIN_PORTLET_NAME = PORTLET_NAME_PREFIX + "scheduleadmin";
    public static final String USER_ADMIN_PORTLET_NAME = PORTLET_NAME_PREFIX + "useradmin";
    public static final String TODOS_PORTLET_NAME = PORTLET_NAME_PREFIX + "todos";
    public static final String OAUTH_CLIENT_PORTLET_NAME = PORTLET_NAME_PREFIX + "oauthclient";
    public static final String LICENSE_TYPE_PORTLET_NAME = PORTLET_NAME_PREFIX + "licensetypes";
    public static final String DEPARTMENT_PORTLET_NAME = PORTLET_NAME_PREFIX + "department";

    //! Keys for Home portlets
    public static final String MY_COMPONENTS_PORTLET_NAME = PORTLET_NAME_PREFIX + "mycomponents";
    public static final String MY_PROJECTS_PORTLET_NAME = PORTLET_NAME_PREFIX + "myprojects";
    public static final String MY_SUBSCRIPTIONS_PORTLET_NAME = PORTLET_NAME_PREFIX + "mysubscriptions";
    public static final String MY_TASK_ASSIGNMENTS_PORTLET_NAME = PORTLET_NAME_PREFIX + "mytaskassignments";
    public static final String MY_TASK_SUBMISSIONS_PORTLET_NAME = PORTLET_NAME_PREFIX + "mytasksubmissions";
    public static final String RECENT_COMPONENTS_PORTLET_NAME = PORTLET_NAME_PREFIX + "recentcomponents";
    public static final String RECENT_RELEASES_PORTLET_NAME = PORTLET_NAME_PREFIX + "recentprojects";

    //! Keys for Welcome portlets
    public static final String SIGNUP_PORTLET_NAME = PORTLET_NAME_PREFIX + "signup";
    public static final String WELCOME_PORTLET_NAME = PORTLET_NAME_PREFIX + "welcome";

    //! Specialized keys for CSS-classes of project (clearing) state boxes
    public static final String PROJECT_STATE_ACTIVE__CSS      = "projectStateActive";
    public static final String PROJECT_STATE_INACTIVE__CSS    = "projectStateInactive";
    public static final String CLEARING_STATE_OPEN__CSS       = "clearingStateOpen";
    public static final String CLEARING_STATE_IUSA__CSS       = "clearingStateISUA";
    public static final String CLEARING_STATE_INPROGRESS__CSS = "clearingStateInProgress";
    public static final String CLEARING_STATE_CLOSED__CSS     = "clearingStateClosed";
    public static final String CLEARING_STATE_UNKNOWN__CSS    = "clearingStateUnknown";

    //! Specialized key for the tooltip CSS-class
    public static final String TOOLTIP_CLASS__CSS = "sw360-tt";

    //! Serve resource keywords

    //! Actions
    // attachment actions
    public static final String ATTACHMENT_PREFIX = "Attachment";
    public static final String ATTACHMENT_CANCEL = ATTACHMENT_PREFIX + "Cancel";
    public static final String ATTACHMENT_UPLOAD = ATTACHMENT_PREFIX + "Upload";
    public static final String ATTACHMENT_RESERVE_ID = ATTACHMENT_PREFIX + "Create";
    public static final String ATTACHMENT_LIST = ATTACHMENT_PREFIX + "List";
    public static final String ATTACHMENT_LINK_TO = ATTACHMENT_PREFIX + "LinkTo";
    public static final String ATTACHMENT_DOWNLOAD = ATTACHMENT_PREFIX + "Download";

    public static final String ATTACHMENT_DELETE_ON_CANCEL = "attachmentDeleteOnCancel";

    public static final String CLEANUP = "Cleanup";
    public static final String DUPLICATES = "Duplicates";
    public static final String DOWNLOAD = "Download";
    public static final String DOWNLOAD_SAMPLE = "DownloadSample";
    public static final String DOWNLOAD_ATTACHMENT_INFO = "DownloadAttachmentInfo";
    public static final String DOWNLOAD_SAMPLE_ATTACHMENT_INFO = "DownloadSampleAttachmentInfo";
    public static final String DOWNLOAD_SAMPLE_RELEASE_LINK_INFO = "DownloadSampleReleaseLinkInfo";
    public static final String DOWNLOAD_RELEASE_LINK_INFO = "DownloadReleaseLinkInfo";
    public static final String DOWNLOAD_LICENSE_BACKUP = "DownloadLicenseBackup";
    public static final String LOAD_SPDX_LICENSE_INFO = "LoadSpdxLicenseInfo";
    public static final String WRITE_SPDX_LICENSE_INFO_INTO_RELEASE = "WriteSpdxLicenseInfoIntoRelease";
    public static final String LOAD_ASSESSMENT_SUMMARY_INFO = "LoadAssessmentSummaryInfo";

    // linked projects and releases actions
    public static final String LINKED_OBJECTS_PREFIX = "load_linked_";
    public static final String LOAD_LINKED_PROJECTS_ROWS = LINKED_OBJECTS_PREFIX + "projects_rows";
    public static final String LOAD_LINKED_RELEASES_ROWS = LINKED_OBJECTS_PREFIX + "releases_rows";
    public static final String LOAD_ATTACHMENT_USAGES_ROWS = "load_attachment_usages_rows";
    public static final String SAVE_ATTACHMENT_USAGES = "save_attachment_usages";
    public static final String PARENT_BRANCH_ID = "parent_branch_id";
    public static final String PARENT_SCOPE_GROUP_ID = "parentScopeGroupId";
    public static final String ADD_LINKED_PACKAGES = "addLinkedPackages";
    public static final String VIEW_LINKED_PACKAGES = "viewLinkedPackages";

    // bom import / export
    public static final String PREPARE_IMPORT_BOM = "prepareImportBom";
    public static final String IMPORT_BOM = "importBom";
    public static final String IMPORT_BOM_AS_NEW = "importBomAsNew";
    public static final String NEW_RELEASE_VERSION = "newReleaseVersion";
    public static final String RDF_FILE_PATH = "rdfFilePath";
    public static final String BOM_TYPE = "bomType";
    public static final String EXPORT_SBOM = "exportSbom";
    public static final String SBOM_FROMAT = "sbomFormat";
    public static final String IS_SBOM_IMPORT_EXPORT_ACCESS_USER = "isSbomImportExportAccessUser";

    // project actions
    public static final String VIEW_LINKED_PROJECTS = "view_linked_projects";
    public static final String REMOVE_PROJECT = "remove_projects";
    public static final String LIST_NEW_LINKED_PROJECTS = "add_to_linked_projects";
    public static final String VIEW_LINKED_RELEASES = "view_linked_releases";
    public static final String LIST_NEW_LINKED_RELEASES = "add_to_linked_releases";
    public static final String DOWNLOAD_LICENSE_INFO = "DownloadLicenseInfo";
    public static final String DOWNLOAD_SOURCE_CODE_BUNDLE = "DownloadSourceCodeBundle";
    public static final String GET_LICENCES_FROM_ATTACHMENT = "GetLicensesFromAttachment";
    public static final String LOAD_LICENSE_INFO_ATTACHMENT_USAGE = "LoadLicenseInfoAttachmentUsage";
    public static final String LOAD_SOURCE_PACKAGE_ATTACHMENT_USAGE = "LoadSourcePackageAttachmentUsage";
    public static final String LOAD_PROJECT_LIST = "load_project_list";
    public static final String REMOVE_ORPHAN_OBLIGATION = "RemoveOrphanObligation";
    public static final String LIST_CLEARING_STATUS = "listClearingStatus";
    public static final String CLEARING_STATUS_ON_LOAD = "clearingStatusOnLoad";
    public static final String PROJECT_CHECK_FOR_ATTACHMENTS = "verifyAttachmentExistance";
    public static final String LICENSE_TO_SOURCE_FILE = "licenseToSourceFile";
    public static final String ADD_LICENSE_TO_RELEASE = "addLicenseToRelease";
    public static final String UPDATE_RELEASE_BY_LINKED_PACKAGES = "updateReleaseByLinkedPackages";

    //component actions
    public static final String ADD_VENDOR = "add_vendor";
    public static final String VIEW_VENDOR = "view_vendor";
    public static final String VIEW_DEPARTMENT = "view_department";
    public static final String CHECK_COMPONENT_NAME = "check_component_name";
    public static final String DELETE_COMPONENT = "delete_component";
    public static final String DELETE_RELEASE = "delete_release";
    public static final String SUBSCRIBE = "subscribe";
    public static final String SUBSCRIBE_RELEASE = "subscribe_release";
    public static final String UNSUBSCRIBE = "unsubscribe";
    public static final String UNSUBSCRIBE_RELEASE = "unsubscribe_release";
    public static final String LOAD_COMPONENT_LIST = "load_component_list";
    public static final String LOAD_ECC_LIST = "load_ecc_list";

    // fossology actions
    public static final String FOSSOLOGY_PREFIX = "fossology";
    public static final String FOSSOLOGY_CONFIG_BEAN = FOSSOLOGY_PREFIX + "Config";
    public static final String FOSSOLOGY_CONFIG_KEY_URL = FOSSOLOGY_PREFIX + "config_url";
    public static final String FOSSOLOGY_CONFIG_KEY_TOKEN = FOSSOLOGY_PREFIX + "config_token";
    public static final String FOSSOLOGY_CONFIG_KEY_FOLDER_ID = FOSSOLOGY_PREFIX + "config_folder_id";
    public static final String FOSSOLOGY_CHECK_CONNECTION = FOSSOLOGY_PREFIX + "check_connection";

    public static final String FOSSOLOGY_ACTION_STATUS = FOSSOLOGY_PREFIX + "status";
    public static final String FOSSOLOGY_ACTION_PROCESS = FOSSOLOGY_PREFIX + "process";
    public static final String FOSSOLOGY_ACTION_OUTDATED = FOSSOLOGY_PREFIX + "outdated";
    public static final String FOSSOLOGY_ACTION_RELOAD_REPORT = FOSSOLOGY_PREFIX + "reload_report";
    public static final String FOSSOLOGY_JOB_VIEW_LINK = "fossologyJobViewLink";

    public static final String RELEASES_AND_PROJECTS = "releasesAndProjects";

    // Task actions
    public static final String LOAD_TASK_ASSIGNMENT_LIST = "load_task_assignment_list";
    public static final String LOAD_TASK_SUBMISSION_LIST = "load_task_submission_list";

    // vendor actions
    public static final String REMOVE_VENDOR = "remove_vendor";

    // oblig actions
    public static final String REMOVE_TODO = "removeTodo";

    // license type actions
    public static final String REMOVE_LICENSE_TYPE = "removeLicenseType";
    public static final String CHECK_LICENSE_TYPE_IN_USE = "checkLicenseTypeInUse";

    // user actions
    public static final String USER_PREFIX = "user";
    public static final String USER_SEARCH = USER_PREFIX + "search";
    public static final String USER_SEARCH_GOT_TRUNCATED = USER_SEARCH + "GotTruncated";
    public static final String SAVE_PREFERENCES = "savePreferences";

    // license actions
    public static final String LICENSE_PREFIX = "license";
    public static final String LICENSE_SEARCH = LICENSE_PREFIX + "search";

    // obligation actions
    public static final String VIEW_IMPORT_OBLIGATION_ELEMENTS = "view_import_obligation_elements";

    //vulnerability actions
    public static  final String UPDATE_VULNERABILITIES_RELEASE = "updateVulnerabilitiesRelease";
    public static  final String UPDATE_VULNERABILITIES_COMPONENT = "updateVulnerabilitiesComponent";
    public static  final String UPDATE_ALL_VULNERABILITIES = "updateAllVulnerabilities";
    public static  final String UPDATE_VULNERABILITIES_PROJECT = "updateVulnerabilitiesProject";
    public static  final String UPDATE_VULNERABILITY_RATINGS = "updateVulnerabilityRatings";
    public static  final String UPDATE_VULNERABILITY_VERIFICATION = "updateVulnerabilityVerification";
    public static  final String LOAD_VULNERABILITIES_PROJECT = "loadVulnerabilitiesProject";
    public static  final String REMOVE_VULNERABILITY = "removeVulnerability";

    public static final String LIST_VULNERABILITY_WITH_VIEW_SIZE_FRIENDLY_URL = "listVulnerabilityWithViewSizeFriendlyUrl";
    public static final String UPDATE_PROJECT_VULNERABILITIES_URL = "updateProjectVulnerabilitiesURL";
    public static final String VIEW_VULNERABILITY_FRIENDLY_URL = "vulfriendlyUrl";
    public static final String UPDATE_VULNERABILITIES__FAILED_IDS = "updateVulnerabilities_failedIds";
    public static final String UPDATE_VULNERABILITIES__NEW_IDS = "updateVulnerabilities_newIds";
    public static final String UPDATE_VULNERABILITIES__UPDATED_IDS = "updateVulnerabilities_updatedIds";

    // vulnerability management actions
    public static final String VM_SYNC_COMPONENTS = "synchronizeComponents";
    public static final String VM_REVERSE_MATCH = "reverseMatch";
    public static final String VM_MATCH_ACCEPT = "acceptMatch";
    public static final String VM_MATCH_DECLINE = "declineMatch";
    public static final String VM_MATCH_GET_LIST = "getMatches";

    // Excel export
    public static final String EXPORT_TO_EXCEL = "export_to_excel";
    public static final String EMAIL_EXPORTED_EXCEL = "email_exported_excel";
    public static final String DOWNLOAD_EXCEL = "download_excel";
    public static final String EXPORT_CLEARING_TO_EXCEL = "export_clearing_to_excel";
    public static final String EXPORT_ID = "export_id";

    public static final String RESPONSE__IMPORT_GENERAL_FAILURE = "response_import_general_failure";

    //custom map keywords
    public static final String CUSTOM_MAP_KEY = "customMapKey";
    public static final String CUSTOM_MAP_VALUE = "customMapValue";
    public static final String EXTERNAL_ID_KEY = "externalIdKey";
    public static final String EXTERNAL_ID_VALUE = "externalIdValue";
    public static final String ADDITIONAL_DATA_KEY = "additionalDataKey";
    public static final String ADDITIONAL_DATA_VALUE = "additionalDataValue";
    public static final String EXTERNAL_URL_KEY = "externalUrlKey";
    public static final String EXTERNAL_URL_VALUE = "externalUrlValue";

    //! request status
    public static final String REQUEST_STATUS = "request_status";


    // friendly url placeholder values
    public static final String FRIENDLY_URL_PREFIX = "friendlyUrl";
    public static final String FRIENDLY_URL_PLACEHOLDER_PAGENAME = FRIENDLY_URL_PREFIX + "Pagename";
    public static final String FRIENDLY_URL_PLACEHOLDER_ID = FRIENDLY_URL_PREFIX + "Id";
    public static final String FRIENDLY_URL_PLACEHOLDER_LICENSE_ID = FRIENDLY_URL_PREFIX + "license.Id";

    // datatables attributes for pagination
    public static final String DATATABLE_DISPLAY_DATA = "aaData";
    public static final String DATATABLE_RECORDS_TOTAL = "recordsTotal";
    public static final String DATATABLE_RECORDS_FILTERED = "recordsFiltered";

    //
    public static String PROJECTIMPORT_HOSTS;

    // User attributes
    public static final String USER_SECONDARY_GROUP_KEY = "userSecondaryGroupKey";
    public static final String USER_SECONDARY_GROUP_VALUES = "userSecondaryGroupValues";
    public static final String PRIMARY_ROLES = "primaryRoles";
    public static final String USER_EMAIL = "userEmail";
    public static final String USER_ACTIVATE_DEACTIVATE = "userActivateDeactivate";
    public static final String USER_MISSING_COUCHDB = "userMissingCouchdb";
    public static final String USER_MISSING_LIFERAY = "userMissingLiferay";
    public static final String IS_PASSWORD_OPTIONAL = "isPasswordOptional";
    public static final String USERS_PRESENT_IN_COUCH_DB = "usersPresentInCouchDb";
    public static final String USERS_ABSENT_IN_COUCH_DB = "usersAbsentInCouchDb";
    public static final String USER_OBJ = "userObj";
    public static final String USER_CLIENT_ID_KEY = "userClientIdKey";
    public static final String USER_CLIENT_ID_ACCESS_VALUE = "userClientIdAccessValue";
    public static final String USER_CLIENT_ID_NAME_VALUE = "userClientIdNameValue";
    
    // Bulk deleting
    public static final String BULK_DELETING_RESULT_REMAINED = "Remained";
    public static final String BULK_DELETING_RESULT_DELETED = "Deleted";
    public static final String BULK_DELETING_RESULT_CONFLICTED = "Conflicted";
    public static final String BULK_DELETING_RESULT_ERROR = "Error";

    // Rest API constants
    public static final UserGroup API_WRITE_ACCESS_USERGROUP;
    public static final Boolean API_TOKEN_ENABLE_GENERATOR;
    public static final Boolean REST_API_WRITE_ACCESS_TOKEN_IN_PREFERENCES_ENABLED;
    public static final String API_TOKEN_MAX_VALIDITY_READ_IN_DAYS;
    public static final String API_TOKEN_MAX_VALIDITY_WRITE_IN_DAYS;
    public static final String API_TOKEN_HASH_SALT;
    public static final String API_TOKEN_ID = "tokenId";

    public static final String WRITE_ACCESS_USER = "writeAccessUser";

    public static final String EXTERNAL_ID_SELECTED_KEYS = "externalIds";
    public static final String ONLY_APPROVED = "onlyApproved";
    public static final String PREDEFINED_TAGS;
    public static final boolean SSO_LOGIN_ENABLED;
    public static final boolean IS_COMPONENT_VISIBILITY_RESTRICTION_ENABLED;
    public static final Boolean IS_BULK_RELEASE_DELETING_ENABLED;
    public static final boolean IS_SVM_ENABLED;

    //! Specialized keys for SPDX
    public static final String SPDX_DOCUMENT = "spdxDocument";
    public static final String SPDX_DOCUMENT_CREATION_INFO = "spdxDocumentCreationInfo";
    public static final String SPDX_PACKAGE_INFO = "spdxPackageInfo";
    public static final String ACTUAL_SPDX_DOCUMENT = "actual_SPDXDocument";
    public static final String ACTUAL_DOCUMENT_CREATION_INFO = "actual_DocumentCreationInfo";
    public static final String ACTUAL_PACKAGE_INFO = "actual_PackageInfo";
    public static final Set<String> SET_RELATIONSHIP_TYPE;

    //! Specialized keys for Flexible project and releases relationship configuration
    public static final String RELEASE_ID_ARRAY = "releaseIdArray[]";
    public static final String RELEASES_WITH_SAME_COMPONENT_ID = "releaseWithSameComponentId";
    public static final String CURRENT_NETWORK = "currentNetwork";
    public static final String CHECK_DIFF_DEPENDENCY_NETWORK_WITH_RELEASES_RELATIONSHIP = "checkDiffDependencyNetworkWithReleasesRelationship";
    public static final String RESTRICTED_RELEASE = "Restricted release";
    public static final String GET_HTML_RELEASE_ROWS = "getHtmlReleaseRows";
    public static final String DEFAULT_RELEASE_RELATION_NETWORK = "[]";
    public static final String ATTACHMENT_USAGE_ON_CLICK = "attachmentUsageOnClick";
    public static final String SUB_PROJECTS_LINK_TRANSITIVE = "subProjectsLinkTransitive";
    public static final String LOGIN_USER = "loginUser";
    public static final String IS_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP_ENABLED = "isFlexibleProjectReleaseRelationshipEnabled";
    public static final String NETWORK_PARENT_BRANCH_ID = "networkParentBranchId";
    public static final String NETWORK_RELEASE_LIST = "networkReleaseList";
    public static final String NETWORK_TOTAL_INACCESSIBLE_ROWS = "netWorkTotalInAccessibleRow";
    public static final String DEPENDENCY_NETWORK_LIST = "dependencyNetworkList";
    public static final String DEPENDENCY_NETWORK_ON_LOAD = "dependencyNetworkOnLoad";
    public static final String IS_OBLIGATION_PRESENT = "isObligationPresent";
    public static final String CREATE_LINKED_RELEASE_ROW = "createLinkedReleaseRow";
    public static final String PARENT_NODE_ID = "parentNodeIds[]";
    public static final String LAYER = "layer[]";
    public static final String RELEASE_RELATION_SHIP = "releaseRelationShip[]";
    public static final String MAINLINE_STATE = "mainlineState[]";
    public static final String INDEXES = "indexes[]";
    public static final String COMMENTS = "comments[]";
    public static final String FIND_LINKED_RELEASE_OF_NODE = "findLinkedReleaseOfNode";
    public static final String RELEASES_IN_NETWORK = "releasesInNetwork";
    public static final String NUMBER_LINKED_RELEASE = "numberLinkedRelease";
    public static final String TOTAL_RELEASE_COUNT = "totalReleaseCount";
    public static final String CHECK_RELEASE_EXIST = "checkReleaseExist";
    public static final String RELEASE_USAGE = "releaseUsage";
    public static final String CYCLIC_LINKED_RELEASE_PATH = "cyclicLinkedReleasePath";
    public static final String CHILD_RELEASE_ID_ARRAY = "childReleaseId[]";
    static {
        Properties props = CommonUtils.loadProperties(PortalConstants.class, PROPERTIES_FILE_PATH);

        PROGRAMMING_LANGUAGES = props.getProperty("programming.languages", "[ \"ActionScript\", \"AppleScript\", \"Asp\",\"Bash\", \"BASIC\", \"C\", \"C++\", \"C#\", \"Cocoa\", \"Clojure\",\"COBOL\",\"ColdFusion\", \"D\", \"Delphi\", \"Erlang\", \"Fortran\", \"Go\", \"Groovy\",\"Haskell\", \"JSP\", \"Java\",\"JavaScript\", \"Objective-C\", \"Ocaml\",\"Lisp\", \"Perl\", \"PHP\", \"Python\", \"Ruby\", \"SQL\", \"SVG\",\"Scala\",\"SmallTalk\", \"Scheme\", \"Tcl\", \"XML\", \"Node.js\", \"JSON\" ]");
        DOMAIN = CommonUtils.splitToSet(props.getProperty("domain", "Application Software, Documentation, Embedded Software, Hardware, Test and Diagnostics"));
        SOFTWARE_PLATFORMS = props.getProperty("software.platforms", "[ \"Adobe AIR\", \"Adobe Flash\", \"Adobe Shockwave\", \"Binary Runtime Environment for Wireless\", \"Cocoa (API)\", \"Cocoa Touch\", \"Java (software platform)|Java platform\", \"Java Platform, Micro Edition\", \"Java Platform, Standard Edition\", \"Java Platform, Enterprise Edition\", \"JavaFX\", \"JavaFX Mobile\", \"Microsoft XNA\", \"Mono (software)|Mono\", \"Mozilla Prism\", \".NET Framework\", \"Silverlight\", \"Open Web Platform\", \"Oracle Database\", \"Qt (framework)|Qt\", \"SAP NetWeaver\", \"Smartface\", \"Vexi\", \"Windows Runtime\" ]");
        OPERATING_SYSTEMS = props.getProperty("operating.systems", "[ \"Android\", \"BSD\", \"iOS\", \"Linux\", \"OS X\", \"QNX\", \"Microsoft Windows\", \"Windows Phone\", \"IBM z/OS\"]");
        SET_CLEARING_TEAMS_STRING = CommonUtils.splitToSet(props.getProperty("clearing.teams", "org1,org2,org3"));
        STATE = props.getProperty("state","[ \"Active\", \"Phase out\", \"Unknown\"]");
        PROJECT_TYPE = props.getProperty("project.type","[ \"Customer Project\", \"Internal Project\", \"Product\", \"Service\", \"Inner Source\"]");
        PROJECT_EXTERNAL_ID_KEYS = CommonUtils.splitToSet(props.getProperty("project.externalkeys", "internal.id"));
        PROJECT_EXTERNAL_URL_KEYS = CommonUtils.splitToSet(props.getProperty("project.externalurls", "homepage,wiki,clearing"));
        LICENSE_IDENTIFIERS = props.getProperty("license.identifiers", "[]");
        COMPONENT_CATEGORIES = props.getProperty("component.categories", "[ \"framework\", \"SDK\", \"big-data\", \"build-management\", \"cloud\", \"content\", \"database\", \"graphics\", \"http\", \"javaee\", \"library\", \"mail\", \"mobile\", \"security\", \"testing\", \"virtual-machine\", \"web-framework\", \"xml\"]");
        COMPONENT_EXTERNAL_ID_KEYS = CommonUtils.splitToSet(props.getProperty("component.externalkeys", "com.github.id,com.gitlab.id,purl.id"));
        PROJECT_ROLES = props.getProperty("custommap.project.roles", "Stakeholder,Analyst,Contributor,Accountant,End user,Quality manager,Test manager,Technical writer,Key user");
        COMPONENT_ROLES = props.getProperty("custommap.component.roles", "Committer,Contributor,Expert");
        RELEASE_ROLES = props.getProperty("custommap.release.roles", "Committer,Contributor,Expert");
        RELEASE_EXTERNAL_IDS = props.getProperty("custommap.release.externalIds", "[]");
        RELEASE_EXTERNAL_ID_KEYS = CommonUtils.splitToSet(props.getProperty("release.externalkeys", "org.maven.id,com.github.id,com.gitlab.id,purl.id"));
        PROJECTIMPORT_HOSTS = props.getProperty("projectimport.hosts", "");
        PREFERRED_COUNTRY_CODES = props.getProperty("preferred.country.codes", "DE,AT,CH,US");
        MAINLINE_STATE_ENABLED_FOR_USER = Boolean.parseBoolean(props.getProperty("mainline.state.enabled.for.user", "false"));
        IS_CLEARING_TEAM_UNKNOWN_ENABLED = Boolean.parseBoolean(props.getProperty("clearing.team.unknown.enabled", "true"));
        PROJECT_OBLIGATIONS_ACTION_SET = CommonUtils.splitToSet(props.getProperty("project.obligation.actions", "Action 1,Action 2,Action 3"));
        IS_PROJECT_OBLIGATIONS_ENABLED = Boolean.parseBoolean(props.getProperty("project.obligations.enabled", "true"));
        CUSTOM_WELCOME_PAGE_GUIDELINE = Boolean.parseBoolean(props.getProperty("custom.welcome.page.guideline", "false"));
        DEFAULT_COUNTRY_NAME = props.getProperty("liferay.default.country.name", "united-states");
        // SW360 REST API Constants
        API_TOKEN_ENABLE_GENERATOR = Boolean.parseBoolean(props.getProperty("rest.apitoken.generator.enable", "false"));
        REST_API_WRITE_ACCESS_TOKEN_IN_PREFERENCES_ENABLED = Boolean.parseBoolean(props.getProperty("rest.api.write.access.token.in.preferences.enabled", "true"));
        API_TOKEN_MAX_VALIDITY_READ_IN_DAYS = props.getProperty("rest.apitoken.read.validity.days", "90");
        API_TOKEN_MAX_VALIDITY_WRITE_IN_DAYS = props.getProperty("rest.apitoken.write.validity.days", "30");
        API_TOKEN_HASH_SALT = props.getProperty("rest.apitoken.hash.salt", "$2a$04$Software360RestApiSalt");
        API_WRITE_ACCESS_USERGROUP = UserGroup.valueOf(props.getProperty("rest.write.access.usergroup", UserGroup.ADMIN.name()));
        USER_ROLE_ALLOWED_TO_MERGE_OR_SPLIT_COMPONENT = UserGroup.valueOf(props.getProperty("user.role.allowed.to.merge.or.split.component", UserGroup.ADMIN.name()));
        LICENSE_INFO_HEADER_TEXT_FILE_NAME_BY_PROJECT_GROUP = props.getProperty("org.eclipse.sw360.licensinfo.header.by.group", "");
        CLEARING_REPORT_TEMPLATE_TO_FILENAMEMAPPING = props.getProperty("org.eclipse.sw360.licensinfo.projectclearing.templatemapping", "");
        CLEARING_REPORT_TEMPLATE_FORMAT = props.getProperty("org.eclipse.sw360.licensinfo.projectclearing.templateformat", "docx");
        PREDEFINED_TAGS = props.getProperty("project.tag", "[]");
        SSO_LOGIN_ENABLED = Boolean.parseBoolean(props.getProperty("sso.login.enabled", "false"));
        SET_RELATIONSHIP_TYPE = CommonUtils.splitToSet(props.getProperty("relationship.type", "DESCRIBES,DESCRIBED_BY,CONTAINS,CONTAINED_BY,DEPENDS_ON,DEPENDENCY_OF,DEPENDENCY_MANIFEST_OF,BUILD_DEPENDENCY_OF,DEV_DEPENDENCY_OF,OPTIONAL_DEPENDENCY_OF,PROVIDED_DEPENDENCY_OF,TEST_DEPENDENCY_OF,RUNTIME_DEPENDENCY_OF,EXAMPLE_OF,GENERATES,GENERATED_FROM,ANCESTOR_OF,DESCENDANT_OF,VARIANT_OF,DISTRIBUTION_ARTIFACT,PATCH_FOR,PATCH_APPLIED,COPY_OF,FILE_ADDED,FILE_DELETED,FILE_MODIFIED,EXPANDED_FROM_ARCHIVE,DYNAMIC_LINK,STATIC_LINK,DATA_FILE_OF,TEST_CASE_OF,BUILD_TOOL_OF,DEV_TOOL_OF,TEST_OF,TEST_TOOL_OF,DOCUMENTATION_OF,OPTIONAL_COMPONENT_OF,METAFILE_OF,PACKAGE_OF,AMENDS,PREREQUISITE_FOR,HAS_PREREQUISITE,REQUIREMENT_DESCRIPTION_FOR,SPECIFICATION_FOR,OTHER"));
        IS_COMPONENT_VISIBILITY_RESTRICTION_ENABLED = Boolean.parseBoolean(
            System.getProperty("RunComponentVisibilityRestrictionTest", props.getProperty("component.visibility.restriction.enabled", "false")));
        DISABLE_CLEARING_REQUEST_FOR_PROJECT_WITH_GROUPS = props.getProperty("org.eclipse.sw360.disable.clearing.request.for.project.group", "");
        SEND_PROJECT_SPREADSHEET_EXPORT_TO_MAIL_ENABLED = Boolean.parseBoolean(props.getProperty("send.project.spreadsheet.export.to.mail.enabled", "false"));
        SEND_COMPONENT_SPREADSHEET_EXPORT_TO_MAIL_ENABLED = Boolean.parseBoolean(props.getProperty("send.component.spreadsheet.export.to.mail.enabled", "false"));
        IS_BULK_RELEASE_DELETING_ENABLED = Boolean.parseBoolean(System.getProperty("RunBulkReleaseDeletingTest", props.getProperty("bulk.release.deleting.enabled", "false")));
        IS_SVM_ENABLED = Boolean.parseBoolean(props.getProperty("enable.security.vulnerability.monitoring", "false"));
    }

    private PortalConstants() {
        // Utility class with only static functions
    }
}
