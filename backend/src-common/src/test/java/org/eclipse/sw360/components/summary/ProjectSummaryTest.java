/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.components.summary;

import com.google.common.collect.ImmutableSet;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStateSummary;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.junit.Test;

import java.util.Collections;

import static org.eclipse.sw360.exporter.ProjectExporter.PROJECT_RENDERED_FIELDS;
import static org.junit.Assert.assertNotNull;

public class ProjectSummaryTest {

    @Test
    public void testAllRequiredFieldsAreSet() throws Exception {
        Project project = new Project();
        Project copy = new Project();

        for (Project._Fields renderedField : PROJECT_RENDERED_FIELDS) {
            switch (renderedField) {
                case STATE:
                    project.state = ProjectState.ACTIVE;
                    break;
                case PERMISSIONS:
                    project.permissions = Collections.emptyMap();
                    break;
                case EXTERNAL_IDS:
                    project.externalIds = Collections.emptyMap();
                    break;
                case ADDITIONAL_DATA:
                    project.additionalData = Collections.emptyMap();
                    break;
                case ATTACHMENTS:
                    project.attachments = Collections.emptySet();
                    break;
                case PROJECT_TYPE:
                    project.projectType = ProjectType.INTERNAL;
                    break;
                case MODERATORS:
                    project.moderators = ImmutableSet.of("moderator@sw360.org");
                    break;
                case CONTRIBUTORS:
                    project.contributors= ImmutableSet.of("contributor1@sw360.org","contributor2@sw360.org");
                    break;
                case SECURITY_RESPONSIBLES:
                    project.securityResponsibles = ImmutableSet.of("securityresponsible1@sw360.org","securityresponsible2@sw360.org");
                    break;
                case VISBILITY:
                    project.visbility = Visibility.EVERYONE;
                    break;
                case LINKED_PROJECTS:
                    project.linkedProjects = Collections.emptyMap();
                    break;
                case RELEASE_ID_TO_USAGE:
                    project.releaseIdToUsage = Collections.emptyMap();
                    break;
                case RELEASE_CLEARING_STATE_SUMMARY:
                    project.releaseClearingStateSummary = new ReleaseClearingStateSummary();
                    break;
                case CLEARING_STATE:
                    project.clearingState = ProjectClearingState.OPEN;
                    break;
                case ROLES:
                    project.roles = Collections.emptyMap();
                    break;
                case ENABLE_SVM:
                    project.enableSvm = true;
                    break;
                case ENABLE_VULNERABILITIES_DISPLAY:
                    project.enableVulnerabilitiesDisplay = true;
                    break;
                case TODOS:
                    project.todos = Collections.emptySet();
                    break;
                case LINKED_OBLIGATIONS:
                    project.linkedObligations = Collections.emptyMap();
                    break;
                default: //most fields are string
                    project.setFieldValue(renderedField, "asd");
                    break;
            }
        }

        ProjectSummary.setSummaryFields(project, copy);

        for (Project._Fields renderedField : PROJECT_RENDERED_FIELDS) {
            assertNotNull(copy.getFieldValue(renderedField));
        }
    }
}