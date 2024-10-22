/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.TFieldIdEnum;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.common.WrappedException.WrappedTException;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseMixInForChangeLog.AttachmentMixin;
import org.eclipse.sw360.datahandler.couchdb.DatabaseMixInForChangeLog.COTSDetailsMixin;
import org.eclipse.sw360.datahandler.couchdb.DatabaseMixInForChangeLog.ClearingInformationMixin;
import org.eclipse.sw360.datahandler.couchdb.DatabaseMixInForChangeLog.EccInformationMixin;
import org.eclipse.sw360.datahandler.couchdb.DatabaseMixInForChangeLog.ProjectProjectRelationshipMixin;
import org.eclipse.sw360.datahandler.couchdb.DatabaseMixInForChangeLog.ObligationStatusInfoMixin;
import org.eclipse.sw360.datahandler.couchdb.DatabaseMixInForChangeLog.ProjectReleaseRelationshipMixin;
import org.eclipse.sw360.datahandler.couchdb.DatabaseMixInForChangeLog.RepositoryMixin;
import org.eclipse.sw360.datahandler.couchdb.DatabaseMixInForChangeLog.VendorMixin;
import org.eclipse.sw360.datahandler.couchdb.DatabaseMixInForChangeLog.ObligationMixin;
import org.eclipse.sw360.datahandler.couchdb.DatabaseMixInForChangeLog.LicenseTypeMixin;
import org.eclipse.sw360.datahandler.couchdb.DatabaseMixInForChangeLog.*;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangedFields;
import org.eclipse.sw360.datahandler.thrift.changelogs.Operation;
import org.eclipse.sw360.datahandler.thrift.changelogs.ReferenceDocData;
import org.eclipse.sw360.datahandler.thrift.components.COTSDetails;
import org.eclipse.sw360.datahandler.thrift.components.ClearingInformation;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.EccInformation;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.Repository;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationStatusInfo;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationList;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.spdx.annotations.Annotations;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.CheckSum;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.Creator;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.ExternalDocumentReferences;
import org.eclipse.sw360.datahandler.thrift.spdx.otherlicensinginformationdetected.OtherLicensingInformationDetected;
import org.eclipse.sw360.datahandler.thrift.spdx.relationshipsbetweenspdxelements.RelationshipsBetweenSPDXElements;
import org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetRange;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.ExternalReference;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageVerificationCode;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseType;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseObligationList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * @author smruti.sahoo@siemens.com
 * @author jaideep.palit@siemens.com
 *
 * Common class for database handlers to put the common/generic logic.
 */

public class DatabaseHandlerUtil {
    private static final Logger log = LogManager.getLogger(DatabaseHandlerUtil.class);
    private static final Logger changelog = LogManager.getLogger("sw360changelog");
    public static final String SEPARATOR = " -> ";
    private static ChangeLogsRepository changeLogRepository;
    private static ObjectMapper mapper = initAndGetObjectMapper();
    public static final String PROPERTIES_FILE_PATH = "/sw360.properties";
    private static final String SVM_JSON_LOG_OUTPUT_LOCATION;
    private static final String SVM_JSON_FILE_PREFIX = "svm_json_output_";
    private static final String SVM_JSON_FILE_SUFFIX = ".json";
    private static final String SVM_JSON_LOG_OUTPUT_FILE_PERMISSION = "rw-------";
    private static final String ATTACHMENT_ID = "attachmentId_";
    private static final String DOCUMENT_ID = "documentId_";
    private static final boolean IS_STORE_ATTACHMENT_TO_FILE_SYSTEM_ENABLED;
    private static final String ATTACHMENT_STORE_FILE_SYSTEM_LOCATION;
    private static final String ATTACHMENT_STORE_FILE_SYSTEM_PERMISSION;
    private static ExecutorService ATTACHMENT_FILE_SYSTEM_STORE_THREAD_POOL = Executors.newFixedThreadPool(5);
    private static final String ATTACHMENT_DELETE_NO_OF_DAYS;
    private static final boolean IS_SW360CHANGELOG_ENABLED;
    private static final String CHANGE_LOG_CONFIG_FILE_PATH;
    private static final String SW360CHANGELOG_OUTPUT_PATH;
    private static boolean isChangeLogDisabledMessageLogged = false;
    private static boolean isLiferayEnvVarNotPresent = true;
    public static final boolean AUTO_SET_ECC_STATUS;

    static {
        Properties props = CommonUtils.loadProperties(DatabaseSettings.class, PROPERTIES_FILE_PATH);
        SVM_JSON_LOG_OUTPUT_LOCATION = props.getProperty("svm.json.log.output.location", "/tmp");
        ATTACHMENT_STORE_FILE_SYSTEM_LOCATION = props.getProperty("attachment.store.file.system.location",
                "/opt/sw360tempattachments");
        ATTACHMENT_STORE_FILE_SYSTEM_PERMISSION = props.getProperty("attachment.store.file.system.permission",
                "rwx------");
        IS_STORE_ATTACHMENT_TO_FILE_SYSTEM_ENABLED = Boolean.parseBoolean(props.getProperty("enable.attachment.store.to.file.system", "false"));
        ATTACHMENT_DELETE_NO_OF_DAYS = props.getProperty("attachemnt.delete.no.of.days",
                "30");
        IS_SW360CHANGELOG_ENABLED = Boolean.parseBoolean(props.getProperty("enable.sw360.change.log", "false"));
        CHANGE_LOG_CONFIG_FILE_PATH = props.getProperty("sw360changelog.config.file.location",
                "/etc/sw360/log4j2.xml");
        SW360CHANGELOG_OUTPUT_PATH = props.getProperty("sw360changelog.output.path",
                "sw360changelog/sw360changelog");
        AUTO_SET_ECC_STATUS = Boolean.parseBoolean(props.getProperty("auto.set.ecc.status", "false"));
    }

    public DatabaseHandlerUtil(DatabaseConnectorCloudant db) {
            changeLogRepository = new ChangeLogsRepository(db);
    }

