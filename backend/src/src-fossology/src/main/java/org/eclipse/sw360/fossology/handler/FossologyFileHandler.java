/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.fossology.handler;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.FilledAttachment;
import org.eclipse.sw360.datahandler.thrift.components.ClearingState;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.FossologyStatus;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.fossology.ssh.FossologyUploader;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Iterables.all;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.union;
import static org.eclipse.sw360.datahandler.common.CommonUtils.*;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.common.SW360Assert.failIf;
import static java.util.Collections.singleton;
import static org.apache.log4j.Logger.getLogger;

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

        if (!release.isSetFossologyId()) {
            /* send the attachment as a new upload */
            AttachmentContent attachmentContent = filledAttachment.getAttachmentContent();
            return sendToFossologyNewUpload(release, user, clearingTeam, attachmentContent, componentClient);
        } else {
            if (!checkSourceAttachment(release, filledAttachment)) {
                return RequestStatus.FAILURE; // TODO summary
            } else {
                /* duplicate the old upload making it visible for clearingTeam */
                return sendToFossologyExistingUpload(release, user, clearingTeam, componentClient);
            }
        }
    }

    protected boolean checkSourceAttachment(Release release, FilledAttachment filledAttachment) {
        boolean check = true;
        if (!Objects.equals(filledAttachment.getAttachmentContent().getId(), release.getAttachmentInFossology())) {
            log.error("the source attachment of this release is not the one in FOSSology (rel=" + release.getId() + ")");
            check = false;
        }
        return check;
    }

    private RequestStatus sendToFossologyNewUpload(Release release, User user, String clearingTeam, AttachmentContent attachmentContent, ComponentService.Iface componentService) throws TException {
        InputStream stream = attachmentConnector.getAttachmentStream(attachmentContent, user, release);
        try {
            int fossologyUploadId = fossologyUploader.uploadToFossology(stream, attachmentContent, clearingTeam);
            if (fossologyUploadId > 0) {
                //to avoid race conditions, get the object again
                setFossologyStatus(release, clearingTeam, FossologyStatus.SENT, Integer.toString(fossologyUploadId), attachmentContent.getId());
                updateReleaseClearingState(release, FossologyStatus.SENT);
                updateRelease(release, user, componentService);
                return RequestStatus.SUCCESS;
            }
        } finally {
            closeQuietly(stream, log);
        }

        return RequestStatus.FAILURE;
    }

    private RequestStatus sendToFossologyExistingUpload(Release release, User user, String clearingTeam, ComponentService.Iface componentClient) throws TException {
        int fossologyUploadId;
        fossologyUploadId = toUnsignedInt(release.getFossologyId());
        failIf(fossologyUploadId <= 0, "release %s has an inconsistent FossologyId", release.getId());

        FossologyStatus currentStatus = fossologyUploader.getStatusInFossology(fossologyUploadId, clearingTeam);
        if (isVisible(currentStatus)) {
            updateFossologyStatus(release, user, clearingTeam, currentStatus, componentClient);
            return RequestStatus.SUCCESS;
        } else {
            return duplicateUploadFor(release, user, clearingTeam, fossologyUploadId, componentClient);
        }
    }

    private RequestStatus duplicateUploadFor(Release release, User user, String clearingTeam, int fossologyUploadId, ComponentService.Iface componentClient) throws TException {
        boolean success;
        success = fossologyUploader.duplicateInFossology(fossologyUploadId, clearingTeam);
        if (success) {
            updateFossologyStatus(release, user, clearingTeam, FossologyStatus.SENT, componentClient);
            return RequestStatus.SUCCESS;
        } else {
            return RequestStatus.FAILURE;
        }
    }

    private void updateFossologyStatus(Release release, User user, String clearingTeam, FossologyStatus currentStatus, ComponentService.Iface componentService) throws TException {
        setFossologyStatus(release, clearingTeam, currentStatus);
        updateReleaseClearingState(release, currentStatus);
        updateRelease(release, user, componentService);
    }

    protected static boolean isVisible(FossologyStatus statusInFossology) {
        return statusInFossology != null && FossologyStatus.INACCESSIBLE.compareTo(statusInFossology) < 0;
    }

    protected static boolean isError(FossologyStatus status) {
        return status == null || status.compareTo(FossologyStatus.ERROR) <= 0;
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

        if (release.isSetFossologyId()) {
            int fossologyId = CommonUtils.toUnsignedInt(release.getFossologyId());

            boolean updateInDb = true;
            for (String allClearingTeam : getAllClearingTeams(release, clearingTeam)) {
                final FossologyStatus status = fossologyUploader.getStatusInFossology(fossologyId, allClearingTeam);
                setFossologyStatus(release, allClearingTeam, status);

                if (isError(status)) {
                    updateInDb = false;
                }
            }
            Optional<FossologyStatus> maxStatus = nullToEmptyMap(release.getClearingTeamToFossologyStatus())
                    .values()
                    .stream()
                    .max(FossologyStatus::compareTo);
            updateReleaseClearingState(release, maxStatus);

            if (updateInDb) {
                updateRelease(release, user, componentClient);
            }
        }

        getReleaseAndUnlockIt(releaseId, user, componentClient); // just unlockit

        return release;
    }

    private void updateReleaseClearingState(Release release, FossologyStatus fossologyStatus) {
        updateReleaseClearingState(release, Optional.of(fossologyStatus));
    }
    private void updateReleaseClearingState(Release release, Optional<FossologyStatus> fossologyStatus) {
        Optional<ClearingState> newClearingState = fossologyStatus.flatMap(this::mapFossologyStatusToClearingState);
        if (newClearingState.isPresent() && newClearingState.get().compareTo(release.getClearingState()) > 0){
            release.setClearingState(newClearingState.get());
        }
    }

    private Optional<ClearingState> mapFossologyStatusToClearingState(FossologyStatus fossologyStatus) {
        if (fossologyStatus==FossologyStatus.IN_PROGRESS){
            return Optional.of(ClearingState.UNDER_CLEARING);
        } else if (fossologyStatus.compareTo(FossologyStatus.SENT) >= 0 &&
                fossologyStatus.compareTo(FossologyStatus.IN_PROGRESS) < 0){
            return Optional.of(ClearingState.SENT_TO_FOSSOLOGY);
        }
        return Optional.empty();
    }

    protected Release getReleaseAndUnlockIt(String releaseId, User user, ComponentService.Iface componentClient) throws TException {
        final Release release = componentClient.getReleaseById(releaseId, user);

        assertNotNull(release, "cannot get release %s", releaseId);

        final Collection<FossologyStatus> fossologyStatuses = nullToEmptyMap(release.getClearingTeamToFossologyStatus()).values();

        if (!fossologyStatuses.isEmpty() && all(fossologyStatuses, equalTo(FossologyStatus.REJECTED))) {
            release.unsetAttachmentInFossology();
            release.unsetFossologyId();
            release.unsetClearingTeamToFossologyStatus();

            updateRelease(release, user, componentClient);
        }

        return release;
    }

    // not static to ease testing
    protected Iterable<? extends String> getAllClearingTeams(Release release, String clearingTeam) {
        Set<String> alreadyChecked = nullToEmptyMap(release.getClearingTeamToFossologyStatus()).keySet();
        return union(alreadyChecked, singleton(clearingTeam));
    }

    protected void setFossologyStatus(Release release, final String clearingTeam, FossologyStatus status) {
        setFossologyStatus(release, clearingTeam, status, null, null);
    }

    protected void setFossologyStatus(Release release, final String clearingTeam, FossologyStatus status, String fossologyUploadId, String attachmentId) {
        Map<String, FossologyStatus> clearingTeamToStatus = release.getClearingTeamToFossologyStatus();
        if (clearingTeamToStatus == null) clearingTeamToStatus = newHashMap();

        clearingTeamToStatus.put(clearingTeam, status);
        release.setClearingTeamToFossologyStatus(clearingTeamToStatus);

        if (!isNullOrEmpty(fossologyUploadId)) {
            release.setFossologyId(fossologyUploadId);
            release.setAttachmentInFossology(attachmentId);
        }
    }

    protected RequestStatus updateRelease(Release release, User user, ComponentService.Iface componentService) throws TException {
        final RequestStatus updated = componentService.updateReleaseFossology(release, user);
        if (RequestStatus.FAILURE == updated) {
            log.error("cannot update release");
        }
        return updated;
    }

    protected Set<Attachment> getSourceAttachment(String releaseId, User user, ComponentService.Iface componentClient) throws TException {
        try {
            return componentClient.getSourceAttachments(releaseId);
        } catch (TException e) {
            log.error("error contacting component Service", e);
            throw e;
        }
    }

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
}
