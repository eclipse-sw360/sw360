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

package org.eclipse.sw360.datahandler.permissions.jgivens;

import com.google.common.collect.ImmutableSet;
import org.eclipse.sw360.datahandler.TEnumToString;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.Quoted;
import com.tngtech.jgiven.annotation.ScenarioState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;

/**
 * @author johannes.najjar@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class GivenProject extends Stage<GivenProject> {
    @ScenarioState
    private Project project;

    public enum ProjectRole {
        CREATED_BY,
        LEAD_ARCHITECT,
        MODERATOR,
        CONTRIBUTOR,
        PROJECT_RESPONSIBLE
    }

    public GivenProject a_new_project() {
        project = mock(Project.class);
        Mockito.when(project.getVisbility()).thenReturn(Visibility.EVERYONE);
        return self();
    }

    public GivenProject a_project_with_$_$(ProjectRole role, @Quoted String m1){
        a_new_project();

        switch (role) {
            case CREATED_BY:
                Mockito.when(project.isSetCreatedBy()).thenReturn(true);
                Mockito.when(project.getCreatedBy()).thenReturn(m1);
                break;
            case LEAD_ARCHITECT:
                Mockito.when(project.isSetLeadArchitect()).thenReturn(true);
                Mockito.when(project.getLeadArchitect()).thenReturn(m1);
                break;
            case MODERATOR:
                Mockito.when(project.isSetModerators()).thenReturn(true);
                Mockito.when(project.getModerators()).thenReturn(ImmutableSet.of(m1));
                break;
            case CONTRIBUTOR:
                Mockito.when(project.isSetContributors()).thenReturn(true);
                Mockito.when(project.getContributors()).thenReturn(ImmutableSet.of(m1));
                break;
            case PROJECT_RESPONSIBLE:
                Mockito.when(project.isSetProjectResponsible()).thenReturn(true);
                Mockito.when(project.getProjectResponsible()).thenReturn(m1);
                break;
        }

        return self();
    }

    public GivenProject a_closed_project_with_$_$(ProjectRole role, @Quoted String user){
        a_project_with_$_$(role, user);
        Mockito.when(project.getClearingState()).thenReturn(ProjectClearingState.CLOSED);
        return self();
    }

    public GivenProject with_visibility_$_and_business_unit_$(@TEnumToString Visibility v1, @Quoted String b1) {
        Mockito.when(project.getVisbility()).thenReturn(v1);
        Mockito.when(project.getBusinessUnit()).thenReturn(b1);
        return self();
    }
}