    private static <T, R> Object[] getCyclicLinkPresenceAndLastElementInCycle(T obj, R handler, User user,
            Map<String, String> linkedPath) throws TException {
        Map linkedElementsMap = null;
        if (obj instanceof Project) {
            Project proj = (Project) obj;
            linkedElementsMap = proj.getLinkedProjects();
        } else if (obj instanceof Release) {
            Release release = (Release) obj;
            linkedElementsMap = release.getReleaseIdToRelationship();
        }

        if (linkedElementsMap != null) {
            Iterator<String> linkedElementIterator = linkedElementsMap.keySet().iterator();
            while (linkedElementIterator.hasNext()) {
                String linkedElementId = linkedElementIterator.next();
                T linkedElement = null;
                String elementFullName = null;
                boolean isAccessibleElement = false;
                String inaccessibleElementLabel = "";
                if (handler instanceof ProjectDatabaseHandler) {
                    ProjectDatabaseHandler projDBHandler = (ProjectDatabaseHandler) handler;
                    Project project = projDBHandler.getProjectById(linkedElementId, user);
                    elementFullName = SW360Utils.printName(project);
                    linkedElement = (T) project;
                    isAccessibleElement = true;
                } else if (handler instanceof ComponentDatabaseHandler) {
                    ComponentDatabaseHandler compDBHandler = (ComponentDatabaseHandler) handler;
                    Release release = compDBHandler.getRelease(linkedElementId, user);
                    elementFullName = SW360Utils.printName(release);
                    linkedElement = (T) release;
                    isAccessibleElement = compDBHandler.isReleaseActionAllowed(release, user, RequestedAction.READ);
                    if (!isAccessibleElement) {
                        inaccessibleElementLabel = SW360Utils.INACCESSIBLE_RELEASE;
                    }
                }

                if (linkedPath.containsKey(linkedElementId)) {
                    return new Object[] { Boolean.TRUE, elementFullName };
                }

                linkedPath.put(linkedElementId, isAccessibleElement ? elementFullName : inaccessibleElementLabel);
                Object[] cyclicLinkPresenceAndLastElementInCycle = getCyclicLinkPresenceAndLastElementInCycle(
                        linkedElement, handler, user, linkedPath);
                boolean isCyclicLinkPresent = (Boolean) cyclicLinkPresenceAndLastElementInCycle[0];

                if (isCyclicLinkPresent) {
                    return cyclicLinkPresenceAndLastElementInCycle;
                }
                linkedPath.remove(linkedElementId);
            }
        }
        return new Object[] { Boolean.FALSE, null };
    }

    public static <T, R> String getCyclicLinkedPath(T obj, R handler, User user) throws TException {
        Map<String, String> linkedPath = new LinkedHashMap<>();
        String firstElementFullName = null;
        String id = null;
        if (obj instanceof Project) {
            Project proj = (Project) obj;
            firstElementFullName = SW360Utils.printName(proj);
            id = proj.getId();
        } else if (obj instanceof Release) {
            Release release = (Release) obj;
            firstElementFullName = SW360Utils.printName(release);
            id = release.getId();
        }
        linkedPath.put(id, firstElementFullName);
        Object[] cyclicLinkPresenceAndLastElementInCycle = getCyclicLinkPresenceAndLastElementInCycle(obj, handler,
                user, linkedPath);
        String cyclicHierarchy = "";
        boolean isCyclicLinkPresent = (Boolean) cyclicLinkPresenceAndLastElementInCycle[0];
        if (isCyclicLinkPresent) {
            String[] arrayOfLinkedPath = linkedPath.values().toArray(new String[0]);
            String lastElementInCycle = (String) cyclicLinkPresenceAndLastElementInCycle[1];
            cyclicHierarchy = String.join(SEPARATOR, arrayOfLinkedPath);
            cyclicHierarchy = cyclicHierarchy.concat(SEPARATOR).concat(lastElementInCycle);
        }
        return cyclicHierarchy;
    }

    public static <T, R extends DatabaseRepositoryCloudantClient<T>> boolean isAllIdInSetExists(Set<String> setOfIds, R repository) {
        long nonExistingIdCount = 0;
        if (setOfIds != null) {
            nonExistingIdCount = setOfIds.stream()
                    .filter(id -> {
                        if(CommonUtils.isNullEmptyOrWhitespace(id))
                           return false;
                        T obj = (T) repository.get(id);
                        return Objects.isNull(obj);
                    }).count();
        }

        if (nonExistingIdCount > 0)
            return false;

        return true;
    }

    public static Set<String> trimSetOfString(Set<String> setOfString) {
        if (setOfString != null) {
            setOfString = setOfString.stream().filter(str -> CommonUtils.isNotNullEmptyOrWhitespace(str))
                    .map(str -> str.trim()).collect(Collectors.toSet());
        }
        return setOfString;
    }

    public static Set<Attachment> trimSetOfAttachement(Set<Attachment> setOfAttachment) {
        if (setOfAttachment != null) {
            setOfAttachment = setOfAttachment.stream().filter(attachment -> attachment != null).map(attachment -> {
                String createdComment = attachment.getCreatedComment();
                if (createdComment != null) {
                    attachment.setCreatedComment(createdComment.trim());
                }
                String checkedComment = attachment.getCheckedComment();
                if (checkedComment != null) {
                    attachment.setCheckedComment(checkedComment.trim());
                }
                return attachment;
            }).collect(Collectors.toSet());
        }
        return setOfAttachment;
    }

    public static Map<String, String> trimMapOfStringKeyStringValue(Map<String, String> mapOfStringKeyStringValue) {
        if (mapOfStringKeyStringValue != null) {
            mapOfStringKeyStringValue = mapOfStringKeyStringValue.entrySet().stream()
                    .filter(entry -> CommonUtils.isNotNullEmptyOrWhitespace(entry.getKey())
                            && CommonUtils.isNotNullEmptyOrWhitespace(entry.getValue()))
                    .collect(Collectors.toMap(entry -> entry.getKey().trim(), entry -> {
                        String value = entry.getValue();
                        if (value != null) {
                            value = value.trim();
                        }
                        return value;
                    }));
        }
        return mapOfStringKeyStringValue;
    }

    public static Map<String, Set<String>> trimMapOfStringKeySetValue(Map<String, Set<String>> mapOfStringKeySetValue) {
        if (mapOfStringKeySetValue != null) {
            mapOfStringKeySetValue = mapOfStringKeySetValue.entrySet().stream()
                    .filter(entry -> CommonUtils.isNotNullEmptyOrWhitespace(entry.getKey())).map(entry -> {
                        Set<String> value = entry.getValue();
                        if (value != null) {
                            Set<String> filteredValue = value.stream()
                                    .filter(valueItem -> CommonUtils.isNotNullEmptyOrWhitespace(valueItem))
                                    .map(valueItem -> valueItem.trim()).collect(Collectors.toSet());
                            entry.setValue(filteredValue);
                        }

                        return entry;
                    }).filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                    .collect(Collectors.toMap(entry -> entry.getKey().trim(), entry -> entry.getValue()));
        }
        return mapOfStringKeySetValue;
    }

