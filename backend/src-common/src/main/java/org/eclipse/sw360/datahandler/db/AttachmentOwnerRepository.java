/*
 * Copyright Siemens AG, 2018. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.db;

import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseRepository;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.ektorp.ViewQuery;
import org.ektorp.support.View;
import org.ektorp.support.Views;

import java.util.List;
import java.util.Set;

@Views({
        @View(name = "attachmentOwner",
                map = "function(doc) { if (doc.type == 'project' || doc.type == 'component' || doc.type == 'release') { " +
                        "for(var i in doc.attachments) { " +
                            "var source;" +
                            "if (doc.type == 'project') {source = {projectId: doc._id}}" +
                            "if (doc.type == 'component') {source = {componentId: doc._id}}" +
                            "if (doc.type == 'release') {source = {releaseId: doc._id}}" +
                        "emit(doc.attachments[i].attachmentContentId, source); } } }")
})

public class AttachmentOwnerRepository extends DatabaseRepository<Source> {

    public AttachmentOwnerRepository(DatabaseConnector db) {
        super(Source.class, db);
        initStandardDesignDocument();
    }

    public List<Source> getOwnersByIds(Set<String> ids) {
        ViewQuery viewQuery = createQuery("attachmentOwner").includeDocs(false).keys(ids);
        return queryView(viewQuery);
    }
}
