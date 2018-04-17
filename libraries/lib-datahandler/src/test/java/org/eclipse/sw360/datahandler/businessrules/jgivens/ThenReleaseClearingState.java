/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.businessrules.jgivens;

import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStateSummary;
import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ExpectedScenarioState;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author daniele.fognini@tngtech.com
 */
public class ThenReleaseClearingState extends Stage<ThenReleaseClearingState> {

    @ExpectedScenarioState
    ReleaseClearingStateSummary releaseClearingStateSummary;

    public ThenReleaseClearingState new_releases_should_be(int i) {
        assertThat(releaseClearingStateSummary.getNewRelease(), is(i));
        return self();
    }

    public ThenReleaseClearingState under_clearing_should_be(int i) {
        assertThat(releaseClearingStateSummary.getUnderClearing(), is(i));
        return self();
    }

    public ThenReleaseClearingState under_clearing_by_project_team_should_be(int i) {
        assertThat(releaseClearingStateSummary.getUnderClearingByProjectTeam(), is(i));
        return self();
    }

    public ThenReleaseClearingState report_available_should_be(int i) {
        assertThat(releaseClearingStateSummary.getReportAvailable(), is(i));
        return self();
    }

    public ThenReleaseClearingState approved_should_be(int i) {
        assertThat(releaseClearingStateSummary.getApproved(), is(i));
        return self();
    }
}
