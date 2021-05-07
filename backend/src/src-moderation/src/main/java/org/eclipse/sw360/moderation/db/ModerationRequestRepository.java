/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.moderation.db;

import org.eclipse.sw360.components.summary.ModerationRequestSummary;
import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.couchdb.SummaryAwareRepository;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;

import com.cloudant.client.api.model.DesignDocument.MapReduce;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CRUD access for the ModerationRequest class
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 */
public class ModerationRequestRepository extends SummaryAwareRepository<ModerationRequest> {
    private static final String ALL = "function(doc) { if (doc.type == 'moderation') emit(null, doc._id) }";

    private static final String DOCUMENTS_VIEW = "function(doc) { " +
            "  if (doc.type == 'moderation') {" +
            "    emit(doc.documentId, doc._id);" +
            "  }" +
            "}";

    private static final String USERS_VIEW = "function(doc) { " +
            "  if (doc.type == 'moderation') {" +
            "    emit(doc.requestingUser, doc);" +
            "    }" +
            "}";

    private static final String MODERATORS_VIEW = "function(doc) {" +
            "  if (doc.type == 'moderation') {" +
            "    for(var i in doc.moderators) {" +
            "      emit(doc.moderators[i], doc);" +
            "    }" +
            "  }" +
            "}";

    public ModerationRequestRepository(DatabaseConnectorCloudant db) {
        super(ModerationRequest.class, db, new ModerationRequestSummary());
        Map<String, MapReduce> views = new HashMap<String, MapReduce>();
        views.put("all", createMapReduce(ALL, null));
        views.put("documents", createMapReduce(DOCUMENTS_VIEW, null));
        views.put("moderators", createMapReduce(MODERATORS_VIEW, null));
        views.put("users", createMapReduce(USERS_VIEW, null));
        initStandardDesignDocument(views, db);
    }

    public List<ModerationRequest> getRequestsByDocumentId(String documentId) {
        return queryView("documents", documentId);
    }

    public List<ModerationRequest> getRequestsByModerator(String moderator) {
        return makeSummaryFromFullDocs(SummaryType.SHORT, queryView("moderators", moderator));
    }

    public List<ModerationRequest> getRequestsByRequestingUser(String user) {
        return makeSummaryFromFullDocs(SummaryType.SHORT, queryView("users", user));
    }

}