    public static <T, R> void trimStringFields(T obj, List<R> listOfStrFields) {
        listOfStrFields.forEach(strField -> {
            if (obj instanceof Component) {
                Component._Fields compField = (Component._Fields) strField;
                Component comp = (Component) obj;
                Object fieldValueObj = comp.getFieldValue(compField);
                if (fieldValueObj instanceof String) {
                    comp.setFieldValue(compField, fieldValueObj.toString().trim());
                }
            } else if (obj instanceof Release) {
                Release._Fields releaseField = (Release._Fields) strField;
                Release release = (Release) obj;
                Object fieldValueObj = release.getFieldValue(releaseField);
                if (fieldValueObj instanceof String) {
                    release.setFieldValue(releaseField, fieldValueObj.toString().trim());
                }
            } else if (obj instanceof Project) {
                Project._Fields projField = (Project._Fields) strField;
                Project proj = (Project) obj;
                Object fieldValueObj = proj.getFieldValue(projField);
                if (fieldValueObj instanceof String) {
                    proj.setFieldValue(projField, fieldValueObj.toString().trim());
                }
            } else if (obj instanceof ClearingInformation) {
                ClearingInformation._Fields clearingInformationField = (ClearingInformation._Fields) strField;
                ClearingInformation clearingInformation = (ClearingInformation) obj;
                Object fieldValueObj = clearingInformation.getFieldValue(clearingInformationField);
                if (fieldValueObj instanceof String) {
                    clearingInformation.setFieldValue(clearingInformationField, fieldValueObj.toString().trim());
                }
            } else if (obj instanceof COTSDetails) {
                COTSDetails._Fields cotsDetailsField = (COTSDetails._Fields) strField;
                COTSDetails cotsDetails = (COTSDetails) obj;
                Object fieldValueObj = cotsDetails.getFieldValue(cotsDetailsField);
                if (fieldValueObj instanceof String) {
                    cotsDetails.setFieldValue(cotsDetailsField, fieldValueObj.toString().trim());
                }
            } else if (obj instanceof EccInformation) {
                EccInformation._Fields eccInformationField = (EccInformation._Fields) strField;
                EccInformation eccInformation = (EccInformation) obj;
                Object fieldValueObj = eccInformation.getFieldValue(eccInformationField);
                if (fieldValueObj instanceof String) {
                    eccInformation.setFieldValue(eccInformationField, fieldValueObj.toString().trim());
                }
            } else if (obj instanceof Package) {
                Package._Fields pkgField = (Package._Fields) strField;
                Package pkg = (Package) obj;
                Object fieldValueObj = pkg.getFieldValue(pkgField);
                if (fieldValueObj instanceof String) {
                    pkg.setFieldValue(pkgField, fieldValueObj.toString().trim());
                }
            }
        });
    }

    /**
     * Register basic informations for the Document.
     */
    public static <T extends TBase<T, ? extends TFieldIdEnum>> ChangeLogs initChangeLogsObj(T newDocVersion,
            String userEdited, String parentDocId, Operation operation, Operation parentOperation) {
        ChangeLogs changeLog = new ChangeLogs();
        changeLog.setUserEdited(userEdited);
        changeLog.setOperation(operation);
        changeLog.setChangeTimestamp(DatabaseHandlerUtil.getTimeStamp());
        changeLog.setParentDocId(parentDocId);
        Map<String, String> info = new LinkedHashMap<String, String>();
        if (newDocVersion instanceof Project) {
            Project newProjVer = (Project) newDocVersion;
            changeLog.setDocumentId(newProjVer.getId());
            changeLog.setDocumentType(newProjVer.getType());
            changeLog.setDbName(DatabaseSettings.COUCH_DB_DATABASE);
        } else if (newDocVersion instanceof ObligationList) {
            ObligationList newOblListVer = (ObligationList) newDocVersion;
            changeLog.setDocumentId(newOblListVer.getId());
            changeLog.setDocumentType(newOblListVer.getType());
            changeLog.setDbName(DatabaseSettings.COUCH_DB_DATABASE);
        } else if (newDocVersion instanceof AttachmentContent) {
            AttachmentContent newAttachmentContentVer = (AttachmentContent) newDocVersion;
            changeLog.setDocumentId(newAttachmentContentVer.getId());
            changeLog.setDocumentType(newAttachmentContentVer.getType());
            changeLog.setDbName(DatabaseSettings.COUCH_DB_ATTACHMENTS);
            info.put(AttachmentContent._Fields.FILENAME.name(), newAttachmentContentVer.getFilename());
            info.put(AttachmentContent._Fields.CONTENT_TYPE.name(), newAttachmentContentVer.getContentType());
        } else if (newDocVersion instanceof Component) {
            Component newCompVer = (Component) newDocVersion;
            changeLog.setDocumentId(newCompVer.getId());
            changeLog.setDocumentType(newCompVer.getType());
            changeLog.setDbName(DatabaseSettings.COUCH_DB_DATABASE);
        } else if (newDocVersion instanceof Release) {
            Release newRelVer = (Release) newDocVersion;
            changeLog.setDocumentId(newRelVer.getId());
            changeLog.setDocumentType(newRelVer.getType());
            changeLog.setDbName(DatabaseSettings.COUCH_DB_DATABASE);
        } else if (newDocVersion instanceof ModerationRequest) {
            ModerationRequest newModReqVer = (ModerationRequest) newDocVersion;
            changeLog.setDocumentId(newModReqVer.getId());
            changeLog.setDocumentType(newModReqVer.getType());
            changeLog.setDbName(DatabaseSettings.COUCH_DB_DATABASE);
        } else if (newDocVersion instanceof SPDXDocument) {
            SPDXDocument newProjVer = (SPDXDocument) newDocVersion;
            changeLog.setDocumentId(newProjVer.getId());
            changeLog.setDocumentType(newProjVer.getType());
            changeLog.setDbName(DatabaseSettings.COUCH_DB_SPDX);
        } else if (newDocVersion instanceof DocumentCreationInformation) {
            DocumentCreationInformation newProjVer = (DocumentCreationInformation) newDocVersion;
            changeLog.setDocumentId(newProjVer.getId());
            changeLog.setDocumentType(newProjVer.getType());
            changeLog.setDbName(DatabaseSettings.COUCH_DB_SPDX);
        } else if (newDocVersion instanceof PackageInformation) {
            PackageInformation newProjVer = (PackageInformation) newDocVersion;
            changeLog.setDocumentId(newProjVer.getId());
            changeLog.setDocumentType(newProjVer.getType());
            changeLog.setDbName(DatabaseSettings.COUCH_DB_SPDX);
        } else if (newDocVersion instanceof Obligation) {
            Obligation newOblVer = (Obligation) newDocVersion;
            changeLog.setDocumentId(newOblVer.getId());
            changeLog.setDocumentType(newOblVer.getType());
            changeLog.setDbName(DatabaseSettings.COUCH_DB_DATABASE);
        } else if (newDocVersion instanceof Package) {
            Package newPkgVer = (Package) newDocVersion;
            changeLog.setDocumentId(newPkgVer.getId());
            changeLog.setDocumentType(newPkgVer.getType());
            changeLog.setDbName(DatabaseSettings.COUCH_DB_DATABASE);
        } else if (newDocVersion instanceof License) {
            License newProjVer = (License) newDocVersion;
            changeLog.setDocumentId(newProjVer.getId());
            changeLog.setDocumentType(newProjVer.getType());
            changeLog.setDbName(DatabaseSettings.COUCH_DB_DATABASE);
        } else if (newDocVersion instanceof LicenseObligationList) {
            LicenseObligationList newProjVer = (LicenseObligationList) newDocVersion;
            changeLog.setDocumentId(newProjVer.getId());
            changeLog.setDocumentType(newProjVer.getType());
            changeLog.setDbName(DatabaseSettings.COUCH_DB_DATABASE);
        }

        log.info("Initialize ChangeLogs for Document Id : " + changeLog.getDocumentId());

        if (parentOperation != null)
            info.put("PARENT_OPERATION", parentOperation.name());
        if (!info.isEmpty())
            changeLog.setInfo(info);
        return changeLog;
    }

