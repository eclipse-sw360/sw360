/*
 * Copyright Siemens AG, 2013-2019. Part of the SW360 Portal Project.
 * With contributions by Siemens Healthcare Diagnostics Inc, 2018.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.cloud.cloudant.v1.Cloudant;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.thrift.TException;
import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.cyclonedx.CycloneDxBOMExporter;
import org.eclipse.sw360.cyclonedx.CycloneDxBOMImporter;
import org.eclipse.sw360.datahandler.businessrules.ReleaseClearingStateSummaryComputer;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.*;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.couchdb.AttachmentStreamConnector;
import org.eclipse.sw360.datahandler.entitlement.ProjectModerator;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.permissions.ProjectPermissions;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.thrift.changelogs.Operation;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.packages.PackageService;
import org.eclipse.sw360.datahandler.thrift.projects.*;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.ProjectVulnerabilityRating;
import org.eclipse.sw360.mail.MailConstants;
import org.eclipse.sw360.mail.MailUtil;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.eclipse.sw360.spdx.SpdxBOMImporter;
import org.eclipse.sw360.spdx.SpdxBOMImporterSink;
import org.jetbrains.annotations.NotNull;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.SpdxModelFactory;
import org.spdx.library.model.v2.SpdxConstantsCompatV2;
import org.spdx.library.model.v2.SpdxDocument;
import org.spdx.library.model.v2.SpdxPackage;
import org.spdx.tools.InvalidFileNameException;
import org.spdx.tools.SpdxToolsHelper;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.datahandler.common.CommonUtils.*;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertId;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.common.SW360Assert.fail;
import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.IS_PACKAGE_PORTLET_ENABLED;
import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.MAINLINE_STATE_ENABLED_FOR_USER;
import static org.eclipse.sw360.datahandler.common.SW360Utils.getBUFromOrganisation;
import static org.eclipse.sw360.datahandler.common.SW360Utils.getCreatedOn;
import static org.eclipse.sw360.datahandler.common.SW360Utils.printName;
import static org.eclipse.sw360.datahandler.common.WrappedException.wrapSW360Exception;
import static org.eclipse.sw360.datahandler.common.WrappedException.wrapTException;
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;
import org.eclipse.sw360.exporter.ProjectExporter;
import java.nio.ByteBuffer;

/**
 * Class for accessing the CouchDB database
 *
 * @author cedric.bodet@tngtech.com
 * @author daniele.fognini@tngtech.com
 * @author alex.borodin@evosoft.com
 * @author thomas.maier@evosoft.com
 * @author ksoranko@verifa.io
 */
public class ProjectDatabaseHandler extends AttachmentAwareDatabaseHandler {

    private static final String PROJECTS = "projects";
    private static final Logger log = LogManager.getLogger(ProjectDatabaseHandler.class);
    private static final int DELETION_SANITY_CHECK_THRESHOLD = 5;
    private static final String DUMMY_NEW_PROJECT_ID = "newproject";
    public static final int SVMML_JSON_LOG_CUTOFF_LENGTH = 3000;
    private static final boolean WITH_ALL_RELEASES = true;
    private static final boolean WITH_ROOT_RELEASES_ONLY = false;

    private ExecutorService projectExecutor;

    private final ProjectRepository repository;
    private final ProjectVulnerabilityRatingRepository pvrRepository;
    private final ObligationListRepository obligationRepository;
    private final ProjectModerator moderator;
    private final AttachmentConnector attachmentConnector;
    private final ComponentDatabaseHandler componentDatabaseHandler;
    private final PackageDatabaseHandler packageDatabaseHandler;
    private final PackageRepository packageRepository;

    private static final Pattern PLAUSIBLE_GID_REGEXP = Pattern.compile("^[zZ].{7}$");
    private final RelationsUsageRepository relUsageRepository;
    private final ReleaseRepository releaseRepository;
    private final VendorRepository vendorRepository;
    private DatabaseHandlerUtil dbHandlerUtil;
    private final MailUtil mailUtil = new MailUtil();
    private static final ObjectMapper mapper = new ObjectMapper();
    // this caching structure is only used for filling clearing state summaries and
    // should be avoided anywhere else so that no other outdated information will be
    // provided
    // for the clearing state it is
    // 1. necessary because they take a long time to get calculated which might be a
    // good reason for service clients to query not for all projects at once, but to
    // divide the queries in subsets of project (and we do not want to load the
    // whole project map for each of this subset queries)
    // 2. okay because the data normally "only moves forward", meaning that already
    // approved clearing reports will not be revoked. So worst case of caching is
    // that outdated data is provided, not wrong one (as it would be okay to see a
    // cleared project that is displayed as not yet cleared - but it won't be okay
    // to see a uncleared project that is displayed as cleared but isn't anymore)
    private static final java.time.Duration ALL_PROJECTS_ID_MAP_CACHE_LIFETIME = java.time.Duration.ofMinutes(2);
    private static final ImmutableList<Project._Fields> listOfStringFieldsInProjToTrim = ImmutableList.of(
            Project._Fields.NAME, Project._Fields.DESCRIPTION, Project._Fields.VERSION, Project._Fields.DOMAIN,
            Project._Fields.BUSINESS_UNIT, Project._Fields.TAG, Project._Fields.PROJECT_RESPONSIBLE,
            Project._Fields.LEAD_ARCHITECT, Project._Fields.PROJECT_OWNER, Project._Fields.OWNER_ACCOUNTING_UNIT,
            Project._Fields.OWNER_GROUP, Project._Fields.OWNER_COUNTRY, Project._Fields.PREEVALUATION_DEADLINE,
            Project._Fields.SYSTEM_TEST_START, Project._Fields.SYSTEM_TEST_END, Project._Fields.DELIVERY_START,
            Project._Fields.CLEARING_SUMMARY, Project._Fields.SPECIAL_RISKS_OSS, Project._Fields.GENERAL_RISKS3RD_PARTY,
            Project._Fields.SPECIAL_RISKS3RD_PARTY, Project._Fields.DELIVERY_CHANNELS,
            Project._Fields.REMARKS_ADDITIONAL_REQUIREMENTS, Project._Fields.OBLIGATIONS_TEXT,
            Project._Fields.LICENSE_INFO_HEADER_TEXT);
    private Map<String, Project> cachedAllProjectsIdMap;
    private Instant cachedAllProjectsIdMapLoadingInstant;

    public ProjectDatabaseHandler(Cloudant client, String dbName, String attachmentDbName) throws MalformedURLException {
        this(client, dbName, attachmentDbName, new ProjectModerator(),
                new ComponentDatabaseHandler(client,dbName,attachmentDbName),
                new PackageDatabaseHandler(client, dbName, DatabaseSettings.COUCH_DB_CHANGE_LOGS, attachmentDbName),
                new AttachmentDatabaseHandler(client, dbName, attachmentDbName));
    }

    public ProjectDatabaseHandler(Cloudant client, String dbName, String changeLogDbName, String attachmentDbName) throws MalformedURLException {
        this(client, dbName, changeLogDbName, attachmentDbName, new ProjectModerator(),
                new ComponentDatabaseHandler(client, dbName, attachmentDbName),
                new PackageDatabaseHandler(client, dbName, changeLogDbName, attachmentDbName),
                new AttachmentDatabaseHandler(client, dbName, attachmentDbName));
    }

    @VisibleForTesting
    public ProjectDatabaseHandler(Cloudant client, String dbName, String changeLogsDbName, String attachmentDbName, ProjectModerator moderator,
                                  ComponentDatabaseHandler componentDatabaseHandler, PackageDatabaseHandler packageDatabaseHandler,
                                  AttachmentDatabaseHandler attachmentDatabaseHandler) throws MalformedURLException {
        this(client, dbName, attachmentDbName, moderator, componentDatabaseHandler, packageDatabaseHandler, attachmentDatabaseHandler);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(client, changeLogsDbName);
        this.dbHandlerUtil = new DatabaseHandlerUtil(db);
    }

    @VisibleForTesting
    public ProjectDatabaseHandler(Cloudant client, String dbName, String attachmentDbName, ProjectModerator moderator,
                                  ComponentDatabaseHandler componentDatabaseHandler, PackageDatabaseHandler packageDatabaseHandler,
                                  AttachmentDatabaseHandler attachmentDatabaseHandler) throws MalformedURLException {
        super(attachmentDatabaseHandler);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(client, dbName);

        // Create the repositories
        repository = new ProjectRepository(db);
        pvrRepository = new ProjectVulnerabilityRatingRepository(db);
        obligationRepository = new ObligationListRepository(db);
        relUsageRepository = new RelationsUsageRepository(db);
        vendorRepository = new VendorRepository(db);
        releaseRepository = new ReleaseRepository(db, vendorRepository);
        packageRepository = new PackageRepository(db);

        // Create the moderator
        this.moderator = moderator;

        // Create the attachment connector
        attachmentConnector = new AttachmentConnector(client, attachmentDbName, Duration.durationOf(30, TimeUnit.SECONDS));

        this.componentDatabaseHandler = componentDatabaseHandler;
        this.packageDatabaseHandler = packageDatabaseHandler;
        DatabaseConnectorCloudant dbChangelogs = new DatabaseConnectorCloudant(client, DatabaseSettings.COUCH_DB_CHANGE_LOGS);
        this.dbHandlerUtil = new DatabaseHandlerUtil(dbChangelogs);
    }

    /////////////////////
    // SUMMARY GETTERS //
    /////////////////////

    public List<Project> getMyProjectsSummary(String user) {
        return repository.getMyProjectsSummary(user);
    }

    public List<Project> getBUProjectsSummary(String organisation) {
        return repository.getBUProjectsSummary(organisation);
    }

    public List<Project> getAccessibleProjectsSummary(User user) {
        return repository.getAccessibleProjectsSummary(user, vendorRepository);
    }

    public Map<PaginationData, List<Project>> getAccessibleProjectsSummary(User user, PaginationData pageData) {
        return repository.getAccessibleProjectsSummary(user, pageData);
    }

    public List<Project> searchByName(String name, User user) {
        return repository.searchByName(name, user);
    }

    public Map<PaginationData, List<Project>> searchProjectByNamePrefixPaginated(User user, String name, PaginationData pageData) {
        return repository.searchProjectByNamePrefixPaginated(user, name, pageData);
    }

    public Map<PaginationData, List<Project>> searchProjectByExactNamePaginated(User user, String name, PaginationData pageData) {
        return repository.searchProjectByExactNamePaginated(user, name, pageData);
    }

    public Map<PaginationData, List<Project>> searchAccessibleProjectByExactValues(Map<String, Set<String>> subQueryRestrictions, User user, PaginationData pageData) {
        return repository.searchAccessibleProjectByExactValues(subQueryRestrictions, user, pageData);
    }

    /////////////////////////////
    // CREATE CLEARING REQUEST //
    /////////////////////////////

    public AddDocumentRequestSummary createClearingRequest(ClearingRequest clearingRequest, User user, String projectUrl) throws SW360Exception {
        Project project = getProjectById(clearingRequest.getProjectId(), user);
        AddDocumentRequestSummary requestSummary = new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.FAILURE);

        if (!isWriteActionAllowedOnProject(project, user)) {
            return requestSummary.setMessage("You do not have WRITE access to the project");
        }

        if (CommonUtils.isNotNullEmptyOrWhitespace(project.getClearingRequestId())) {
            log.warn("Clearing request is already present for the project: " + project.getId());
            return requestSummary.setRequestStatus(AddDocumentRequestStatus.DUPLICATE)
                    .setId(project.getClearingRequestId()).setMessage("Clearing request already present for project");
        }

