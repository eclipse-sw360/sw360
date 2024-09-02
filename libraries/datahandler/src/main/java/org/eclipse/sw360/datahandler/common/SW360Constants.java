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
    public static final String SVM_COMPONENT_ID;
    public static final String SVM_MONITORINGLIST_ID;
    public static final Boolean SPDX_DOCUMENT_ENABLED;
    public static final String MAINLINE_COMPONENT_ID;
    public static final String SVM_COMPONENT_ID_KEY;
    public static final String SVM_SHORT_STATUS;
    public static final String SVM_SHORT_STATUS_KEY;
    public static final String SVM_SCHEDULER_EMAIL;
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

    public static final Collection<AttachmentType> LICENSE_INFO_ATTACHMENT_TYPES = Arrays.asList(AttachmentType.COMPONENT_LICENSE_INFO_XML, AttachmentType.COMPONENT_LICENSE_INFO_COMBINED);
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

    private SW360Constants() {
        // Utility class with only static functions
    }

    static {
        Properties props = CommonUtils.loadProperties(SW360Constants.class, PROPERTIES_FILE_PATH);
        SVM_COMPONENT_ID = props.getProperty("svm.component.id", "");
        MAINLINE_COMPONENT_ID = props.getProperty("mainline.component.id", "");
        SVM_COMPONENT_ID_KEY = props.getProperty("svm.component.id.key", "");
        SVM_SHORT_STATUS = props.getProperty("svm.short.status", "");
        SVM_SHORT_STATUS_KEY = props.getProperty("svm.short.status.key", "");
        SVM_SCHEDULER_EMAIL = props.getProperty("svm.scheduler.email", "");
        SVM_MONITORINGLIST_ID = props.getProperty("svm.monitoringlist.id", "");
        SPDX_DOCUMENT_ENABLED = Boolean.parseBoolean(props.getProperty("spdx.document.enabled", "false"));
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
    }

    private static Map.Entry<String, String> pair(TFieldIdEnum field, String displayName){
        return new AbstractMap.SimpleImmutableEntry<>(field.toString(), displayName);
    }
}
