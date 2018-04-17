/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.moderation.db;

import com.google.common.collect.Sets;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.ProjectDatabaseHandler;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.ModerationState;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.moderation.DocumentType;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.licenses.db.LicenseDatabaseHandler;
import org.eclipse.sw360.mail.MailConstants;
import org.eclipse.sw360.mail.MailUtil;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.ektorp.http.HttpClient;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.notEmptyOrNull;

/**
 * Class for accessing the CouchDB database for the moderation objects
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class ModerationDatabaseHandler {

    private static final Logger log = Logger.getLogger(ModerationDatabaseHandler.class);

    /**
     * Connection to the couchDB database
     */
    private final ModerationRequestRepository repository;
    private final LicenseDatabaseHandler licenseDatabaseHandler;
    private final ProjectDatabaseHandler projectDatabaseHandler;
    private final ComponentDatabaseHandler componentDatabaseHandler;
    private final DatabaseConnector db;

    private final MailUtil mailUtil = new MailUtil();

    public ModerationDatabaseHandler(Supplier<HttpClient> httpClient, String dbName, String attachmentDbName) throws MalformedURLException {
        db = new DatabaseConnector(httpClient, dbName);

        // Create the repository
        repository = new ModerationRequestRepository(db);

        licenseDatabaseHandler = new LicenseDatabaseHandler(httpClient, dbName);
        projectDatabaseHandler = new ProjectDatabaseHandler(httpClient, dbName, attachmentDbName);
        componentDatabaseHandler = new ComponentDatabaseHandler(httpClient, dbName, attachmentDbName);
    }

    public List<ModerationRequest> getRequestsByModerator(String moderator) {
        return repository.getRequestsByModerator(moderator);
    }

    public List<ModerationRequest> getRequestsByRequestingUser(String user) {
        return repository.getRequestsByRequestingUser(user);
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
        try {
            String department =  getDepartmentByUserEmail(component.getCreatedBy());
            CommonUtils.addAll(moderators, getUsersAtLeast(UserGroup.CLEARING_ADMIN, department));
        } catch (TException e){
            log.error("Could not get user from database. Clearing admins not added as moderators, since department is missing.");
        }
        CommonUtils.addAll(moderators, getUsersAtLeast(UserGroup.ADMIN));

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
        try{
            String department =  getDepartmentByUserEmail(release.getCreatedBy());
            CommonUtils.addAll(moderators, getUsersAtLeast(UserGroup.CLEARING_ADMIN, department));
        } catch (TException e){
            log.error("Could not get users from database. Clearing admins not added as moderators, since department is missing.");
        }
        CommonUtils.addAll(moderators, getUsersAtLeast(UserGroup.ADMIN));
        return moderators;
    }

    @NotNull
    private Set<String> getEccReleaseModerators(Release release) {
        // Define moderators
        Set<String> moderators = new HashSet<>();
        try{
            String department =  getDepartmentByUserEmail(release.getCreatedBy());
            CommonUtils.addAll(moderators, getUsersAtLeast(UserGroup.ECC_ADMIN, department));
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
        Set<String> moderators = getProjectModerators(dbproject);
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
    private Set<String> getProjectModerators(Project project) {
        Set<String> moderators = new HashSet<>();
        if (project.getClearingState() != ProjectClearingState.CLOSED){
            CommonUtils.add(moderators, project.getCreatedBy());
            CommonUtils.add(moderators, project.getProjectResponsible());
            CommonUtils.addAll(moderators, project.getModerators());
        }
        CommonUtils.addAll(moderators, getUsersAtLeast(UserGroup.CLEARING_ADMIN, project.getBusinessUnit(), false));
        CommonUtils.addAll(moderators, getUsersAtLeast(UserGroup.ADMIN));
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
        Set<String> moderators = sw360users
                .stream()
                .filter(user1 -> PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user1))
                .filter(user1 -> user1.getDepartment().equals(department))
                .map(User::getEmail)
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
        return getUsersAtLeast(userGroup, department, true);
    }

    private Set<String> getUsersAtLeast(UserGroup userGroup, String department, boolean defaultToAllUsersInGroup) {
        List<User> sw360users = getAllSW360Users();
        List<User> allRelevantUsers = sw360users
                    .stream()
                    .filter(user1 -> PermissionUtils.isUserAtLeast(userGroup, user1))
                    .collect(Collectors.toList());

        List<User> relevantUsersOfDepartment = Collections.emptyList();
        if(department != null) {
            relevantUsersOfDepartment = allRelevantUsers.stream()
                    .filter(user -> user.getDepartment().equals(department))
                    .collect(Collectors.toList());
        }

        List<User> defaultUsersList = defaultToAllUsersInGroup ? allRelevantUsers : Collections.emptyList();
        List<User> resultingUsers = relevantUsersOfDepartment.isEmpty() ? defaultUsersList : relevantUsersOfDepartment;

        Set<String> resultingUserEmails = resultingUsers.stream()
                    .map(User::getEmail)
                    .collect(Collectors.toSet());

        return resultingUserEmails;
    }

    private Set<String> getUsersAtLeast(UserGroup userGroup){
        return getUsersAtLeast(userGroup, null);
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
