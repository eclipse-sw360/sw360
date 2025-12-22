/*
 * Copyright Bosch Software Innovations GmbH, 2018.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.commonIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentStreamConnector;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Component
public class AttachmentFrontendUtils {

    private static final Logger log = LogManager.getLogger(AttachmentFrontendUtils.class);

    @Autowired
    private AttachmentStreamConnector connector;

    protected final ThreadLocal<AttachmentService.Iface> attchmntClient = ThreadLocal.<AttachmentService.Iface>withInitial(
            () -> {
                return new ThriftClients().makeAttachmentClient();
            });

    public AttachmentFrontendUtils() {
    }

    public InputStream getStreamToServeAFile(Collection<AttachmentContent> attachments, User user, Object context)
            throws TException, IOException {
        if (attachments == null) {
            throw new SW360Exception("Tried to download empty set of Attachments");
        } else if (attachments.size() == 0) {
            return getConnector().getAttachmentBundleStream(new HashSet<>(), user, context);
        } else if(attachments.size() == 1) {
            // Temporary solutions, permission check needs to be implemented (getAttachmentStream)
            return getConnector().unsafeGetAttachmentStream(attachments.iterator().next());
        } else {
            return getConnector().getAttachmentBundleStream(new HashSet<>(attachments), user, context);
        }
    }

    public InputStream getStreamToServeBundle(Collection<AttachmentContent> attachments, User user, Object context)
            throws TException, IOException {
        if (attachments == null || attachments.size() == 0) {
            throw new SW360Exception("Tried to download empty set of Attachments");
        } else {
            return getConnector().getAttachmentBundleStream(new HashSet<>(attachments), user, context);
        }
    }

    public AttachmentStreamConnector getConnector() throws TException {
        return connector;
    }

    public AttachmentContent getAttachmentContent(String id) throws TException {
        return attchmntClient.get().getAttachmentContent(id);
    }

    public AttachmentContent makeAttachmentContent(AttachmentContent attachmentContent) throws TException {
        return attchmntClient.get().makeAttachmentContent(attachmentContent);
    }

    public Attachment getAttachmentForDisplay(User user, String attachmentContentId) {
        try {
            String filename = getAttachmentContent(attachmentContentId).getFilename();
            return CommonUtils.getNewAttachment(user, attachmentContentId, filename);
        } catch (TException e) {
            log.error("Could not get attachment content", e);
        }
        return null;
    }

    public void deleteAttachments(Set<String> attachmentContentIds){
        try {
            for(String id: attachmentContentIds) {
                attchmntClient.get().deleteAttachmentContent(id);
            }
        } catch (TException e){
            log.error("Could not delete attachments from database.",e);
        }
    }


    public Attachment uploadAttachmentContent(AttachmentContent attachmentContent, InputStream fileStream, User sw360User) {
        if (attachmentContent != null) {
            try {
                connector.uploadAttachment(attachmentContent, fileStream);
                if (sw360User != null) {
                    return CommonUtils.getNewAttachment(sw360User, attachmentContent.getId(), attachmentContent.getFilename());
                } else {
                    return CommonUtils.getNewAttachment(attachmentContent.getId(), attachmentContent.getFilename());
                }
            } catch (TException e) {
                log.error("Error saving attachment part", e);
            }
        }
        return null;
    }

    protected AttachmentContent updateAttachmentContent(AttachmentContent attachment) throws TException {
        try {
            attchmntClient.get().updateAttachmentContent(attachment);
        } catch (SW360Exception e) {
            log.error("Error updating attachment", e);
            return null;
        }
        return attachment;
    }
}
