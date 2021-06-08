/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.moderation.db;

import com.cloudant.client.api.CloudantClient;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.DatabaseHandlerUtil;
import org.eclipse.sw360.datahandler.db.ProjectDatabaseHandler;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.ClearingRequestEmailTemplate;
import org.eclipse.sw360.datahandler.thrift.ClearingRequestState;
import org.eclipse.sw360.datahandler.thrift.Comment;
import org.eclipse.sw360.datahandler.thrift.ModerationState;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.changelogs.Operation;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.moderation.DocumentType;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.licenses.db.LicenseDatabaseHandler;
import org.eclipse.sw360.mail.MailConstants;
import org.eclipse.sw360.mail.MailUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.sw360.datahandler.common.CommonUtils.notEmptyOrNull;
import static org.eclipse.sw360.datahandler.common.SW360Assert.fail;
import static org.eclipse.sw360.datahandler.common.SW360Utils.getBUFromOrganisation;
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;

/**
 * Class for accessing the CouchDB database for the moderation objects
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class ModerationDatabaseHandler {

    private static final Logger log = LogManager.getLogger(ModerationDatabaseHandler.class);
    private static final String CR = "CR-";

    /**
     * Connection to the couchDB database
     */
    private final ModerationRequestRepository repository;
    private final ClearingRequestRepository clearingRequestRepository;
    private final LicenseDatabaseHandler licenseDatabaseHandler;
    private final ProjectDatabaseHandler projectDatabaseHandler;
    private final ComponentDatabaseHandler componentDatabaseHandler;
    private final DatabaseConnectorCloudant db;
    private DatabaseHandlerUtil dbHandlerUtil;

    private final MailUtil mailUtil = new MailUtil();

    public ModerationDatabaseHandler(Supplier<CloudantClient> httpClient, String dbName, String attachmentDbName) throws MalformedURLException {
        db = new DatabaseConnectorCloudant(httpClient, dbName);

        // Create the repository
        repository = new ModerationRequestRepository(db);
        clearingRequestRepository = new ClearingRequestRepository(db);

        licenseDatabaseHandler = new LicenseDatabaseHandler(httpClient, dbName);
        projectDatabaseHandler = new ProjectDatabaseHandler(httpClient, dbName, attachmentDbName);
        componentDatabaseHandler = new ComponentDatabaseHandler(httpClient, dbName, attachmentDbName);
        DatabaseConnectorCloudant dbChangeLogs = new DatabaseConnectorCloudant(httpClient, DatabaseSettings.COUCH_DB_CHANGE_LOGS);
        this.dbHandlerUtil = new DatabaseHandlerUtil(dbChangeLogs);
    }

    public ModerationDatabaseHandler(Supplier<CloudantClient> httpClient, String dbName, String changeLogsDbName, String attachmentDbName) throws MalformedURLException {
        this(httpClient, dbName, attachmentDbName);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(httpClient, changeLogsDbName);
        this.dbHandlerUtil = new DatabaseHandlerUtil(db);

    }

    public List<ModerationRequest> getRequestsByModerator(String moderator) {
        return repository.getRequestsByModerator(moderator);
    }

    public Map<PaginationData, List<ModerationRequest>> getRequestsByModerator(String moderator, PaginationData pageData, boolean open) {
        return repository.getRequestsByModerator(moderator, pageData, open);
    }

    public List<ModerationRequest> getRequestsByRequestingUser(String user) {
        return repository.getRequestsByRequestingUser(user);
    }

    public ClearingRequest getClearingRequestByProjectId(String projectId, User user) throws SW360Exception {
        projectDatabaseHandler.getProjectById(projectId, user); // check if user have READ access to project.
        return clearingRequestRepository.getClearingRequestByProjectId(projectId);
    }

    public Set<ClearingRequest> getMyClearingRequests(String user) {
        return new HashSet<ClearingRequest>(clearingRequestRepository.getMyClearingRequests(user));
    }

    public Set<ClearingRequest> getClearingRequestsByBU(String businessUnit) {
        return new HashSet<ClearingRequest>(clearingRequestRepository.getClearingRequestsByBU(businessUnit));
    }

    public ModerationRequest getRequest(String requestId) {
        ModerationRequest moderationRequest = repository.get(requestId);
        return moderationRequest;
    }

    public List<ModerationRequest> getRequestByDocumentId(String documentId) {
        List<ModerationRequest> requests = CommonUtils.nullToEmptyList(repository.getRequestsByDocumentId(documentId));

        Collections.sort(requests, CommonUtils.compareByTimeStampDescending());

        return requests;
    }

    public String createClearingRequest(ClearingRequest request, User user) {
        request.setTimestamp(System.currentTimeMillis());
        long id = 1;
        synchronized (ModerationDatabaseHandler.class) {
            Set<String> allIds = clearingRequestRepository.getAllIds();
            String maxId = allIds.stream().max(Comparator.comparingInt(SW360Utils::parseStringToNumber)).orElse("");
            if (CommonUtils.isNotNullEmptyOrWhitespace(maxId)) {
                maxId = maxId.split("-")[1];
                id = Long.valueOf(maxId) + 1;
            }
            request.setId(new StringBuilder(CR).append(id).toString());
            clearingRequestRepository.add(request);
        }
        return request.getId();
    }

    public ClearingRequest getClearingRequestById(String id, User user) throws SW360Exception {
        ClearingRequest clearingRequest = clearingRequestRepository.get(id);
        if (CommonUtils.isNullEmptyOrWhitespace(clearingRequest.getProjectId())) {
            return clearingRequest;
        }
        if (!(clearingRequest.getClearingTeam().equals(user.getEmail()) || clearingRequest.getRequestingUser().equals(user.getEmail()))) {
            projectDatabaseHandler.getProjectById(clearingRequest.getProjectId(), user); // check if user have READ access to project.
        }
        return clearingRequest;
    }

    public ClearingRequest getClearingRequestByIdForEdit(String id, User user) throws SW360Exception {
        ClearingRequest clearingRequest = clearingRequestRepository.get(id);
        if (CommonUtils.isNullEmptyOrWhitespace(clearingRequest.getProjectId())) {
            return clearingRequest;
        }
        Project project = projectDatabaseHandler.getProjectById(clearingRequest.getProjectId(), user);
        if (!(clearingRequest.getClearingTeam().equals(user.getEmail())
                || clearingRequest.getRequestingUser().equals(user.getEmail())
                || makePermission(project, user).isActionAllowed(RequestedAction.WRITE))) {
            throw fail("User " + SW360Utils.printFullname(user) + ", does not have WRITE access to clearing request: " + clearingRequest.getId());
        }
        return clearingRequest;
    }

    public RequestStatus updateClearingRequest(ClearingRequest request, User user, String projectUrl) {
        try {
            if (request.getTimestampOfDecision() < 1 && (ClearingRequestState.CLOSED.equals(request.getClearingState())
                    || ClearingRequestState.REJECTED.equals(request.getClearingState()))) {
                request.setTimestampOfDecision(System.currentTimeMillis());
            }
            ClearingRequest currentRequest = getClearingRequestByIdForEdit(request.getId(), user);
            StringBuilder commentText = new StringBuilder("Clearing Request is ");
            if (!currentRequest.getClearingState().equals(request.getClearingState())) {
                if (ClearingRequestState.CLOSED.equals(currentRequest.getClearingState())
                        || ClearingRequestState.REJECTED.equals(currentRequest.getClearingState())) {
                    commentText.append("re-opened.");
                } else {
                    commentText.append("updated.");
                }
                commentText = commentText.append("\n\tStatus changed from: <b>")
                        .append(ThriftEnumUtils.enumToString(currentRequest.getClearingState())).append("</b> to <b>")
                        .append(ThriftEnumUtils.enumToString(request.getClearingState())).append("</b>");
            }
            String oldAgreedClDate = CommonUtils.nullToEmptyString(currentRequest.getAgreedClearingDate());
            String newAgreedClDate = CommonUtils.nullToEmptyString(request.getAgreedClearingDate());
            if (!oldAgreedClDate.equals(newAgreedClDate)) {
                commentText = commentText.append("updated.\n\tAgreed Clearing Date changed from: <b>")
                        .append(StringUtils.defaultIfBlank(oldAgreedClDate, "NULL")).append("</b> to <b>")
                        .append(StringUtils.defaultIfBlank(newAgreedClDate, "NULL")).append("</b>");
            }
            Comment comment = new Comment().setText(commentText.toString());
            comment.setCommentedBy(user.getEmail());
            comment.setAutoGenerated(true);
            comment.setCommentedOn(System.currentTimeMillis());
            request.addToComments(comment);
            request.setModifiedOn(System.currentTimeMillis());
            clearingRequestRepository.update(request);
            projectDatabaseHandler.sendEmailForClearingRequestUpdate(request, projectUrl, user);
            return RequestStatus.SUCCESS;
        } catch (SW360Exception e) {
            log.error("Failed to update clearing request: " + request.getId(), e);
        }
        return RequestStatus.FAILURE;
    }

    public void updateClearingRequestForProjectDeletion(Project project, User user) {
        ClearingRequest clearingRequest = clearingRequestRepository.get(project.getClearingRequestId());
        Comment comment = new Comment().setText(new StringBuilder("Clearing Request is orphaned, as project (name): <b>")
                .append(SW360Utils.printName(project))
                .append("</b> associated with CR is deleted!").toString());
        comment.setCommentedBy(user.getEmail());
        comment.setAutoGenerated(true);
        comment.setCommentedOn(System.currentTimeMillis());
        clearingRequest.unsetProjectId();
        clearingRequest.addToComments(comment);
        clearingRequest.setModifiedOn(System.currentTimeMillis());
        clearingRequestRepository.update(clearingRequest);
    }

    public RequestStatus addCommentToClearingRequest(String id, Comment comment, User user) {
        try {
            comment.setCommentedOn(System.currentTimeMillis());
            ClearingRequest clearingRequest;
            if (comment.isAutoGenerated()) {
                clearingRequest = clearingRequestRepository.get(id);
                clearingRequest.addToComments(comment);
            } else {
                clearingRequest = getClearingRequestByIdForEdit(id, user);
                clearingRequest.setModifiedOn(System.currentTimeMillis());
                clearingRequest.addToComments(comment);
                sendMailForNewCommentInCR(clearingRequest, comment, user);
            }
            clearingRequestRepository.update(clearingRequest);
            return RequestStatus.SUCCESS;
        } catch (SW360Exception e) {
            log.error("Failed to add comment in clearing request: " + id, e);
        }
        return RequestStatus.FAILURE;
    }

    public void updateModerationRequest(ModerationRequest request) {
        repository.update(request);
    }

    public void deleteRequestsOnDocument(String documentId) {
        List<ModerationRequest> requests = repository.getRequestsByDocumentId(documentId);

        if (requests != null) {
            if (requests.size() > 1) {
                log.warn("More than one moderation request found for document " + documentId);
            }

            for (ModerationRequest request : requests) {
                repository.remove(request);
            }
        }
    }

    public RequestStatus deleteModerationRequest(String id, User user){
        ModerationRequest moderationRequest = repository.get(id);
        if(moderationRequest!=null) {
            if (hasPermissionToDeleteModerationRequest(user, moderationRequest)) {
                boolean succeeded = repository.remove(id);
                return succeeded ? RequestStatus.SUCCESS : RequestStatus.FAILURE;
            } else {
                log.error("Problems deleting moderation request: User " + user.getEmail() + " tried to delete " +
                        "moderation request of user " + moderationRequest.getRequestingUser());
                return RequestStatus.FAILURE;
            }
        }
        log.error("Moderation request to delete was null.");
        return RequestStatus.FAILURE;
    }

    private boolean hasPermissionToDeleteModerationRequest(User user, ModerationRequest moderationRequest) {
        boolean isCreator = moderationRequest.getRequestingUser().equals(user.getEmail());
        return isCreator || PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user);
    }

    public void refuseRequest(String requestId, String moderationDecisionComment, String reviewer) {
        ModerationRequest request = repository.get(requestId);
        request.setModerationState(ModerationState.REJECTED);
        request.setTimestampOfDecision(System.currentTimeMillis());
        request.setCommentDecisionModerator(moderationDecisionComment);
        request.setReviewer(reviewer);
        repository.update(request);
        sendMailToUserForDeclinedRequest(request);
    }

    public void acceptRequest(ModerationRequest request, String moderationComment, String reviewer) {
        ModerationRequest dbRequest = repository.get(request.getId());
        ModerationRequest requestBefore = dbRequest.deepCopy();
        // when an MR requests deletion of a document and is accepted, all outstanding MRs for that document are deleted,
        // which means that at this point we can't be sure that the MR still exists.
        // Therefore, we update it only if it still exists in the DB, but send mail notifications using the data from
        // now deleted MR anyways
        if (dbRequest != null){
            dbRequest.setModerationState(ModerationState.APPROVED);
            dbRequest.setTimestampOfDecision(System.currentTimeMillis());
            dbRequest.setReviewer(reviewer);
            dbRequest.setCommentDecisionModerator(moderationComment);
            repository.update(dbRequest);
        }
        dbHandlerUtil.addChangeLogs(dbRequest, requestBefore, reviewer, Operation.MODERATION_ACCEPT,
                null, Lists.newArrayList(), dbRequest.getDocumentId(),null);
        sendMailNotificationsForAcceptedRequest(request);
    }

    public RequestStatus createRequest(Component component, User user, Boolean isDeleteRequest) {
        Component dbcomponent;
        try {
            dbcomponent = componentDatabaseHandler.getComponent(component.getId(), user);
        } catch (SW360Exception e) {
            log.error("Could not get original component from database. Could not generate moderation request.", e);
            return RequestStatus.FAILURE;
        }
        // Define moderators
        Set<String> moderators = new HashSet<>();
        CommonUtils.add(moderators, dbcomponent.getCreatedBy());
        CommonUtils.addAll(moderators, dbcomponent.getModerators());
        CommonUtils.addAll(moderators, getUsersAtLeast(UserGroup.CLEARING_ADMIN, null, false, true));
        ModerationRequest request = createStubRequest(user, isDeleteRequest, component.getId(), moderators);

        // Set meta-data
        request.setDocumentType(DocumentType.COMPONENT);
        request.setDocumentName(SW360Utils.printName(dbcomponent));

        //Fill the request
        ModerationRequestGenerator generator = new ComponentModerationRequestGenerator();
        request = generator.setAdditionsAndDeletions(request, component, dbcomponent);
        if(component.isSetComponentType()) {
            request.setComponentType(component.getComponentType());
        }
        addOrUpdate(request, user);
        return RequestStatus.SENT_TO_MODERATOR;
    }

    public RequestStatus createRequest(Release release, User user, Boolean isDeleteRequest) {
        return createRequest(release, user, isDeleteRequest, this::getStandardReleaseModerators);
    }

    public RequestStatus createRequest(Release release, User user, Boolean isDeleteRequest, Function<Release, Set<String>> moderatorsProvider) {
        Release dbrelease;
        try {
            dbrelease = componentDatabaseHandler.getRelease(release.getId(), user);
        } catch (SW360Exception e) {
            log.error("Could not get original release from database. Could not generate moderation request.", e);
            return RequestStatus.FAILURE;
        }
        Set<String> moderators = moderatorsProvider.apply(dbrelease);

        ModerationRequest request = createStubRequest(user, isDeleteRequest, release.getId(), moderators);

        // Set meta-data
        request.setDocumentType(DocumentType.RELEASE);
        request.setDocumentName(SW360Utils.printName(dbrelease));

        // Fill the rest
        SW360Utils.setVendorId(release);
        SW360Utils.setVendorId(dbrelease);
        ModerationRequestGenerator generator = new ReleaseModerationRequestGenerator();
        request = generator.setAdditionsAndDeletions(request, release, dbrelease);
        try {
            Component parentComponent = componentDatabaseHandler.getComponent(release.getComponentId(), user);
            request.setComponentType(parentComponent.getComponentType());
        } catch (SW360Exception e) {
            log.error("Could not retrieve parent component type of release with ID=" + release.getId());
        }
        addOrUpdate(request, user);
        return RequestStatus.SENT_TO_MODERATOR;
    }

    @NotNull
    private Set<String> getStandardReleaseModerators(Release release) {
        // Define moderators
        Set<String> moderators = new HashSet<>();
        CommonUtils.add(moderators, release.getCreatedBy());
        CommonUtils.addAll(moderators, release.getModerators());
        CommonUtils.addAll(moderators, getUsersAtLeast(UserGroup.CLEARING_ADMIN, null, false, true));

        return moderators;
    }

    @NotNull
    private Set<String> getEccReleaseModerators(Release release) {
        // Define moderators
        Set<String> moderators = new HashSet<>();
        try{
            String department =  getDepartmentByUserEmail(release.getCreatedBy());
            CommonUtils.addAll(moderators, getUsersAtLeast(UserGroup.ECC_ADMIN, department, false, true));
        } catch (TException e){
            log.error("Could not get users from database. ECC admins not added as moderators, since department is missing.");
        }
        CommonUtils.addAll(moderators, getUsersAtLeast(UserGroup.ADMIN));
        return moderators;
    }

    private void appendCommentRequestingUserToRequest(ModerationRequest request, String comment) {
        /* For the case that the user is enlarging an existing moderation request, do not overwrite the old comment,
           but rather append the new comment to the old one. */
        if(request.isSetCommentRequestingUser()) {
            String totalComment = request.getCommentRequestingUser() + System.lineSeparator() + comment;
            request.setCommentRequestingUser(totalComment);
        } else {
            request.setCommentRequestingUser(comment);
        }
    }

    private void fillRequestWithCommentOfUser(ModerationRequest request, User user) {
        if(user.isSetCommentMadeDuringModerationRequest()) {
            appendCommentRequestingUserToRequest(request, user.getCommentMadeDuringModerationRequest());
        } else {
            appendCommentRequestingUserToRequest(request, "");
        }
    }

    public Function<Release, Set<String>> getEccModeratorsProvider() {
        return this::getEccReleaseModerators;
    }

    public RequestStatus createRequest(Project project, User user, Boolean isDeleteRequest) {
        Project dbproject;
        try {
            dbproject = projectDatabaseHandler.getProjectById(project.getId(), user);
        } catch (SW360Exception e) {
            log.error("Could not get original project from database. Could not generate moderation request.", e);
            return RequestStatus.FAILURE;
        }

        // Define moderators
        Set<String> moderators = getProjectModerators(dbproject, user.getDepartment());
        ModerationRequest request = createStubRequest(user, isDeleteRequest, project.getId(), moderators);

        // Set meta-data
        request.setDocumentType(DocumentType.PROJECT);
        request.setDocumentName(SW360Utils.printName(dbproject));

        // Fill the request
        ModerationRequestGenerator generator = new ProjectModerationRequestGenerator();
        request = generator.setAdditionsAndDeletions(request, project, dbproject);
        addOrUpdate(request, user);
        return RequestStatus.SENT_TO_MODERATOR;
    }

    @NotNull
    private Set<String> getProjectModerators(Project project, String department) {
        Set<String> moderators = new HashSet<>();
        if (project.getClearingState() != ProjectClearingState.CLOSED){
            CommonUtils.add(moderators, project.getCreatedBy());
            CommonUtils.add(moderators, project.getProjectResponsible());
            CommonUtils.addAll(moderators, project.getModerators());
        }
        CommonUtils.addAll(moderators, getUsersAtLeast(UserGroup.CLEARING_ADMIN, project.getBusinessUnit(), true, false));
        return moderators;
    }

    public RequestStatus createRequest(License license, User user) {
        License dblicense;
        try{
            dblicense = licenseDatabaseHandler.getLicenseForOrganisation(license.getId(), user.getDepartment());
        } catch (SW360Exception e) {
            log.error("Could not get original license from database. Could not generate moderation request.", e);
            return RequestStatus.FAILURE;
        }
        // Define moderators
        Set<String> moderators = getLicenseModerators(user.getDepartment());
        ModerationRequest request = createStubRequest(user, false, license.getId(), moderators);

        // Set meta-data
        request.setDocumentType(DocumentType.LICENSE);
        request.setDocumentName(SW360Utils.printName(license));

        // Fill the request
        ModerationRequestGenerator generator = new LicenseModerationRequestGenerator();
        request = generator.setAdditionsAndDeletions(request, license, dblicense);
        addOrUpdate(request, user);
        return RequestStatus.SENT_TO_MODERATOR;
    }

 public void createRequest(User user) {
        // Define moderators
        Set<String> admins = getUsersAtLeast(UserGroup.CLEARING_ADMIN, user.getDepartment());
        ModerationRequest request = createStubRequest(user, false, user.getId(), admins);

        // Set meta-data
        request.setDocumentType(DocumentType.USER);
        request.setDocumentName(SW360Utils.printName(user));

        // Set the object
        request.setUser(user);

        addOrUpdate(request, user);
    }

    private String getDepartmentByUserEmail(String userEmail) throws TException {
        UserService.Iface client = (new ThriftClients()).makeUserClient();
        return client.getDepartmentByEmail(userEmail);
    }

    private Set<String> getLicenseModerators(String department) {
        List<User> sw360users = getAllSW360Users();
        //try first clearing admins or admins from same department
        Set<String> moderators = sw360users.stream()
                .filter(getRelevantUserPredicate(UserGroup.CLEARING_ADMIN, department, true, true)).map(User::getEmail)
                .collect(Collectors.toSet());

        //second choice are all clearing admins or admins in SW360
        if (moderators.size() == 0) {
            moderators = sw360users
                    .stream()
                    .filter(user1 -> PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user1))
                    .map(User::getEmail)
                    .collect(Collectors.toSet());
        }
        return moderators;
    }

    private Set<String> getUsersAtLeast(UserGroup userGroup, String department) {
        return getUsersAtLeast(userGroup, department, false, false);
    }

    private Stream<User> getFilteredStreamOfProjectModeratorBasedOnSecondaryRole(UserGroup userGroup,
            List<User> sw360users, String department) {
        return sw360users.stream().filter(user -> {
            String buFromOrganisation = getBUFromOrganisation(user.getDepartment());
            if (!department.equals(buFromOrganisation)
                    && !CommonUtils.isNullOrEmptyMap(user.getSecondaryDepartmentsAndRoles())) {
                for (Entry<String, Set<UserGroup>> entry : user.getSecondaryDepartmentsAndRoles().entrySet()) {
                    if (CommonUtils.isNotNullEmptyOrWhitespace(entry.getKey())) {
                        String buFromSecondaryOrganisation = getBUFromOrganisation(entry.getKey());
                        if (department.equals(buFromSecondaryOrganisation) && PermissionUtils
                                .isUserAtLeastDesiredRoleInSecondaryGroup(userGroup, entry.getValue())) {
                            return true;
                        }
                    }
                }
            }
            return false;
        });
    }

    private Set<String> getUsersAtLeast(UserGroup userGroup, String department,
            boolean isFetchProjectModeratorBasedOnSecondaryRole, boolean isScanSecondaryGroup) {
        List<User> sw360users = getAllSW360Users();
        List<User> projectModeratorBasedOnSecondaryRole = Collections.emptyList();
        if (isFetchProjectModeratorBasedOnSecondaryRole && department != null) {
            projectModeratorBasedOnSecondaryRole = getFilteredStreamOfProjectModeratorBasedOnSecondaryRole(userGroup,
                    sw360users, department).collect(Collectors.toList());
        }

        List<User> allRelevantUsers = sw360users.stream().filter(getRelevantUserPredicate(userGroup, department,
                isScanSecondaryGroup, !isFetchProjectModeratorBasedOnSecondaryRole)).collect(Collectors.toList());

        allRelevantUsers.addAll(projectModeratorBasedOnSecondaryRole);

        Set<String> resultingUserEmails = allRelevantUsers.stream().map(User::getEmail).collect(Collectors.toSet());

        return resultingUserEmails;
    }

    private Set<String> getUsersAtLeast(UserGroup userGroup){
        return getUsersAtLeast(userGroup, null);
    }

    private Predicate<User> getRelevantUserPredicate(UserGroup userGroup, String department,
            boolean isScanSecondaryGroup, boolean isDepartmentFilterRequired) {
        return user1 -> {
            boolean isUserAtLeastByPrimaryGrpPass = PermissionUtils.isUserAtLeast(userGroup, user1);
            boolean isUserAtLeastBySecGrpPass = false;
            boolean isDepartmentFilterPassed = true;
            if ((!isUserAtLeastByPrimaryGrpPass || department != null) && isScanSecondaryGroup) {
                Set<UserGroup> allSecRoles = !CommonUtils.isNullOrEmptyMap(user1.getSecondaryDepartmentsAndRoles())
                        ? user1.getSecondaryDepartmentsAndRoles().entrySet().stream()
                                .flatMap(entry -> entry.getValue().stream()).collect(Collectors.toSet())
                        : new HashSet<UserGroup>();
                isUserAtLeastBySecGrpPass = PermissionUtils.isUserAtLeastDesiredRoleInSecondaryGroup(userGroup,
                        allSecRoles);
            }

            if (isDepartmentFilterRequired && department != null) {
                if (isUserAtLeastByPrimaryGrpPass && user1.getDepartment().equals(department)) {
                    return true;
                }

                if (isUserAtLeastBySecGrpPass) {
                    for (Entry<String, Set<UserGroup>> entry : user1.getSecondaryDepartmentsAndRoles().entrySet()) {
                        if (PermissionUtils.isUserAtLeastDesiredRoleInSecondaryGroup(userGroup, entry.getValue())
                                && department.equals(entry.getKey())) {
                            return true;
                        }
                    }
                }
                isDepartmentFilterPassed = false;
            }
            return isDepartmentFilterPassed && (isUserAtLeastByPrimaryGrpPass || isUserAtLeastBySecGrpPass);
        };
    }

    private List<User> getAllSW360Users() {
        List<User> sw360users = Collections.emptyList();
        try {
            UserService.Iface client = (new ThriftClients()).makeUserClient();
            sw360users = CommonUtils.nullToEmptyList(client.getAllUsers());
        } catch (TException e) {
            log.error("Problem with user client", e);
        }
        return sw360users;
    }

    public void addOrUpdate(ModerationRequest request, User user) {
        addOrUpdate(request, user.getEmail());
    }
    public void addOrUpdate(ModerationRequest request, String userEmail) {
        if (request.isSetId()) {
            repository.update(request);
            sendMailNotificationsForUpdatedRequest(request, userEmail);
        } else {
            repository.add(request);
            sendMailNotificationsForNewRequest(request, userEmail);
        }
    }

    private ModerationRequest createStubRequest(User user, boolean isDeleteRequest, String documentId, Set<String> moderators) {
        final ModerationRequest request;

        List<ModerationRequest> requestByDocumentId = getRequestByDocumentId(documentId);
        Optional<ModerationRequest> firstModerationRequestOfUser = CommonUtils.getFirstModerationRequestOfUser(requestByDocumentId, user.getEmail());
        if (firstModerationRequestOfUser.isPresent() && CommonUtils.isStillRelevant(firstModerationRequestOfUser.get())) {
            request = firstModerationRequestOfUser.get();
        } else {
            request = new ModerationRequest();
            request.setRequestingUser(user.getEmail());
            request.setDocumentId(documentId);
        }

        request.setTimestamp(System.currentTimeMillis());
        request.setModerationState(ModerationState.PENDING);
        request.setRequestDocumentDelete(isDeleteRequest);
        request.setModerators(Sets.filter(moderators, notEmptyOrNull()));
        request.setRequestingUserDepartment(user.getDepartment());

        fillRequestWithCommentOfUser(request, user);;

        return request;

    }

    public Map<String, Long> getCountByModerationState(String moderator) {
        return repository.getCountByModerationState(moderator);
    }

    public Set<String> getRequestingUserDepts() {
        return repository.getRequestingUserDepts();
    }

    private void sendMailForNewCommentInCR(ClearingRequest cr, Comment comment, User user) throws SW360Exception {
        Project project = projectDatabaseHandler.getProjectById(cr.getProjectId(), user);
        Map<String, String> recipients = Maps.newHashMap();
        recipients.put(ClearingRequest._Fields.REQUESTING_USER.toString(), cr.getRequestingUser());
        recipients.put(ClearingRequest._Fields.CLEARING_TEAM.toString(), cr.getClearingTeam());
        String userDetails = new StringBuilder(CommonUtils.nullToEmptyString(user.getUserGroup())).append(MailConstants.DASH).append(SW360Utils.printFullname(user)).toString();
        mailUtil.sendClearingMail(ClearingRequestEmailTemplate.NEW_COMMENT, MailConstants.SUBJECT_FOR_CLEARING_REQUEST_COMMENT, recipients,
            userDetails, CommonUtils.nullToEmptyString(cr.getId()), SW360Utils.printName(project), CommonUtils.nullToEmptyString(comment.getText()));
    }

    private void sendMailNotificationsForNewRequest(ModerationRequest request, String userEmail){
        mailUtil.sendMail(request.getModerators(), userEmail,
                MailConstants.SUBJECT_FOR_NEW_MODERATION_REQUEST,
                MailConstants.TEXT_FOR_NEW_MODERATION_REQUEST,
                SW360Constants.NOTIFICATION_CLASS_MODERATION_REQUEST, ModerationRequest._Fields.MODERATORS.toString());
    }

    private void sendMailNotificationsForUpdatedRequest(ModerationRequest request, String userEmail){
        mailUtil.sendMail(request.getModerators(), userEmail,
                MailConstants.SUBJECT_FOR_UPDATE_MODERATION_REQUEST,
                MailConstants.TEXT_FOR_UPDATE_MODERATION_REQUEST,
                SW360Constants.NOTIFICATION_CLASS_MODERATION_REQUEST, ModerationRequest._Fields.MODERATORS.toString());
    }

    private void sendMailToUserForDeclinedRequest(ModerationRequest request){
        boolean isUserRequest = request.getDocumentType() == DocumentType.USER;
        if (isUserRequest){
            mailUtil.sendMail(request.getRequestingUser(),
                    MailConstants.SUBJECT_FOR_DECLINED_USER_MODERATION_REQUEST,
                    MailConstants.TEXT_FOR_DECLINED_USER_MODERATION_REQUEST,
                    SW360Constants.NOTIFICATION_CLASS_MODERATION_REQUEST,
                    ModerationRequest._Fields.REQUESTING_USER.toString(),
                    false);
        } else {
            mailUtil.sendMail(request.getRequestingUser(),
                    MailConstants.SUBJECT_FOR_DECLINED_MODERATION_REQUEST,
                    MailConstants.TEXT_FOR_DECLINED_MODERATION_REQUEST,
                    SW360Constants.NOTIFICATION_CLASS_MODERATION_REQUEST,
                    ModerationRequest._Fields.REQUESTING_USER.toString(),
                    true,
                    ThriftEnumUtils.enumToString(request.getDocumentType()),
                    request.getDocumentName());
        }
    }

    private void sendMailNotificationsForAcceptedRequest(ModerationRequest request) {
        boolean isUserRequest = request.getDocumentType() == DocumentType.USER;
        mailUtil.sendMail(request.getRequestingUser(),
                MailConstants.SUBJECT_FOR_ACCEPTED_MODERATION_REQUEST,
                MailConstants.TEXT_FOR_ACCEPTED_MODERATION_REQUEST,
                SW360Constants.NOTIFICATION_CLASS_MODERATION_REQUEST,
                ModerationRequest._Fields.REQUESTING_USER.toString(),
                !isUserRequest,
                ThriftEnumUtils.enumToString(request.getDocumentType()),
                request.getDocumentName());
    }
}