    /**
     * Register Changelog for Deleted attachement as part of Document Upadte
     * operation
     */
    public static void populateChangeLogsForAttachmentsDeleted(Set<Attachment> attachmentsBefore,
            Set<Attachment> attachmentsAfter, List<ChangeLogs> referenceDocLogList, String userEdited,
            String parentDocId, Operation parentOperation, AttachmentConnector attachmentConnector,
            boolean deleteAllAttachments) {

        Set<String> attachentContentIdsToBeDeleted = attachmentConnector
                .getAttachentContentIdsToBeDeleted(attachmentsBefore, attachmentsAfter);
        if (deleteAllAttachments) {
            attachentContentIdsToBeDeleted = attachmentConnector.getAttachmentContentIds(attachmentsBefore);
        }
        attachentContentIdsToBeDeleted.stream().forEach(attachmentContentId -> WrappedTException.wrapTException(() -> {
            AttachmentContent attachmentContent = attachmentConnector.getAttachmentContent(attachmentContentId);
            if (attachmentContent == null)
                return;
            ChangeLogs changeLog = initChangeLogsObj(attachmentContent, userEdited, parentDocId, Operation.DELETE,
                    parentOperation);
            referenceDocLogList.add(changeLog);
        }));
    }

    public static String convertObjectToJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Error occured while converting Object to Json : ", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Add Select Logs
     * @param newDocVersion
     * @param userEdited
     * @param attachmentConnector
     */
    public static <T extends TBase> void addSelectLogs(T newDocVersion, String userEdited, AttachmentConnector attachmentConnector) {

        if (!IS_SW360CHANGELOG_ENABLED) {
            if (!isChangeLogDisabledMessageLogged) {
                log.info("sw360change log is disabled");
                isChangeLogDisabledMessageLogged = true;
            }
            return;
        }

        Runnable changeLogRunnable = ()-> {
          try {
              log.info("Generating SelectLogs.");
              ChangeLogs changeLogParent =initChangeLogsObj(newDocVersion, userEdited, null, Operation.CREATE,null);
              Map<String,Object> logMap=new LinkedHashMap();
              logMap.put("type",changeLogParent.getType());
              logMap.put("documentId",changeLogParent.getDocumentId());
              logMap.put("documentType",changeLogParent.getDocumentType());
              logMap.put("dbName",changeLogParent.getDbName());
              logMap.put("operation","SELECT");
              logMap.put("userEdited",changeLogParent.getUserEdited());
              logMap.put("changeTimestamp",changeLogParent.getChangeTimestamp());
              changelog.debug(convertObjectToJson(logMap));
          } catch (Exception exp) {
              log.error("Error occured while creating Select Logs", exp);
          }
        };

        Thread changeLogsThread = new Thread(changeLogRunnable);
        File sw360ChangeLogFileLocation = new File(CHANGE_LOG_CONFIG_FILE_PATH);
        if (sw360ChangeLogFileLocation.exists()) {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            context.setConfigLocation(sw360ChangeLogFileLocation.toURI());
        } else {
            Map<String, String> env = System.getenv();
            if (!env.containsKey("LIFERAY_INSTALL") && isLiferayEnvVarNotPresent) {
                log.info("LIFERAY_INSTALL is not set as environment variable to store the changelog.");
                isLiferayEnvVarNotPresent = false;
                return;
            }
            String LIFERAY_HOME = env.get("LIFERAY_INSTALL");
            configureLog4J(SW360CHANGELOG_OUTPUT_PATH, LIFERAY_HOME);
        }
        changeLogsThread.start();
    }

    /**
     * Add Chaneglogs into the DB
     */
    public <T extends TBase> void addChangeLogs(T newDocVersion, T oldDocVersion, String userEdited,
            Operation operation, AttachmentConnector attachmentConnector, List<ChangeLogs> referenceDocLogList,
            String parentDocId, Operation parentOperation) {
        if (DatabaseSettings.COUCH_DB_DATABASE.contains("test")
                || DatabaseSettings.COUCH_DB_ATTACHMENTS.contains("test")) {
            return;
        }
        Runnable changeLogRunnable = prepareChangeLogRunnable(newDocVersion, oldDocVersion, userEdited, operation,
                attachmentConnector, referenceDocLogList, parentDocId, parentOperation);

        Thread changeLogsThread = new Thread(changeLogRunnable);
        changeLogsThread.start();
    }