        if (!(ProjectClearingState.CLOSED.equals(project.getClearingState()) || Visibility.PRIVATE.equals(project.getVisbility()))) {
            clearingRequest.setProjectBU(project.getBusinessUnit());
            String crId = moderator.createClearingRequest(clearingRequest, user);
            if (CommonUtils.isNotNullEmptyOrWhitespace(crId)) {
                project.setClearingRequestId(crId);
                clearingRequest.setId(crId);
                updateProject(project, user);
                sendMailForNewClearing(project, projectUrl, clearingRequest, user);
                //set requestSummary message that creation of CR was successful
                requestSummary.setMessage("Clearing request created successfully");
                return requestSummary.setRequestStatus(AddDocumentRequestStatus.SUCCESS).setId(project.getClearingRequestId());
            } else {
                log.error("Failed to create clearing request for project: " + project.getId());
            }
        } else {
            log.error("Cannot create clearing request for closed or private project: " + project.getId());
        }
        return requestSummary.setMessage("Failed to create clearing request");
    }

    private boolean isWriteActionAllowedOnProject(Project project, User user) {
        return makePermission(project, user).isActionAllowed(RequestedAction.WRITE);
    }

    public RequestStatus sendEmailForClearingRequestUpdate(ClearingRequest clearingRequest, String projectUrl, User user) throws SW360Exception {
        Project project = getProjectById(clearingRequest.getProjectId(), user);
        if (CommonUtils.isNotNullEmptyOrWhitespace(project.getClearingRequestId())) {
            switch (clearingRequest.getClearingState()) {
            case IN_PROGRESS:
            case IN_QUEUE:
            case ACCEPTED:
            case AWAITING_RESPONSE:
                sendMailForUpdatedCR(project, projectUrl, clearingRequest, user);
                break;

            case CLOSED:
                sendMailForClosedOrRejectedCR(project, clearingRequest, user, true);
                break;

            case NEW:
                sendMailForUpdatedCR(project, projectUrl, clearingRequest, user);
                break;

            case REJECTED:
                sendMailForClosedOrRejectedCR(project, clearingRequest, user, false);
                break;

            default:
                break;
            }
            return RequestStatus.SUCCESS;
        }
        log.error("Failed to send email for change in clearing request, projectId: " + project.getId());
        return RequestStatus.FAILURE;
    }

    ////////////////////////////
    // GET INDIVIDUAL OBJECTS //
    ////////////////////////////

    public void addSelectLogs(Project project, User user) {

        DatabaseHandlerUtil.addSelectLogs(project, user.getEmail(), attachmentConnector);
    }

    public Project getProjectById(String id, User user) throws SW360Exception {
        Project project = repository.get(id);
        assertNotNull(project);

        if(!makePermission(project, user).isActionAllowed(RequestedAction.READ)) {
            throw fail(403, "User: %s is not allowed to view the requested project: %s", user.getEmail(), project.getId());
        }
        vendorRepository.fillVendor(project);
        return project;
    }

    public Project getProjectByIdIgnoringVisibility(String id) throws SW360Exception {
        Project project = repository.get(id);
        assertNotNull(project);
        return project;
    }

    ////////////////////////////
    // ADD INDIVIDUAL OBJECTS //
    ////////////////////////////
    public List<Project> getMyProjectsFull(User user, Map<String, Boolean> userRoles) {
        String userEmail = user.getEmail();
        List<Project> myProjectsFull = repository.getMyProjectsFull(userEmail);
        if (userRoles != null && !userRoles.isEmpty()) {
            Boolean creator = userRoles.get(Project._Fields.CREATED_BY.toString());
            Boolean moderator = userRoles.get(Project._Fields.MODERATORS.toString());
            Boolean contributor = userRoles.get(Project._Fields.CONTRIBUTORS.toString());
            Boolean projectOwner = userRoles.get(Project._Fields.PROJECT_OWNER.toString());
            Boolean leadArchitect = userRoles.get(Project._Fields.LEAD_ARCHITECT.toString());
            Boolean projectResponsible = userRoles.get(Project._Fields.PROJECT_RESPONSIBLE.toString());
            Boolean securityResponsible = userRoles.get(Project._Fields.SECURITY_RESPONSIBLES.toString());

            myProjectsFull = myProjectsFull.stream().filter(ProjectPermissions.isVisible(user))
                    .filter(project -> {
                        if (creator != null && creator && project.getCreatedBy().equals(userEmail)) {
                            return true;
                        } else if (moderator != null && moderator && project.getModerators().contains(userEmail)) {
                            return true;
                        } else if (contributor != null && contributor
                                && project.getContributors().contains(userEmail)) {
                            return true;
                        } else if (projectOwner != null && projectOwner
                                && project.getProjectOwner().equals(userEmail)) {
                            return true;
                        } else if (leadArchitect != null && leadArchitect
                                && project.getLeadArchitect().equals(userEmail)) {
                            return true;
                        } else if (projectResponsible != null && projectResponsible
                                && project.getProjectResponsible().equals(userEmail)) {
                            return true;
                        } else if (securityResponsible != null && securityResponsible
                                && project.getSecurityResponsibles().contains(userEmail)) {
                            return true;
                        }

                        return false;
                    }).collect(Collectors.toList());
        }
        return myProjectsFull;
    }

    public AddDocumentRequestSummary addProject(Project project, User user) throws SW360Exception {
        removeLeadingTrailingWhitespace(project);
        String name = project.getName();
        if (name == null || name.isEmpty()) {
            return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.NAMINGERROR);
        }

        // Prepare project for database
        prepareProject(project);
        if(isDuplicate(project)) {
            final AddDocumentRequestSummary addDocumentRequestSummary = new AddDocumentRequestSummary()
                    .setRequestStatus(AddDocumentRequestStatus.DUPLICATE);
            List<Project> duplicates = repository.searchByNameAndVersion(project.getName(), project.getVersion());
            if (duplicates.size() == 1) {
                duplicates.stream()
                        .map(Project::getId)
                        .forEach(addDocumentRequestSummary::setId);
            }
            return addDocumentRequestSummary;
        }

        if (!isDependenciesExists(project, user) || (SW360Utils.readConfig(IS_PACKAGE_PORTLET_ENABLED, true) && isLinkedReleasesUpdateFromLinkedPackagesFailed(project, Collections.emptySet()))) {
            return new AddDocumentRequestSummary()
                    .setRequestStatus(AddDocumentRequestStatus.INVALID_INPUT);
        }

        // Save creating user
        project.createdBy = user.getEmail();
        project.createdOn = getCreatedOn();
        if (CommonUtils.isNullEmptyOrWhitespace(project.businessUnit)) {
            project.businessUnit = getBUFromOrganisation(user.getDepartment());
        }

        setRequestedDateAndTrimComment(project, null, user);
        setRequestedDateAndTrimCommentForPackages(project, null, user);
        project.unsetVendor();
        // Add project to database and return ID
        repository.add(project);

        dbHandlerUtil.addChangeLogs(project, null, user.getEmail(), Operation.CREATE, null, Lists.newArrayList(),
                null, null);
        sendMailNotificationsForNewProject(project, user.getEmail());
        return new AddDocumentRequestSummary().setId(project.getId()).setRequestStatus(AddDocumentRequestStatus.SUCCESS);
    }

    private boolean isDuplicate(Project project){
        List<Project> duplicates = repository.searchByNameAndVersion(project.getName(), project.getVersion());
        return duplicates.size()>0;
    }
    ///////////////////////////////
    // UPDATE INDIVIDUAL OBJECTS //
    ///////////////////////////////
    public RequestStatus updateProject(Project project, User user) throws SW360Exception {
        return updateProject(project, user, false);
    }

    public RequestStatus updateProject(Project project, User user, boolean forceUpdate) throws SW360Exception {
        removeLeadingTrailingWhitespace(project);
        String name = project.getName();
        if (name == null || name.isEmpty()) {
            return RequestStatus.NAMINGERROR;
        }

        // Prepare project for database
        prepareProject(project);

        Project actual = repository.get(project.getId());

        assertNotNull(project);

        DatabaseHandlerUtil.saveAttachmentInFileSystem(attachmentConnector, actual.getAttachments(),
                project.getAttachments(), user.getEmail(), project.getId());
        if (changeWouldResultInDuplicate(actual, project)) {
            return RequestStatus.DUPLICATE;
        } else if (duplicateAttachmentExist(project)) {
            return RequestStatus.DUPLICATE_ATTACHMENT;
        } else if (!updateProjectAllowed(actual, user)) {
            return RequestStatus.CLOSED_UPDATE_NOT_ALLOWED;
        } else if (!changePassesSanityCheck(project, actual)){
            return RequestStatus.FAILED_SANITY_CHECK;
        } else if (!isDependenciesExists(project, user) || (SW360Utils.readConfig(IS_PACKAGE_PORTLET_ENABLED, true) &&
                isLinkedReleasesUpdateFromLinkedPackagesFailed(project, CommonUtils.nullToEmptySet(actual != null && actual.getPackageIds() != null ?
                        actual.getPackageIds().keySet() :
                        Collections.emptySet())))) {
            return RequestStatus.INVALID_INPUT;
        } else if (isWriteActionAllowedOnProject(actual, user) || forceUpdate) {
            copyImmutableFields(project,actual);
            setRequestedDateAndTrimComment(project, actual, user);
            setRequestedDateAndTrimCommentForPackages(project, actual, user);
            project.setAttachments( getAllAttachmentsToKeep(toSource(actual), actual.getAttachments(), project.getAttachments()) );
            setReleaseRelations(project, user, actual);
            updateProjectDependentLinkedFields(project, actual);
            project.unsetVendor();
            updateModifiedFields(project, user.getEmail());
            repository.update(project);

            List<ChangeLogs> referenceDocLogList=new LinkedList<>();
            Set<Attachment> attachmentsAfter = project.getAttachments();
            Set<Attachment> attachmentsBefore = actual.getAttachments();
            DatabaseHandlerUtil.populateChangeLogsForAttachmentsDeleted(attachmentsBefore, attachmentsAfter,
                    referenceDocLogList, user.getEmail(), project.getId(), Operation.PROJECT_UPDATE,
                    attachmentConnector, false);

            //clean up attachments in database
            attachmentConnector.deleteAttachmentDifference(actual.getAttachments(), project.getAttachments());

            if (CommonUtils.isNotNullEmptyOrWhitespace(actual.getClearingRequestId())) {
                updateProjectDependentFieldsInClearingRequest(project, actual, user);
            }
            sendMailNotificationsForProjectUpdate(project, actual, user);
            dbHandlerUtil.addChangeLogs(project, actual, user.getEmail(), Operation.UPDATE, attachmentConnector,
                    referenceDocLogList, null, null);
            return RequestStatus.SUCCESS;
        } else {
            return moderator.updateProject(project, user);
        }
    }

    private void setRequestedDateAndTrimComment(Project project, Project actual, User user) {
        Set<String> actualReleaseIds = null;
        if (Objects.nonNull(actual) && Objects.nonNull(actual.getReleaseIdToUsage())) {
            actualReleaseIds = CommonUtils.nullToEmptySet(actual.getReleaseIdToUsage().keySet());
        }
        final Set<String> actualReleaseIdsFinal = CommonUtils.nullToEmptySet(actualReleaseIds);
        Set<String> updatedReleaseIds = null;
        Map<String, ProjectReleaseRelationship> updatedProjectReleaseIdToUsage = project.getReleaseIdToUsage();
        if (Objects.nonNull(updatedProjectReleaseIdToUsage)) {
            updatedReleaseIds = CommonUtils.nullToEmptySet(updatedProjectReleaseIdToUsage.keySet());
        } else {
            updatedReleaseIds = new HashSet<>();
        }

        updatedReleaseIds.stream().filter(updatedReleaseId -> !actualReleaseIdsFinal.contains(updatedReleaseId))
                .forEach(updatedReleaseId -> {
                    ProjectReleaseRelationship projectReleaseRelationship = updatedProjectReleaseIdToUsage
                            .get(updatedReleaseId);
                    if (Objects.nonNull(projectReleaseRelationship)) {
                        projectReleaseRelationship.setCreatedOn(SW360Utils.getCreatedOn());
                        projectReleaseRelationship.setCreatedBy(user.getEmail());
                    }
                });

        updatedReleaseIds.stream().filter(commonReleaseId -> actualReleaseIdsFinal.contains(commonReleaseId))
                .forEach(commonReleaseId -> {
                    ProjectReleaseRelationship projectReleaseRelationship = updatedProjectReleaseIdToUsage
                            .get(commonReleaseId);
                    ProjectReleaseRelationship actualProjectReleaseRelationship = actual.getReleaseIdToUsage()
                            .get(commonReleaseId);
                    if (Objects.nonNull(projectReleaseRelationship)
                            && Objects.nonNull(actualProjectReleaseRelationship)) {
                        projectReleaseRelationship.setCreatedOn(actualProjectReleaseRelationship.getCreatedOn());
                        projectReleaseRelationship.setCreatedBy(actualProjectReleaseRelationship.getCreatedBy());
                    }
                });

        if (Objects.nonNull(updatedProjectReleaseIdToUsage)) {
            project.getReleaseIdToUsage().entrySet().stream().forEach(entry -> {
                if (Objects.nonNull(entry.getValue()) && Objects.nonNull(entry.getValue().getComment())) {
                    entry.getValue().setComment(entry.getValue().getComment().trim());
                }
            });
        }
    }

    private void setRequestedDateAndTrimCommentForPackages(Project project, Project actual, User user) {
        Set<String> actualPackagesIds = null;
        if (Objects.nonNull(actual) && Objects.nonNull(actual.getPackageIds())) {
            actualPackagesIds = CommonUtils.nullToEmptySet(actual.getPackageIds().keySet());
        }
        final Set<String> actualPackageIdsFinal = CommonUtils.nullToEmptySet(actualPackagesIds);
        Set<String> updatedPackageIds = null;
        Map<String, ProjectPackageRelationship> updatedProjectPackageIdToUsage = project.getPackageIds();
        if (Objects.nonNull(updatedProjectPackageIdToUsage)) {
            updatedPackageIds = CommonUtils.nullToEmptySet(updatedProjectPackageIdToUsage.keySet());
        } else {
            updatedPackageIds = new HashSet<>();
        }

        updatedPackageIds.stream().filter(updatedPackageId -> !actualPackageIdsFinal.contains(updatedPackageId))
                .forEach(updatedPackageId -> {
                    ProjectPackageRelationship projectPackageRelationship = updatedProjectPackageIdToUsage
                            .get(updatedPackageId);
                    if (Objects.nonNull(projectPackageRelationship)) {
                        projectPackageRelationship.setCreatedOn(SW360Utils.getCreatedOn());
                        projectPackageRelationship.setCreatedBy(user.getEmail());
                    }
                });

        updatedPackageIds.stream().filter(commonPackageId -> actualPackageIdsFinal.contains(commonPackageId))
                .forEach(commonPackageId -> {
                    ProjectPackageRelationship projectPackageRelationship = updatedProjectPackageIdToUsage
                            .get(commonPackageId);
                    ProjectPackageRelationship actualProjectPackageRelationship = actual.getPackageIds()
                            .get(commonPackageId);
                    if (Objects.nonNull(projectPackageRelationship)
                            && Objects.nonNull(actualProjectPackageRelationship)) {
                        projectPackageRelationship.setCreatedOn(actualProjectPackageRelationship.getCreatedOn());
                        projectPackageRelationship.setCreatedBy(actualProjectPackageRelationship.getCreatedBy());
                    }
                });

        if (Objects.nonNull(updatedProjectPackageIdToUsage)) {
            project.getPackageIds().entrySet().stream().forEach(entry -> {
                if (Objects.nonNull(entry.getValue()) && Objects.nonNull(entry.getValue().getComment())) {
                    entry.getValue().setComment(entry.getValue().getComment().trim());
                }
            });
        }
    }

    private boolean isDependenciesExists(Project project, User user) {
        boolean isValidDependentIds = true;
        if (project.isSetReleaseIdToUsage()) {
            Set<String> releaseIds = project.getReleaseIdToUsage().keySet();
            isValidDependentIds = DatabaseHandlerUtil.isAllIdInSetExists(releaseIds, releaseRepository);
        }

        if (SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            if (project.isSetReleaseRelationNetwork()) {
                Set<String> releaseIds = SW360Utils.getReleaseIdsLinkedWithProject(project);
                isValidDependentIds = DatabaseHandlerUtil.isAllIdInSetExists(releaseIds, releaseRepository);
            }
        }

        if (isValidDependentIds && project.isSetLinkedProjects()) {
            Set<String> projectIds = project.getLinkedProjects().keySet();
            isValidDependentIds =  DatabaseHandlerUtil.isAllIdInSetExists(projectIds, repository) && verifyLinkedProjectsAreAccessible(projectIds, user);
        }

        if (isValidDependentIds && project.isSetLinkedObligationId()) {
            String obligationId = project.getLinkedObligationId();
            isValidDependentIds = DatabaseHandlerUtil.isAllIdInSetExists(Sets.newHashSet(obligationId), obligationRepository);
        }

        if (isValidDependentIds && CommonUtils.isNotNullEmptyOrWhitespace(project.getVendorId())) {
            isValidDependentIds = DatabaseHandlerUtil.isAllIdInSetExists(Sets.newHashSet(project.getVendorId()),
                    vendorRepository);
        }

        if (isValidDependentIds && project.isSetPackageIds()) {
            Set<String> pacakgeIds = project.getPackageIds().keySet();
            isValidDependentIds = DatabaseHandlerUtil.isAllIdInSetExists(pacakgeIds, packageRepository);
        }
        return isValidDependentIds;
    }

    private boolean verifyLinkedProjectsAreAccessible(Set<String> linkedProjectIds, User user) {
        long nonAccessibleProjectIdsCount = 0;
        if (linkedProjectIds != null) {
            nonAccessibleProjectIdsCount = linkedProjectIds.stream().filter(id -> {
                Project project = repository.get(id);
                return !PermissionUtils.makePermission(project, user).isActionAllowed(RequestedAction.READ);
            }).count();
        }

        if (nonAccessibleProjectIdsCount > 0)
            return false;

        return true;

    }

    private void updateProjectDependentFieldsInClearingRequest(Project updated, Project current, User user) {
        if (isLinkedReleaseUpdated(updated, current)) {
            addCommentToClearingRequest(updated, current, user);
        }
        if (isProjectBusinessUnitUpdated(updated, current, user)) {
            updateBusinessUnitInClearingRequest(updated.getClearingRequestId(), updated.getBusinessUnit(), user);
        }
    }

    private void updateBusinessUnitInClearingRequest(String crId, String businessUnit, User user) {
       moderator.updateClearingRequestForChangeInProjectBU(crId, businessUnit, user);
    }

    private void addCommentToClearingRequest(Project updated, Project current, User user) {
        Set<String> currentReleaseIds = CommonUtils.getNullToEmptyKeyset(current.getReleaseIdToUsage());
        Set<String> updatedReleaseIds = CommonUtils.getNullToEmptyKeyset(updated.getReleaseIdToUsage());
        Set<String> allReleaseIds = Sets.newHashSet(currentReleaseIds);
        allReleaseIds.addAll(updatedReleaseIds);
        Set<String> added = Sets.difference(updatedReleaseIds, currentReleaseIds);
        Set<String> removed = Sets.difference(currentReleaseIds, updatedReleaseIds);
        Collection<Release> releases = CommonUtils.nullToEmptyCollection(componentDatabaseHandler.getReleasesForClearingStateSummary(allReleaseIds));
        // Build plain text comment
        StringBuilder commentText = new StringBuilder("Linked release(s) are updated for the project.");

        // Build HTML for email using template method
        String htmlChanges = generateReleaseChangesHtml(releases, added, removed);

        if (CommonUtils.isNotEmpty(added)) {
            Set<String> releaseNames = extractReleaseNameWithId(releases, added);
            commentText.append(System.lineSeparator()).append("Added Release Ids: ").append(SW360Utils.spaceJoiner.join(releaseNames));
        }
        if (CommonUtils.isNotEmpty(removed)) {
            Set<String> releaseNames = extractReleaseNameWithId(releases, removed);
            commentText.append(System.lineSeparator()).append("Removed Release Ids: ").append(SW360Utils.spaceJoiner.join(releaseNames));
        }
        // filter all current releases
        releases = releases.stream().filter(rel -> updatedReleaseIds.contains(rel.getId())).collect(Collectors.toSet());

        Map<ComponentType, Integer> compTypeCountMap = new HashMap<>();
        Set<String> nonOssCompIds = getNonOssComponentIdsFromRelease(releases);
        String nonOssCompCount = "0";
        if (!nonOssCompIds.isEmpty()) {
            nonOssCompCount = appendCompTypeToReleaseVersion(releases, nonOssCompIds, compTypeCountMap, user);
        }
        commentText.append(System.lineSeparator()).append("Total number of non-OSS components: ").append(nonOssCompCount);
        Comment comment = new Comment().setText(commentText.toString()).setCommentedBy(user.getEmail()).setAutoGenerated(true);
        moderator.addCommentToClearingRequest(current.getClearingRequestId(), comment, user);

        // Pass HTML changes to email method
        sendMailForUpdatedProjectWithClearingRequest(updated, releases, nonOssCompCount, compTypeCountMap, user, htmlChanges);
    }

    private Set<String> extractReleaseNameWithId(Collection<Release> releases, Collection<String> filterCriteria) {
        return releases.stream().filter(rel -> filterCriteria.contains(rel.getId()))
                .map(rel -> new StringBuilder(System.lineSeparator()).append("\t").append(SW360Utils.printFullname(rel)).append(" (")
                        .append(rel.getId()).append(")").toString())
                .collect(Collectors.toSet());
    }

    public ObligationList getLinkedObligations(String obligationId, User user) throws TException {
        ObligationList obligation = obligationRepository.get(obligationId);
        assertNotNull(obligation);
        assertId(obligation.getProjectId());
        return obligation;
    }

    public RequestStatus addLinkedObligations(ObligationList obligation, User user) throws TException {
        ThriftValidate.prepareProjectObligation(obligation);
        obligationRepository.add(obligation);
        Project project = getProjectById(obligation.getProjectId(), user);
        project.setLinkedObligationId(obligation.getId());
        repository.update(project);
        project.unsetLinkedObligationId();
        dbHandlerUtil.addChangeLogs(obligation, null, user.getEmail(), Operation.CREATE, attachmentConnector,
                Lists.newArrayList(), obligation.getProjectId(), Operation.PROJECT_UPDATE);
        dbHandlerUtil.addChangeLogs(getProjectById(obligation.getProjectId(), user), project, user.getEmail(),
                Operation.UPDATE, attachmentConnector, Lists.newArrayList(), null, Operation.OBLIGATION_ADD);

        return RequestStatus.SUCCESS;
    }

    public RequestStatus updateLinkedObligations(ObligationList obligation, User user) throws TException {
        Project project = getProjectById(obligation.getProjectId(), user);
        ObligationList projectObligationbefore = obligationRepository.get(obligation.getId());
        if (isWriteActionAllowedOnProject(project, user)) {
            obligationRepository.update(obligation);
            dbHandlerUtil.addChangeLogs(obligation, projectObligationbefore, user.getEmail(), Operation.UPDATE,
                    attachmentConnector, Lists.newArrayList(), obligation.getProjectId(), Operation.PROJECT_UPDATE);
            return RequestStatus.SUCCESS;
        }
        return RequestStatus.FAILURE;
    }

    private boolean isProjectBusinessUnitUpdated(Project updated, Project current, User user) {
        ClearingRequest cr = moderator.getClearingRequestByProjectId(current.getId(), user);
        return !CommonUtils.nullToEmptyString(updated.getBusinessUnit()).equalsIgnoreCase(current.getBusinessUnit())
                && !cr.getProjectBU().equalsIgnoreCase(updated.getBusinessUnit());
    }

    private boolean isLinkedReleaseUpdated(Project updated, Project current) {
        Set<String> updatedReleaseIds = CommonUtils.getNullToEmptyKeyset(updated.getReleaseIdToUsage());
        Set<String> currentReleaseIds = CommonUtils.getNullToEmptyKeyset(current.getReleaseIdToUsage());
        if (updatedReleaseIds.equals(currentReleaseIds)) {
            return false;
        }
        return true;
    }

    private void setReleaseRelations(Project updated, User user, Project current) {
        final boolean isMainLineStateEnabledForUser = SW360Utils.readConfig(MAINLINE_STATE_ENABLED_FOR_USER, false);
        boolean isMainlineStateDisabled = !(isMainLineStateEnabledForUser
                || PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user))
                && updated.getReleaseIdToUsageSize() > 0;

        Map<String, ProjectReleaseRelationship> updatedReleaseIdToUsage = updated.getReleaseIdToUsage();

        if ((null == current || current.getReleaseIdToUsageSize() == 0) && isMainlineStateDisabled) {
            updatedReleaseIdToUsage.forEach((k, v) -> v.setMainlineState(MainlineState.OPEN));
        } else if (isMainlineStateDisabled) {
            Map<String, ProjectReleaseRelationship> currentReleaseIdToUsage = current.getReleaseIdToUsage();

            for (Map.Entry<String, ProjectReleaseRelationship> entry : updatedReleaseIdToUsage.entrySet()) {
                ProjectReleaseRelationship prr = currentReleaseIdToUsage.get(entry.getKey());
                if (null != prr) {
                    entry.getValue().setMainlineState(prr.getMainlineState());
                } else {
                    entry.getValue().setMainlineState(MainlineState.OPEN);
                }
            }
        }
    }

    private boolean changeWouldResultInDuplicate(Project before, Project after) {
        if (before.getName().equals(after.getName()) &&
            (
                  (Strings.isNullOrEmpty(before.getVersion()) && Strings.isNullOrEmpty(after.getVersion()))
              ||  (before.getVersion() != null && before.getVersion().equals(after.getVersion()))
            )
        ) {
            // sth else was changed, not one of the duplication relevant properties
            return false;
        }

        return isDuplicate(after);
    }

    private boolean duplicateAttachmentExist(Project project) {
        if(project.attachments != null && !project.attachments.isEmpty()) {
            return AttachmentConnector.isDuplicateAttachment(project.attachments);
        }
        return false;
    }

    private boolean updateProjectAllowed(Project project, User user) {
        if (project.clearingState != null && project.clearingState.equals(ProjectClearingState.CLOSED)
                && !PermissionUtils.isUserAtLeast(UserGroup.SW360_ADMIN, user) && !SW360Utils.isUserAllowedToEditClosedProject(project, user)) {
            return false;
        }
        return true;
    }

    private ObligationList deleteObligationsOfUnlinkedReleases(Project updated) {
        ObligationList obligation = obligationRepository.get(updated.getLinkedObligationId());
        Set<String> updatedLinkedReleaseIds = nullToEmptyMap(updated.getReleaseIdToUsage()).keySet();

        Map<String, ObligationStatusInfo> updatedOsInfoMap = nullToEmptyMap(obligation.getLinkedObligationStatus());
        for (Iterator<Map.Entry<String, ObligationStatusInfo>> it = updatedOsInfoMap.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, ObligationStatusInfo> entry = it.next();
            Map<String, String> releaseIdToAcceptedCLI = entry.getValue().getReleaseIdToAcceptedCLI();
            if (releaseIdToAcceptedCLI != null) {
                releaseIdToAcceptedCLI.keySet().retainAll(updatedLinkedReleaseIds);
                if (releaseIdToAcceptedCLI.isEmpty()) {
                    it.remove();
                }
            }
        }
        if(CommonUtils.isNullOrEmptyMap(updatedOsInfoMap)) {
            obligation.unsetLinkedObligationStatus();
            return obligation;
        }
        obligation.setLinkedObligationStatus(updatedOsInfoMap);
        return obligation;
    }

    private void updateProjectDependentLinkedFields(Project updated, Project actual) throws SW360Exception {
        Source usedBy = Source.projectId(updated.getId());
        Set<String> updatedLinkedReleaseIds = nullToEmptyMap(updated.getReleaseIdToUsage()).keySet();
        Set<String> actualLinkedReleaseIds = nullToEmptyMap(actual.getReleaseIdToUsage()).keySet();
        deleteAttachmentUsagesOfUnlinkedReleases(usedBy, updatedLinkedReleaseIds, actualLinkedReleaseIds);

        // update the obligations only if linked obligations were present in current project,
        // and there is change in linked releases in updated project
        if (CommonUtils.isNotNullEmptyOrWhitespace(actual.getLinkedObligationId()) && !actualLinkedReleaseIds.equals(updatedLinkedReleaseIds)) {
            obligationRepository.update(deleteObligationsOfUnlinkedReleases(updated));
        }
    }

    /**
     * Link the Release of newly linked Packages to the Project.
     * Unlink the Release of unlinked Packages from the Project.
     * @return true if linking or unlinking is failed, else returns false.
     */
    private boolean isLinkedReleasesUpdateFromLinkedPackagesFailed(Project updatedProject, Set<String> currentPackageIds) throws SW360Exception {
        Set<String> updatedPackageIds = updatedProject.getPackageIds() != null
                ? CommonUtils.nullToEmptySet(updatedProject.getPackageIds().keySet())
                : Collections.emptySet();
        if (updatedPackageIds.equals(currentPackageIds)) {
            return false;
        }
        Set<String> linkedPacakgeIds = Sets.difference(updatedPackageIds, currentPackageIds);
        Set<String> unlinkedPacakgeIds = Sets.difference(currentPackageIds, updatedPackageIds);
        final ProjectReleaseRelationship releaseRelation = new ProjectReleaseRelationship(ReleaseRelationship.UNKNOWN, MainlineState.OPEN);
        if (CommonUtils.isNotEmpty(linkedPacakgeIds)) {
            try {
                PackageService.Iface packageClient = new ThriftClients().makePackageClient();
                List<Package> addedPackages = packageClient.getPackageByIds(linkedPacakgeIds);

                Map<String, ProjectReleaseRelationship> releaseIdToUsageMap = addedPackages.stream().map(Package::getReleaseId)
                        .filter(CommonUtils::isNotNullEmptyOrWhitespace)
                        .map(relId -> new AbstractMap.SimpleEntry<>(relId, releaseRelation))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldVal, newVal) -> newVal));

                /* Check if the releaseId found from linked Packages are present in database.
                 * If present, then link the Release to the Project.
                 * If not present, then return true (status failed).
                 */
                if (DatabaseHandlerUtil.isAllIdInSetExists(releaseIdToUsageMap.keySet(), releaseRepository)) {
                    Map<String, ProjectReleaseRelationship> targetMap = CommonUtils.nullToEmptyMap(updatedProject.getReleaseIdToUsage());
                    if (CommonUtils.isNullOrEmptyMap(targetMap)) {
                        updatedProject.setReleaseIdToUsage(releaseIdToUsageMap);
                    } else {
                        for (Map.Entry<String, ProjectReleaseRelationship> entry : releaseIdToUsageMap.entrySet()) {
                            targetMap.putIfAbsent(entry.getKey(), entry.getValue());
                        }
                    }
                } else {
                    return true;
                }
            } catch (TException e) {
                log.error(String.format("Error fetching newly added linked package info of project: %s", updatedProject.getId()), e.getCause());
                throw new SW360Exception(e.getMessage());
            }
        }

        if (CommonUtils.isNotEmpty(unlinkedPacakgeIds)) {
            try {
                PackageService.Iface packageClient = new ThriftClients().makePackageClient();
                List<Package> removedPackages = packageClient.getPackageWithReleaseByPackageIds(unlinkedPacakgeIds);

                Map<String, Set<String>> releaseIdToPackageIdsMap = removedPackages.stream()
                        .filter(packageFilter -> packageFilter.getRelease() != null)
                        .map(Package::getRelease)
                        .filter(rel -> CommonUtils.isNotEmpty(rel.getPackageIds()))
                        .map(rel -> new AbstractMap.SimpleEntry<>(rel.getId(), rel.getPackageIds()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldVal, newVal) -> newVal));

                Map<String, ProjectReleaseRelationship> targetMap = CommonUtils.nullToEmptyMap(updatedProject.getReleaseIdToUsage());

                /* If Project contains releaseId of unlinked Package,
                 * & check if Project contains at least one Package from the Release of unlinked Package.
                 * If No, unlink the Release from Project.
                 */
                for (Map.Entry<String, Set<String>> entry : releaseIdToPackageIdsMap.entrySet()) {
                    if (targetMap.containsKey(entry.getKey()) &&
                            CommonUtils.isNotEmpty(CommonUtils.nullToEmptySet(Sets.intersection(entry.getValue(), unlinkedPacakgeIds)))) {
                        targetMap.remove(entry.getKey());
                    }
                }
            } catch (TException e) {
                log.error(String.format("Error fetching removed linked package info of project: %s", updatedProject.getId()), e.getCause());
                throw new SW360Exception(e.getMessage());
            }
        }
        return false;
    }

    private boolean changePassesSanityCheck(Project updated, Project current) {
        return !(nullToEmptyMap(current.getReleaseIdToUsage()).size() > DELETION_SANITY_CHECK_THRESHOLD &&
                nullToEmptyMap(updated.getReleaseIdToUsage()).size() == 0 ||
                nullToEmptyMap(current.getLinkedProjects()).size() > DELETION_SANITY_CHECK_THRESHOLD &&
                nullToEmptyMap(updated.getLinkedProjects()).size() == 0);
    }

    private void prepareProject(Project project) throws SW360Exception {
        // Prepare project for database
        ThriftValidate.prepareProject(project);

        //add sha1 to attachments if necessary
        if(project.isSetAttachments()) {
            attachmentConnector.setSha1ForAttachments(project.getAttachments());
        }
    }

    public RequestStatus updateProjectFromAdditionsAndDeletions(Project projectAdditions, Project projectDeletions, User user){

        try {
            Project project = getProjectById(projectAdditions.getId(), user);
            project = moderator.updateProjectFromModerationRequest(project, projectAdditions, projectDeletions);
            return updateProject(project, user);
        } catch (SW360Exception e) {
            log.error("Could not get original project when updating from moderation request.");
            return RequestStatus.FAILURE;
        }

    }

    private void copyImmutableFields(Project destination, Project source) {
        ThriftUtils.copyField(source, destination, Project._Fields.CREATED_ON);
        ThriftUtils.copyField(source, destination, Project._Fields.CREATED_BY);
    }

    ///////////////////////////////
    // DELETE INDIVIDUAL OBJECTS //
    ///////////////////////////////
    public RequestStatus deleteProject(String id, User user) throws SW360Exception {
        return deleteProject(id, user, false);
    }

    public RequestStatus deleteProject(String id, User user, boolean forceDelete) throws SW360Exception {
        Project project = repository.get(id);
        assertNotNull(project);

        if (checkIfInUse(id)) {
            return RequestStatus.IN_USE;
        }

        // Remove the project if the user is allowed to do it by himself
        if (makePermission(project, user).isActionAllowed(RequestedAction.DELETE) || forceDelete) {
            removeProjectAndCleanUp(project, user);
            dbHandlerUtil.addChangeLogs(null, project, user.getEmail(), Operation.DELETE, attachmentConnector,
                    Lists.newArrayList(), null, null);
            return RequestStatus.SUCCESS;
        } else {
            return moderator.deleteProject(project, user);
        }
    }

    public boolean checkIfInUse(String projectId) {
        final Set<Project> usingProjects = repository.searchByLinkingProjectId(projectId);
       return !usingProjects.isEmpty();
    }

    public Set<Project> searchByPackageId(String id, User user) {
        return repository.searchByPackageId(id, user);
    }

    public Set<Project> searchByPackageIds(Set<String> ids, User user) {
        return repository.searchByPackageIds(ids, user);
    }

    public int getProjectCountByPackageId(String packageId) {
        return repository.getCountByPackageId(packageId);
    }

    private void removeProjectAndCleanUp(Project project, User user) throws SW360Exception {
        attachmentConnector.deleteAttachments(project.getAttachments());
        attachmentDatabaseHandler.deleteUsagesBy(Source.projectId(project.getId()));
        repository.remove(project);
        if (project.isSetLinkedObligationId()) {
            obligationRepository.remove(project.getLinkedObligationId());
        }
        moderator.notifyModeratorOnDelete(project.getId());
        deleteUsedReleaseRelations(project.getId());
        if (CommonUtils.isNotNullEmptyOrWhitespace(project.getClearingRequestId())) {
            moderator.unlinkClearingRequestForProjectDeletion(project, user);
        }
    }

    private void deleteUsedReleaseRelations(String projectId) throws SW360Exception {
        List<UsedReleaseRelations> usedReleaseRelations;
        try {
            usedReleaseRelations = nullToEmptyList(getUsedReleaseRelationsByProjectId(projectId));
            if (CommonUtils.isNotEmpty(usedReleaseRelations)) {
                for (UsedReleaseRelations usedReleaseRelation : usedReleaseRelations) {
                    deleteReleaseRelationsUsage(usedReleaseRelation);
                }
            }
        } catch (TException e) {
            throw new SW360Exception(e.getMessage());
        }
    }

    //////////////////////
    // HELPER FUNCTIONS //
    //////////////////////

    public List<ProjectLink> getLinkedProjects(Project project, boolean deep, User user) throws SW360Exception {
        Deque<String> visitedIds = new ArrayDeque<>();

        Map<String, ProjectProjectRelationship> fakeRelations = new HashMap<>();
        fakeRelations.put(project.isSetId() ? project.getId() : DUMMY_NEW_PROJECT_ID, new ProjectProjectRelationship(ProjectRelationship.UNKNOWN));
        List<ProjectLink> out = iterateProjectRelationShips(fakeRelations, null, visitedIds, deep ? -1 : 2, user);
        return out;
    }

    public List<ProjectLink> getLinkedProjects(Map<String, ProjectProjectRelationship> relations, boolean depth, User user) throws SW360Exception {
        List<ProjectLink> out;

        Deque<String> visitedIds = new ArrayDeque<>();
        out = iterateProjectRelationShips(relations, null, visitedIds, depth ? -1 : 1, user);

        return out;
    }

    private List<ProjectLink> iterateProjectRelationShips(Map<String, ProjectProjectRelationship> relations,
            String parentNodeId, Deque<String> visitedIds, int maxDepth, User user) throws SW360Exception {
        List<ProjectLink> out = new ArrayList<>();
        for (Map.Entry<String, ProjectProjectRelationship> entry : relations.entrySet()) {
            Optional<ProjectLink> projectLinkOptional;
            if (!SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
                projectLinkOptional = createProjectLink(entry.getKey(), entry.getValue(),
                        parentNodeId, visitedIds, maxDepth, user);
            } else {
                projectLinkOptional = createProjectLinkForDependencyNetwork(entry.getKey(), entry.getValue(),
                        parentNodeId, visitedIds, maxDepth, user, true, WITH_ROOT_RELEASES_ONLY);
            }
            projectLinkOptional.ifPresent(out::add);
        }
        out.sort(Comparator.comparing(ProjectLink::getName).thenComparing(ProjectLink::getVersion));
        return out;
    }

    private Optional<ProjectLink> createProjectLink(String id, ProjectProjectRelationship projectProjectRelationship, String parentNodeId,
            Deque<String> visitedIds, int maxDepth, User user) throws SW360Exception {
        ProjectLink projectLink = null;
        if (!visitedIds.contains(id) && (maxDepth < 0 || visitedIds.size() < maxDepth)) {
            visitedIds.push(id);
            Project project = repository.get(id);
            if (project != null
                    && (user == null || !makePermission(project, user).isActionAllowed(RequestedAction.READ))) {
                log.error("User " + user == null ? ""
                        : user.getEmail() + " requested not accessible project " + printName(project));
                project = null;
            }
            if (project != null) {
                projectLink = new ProjectLink(id, project.name);
                if (project.isSetReleaseIdToUsage() && (maxDepth < 0 || visitedIds.size() < maxDepth)){ // ProjectLink on the last level does not get children added
                    List<ReleaseLink> linkedReleases = componentDatabaseHandler.getLinkedReleasesWithAccessibility(project, visitedIds, user);
                    fillMainlineStates(linkedReleases, project.getReleaseIdToUsage());
                    projectLink.setLinkedReleases(nullToEmptyList(linkedReleases));
                }

                projectLink
                        .setNodeId(generateNodeId(id))
                        .setParentNodeId(parentNodeId)
                        .setRelation(projectProjectRelationship.getProjectRelationship())
                        .setEnableSvm(projectProjectRelationship.isEnableSvm())
                        .setVersion(project.getVersion())
                        .setState(project.getState())
                        .setProjectType(project.getProjectType())
                        .setClearingState(project.getClearingState())
                        .setTreeLevel(visitedIds.size() - 1);
                if (project.isSetLinkedProjects()) {
                    List<ProjectLink> subprojectLinks = iterateProjectRelationShips(project.getLinkedProjects(),
                            projectLink.getNodeId(), visitedIds, maxDepth, user);
                    projectLink.setSubprojects(subprojectLinks);
                }
            } else {
                log.error("Broken ProjectLink in project with id: " + parentNodeId + ". Linked project with id " + id + " was not found");
            }
            visitedIds.pop();
        }
        return Optional.ofNullable(projectLink);
    }

    private void fillMainlineStates(List<ReleaseLink> linkedReleases, Map<String, ProjectReleaseRelationship> releaseIdToUsage) {
        for (ReleaseLink releaseLink : linkedReleases) {
            releaseLink.setMainlineState(releaseIdToUsage.get(releaseLink.getId()).getMainlineState());
        }
    }

    private String generateNodeId(String id) {
        return id == null ? null : id + "_" + UUID.randomUUID();
    }

    public Set<Project> searchByReleaseId(String id, User user) {
        return repository.searchByReleaseId(id, user);
    }

    public Set<Project> searchByReleaseId(Set<String> ids, User user) {
        return repository.searchByReleaseId(ids, user);
    }

    public Set<Project> searchLinkingProjects(String id, User user) {
        return repository.searchByLinkingProjectId(id, user);
    }

    public Project getProjectForEdit(String id, User user) throws SW360Exception {

        List<ModerationRequest> moderationRequestsForDocumentId = moderator.getModerationRequestsForDocumentId(id);

        Project project = getProjectById(id,user);
        Visibility actualVisbility = project.getVisbility();
        DocumentState documentState;
        if (moderationRequestsForDocumentId.isEmpty()) {
            documentState = CommonUtils.getOriginalDocumentState();
        } else {
            final String email = user.getEmail();
            Optional<ModerationRequest> moderationRequestOptional = CommonUtils.getFirstModerationRequestOfUser(moderationRequestsForDocumentId, email);
            if (moderationRequestOptional.isPresent()
                    && isInProgressOrPending(moderationRequestOptional.get())){
                ModerationRequest moderationRequest = moderationRequestOptional.get();
                project = moderator.updateProjectFromModerationRequest(project,
                        moderationRequest.getProjectAdditions(),
                        moderationRequest.getProjectDeletions());

                if (moderationRequest.getProjectAdditions() != null && moderationRequest.getProjectDeletions() != null
                        && moderationRequest.getProjectAdditions().getVisbility() == moderationRequest
                                .getProjectDeletions().getVisbility()) {
                    project.setVisbility(actualVisbility);
                }
                documentState = CommonUtils.getModeratedDocumentState(moderationRequest);
            } else {
                documentState = new DocumentState().setIsOriginalDocument(true).setModerationState(moderationRequestsForDocumentId.get(0).getModerationState());
            }
        }
        project.setPermissions(makePermission(project, user).getPermissionMap());
        project.setDocumentState(documentState);
        return project;
    }

    public List<Project> getProjectsById(List<String> ids, User user) {

        List<Project> projects = repository.makeSummaryFromFullDocs(SummaryType.SUMMARY, repository.get(ids, true));

        List<Project> output = new ArrayList<>();
        for (Project project : projects) {
            if (makePermission(project, user).isActionAllowed(RequestedAction.READ)) {
                output.add(project);
            } else {
                log.error("User " + user.getEmail() + " requested not accessible project " + printName(project));
            }
        }

        return output;
    }

    public int getCountByReleaseIds(Set<String> ids) {
        return repository.getCountByReleaseIds(ids);
    }

    public int getCountByProjectId(String id) {
        return repository.getCountByProjectId(id);
    }

    public Set<Project> searchByExternalIds(Map<String, Set<String>> externalIds, User user) {
        return repository.searchByExternalIds(externalIds, user);
    }

    public Set<Project> getAccessibleProjects(User user) {
        return repository.getAccessibleProjects(user, vendorRepository);
    }

    public Map<String, List<String>> getDuplicateProjects() {
        ListMultimap<String, String> projectIdentifierToReleaseId = ArrayListMultimap.create();

        for (Project project : repository.getAll()) {
            projectIdentifierToReleaseId.put(SW360Utils.printName(project), project.getId());
        }

        return CommonUtils.getIdentifierToListOfDuplicates(projectIdentifierToReleaseId);
    }

    public List<ProjectVulnerabilityRating> getProjectVulnerabilityRatingByProjectId(String projectId){
        return pvrRepository.getProjectVulnerabilityRating(projectId);
    }

    public RequestStatus updateProjectVulnerabilityRating(ProjectVulnerabilityRating link) {
        if( ! link.isSetId()){
            link.setId(SW360Constants.PROJECT_VULNERABILITY_RATING_ID_PREFIX + link.getProjectId());
            try {
                pvrRepository.add(link);
            } catch (SW360Exception e) {
                log.error("Unable to update project vulnerability rating.", e);
                return RequestStatus.FAILURE;
            }
        } else {
            pvrRepository.update(link);
        }
        return RequestStatus.SUCCESS;
    }

    public List<ProjectVulnerabilityRating> getProjectVulnerabilityRatingsByReleaseId(String releaseId) {
        return pvrRepository.getProjectVulnerabilityRatingsByReleaseId(releaseId);
    }

    public List<Project> fillClearingStateSummary(List<Project> projects, User user) {
        Function<Project, Set<String>> extractReleaseIds = project -> CommonUtils.nullToEmptyMap(project.getReleaseIdToUsage()).keySet();

        Set<String> allReleaseIds = projects.stream().map(extractReleaseIds).reduce(Sets.newHashSet(), Sets::union);
        if (!allReleaseIds.isEmpty()) {
            Map<String, Release> releasesById = ThriftUtils.getIdMap(componentDatabaseHandler.getReleasesForClearingStateSummary(allReleaseIds));
            for (Project project : projects) {
                final Set<String> releaseIds = extractReleaseIds.apply(project);
                List<Release> releases = releaseIds.stream().map(releasesById::get).collect(Collectors.toList());
                final ReleaseClearingStateSummary releaseClearingStateSummary = ReleaseClearingStateSummaryComputer.computeReleaseClearingStateSummary(releases, project
                        .getClearingTeam());
                project.setReleaseClearingStateSummary(releaseClearingStateSummary);
            }
        }
        return projects;
    }

    private List<Release> getDirectlyLinkedReleasesInNewState(Project project) {
        Set<String> releaseIds = CommonUtils.nullToEmptyMap(project.getReleaseIdToUsage()).keySet();
        List<Release> releases = componentDatabaseHandler.getReleasesForClearingStateSummary(releaseIds);
        return getDirectlyLinkedReleasesInNewState(releases);
    }

    private List<Release> getDirectlyLinkedReleasesInNewState(Collection<Release> releases) {
        return releases.stream().filter(release -> null == release.getClearingState()
                || ClearingState.NEW_CLEARING.equals(release.getClearingState())).collect(Collectors.toList());
    }

    public List<ReleaseClearingStatusData> getReleaseClearingStatuses(String projectId, User user) throws SW360Exception {
        Project project = getProjectById(projectId, user);
        SetMultimap<String, ProjectWithReleaseRelationTuple> releaseIdsToProject = releaseIdToProjects(project, user);
        List<Release> releasesById = componentDatabaseHandler.getDetailedReleasesForExport(releaseIdsToProject.keySet());
        Map<String, Component> componentsById = ThriftUtils.getIdMap(
                componentDatabaseHandler.getComponentsShort(
                        releasesById.stream().map(Release::getComponentId).collect(Collectors.toSet())));

        List<ReleaseClearingStatusData> releaseClearingStatuses = new ArrayList<>();
        for (Release release : releasesById) {
            List<String> projectNames = new ArrayList<>();
            List<String> mainlineStates = new ArrayList<>();

            for (ProjectWithReleaseRelationTuple projectWithReleaseRelation : releaseIdsToProject.get(release.getId())) {
                projectNames.add(printName(projectWithReleaseRelation.getProject()));
                mainlineStates.add(ThriftEnumUtils.enumToString(projectWithReleaseRelation.getRelation().getMainlineState()));
                if (projectNames.size() > 3) {
                    projectNames.add("...");
                    mainlineStates.add("...");
                    break;
                }

            }
            releaseClearingStatuses.add(new ReleaseClearingStatusData(release)
                    .setProjectNames(joinStrings(projectNames))
                    .setMainlineStates(joinStrings(mainlineStates))
                    .setComponentType(componentsById.get(release.getComponentId()).getComponentType()));
        }
        return releaseClearingStatuses;
    }

    public List<ReleaseClearingStatusData> getReleaseClearingStatusesWithAccessibility(String projectId, User user) throws SW360Exception {
        Project project = getProjectById(projectId, user);
        SetMultimap<String, ProjectWithReleaseRelationTuple> releaseIdsToProject = releaseIdToProjects(project, user);
        List<Release> releasesById = componentDatabaseHandler.getDetailedReleasesWithAccessibilityForExport(releaseIdsToProject.keySet(), user);
        Map<String, Component> componentsById = ThriftUtils.getIdMap(
                componentDatabaseHandler.getComponentsShort(
                        releasesById.stream().map(Release::getComponentId).collect(Collectors.toSet())));

        List<ReleaseClearingStatusData> releaseClearingStatuses = new ArrayList<>();
        for (Release release : releasesById) {
            List<String> projectNames = new ArrayList<>();
            List<String> mainlineStates = new ArrayList<>();

            for (ProjectWithReleaseRelationTuple projectWithReleaseRelation : releaseIdsToProject.get(release.getId())) {
                projectNames.add(printName(projectWithReleaseRelation.getProject()));
                mainlineStates.add(ThriftEnumUtils.enumToString(projectWithReleaseRelation.getRelation().getMainlineState()));
                if (projectNames.size() > 3) {
                    projectNames.add("...");
                    mainlineStates.add("...");
                    break;
                }

            }
            ReleaseClearingStatusData releaseClearingStatusData = new ReleaseClearingStatusData(release)
                    .setProjectNames(joinStrings(projectNames))
                    .setMainlineStates(joinStrings(mainlineStates))
                    .setComponentType(componentsById.get(release.getComponentId()).getComponentType());

            boolean isAccessible = componentDatabaseHandler.isReleaseActionAllowed(release, user, RequestedAction.READ);
            releaseClearingStatusData.setAccessible(isAccessible);
            releaseClearingStatuses.add(releaseClearingStatusData);
        }
        return releaseClearingStatuses;
     }

    SetMultimap<String, ProjectWithReleaseRelationTuple> releaseIdToProjects(Project project, User user) throws SW360Exception {
        Set<String> visitedProjectIds = new HashSet<>();
        SetMultimap<String, ProjectWithReleaseRelationTuple> releaseIdToProjects = HashMultimap.create();

        releaseIdToProjects(project, user, visitedProjectIds, releaseIdToProjects);
        return releaseIdToProjects;
    }

    public String getCyclicLinkedProjectPath(Project project, User user) throws TException {
        return DatabaseHandlerUtil.getCyclicLinkedPath(project, this, user);
    }

    private void releaseIdToProjects(Project project, User user, Set<String> visitedProjectIds, Multimap<String, ProjectWithReleaseRelationTuple> releaseIdToProjects) throws SW360Exception {

        if (nothingTodo(project, visitedProjectIds)) return;

        nullToEmptyMap(project.getReleaseIdToUsage()).forEach((releaseId, relation) -> {
            releaseIdToProjects.put(releaseId, new ProjectWithReleaseRelationTuple(project, relation));
        });

        Map<String, ProjectProjectRelationship> linkedProjects = project.getLinkedProjects();
        if (linkedProjects != null) {

                for (String projectId : linkedProjects.keySet()) {
                    if (visitedProjectIds.contains(projectId)) continue;

                    Project linkedProject = getProjectById(projectId, user);
                    releaseIdToProjects(linkedProject, user, visitedProjectIds, releaseIdToProjects);
                }
        }
    }

    private boolean nothingTodo(Project project, Set<String> visitedProjectIds) {
        if (project == null) {
            return true;
        }
        return alreadyBeenHere(project.getId(), visitedProjectIds);
    }

    private boolean alreadyBeenHere(String id, Set<String> visitedProjectIds) {
        if (visitedProjectIds.contains(id)) {
            return true;
        }
        visitedProjectIds.add(id);
        return false;
    }

    public List<Project> fillClearingStateSummaryIncludingSubprojects(List<Project> projects, User user) {
        final Map<String, Project> allProjectsIdMap = getRefreshedAllProjectsIdMap();

        projects.stream().forEach(project -> {
            // build project tree, get all linked release ids and fetch the releases
            // current decision is to not check any permissions for subproject visibility
            Set<String> releaseIdsOfProjectTree = getReleaseIdsOfProjectTree(project, Sets.newHashSet(),
                    allProjectsIdMap, user, null);
            List<Release> releasesForClearingStateSummary = componentDatabaseHandler
                    .getReleasesForClearingStateSummary(releaseIdsOfProjectTree);
            // compute the summaries
            final ReleaseClearingStateSummary releaseClearingStateSummary = ReleaseClearingStateSummaryComputer
                    .computeReleaseClearingStateSummary(releasesForClearingStateSummary, project.getClearingTeam());

            project.setReleaseClearingStateSummary(releaseClearingStateSummary);
        });

        return projects;
    }

    public Project fillClearingStateSummaryIncludingSubprojectsForSingleProject(Project project, User user) {
        final Map<String, Project> allProjectsIdMap = getRefreshedAllProjectsIdMap();

        Set<String> releaseIdsOfProjectTree = getReleaseIdsOfProjectTree(project, Sets.newHashSet(),
                allProjectsIdMap, user, null);
        List<Release> releasesForClearingStateSummary = componentDatabaseHandler
                .getReleasesForClearingStateSummary(releaseIdsOfProjectTree);
        final ReleaseClearingStateSummary releaseClearingStateSummary = ReleaseClearingStateSummaryComputer
                .computeReleaseClearingStateSummary(releasesForClearingStateSummary, project.getClearingTeam());

        project.setReleaseClearingStateSummary(releaseClearingStateSummary);
        return project;
    }

    /**
     * Synchronization is not really necessary, we could also remove it. Worst case
     * would then be that the projects map would be loaded twice if one thread just
     * loaded it but has not yet updated the loadingInstant field while another
     * thread queries already (if we change the order and set the instant first and
     * then load the new map, worst case would be that the other thread would get an
     * older map as the new one is not yet set).
     */
    private synchronized Map<String, Project> getRefreshedAllProjectsIdMap() {
        if (cachedAllProjectsIdMap != null && Instant.now()
                .isBefore(cachedAllProjectsIdMapLoadingInstant.plus(ALL_PROJECTS_ID_MAP_CACHE_LIFETIME))) {
            return cachedAllProjectsIdMap;
        }

        cachedAllProjectsIdMap = ThriftUtils.getIdMap(repository.getAll());
        cachedAllProjectsIdMapLoadingInstant = Instant.now();

        return cachedAllProjectsIdMap;
    }

    private Set<String> getReleaseIdsOfProjectTree(Project project, Set<String> visitedProjectIds,
            Map<String, Project> allProjectsIdMap, User user, List<RequestedAction> permissionsFilter) {
        // no need to visit a project twice
        if (visitedProjectIds.contains(project.getId())) {
            return Collections.emptySet();
        }

        // we are now checking this project so no need for further examination in
        // recursion
        visitedProjectIds.add(project.getId());

        // container to aggregate results
        Set<String> releaseIds = Sets.newHashSet();

        // traverse linked projects with relation type other than "REFERRED" and
        // "DUPLICATE" and add the result to this result
        if (project.isSetLinkedProjects()) {
            project.getLinkedProjects().entrySet().stream().forEach(e -> {
                if (!ProjectRelationship.REFERRED.equals(e.getValue())
                        && !ProjectRelationship.DUPLICATE.equals(e.getValue())) {
                    Project childProject = allProjectsIdMap.get(e.getKey());
                    if (childProject != null) {
                        // since we fetched all project up front we did not yet check any permission -
                        // so do it now
                        if (permissionsFilter == null || permissionsFilter.isEmpty()
                                || makePermission(childProject, user).areActionsAllowed(permissionsFilter)) {
                            // recursion inside :-)
                            releaseIds.addAll(getReleaseIdsOfProjectTree(childProject, visitedProjectIds,
                                    allProjectsIdMap, user, permissionsFilter));
                        }
                    }
                }
            });
        }

        // add own releases to result if they are not just "REFERRED"
        if (project.isSetReleaseIdToUsage()) {
            project.getReleaseIdToUsage().entrySet().stream().forEach(e -> {
                if (!ReleaseRelationship.REFERRED.equals(e.getValue().getReleaseRelation())) {
                    releaseIds.add(e.getKey());
                }
            });
        }

        return releaseIds;
    }

    private Set<String> getNonOssComponentIdsFromRelease(Collection<Release> releases) {
        if (CommonUtils.isNullOrEmptyCollection(releases)) {
            return Collections.emptySet();
        }
        Set<String> compIds = releases.stream().map(Release::getComponentId).collect(Collectors.toSet());
        List<Component> components = CommonUtils.nullToEmptyList(componentDatabaseHandler.getComponentsShort(compIds));
        return components.stream()
                .filter(c -> c != null && !ComponentType.OSS.equals(c.getComponentType()))
                .map(Component::getId)
                .collect(Collectors.toSet());
    }

    private String extractReleaseNameForClearingEmail(Collection<Release> releases) {
        if (releases.size() < 1) {
            return "<li>No linked release(s) with clearing state new.</li>";
        }

        StringBuilder builder = new StringBuilder();
        for (Release release : releases) {
            builder.append("<li>").append(SW360Utils.printFullname(release)).append("</li>");
        }
        return builder.toString();
    }

    private String appendCompTypeToReleaseVersion(Collection<Release> releases, Set<String> nonOssCompIds, Map<ComponentType, Integer> compTypeCountMap, User user) {
        int count = 0;
        for (Release release : releases) {
            if (nonOssCompIds.contains(release.getComponentId())) {
                count++;
                Component comp = null;
                try {
                    comp = componentDatabaseHandler.getComponent(release.getComponentId(), user);
                } catch (SW360Exception e) {
                    // Log and skip if unable to fetch component
                    log.warn("Could not fetch component for ID: " + release.getComponentId(), e);
                    continue;
                }
                ComponentType compType = (comp != null) ? comp.getComponentType() : null;
                if (compType != null) {
                    compTypeCountMap.put(compType, compTypeCountMap.getOrDefault(compType, 0) + 1);
                }
                release.setVersion(new StringBuilder(CommonUtils.nullToEmptyString(release.getVersion())).append(DatabaseHandlerUtil.SEPARATOR).append(compType != null ? compType : "UNKNOWN").toString());
            }
        }
        return String.valueOf(count);
    }

    private Map<String, String> getRecipients(ClearingRequest cr) {
        Map<String, String> recipients = Maps.newHashMap();
        recipients.put(ClearingRequest._Fields.REQUESTING_USER.toString(), cr.getRequestingUser());
        recipients.put(ClearingRequest._Fields.CLEARING_TEAM.toString(), cr.getClearingTeam());
        return recipients;
    }

    private String getUserDetails(User user) {
        return new StringBuilder(CommonUtils.nullToEmptyString(user.getUserGroup())).append(MailConstants.DASH).append(SW360Utils.printFullname(user)).toString();
    }

    /**
     * Generates HTML for displaying release changes (added/removed releases)
     * This template method creates a formatted HTML section for email notifications
     * showing which releases were added and which were removed from a project
     *
     * @param releases Collection of all releases involved
     * @param addedIds Set of IDs for releases that were added
     * @param removedIds Set of IDs for releases that were removed
     * @return HTML string with formatted changes, or empty string if no changes
     */
    private String generateReleaseChangesHtml(Collection<Release> releases, Set<String> addedIds, Set<String> removedIds) {
        // Return empty string if there are no changes
        if ((addedIds == null || addedIds.isEmpty()) && (removedIds == null || removedIds.isEmpty())) {
            return "";
        }

        StringBuilder html = new StringBuilder();
        html.append("<div style='margin: 15px 0;'>");
        html.append("<h3 style='color: #333; margin-bottom: 10px;'>Release Changes</h3>");

        if (CommonUtils.isNotEmpty(addedIds)) {
            html.append("<div style='margin: 10px 0;'>");
            html.append("<strong style='color: #28a745;'>✓ Added Releases:</strong>");
            html.append("<ul style='margin: 5px 0; padding-left: 20px;'>");
            releases.stream()
                .filter(rel -> addedIds.contains(rel.getId()))
                .forEach(rel -> html.append("<li>")
                    .append(SW360Utils.printFullname(rel))
                    .append("</li>"));
            html.append("</ul>");
            html.append("</div>");
        }

        if (CommonUtils.isNotEmpty(removedIds)) {
            html.append("<div style='margin: 10px 0;'>");
            html.append("<strong style='color: #dc3545;'>✗ Removed Releases:</strong>");
            html.append("<ul style='margin: 5px 0; padding-left: 20px;'>");
            releases.stream()
                .filter(rel -> removedIds.contains(rel.getId()))
                .forEach(rel -> html.append("<li>")
                    .append(SW360Utils.printFullname(rel))
                    .append("</li>"));
            html.append("</ul>");
            html.append("</div>");
        }

        html.append("</div>");
        return html.toString();
    }

    private void sendMailForNewClearing(Project project, String projectUrl, ClearingRequest clearingRequest, User user) {
        project = fillClearingStateSummary(Arrays.asList(project), user).get(0);
        Set<String> releaseIds = CommonUtils.nullToEmptyMap(project.getReleaseIdToUsage()).keySet();
        Collection<Release> releases = componentDatabaseHandler.getReleasesForClearingStateSummary(releaseIds);

        Set<String> nonOssCompIds = getNonOssComponentIdsFromRelease(releases);
        String nonOssCompCount ="0";
        Map<ComponentType, Integer> compTypeCountMap = new HashMap<>();

        if (!nonOssCompIds.isEmpty()) {
            nonOssCompCount = appendCompTypeToReleaseVersion(releases, nonOssCompIds, compTypeCountMap, user);
        }

        String userDetails = getUserDetails(user);
        int totalCount, approvedCount;
        totalCount = approvedCount = 0;
        if (project.isSetReleaseClearingStateSummary()) {
            ReleaseClearingStateSummary clearingSummary = project.getReleaseClearingStateSummary();
            approvedCount = clearingSummary.getApproved();
            totalCount = SW360Utils.getTotalReleaseCount(clearingSummary);
        }
        releases = CommonUtils.nullToEmptyCollection(getDirectlyLinkedReleasesInNewState(releases));
        StringBuilder commentText = new StringBuilder(extractReleaseNameForClearingEmail(releases));
        mailUtil.sendClearingMail(ClearingRequestEmailTemplate.NEW, MailConstants.SUBJECT_FOR_NEW_CLEARING_REQUEST, getRecipients(clearingRequest),
                userDetails, CommonUtils.nullToEmptyString(clearingRequest.getId()), CommonUtils.nullToEmptyString(projectUrl), SW360Utils.printName(project),
                String.valueOf(project.getLinkedProjectsSize()), String.valueOf(project.getReleaseIdToUsageSize()), String.valueOf(totalCount),
                String.valueOf(approvedCount), clearingRequest.getRequestedClearingDate(), nonOssCompCount, commentText.toString());
        if (releases.size() > 0) {
            commentText = new StringBuilder("Linked release(s) with clearing state new:").append(System.lineSeparator()).append(commentText);
        }
        commentText.append(System.lineSeparator()).append("Total number of non-OSS components:").append(nonOssCompCount);
        Comment comment = new Comment(commentText.toString(), user.getEmail()).setAutoGenerated(true);
        moderator.addCommentToClearingRequest(clearingRequest.getId(), comment, user);
    }

    private void sendMailForUpdatedCR(Project project, String projectUrl, ClearingRequest clearingRequest, User user) {
        List<Release> releases = getDirectlyLinkedReleasesInNewState(project);
        String userDetails = getUserDetails(user);
        mailUtil.sendClearingMail(ClearingRequestEmailTemplate.UPDATED, MailConstants.SUBJECT_FOR_UPDATED_CLEARING_REQUEST, getRecipients(clearingRequest),
                userDetails, CommonUtils.nullToEmptyString(clearingRequest.getId()), CommonUtils.nullToEmptyString(projectUrl), SW360Utils.printName(project),
                CommonUtils.getEnumStringOrNull(clearingRequest.getClearingState()), clearingRequest.getRequestedClearingDate(),
                CommonUtils.nullToEmptyString(clearingRequest.getAgreedClearingDate()), extractReleaseNameForClearingEmail(releases));
    }

    private void sendMailForUpdatedProjectWithClearingRequest(Project updated, Collection<Release> releases, String nonOssCompCount, Map<ComponentType, Integer> compTypeCountMap, User user, String releaseChangesHtml) {
        updated = fillClearingStateSummary(Arrays.asList(updated), user).get(0);
        ClearingRequest clearingRequest = moderator.getClearingRequestByProjectId(updated.getId(), user);
        String userDetails = getUserDetails(user);
        int totalCount, approvedCount;
        totalCount = approvedCount = 0;
        if (updated.isSetReleaseClearingStateSummary()) {
            ReleaseClearingStateSummary clearingSummary = updated.getReleaseClearingStateSummary();
            approvedCount = clearingSummary.getApproved();
            totalCount = SW360Utils.getTotalReleaseCount(clearingSummary);
        }
        releases = getDirectlyLinkedReleasesInNewState(releases);
        mailUtil.sendClearingMail(ClearingRequestEmailTemplate.PROJECT_UPDATED, MailConstants.SUBJECT_FOR_UPDATED_PROJECT_WITH_CLEARING_REQUEST,
                getRecipients(clearingRequest), userDetails, SW360Utils.printName(updated), updated.getClearingRequestId(),
                releaseChangesHtml, // HTML-formatted release changes
                String.valueOf(updated.getLinkedProjectsSize()), String.valueOf(updated.getReleaseIdToUsageSize()), String.valueOf(totalCount),
                String.valueOf(approvedCount), CommonUtils.getEnumStringOrNull(clearingRequest.getClearingState()),
                clearingRequest.getRequestedClearingDate(), CommonUtils.nullToEmptyString(clearingRequest.getAgreedClearingDate()),
                nonOssCompCount, extractReleaseNameForClearingEmail(releases));
    }

    private void sendMailForClosedOrRejectedCR(Project project, ClearingRequest clearingRequest, User user, boolean isApproved) {
        mailUtil.sendClearingMail(isApproved ? ClearingRequestEmailTemplate.CLOSED : ClearingRequestEmailTemplate.REJECTED,
                isApproved ? MailConstants.SUBJECT_FOR_CLOSED_CLEARING_REQUEST : MailConstants.SUBJECT_FOR_REJECTED_CLEARING_REQUEST,
                getRecipients(clearingRequest), project.getClearingRequestId(), SW360Utils.printName(project), isApproved ? "closed" : "rejected");
    }

    private void sendMailNotificationsForNewProject(Project project, String user) {
        mailUtil.sendMail(project.getProjectResponsible(),
                MailConstants.SUBJECT_FOR_NEW_PROJECT,
                MailConstants.TEXT_FOR_NEW_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.PROJECT_RESPONSIBLE.toString(),
                project.getName(), project.getVersion());
        mailUtil.sendMail(project.getProjectOwner(),
                MailConstants.SUBJECT_FOR_NEW_PROJECT,
                MailConstants.TEXT_FOR_NEW_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.PROJECT_OWNER.toString(),
                project.getName(), project.getVersion());
        mailUtil.sendMail(project.getLeadArchitect(),
                MailConstants.SUBJECT_FOR_NEW_PROJECT,
                MailConstants.TEXT_FOR_NEW_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.LEAD_ARCHITECT.toString(),
                project.getName(), project.getVersion());
        mailUtil.sendMail(project.getModerators(), user,
                MailConstants.SUBJECT_FOR_NEW_PROJECT,
                MailConstants.TEXT_FOR_NEW_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.MODERATORS.toString(),
                project.getName(), project.getVersion());
        mailUtil.sendMail(project.getContributors(), user,
                MailConstants.SUBJECT_FOR_NEW_PROJECT,
                MailConstants.TEXT_FOR_NEW_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.CONTRIBUTORS.toString(),
                project.getName(), project.getVersion());
        mailUtil.sendMail(project.getSecurityResponsibles(), user,
                MailConstants.SUBJECT_FOR_NEW_PROJECT,
                MailConstants.TEXT_FOR_NEW_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.SECURITY_RESPONSIBLES.toString(),
                project.getName(), project.getVersion());
        mailUtil.sendMail(SW360Utils.unionValues(project.getRoles()), user,
                MailConstants.SUBJECT_FOR_NEW_PROJECT,
                MailConstants.TEXT_FOR_NEW_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.ROLES.toString(),
                project.getName(), project.getVersion());
    }

    private void sendMailNotificationsForProjectUpdate(Project newProject, Project oldProject, User user) {
        // Generate detailed change summary
        String changesSummary = getProjectChangesSummary(newProject, oldProject, user);

        // Get user details for "Modified by" information
        String modifiedBy = SW360Utils.printFullname(user);

        // Prepare enhanced notification text with changes
        String projectName = newProject.getName();
        String projectVersion = nullToEmptyString(newProject.getVersion());

        mailUtil.sendMail(newProject.getProjectResponsible(),
                MailConstants.SUBJECT_FOR_UPDATE_PROJECT,
                MailConstants.TEXT_FOR_UPDATE_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.PROJECT_RESPONSIBLE.toString(),
                projectName, projectVersion, modifiedBy, changesSummary);
        mailUtil.sendMail(newProject.getProjectOwner(),
                MailConstants.SUBJECT_FOR_UPDATE_PROJECT,
                MailConstants.TEXT_FOR_UPDATE_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.PROJECT_OWNER.toString(),
                projectName, projectVersion, modifiedBy, changesSummary);
        mailUtil.sendMail(newProject.getLeadArchitect(),
                MailConstants.SUBJECT_FOR_UPDATE_PROJECT,
                MailConstants.TEXT_FOR_UPDATE_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.LEAD_ARCHITECT.toString(),
                projectName, projectVersion, modifiedBy, changesSummary);
        mailUtil.sendMail(newProject.getModerators(), user.getEmail(),
                MailConstants.SUBJECT_FOR_UPDATE_PROJECT,
                MailConstants.TEXT_FOR_UPDATE_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.MODERATORS.toString(),
                projectName, projectVersion, modifiedBy, changesSummary);
        mailUtil.sendMail(newProject.getContributors(), user.getEmail(),
                MailConstants.SUBJECT_FOR_UPDATE_PROJECT,
                MailConstants.TEXT_FOR_UPDATE_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.CONTRIBUTORS.toString(),
                projectName, projectVersion, modifiedBy, changesSummary);
        mailUtil.sendMail(newProject.getSecurityResponsibles(), user.getEmail(),
                MailConstants.SUBJECT_FOR_UPDATE_PROJECT,
                MailConstants.TEXT_FOR_UPDATE_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.SECURITY_RESPONSIBLES.toString(),
                projectName, projectVersion, modifiedBy, changesSummary);
        mailUtil.sendMail(SW360Utils.unionValues(newProject.getRoles()), user.getEmail(),
                MailConstants.SUBJECT_FOR_UPDATE_PROJECT,
                MailConstants.TEXT_FOR_UPDATE_PROJECT,
                SW360Constants.NOTIFICATION_CLASS_PROJECT, Project._Fields.ROLES.toString(),
                projectName, projectVersion, modifiedBy, changesSummary);
    }

    /**
     * Generates a detailed summary of changes between old and new project states.
     * This includes changes to releases, linked projects, project state, clearing state, and description.
     *
     * @param newProject The updated project state
     * @param oldProject The previous project state
     * @param user The user making the changes
     * @return A formatted string summarizing all changes
     */
    private String getProjectChangesSummary(Project newProject, Project oldProject, User user) {
        StringBuilder changes = new StringBuilder();
        boolean hasChanges = false;

        // Track release changes
        Set<String> oldReleaseIds = getNullToEmptyKeyset(oldProject.getReleaseIdToUsage());
        Set<String> newReleaseIds = getNullToEmptyKeyset(newProject.getReleaseIdToUsage());

        Set<String> addedReleaseIds = Sets.difference(newReleaseIds, oldReleaseIds);
        Set<String> removedReleaseIds = Sets.difference(oldReleaseIds, newReleaseIds);

        if (!addedReleaseIds.isEmpty() || !removedReleaseIds.isEmpty()) {
            changes.append("- Releases:\n");
            hasChanges = true;

            if (!addedReleaseIds.isEmpty()) {
                Collection<Release> addedReleases = componentDatabaseHandler.getReleasesForClearingStateSummary(addedReleaseIds);
                String addedNames = extractReleaseNamesForEmail(addedReleases);
                changes.append("  Added: ").append(addedNames).append("\n");
            }

            if (!removedReleaseIds.isEmpty()) {
                Collection<Release> removedReleases = componentDatabaseHandler.getReleasesForClearingStateSummary(removedReleaseIds);
                String removedNames = extractReleaseNamesForEmail(removedReleases);
                changes.append("  Removed: ").append(removedNames).append("\n");
            }
        }

        // Track linked project changes
        Set<String> oldLinkedProjects = nullToEmptyMap(oldProject.getLinkedProjects()).keySet();
        Set<String> newLinkedProjects = nullToEmptyMap(newProject.getLinkedProjects()).keySet();

        Set<String> addedProjectIds = Sets.difference(newLinkedProjects, oldLinkedProjects);
        Set<String> removedProjectIds = Sets.difference(oldLinkedProjects, newLinkedProjects);

        if (!addedProjectIds.isEmpty() || !removedProjectIds.isEmpty()) {
            changes.append("- Linked Projects:\n");
            hasChanges = true;

            if (!addedProjectIds.isEmpty()) {
                List<Project> addedProjects = getProjectsById(new ArrayList<>(addedProjectIds), user);
                String addedNames = extractProjectNamesForEmail(addedProjects);
                changes.append("  Added: ").append(addedNames).append("\n");
            }

            if (!removedProjectIds.isEmpty()) {
                List<Project> removedProjects = getProjectsById(new ArrayList<>(removedProjectIds), user);
                String removedNames = extractProjectNamesForEmail(removedProjects);
                changes.append("  Removed: ").append(removedNames).append("\n");
            }
        }


        if (!hasChanges) {
            return "- General project information updated";
        }

        return changes.toString();
    }

    /**
     * Extracts release names from a collection of releases and formats them for email display.
     *
     * @param releases Collection of releases to extract names from
     * @return Comma-separated list of release names with versions
     */
    private String extractReleaseNamesForEmail(Collection<Release> releases) {
        if (releases == null || releases.isEmpty()) {
            return "(none)";
        }

        return releases.stream()
                .map(SW360Utils::printFullname)
                .collect(Collectors.joining(", "));
    }

    /**
     * Extracts project names from a collection of projects and formats them for email display.
     *
     * @param projects Collection of projects to extract names from
     * @return Comma-separated list of project names with versions
     */
    private String extractProjectNamesForEmail(Collection<Project> projects) {
        if (projects == null || projects.isEmpty()) {
            return "(none)";
        }

        return projects.stream()
                .map(SW360Utils::printName)
                .collect(Collectors.joining(", "));
    }

    /**
     * CAUTION!
     * You should most probably use {@link #getAccessibleProjects(User)} instead of this method.
     * This method reads all the projects without checking for visibility constraints. It is intended to provide
     * all projects for export from a scheduled service, where a valid sw360 user is not available.
     * This is a less than optimal situation, but that's the way it is at the moment.
     * @return list of all projects in the database
     */
    private List<Project> getAllProjects() {
        return nullToEmptyList(repository.getAll());
    }

    @NotNull
    private JsonArray serializeProjectsToJson(List<Project> projects) {
        JsonArray  serializedProjects = new JsonArray();
        Map<String, Release> releaseMap = componentDatabaseHandler.getAllReleasesIdMap();
        componentDatabaseHandler.fillVendors(releaseMap.values());
        Map<String, Component> componentMap = componentDatabaseHandler.getAllComponentsIdMap();
        for (Project p : projects) {
            Set<String> linkedProjectIds = nullToEmptyMap(p.getLinkedProjects()).entrySet().stream().filter(entry -> {
                ProjectProjectRelationship projectProjectRelationship = entry.getValue();
                return projectProjectRelationship != null && projectProjectRelationship.isEnableSvm();
            }).map(entry -> entry.getKey()).collect(Collectors.toSet());
            JsonObject json = new JsonObject();
            json.addProperty("application_id", p.getId());
            json.addProperty("application_name", p.getName());
            json.addProperty("application_version", p.getVersion());
            json.addProperty("business_unit", p.getBusinessUnit());
            json.add("user_gids", stringCollectionToJsonArray(nullToEmptySet(p.getSecurityResponsibles())));
            json.add("children_application_ids",
                    p.isConsiderReleasesFromExternalList() ? new JsonArray()
                            : stringCollectionToJsonArray(linkedProjectIds));
            json.add("components",
                    p.isConsiderReleasesFromExternalList() ? new JsonArray()
                            : serializeReleasesToJson(nullToEmptyMap(p.getReleaseIdToUsage()).keySet().stream()
                                    .map(releaseMap::get).filter(Objects::nonNull).collect(Collectors.toList()),
                                    componentMap));

            Set<String> externalIdValueSet = new TreeSet<>();
            String externalIdValues = p.getExternalIds() == null ? null
                    : p.getExternalIds().get(SW360Constants.SVM_MONITORINGLIST_ID);
            if (CommonUtils.isNotNullEmptyOrWhitespace(externalIdValues)) {
                try {
                    externalIdValueSet = mapper.readValue(externalIdValues, Set.class);
                } catch (IOException e) {
                    externalIdValueSet.add(externalIdValues);
                }
                Set<String> filteredExternalIds = externalIdValueSet.stream()
                        .filter(externalId -> externalId.length() == 8).collect(Collectors.toSet());
                if (CommonUtils.isNotEmpty(filteredExternalIds)) {
                    json.add("svm_children_ml_ids", stringCollectionToJsonArray(filteredExternalIds));
                }
            }
            serializedProjects.add(json);
        }
        return serializedProjects;
    }

    private JsonArray stringCollectionToJsonArray(Collection<String> strings) {
        JsonArray jsonArray = new JsonArray();
        strings.forEach(jsonArray::add);
        return jsonArray;
    }

    private JsonArray serializeReleasesToJson(List<Release> releases, Map<String, Component> componentMap) {
        JsonArray serializedReleases = new JsonArray();
        for (Release r: releases){
            Component c = componentMap.get(r.getComponentId());
            JsonObject json = new JsonObject();
            Map<String, String> externalIds = CommonUtils.nullToEmptyMap(r.getExternalIds());
            json.addProperty("external_component_id", r.getId());
            putExternalIdToJsonAsInteger(json, "svm_component_id", externalIds.get(SW360Constants.SVM_COMPONENT_ID));
            putExternalIdToJsonAsInteger(json, "swml_component_id", externalIds.get(SW360Constants.MAINLINE_COMPONENT_ID));
            json.addProperty("vendor", Optional.ofNullable(r.getVendor()).map(Vendor::getShortname).orElse(""));
            json.addProperty("name", r.getName());
            json.addProperty("version", r.getVersion());
            if (c == null) {
                log.warn(String.format("Parent component of release %s (%s) with id %s was not found", r.getName(), r.getId(), r.getComponentId()));
            } else {
                json.add("urls", getUrlsJson(r, c));
                json.addProperty("description", c.getDescription());
            }
            json.add("cpe_items", getReleaseCpeIdsJson(r));
            serializedReleases.add(json);
        }
        return serializedReleases;
    }

    private void putExternalIdToJsonAsInteger(JsonObject json, String jsonKey, String extId){
        if (extId == null){
            json.add(jsonKey, (JsonObject) null);
        }
        try {
            json.addProperty(jsonKey, Integer.parseInt(extId));
        } catch (NumberFormatException e){
            json.add(jsonKey, (JsonObject) null);
        }
    }

    private JsonArray putIfNotEmpty(JsonArray array, String value){
        if (!isNullOrEmpty(value)){
            array.add(value);
        }
        return array;
    }

    private JsonArray getReleaseCpeIdsJson(Release r) {
        JsonArray jsonArray = new JsonArray();
        putIfNotEmpty(jsonArray, r.getCpeid());
        return jsonArray;
    }

    private JsonArray getUrlsJson(Release r, Component c) {
        JsonArray jsonArray = new JsonArray();
        putIfNotEmpty(jsonArray, c.getHomepage());
        putIfNotEmpty(jsonArray, c.getBlog());
        putIfNotEmpty(jsonArray, c.getWiki());
        putIfNotEmpty(jsonArray, r.getSourceCodeDownloadurl());
        return jsonArray;
    }

    public RequestStatus exportForMonitoringList() throws TException {
        // load all projects
        log.info("SVMML: Starting export of projects to SVM monitoring lists");
        List<Project> allProjects = getAllProjects();
        log.info("SVMML: successfully loaded " + allProjects.size() + " projects");
        List<Project> projects = prepareProjectsForSVM(allProjects);
        log.info("SVMML: after filtering out projects with missing security responsibles, " + projects.size() + " projects are left");
        // serialize projects to json
        JsonArray jsonResult = serializeProjectsToJson(projects);
        String jsonString = jsonResult.toString();
        log.info("SVMML: projects serialized to JSON string. String length: " + jsonString.length());
        log.info("SVMML: JSON starts with: " + StringUtils.abbreviate(jsonString, SVMML_JSON_LOG_CUTOFF_LENGTH));

        try {
            DatabaseHandlerUtil.writeToFile(jsonString);
        } catch (Exception e) {
            log.warn("Unable to write to Json Output to File . ", e);
        }

        // send json to svm
        try {
            new SvmConnector().sendProjectExportForMonitoringLists(jsonString);
            log.info("SVMML: sent JSON to SVM");
        } catch (IOException | SW360Exception e) {
            log.error(e);
            return RequestStatus.FAILURE;
        }

        return RequestStatus.SUCCESS;
    }

    private List<Project> prepareProjectsForSVM(List<Project> projects) throws TException {
        Map<String, String> gidsByEmail = getGidsByEmail();

        // convert all security responsibles to gids
        projects.forEach(p -> {
            Set<String> emails = nullToEmptySet(p.getSecurityResponsibles());
            Set<String> gids = emails
                    .stream()
                    .map(gidsByEmail::get)
                    .filter(Objects::nonNull)
                    .filter(s -> PLAUSIBLE_GID_REGEXP.matcher(s).matches())
                    .collect(Collectors.toSet());
            if (emails.size() != gids.size()){
                log.warn("SVMML: couldn't find gids for some of the emails from project " + SW360Utils.printName(p) + " " + p.getId());
            }
            p.setSecurityResponsibles(gids);

            // Here comes the tricky part.
            // If SVM is disabled, clear security responsibles, but enable SVM.
            // This way, the project will be sent to SVM only if it gets some propagated secreps
            // from parent projects and only the propagated secreps will get the notifications.
            // At the same time, only projects that had enableSVM from the start or subprojects of such projects
            // will be sent to SVM.
            if (!p.isEnableSvm()){
                p.setSecurityResponsibles(Collections.emptySet());
                p.setEnableSvm(true);
            }
        });

        // create copies for propagating responsibles to
        // propagating directly to originals will cause unnecessary propagation when the iteration comes to
        // the subprojects
        List<Project> projectCopies = projects.stream().map(Project::new).collect(Collectors.toList());
        Map<String, Project> projectsById = ThriftUtils.getIdMap(projectCopies);

        // propagate security responsibles' gids to subprojects
        // take the responsibles from the original projects, not the copies
        projects.forEach(p -> {
            Set<String> responsibles = nullToEmptySet(p.getSecurityResponsibles());
            Set<String> linkedProjectIds = nullToEmptyMap(p.getLinkedProjects()).entrySet().stream().filter(entry -> {
                ProjectProjectRelationship projectProjectRelationship = entry.getValue();
                return projectProjectRelationship != null && projectProjectRelationship.isEnableSvm();
            }).map(entry -> entry.getKey()).collect(Collectors.toSet());
            if (!responsibles.isEmpty() && !linkedProjectIds.isEmpty()) {
                propagateSecurityResponsiblesToLinkedProjects(responsibles, linkedProjectIds, projectsById, new HashSet<>());
            }
        });

        // return the copies, not the originals
        return projects
                .stream()
                .map(p -> projectsById.get(p.getId()))
                .filter(p -> p.isSetSecurityResponsibles() && !p.getSecurityResponsibles().isEmpty())
                .filter(Project::isEnableSvm)
                .collect(Collectors.toList());
    }

    private void propagateSecurityResponsiblesToLinkedProjects(Set<String> responsibles, Set<String> linkedProjectIds, Map<String, Project> projectsById, HashSet<String> visitedIds) {
        linkedProjectIds.stream().map(projectsById::get).filter(Objects::nonNull).forEach(p -> {
            if (!visitedIds.contains(p.getId())) {
                Set<String> currentResponsibles = nullToEmptySet(p.getSecurityResponsibles());
                currentResponsibles.addAll(responsibles);
                p.setSecurityResponsibles(currentResponsibles);
                visitedIds.add(p.getId());
                propagateSecurityResponsiblesToLinkedProjects(responsibles, nullToEmptyMap(p.getLinkedProjects()).keySet(), projectsById, visitedIds);
            }
        });
    }

    private Map<String, String> getGidsByEmail() throws TException {
        ThriftClients thriftClients = new ThriftClients();
        UserService.Iface userClient = thriftClients.makeUserClient();
        Map<String, String> gidByEmail = new HashMap<>();
        userClient
                .getAllUsers()
                .stream()
                .filter(User::isSetExternalid)
                .forEach(user -> {
                    gidByEmail.put(user.getEmail(), user.getExternalid());
                    nullToEmptySet(user.getFormerEmailAddresses())
                            .forEach(email -> gidByEmail.put(email, user.getExternalid()));
                });
        return gidByEmail;
    }

    public List<UsedReleaseRelations> getUsedReleaseRelationsByProjectId(String projectId) throws TException {
        return relUsageRepository.getUsedRelationsByProjectId(projectId);
    }

    public void deleteReleaseRelationsUsage(UsedReleaseRelations usedReleaseRelations) throws TException {
        String usedReleaseRelationsId = usedReleaseRelations.getId();
        relUsageRepository.remove(usedReleaseRelationsId);
    }

    public void addReleaseRelationsUsage(UsedReleaseRelations usedReleaseRelations) throws TException {
        relUsageRepository.add(usedReleaseRelations);
    }

    public void updateReleaseRelationsUsage(UsedReleaseRelations usedReleaseRelations) throws TException {
        relUsageRepository.update(usedReleaseRelations);
    }

    public RequestSummary importBomFromAttachmentContent(User user, String attachmentContentId) throws SW360Exception {
        final AttachmentContent attachmentContent = attachmentConnector.getAttachmentContent(attachmentContentId);
        final Duration timeout = Duration.durationOf(30, TimeUnit.SECONDS);
        try {
            final AttachmentStreamConnector attachmentStreamConnector = new AttachmentStreamConnector(timeout);
            try (final InputStream inputStream = attachmentStreamConnector.unsafeGetAttachmentStream(attachmentContent)) {
                final SpdxBOMImporterSink spdxBOMImporterSink = new SpdxBOMImporterSink(user, this, componentDatabaseHandler);
                final SpdxBOMImporter spdxBOMImporter = new SpdxBOMImporter(spdxBOMImporterSink);
                return spdxBOMImporter.importSpdxBOMAsProject(inputStream, attachmentContent, user);
            }
        } catch (IOException e) {
            throw new SW360Exception(e.getMessage());
        }
    }

    public ImportBomDryRunReport dryRunImportBom(User user, String filename, ByteBuffer bomContent) throws SW360Exception {
        ImportBomDryRunReport report = new ImportBomDryRunReport();
        report.setRequestStatus(RequestStatus.FAILURE);
        report.setNewComponents(new HashSet<>());
        report.setExistingComponents(new HashSet<>());
        report.setLicenseConflicts(new HashSet<>());
        report.setWarnings(new HashSet<>());

        if (isNullEmptyOrWhitespace(filename)) {
            report.getWarnings().add("Filename is required for SPDX dry-run import.");
            return report;
        }
        if (bomContent == null || !bomContent.hasRemaining()) {
            report.getWarnings().add("SBOM content is empty.");
            return report;
        }

        String fileType = getFileType(filename);
        if (!"rdf".equals(fileType) && !"spdx".equals(fileType)) {
            report.getWarnings().add("Invalid SBOM file. Only SPDX(.rdf/.spdx) files are supported in dry-run mode.");
            return report;
        }

        String ext = "." + fileType;
        byte[] content = getByteArrayFromByteBuffer(bomContent);
        try (InputStream inputStream = new ByteArrayInputStream(content)) {
            File sourceFile = DatabaseHandlerUtil.saveAsTempFile(inputStream, "spdx-dry-run", ext);
            List<SpdxPackageDryRunData> packageData = getSpdxPackageDryRunData(sourceFile, report.getWarnings());

            if (packageData.isEmpty()) {
                if (report.getWarnings().isEmpty()) {
                    report.getWarnings().add("No SPDX packages were detected in the uploaded file.");
                }
                return report;
            }

            Set<String> uniqueReleasesInBom = new HashSet<>();
            for (SpdxPackageDryRunData pkg : packageData) {
                if (isNullEmptyOrWhitespace(pkg.name())) {
                    report.getWarnings().add("Encountered an SPDX package entry without name.");
                    continue;
                }

                if (isNullEmptyOrWhitespace(pkg.version())) {
                    report.getWarnings().add(String.format("Package '%s' does not declare a version.", pkg.name()));
                }

                String releaseKey = pkg.name() + ":" + pkg.version();
                if (!uniqueReleasesInBom.add(releaseKey)) {
                    report.getWarnings().add(String.format("Duplicate package entry in SPDX file: %s %s", pkg.name(), pkg.version()));
                }

                if (componentDatabaseHandler.isDuplicate(pkg.name(), true)) {
                    report.getExistingComponents().add(pkg.name());
                } else {
                    report.getNewComponents().add(pkg.name());
                }

                List<Release> existingReleases = releaseRepository.searchByNameAndVersion(pkg.name(), pkg.version(), true);
                if (!existingReleases.isEmpty()) {
                    report.getWarnings().add(String.format("Existing release detected: %s %s", pkg.name(), pkg.version()));
                    checkLicenseConflict(pkg, existingReleases, report.getLicenseConflicts());
                }
            }

            report.setRequestStatus(RequestStatus.SUCCESS);
            return report;
        } catch (IOException e) {
            throw new SW360Exception(e.getMessage());
        }
    }

    private List<SpdxPackageDryRunData> getSpdxPackageDryRunData(File sourceFile, Set<String> warnings) throws SW360Exception {
        SpdxDocument spdxDocument = openAsSpdx(sourceFile);
        if (spdxDocument == null) {
            return Collections.emptyList();
        }

        try {
            List<SpdxPackage> describedPackages = spdxDocument.getDocumentDescribes().stream()
                    .filter(SpdxPackage.class::isInstance)
                    .map(SpdxPackage.class::cast)
                    .collect(Collectors.toList());
            if (describedPackages.isEmpty()) {
                warnings.add("The provided BOM did not contain any top level packages.");
                return Collections.emptyList();
            }
            if (describedPackages.size() > 1) {
                warnings.add("The provided BOM file contained multiple described top level packages. This is not allowed here.");
                return Collections.emptyList();
            }
        } catch (InvalidSPDXAnalysisException e) {
            warnings.add("Failed to inspect SPDX document structure.");
            return Collections.emptyList();
        }

        try (@SuppressWarnings("unchecked")
             Stream<SpdxPackage> allPackagesStream = (Stream<SpdxPackage>) SpdxModelFactory.getSpdxObjects(
                     spdxDocument.getModelStore(),
                     spdxDocument.getCopyManager(),
                     SpdxConstantsCompatV2.CLASS_SPDX_PACKAGE,
                     spdxDocument.getDocumentUri(),
                     null)) {
            List<SpdxPackageDryRunData> packageData = new ArrayList<>();
            allPackagesStream.forEach(spdxPackage -> packageData.add(toDryRunData(spdxPackage, warnings)));
            return packageData;
        } catch (InvalidSPDXAnalysisException e) {
            warnings.add("Failed to parse SPDX packages from the document.");
            return Collections.emptyList();
        }
    }

    private SpdxDocument openAsSpdx(File file) throws SW360Exception {
        try {
            return SpdxToolsHelper.deserializeDocumentCompatV2(file);
        } catch (InvalidSPDXAnalysisException | IOException | InvalidFileNameException e) {
            throw new SW360Exception(e.getMessage());
        }
    }

    private String getConcludedLicense(SpdxPackage spdxPackage) {
        try {
            return String.valueOf(spdxPackage.getLicenseConcluded());
        } catch (InvalidSPDXAnalysisException e) {
            return "";
        }
    }

    private SpdxPackageDryRunData toDryRunData(SpdxPackage spdxPackage, Set<String> warnings) {
        try {
            return new SpdxPackageDryRunData(
                    normalizeSpdxValue(spdxPackage.getName()),
                    normalizeSpdxValue(spdxPackage.getVersionInfo()),
                    normalizeSpdxValue(getConcludedLicense(spdxPackage)));
        } catch (InvalidSPDXAnalysisException e) {
            warnings.add("Failed to read metadata from an SPDX package entry.");
            return new SpdxPackageDryRunData("", "", "");
        }
    }

    private String normalizeSpdxValue(Object value) {
        if (value == null) {
            return "";
        }
        String normalized = String.valueOf(value).trim();
        if ("Optional.empty".equalsIgnoreCase(normalized)) {
            return "";
        }
        if (normalized.startsWith("Optional[") && normalized.endsWith("]")) {
            normalized = normalized.substring("Optional[".length(), normalized.length() - 1);
        }
        return normalized;
    }

    private byte[] getByteArrayFromByteBuffer(ByteBuffer byteBuffer) {
        ByteBuffer duplicate = byteBuffer.duplicate();
        byte[] bytes = new byte[duplicate.remaining()];
        duplicate.get(bytes);
        return bytes;
    }

    private void checkLicenseConflict(SpdxPackageDryRunData pkg, List<Release> existingReleases, Set<String> conflicts) {
        if (isNullEmptyOrWhitespace(pkg.declaredLicense()) || isNoAssertionLicense(pkg.declaredLicense())) {
            return;
        }

        for (Release existingRelease : existingReleases) {
            Set<String> mainLicenseIds = nullToEmptySet(existingRelease.getMainLicenseIds());
            if (mainLicenseIds.isEmpty()) {
                continue;
            }

            boolean matched = mainLicenseIds.stream()
                    .filter(Objects::nonNull)
                    .anyMatch(licenseId -> pkg.declaredLicense().equalsIgnoreCase(licenseId));
            if (!matched) {
                conflicts.add(String.format(
                        "Release '%s %s' has existing licenses %s but SPDX declares '%s'.",
                        pkg.name(),
                        pkg.version(),
                        mainLicenseIds,
                        pkg.declaredLicense()));
            }
        }
    }

    private boolean isNoAssertionLicense(String licenseExpression) {
        if (licenseExpression == null) {
            return true;
        }
        String normalized = licenseExpression.trim();
        return normalized.isEmpty()
                || "NOASSERTION".equalsIgnoreCase(normalized)
                || "Optional[NOASSERTION]".equalsIgnoreCase(normalized);
    }

    private record SpdxPackageDryRunData(String name, String version, String declaredLicense) { }

    public RequestSummary importCycloneDxFromAttachmentContent(User user, String attachmentContentId, String projectId) throws SW360Exception {
        return importCycloneDxFromAttachmentContent(user, attachmentContentId, projectId, false);
    }

    public RequestSummary importCycloneDxFromAttachmentContent(User user, String attachmentContentId, String projectId, boolean doNotReplacePackageAndRelease) throws SW360Exception {
        final AttachmentContent attachmentContent = attachmentConnector.getAttachmentContent(attachmentContentId);
        final Duration timeout = Duration.durationOf(30, TimeUnit.SECONDS);
        try {
            final AttachmentStreamConnector attachmentStreamConnector = new AttachmentStreamConnector(timeout);
            try (final InputStream inputStream = attachmentStreamConnector
                    .unsafeGetAttachmentStream(attachmentContent)) {
                final CycloneDxBOMImporter cycloneDxBOMImporter = new CycloneDxBOMImporter(this,
                        componentDatabaseHandler, packageDatabaseHandler, attachmentConnector, user);
                return cycloneDxBOMImporter.importFromBOM(inputStream, attachmentContent, projectId, user, doNotReplacePackageAndRelease);
            }
        } catch (IOException e) {
            log.error("Error while importing / parsing CycloneDX SBOM! ", e);
            throw new SW360Exception(e.getMessage());
        }
    }

    public RequestSummary exportCycloneDxSbom(String projectId, String bomType, Boolean includeSubProjReleases, User user) throws SW360Exception {
        try {
            final CycloneDxBOMExporter cycloneDxBOMExporter = new CycloneDxBOMExporter(this, componentDatabaseHandler, packageDatabaseHandler, user);
            return cycloneDxBOMExporter.exportSbom(projectId, bomType, includeSubProjReleases, user);
        } catch (Exception e) {
            log.error("Error while exporting CycloneDX SBOM! ", e);
            throw new SW360Exception(e.getMessage());
        }
    }

    public String getSbomImportInfoFromAttachmentAsString(String attachmentContentId) throws SW360Exception {
        final AttachmentContent attachmentContent = attachmentConnector.getAttachmentContent(attachmentContentId);
        final Duration timeout = Duration.durationOf(30, TimeUnit.SECONDS);
        try {
            final AttachmentStreamConnector attachmentStreamConnector = new AttachmentStreamConnector(timeout);
            try (final InputStream inputStream = attachmentStreamConnector
                    .unsafeGetAttachmentStream(attachmentContent)) {
                return IOUtils.toString(inputStream, Charset.defaultCharset());
            }
        } catch (IOException e) {
            log.error("Error while getting sbom import info from attachment! ", e);
            throw new SW360Exception(e.getMessage());
        }
    }

    private void removeLeadingTrailingWhitespace(Project project) {
        DatabaseHandlerUtil.trimStringFields(project, listOfStringFieldsInProjToTrim);

        project.setAttachments(DatabaseHandlerUtil.trimSetOfAttachement(project.getAttachments()));

        project.setContributors(DatabaseHandlerUtil.trimSetOfString(project.getContributors()));

        project.setSecurityResponsibles(DatabaseHandlerUtil.trimSetOfString(project.getSecurityResponsibles()));

        project.setModerators(DatabaseHandlerUtil.trimSetOfString(project.getModerators()));

        project.setExternalIds(DatabaseHandlerUtil.trimMapOfStringKeyStringValue(project.getExternalIds()));

        project.setAdditionalData(DatabaseHandlerUtil.trimMapOfStringKeyStringValue(project.getAdditionalData()));

        project.setRoles(DatabaseHandlerUtil.trimMapOfStringKeySetValue(project.getRoles()));

        project.setTag(DatabaseHandlerUtil.trimProjectTag(project.getTag()));
    }

    public List<Map<String, String>> getClearingStateInformationForListView(String projectId, User user, boolean isInaccessibleLinkMasked)
            throws SW360Exception {
        Project projectById = getProjectById(projectId, user);
        List<Map<String, String>> clearingStatusList = new ArrayList<Map<String, String>>();
        LinkedHashMap<String, String> projectOrigin = new LinkedHashMap<>();
        projectOrigin.put(projectId, SW360Utils.printName(projectById));
        LinkedHashMap<String, String> releaseOrigin = new LinkedHashMap<>();
        Map<String, ProjectProjectRelationship> linkedProjects = projectById.getLinkedProjects();
        Map<String, ProjectReleaseRelationship> releaseIdToUsage = projectById.getReleaseIdToUsage();
        if (linkedProjects != null && !linkedProjects.isEmpty()) {
            flattenClearingStatusForLinkedProject(linkedProjects, projectOrigin, releaseOrigin, clearingStatusList,
                    user, isInaccessibleLinkMasked);
        }
        if (releaseIdToUsage != null && !releaseIdToUsage.isEmpty()) {
            flattenClearingStatusForReleases(releaseIdToUsage, projectOrigin, releaseOrigin, clearingStatusList, user, isInaccessibleLinkMasked);
        }

        return clearingStatusList;
    }

    private void flattenClearingStatusForLinkedProject(Map<String, ProjectProjectRelationship> linkedProjects,
            LinkedHashMap<String, String> projectOrigin, LinkedHashMap<String, String> releaseOrigin,
            List<Map<String, String>> clearingStatusList, User user, boolean isInaccessibleLinkMasked) {

        linkedProjects.entrySet().stream().forEach(lp -> wrapTException(() -> {
            String projId = lp.getKey();
            String relation = ThriftEnumUtils.enumToString(lp.getValue().getProjectRelationship());
            if (projectOrigin.containsKey(projId))
                return;
            Project linkedProjectById = getProjectById(projId, user);
            projectOrigin.put(projId, SW360Utils.printName(linkedProjectById));
            Map<String, String> row = createProjectCSRow(relation, linkedProjectById, clearingStatusList);
            Map<String, ProjectProjectRelationship> subprojects = linkedProjectById.getLinkedProjects();
            Map<String, ProjectReleaseRelationship> linkedReleases = linkedProjectById.getReleaseIdToUsage();

            if (linkedReleases != null && !linkedReleases.isEmpty()) {
                flattenClearingStatusForReleases(linkedReleases, projectOrigin, releaseOrigin, clearingStatusList,
                        user, isInaccessibleLinkMasked);
            }

            if (subprojects != null && !subprojects.isEmpty()) {
                flattenClearingStatusForLinkedProject(subprojects, projectOrigin, releaseOrigin, clearingStatusList,
                        user, isInaccessibleLinkMasked);
            }

            projectOrigin.remove(projId);
            row.put("projectOrigin", String.join(" -> ", projectOrigin.values()));
        }));
    }

    private void flattenClearingStatusForReleases(Map<String, ProjectReleaseRelationship> linkedReleases,
            LinkedHashMap<String, String> projectOrigin, LinkedHashMap<String, String> releaseOrigin,
            List<Map<String, String>> clearingStatusList, User user, boolean isInaccessibleLinkMasked) {

        linkedReleases.entrySet().stream().forEach(rl -> wrapTException(() -> {
            String relation = ThriftEnumUtils.enumToString(rl.getValue().getReleaseRelation());
            String projectMailLineState = ThriftEnumUtils.enumToString(rl.getValue().getMainlineState());
            String comment = rl.getValue().getComment();
            String releaseId = rl.getKey();
            if (releaseOrigin.containsKey(releaseId))
                return;
            Release rel = componentDatabaseHandler.getRelease(releaseId, user);

            if (!isInaccessibleLinkMasked || componentDatabaseHandler.isReleaseActionAllowed(rel, user, RequestedAction.READ)) {
                Map<String, ReleaseRelationship> releaseIdToRelationship = rel.getReleaseIdToRelationship();
                releaseOrigin.put(releaseId, SW360Utils.printName(rel));
                Map<String, String> row = createReleaseCSRow(relation, projectMailLineState, rel, clearingStatusList, user, comment);
                if (releaseIdToRelationship != null && !releaseIdToRelationship.isEmpty()) {
                    flattenlinkedReleaseOfRelease(releaseIdToRelationship, projectOrigin, releaseOrigin, clearingStatusList,
                                user, isInaccessibleLinkMasked);
                }
                releaseOrigin.remove(releaseId);
                row.put("projectOrigin", String.join(" -> ", projectOrigin.values()));
                row.put("releaseOrigin", String.join(" -> ", releaseOrigin.values()));
            } else {
                Map<String, String> row = createInaccessibleReleaseCSRow(clearingStatusList);
                row.put("projectOrigin", "");
                row.put("releaseOrigin", "");
            }
        }));
    }

    private void flattenlinkedReleaseOfRelease(Map<String, ReleaseRelationship> releaseIdToRelationship,
            LinkedHashMap<String, String> projectOrigin, LinkedHashMap<String, String> releaseOrigin,
            List<Map<String, String>> clearingStatusList, User user, boolean isInaccessibleLinkMasked) {
        releaseIdToRelationship.entrySet().stream().forEach(rl -> wrapTException(() -> {
            String relation = ThriftEnumUtils.enumToString(rl.getValue());
            String projectMailLineState = "";
            String releaseId = rl.getKey();
            if (releaseOrigin.containsKey(releaseId))
                return;
            Release rel = componentDatabaseHandler.getRelease(releaseId, user);

            if (!isInaccessibleLinkMasked || componentDatabaseHandler.isReleaseActionAllowed(rel, user, RequestedAction.READ)) {
                Map<String, ReleaseRelationship> subReleaseIdToRelationship = rel.getReleaseIdToRelationship();
                releaseOrigin.put(releaseId, SW360Utils.printName(rel));
                Map<String, String> row = createReleaseCSRow(relation, projectMailLineState, rel, clearingStatusList, user, "");
                if (subReleaseIdToRelationship != null && !subReleaseIdToRelationship.isEmpty()) {
                    flattenlinkedReleaseOfRelease(subReleaseIdToRelationship, projectOrigin, releaseOrigin,
                                clearingStatusList, user, isInaccessibleLinkMasked);
                }
                releaseOrigin.remove(releaseId);
                row.put("projectOrigin", String.join(" -> ", projectOrigin.values()));
                row.put("releaseOrigin", String.join(" -> ", releaseOrigin.values()));
            } else {
                Map<String, String> row = createInaccessibleReleaseCSRow(clearingStatusList);
                row.put("projectOrigin", "");
                row.put("releaseOrigin", "");
            }
        }));
    }

    public void sendExportSpreadsheetSuccessMail(String url, String recepient) throws TException {
        mailUtil.sendMail(recepient, MailConstants.SUBJECT_SPREADSHEET_EXPORT_SUCCESS,
                MailConstants.TEXT_SPREADSHEET_EXPORT_SUCCESS, SW360Constants.NOTIFICATION_CLASS_PROJECT, "", false,
                "project", url);
    }

    private Map<String, String> createProjectCSRow(String relation, Project prj,
            List<Map<String, String>> clearingStatusList) {
        String projectId = prj.getId();
        Map<String, String> row = new HashMap<>();
        row.put("id", projectId);
        row.put("name", SW360Utils.printName(prj));
        row.put("type", ThriftEnumUtils.enumToString(prj.getProjectType()));
        row.put("relation", relation);
        row.put("isRelease", "false");
        row.put("clearingState", ThriftEnumUtils.enumToString(prj.getClearingState()));
        row.put("projectState", ThriftEnumUtils.enumToString(prj.getState()));
        row.put("isAccessible", "true");
        clearingStatusList.add(row);
        return row;
    }

    private Map<String, String> createReleaseCSRow(String relation, String projectMailLineState, Release rl,
            List<Map<String, String>> clearingStatusList, User user, String comment) throws SW360Exception {
        Map<String, String> row = new HashMap<>();
        Component component = componentDatabaseHandler.getComponent(rl.getComponentId(), user);
        String releaseId = rl.getId();
        row.put("id", releaseId);
        row.put("name", SW360Utils.printName(rl));
        row.put("type", ThriftEnumUtils.enumToString(component.getComponentType()));
        Set<String> collectedLicIds = CommonUtils.nullToEmptySet(rl.getMainLicenseIds());
        row.put("relation", relation);
        row.put("mainLicenses", String.join(",", collectedLicIds));
        row.put("isRelease", "true");
        row.put("releaseMainlineState", ThriftEnumUtils.enumToString(rl.getMainlineState()));
        row.put("clearingState", ThriftEnumUtils.enumToString(rl.getClearingState()));
        row.put("projectMainlineState", projectMailLineState);
        row.put("comment", CommonUtils.nullToEmptyString(comment));
        row.put("isAccessible", "true");
        clearingStatusList.add(row);
        return row;
    }

    private Map<String, String> createInaccessibleReleaseCSRow(List<Map<String, String>> clearingStatusList) throws SW360Exception {
        Map<String, String> row = new HashMap<>();
        row.put("id", "");
        row.put("name", "");
        row.put("type", "");
        row.put("relation", "");
        row.put("mainLicenses", "");
        row.put("isRelease", "true");
        row.put("releaseMainlineState", "");
        row.put("clearingState", "");
        row.put("projectMainlineState", "");
        row.put("comment", "");
        row.put("isAccessible", "false");
        clearingStatusList.add(row);
        return row;
    }

    public Set<String> getGroups() {
        return repository.getGroups();
    }

    public int getMyAccessibleProjects(User user) {
        return repository.getMyAccessibleProjectsCount(user);
    }

    public ProjectData searchByGroup(String group, User user) {
        return repository.searchByGroup(group, user);
    }

    public ProjectData searchByTag(String tag, User user) {
        return repository.searchByTag(tag, user);
    }

    public ProjectData searchByType(String type, User user) {
        return repository.searchByType(type, user);
    }

	public ByteBuffer getReportDataStream(User user, boolean extendedByReleases, String projectId) throws TException {
	    List<Project> projectList = null;
        try {
            if (!isNullOrEmpty(projectId)) {
                projectList = getProjectDetailsBasedOnId(user, projectId);
            }else {
                projectList =  getAccessibleProjectsSummary(user);
            }
            ProjectExporter exporter = getProjectExporterObject(projectList, user, extendedByReleases);
            InputStream stream = exporter.makeExcelExport(projectList);
            return ByteBuffer.wrap(IOUtils.toByteArray(stream));
          }catch (IOException e) {
            throw new SW360Exception(e.getMessage());
       }
     }

    private ProjectExporter getProjectExporterObject(List<Project> documents, User user, boolean extendedByReleases) throws SW360Exception {
    	ThriftClients thriftClients = new ThriftClients();
    	return new ProjectExporter(thriftClients.makeComponentClient(),
                thriftClients.makeProjectClient(), user, documents, extendedByReleases);
    }

    public String getReportInEmail(User user,
			boolean extendedByReleases, String projectId) throws TException {
        List<Project> projectList = null;
        try {
            if (!isNullOrEmpty(projectId)) {
                projectList = getProjectDetailsBasedOnId(user, projectId);
            }else {
                projectList = getAccessibleProjectsSummary(user);
            }
            ProjectExporter exporter = getProjectExporterObject(projectList, user, extendedByReleases);
            return exporter.makeExcelExportForProject(projectList, user);
          }catch (IOException | TException e) {
             throw new SW360Exception(e.getMessage());
       }
     }

     private List<Project> getProjectDetailsBasedOnId(User user, String projectId) throws TException {
         final Collection<ProjectLink> projectLinks = SW360Utils.getLinkedProjectsAsFlatList(projectId, true,
                 new ThriftClients(), log, user);
         if (projectLinks.isEmpty()) {
             throw new TException("For the projectId : " + projectId
                     + ", No data available. Please check the projectId and try again.");
         }
         List<String> linkedProjectIds = projectLinks.stream().map(ProjectLink::getId).collect(Collectors.toList());
         return getProjectsById(linkedProjectIds, user);
     }

     public ByteBuffer downloadExcel(User user, boolean extendedByReleases, String token) throws SW360Exception {
        ThriftClients thriftClients = new ThriftClients();
        ProjectExporter exporter = new ProjectExporter(thriftClients.makeComponentClient(),
				thriftClients.makeProjectClient(), user, extendedByReleases);
		try {
			InputStream stream = exporter.downloadExcelSheet(token);
			return ByteBuffer.wrap(IOUtils.toByteArray(stream));
		} catch (IOException e) {
			throw new SW360Exception(e.getMessage());
		}
	}

    public List<ReleaseLink> getReleaseLinksOfProjectNetWorkByTrace(List<String> trace, String projectId, User user) throws TException{
        Project project = repository.get(projectId);
        String releaseNetwork = project.getReleaseRelationNetwork();
        List<ReleaseNode> listReleaseLinkJson;
        List<ReleaseLink> linkedReleases = new ArrayList<>();
        try {
            listReleaseLinkJson = mapper.readValue(releaseNetwork, new TypeReference<List<ReleaseNode>>() {
            });
            ReleaseNode previousNode = listReleaseLinkJson.get(Integer.parseInt(trace.get(0)));
            for (int i = 1; i < trace.size(); i++){
                previousNode = previousNode.getReleaseLink().get(Integer.parseInt(trace.get(i)));
            }
            linkedReleases = convertReleaseNodesToReleaseLinksSequentially(previousNode.getReleaseLink(), projectId, user, previousNode.getReleaseId(), trace.size());
        } catch (JsonProcessingException e) {
            log.error("JsonProcessingException: " + e);
        }
        return linkedReleases;
    }

    protected List<ReleaseLink> convertReleaseNodesToReleaseLinksSequentially(List<ReleaseNode> releaseNodes, String projectId, User user, String parentId, int layer) throws TException {
        List<ReleaseLink> releaseLinks = new ArrayList<>();
        int index = 0;
        for (ReleaseNode releaseNode : releaseNodes) {
            ReleaseLink releaseLink = convertReleaseNodeToReleaseLink(releaseNode, projectId, user, parentId, layer);
            releaseLink.setIndex(index);
            index++;
            releaseLinks.add(releaseLink);
        }
        return releaseLinks;
    }

    protected List<ReleaseLink> convertReleaseNodesToReleaseLinksParallel(List<ReleaseNode> releaseNodes, String projectId, User user) {
        ExecutorService executor = Executors.newFixedThreadPool(10);

        final List<Callable<ReleaseLink>> callableTasksToConvertReleaseNodes = new ArrayList<>();
        releaseNodes.forEach(releaseNode -> {
            Callable<ReleaseLink> convertToReleaseLink = () ->
                    convertReleaseNodeToReleaseLink(releaseNode, projectId, user, "", 0);
            callableTasksToConvertReleaseNodes.add(convertToReleaseLink);
        });

        List<Future<ReleaseLink>> releaseLinksFuture;
        try {
            releaseLinksFuture = executor.invokeAll(callableTasksToConvertReleaseNodes);
        } catch (InterruptedException e) {
            throw new RuntimeException("Error when convert releaseLink: " + e.getMessage());
        } finally {
            executor.shutdown();
        }

        AtomicInteger index = new AtomicInteger();
        return releaseLinksFuture.stream().map(fut -> {
            try {
                ReleaseLink releaseLink = fut.get();
                releaseLink.setIndex(index.getAndIncrement());
                return releaseLink;
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Error when convert releaseLink: " + e.getMessage());
            }
        }).collect(Collectors.toList());
    }

    public List<Map<String, String>> getClearingStateForDependencyNetworkListView(String projectId, User user, boolean isInaccessibleLinkMasked)
            throws SW360Exception {
        Project projectById = getProjectById(projectId, user);
        List<Map<String, String>> clearingStatusList = new ArrayList<>();
        LinkedHashMap<String, String> projectOrigin = new LinkedHashMap<>();
        projectOrigin.put(projectId, SW360Utils.printName(projectById));
        LinkedHashMap<String, String> releaseOrigin = new LinkedHashMap<>();
        Map<String, ProjectProjectRelationship> linkedProjects = projectById.getLinkedProjects();
        projectExecutor = Executors.newFixedThreadPool(5);
        String releaseNetwork = projectById.getReleaseRelationNetwork();
        List<ReleaseNode> listReleaseLinkJson;
        if (releaseNetwork != null) {
            try {
                listReleaseLinkJson = mapper.readValue(releaseNetwork, new TypeReference<List<ReleaseNode>>() {
                });
                flattenLinkedReleaseOfRelease(listReleaseLinkJson, projectOrigin, releaseOrigin, clearingStatusList, user, isInaccessibleLinkMasked);
            } catch (JsonProcessingException e) {
                log.error("JsonProcessingException: " + e);
            }
        }

        if (linkedProjects != null && !linkedProjects.isEmpty()) {
            try {
                flattenDependencyNetworkForLinkedProject(linkedProjects, projectOrigin, releaseOrigin, clearingStatusList,
                        user, isInaccessibleLinkMasked);
            } catch (WrappedException.WrappedSW360Exception exception) {
                throw new SW360Exception(exception.getCause());
            }
        }
        projectExecutor.shutdown();
        return clearingStatusList;
    }

    private void flattenLinkedReleaseOfRelease(List<ReleaseNode>  listReleaseLinkJson,
                                               LinkedHashMap<String, String> projectOrigin, LinkedHashMap<String, String> releaseOrigin,
                                               List<Map<String, String>> clearingStatusList, User user, boolean isInaccessibleLinkMasked) {
        final List<Callable<Void>> callables = new ArrayList<>();
        listReleaseLinkJson.forEach(rl -> {
            Callable<Void> callableTask = () -> {
                String relation = ThriftEnumUtils.enumToString(ReleaseRelationship.valueOf(rl.getReleaseRelationship()));
                String projectMailLineState = ThriftEnumUtils.enumToString(MainlineState.valueOf(rl.getMainlineState()));
                String comment = rl.getComment();
                String releaseId = rl.getReleaseId();
                LinkedHashMap<String, String> cpReleaseOrigin = new LinkedHashMap<>(releaseOrigin);
                if (cpReleaseOrigin.containsKey(releaseId))
                    return null;
                Release rel = componentDatabaseHandler.getRelease(releaseId, user);
                List<ReleaseNode>  listLinkedRelease = rl.getReleaseLink();
                if (!isInaccessibleLinkMasked || componentDatabaseHandler.isReleaseActionAllowed(rel, user, RequestedAction.READ)) {
                    cpReleaseOrigin.put(releaseId, SW360Utils.printName(rel));
                    Map<String, String> row = createReleaseCSRow(relation, projectMailLineState, rel, clearingStatusList, user, comment);
                    if (CommonUtils.isNotEmpty(listLinkedRelease)) {
                        flattenLinkedReleaseOfRelease(listLinkedRelease, projectOrigin, cpReleaseOrigin,
                                clearingStatusList, user, isInaccessibleLinkMasked);
                    }
                    cpReleaseOrigin.remove(releaseId);
                    row.put("projectOrigin", String.join(" -> ", projectOrigin.values()));
                    row.put("releaseOrigin", String.join(" -> ", releaseOrigin.values()));
                } else {
                    Map<String, String> row = createInaccessibleReleaseCSRow(clearingStatusList);
                    row.put("projectOrigin", "");
                    row.put("releaseOrigin", "");
                }
                return null;
            };
            callables.add(callableTask);
        });
        try {
            projectExecutor.invokeAll(callables);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
    }

    private void flattenDependencyNetworkForLinkedProject(Map<String, ProjectProjectRelationship> linkedProjects,
                                                          LinkedHashMap<String, String> projectOrigin, LinkedHashMap<String, String> releaseOrigin,
                                                          List<Map<String, String>> clearingStatusList, User user, boolean isInaccessibleLinkMasked) throws WrappedException.WrappedSW360Exception {

        linkedProjects.entrySet().stream().forEach(lp -> wrapSW360Exception(() -> {
            String projId = lp.getKey();
            String relation = ThriftEnumUtils.enumToString(lp.getValue().getProjectRelationship());
            if (projectOrigin.containsKey(projId))
                return;
            Project linkedProjectById = getProjectById(projId, user);
            projectOrigin.put(projId, SW360Utils.printName(linkedProjectById));
            Map<String, String> row = createProjectCSRow(relation, linkedProjectById, clearingStatusList);
            Map<String, ProjectProjectRelationship> subprojects = linkedProjectById.getLinkedProjects();

            String releaseNetwork = linkedProjectById.getReleaseRelationNetwork();
            List<ReleaseNode> listReleaseLinkJson;
            if (releaseNetwork != null) {
                try {
                    listReleaseLinkJson = mapper.readValue(releaseNetwork, new TypeReference<List<ReleaseNode>>() {
                    });
                    flattenLinkedReleaseOfRelease(listReleaseLinkJson, projectOrigin, releaseOrigin, clearingStatusList, user, isInaccessibleLinkMasked);
                } catch (JsonProcessingException e) {
                    log.error("JsonProcessingException: " + e);
                }
            }

            if (subprojects != null && !subprojects.isEmpty()) {
                try {
                    flattenDependencyNetworkForLinkedProject(subprojects, projectOrigin, releaseOrigin, clearingStatusList,
                            user, isInaccessibleLinkMasked);
                } catch (WrappedException.WrappedSW360Exception exception) {
                    throw new SW360Exception(exception.getCause());
                }
            }

            projectOrigin.remove(projId);
            row.put("projectOrigin", String.join(" -> ", projectOrigin.values()));
        }));
    }

    private Optional<ProjectLink> createProjectLinkForDependencyNetwork(String id, ProjectProjectRelationship projectProjectRelationship, String parentNodeId,
                                                                        Deque<String> visitedIds, int maxDepth, User user, boolean withRelease, boolean withAllReleases) throws SW360Exception {
        ProjectLink projectLink = null;
        if (!visitedIds.contains(id) && (maxDepth < 0 || visitedIds.size() < maxDepth)) {
            visitedIds.push(id);
            Project project = getProjectById(id, user);
            if (project != null
                    && (user == null || !makePermission(project, user).isActionAllowed(RequestedAction.READ))) {
                log.error("User " + (user == null ? "" : user.getEmail())
                        + " requested not accessible project " + printName(project));
                project = null;
            }
            if (project != null) {
                projectLink = new ProjectLink(id, project.name);
                if (withRelease) {
                    if (project.getReleaseRelationNetwork() != null && project.getReleaseRelationNetwork().length() > 0 && (maxDepth < 0 || visitedIds.size() < maxDepth)) {
                        String releaseNetwork = project.getReleaseRelationNetwork();
                        List<ReleaseNode> releaseNodes;
                        List<ReleaseLink> linkedReleases = new ArrayList<>();
                        try {
                            releaseNodes = mapper.readValue(releaseNetwork, new TypeReference<>() {
                            });
                            if (withAllReleases == WITH_ROOT_RELEASES_ONLY) {
                                linkedReleases = convertReleaseNodesToReleaseLinksParallel(releaseNodes, id, user);
                            } else {
                                List<ReleaseNode> flattenedNetwork = new ArrayList<>();
                                Set<String> visitedNodeIds = new HashSet<>();
                                for (ReleaseNode node : releaseNodes) {
                                    flattenedNetwork.addAll(flattenNodeNetwork(node, visitedNodeIds));
                                }
                                linkedReleases = convertReleaseNodesToReleaseLinksParallel(flattenedNetwork, id, user);
                            }
                        } catch (JsonProcessingException e) {
                            log.error("JsonProcessingException: " + e);
                        }
                        projectLink.setLinkedReleases(nullToEmptyList(linkedReleases));
                    }
                } else {
                    projectLink.setLinkedReleases(Collections.emptyList());
                }
                projectLink
                        .setNodeId(generateNodeId(id))
                        .setParentNodeId(parentNodeId)
                        .setRelation(projectProjectRelationship.getProjectRelationship())
                        .setEnableSvm(projectProjectRelationship.isEnableSvm())
                        .setVersion(project.getVersion())
                        .setState(project.getState())
                        .setProjectType(project.getProjectType())
                        .setClearingState(project.getClearingState())
                        .setTreeLevel(visitedIds.size() - 1);
                if (project.isSetLinkedProjects()) {
                    List<ProjectLink> subprojectLinks =
                            (withAllReleases == WITH_ROOT_RELEASES_ONLY)
                                    ? iterateProjectRelationShips(project.getLinkedProjects(),
                                        projectLink.getNodeId(), visitedIds, maxDepth, user, withRelease)
                                    : iterateProjectRelationShipsWithAllReleases(project.getLinkedProjects(),
                                        projectLink.getNodeId(), visitedIds, maxDepth, user);
                    projectLink.setSubprojects(subprojectLinks);
                }
            } else {
                log.error("Broken ProjectLink in project with id: " + parentNodeId + ". Linked project with id " + id + " was not found");
            }
            visitedIds.pop();
        }
        return Optional.ofNullable(projectLink);
    }

    public List<ProjectLink> getLinkedProjectsWithoutReleases(Map<String, ProjectProjectRelationship> relations, boolean depth, User user) throws SW360Exception {
        List<ProjectLink> out;

        Deque<String> visitedIds = new ArrayDeque<>();
        out = iterateProjectRelationShips(relations, null, visitedIds, depth ? -1 : 1, user, false);

        return out;
    }

    private List<ProjectLink> iterateProjectRelationShips(Map<String, ProjectProjectRelationship> relations,
                                                          String parentNodeId, Deque<String> visitedIds, int maxDepth,
                                                          User user, boolean withRelease) throws SW360Exception {
        List<ProjectLink> out = new ArrayList<>();
        for (Map.Entry<String, ProjectProjectRelationship> entry : relations.entrySet()) {
            Optional<ProjectLink> projectLinkOptional;
            if (!SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
                projectLinkOptional = createProjectLink(entry.getKey(), entry.getValue(),
                        parentNodeId, visitedIds, maxDepth, user);
            } else {
                projectLinkOptional = createProjectLinkForDependencyNetwork(entry.getKey(), entry.getValue(),
                        parentNodeId, visitedIds, maxDepth, user, withRelease, WITH_ROOT_RELEASES_ONLY);
            }
            projectLinkOptional.ifPresent(out::add);
        }
        out.sort(Comparator.comparing(ProjectLink::getName).thenComparing(ProjectLink::getVersion));
        return out;
    }

    public List<ProjectLink> getLinkedProjectsWithoutReleases(Project project, boolean deep, User user) throws SW360Exception {
        Deque<String> visitedIds = new ArrayDeque<>();

        Map<String, ProjectProjectRelationship> fakeRelations = new HashMap<>();
        fakeRelations.put(project.isSetId() ? project.getId() : DUMMY_NEW_PROJECT_ID, new ProjectProjectRelationship(ProjectRelationship.UNKNOWN));
        List<ProjectLink> out = iterateProjectRelationShips(fakeRelations, null, visitedIds, deep ? -1 : 2, user, false);
        return out;
    }

    public List<ProjectLink> getLinkedProjectsWithAllReleases(Project project, boolean deep, User user) throws SW360Exception {
        Deque<String> visitedIds = new ArrayDeque<>();

        Map<String, ProjectProjectRelationship> fakeRelations = new HashMap<>();
        fakeRelations.put(project.isSetId() ? project.getId() : DUMMY_NEW_PROJECT_ID, new ProjectProjectRelationship(ProjectRelationship.UNKNOWN));
        return iterateProjectRelationShipsWithAllReleases(fakeRelations, null, visitedIds, deep ? -1 : 2, user);
    }

    private List<ProjectLink> iterateProjectRelationShipsWithAllReleases(Map<String, ProjectProjectRelationship> relations,
                                                                         String parentNodeId, Deque<String> visitedIds, int maxDepth, User user) throws SW360Exception {
        List<ProjectLink> out = new ArrayList<>();
        for (Map.Entry<String, ProjectProjectRelationship> entry : relations.entrySet()) {
            Optional<ProjectLink> projectLinkOptional = createProjectLinkForDependencyNetwork(entry.getKey(), entry.getValue(),
                    parentNodeId, visitedIds, maxDepth, user, true, WITH_ALL_RELEASES);
            projectLinkOptional.ifPresent(out::add);
        }
        out.sort(Comparator.comparing(ProjectLink::getName).thenComparing(ProjectLink::getVersion));
        return out;
    }

    private List<ReleaseNode> flattenNodeNetwork(ReleaseNode node, Set<String> visitedNodeIds) {
        if (node == null) {
            return Collections.emptyList();
        }

        List<ReleaseNode> releaseNodes = new ArrayList<>();

        if (!visitedNodeIds.contains(node.getReleaseId())) {
            visitedNodeIds.add(node.getReleaseId());
            releaseNodes.add(node);
        }

        if (node.getReleaseLink() == null) {
            return releaseNodes;
        }

        if (!node.getReleaseLink().isEmpty()) {
            List<ReleaseNode> children = node.getReleaseLink();
            for (ReleaseNode child : children) {
                if (child.getReleaseLink() != null) {
                    releaseNodes.addAll(flattenNodeNetwork(child, visitedNodeIds));
                }
            }
        }
        return releaseNodes;
    }

    protected ReleaseLink convertReleaseNodeToReleaseLink(ReleaseNode releaseNode, String projectId, User user, String parentId, int layer) throws TException {
        Release releaseById = componentDatabaseHandler.getRelease(releaseNode.getReleaseId(), user);

        ReleaseLink releaseLink = new ReleaseLink();
        boolean isAccessible = componentDatabaseHandler.isReleaseActionAllowed(releaseById, user, RequestedAction.READ);
        releaseLink.setId(releaseNode.getReleaseId());
        releaseLink.setAccessible(isAccessible);
        releaseLink.setName(isAccessible ? releaseById.getName() : "");
        releaseLink.setVersion(isAccessible ? releaseById.getVersion() : "");
        releaseLink.setLayer(layer);
        releaseLink.setLongName(isAccessible ? SW360Utils.printFullname(releaseById) : "Restricted release");
        releaseLink.setHasSubreleases(!releaseNode.getReleaseLink().isEmpty());
        releaseLink.setNodeId(releaseById.getId() + "_" + UUID.randomUUID());
        releaseLink.setParentNodeId(parentId);
        releaseLink.setProjectId(projectId);
        releaseLink.setVendor((isAccessible && releaseById.getVendor() != null) ? releaseById.getVendor().getFullname() : "");

        if (isAccessible) {
            releaseLink.setReleaseRelationship(ReleaseRelationship.valueOf(releaseNode.getReleaseRelationship()));
            releaseLink.setMainlineState(MainlineState.valueOf(releaseNode.getMainlineState()));
            releaseLink.setComment(releaseNode.getComment());
            releaseLink.setClearingState(releaseById.getClearingState());
            releaseLink.setLicenseIds(releaseById.getMainLicenseIds());
            releaseLink.setOtherLicenseIds(releaseById.getOtherLicenseIds());
            releaseLink.setReleaseMainLineState(releaseById.getMainlineState());
            releaseLink.setAttachments(releaseById.getAttachments() != null ? Lists.newArrayList(releaseById.getAttachments()) : Collections.emptyList());
            if (releaseById.getComponentType() != null) {
                releaseLink.setComponentType(releaseById.getComponentType());
            } else {
                Component componentById = componentDatabaseHandler.getComponent(releaseById.getComponentId(), user);
                releaseLink.setComponentType(componentById.getComponentType());
            }
        }

        return releaseLink;
    }

    public List<ReleaseLink> getReleaseLinksOfProjectNetWorkByIndexPath(List<String> indexPath, String projectId, User user) throws SW360Exception {
        Project project = getProjectById(projectId, user);

        if (project == null) {
            throw new SW360Exception("Project not found: " + projectId).setErrorCode(404);
        }

        String releaseNetwork = project.getReleaseRelationNetwork();
        List<ReleaseLink> linkedReleases = new ArrayList<>();
        try {
            List<ReleaseNode> releaseNodes = mapper.readValue(releaseNetwork, new TypeReference<List<ReleaseNode>>() {
            });
            ReleaseNode previousNode = releaseNodes.get(Integer.parseInt(indexPath.get(0)));
            for (int i = 1; i < indexPath.size(); i++){
                previousNode = previousNode.getReleaseLink().get(Integer.parseInt(indexPath.get(i)));
            }
            linkedReleases = convertReleaseNodesToReleaseLinksParallel(previousNode.getReleaseLink(), projectId, user);
        } catch (JsonProcessingException e) {
            log.error("JsonProcessingException: " + e);
        } catch (IndexOutOfBoundsException exception) {
            throw new SW360Exception("Index path is incorrect").setErrorCode(400);
        }

        return linkedReleases;
    }

    public List<ReleaseNode> getLinkedReleasesInDependencyNetworkOfProject(String projectId, User sw360User) throws SW360Exception {
        final Project projectById = getProjectById(projectId, sw360User);
        if (projectById.getReleaseRelationNetwork() == null) {
            return new ArrayList<>();
        }
        List<ReleaseNode> releaseNodes;
        try {
            releaseNodes = mapper.readValue(projectById.getReleaseRelationNetwork(), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Error while parsing JSON data");
            return new ArrayList<>();
        }

        return releaseNodes.stream()
                .map(releaseNode -> wrapSW360Exception(() -> createReleaseNodeWithDetail(releaseNode, sw360User)))
                .collect(Collectors.toList());
    }

    public ReleaseNode createReleaseNodeWithDetail(ReleaseNode releaseNode, User sw360User) throws SW360Exception {
        Release releaseById = componentDatabaseHandler.getRelease(releaseNode.getReleaseId(), sw360User);
        boolean isActionAllowed = componentDatabaseHandler.isReleaseActionAllowed(releaseById, sw360User, RequestedAction.READ);
        ReleaseNode detailReleaseNode = new ReleaseNode();
        detailReleaseNode.setReleaseId(releaseNode.getReleaseId());
        detailReleaseNode.setReleaseRelationship(releaseNode.getReleaseRelationship());
        detailReleaseNode.setMainlineState(releaseNode.getMainlineState());
        detailReleaseNode.setComment(releaseNode.getComment());
        detailReleaseNode.setReleaseVersion(isActionAllowed ? releaseById.getVersion() : "");
        detailReleaseNode.setReleaseName(isActionAllowed ? releaseById.getName() : "");
        detailReleaseNode.setComponentId(isActionAllowed ? releaseById.getComponentId() : "");
        detailReleaseNode.setReleaseLink(
                releaseNode.getReleaseLink().stream()
                        .map(node -> wrapSW360Exception(() -> createReleaseNodeWithDetail(node, sw360User)))
                        .collect(Collectors.toList())
        );
        return detailReleaseNode;
    }
}
