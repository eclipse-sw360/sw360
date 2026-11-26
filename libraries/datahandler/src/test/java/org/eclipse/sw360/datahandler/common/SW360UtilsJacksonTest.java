/*
 * Copyright Siemens AG, 2026.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.common;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SW360UtilsJacksonTest {
    private final ObjectMapper legacy = new ObjectMapper();

    @Test
    void shouldPreserveThriftRelationshipJson() throws Exception {
        ProjectReleaseRelationship relation = new ProjectReleaseRelationship(
                ReleaseRelationship.CONTAINED, MainlineState.MAINLINE).setComment("example");
        JsonNode actual = legacy.readTree(SW360Utils.fieldValueAsString(Map.of("release", relation)));
        JsonNode expected = legacy.readTree("""
                {"release":{"releaseRelation":"Contained","mainlineState":"Mainline",
                "comment":"example","createdBy":"","createdOn":""}}
                """);

        assertEquals(expected, actual);
    }

    @Test
    void shouldDeduplicateOnlyValidJsonArrays() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("valid", "[\"pkg:a\",\"pkg:b\",\"pkg:a\"]");
        values.put("invalid", "[not-json");
        values.put("single", "plain");

        SW360Utils.deduplicateMultiValueExternalIds(values);

        assertEquals("[\"pkg:a\",\"pkg:b\"]", values.get("valid"));
        assertEquals("[not-json", values.get("invalid"));
        assertEquals("plain", values.get("single"));
    }

    @Test
    void shouldReadReleaseIdsFromDependencyNetwork() {
        Project project = new Project().setReleaseRelationNetwork(
                "[{\"releaseId\":\"release-id\",\"releaseLink\":[]}]");
        assertEquals(Set.of("release-id"), SW360Utils.getReleaseIdsLinkedWithProject(project));
    }
}
