/*
 * Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.fossology.handler;

import com.google.common.annotations.VisibleForTesting;

import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.FilledAttachment;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.fossology.ssh.FossologyUploader;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Iterables.all;
import static org.apache.log4j.Logger.getLogger;
import static org.eclipse.sw360.datahandler.common.CommonUtils.closeQuietly;
import static org.eclipse.sw360.datahandler.common.CommonUtils.getFirst;
import static org.eclipse.sw360.datahandler.common.CommonUtils.toUnsignedInt;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.common.SW360Assert.failIf;

/**
 * Some facts about the representation of requests to Fossology as
 * {@link ExternalToolRequest}s:
 * <ul>
 * <li>There can be more than one {@link ExternalToolRequest} for a single
 * release</li>
 * <li>Attributes that are the same in all those requests for a single release:
 * <ul>
 * <li>{@link ExternalToolRequest#externalTool} will always be
 * {@link ExternalTool#FOSSOLOGY}</li>
 * <li>{@link ExternalToolRequest#attachmentId} there can be only requests where
 * the attachmentId matches the one of the first request for this release</li>
 * <li>{@link ExternalToolRequest#attachmentHash} there can be only requests
 * where the attachmentHash matches the one of the first request for this
 * release</li>
 * <li>{@link ExternalToolRequest#toolId} the id of the clearing process in
 * fossology, will be the same for the attachment regardless of the responsible
 * teams in fossology</li>
 * </ul>
 * </li>
 * <li>Attributes that are different between all those requests for a single
 * release:
 * <ul>
 * <li>{@link ExternalToolRequest#id} internal id of request object</li>
 * <li>{@link ExternalToolRequest#createdOn} the creation date of the request
 * will normally be different</li>
 * <li>{@link ExternalToolRequest#createdBy} the user id who created the request
 * - since the clearing teams need to be different, probably the user will be
 * different as well (though it is no strong must)</li>
 * <li>{@link ExternalToolRequest#createdByGroup} the business unit of the user
 * who created the request - since the clearing teams need to be different,
 * probably the user will be different as well (though it is no strong
 * must)</li>
 * <li>{@link ExternalToolRequest#externalToolStatus} the status in fossology -
 * can be the same, but doesn't have to</li>
 * <li>{@link ExternalToolRequest#externalToolWorkflowStatus} the status of the
 * workflow in sw360 - can be the same, but doesn't have to</li>
 * <li>{@link ExternalToolRequest#linkToJob} a link to the release's clearing
 * process in fossology, will be different since responsible teams in fossology
 * are different</li>
 * <li>{@link ExternalToolRequest#toolUserGroup} the responsible clearing team
 * in fossology, will be different since the same team will not get two clearing
 * requests for the same release</li>
 * <li>{@link ExternalToolRequest#toolUserId} maybe not needed in fossology
 * context?</li>
 * </ul>
 * </li>
 * </ul>
 */
@Component
public class FossologyFileHandler {

    private final static Logger log = getLogger(FossologyFileHandler.class);

    private final AttachmentConnector attachmentConnector;
    private final FossologyUploader fossologyUploader;
    private final ThriftClients thriftClients;

    @Autowired
    public FossologyFileHandler(AttachmentConnector attachmentConnector, FossologyUploader fossologyUploader, ThriftClients thriftClients) {
        this.attachmentConnector = attachmentConnector;
        this.fossologyUploader = fossologyUploader;
        this.thriftClients = thriftClients;
    }

    public RequestStatus sendToFossology(String releaseId, User user, String clearingTeam) throws TException {
        Release release;
        final ComponentService.Iface componentClient = thriftClients.makeComponentClient();

        release = getReleaseAndUnlockIt(releaseId, user, componentClient);

        Set<Attachment> sourceAttachments = getSourceAttachment(releaseId, user, componentClient);

        if (sourceAttachments.size() != 1) {
            log.error("release " + releaseId + " does not have a single source attachment");
            return RequestStatus.FAILURE; //TODO return a summary and better fitting status
        }

        final Attachment attachment = getFirst(sourceAttachments);
        final FilledAttachment filledAttachment = fillAttachment(attachment);

        Set<ExternalToolRequest> fossologyRequests = SW360Utils.getExternalToolRequestsForTool(release,
                ExternalTool.FOSSOLOGY);
        if (fossologyRequests.size() > 0) {
            if (checkSourceAttachment(fossologyRequests, filledAttachment)) {
                /* duplicate the old upload making it visible for clearingTeam */
                return sendToFossologyExistingUpload(release, user, clearingTeam, componentClient);
            } else {
                log.error("The source attachment of this release is different from at least one already sent to "
                        + "FOSSology for this release (rel=" + release.getId() + ")");
                return RequestStatus.FAILURE; // TODO summary
            }
        } else {
            /* send the attachment as a new upload */
            return sendToFossologyNewUpload(release, user, clearingTeam, filledAttachment, componentClient);
        }
    }

