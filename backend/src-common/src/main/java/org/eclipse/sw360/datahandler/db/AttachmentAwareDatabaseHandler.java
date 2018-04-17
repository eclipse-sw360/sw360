/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.datahandler.db;

import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyCollection;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;

public abstract class AttachmentAwareDatabaseHandler {

    public Set<Attachment> getAllAttachmentsToKeep(Set<Attachment> originalAttachments, Set<Attachment> changedAttachments) {
        Set <Attachment> attachmentsToKeep = new HashSet<>();
        attachmentsToKeep.addAll(nullToEmptySet(changedAttachments));
        Set<String> alreadyPresentIdsInAttachmentsToKeep = nullToEmptyCollection(attachmentsToKeep).stream().map(Attachment::getAttachmentContentId).collect(Collectors.toSet());

        // prevent deletion of already accepted attachments
        Set<Attachment> acceptedAttachmentsNotYetToKeep = nullToEmptySet(originalAttachments).stream().filter(a -> (a.getCheckStatus() == CheckStatus.ACCEPTED && !alreadyPresentIdsInAttachmentsToKeep.contains(a.getAttachmentContentId()))).collect(Collectors.toSet());
        attachmentsToKeep.addAll(acceptedAttachmentsNotYetToKeep);

        return attachmentsToKeep;
    }


    public AttachmentAwareDatabaseHandler() {

    }
}
