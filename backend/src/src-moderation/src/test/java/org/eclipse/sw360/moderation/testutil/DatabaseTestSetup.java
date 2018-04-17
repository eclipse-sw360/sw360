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

package org.eclipse.sw360.moderation.testutil;

import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.moderation.DocumentType;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;

import java.net.MalformedURLException;

/**
 * Created by GerritGrenzebachTNG on 09.02.15.
 */
public class DatabaseTestSetup {


    public static void main(String[] args) throws MalformedURLException {

        DatabaseConnector db = new DatabaseConnector(DatabaseSettings.getConfiguredHttpClient(), DatabaseSettings.COUCH_DB_DATABASE);

        Project project = new Project().setName("Test Project");
        project.addToModerators("user1");

        db.add(project);

        ModerationRequest moderationRequest = new ModerationRequest();
        moderationRequest.setDocumentId(project.id).setDocumentType(DocumentType.PROJECT);
        moderationRequest.setRequestingUser("cedric.bodet@tngtech.com");
        moderationRequest.addToModerators("cedric.bodet@tngtech.com");

        db.add(moderationRequest);

    }

}
