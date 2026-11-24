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

package org.eclipse.sw360.datahandler.db;

import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseNode;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class DatabaseJacksonCompatibilityTest {
    private final ObjectMapper mapper = JsonMapper.builderWithJackson2Defaults().build();

    @Test
    void shouldPreserveChangelogJson() throws Exception {
        List<Object> values = List.of(
                new Attachment().setAttachmentContentId("id").setFilename("example.txt"),
                new Attachment().setAttachmentContentId("id").setFilename("example.txt").setSha1("sha"),
                new Vendor().setId("id").setFullname("Example").setShortname("EX"),
                new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE)
                        .setComment("example"),
                Map.of("name", "example", "values", List.of("a", "b")));

        List<String> expected = List.of(
                """
                {"attachmentContentId":"id","filename":"example.txt","setSuperAttachmentId":false,
                 "setProjectAttachmentUsage":false,"setSuperAttachmentFilename":false}
                """,
                """
                {"attachmentContentId":"id","filename":"example.txt","sha1":"sha","setSuperAttachmentId":false,
                 "setProjectAttachmentUsage":false,"setSuperAttachmentFilename":false}
                """,
                """
                {"id":"id","type":"vendor","shortname":"EX","fullname":"Example"}
                """,
                """
                {"releaseRelation":"CONTAINED","mainlineState":"MAINLINE","comment":"example"}
                """,
                """
                {"name":"example","values":["a","b"]}
                """);
        for (int i = 0; i < values.size(); i++) {
            assertEquals(mapper.readTree(expected.get(i)),
                    mapper.readTree(DatabaseHandlerUtil.convertObjectToJson(values.get(i))),
                    values.get(i).getClass().getSimpleName());
        }
    }

    @Test
    void shouldPreserveNestedDependencyNetworkInterpretation() throws Exception {
        String json = """
                [{"releaseId":"root","releaseRelationship":"CONTAINED","mainlineState":"MAINLINE",
                  "comment":"example","releaseLink":[{"releaseId":"child","releaseLink":[]}]}]
                """;
        List<ReleaseNode> expected = List.of(new ReleaseNode().setReleaseId("root")
                .setReleaseRelationship("CONTAINED").setMainlineState("MAINLINE").setComment("example")
                .setReleaseLink(List.of(new ReleaseNode().setReleaseId("child").setReleaseLink(List.of()))));
        List<ReleaseNode> actual = mapper.readValue(json, new TypeReference<List<ReleaseNode>>() {});

        assertEquals(expected, actual);
        assertEquals("child", actual.getFirst().getReleaseLink().getFirst().getReleaseId());
    }

    @Test
    void shouldLeaveMalformedDependencyNetworkEmpty() throws Exception {
        ProjectDatabaseHandler handler = mock(ProjectDatabaseHandler.class, org.mockito.Mockito.CALLS_REAL_METHODS);
        User user = new User();
        doReturn(new Project().setReleaseRelationNetwork("{")).when(handler).getProjectById("project", user);

        assertEquals(List.of(), handler.getLinkedReleasesInDependencyNetworkOfProject("project", user));
    }
}
