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
package org.eclipse.sw360.datahandler.businessrules;

import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.components.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author daniele.fognini@tngtech.com
 */
public class ReleaseClearingStateSummaryComputer {
    public static ReleaseClearingStateSummary computeReleaseClearingStateSummary(List<Release> releases, String clearingTeam) {
        ReleaseClearingStateSummary summary = new ReleaseClearingStateSummary(0, 0, 0, 0, 0);

        if (releases == null) {
            return summary;
        }

        for (Release release : releases) {
            // once there are other clearing tools available, this filtering need to differentiate between tools
            Set<ExternalToolRequest> fossologyStatuses = SW360Utils.getExternalToolRequestsForTool(release,
                    ExternalTool.FOSSOLOGY);
            Set<ExternalToolRequest> clearingTeamETRSet = fossologyStatuses.stream()
                    .filter(etr -> clearingTeam.equals(etr.getToolUserGroup())).collect(Collectors.toSet());
            ExternalToolRequest clearingTeamETR = clearingTeamETRSet.size() == 1 ? clearingTeamETRSet.iterator().next()
                    : null;

            ViewedState globalState = getGlobalState(release.getClearingState());
            ViewedState myTeamState = getStateOfFossology(clearingTeamETR);
            ViewedState otherTeamState = getBestStateOfFossologyForOtherTeams(clearingTeam, fossologyStatuses);

            addReleaseWithStates(summary, globalState, myTeamState, otherTeamState);
        }

        return summary;
    }

    static Void addReleaseWithStates(ReleaseClearingStateSummary summary, ViewedState globalState, ViewedState myTeamState, ViewedState otherTeamState) {
        // if the release has its clearing state field set to REPORT_AVAILABLE or APPROVED use it and ignore the rest
        // otherwise, look at what fossology has to say
        // if all else fails, default to NEW
        if (globalState.present() && globalState.compareTo(ViewedState.REPORT_AVAILABLE) >= 0) {
            return addReleaseState(summary, globalState);
        }
        if (myTeamState.present()) { // if my team has something to say ignore the rest
            return addReleaseStateTeamDependent(summary, myTeamState, ViewedState.CLEARING_MY_TEAM);
        }
        if (otherTeamState.present()) { // now consider other teams
            return addReleaseStateTeamDependent(summary, otherTeamState, ViewedState.CLEARING);
        }
        return addReleaseState(summary, ViewedState.NEW); // nobody said anything: it is new
    }

    static ViewedState getGlobalState(ClearingState clearingState) {
        if (clearingState != null) {
            switch (clearingState) {
                case NEW_CLEARING:
                    return ViewedState.NONE; // the default state NEW is like having no global state
                case SENT_TO_FOSSOLOGY:
                case UNDER_CLEARING:
                    return ViewedState.CLEARING;
                case REPORT_AVAILABLE:
                    return ViewedState.REPORT_AVAILABLE;
                case APPROVED:
                    return ViewedState.APPROVED;
            }
        }
        return ViewedState.NONE;
    }

    static ViewedState getStateOfFossology(ExternalToolRequest etr) {
        if (etr != null) {
            ExternalToolWorkflowStatus workflowStatus = etr.getExternalToolWorkflowStatus();
            if (workflowStatus != null) {
                switch (workflowStatus) {
                case NOT_SENT:
                case UPLOADING:
                    return ViewedState.NEW;
                case SENT:
                    ExternalToolStatus externalToolStatus = etr.getExternalToolStatus();
                    if (externalToolStatus != null) {
                        switch (externalToolStatus) {
                        case OPEN:
                        case IN_PROGRESS:
                            return ViewedState.CLEARING;
                        case CLOSED:
                        case RESULT_AVAILABLE:
                            return ViewedState.REPORT_AVAILABLE;
                        case REJECTED:
                        default:
                            break;
                        }
                    }
                    break;
                case ACCESS_DENIED:
                case NOT_FOUND:
                case CONNECTION_TIMEOUT:
                case CONNECTION_FAILED:
                case SERVER_ERROR:
                default:
                    break;
                }
            }
        }
        return ViewedState.NONE;
    }

    static Void addReleaseState(ReleaseClearingStateSummary summary, ViewedState state) {
        if (state == null || !state.present()) {
            throwBadState(state);
        }
        switch (state) {
            case NONE:
                throwBadState(state);
            case NEW:
                summary.newRelease++;
                break;
            case CLEARING:
                summary.underClearing++;
                break;
            case CLEARING_MY_TEAM:
                summary.underClearingByProjectTeam++;
                break;
            case REPORT_AVAILABLE:
                summary.reportAvailable++;
                break;
            case APPROVED:
                summary.approved++;
                break;
            default:
                break;
        }
        return null;
    }

    static Void addReleaseStateTeamDependent(ReleaseClearingStateSummary summary, ViewedState state, ViewedState stateWhenCleared) {
        if (state == null || !state.present()) {
            throwBadState(state);
        }
        switch (state) {
            case NONE:
                return throwBadState(state);
            case CLEARING:
            case CLEARING_MY_TEAM:
                return addReleaseState(summary, stateWhenCleared);
            case NEW:
            case REPORT_AVAILABLE:
            case APPROVED:
            default:
                return addReleaseState(summary, state);
        }
    }

    static ViewedState getBestStateOfFossologyForOtherTeams(String clearingTeam,
            Set<ExternalToolRequest> fossologyStatuses) {
        Optional<ViewedState> fossologyStatusesOfOtherTeamsOptional = fossologyStatuses.stream()
                .filter(etr -> !clearingTeam.equals(etr.getToolUserGroup()))
                .map(ReleaseClearingStateSummaryComputer::getStateOfFossology)
                .collect(Collectors.maxBy(Comparator.naturalOrder()));

        return fossologyStatusesOfOtherTeamsOptional.orElse(ViewedState.NONE);
    }

    static Void throwBadState(ViewedState state) {
        throw new IllegalStateException("down here the state must be present, but it was " + state);
    }

    enum ViewedState {
        NONE(false), NEW, CLEARING, CLEARING_MY_TEAM, REPORT_AVAILABLE, APPROVED;

        private boolean present;

        ViewedState(boolean present) {
            this.present = present;
        }

        ViewedState() {
            this(true);
        }

        private boolean present() {
            return present;
        }
    }
}
