/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.moderation.testutil;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.moderation.DocumentType;
import org.eclipse.sw360.datahandler.services.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.services.projects.Project;

import java.util.Set;

import java.net.MalformedURLException;

/**
 * Created by GerritGrenzebachTNG on 09.02.15.
 */
public class DatabaseTestSetup {


    public static void main(String[] args) throws MalformedURLException, SW360Exception {

        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), DatabaseSettingsTest.COUCH_DB_DATABASE);

        Project project = new Project().setName("Test Project");
        project.setModerators(Set.of("user1"));

        db.add(project);

        ModerationRequest moderationRequest = new ModerationRequest();
        moderationRequest.setDocumentId(project.getId()).setDocumentType(DocumentType.PROJECT);
        moderationRequest.setRequestingUser("cedric.bodet@tngtech.com");
        moderationRequest.setModerators(Set.of("cedric.bodet@tngtech.com"));

        db.add(moderationRequest);

    }

}
