/*
 * Copyright Siemens AG, 2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseRepository;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.ektorp.ViewQuery;
import org.ektorp.support.View;
import org.ektorp.support.Views;

import java.util.List;
import java.util.Set;

@Views({
        @View(name = "byid",
                map = "function(doc) { if (doc.type == 'project' || doc.type == 'component' || doc.type == 'release') { " +
                        "for(var i in doc.attachments) { " +
                        "emit(doc.attachments[i].attachmentContentId, doc.attachments[i]); } } }"),
        @View(name = "bysha1",
                map = "function(doc) { if (doc.type == 'project' || doc.type == 'component' || doc.type == 'release') { " +
                        "for(var i in doc.attachments) { " +
                        "emit(doc.attachments[i].sha1, doc.attachments[i]); } } }")
})

public class AttachmentRepository extends DatabaseRepository<Attachment> {

    public AttachmentRepository(DatabaseConnector db) {
        super(Attachment.class, db);
        initStandardDesignDocument();
    }

    public List<Attachment> getAttachmentsByIds(Set<String> ids) {
        ViewQuery viewQuery = createQuery("byid").includeDocs(false).keys(ids);
        return queryView(viewQuery);
    }

    public List<Attachment> getAttachmentsBySha1s(Set<String> sha1s) {
        ViewQuery viewQuery = createQuery("bysha1").includeDocs(false).keys(sha1s);
        return queryView(viewQuery);
    }
}