    /**
     * Write content to a file
     */
    public static void writeToFile(String content) {
        Runnable fileHandlerRunnable = prepareFileHandlerRunnable(content);
        Thread fileWriterThread = new Thread(fileHandlerRunnable);
        fileWriterThread.start();
    }

    /**
     * Prepare ChangeLog Runnable along with all the Change data.
     */
    private static <T extends TBase> Runnable prepareChangeLogRunnable(T newDocVersion, T oldDocVersion,
            String userEdited, Operation operation, AttachmentConnector attachmentConnector,
            List<ChangeLogs> referenceDocLogList, String parentDocId, Operation parentOperation) {
        return () -> {
            try {
                ChangeLogs changeLogParent = initChangeLogsObj(newDocVersion, userEdited, parentDocId, operation,
                        parentOperation);
                if (oldDocVersion == null) {
                    changeLogsForNewlyCreatedOrDeleted(newDocVersion, changeLogParent, false);
                } else if (newDocVersion == null) {
                    changeLogParent = initChangeLogsObj(oldDocVersion, userEdited, parentDocId, operation,
                            parentOperation);
                    changeLogsForNewlyCreatedOrDeleted(oldDocVersion, changeLogParent, true);
                } else {
                    evaluateAndAddChanges(oldDocVersion, newDocVersion, changeLogParent);

                    if (attachmentConnector != null) {
                        referenceDocChanges(oldDocVersion, newDocVersion, userEdited, referenceDocLogList,
                                attachmentConnector, changeLogParent);
                    }
                }
                changelog.debug(convertObjectToJson(changeLogParent));
                changeLogRepository.add(changeLogParent);
                String changeLogParentId = changeLogParent.getId();
                referenceDocLogList.stream().forEach(referenceDocLog -> {
                    referenceDocLog.setDocumentId(changeLogParentId);
                    changelog.debug(convertObjectToJson(referenceDocLog));
                    try {
                        changeLogRepository.add(referenceDocLog);
                    } catch (SW360Exception e) {
                        log.error("Error occurred while adding change log", e);
                    }
                });
            } catch (Exception exp) {
                log.error("Error occurred while creating Change Logs", exp);
            }
        };
    }

    /**
     * Register basic informations for the Document.
     */
    private static <T extends TBase> void changeLogsForNewlyCreatedOrDeleted(T neworDeletedVersion, ChangeLogs changeLog,
            boolean isDeletion) {

        Set<ChangedFields> changes = new LinkedHashSet<ChangedFields>();
        TFieldIdEnum[] fields = null;
        if (neworDeletedVersion instanceof Project) {
            fields = Project._Fields.values();
        } else if (neworDeletedVersion instanceof ObligationList) {
            fields = ObligationList._Fields.values();
        } else if (neworDeletedVersion instanceof AttachmentUsage) {
            fields = AttachmentUsage._Fields.values();
        } else if (neworDeletedVersion instanceof Component) {
            fields = Component._Fields.values();
        } else if (neworDeletedVersion instanceof Release) {
            fields = Release._Fields.values();
        } else if (neworDeletedVersion instanceof ModerationRequest) {
            fields = ModerationRequest._Fields.values();
        } else if (neworDeletedVersion instanceof SPDXDocument) {
            fields = SPDXDocument._Fields.values();
        } else if (neworDeletedVersion instanceof DocumentCreationInformation) {
            fields = DocumentCreationInformation._Fields.values();
        } else if (neworDeletedVersion instanceof PackageInformation) {
            fields = PackageInformation._Fields.values();
        } else if (neworDeletedVersion instanceof Obligation) {
            fields = Obligation._Fields.values();
        } else if (neworDeletedVersion instanceof Package) {
            fields = Package._Fields.values();
        } else if (neworDeletedVersion instanceof License) {
            fields = License._Fields.values();
        } else if (neworDeletedVersion instanceof LicenseObligationList) {
            fields = LicenseObligationList._Fields.values();
        } else {
            return;
        }
        recordChangesForNeworDeletedDoc(fields, changes, neworDeletedVersion, isDeletion);
        changeLog.setChanges(changes);
    }

    /**
     * Evaluate and record the changes.
     */
    private static <T extends TBase<T, ? extends TFieldIdEnum>> void evaluateAndAddChanges(T oldVersion, T newVersion,
            ChangeLogs changeLog) {
        Set<ChangedFields> changes = new LinkedHashSet<ChangedFields>();
        TFieldIdEnum[] fields = null;
        if (newVersion instanceof Project) {
            fields = Project._Fields.values();
        } else if (newVersion instanceof ObligationList) {
            fields = ObligationList._Fields.values();
        } else if (newVersion instanceof Component) {
            fields = Component._Fields.values();
        } else if (newVersion instanceof Release) {
            fields = Release._Fields.values();
        } else if (newVersion instanceof ModerationRequest) {
            fields = ModerationRequest._Fields.values();
        } else if (newVersion instanceof SPDXDocument) {
            fields = SPDXDocument._Fields.values();
        } else if (newVersion instanceof DocumentCreationInformation) {
            fields = DocumentCreationInformation._Fields.values();
        } else if (newVersion instanceof PackageInformation) {
            fields = PackageInformation._Fields.values();
        } else if (newVersion instanceof Obligation) {
            fields = Obligation._Fields.values();
        } else if (newVersion instanceof Package) {
            fields = Package._Fields.values();
        } else if (newVersion instanceof License) {
            fields = License._Fields.values();
        } else if (newVersion instanceof LicenseObligationList) {
            fields = LicenseObligationList._Fields.values();
        } else {
            return;
        }

        recordChangesForExistingDoc(fields, changes, newVersion, oldVersion);

        changeLog.setChanges(changes);
    }

