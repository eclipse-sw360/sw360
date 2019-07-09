/*
 * Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.businessrules.jgivens;

import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.Quoted;
import com.tngtech.jgiven.annotation.ScenarioState;

import org.eclipse.sw360.datahandler.TEnumToString;
import org.eclipse.sw360.datahandler.thrift.components.*;

import org.mockito.Mockito;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.mock;

/**
 * @author daniele.fognini@tngtech.com
 */
public class GivenReleasesWithExternalToolStatus extends Stage<GivenReleasesWithExternalToolStatus> {
    @ScenarioState
    public List<Release> releaseList = newArrayList();

    private Release release;

    private Set<ExternalToolRequest> externalToolRequests;

    public GivenReleasesWithExternalToolStatus a_release_with_external_tool_workflow_status_$_with_external_tool_status_$_for_$(
            @TEnumToString ExternalToolWorkflowStatus w1, @TEnumToString ExternalToolStatus s1, @Quoted String t1) {
        a_new_release();

        addExternalToolRequest(w1, s1, t1);

        return self();
    }

    public GivenReleasesWithExternalToolStatus a_release_with_external_tool_workflow_status_$_with_external_tool_status_$_for_$_and_$_with_$_for_$(
            @TEnumToString ExternalToolWorkflowStatus w1, @TEnumToString ExternalToolStatus s1, @Quoted String t1,
            @TEnumToString ExternalToolWorkflowStatus w2, @TEnumToString ExternalToolStatus s2, @Quoted String t2) {
        a_new_release();

        addExternalToolRequest(w1, s1, t1);
        addExternalToolRequest(w2, s2, t2);

        return self();
    }

    public GivenReleasesWithExternalToolStatus a_release_with_external_tool_workflow_status_$_with_external_tool_status_$_for_$_and_$_with_$_for_$_and_$_with_$_for_$(
            @TEnumToString ExternalToolWorkflowStatus w1, @TEnumToString ExternalToolStatus s1, @Quoted String t1,
            @TEnumToString ExternalToolWorkflowStatus w2, @TEnumToString ExternalToolStatus s2, @Quoted String t2,
            @TEnumToString ExternalToolWorkflowStatus w3, @TEnumToString ExternalToolStatus s3, @Quoted String t3) {
        a_new_release();

        addExternalToolRequest(w1, s1, t1);
        addExternalToolRequest(w2, s2, t2);
        addExternalToolRequest(w3, s3, t3);

        return self();
    }

    public GivenReleasesWithExternalToolStatus a_release_with_clearing_status_$_and_external_tool_workflow_status_$_with_external_tool_status_$_for_$_and_$_with_$_for_$(
            @TEnumToString ClearingState clearingState, @TEnumToString ExternalToolWorkflowStatus w1,
            @TEnumToString ExternalToolStatus s1, @Quoted String t1, @TEnumToString ExternalToolWorkflowStatus w2,
            @TEnumToString ExternalToolStatus s2, @Quoted String t2) {
        a_new_release();

        Mockito.when(release.getClearingState()).thenReturn(clearingState);

        addExternalToolRequest(w1, s1, t1);
        addExternalToolRequest(w2, s2, t2);

        return self();
    }

    public GivenReleasesWithExternalToolStatus a_release_with_clearing_status_$_and_external_tool_workflow_status_$_with_external_tool_status_$_for_$(
            @TEnumToString ClearingState clearingState, @TEnumToString ExternalToolWorkflowStatus w1,
            @TEnumToString ExternalToolStatus s1, @Quoted String t1) {
        a_new_release();

        Mockito.when(release.getClearingState()).thenReturn(clearingState);

        addExternalToolRequest(w1, s1, t1);

        return self();
    }

    public GivenReleasesWithExternalToolStatus a_new_release() {
        release = mock(Release.class);
        Mockito.when(release.getClearingState()).thenReturn(ClearingState.NEW_CLEARING);

        externalToolRequests = new HashSet<>();
        Mockito.when(release.getExternalToolRequests()).thenReturn(externalToolRequests);

        releaseList.add(release);
        return self();
    }

    public GivenReleasesWithExternalToolStatus a_release_with_clearing_status(@TEnumToString ClearingState clearingState) {
        a_new_release();
        Mockito.when(release.getClearingState()).thenReturn(clearingState);
        return self();
    }

    private void addExternalToolRequest(ExternalToolWorkflowStatus workflowStatus, ExternalToolStatus status,
            String team) {
        ExternalToolRequest etr = new ExternalToolRequest();

        etr.setExternalTool(ExternalTool.FOSSOLOGY);
        etr.setExternalToolWorkflowStatus(workflowStatus);
        etr.setExternalToolStatus(status);
        etr.setToolUserGroup(team);

        externalToolRequests.add(etr);
    }
}
