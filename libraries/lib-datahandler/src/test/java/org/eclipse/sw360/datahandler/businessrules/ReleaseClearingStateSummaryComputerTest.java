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

package org.eclipse.sw360.datahandler.businessrules;

import org.eclipse.sw360.datahandler.businessrules.jgivens.GivenReleasesWithFossologyStatus;
import org.eclipse.sw360.datahandler.businessrules.jgivens.ThenReleaseClearingState;
import org.eclipse.sw360.datahandler.businessrules.jgivens.WhenComputeClearingState;
import org.eclipse.sw360.datahandler.thrift.components.ClearingState;
import org.eclipse.sw360.datahandler.thrift.components.FossologyStatus;
import com.tngtech.jgiven.junit.ScenarioTest;
import org.junit.Test;

/**
 * @author daniele.fognini@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class ReleaseClearingStateSummaryComputerTest extends ScenarioTest<GivenReleasesWithFossologyStatus, WhenComputeClearingState, ThenReleaseClearingState> {

    public static final String CLEARING_TEAM = "the project clearing team";
    public static final String ANOTHER_CLEARING_TEAM = "another clearing team";

    @Test
    public void test0() throws Exception {
        given()
                .a_new_release()
                .and()
                .a_new_release();

        when().the_clearing_state_is_computed_for(CLEARING_TEAM);

        then()
                .new_releases_should_be(2).and()
                .under_clearing_should_be(0).and()
                .under_clearing_by_project_team_should_be(0).and()
                .report_available_should_be(0);
    }

    @Test
    public void test1() throws Exception {
        given()
                .a_release_with_fossology_status_$_for_$_and_$_for_$(
                        FossologyStatus.SCANNING, CLEARING_TEAM,
                        FossologyStatus.SENT, ANOTHER_CLEARING_TEAM);

        when().the_clearing_state_is_computed_for(CLEARING_TEAM);

        then()
                .new_releases_should_be(0).and()
                .under_clearing_should_be(0).and()
                .under_clearing_by_project_team_should_be(1).and()
                .report_available_should_be(0);
    }


    @Test
    public void test2() throws Exception {
        given()
                .a_release_with_clearing_status_$_and_fossology_status_$_for_$_and_$_for_$(
                        ClearingState.REPORT_AVAILABLE,
                        FossologyStatus.SCANNING, CLEARING_TEAM,
                        FossologyStatus.REPORT_AVAILABLE, ANOTHER_CLEARING_TEAM)
                .and()
                .a_new_release();

        when().the_clearing_state_is_computed_for(CLEARING_TEAM);

        then()
                .new_releases_should_be(1).and()
                .under_clearing_should_be(0).and()
                .under_clearing_by_project_team_should_be(0).and()
                .report_available_should_be(1);

    }

    @Test
    public void test3() throws Exception {
        given()
                .a_release_with_clearing_status(ClearingState.NEW_CLEARING)
                .and()
                .a_release_with_clearing_status_$_and_fossology_status_$_for_$_and_$_for_$(
                        ClearingState.SENT_TO_FOSSOLOGY,
                        FossologyStatus.SCANNING, CLEARING_TEAM,
                        FossologyStatus.CLOSED, ANOTHER_CLEARING_TEAM);

        when().the_clearing_state_is_computed_for(CLEARING_TEAM);

        then()
                .new_releases_should_be(1).and()
                .under_clearing_should_be(0).and()
                .under_clearing_by_project_team_should_be(1).and()
                .report_available_should_be(0);

    }

    @Test
    public void test4() throws Exception {
        given()
                .a_release_with_clearing_status_$_and_fossology_status_$_for_$(
                        ClearingState.REPORT_AVAILABLE,
                        FossologyStatus.IN_PROGRESS, ANOTHER_CLEARING_TEAM)
                .and()
                .a_new_release();

        when().the_clearing_state_is_computed_for(CLEARING_TEAM);

        then()
                .new_releases_should_be(1).and()
                .under_clearing_should_be(0).and()
                .under_clearing_by_project_team_should_be(0).and()
                .report_available_should_be(1);

    }

    @Test
    public void test5() throws Exception {
        given()
                .a_release_with_clearing_status_$_and_fossology_status_$_for_$(
                        ClearingState.REPORT_AVAILABLE,
                        FossologyStatus.SENT, ANOTHER_CLEARING_TEAM)
                .and()
                .a_release_with_clearing_status_$_and_fossology_status_$_for_$_and_$_for_$(
                        ClearingState.REPORT_AVAILABLE,
                        FossologyStatus.SCANNING, CLEARING_TEAM,
                        FossologyStatus.SENT, ANOTHER_CLEARING_TEAM)
                .and()
                .a_new_release();

        when().the_clearing_state_is_computed_for(CLEARING_TEAM);

        then()
                .new_releases_should_be(1).and()
                .under_clearing_should_be(0).and()
                .under_clearing_by_project_team_should_be(0).and()
                .report_available_should_be(2);

    }

    @Test
    public void test6() throws Exception {
        given()
                .a_release_with_fossology_status_$_for_$_and_$_for_$_and_$_for_$(
                        FossologyStatus.IN_PROGRESS, CLEARING_TEAM,
                        FossologyStatus.REPORT_AVAILABLE, ANOTHER_CLEARING_TEAM,
                        FossologyStatus.OPEN, "yet " + ANOTHER_CLEARING_TEAM)
                .and()
                .a_new_release()
                .and()
                .a_release_with_clearing_status_$_and_fossology_status_$_for_$_and_$_for_$(
                        ClearingState.REPORT_AVAILABLE,
                        FossologyStatus.IN_PROGRESS, CLEARING_TEAM,
                        FossologyStatus.REPORT_AVAILABLE, ANOTHER_CLEARING_TEAM)
                .and()
                .a_new_release();

        when().the_clearing_state_is_computed_for(CLEARING_TEAM);

        then()
                .new_releases_should_be(2).and()
                .under_clearing_should_be(0).and()
                .under_clearing_by_project_team_should_be(1).and()
                .report_available_should_be(1);

    }

    @Test
    public void test7() throws Exception {
        given()
                .a_release_with_clearing_status_$_and_fossology_status_$_for_$(ClearingState.SENT_TO_FOSSOLOGY, FossologyStatus.OPEN, ANOTHER_CLEARING_TEAM);

        when().the_clearing_state_is_computed_for(CLEARING_TEAM);

        then()
                .new_releases_should_be(0).and()
                .under_clearing_should_be(1).and()
                .under_clearing_by_project_team_should_be(0).and()
                .report_available_should_be(0);

    }

    @Test
    public void test8() throws Exception {
        given()
                .a_release_with_clearing_status(ClearingState.NEW_CLEARING);

        when().the_clearing_state_is_computed_for(CLEARING_TEAM);

        then()
                .new_releases_should_be(1).and()
                .under_clearing_should_be(0).and()
                .under_clearing_by_project_team_should_be(0).and()
                .report_available_should_be(0);

    }


    @Test
    public void test9() throws Exception {
        given()
                .a_release_with_fossology_status_$_for_$(FossologyStatus.CLOSED, ANOTHER_CLEARING_TEAM);

        when().the_clearing_state_is_computed_for(CLEARING_TEAM);

        then()
                .new_releases_should_be(0).and()
                .under_clearing_should_be(0).and()
                .under_clearing_by_project_team_should_be(0).and()
                .report_available_should_be(1);

    }

    @Test
    public void test90() throws Exception {
        given()
                .a_release_with_fossology_status_$_for_$(FossologyStatus.OPEN, ANOTHER_CLEARING_TEAM);

        when().the_clearing_state_is_computed_for(CLEARING_TEAM);

        then()
                .new_releases_should_be(0).and()
                .under_clearing_should_be(1).and()
                .under_clearing_by_project_team_should_be(0).and()
                .report_available_should_be(0);

    }

    @Test
    public void the_clearing_moves_to_the_right_following_clearing_team_but_is_overwritten_by_global_clearing_state_above_under_clearing() {
        given().a_new_release();

        when().the_clearing_state_is_computed_for(CLEARING_TEAM);

        then().new_releases_should_be(1);

        when().the_release_is_sent_for_clearing_to(CLEARING_TEAM);
        then().new_releases_should_be(0).and().under_clearing_by_project_team_should_be(1).and().report_available_should_be(0).and().approved_should_be(0);

        when().the_release_is_sent_for_clearing_to(ANOTHER_CLEARING_TEAM);
        then().under_clearing_should_be(0).and().under_clearing_by_project_team_should_be(1).and().report_available_should_be(0).and().approved_should_be(0);

//        when().team_$_sets_fossology_status_to(CLEARING_TEAM, FossologyStatus.CLOSED);
//        then().under_clearing_should_be(0).and().under_clearing_by_project_team_should_be(0).and().report_available_should_be(1);
//
        when().the_release_clearing_state_is_set_to(ClearingState.REPORT_AVAILABLE);
        then().under_clearing_should_be(0).and().under_clearing_by_project_team_should_be(0).and().report_available_should_be(1).and().approved_should_be(0);

        when().the_release_clearing_state_is_set_to(ClearingState.APPROVED);
        then().under_clearing_should_be(0).and().under_clearing_by_project_team_should_be(0).and().report_available_should_be(0).and().approved_should_be(1);
    }
}