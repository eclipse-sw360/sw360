/*
 * Copyright Siemens AG, 2014-2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.thrift.TFieldIdEnum;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Sets.newHashSet;

/**
 * Utility class with definitions for the CouchDB connection
 *
 * @author cedric.bodet@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class SW360Constants {

    public static final String KEY_ID = "_id";
    public static final String KEY_REV = "_rev";
    public static final String PROPERTIES_FILE_PATH = "/sw360.properties";

    public static final String PROJECT_VULNERABILITY_RATING_ID_PREFIX = "pvrating_";
    public static final String LICENSE_TYPE_GLOBAL = "Global";
    public static final String LICENSE_TYPE_OTHERS = "Others";
    public static final String NA = "n/a";
    public static final String LICENSE_NAME_UNKNOWN = "License name unknown";
    public static final String OBLIGATION_TOPIC_UNKNOWN = "Obligation topic unknown";
    public static final String NO_ASSERTION = "noassertion";
    public static final String STATUS = "status";
    public static final String SUCCESS = "success";
    public static final String FAILURE = "failure";
    public static final String MESSAGE = "message";
    public static final String NULL_STRING = "null";
    public static final String PACKAGE_URL = "package-url";
    public static final String PURL_ID = "purl.id";
    public static final String DUPLICATE_PACKAGE_BY_PURL = "duplicatePackagesByPurl";
    public static final String XML_FILE_EXTENSION = "xml";
    public static final String JSON_FILE_EXTENSION = "json";
    public static final String PROJECT_IDS = "projectIds";
    public static final String RELEASE_IDS = "releaseIds";
    public static final String PACKAGE_IDS = "packageIds";

    // Proper values of the "type" member to deserialize to CouchDB
    public static final String TYPE_OBLIGATION = "obligation";
    public static final String TYPE_OBLIGATIONS = "obligations";
    public static final String TYPE_RISKCATEGORY = "riskCategory";
    public static final String TYPE_RISK = "risk";
    public static final String TYPE_LICENSETYPE = "licenseType";
    public static final String TYPE_LICENSE = "license";
    public static final String TYPE_VENDOR = "vendor";
    public static final String TYPE_USER = "user";
    public static final String TYPE_COMPONENT = "component";
    public static final String TYPE_RELEASE = "release";
    public static final String TYPE_ATTACHMENT = "attachment";
    public static final String TYPE_PROJECT = "project";
    public static final String TYPE_PROJECT_OBLIGATION = "obligationList";
    public static final String TYPE_MODERATION = "moderation";
    public static final String TYPE_CLEARING = "clearing";
    public static final String TYPE_SEARCHRESULT = "searchResult";
    public static final String TYPE_CHANGELOG = "changeLog";
    public static final String TYPE_VULNERABILITYDTO = "vulDTO";
    public static final String TYPE_VULNERABILITYSUMMARY = "vulSummary";
    public static final String TYPE_VULNERABILITY = "vul";
    public static final String TYPE_OBLIGATIONELEMENT = "obligationElement";
    public static final String TYPE_OBLIGATIONNODE = "obligationNode";
    public static final String TYPE_DOCUMENT = "document";
    public static final String TYPE_COMMENT = "comment";
    public static final String TYPE_SPDX_DOCUMENT = "SPDXDocument";
    public static final String TYPE_SPDX_DOCUMENT_CREATION_INFO = "documentCreationInformation";
    public static final String TYPE_SPDX_PACKAGE_INFO = "packageInformation";
    public static final String TYPE_PACKAGE = "package";
    public static final String TYPE_ECC = "ecc";
    public static final String PLEASE_ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP = "Please enable flexible project " +
            "release relationship configuration to use this function (enable.flexible.project.release.relationship = true)";

    public static final String RDF_FILE_EXTENSION = ".rdf";
    public static final String MAIN_LICENSE_FILES = "LICENSE.*|License.*|license|license.txt|license.html|COPYING.*|Copying.*|copying|copying.txt|copying.html";
    public static final String LICENSE_PREFIX = "license";
    public static final String CONCLUDED_LICENSE_IDS = "Concluded License Ids";
    public static final String LICENSE_IDS = "licenseIds";
    public static final String MAIN_LICENSE_ID = "Main License Id";
    public static final String OTHER_LICENSE = "otherLicense";
    public static final String OTHER_LICENSE_IDS = "Other License Ids";
    public static final String OTHER_LICENSE_IDS_KEY = "otherLicenseIds";
    public static final String POSSIBLE_MAIN_LICENSE_IDS = "Possible Main License Ids";
    public static final String TOTAL_FILE_COUNT = "totalFileCount";

    // SVM Constants
    public static final String SVM_COMPONENT_ID;
    public static final String SVM_MONITORINGLIST_ID;
    public static final Boolean SPDX_DOCUMENT_ENABLED;
    public static final Boolean SPDX_USE_LICENSE_INFO_FROM_FILES;
    public static final String MAINLINE_COMPONENT_ID;
    public static final String SVM_COMPONENT_ID_KEY;
    public static final String SVM_SHORT_STATUS;
    public static final String SVM_SHORT_STATUS_KEY;
    public static final String SVM_SCHEDULER_EMAIL;
    public static final String SVM_MONITORING_LIST_API_URL;
    public static final String SVM_COMPONENT_MAPPINGS_API_URL;
    public static final char[] SVM_KEY_STORE_PASSPHRASE;
    public static final String SVM_KEY_STORE_FILENAME;
    public static final char[] SVM_JAVA_KEYSTORE_PASSWORD;
    public static final String SVM_BASE_HOST_URL;
    public static final String SVM_API_ROOT_PATH;
    public static final String SVM_COMPONENTS_URL;
    public static final String SVM_ACTIONS_URL;
    public static final String SVM_PRIORITIES_URL;
    public static final String SVM_VULNERABILITIES_PER_COMPONENT_URL;
    public static final String SVM_VULNERABILITIES_URL;
    public static final String SVM_COMPONENTS_ID_WILDCARD = "#compVmId#";

    public static final String DATA_HANDLER_POM_FILE_PATH;
    public static final UserGroup PACKAGE_PORTLET_WRITE_ACCESS_USER_ROLE;
    public static final boolean IS_PACKAGE_PORTLET_ENABLED;
    public static final String TOOL_NAME;
    public static final String TOOL_VENDOR;
    public static final UserGroup SBOM_IMPORT_EXPORT_ACCESS_USER_ROLE;
    public static final boolean ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP;
    public static final String URL_FORMATS;
    public static final String SRC_ATTACHMENT_UPLOADER_EMAIL;
    public static final String SRC_ATTACHMENT_DOWNLOAD_LOCATION;
    public static final String PREFERRED_CLEARING_DATE_LIMIT;
    public static final Boolean MAIL_REQUEST_FOR_PROJECT_REPORT;
    public static final Boolean MAIL_REQUEST_FOR_COMPONENT_REPORT;

    public static final Boolean MAINLINE_STATE_ENABLED_FOR_USER;
    public static final Boolean IS_BULK_RELEASE_DELETING_ENABLED;
    public static final Boolean IS_FORCE_UPDATE_ENABLED;
    public static final boolean IS_COMPONENT_VISIBILITY_RESTRICTION_ENABLED;
    public static final boolean IS_ADMIN_PRIVATE_ACCESS_ENABLED;
    public static final Boolean DISABLE_CLEARING_FOSSOLOGY_REPORT_DOWNLOAD;
    public static final String FRIENDLY_RELEASE_URL;

    // Authorization server
    private static final String DEFAULT_WRITE_ACCESS_USERGROUP = UserGroup.SW360_ADMIN.name();
    private static final String DEFAULT_ADMIN_ACCESS_USERGROUP = UserGroup.SW360_ADMIN.name();
    public static final String SW360_LIFERAY_COMPANY_ID;
    public static final String CONFIG_ACCESS_TOKEN_VALIDITY_SECONDS;
    public static final UserGroup CONFIG_WRITE_ACCESS_USERGROUP;
    public static final UserGroup CONFIG_ADMIN_ACCESS_USERGROUP;

    // CLI Constants
    public static final String CLI_RELEASE_EXTERNAL_ID_CORRELATION_KEY;

    // CVE Constants
    public static final int CVE_VENDOR_THRESHOLD;
    public static final int CVE_PRODUCT_THRESHOLD;
    public static final int CVE_CUTOFF;

    // REST Constants
    public static final String REST_API_TOKEN_HASH_SALT;
    public static final String REST_API_TOKEN_MAX_VALIDITY_READ_IN_DAYS;
    public static final String REST_API_TOKEN_MAX_VALIDITY_WRITE_IN_DAYS;
    public static final Boolean REST_API_TOKEN_GENERATOR_ENABLE;
    public static final Boolean REST_API_WRITE_ACCESS_TOKEN_ENABLE;
    public static final Set<String> DOMAIN;
    public static final String REST_REPORT_FILENAME_MAPPING;
    public static final String REST_JWKS_ISSUER_URL;
    public static final String REST_JWKS_ENDPOINT_URL;
    public static final Boolean REST_IS_JWKS_VALIDATION_ENABLED;
    public static final String REST_SERVER_PATH_URL;

    // UI Portlet Constants
    public static final String CLEARING_TEAMS;
    public static final Boolean CLEARING_TEAM_UNKNOWN_ENABLED;
    public static final Set<String> COMPONENT_CATEGORIES;
    public static final Set<String> COMPONENT_EXTERNAL_ID_KEYS;
    public static final Set<String> COMPONENTS_ACTIVATE;
    public static final Set<String> COMPONENT_ROLES;
    public static final Set<String> PROJECT_ROLES;
    public static final Set<String> RELEASE_EXTERNAL_IDS;
    public static final Set<String> RELEASE_ROLES;
    public static final Boolean CUSTOM_WELCOME_PAGE_GUIDELINE;
    public static final Boolean ENABLE_ADD_LIC_INFO_TO_RELEASE;
    public static final Boolean IS_SVM_ENABLED;
    public static final Set<String> LICENSE_IDENTIFIERS;
    public static final Set<String> OPERATING_SYSTEMS;
    public static final Boolean DISABLE_CLEARING_REQUEST_FOR_PROJECT_WITH_GROUPS;
    public static final String LICENSE_INFO_HEADER_TEXT_FILE_NAME_BY_PROJECT_GROUP;
    public static final String CLEARING_REPORT_TEMPLATE_FORMAT;
    public static final Set<String> PROGRAMMING_LANGUAGES;
    public static final Set<String> PROJECT_EXTERNAL_ID_KEYS;
    public static final Set<String> PROJECT_EXTERNAL_URL_KEYS;
    public static final String PROJECTIMPORT_HOSTS;
    public static final Set<String> PROJECT_OBLIGATIONS_ACTION_SET;
    public static final Boolean IS_PROJECT_OBLIGATIONS_ENABLED;
    public static final Set<String> PREDEFINED_TAGS;
    public static final Set<String> PROJECT_TYPE;
    public static final Set<String> SET_RELATIONSHIP_TYPE;
    public static final Set<String> RELEASE_EXTERNAL_ID_KEYS;
    public static final Boolean SEND_PROJECT_LICENSE_INFO_SPREADSHEET_EXPORT_TO_MAIL_ENABLED;
    public static final Set<String> SOFTWARE_PLATFORMS;
    public static final Set<String> STATE;
    public static final UserGroup USER_ROLE_ALLOWED_TO_MERGE_OR_SPLIT_COMPONENT;

    public static final Properties props;

    /**
     * Hashmap containing the name field for each type.
     * Used by the search service to fill the search results
     */
    public static final Map<String, String> MAP_FULLTEXT_SEARCH_NAME =
            ImmutableMap.<String, String>builder()
                    .put(TYPE_LICENSE, "fullname")
                    .put(TYPE_OBLIGATIONS, "text")
                    .put(TYPE_OBLIGATION, "title")
                    .put(TYPE_USER, "email")
                    .put(TYPE_VENDOR, "fullname")
                    .put(TYPE_COMPONENT, "name")
                    .put(TYPE_RELEASE, "name version")
                    .put(TYPE_PROJECT, "name version")
                    .put(TYPE_PACKAGE, "name version")
                    .build();

    public static final Collection<AttachmentType> LICENSE_INFO_ATTACHMENT_TYPES = Arrays.asList(AttachmentType.COMPONENT_LICENSE_INFO_XML,
            AttachmentType.COMPONENT_LICENSE_INFO_COMBINED);
    public static final Collection<AttachmentType> INITIAL_LICENSE_INFO_ATTACHMENT_TYPES = Arrays.asList(AttachmentType.COMPONENT_LICENSE_INFO_XML,
            AttachmentType.COMPONENT_LICENSE_INFO_COMBINED, AttachmentType.INITIAL_SCAN_REPORT);
    public static final Collection<AttachmentType> SOURCE_CODE_ATTACHMENT_TYPES = Arrays.asList(AttachmentType.SOURCE, AttachmentType.SOURCE_SELF);
    public static final String CONTENT_TYPE_OPENXML_SPREADSHEET = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public static final String NOTIFICATION_CLASS_RELEASE = "release";
    public static final String NOTIFICATION_CLASS_MODERATION_REQUEST = "moderation";
    public static final String NOTIFICATION_CLASS_CLEARING_REQUEST = "clearing";
    public static final String NOTIFICATION_CLASS_COMPONENT = "component";
    public static final String NOTIFICATION_CLASS_PROJECT = "project";
    public static final Map<String, List<Map.Entry<String, String>>> NOTIFIABLE_ROLES_BY_OBJECT_TYPE = ImmutableMap.<String, List<Map.Entry<String, String>>>builder()
            .put(NOTIFICATION_CLASS_PROJECT, ImmutableList.<Map.Entry<String, String>>builder()
                    .add(pair(Project._Fields.PROJECT_RESPONSIBLE, "Project Responsible"),
                            pair(Project._Fields.PROJECT_OWNER, "Project Owner"),
                            pair(Project._Fields.LEAD_ARCHITECT, "Lead Architect"),
                            pair(Project._Fields.MODERATORS, "Moderator"),
                            pair(Project._Fields.CONTRIBUTORS, "Contributor"),
                            pair(Project._Fields.SECURITY_RESPONSIBLES, "Security Responsible"),
                            pair(Project._Fields.ROLES, "Additional Role"))
                    .build())
            .put(NOTIFICATION_CLASS_COMPONENT, ImmutableList.<Map.Entry<String, String>>builder()
                    .add(pair(Component._Fields.CREATED_BY, "Creator"),
                            pair(Component._Fields.COMPONENT_OWNER, "Component Owner"),
                            pair(Component._Fields.MODERATORS, "Moderator"),
                            pair(Component._Fields.SUBSCRIBERS, "Subscriber"),
                            pair(Component._Fields.ROLES, "Additional Role"))
                    .build())
            .put(NOTIFICATION_CLASS_RELEASE, ImmutableList.<Map.Entry<String, String>>builder()
                    .add(pair(Release._Fields.CREATED_BY, "Creator"),
                            pair(Release._Fields.CONTRIBUTORS, "Contributor"),
                            pair(Release._Fields.MODERATORS, "Moderator"),
                            pair(Release._Fields.SUBSCRIBERS, "Subscriber"),
                            pair(Release._Fields.ROLES, "Additional Role"))
                    .build())
            .put(NOTIFICATION_CLASS_MODERATION_REQUEST, ImmutableList.<Map.Entry<String, String>>builder()
                    .add(pair(ModerationRequest._Fields.REQUESTING_USER, "Requesting User"),
                            pair(ModerationRequest._Fields.MODERATORS, "Moderator"))
                    .build())
            .put(NOTIFICATION_CLASS_CLEARING_REQUEST, ImmutableList.<Map.Entry<String, String>>builder()
                    .add(pair(ClearingRequest._Fields.REQUESTING_USER, "Requesting User"))
                    .build())
            .build();
    public static final List<String> NOTIFICATION_EVENTS_KEYS = NOTIFIABLE_ROLES_BY_OBJECT_TYPE.entrySet()
            .stream()
            .map(notificationClassEntry -> notificationClassEntry.getValue()
                    .stream()
                    .map(roleEntry -> SW360Utils.notificationPreferenceKey(notificationClassEntry.getKey(), roleEntry.getKey())))
            .flatMap(Function.identity())
            .collect(Collectors.toList());

    static final Map<String, Boolean> DEFAULT_NOTIFICATION_PREFERENCES = NOTIFICATION_EVENTS_KEYS.stream().collect(Collectors.toMap(s -> s, s -> Boolean.FALSE));

    public static Collection<AttachmentType> allowedAttachmentTypes(String documentType) {
        Set<AttachmentType> types = newHashSet(AttachmentType.values());

        if (TYPE_COMPONENT.equals(documentType)) {
            return Sets.filter(types, not(equalTo(AttachmentType.CLEARING_REPORT)));
        } else {
            return types;
        }
    }

    SW360Constants() {
        // Utility class with only static functions
    }

    static {
        props = CommonUtils.loadProperties(SW360Constants.class, PROPERTIES_FILE_PATH);

        SVM_COMPONENT_ID = props.getProperty("svm.component.id", "");
        MAINLINE_COMPONENT_ID = props.getProperty("mainline.component.id", "");
        SVM_COMPONENT_ID_KEY = props.getProperty("svm.component.id.key", "");
        SVM_SHORT_STATUS = props.getProperty("svm.short.status", "");
        SVM_SHORT_STATUS_KEY = props.getProperty("svm.short.status.key", "");
        SVM_SCHEDULER_EMAIL = props.getProperty("svm.scheduler.email", "");
        SVM_MONITORINGLIST_ID = props.getProperty("svm.monitoringlist.id", "");
        SVM_MONITORING_LIST_API_URL  = props.getProperty("svm.sw360.api.url", "");
        SVM_COMPONENT_MAPPINGS_API_URL  = props.getProperty("svm.sw360.componentmappings.api.url", "");
        SVM_KEY_STORE_FILENAME  = props.getProperty("svm.sw360.certificate.filename", "not-configured");
        SVM_KEY_STORE_PASSPHRASE = props.getProperty("svm.sw360.certificate.passphrase", "").toCharArray();
        SVM_JAVA_KEYSTORE_PASSWORD = props.getProperty("svm.sw360.jks.password", "changeit").toCharArray();

        SVM_BASE_HOST_URL  = props.getProperty("svm.base.path", "");
        SVM_API_ROOT_PATH  = props.getProperty("svm.api.root.path", "");
        SVM_COMPONENTS_URL = props.getProperty("svm.components.url", SVM_BASE_HOST_URL + SVM_API_ROOT_PATH + "/components");
        SVM_ACTIONS_URL    = props.getProperty("svm.actions.url", SVM_BASE_HOST_URL + SVM_API_ROOT_PATH + "/actions");
        SVM_PRIORITIES_URL = props.getProperty("svm.priorities.url", SVM_BASE_HOST_URL + SVM_API_ROOT_PATH + "/priorities");
        SVM_VULNERABILITIES_PER_COMPONENT_URL = props.getProperty("svm.components.vulnerabilities.url",
                SVM_BASE_HOST_URL + SVM_API_ROOT_PATH + "/components/" + SVM_COMPONENTS_ID_WILDCARD + "/notifications");
        SVM_VULNERABILITIES_URL = props.getProperty("svm.vulnerabilities.url", SVM_BASE_HOST_URL + SVM_API_ROOT_PATH + "/notifications");

        SPDX_DOCUMENT_ENABLED = Boolean.parseBoolean(props.getProperty("spdx.document.enabled", "false"));
        SPDX_USE_LICENSE_INFO_FROM_FILES = Boolean
                .valueOf(props.getProperty("licenseinfo.spdxparser.use-license-info-from-files", "true"));

        DATA_HANDLER_POM_FILE_PATH = props.getProperty("datahandler.pom.file.path", "/META-INF/maven/org.eclipse.sw360/datahandler/pom.xml");
        PACKAGE_PORTLET_WRITE_ACCESS_USER_ROLE = UserGroup.valueOf(props.getProperty("package.portlet.write.access.usergroup", UserGroup.USER.name()));
        IS_PACKAGE_PORTLET_ENABLED = Boolean.parseBoolean(props.getProperty("package.portlet.enabled", "true"));
        TOOL_NAME = props.getProperty("sw360.tool.name", "SW360");
        TOOL_VENDOR = props.getProperty("sw360.tool.vendor", "Eclipse Foundation");
        SBOM_IMPORT_EXPORT_ACCESS_USER_ROLE = UserGroup.valueOf(props.getProperty("sbom.import.export.access.usergroup", UserGroup.USER.name()));
        ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP = Boolean.parseBoolean(
                System.getProperty("RunTestFlexibleRelationship", props.getProperty("enable.flexible.project.release.relationship", "false")));
        URL_FORMATS = props.getProperty("source.download.formats","");
        SRC_ATTACHMENT_UPLOADER_EMAIL = props.getProperty("source.code.attachment.uploader.email", "");
        SRC_ATTACHMENT_DOWNLOAD_LOCATION = props.getProperty("src.attachment.download.location", "");
        PREFERRED_CLEARING_DATE_LIMIT =  props.getProperty("preferred.clearing.date.limit","");
        MAIL_REQUEST_FOR_PROJECT_REPORT = Boolean.parseBoolean(props.getProperty("send.project.spreadsheet.export.to.mail.enabled", "false"));
        MAIL_REQUEST_FOR_COMPONENT_REPORT = Boolean.parseBoolean(props.getProperty("send.component.spreadsheet.export.to.mail.enabled", "false"));

        MAINLINE_STATE_ENABLED_FOR_USER = Boolean.parseBoolean(props.getProperty("mainline.state.enabled.for.user", "false"));
        IS_BULK_RELEASE_DELETING_ENABLED = Boolean.parseBoolean(System.getProperty("RunBulkReleaseDeletingTest", props.getProperty("bulk.release.deleting.enabled", "false")));
        IS_FORCE_UPDATE_ENABLED = Boolean.parseBoolean(
                System.getProperty("RunRestForceUpdateTest", props.getProperty("rest.force.update.enabled", "false")));
        IS_COMPONENT_VISIBILITY_RESTRICTION_ENABLED = Boolean.parseBoolean(
                System.getProperty("RunComponentVisibilityRestrictionTest", props.getProperty("component.visibility.restriction.enabled", "false")));
        IS_ADMIN_PRIVATE_ACCESS_ENABLED = Boolean.parseBoolean(
                System.getProperty("RunPrivateProjectAccessTest", props.getProperty("admin.private.project.access.enabled", "false")));
        DISABLE_CLEARING_FOSSOLOGY_REPORT_DOWNLOAD = Boolean.parseBoolean(props.getProperty("disable.clearing.fossology.report.download", "false"));

        FRIENDLY_RELEASE_URL = props.getProperty("release.friendly.url", "");

        CONFIG_WRITE_ACCESS_USERGROUP = UserGroup.valueOf(props.getProperty("rest.write.access.usergroup", DEFAULT_WRITE_ACCESS_USERGROUP));
        CONFIG_ADMIN_ACCESS_USERGROUP = UserGroup.valueOf(props.getProperty("rest.admin.access.usergroup", DEFAULT_ADMIN_ACCESS_USERGROUP));
        CONFIG_ACCESS_TOKEN_VALIDITY_SECONDS = props.getProperty("rest.access.token.validity.seconds", null);
        SW360_LIFERAY_COMPANY_ID = props.getProperty("sw360.liferay.company.id", null);

        CLI_RELEASE_EXTERNAL_ID_CORRELATION_KEY = props.getProperty("combined.cli.parser.external.id.correlation.key");

        CVE_VENDOR_THRESHOLD = CommonUtils.getIntOrDefault(props.getProperty("cvesearch.vendor.threshold"), 1);
        CVE_PRODUCT_THRESHOLD = CommonUtils.getIntOrDefault(props.getProperty("cvesearch.product.threshold"), 0);
        CVE_CUTOFF = CommonUtils.getIntOrDefault(props.getProperty("cvesearch.cutoff"), 6);

        REST_API_TOKEN_MAX_VALIDITY_READ_IN_DAYS = props.getProperty("rest.apitoken.read.validity.days", "90");
        REST_API_TOKEN_MAX_VALIDITY_WRITE_IN_DAYS = props.getProperty("rest.apitoken.write.validity.days", "30");
        REST_API_TOKEN_GENERATOR_ENABLE = Boolean.parseBoolean(props.getProperty("rest.apitoken.generator.enable", "false"));
        REST_API_WRITE_ACCESS_TOKEN_ENABLE = Boolean.parseBoolean(props.getProperty("rest.api.write.access.token.in.preferences.enabled", "true"));
        REST_API_TOKEN_HASH_SALT = props.getProperty("rest.apitoken.hash.salt", "$2a$04$Software360RestApiSalt");
        DOMAIN = CommonUtils.splitToSet(props.getProperty("domain",
                "Application Software, Documentation, Embedded Software, Hardware, Test and Diagnostics"));
        REST_REPORT_FILENAME_MAPPING = props.getProperty("org.eclipse.sw360.licensinfo.projectclearing.templatemapping", "");
        REST_JWKS_ISSUER_URL = props.getProperty("jwks.issuer.url", null);
        REST_JWKS_ENDPOINT_URL = props.getProperty("jwks.endpoint.url", null);
        REST_IS_JWKS_VALIDATION_ENABLED = Boolean.parseBoolean(props.getProperty("jwks.validation.enabled", "false"));
        REST_SERVER_PATH_URL = props.getProperty("backend.url", "http://localhost:8080");

        CLEARING_TEAMS = props.getProperty("clearing.teams", "org1,org2,org3");
        CLEARING_TEAM_UNKNOWN_ENABLED = Boolean.valueOf(props.getProperty("clearing.team.unknown.enabled", "true"));
        COMPONENT_CATEGORIES = CommonUtils.splitToSet(props.getProperty("component.categories", "framework,SDK,big-data,build-management,cloud,content,database,graphics,http,javaee,library,mail,mobile,security,testing,virtual-machine,web-framework,xml"));
        COMPONENT_EXTERNAL_ID_KEYS = CommonUtils.splitToSet(props.getProperty("component.externalkeys", "com.github.id,com.gitlab.id,purl.id"));
        COMPONENTS_ACTIVATE = CommonUtils.splitToSet(props.getProperty("components.activate", ""));
        COMPONENT_ROLES = CommonUtils.splitToSet(props.getProperty("custommap.component.roles", "Committer,Contributor,Expert"));
        PROJECT_ROLES = CommonUtils.splitToSet(props.getProperty("custommap.project.roles", "Stakeholder,Analyst,Contributor,Accountant,End user,Quality manager,Test manager,Technical writer,Key user"));
        RELEASE_EXTERNAL_IDS = CommonUtils.splitToSet(props.getProperty("custommap.release.externalIds", ""));
        RELEASE_ROLES = CommonUtils.splitToSet(props.getProperty("custommap.release.roles", "Committer,Contributor,Expert"));
        CUSTOM_WELCOME_PAGE_GUIDELINE = Boolean.parseBoolean(props.getProperty("custom.welcome.page.guideline", "false"));
        ENABLE_ADD_LIC_INFO_TO_RELEASE = Boolean.parseBoolean(props.getProperty("enable.add.license.info.to.release.button", "false"));
        IS_SVM_ENABLED = Boolean.parseBoolean(props.getProperty("enable.security.vulnerability.monitoring", "false"));
        LICENSE_IDENTIFIERS = CommonUtils.splitToSet(props.getProperty("license.identifiers", ""));
        OPERATING_SYSTEMS = CommonUtils.splitToSet(props.getProperty("operating.systems", "Android,BSD,iOS,Linux,OS X,QNX,Microsoft Windows,Windows Phone,IBM z/OS"));
        DISABLE_CLEARING_REQUEST_FOR_PROJECT_WITH_GROUPS = Boolean.parseBoolean(props.getProperty("org.eclipse.sw360.disable.clearing.request.for.project.group", "false"));
        LICENSE_INFO_HEADER_TEXT_FILE_NAME_BY_PROJECT_GROUP = props.getProperty("org.eclipse.sw360.licensinfo.header.by.group", "");
        CLEARING_REPORT_TEMPLATE_FORMAT = props.getProperty("org.eclipse.sw360.licensinfo.projectclearing.templateformat", "docx");
        PROGRAMMING_LANGUAGES = CommonUtils.splitToSet(props.getProperty("programming.languages", "ActionScript,AppleScript,Asp,Bash,BASIC,C,C++,C#,Cocoa,Clojure,COBOL,ColdFusion,D,Delphi,Erlang,Fortran,Go,Groovy,Haskell,JSP,Java,JavaScript,Objective-C,Ocaml,Lisp,Perl,PHP,Python,Ruby,SQL,SVG,Scala,SmallTalk,Scheme,Tcl,XML,Node.js,JSON"));
        PROJECT_EXTERNAL_ID_KEYS = CommonUtils.splitToSet(props.getProperty("project.externalkeys", "internal.id"));
        PROJECT_EXTERNAL_URL_KEYS = CommonUtils.splitToSet(props.getProperty("project.externalurls", "homepage,wiki,clearing"));
        PROJECTIMPORT_HOSTS = props.getProperty("projectimport.hosts", "");
        PROJECT_OBLIGATIONS_ACTION_SET = CommonUtils.splitToSet(props.getProperty("project.obligation.actions", "Action 1,Action 2,Action 3"));
        IS_PROJECT_OBLIGATIONS_ENABLED = Boolean.parseBoolean(props.getProperty("project.obligations.enabled", "true"));
        PREDEFINED_TAGS = CommonUtils.splitToSet(props.getProperty("project.tag", ""));
        PROJECT_TYPE = CommonUtils.splitToSet(props.getProperty("project.type","Customer Project,Internal Project,Product,Service,Inner Source"));
        SET_RELATIONSHIP_TYPE = CommonUtils.splitToSet(props.getProperty("relationship.type", "DESCRIBES,DESCRIBED_BY,CONTAINS,CONTAINED_BY,DEPENDS_ON,DEPENDENCY_OF,DEPENDENCY_MANIFEST_OF,BUILD_DEPENDENCY_OF,DEV_DEPENDENCY_OF,OPTIONAL_DEPENDENCY_OF,PROVIDED_DEPENDENCY_OF,TEST_DEPENDENCY_OF,RUNTIME_DEPENDENCY_OF,EXAMPLE_OF,GENERATES,GENERATED_FROM,ANCESTOR_OF,DESCENDANT_OF,VARIANT_OF,DISTRIBUTION_ARTIFACT,PATCH_FOR,PATCH_APPLIED,COPY_OF,FILE_ADDED,FILE_DELETED,FILE_MODIFIED,EXPANDED_FROM_ARCHIVE,DYNAMIC_LINK,STATIC_LINK,DATA_FILE_OF,TEST_CASE_OF,BUILD_TOOL_OF,DEV_TOOL_OF,TEST_OF,TEST_TOOL_OF,DOCUMENTATION_OF,OPTIONAL_COMPONENT_OF,METAFILE_OF,PACKAGE_OF,AMENDS,PREREQUISITE_FOR,HAS_PREREQUISITE,REQUIREMENT_DESCRIPTION_FOR,SPECIFICATION_FOR,OTHER"));
        RELEASE_EXTERNAL_ID_KEYS = CommonUtils.splitToSet(props.getProperty("release.externalkeys", "org.maven.id,com.github.id,com.gitlab.id,purl.id"));
        SEND_PROJECT_LICENSE_INFO_SPREADSHEET_EXPORT_TO_MAIL_ENABLED = Boolean.parseBoolean(props.getProperty("send.project.license.info.spreadsheet.export.to.mail.enabled", "true"));
        SOFTWARE_PLATFORMS = CommonUtils.splitToSet(props.getProperty("software.platforms", "Adobe AIR,Adobe Flash,Adobe Shockwave,Binary Runtime Environment for Wireless,Cocoa (API),Cocoa Touch,Java (software platform)|Java platform,Java Platform - Micro Edition,Java Platform - Standard Edition,Java Platform - Enterprise Edition,JavaFX,JavaFX Mobile,Microsoft XNA,Mono (software)|Mono,Mozilla Prism,.NET Framework,Silverlight,Open Web Platform,Oracle Database,Qt (framework)|Qt,SAP NetWeaver,Smartface,Vexi,Windows Runtime"));
        STATE = CommonUtils.splitToSet(props.getProperty("state","Active,Phase out,Unknown"));
        USER_ROLE_ALLOWED_TO_MERGE_OR_SPLIT_COMPONENT = UserGroup.valueOf(props.getProperty("user.role.allowed.to.merge.or.split.component", UserGroup.ADMIN.name()));
    }

    private static Map.Entry<String, String> pair(TFieldIdEnum field, String displayName){
        return new AbstractMap.SimpleImmutableEntry<>(field.toString(), displayName);
    }
}