    /**
     * Register Reference Doc changes ad part of Parent Doc update
     */
    private static <T extends TBase> void referenceDocChanges(T oldDocVersion, T newDocVersion, String userEdited,
            List<ChangeLogs> referenceDocLogList, AttachmentConnector attachmentConnector, ChangeLogs changeLogParent) {

        if (newDocVersion instanceof Project || newDocVersion instanceof Component
                || newDocVersion instanceof Release) {
            getChangeLogsForAttachments(oldDocVersion, newDocVersion, userEdited, referenceDocLogList,
                    attachmentConnector);
        }

        Set<ReferenceDocData> referenceDocDataSet = new HashSet<ReferenceDocData>();
        referenceDocLogList.stream().forEach(refLog -> {
            ReferenceDocData refDocData = new ReferenceDocData();
            refDocData.setDbName(refLog.getDbName());
            refDocData.setRefDocId(refLog.getDocumentId());
            refDocData.setRefDocOperation(refLog.getOperation());
            refDocData.setRefDocType(refLog.getDocumentType());
            referenceDocDataSet.add(refDocData);
        });

        changeLogParent.setReferenceDoc(referenceDocDataSet);
    }

    /**
     * Returns whether value of a particular field is changed
     */
    private static boolean isChanged(Object actualValue, Object newValue) {
        if (actualValue instanceof String && newValue instanceof String) {
            return !actualValue.equals(newValue);
        } else if (actualValue instanceof Collection && newValue instanceof Collection) {
            Collection<?> actualValueCollection = (Collection<?>) actualValue;
            Collection<?> newValueCollection = (Collection<?>) newValue;
            return !isTwoCollectionSame(actualValueCollection, newValueCollection);
        } else if (actualValue instanceof Map && newValue instanceof Map) {
            Map<?, ?> actualValueMap = (Map<?, ?>) actualValue;
            Map<?, ?> newValueMap = (Map<?, ?>) newValue;
            Set<?> actualValueMapKeySet = actualValueMap.keySet();
            Set<?> newValueMapKeySet = newValueMap.keySet();
            if (isTwoCollectionSame(actualValueMapKeySet, newValueMapKeySet)) {
                long diffCount = newValueMap.entrySet().stream()
                        .filter(entry -> !actualValueMap.containsKey(entry.getKey())
                                || !actualValueMap.get(entry.getKey()).equals(entry.getValue()))
                        .count();
                if (diffCount == 0)
                    return false;
            }
            return true;
        } else if (actualValue instanceof Boolean && newValue instanceof Boolean) {
            Boolean actualValueBool = (Boolean) actualValue;
            Boolean newValueBool = (Boolean) newValue;
            return !actualValueBool.equals(newValueBool);
        } else if (actualValue instanceof Number && newValue instanceof Number) {
            Number actualValueNumber = (Number) actualValue;
            Number newValueNumber = (Number) newValue;
            return !actualValueNumber.equals(newValueNumber);
        } else if (actualValue != null && newValue != null) {
            return !actualValue.equals(newValue);
        }

        return false;
    }

    /**
     * Returns whether two collections are equivalent
     */
    private static boolean isTwoCollectionSame(Collection<?> col1, Collection<?> col2) {
        return (col1.size() == col2.size()) && (col1.containsAll(col2));
    }

    private static String getTimeStamp() {
        SimpleDateFormat timestampPattern = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
        Date timeNow = new Date(System.currentTimeMillis());
        return timestampPattern.format(timeNow);
    }

    /**
     * Record changes for Newly created or Deleted Document.
     */
    private static <T extends TBase<T, TFieldIdEnum>> void recordChangesForNeworDeletedDoc(TFieldIdEnum[] fields,
            Set<ChangedFields> changes, T newOrDeletedDoc, boolean isDeletion) {
        Arrays.stream(fields).forEach(field -> {
            Object newOrDeletedValueObj = newOrDeletedDoc.getFieldValue(field);

            if (newOrDeletedValueObj == null || field.getFieldName().equals("type") || field.getFieldName().equals("id")
                    || field.getFieldName().equals("documentState")) {
                return;
            }

            ChangedFields changedField = new ChangedFields();
            changedField.setFieldName(field.getFieldName());
            if (isDeletion) {
                changedField.setFieldValueOld(convertObjectToJson(newOrDeletedValueObj));
            } else {
                changedField.setFieldValueNew(convertObjectToJson(newOrDeletedValueObj));
            }
            changes.add(changedField);
        });
    }

    /**
     * Record changes for Existing Document.
     */
    private static <T extends TBase> void recordChangesForExistingDoc(TFieldIdEnum[] fields, Set<ChangedFields> changes,
            T newDoc, T oldDoc) {
        log.info("Record Changes For Existing Doc.");
        Arrays.stream(fields).forEach(field -> {
            Object newProjectValueObj = newDoc.getFieldValue(field);
            Object oldProjectValueObj = oldDoc.getFieldValue((TFieldIdEnum) field);

            if (newProjectValueObj == null && oldProjectValueObj == null) {
                return;
            }

            String oldFieldValue = convertObjectToJson(oldProjectValueObj);
            String newFieldValue = convertObjectToJson(newProjectValueObj);
            if (newProjectValueObj != null && oldProjectValueObj != null) {
                if (DatabaseHandlerUtil.isChanged(oldProjectValueObj, newProjectValueObj)) {
                    ChangedFields changedField = new ChangedFields();
                    changedField.setFieldName(field.getFieldName());
                    changedField.setFieldValueOld(oldFieldValue);
                    changedField.setFieldValueNew(newFieldValue);
                    changes.add(changedField);
                }
            } else {
                ChangedFields changedField = new ChangedFields();
                changedField.setFieldName(field.getFieldName());
                if (oldProjectValueObj != null) {
                    changedField.setFieldValueOld(oldFieldValue);
                }

                if (newProjectValueObj != null) {
                    changedField.setFieldValueNew(newProjectValueObj == null ? null : newFieldValue);
                }

                changes.add(changedField);
            }
        });
    }

