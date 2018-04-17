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

package org.eclipse.sw360.moderation.db;

import org.eclipse.sw360.components.summary.ModerationRequestSummary;
import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.SummaryAwareRepository;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.ektorp.support.View;

import java.util.List;

/**
 * CRUD access for the ModerationRequest class
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 */
@View(name = "all", map = "function(doc) { if (doc.type == 'moderation') emit(null, doc._id) }")
public class ModerationRequestRepository extends SummaryAwareRepository<ModerationRequest> {

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

    public ModerationRequestRepository(DatabaseConnector db) {
        super(ModerationRequest.class, db, new ModerationRequestSummary());

        initStandardDesignDocument();
    }

    @View(name = "documents", map = DOCUMENTS_VIEW)
    public List<ModerationRequest> getRequestsByDocumentId(String documentId) {
        return queryView("documents", documentId);
    }

    @View(name = "moderators", map = MODERATORS_VIEW)
    public List<ModerationRequest> getRequestsByModerator(String moderator) {
        return makeSummaryFromFullDocs(SummaryType.SHORT, queryView("moderators", moderator));
    }

    @View(name = "users", map = USERS_VIEW)
    public List<ModerationRequest> getRequestsByRequestingUser(String user) {
        return makeSummaryFromFullDocs(SummaryType.SHORT, queryView("users", user));
    }

}
