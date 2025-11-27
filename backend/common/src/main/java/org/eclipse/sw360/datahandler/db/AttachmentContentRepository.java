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

import com.ibm.cloud.cloudant.v1.model.DocumentResult;
import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import com.ibm.cloud.cloudant.v1.model.PostViewOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

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
@Component
public class AttachmentContentRepository extends DatabaseRepositoryCloudantClient<AttachmentContent> {

    private static final String ALL = "function(doc) { if (doc.type == 'attachment') emit(null, doc._id) }";
    private static final String ONLYREMOTES = "function(doc) { if(doc.type == 'attachment' && doc.onlyRemote) { emit(null, doc) } }";

    @Autowired
    public AttachmentContentRepository(
            @Qualifier("CLOUDANT_DB_CONNECTOR_ATTACHMENTS") DatabaseConnectorCloudant db
    ) {
        super(db, AttachmentContent.class);
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("onlyRemotes", createMapReduce(ONLYREMOTES, null));
        views.put("all", createMapReduce(ALL, null));
        initStandardDesignDocument(views, db);
    }

    public List<AttachmentContent> getOnlyRemoteAttachments() {
        PostViewOptions req = getConnector()
                .getPostViewQueryBuilder(AttachmentContent.class, "onlyRemotes")
                .includeDocs(true)
                .build();
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

        final List<DocumentResult> documentOperationResults = getConnector().deleteIds(
                unusedAttachmentContents
                        .stream()
                        .map(AttachmentContent::getId).collect(Collectors.toSet())
        );
        if (unusedAttachmentContents.isEmpty() || !documentOperationResults.isEmpty()) {
            requestSummary.setRequestStatus(RequestStatus.SUCCESS);
        } else {
            requestSummary.setRequestStatus(RequestStatus.FAILURE);
        }
        return requestSummary;
    }
}
