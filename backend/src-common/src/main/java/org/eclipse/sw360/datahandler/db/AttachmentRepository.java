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

package org.eclipse.sw360.attachments.db;

import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseRepository;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.ektorp.DocumentOperationResult;
import org.ektorp.ViewQuery;
import org.ektorp.support.View;
import org.ektorp.support.Views;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CRUD access for the Attachment class
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author daniele.fognini@tngtech.com
 */
@Views({
        @View(name = "all", map = "function(doc) { if (doc.type == 'attachment') emit(null, doc._id) }"),
        @View(name = "onlyRemotes", map = "function(doc) { if(doc.type == 'attachment' && doc.onlyRemote) { emit(null, doc) } }")
})
public class AttachmentRepository extends DatabaseRepository<AttachmentContent> {

    public AttachmentRepository(DatabaseConnector db) {
        super(AttachmentContent.class, db);

        initStandardDesignDocument();
    }

    public List<AttachmentContent> getOnlyRemoteAttachments() {
        ViewQuery query = createQuery("onlyRemotes");
        query.includeDocs(false);
        return queryView(query);
    }

    public RequestSummary vacuumAttachmentDB(User user, final Set<String> usedIds) {
        final RequestSummary requestSummary = new RequestSummary();
        if (!PermissionUtils.isAdmin(user))
            return requestSummary.setRequestStatus(RequestStatus.FAILURE);

        final List<AttachmentContent> allAttachmentContents = getAll();
        final Set<AttachmentContent> unusedAttachmentContents = allAttachmentContents.stream()
                .filter(input -> !usedIds.contains(input.getId()))
                .collect(Collectors.toSet());

        requestSummary.setTotalElements(allAttachmentContents.size());
        requestSummary.setTotalAffectedElements(unusedAttachmentContents.size());

        final List<DocumentOperationResult> documentOperationResults = deleteBulk(unusedAttachmentContents);
        if (documentOperationResults.isEmpty()) {
            requestSummary.setRequestStatus(RequestStatus.SUCCESS);
        }else{
            requestSummary.setRequestStatus(RequestStatus.FAILURE);
        }
        return requestSummary;
    }
}
