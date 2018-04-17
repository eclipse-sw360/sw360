/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *//*


package org.eclipse.sw360.moderation.db;

import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;

import java.net.MalformedURLException;

import static org.eclipse.sw360.datahandler.common.SW360Assert.assertId;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.thrift.ThriftUtils.copyField;

*/
/**
 * Class for accessing the CouchDB database and updating documents after moderation approval
 *
 * @author cedric.bodet@tngtech.com
 *//*

public class DocumentDatabaseHandler {

    public static final String ORIGINAL_NOT_FOUND = "Original document could not be found in database!";
    public static final String UPDATED_NOT_FOUND = "Updated document could not be found in moderation request!";
    */
/**
     * Connection to the couchDB database
     *//*

    private final DatabaseConnector db;

    public DocumentDatabaseHandler(String url, String dbName) throws MalformedURLException {
        db = new DatabaseConnector(url, dbName);
    }

    public String updateDocument(ModerationRequest request) throws SW360Exception {
        assertNotNull(request.documentType);
        assertId(request.documentId);

        if (request.requestDocumentDelete) {
            db.delete(request.documentId);
        } else {
            switch (request.documentType) {
                case COMPONENT:
                    updateComponent(request);
                    break;
                case RELEASE:
                    updateRelease(request);
                    break;
                case PROJECT:
                    updateProject(request);
                    break;
                default:
                    break;
            }

        }
        return request.documentId;
    }

    private void updateComponent(ModerationRequest request) throws SW360Exception {
        Component original = db.get(Component.class, request.documentId);
        assertNotNull(original, ORIGINAL_NOT_FOUND);
        Component updated = request.getComponentAdditions();
        assertNotNull(updated, UPDATED_NOT_FOUND);

        copyField(original, updated, Component._Fields.ID);
        copyField(original, updated, Component._Fields.REVISION);
        copyField(original, updated, Component._Fields.RELEASE_IDS);

        updated.releases = null;

        // Update document in the database
        db.update(updated);
    }

    private void updateRelease(ModerationRequest request) throws SW360Exception {
        Release original = db.get(Release.class, request.documentId);
        assertNotNull(original, ORIGINAL_NOT_FOUND);
        Release updated = request.getReleaseAdditions();
        assertNotNull(updated, UPDATED_NOT_FOUND);

        copyField(original, updated, Release._Fields.ID);
        copyField(original, updated, Release._Fields.REVISION);

        if (updated.isSetVendor() && !updated.isSetVendorId()) {
            updated.setVendorId(updated.getVendor().getId());
            updated.unsetVendor();
        }

        // Update document in the database
        db.update(updated);
    }

    private void updateProject(ModerationRequest request) throws SW360Exception {
        Project original = db.get(Project.class, request.documentId);
        assertNotNull(original, ORIGINAL_NOT_FOUND);
        Project updated = request.getProjectAdditions();
        assertNotNull(updated, UPDATED_NOT_FOUND);

        copyField(original, updated, Project._Fields.ID);
        copyField(original, updated, Project._Fields.REVISION);

        // Update document in the database
        db.update(updated);
    }

}
*/
