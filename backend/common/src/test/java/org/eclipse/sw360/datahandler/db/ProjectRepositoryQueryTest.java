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

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectRepositoryQueryTest {

    private DatabaseConnectorCloudant db;
    private ProjectRepository repository;

    @BeforeEach
    void setUp() {
        db = mock(DatabaseConnectorCloudant.class);
        when(db.getDbName()).thenReturn("sw360db");
        repository = new ProjectRepository(db);
    }

    @Test
    void getQueryFromRestrictions_shouldUsePrefixRegexForSingleVersion() {
        Map<String, Set<String>> restrictions = Map.of("version", Set.of("2.5"));

        Map<String, Object> query = repository.getQueryFromRestrictions(restrictions);

        assertThat(query).isNotNull();
        assertThat(query).containsKey("$and");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> andList = (List<Map<String, Object>>) query.get("$and");
        assertThat(andList).hasSize(1);
        Map<String, Object> versionCondition = andList.getFirst();
        assertThat(versionCondition).containsKey("version");
        @SuppressWarnings("unchecked")
        Map<String, Object> regexMap = (Map<String, Object>) versionCondition.get("version");
        assertThat(regexMap).containsEntry("$regex", "(?i)^2\\.5");
    }

    @Test
    void getQueryFromRestrictions_shouldUseOrForMultipleVersions() {
        Map<String, Set<String>> restrictions = Map.of("version", Set.of("2.5", "3.0"));

        Map<String, Object> query = repository.getQueryFromRestrictions(restrictions);

        assertThat(query).isNotNull();
        assertThat(query).containsKey("$and");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> andList = (List<Map<String, Object>>) query.get("$and");
        assertThat(andList).hasSize(1);

        Map<String, Object> orCondition = andList.getFirst();
        assertThat(orCondition).containsKey("$or");
    }

    @Test
    void getQueryFromRestrictions_shouldUseEqIgnoreCaseForName() {
        Map<String, Set<String>> restrictions = Map.of("name", Set.of("MyProject"));

        Map<String, Object> query = repository.getQueryFromRestrictions(restrictions);

        assertThat(query).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> andList = (List<Map<String, Object>>) query.get("$and");
        assertThat(andList).hasSize(1);
        Map<String, Object> nameCondition = andList.getFirst();
        assertThat(nameCondition).containsKey("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> regexMap = (Map<String, Object>) nameCondition.get("name");
        assertThat(regexMap).containsEntry("$regex", "(?i)^MyProject$");
    }
}