    @VisibleForTesting
    protected Release getReleaseAndUnlockIt(String releaseId, User user, ComponentService.Iface componentClient)
            throws TException {
        final Release release = componentClient.getReleaseById(releaseId, user);

        assertNotNull(release, "cannot get release %s", releaseId);

        Set<ExternalToolRequest> fossologyRequests = SW360Utils.getExternalToolRequestsForTool(release,
                ExternalTool.FOSSOLOGY);
        if (!fossologyRequests.isEmpty()
                && all(fossologyRequests, etr -> ExternalToolStatus.REJECTED.equals(etr.getExternalToolStatus()))) {
            release.unsetExternalToolRequests();

            updateRelease(release, user, componentClient);
        }

        return release;
    }

    private RequestStatus updateRelease(Release release, User user, ComponentService.Iface componentService)
            throws TException {
        final RequestStatus updated = componentService.updateReleaseFossology(release, user);
        if (RequestStatus.FAILURE == updated) {
            log.error("cannot update release");
        }
        return updated;
    }

    private Set<Attachment> getSourceAttachment(String releaseId, User user, ComponentService.Iface componentClient)
            throws TException {
        try {
            return componentClient.getSourceAttachments(releaseId);
        } catch (TException e) {
            log.error("error contacting component Service", e);
            throw e;
        }
    }

    @VisibleForTesting
    protected FilledAttachment fillAttachment(Attachment attachment) throws SW360Exception {
        String attachmentContentId = attachment.getAttachmentContentId();
        try {
            AttachmentContent attachmentContent = attachmentConnector.getAttachmentContent(attachmentContentId);
            assertNotNull(attachmentContent);
            return new FilledAttachment().setAttachment(attachment).setAttachmentContent(attachmentContent);
        } catch (SW360Exception e) {
            log.error("cannot retrieve attachment " + attachmentContentId, e);
            throw e;
        }
    }

    /**
     * This method checks all given requests to Fossology whether their attachment
     * id and hash has been the same as the one of the given FilledAttachment. All
     * have to match because normally there should be no possibility to create a
     * request with different id or hash once there is already one. But to prevent
     * usage of old broken data, we just make sure that everything matches.
     *
     * @param fossologyRequests a {@link Set} of {@link ExternalToolRequest}s with
     *                          {@link ExternalTool#FOSSOLOGY}
     * @param filledAttachment  a {@link FilledAttachment} of sources that is
     *                          currently attached to a release
     * @return true if the attachment id and hashes of all fossology requests match
     *         the data of the given attachment
     */
    private boolean checkSourceAttachment(Set<ExternalToolRequest> fossologyRequests,
            FilledAttachment filledAttachment) {
        return fossologyRequests.stream().allMatch(fr -> {
            return Objects.equals(filledAttachment.getAttachmentContent().getId(), fr.getAttachmentId())
                    && Objects.equals(filledAttachment.getAttachment().getSha1(), fr.getAttachmentHash());
        });
    }

    private RequestStatus sendToFossologyNewUpload(Release release, User user, String clearingTeam, FilledAttachment filledAttachment, ComponentService.Iface componentService) throws TException {
        AttachmentContent attachmentContent = filledAttachment.getAttachmentContent();
        InputStream stream = attachmentConnector.getAttachmentStream(attachmentContent, user, release);
        try {
            int fossologyUploadId = fossologyUploader.uploadToFossology(stream, attachmentContent, clearingTeam);
            if (fossologyUploadId > 0) {
                //to avoid race conditions, get the object again
                createExternalToolRequest(release, user, clearingTeam, filledAttachment.getAttachmentContent().getId(),
                        filledAttachment.getAttachment().getSha1(), ExternalToolWorkflowStatus.SENT,
                        ExternalToolStatus.OPEN, Integer.toString(fossologyUploadId));
                release.setClearingState(ClearingState.SENT_TO_FOSSOLOGY);
                updateRelease(release, user, componentService);
                return RequestStatus.SUCCESS;
            }
        } finally {
            closeQuietly(stream, log);
        }

        return RequestStatus.FAILURE;
    }

