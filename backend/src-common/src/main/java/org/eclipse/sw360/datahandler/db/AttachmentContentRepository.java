/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.db;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.users.User;

import com.cloudant.client.api.model.Response;
import com.cloudant.client.api.model.DesignDocument.MapReduce;
import com.cloudant.client.api.views.Key;
import com.cloudant.client.api.views.UnpaginatedRequestBuilder;
import com.cloudant.client.api.views.ViewRequestBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CRUD access for the Attachment class
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author daniele.fognini@tngtech.com
 */
public class AttachmentContentRepository extends DatabaseRepositoryCloudantClient<AttachmentContent> {

    private static final String ALL = "function(doc) { if (doc.type == 'attachment') emit(null, doc._id) }";
    private static final String ONLYREMOTES = "function(doc) { if(doc.type == 'attachment' && doc.onlyRemote) { emit(null, doc) } }";

    public AttachmentContentRepository(DatabaseConnectorCloudant db) {
        super(db, AttachmentContent.class);
        Map<String, MapReduce> views = new HashMap<String, MapReduce>();
        views.put("onlyRemotes", createMapReduce(ONLYREMOTES, null));
        views.put("all", createMapReduce(ALL, null));
        initStandardDesignDocument(views, db);
    }

    public List<AttachmentContent> getOnlyRemoteAttachments() {
        ViewRequestBuilder query = getConnector().createQuery(AttachmentContent.class, "onlyRemotes");
        UnpaginatedRequestBuilder req = query.newRequest(Key.Type.STRING, Object.class).includeDocs(true);
        return queryView(req);
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

        final List<Response> documentOperationResults = getConnector().deleteBulk(unusedAttachmentContents);
        if (unusedAttachmentContents.isEmpty() || !documentOperationResults.isEmpty()) {
            requestSummary.setRequestStatus(RequestStatus.SUCCESS);
        }else{
            requestSummary.setRequestStatus(RequestStatus.FAILURE);
        }
        return requestSummary;
    }
}