    /**
     * Get the Changelogs for newly added Attachment on Document update.
     */
    private static <T extends TBase> void getChangeLogsForAttachments(T oldDocVersion, T newDocVersion,
            String userEdited, List<ChangeLogs> referenceDocLogList, AttachmentConnector attachmentConnector) {
        log.info("Initialize ChangeLogs for Attachments.");
        Set<Attachment> attachmentsAfter = null;
        Set<Attachment> attachmentsBefore = null;
        Operation parentOperation = null;
        String id = null;
        if (newDocVersion instanceof Project) {
            Project newProjVersion = (Project) newDocVersion;
            parentOperation = Operation.PROJECT_UPDATE;
            attachmentsAfter = newProjVersion.getAttachments();
            id = newProjVersion.getId();
            Project oldProjVersion = (Project) oldDocVersion;
            attachmentsBefore = oldProjVersion.getAttachments();
        } else if (newDocVersion instanceof Component) {
            Component newCompVersion = (Component) newDocVersion;
            parentOperation = Operation.COMPONENT_UPDATE;
            attachmentsAfter = newCompVersion.getAttachments();
            id = newCompVersion.getId();
            Component oldCompVersion = (Component) oldDocVersion;
            attachmentsBefore = oldCompVersion.getAttachments();
        } else if (newDocVersion instanceof Release) {
            Release newReleaseVersion = (Release) newDocVersion;
            parentOperation = Operation.RELEASE_UPDATE;
            attachmentsAfter = newReleaseVersion.getAttachments();
            id = newReleaseVersion.getId();
            Release oldReleaseVersion = (Release) oldDocVersion;
            attachmentsBefore = oldReleaseVersion.getAttachments();
        }

        String idForLambdaExpr = id;
        Operation parentOperationForLambdaExpr = parentOperation;
        Set<String> newAttachmentContentIds = attachmentConnector.getAttachmentContentIds(attachmentsAfter);

        newAttachmentContentIds.removeAll(attachmentConnector.getAttachmentContentIds(attachmentsBefore));

        newAttachmentContentIds.stream().forEach(attachmentContentId -> WrappedTException.wrapTException(() -> {
            AttachmentContent attachmentContent = attachmentConnector.getAttachmentContent(attachmentContentId);
            if (attachmentContent == null)
                return;
            ChangeLogs changeLog = initChangeLogsObj(attachmentContent, userEdited, idForLambdaExpr, Operation.CREATE,
                    parentOperationForLambdaExpr);
            referenceDocLogList.add(changeLog);
        }));
    }

    /**
     * Added Mixin annotation, to prevent registering unwanted fields into changelog DB , like setCheckStatus
     */
    private static ObjectMapper initAndGetObjectMapper() {
        if (mapper == null) {
            mapper = new ObjectMapper();
            mapper.addMixInAnnotations(Attachment.class, AttachmentMixin.class);
            mapper.addMixInAnnotations(ObligationStatusInfo.class, ObligationStatusInfoMixin.class);
            mapper.addMixInAnnotations(ClearingInformation.class, ClearingInformationMixin.class);
            mapper.addMixInAnnotations(COTSDetails.class, COTSDetailsMixin.class);
            mapper.addMixInAnnotations(EccInformation.class, EccInformationMixin.class);
            mapper.addMixInAnnotations(Vendor.class, VendorMixin.class);
            mapper.addMixInAnnotations(Repository.class, RepositoryMixin.class);
            mapper.addMixInAnnotations(ProjectProjectRelationship.class, ProjectProjectRelationshipMixin.class);
            mapper.addMixInAnnotations(ProjectReleaseRelationship.class, ProjectReleaseRelationshipMixin.class);
            mapper.addMixInAnnotations(CheckSum.class, CheckSumMixin.class);
            mapper.addMixInAnnotations(Annotations.class, AnnotationsMixin.class);
            mapper.addMixInAnnotations(ExternalDocumentReferences.class, ExternalDocumentReferencesMixin.class);
            mapper.addMixInAnnotations(Creator.class, CreatorMixin.class);
            mapper.addMixInAnnotations(OtherLicensingInformationDetected.class, OtherLicensingInformationDetectedMixin.class);
            mapper.addMixInAnnotations(PackageVerificationCode.class, PackageVerificationCodeMixin.class);
            mapper.addMixInAnnotations(ExternalReference.class, ExternalReferenceMixin.class);
            mapper.addMixInAnnotations(RelationshipsBetweenSPDXElements.class, RelationshipsBetweenSPDXElementsMixin.class);
            mapper.addMixInAnnotations(SnippetInformation.class, SnippetInformationMixin.class);
            mapper.addMixInAnnotations(SnippetRange.class, SnippetRangeMixin.class);
            mapper.addMixInAnnotations(Obligation.class, ObligationMixin.class);
            mapper.addMixInAnnotations(LicenseType.class, LicenseTypeMixin.class);
        }
        return mapper;
    }

    /**
     * Prepare Runnable to create file , set permissions and write to file
     */
    private static Runnable prepareFileHandlerRunnable(String content) {
        return () -> {
            try {
                log.info("Preparing to write content to file");
                StringBuilder fileNameSb = new StringBuilder(SVM_JSON_FILE_PREFIX)
                        .append(getTimeStamp().replaceAll(" ", "_").replaceAll(":", "-")).append(SVM_JSON_FILE_SUFFIX);
                Path outputFile = Paths.get(SVM_JSON_LOG_OUTPUT_LOCATION, fileNameSb.toString());
                if (!Files.exists(outputFile)) {
                    Set<PosixFilePermission> perms = PosixFilePermissions.fromString(SVM_JSON_LOG_OUTPUT_FILE_PERMISSION);
                    FileAttribute<Set<PosixFilePermission>> fileAttribute = PosixFilePermissions.asFileAttribute(perms);
                    Files.createFile(outputFile, fileAttribute);
                    try (BufferedWriter bw = Files.newBufferedWriter(outputFile, StandardOpenOption.WRITE)) {
                        bw.write(content);
                        bw.flush();
                    }
                }
            } catch (Exception exp) {
                log.warn("Error occured while writing to a file", exp);
            }
        };
    }

    public static String trimProjectTag(String projectTag) {
        if (projectTag != null) {
            projectTag = projectTag.replaceAll(",$", "");
        }
        return projectTag;
    }

    /**
     * Execute Runnable To Save Attachment To File System
     */
    private static void executeRunnableToSaveAttachmentToFileSystem(InputStream content, String userEmail,
            String documentId, String attachmentId, String fileName) {
        Runnable fileHandlerRunnable = prepareFileHandlerRunnable(content, userEmail, documentId, attachmentId,
                fileName);
        ATTACHMENT_FILE_SYSTEM_STORE_THREAD_POOL.submit(fileHandlerRunnable);
    }