    private ExternalToolRequest createExternalToolRequest(Release release, User user, String clearingTeam,
            String attachmentId, String attachmentHash, ExternalToolWorkflowStatus etrs, ExternalToolStatus ets,
            String fossologyId) {
        ExternalToolRequest etr = new ExternalToolRequest();
        etr.setExternalTool(ExternalTool.FOSSOLOGY);
        etr.setExternalToolWorkflowStatus(etrs);
        etr.setExternalToolStatus(ets);
        etr.setAttachmentId(attachmentId);
        etr.setAttachmentHash(attachmentHash);
        etr.setToolId(fossologyId);
        etr.setToolUserGroup(clearingTeam);
        etr.setCreatedOn(Instant.now().toString());
        etr.setCreatedBy(user.getEmail());
        etr.setCreatedByGroup(user.getDepartment());

        // TODO: are they needed in fossology case?
        // etr.setId(id);
        // etr.setToolUserId(toolUserId);
        // etr.setLinkToJob(linkToJob);

        release.addToExternalToolRequests(etr);

        return etr;
    }

    private RequestStatus sendToFossologyExistingUpload(Release release, User user, String clearingTeam, ComponentService.Iface componentClient) throws TException {
        // check if there is already a request for our clearing team
        // if not, create one by copying another and replace the important properties
        Optional<ExternalToolRequest> fossologyRequestO = SW360Utils
                .getExternalToolRequestsForTool(release, ExternalTool.FOSSOLOGY).stream()
                .filter(etr -> clearingTeam.equals(etr.getToolUserGroup())).findFirst();

        boolean existingRequest;
        ExternalToolRequest fossologyRequest;
        ExternalToolWorkflowStatus existingExternalToolWorkflowStatus = null;
        ExternalToolStatus existingExternalToolStatus = null;
        if (fossologyRequestO.isPresent()) {
            existingRequest = true;
            fossologyRequest = fossologyRequestO.get();
            existingExternalToolWorkflowStatus = fossologyRequest.getExternalToolWorkflowStatus();
            existingExternalToolStatus = fossologyRequest.getExternalToolStatus();
        } else {
            existingRequest = false;
            fossologyRequest = copyExistingEtrForNewTeam(release, user, clearingTeam);
        }

        int fossologyUploadId = toUnsignedInt(fossologyRequest.getToolId());
        failIf(fossologyUploadId <= 0, "release %s has an inconsistent FossologyId", release.getId());

        // this checks and also already updates the ExternalToolStatus in the request
        // object
        ExternalToolWorkflowStatus externalToolWorkflowStatus = fossologyUploader
                .updateStatusInFossologyRequest(fossologyRequest);
        if (ExternalToolWorkflowStatus.SENT.equals(externalToolWorkflowStatus)) {
            // this means, we were allowed to see the state, so we do not need to duplicate
            // the upload
            fossologyRequest.setExternalToolWorkflowStatus(externalToolWorkflowStatus);
            updateReleaseClearingState(release, fossologyRequest.getExternalToolStatus());
            updateRelease(release, user, componentClient);
            return RequestStatus.SUCCESS;
        } else if (ExternalToolWorkflowStatus.ACCESS_DENIED.equals(externalToolWorkflowStatus)) {
            // this means, the status request succeeded, but we are not allowed to see the
            // process, so we have to duplicate it
            return duplicateUploadFor(release, user, fossologyRequest, componentClient);
        } else {
            // the other cases are technical error, so that we actually do not know the
            // current state, so we should return a failure
            if (existingRequest) {
                // since just the status check request failed, do not use the status for the
                // general fossology status
                fossologyRequest.setExternalToolWorkflowStatus(existingExternalToolWorkflowStatus);
                fossologyRequest.setExternalToolStatus(existingExternalToolStatus);
            }
            log.error("Status test for release " + release.getId() + " and fossology id " + fossologyRequest.getToolId()
                    + " failed with problem " + externalToolWorkflowStatus + " so that we cannot know if the team "
                    + clearingTeam + " can see the process of if we would need to duplicate the upload!");
            return RequestStatus.FAILURE;
        }
    }

