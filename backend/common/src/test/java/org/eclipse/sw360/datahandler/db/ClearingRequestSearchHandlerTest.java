/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ClearingRequestSearchHandlerTest {

    private static List<String> mapSortColumnDirect(int sortColumnNumber) {
        return switch (sortColumnNumber) {
            case 0 -> List.of("clearingState_sort", "-timestamp");
            case 1 -> List.of("projectBU_sort", "-timestamp");
            case 2 -> List.of("requestingUser_sort", "-timestamp");
            case 3 -> List.of("clearingTeam_sort", "-timestamp");
            case -1 -> List.of("timestamp");
            default -> List.of(SCORE_SORTING_FIELD);
        };
    }

    @Test
    void byClearingState_shouldReturnStateWithTimestampTiebreaker() {
        assertEquals(List.of("clearingState_sort", "-timestamp"), mapSortColumnDirect(0));
    }

    @Test
    void byProjectBu_shouldReturnProjectBuWithTimestampTiebreaker() {
        assertEquals(List.of("projectBU_sort", "-timestamp"), mapSortColumnDirect(1));
    }

    @Test
    void byRequestingUser_shouldReturnUserWithTimestampTiebreaker() {
        assertEquals(List.of("requestingUser_sort", "-timestamp"), mapSortColumnDirect(2));
    }

    @Test
    void byClearingTeam_shouldReturnTeamWithTimestampTiebreaker() {
        assertEquals(List.of("clearingTeam_sort", "-timestamp"), mapSortColumnDirect(3));
    }

    @Test
    void byTimestamp_shouldReturnOnlyTimestamp() {
        assertEquals(List.of("timestamp"), mapSortColumnDirect(-1));
    }

    @Test
    void unknownColumn_shouldDefaultToScore() {
        assertEquals(List.of(SCORE_SORTING_FIELD), mapSortColumnDirect(999));
    }
}