    /**
     * Prepare Runnable to save attachment , set permissions and write to file
     */
    private static Runnable prepareFileHandlerRunnable(InputStream content, String userEmail, String documentId,
            String attachmentId, String fileName) {
        return () -> {
            try {
                Path outputDir = Paths.get(ATTACHMENT_STORE_FILE_SYSTEM_LOCATION, userEmail, DOCUMENT_ID + documentId,
                        ATTACHMENT_ID + attachmentId);
                Path outputFile = Paths.get(fileName);
                Path outputFilePath = outputDir.resolve(outputFile);
                log.info("Preparing to store attachment in file system" + outputFilePath);
                if (!Files.exists(outputFile)) {
                    Set<PosixFilePermission> perms = PosixFilePermissions
                            .fromString(ATTACHMENT_STORE_FILE_SYSTEM_PERMISSION);
                    FileAttribute<Set<PosixFilePermission>> fileAttribute = PosixFilePermissions.asFileAttribute(perms);
                    Files.createDirectories(outputDir, fileAttribute);
                    Files.createFile(outputFilePath, fileAttribute);
                    try (OutputStream os = Files.newOutputStream(outputFilePath, StandardOpenOption.WRITE)) {
                        os.write(content.readAllBytes());
                        os.flush();
                        content.close();
                    }
                }
            } catch (Exception exp) {
                log.warn("Error occured while writing to a file", exp);
            }
        };
    }

    /**
     * Compare and find newly added attachments to be saved to File System
     */
    public static void saveAttachmentInFileSystem(AttachmentConnector attachmentConnector, Set<Attachment> before,
            Set<Attachment> after, String userEmail, String documentId) {
        if (!IS_STORE_ATTACHMENT_TO_FILE_SYSTEM_ENABLED) {
            log.debug("Store attachment to file system is disabled");
            return;
        }

        after = after == null ? new HashSet<>() : new HashSet<>(after);
        before = before == null ? new HashSet<>() : new HashSet<>(before);
        after.removeAll(before);
        after.stream().filter(att -> CommonUtils.isNotNullEmptyOrWhitespace(att.getAttachmentContentId()))
                .forEach(att -> {
                    String attachmentContentId = att.getAttachmentContentId();
                    AttachmentContent attachmentContent = null;
                    try {
                        attachmentContent = attachmentConnector.getAttachmentContent(attachmentContentId);
                    } catch (SW360Exception e) {
                        log.error(
                                "Error occured while fetching Attachment content. During Update Project to store Attachment in fileSystem",
                                e);
                    }
                    if (attachmentContent == null)
                        return;
                    executeRunnableToSaveAttachmentToFileSystem(
                            attachmentConnector.readAttachmentStream(attachmentContent), userEmail, documentId,
                            attachmentContentId, att.getFilename());
                });
    }

    public static RequestStatus deleteOldAttachmentFromFileSystem() {
        int noOfDays = Integer.parseInt(ATTACHMENT_DELETE_NO_OF_DAYS);
        RequestStatus status = null;
        LocalDate todayDate = LocalDate.now();
        LocalDate thresholdDateForAttachmentDelete = todayDate.minusDays(noOfDays);
        Date thresholdDate = Date.from(thresholdDateForAttachmentDelete.atStartOfDay(ZoneId.systemDefault()).toInstant());
        try {
            deleteAttachmentAndDirectory(ATTACHMENT_STORE_FILE_SYSTEM_LOCATION, thresholdDate);
            status = RequestStatus.SUCCESS;
        } catch (IOException e) {
            log.error("Unable to delete attachment. ", e);
            status = RequestStatus.FAILURE;
        }
        return status;
    }

    public static void deleteAttachmentAndDirectory(String directoryFilePath, Date thresholdDate) throws IOException {
        Path directory = Paths.get(directoryFilePath);
        if (Files.exists(directory)) {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    File file = path.toFile();
                    long fileLastModifiedDate = file.lastModified();
                    Date modifieddate = new Date(fileLastModifiedDate);
                    if (thresholdDate.after(modifieddate)) {
                        Files.delete(path);
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path directory, IOException ioException) throws IOException {
                    DirectoryStream<Path> stream = Files.newDirectoryStream(directory);
                    boolean isFolderEmpty = !stream.iterator().hasNext();
                    if (isFolderEmpty) {
                        Files.delete(directory);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private static void configureLog4J(String outputpath, String liferayhome) {
	        ConfigurationBuilder< BuiltConfiguration > builder = ConfigurationBuilderFactory.newConfigurationBuilder();
	        builder.setStatusLevel(Level.WARN);
	        builder.setConfigurationName("RollingBuilder");
	        // create a console appender
	        AppenderComponentBuilder appenderBuilder = builder.newAppender("Stdout", "Console").addAttribute("target",
		    ConsoleAppender.Target.SYSTEM_OUT);
	        appenderBuilder.add(builder.newLayout("PatternLayout")
			.addAttribute("pattern", "%d [%t] %-5level: %msg%n%throwable"));
	        builder.add(appenderBuilder);
	        // create a rolling file appender
	        LayoutComponentBuilder layoutBuilder = builder.newLayout("PatternLayout")
	        .addAttribute("pattern", "%d [%t] %-5level: %msg%n");
	        ComponentBuilder<?> triggeringPolicy = builder.newComponent("Policies")
	        .addComponent(builder.newComponent("CronTriggeringPolicy").addAttribute("schedule", "0 0 0 * * ?"))
	        .addComponent(builder.newComponent("SizeBasedTriggeringPolicy").addAttribute("size", "10M"));
	        appenderBuilder = builder.newAppender("ChangeLogFile", "RollingFile")
		    .addAttribute("fileName", liferayhome + outputpath +".log")
		    .addAttribute("filePattern", liferayhome + outputpath + "-%d{MM-dd-yy}.log.gz")
	        .add(layoutBuilder)
	        .addComponent(triggeringPolicy);
	        builder.add(appenderBuilder);
	        // create the new logger sw360changelog
	        builder.add( builder.newLogger("sw360changelog", Level.DEBUG)
	        .add( builder.newAppenderRef("ChangeLogFile")));
	        builder.add( builder.newRootLogger(Level.WARN)
	       .add( builder.newAppenderRef("ChangeLogFile")));
	       Configurator.reconfigure(builder.build());
       }

    public static File saveAsTempFile(InputStream inputStream, String prefix, String suffix) throws IOException {
        final File tempFile = File.createTempFile(prefix, suffix);
        tempFile.deleteOnExit();
        // Set append to false, overwrite if file existed
        try (FileOutputStream outputStream = new FileOutputStream(tempFile, false)) {
            IOUtils.copy(inputStream, outputStream);
        }
        return tempFile;
    }
}