    private ExternalToolRequest copyExistingEtrForNewTeam(Release release, User user, String clearingTeam) {
        ExternalToolRequest externalToolRequestTemplate = SW360Utils
                .getExternalToolRequestsForTool(release, ExternalTool.FOSSOLOGY).stream().findFirst().get();
        return createExternalToolRequest(release, user, clearingTeam, externalToolRequestTemplate.getAttachmentId(),
                externalToolRequestTemplate.getAttachmentHash(), ExternalToolWorkflowStatus.SENT,
                ExternalToolStatus.OPEN, externalToolRequestTemplate.getToolId());
    }

    private void updateReleaseClearingState(Release release, ExternalToolStatus externalToolStatus) {
        ClearingState newClearingState = mapExternalToolStatusToClearingState(externalToolStatus);
        if (newClearingState != null
                && (release.getClearingState() == null || newClearingState.compareTo(release.getClearingState()) > 0)) {
            release.setClearingState(newClearingState);
        }
    }

    /**
     * This method only evaluates the {@link ExternalToolStatus} which is also
     * {@link ExternalToolStatus#OPEN} if the attachment has never been uploaded
     * successfully. So please make sure, that the
     * {@link ExternalToolWorkflowStatus} is already
     * {@link ExternalToolWorkflowStatus#SENT}.
     *
     * @param externalToolStatus the {@link ExternalToolStatus} to map onto a
     *                           {@link ClearingState}
     * @return the corresponding {@link ClearingState}
     */
    private ClearingState mapExternalToolStatusToClearingState(ExternalToolStatus externalToolStatus) {
        switch (externalToolStatus) {
        case OPEN:
            return ClearingState.SENT_TO_FOSSOLOGY;
        case IN_PROGRESS:
            return ClearingState.UNDER_CLEARING;
        case RESULT_AVAILABLE:
        case CLOSED:
            return ClearingState.REPORT_AVAILABLE;
        case REJECTED:
        default:
            return null;
        }
    }

    private RequestStatus duplicateUploadFor(Release release, User user, ExternalToolRequest etr,
            ComponentService.Iface componentClient) throws TException {
        boolean success;
        success = fossologyUploader.duplicateInFossology(Integer.valueOf(etr.getToolId()), etr.getToolUserGroup());
        if (success) {
            etr.setExternalToolWorkflowStatus(ExternalToolWorkflowStatus.SENT);
            etr.setExternalToolStatus(ExternalToolStatus.OPEN);
            updateReleaseClearingState(release, etr.getExternalToolStatus());
            updateRelease(release, user, componentClient);
            return RequestStatus.SUCCESS;
        } else {
            // this behavior is different then the one before the refactoring. until now a
            // failed attempt to duplicate the upload was not documented in the map of
            // fossology states per clearing team. since we now add the new request, we
            // document as well the first try for the new team und save the release with
            // this new state afterwards. if this should be changed back again, feel free
            etr.setExternalToolWorkflowStatus(ExternalToolWorkflowStatus.SERVER_ERROR);
            etr.setExternalToolStatus(ExternalToolStatus.OPEN);
            updateRelease(release, user, componentClient);
            return RequestStatus.FAILURE;
        }
    }

    public Release getStatusInFossology(String releaseId, User user, String clearingTeam) throws TException {
        final Release release;
        final ComponentService.Iface componentClient;

        try {
            componentClient = thriftClients.makeComponentClient();
            release = componentClient.getReleaseById(releaseId, user);
            assertNotNull(release);
        } catch (TException e) {
            log.error("cannot get release " + releaseId, e);
            throw e;
        }

        Set<ExternalToolRequest> externalToolRequests = SW360Utils.getExternalToolRequestsForTool(release,
                ExternalTool.FOSSOLOGY);
        if (externalToolRequests.size() > 0) {
            boolean updateInDb = true;
            for (ExternalToolRequest etr : externalToolRequests) {
                ExternalToolWorkflowStatus etws = fossologyUploader.updateStatusInFossologyRequest(etr);

                if (isError(etws)) {
                    updateInDb = false;
                } else {
                    etr.setExternalToolWorkflowStatus(etws);
                }
            }
            Optional<ExternalToolStatus> maxStatus = externalToolRequests.stream()
                    .map(ExternalToolRequest::getExternalToolStatus).max(ExternalToolStatus::compareTo);
            updateReleaseClearingState(release, maxStatus.get());

            if (updateInDb) {
                updateRelease(release, user, componentClient);
            }
        }

        getReleaseAndUnlockIt(releaseId, user, componentClient); // just unlockit

        return release;
    }

    private boolean isError(ExternalToolWorkflowStatus etws) {
        return etws == null || !ExternalToolWorkflowStatus.SENT.equals(etws);
    }
}
