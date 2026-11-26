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

package org.eclipse.sw360.rest.resourceserver.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.SPDX_DOCUMENT_ENABLED;

class JacksonCustomizationsTest {

    private final JsonMapper mapper = createMapper();

    private static JsonMapper createMapper() {
        JsonMapper.Builder builder = JsonMapper.builder();
        new JacksonCustomizations().sw360JsonMapperCustomizer().customize(builder);
        return builder.build();
    }

    @Test
    void shouldApplyThriftMixinsToJackson3Responses() {
        Vendor vendor = new Vendor().setFullname("Example").setRevision("internal-revision");

        JsonNode json = mapper.readTree(mapper.writeValueAsString(vendor));

        assertThat(json.get("fullName").asText()).isEqualTo("Example");
        assertThat(json.has("fullname")).isFalse();
        assertThat(json.has("revision")).isFalse();
        assertThat(json.has("fullnameIsSet")).isFalse();
    }

    @Test
    void shouldPreserveRepresentativeThriftResponseContent() throws Exception {
        List<Object> entities = List.of(
                new Vendor(),
                new Vendor().setId("vendor-id").setFullname("Example").setShortname("Example")
                        .setRevision("internal"),
                new Component(),
                new Component().setId("component-id").setName("Example").setComponentType(ComponentType.OSS),
                new Release(),
                new Release().setId("release-id").setName("Example").setVersion("1.0"),
                new License(),
                new License().setId("license-id").setFullname("Example License").setShortname("EXAMPLE"),
                new Project(),
                new Project().setId("project-id").setName("Example").setVersion("1.0"),
                new User(),
                new User().setEmail("user@example.org").setFullname("Example User"));
        List<String> expected = List.of(
                "{\"type\":\"vendor\"}",
                "{\"type\":\"vendor\",\"shortName\":\"Example\",\"fullName\":\"Example\"}",
                "{\"type\":\"component\",\"visbility\":\"EVERYONE\"}",
                "{\"id\":\"component-id\",\"type\":\"component\",\"name\":\"Example\","
                        + "\"componentType\":\"OSS\",\"visbility\":\"EVERYONE\"}",
                "{\"type\":\"release\"}",
                "{\"id\":\"release-id\",\"type\":\"release\",\"name\":\"Example\",\"version\":\"1.0\"}",
                "{\"type\":\"license\",\"OSIApproved\":\"NA\",\"FSFLibre\":\"NA\",\"checked\":true}",
                "{\"type\":\"license\",\"OSIApproved\":\"NA\",\"FSFLibre\":\"NA\",\"checked\":true,"
                        + "\"shortName\":\"EXAMPLE\",\"fullName\":\"Example License\"}",
                "{\"type\":\"project\",\"state\":\"ACTIVE\",\"securityResponsibles\":[],\"enableSvm\":false,"
                        + "\"considerReleasesFromExternalList\":false,\"enableVulnerabilitiesDisplay\":false,"
                        + "\"projectType\":\"PRODUCT\",\"visibility\":\"EVERYONE\"}",
                "{\"type\":\"project\",\"name\":\"Example\",\"version\":\"1.0\",\"state\":\"ACTIVE\","
                        + "\"securityResponsibles\":[],\"enableSvm\":false,\"considerReleasesFromExternalList\":false,"
                        + "\"enableVulnerabilitiesDisplay\":false,\"id\":\"project-id\",\"projectType\":\"PRODUCT\","
                        + "\"visibility\":\"EVERYONE\"}",
                "{\"type\":\"user\",\"wantsMailNotification\":false,\"deactivated\":false}",
                "{\"type\":\"user\",\"email\":\"user@example.org\",\"wantsMailNotification\":false,"
                        + "\"deactivated\":false,\"fullName\":\"Example User\"}");

        for (int i = 0; i < entities.size(); i++) {
            assertThat(mapper.readTree(mapper.writeValueAsString(entities.get(i))))
                    .as("Jackson response for %s", entities.get(i).getClass().getSimpleName())
                    .isEqualTo(mapper.readTree(expected.get(i)));
        }
    }

    @Test
    void shouldPreserveProjectRelationshipResponseContent() throws Exception {
        Project project = new Project().setId("project-id").setName("Example")
                .setLinkedProjects(Map.of("child-id",
                        new ProjectProjectRelationship(ProjectRelationship.CONTAINED).setEnableSvm(true)))
                .setReleaseIdToUsage(Map.of("release-id",
                        new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE)
                                .setComment("example")));

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        try {
            JsonNode expected = mapper.readTree("""
                    {"type":"project","name":"Example","state":"ACTIVE","securityResponsibles":[],
                     "enableSvm":false,"considerReleasesFromExternalList":false,
                     "enableVulnerabilitiesDisplay":false,"id":"project-id","projectType":"PRODUCT",
                     "visibility":"EVERYONE",
                     "linkedProjects":[{"project":"http://localhost/api/projects/child-id",
                       "enableSvm":"true","relation":"CONTAINED"}],
                     "linkedReleases":[{"createdBy":"","release":"http://localhost/api/releases/release-id",
                       "mainlineState":"MAINLINE","comment":"example","projectMainlineState":"MAINLINE",
                       "createdOn":"","relation":"CONTAINED"}]}
                    """);
            JsonNode actual = mapper.readTree(mapper.writeValueAsString(project));
            assertThat(actual).isEqualTo(expected);
            assertThat(actual.get("linkedProjects").isArray()).isTrue();
            assertThat(actual.get("linkedReleases").isArray()).isTrue();
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void shouldSanitizeStringsExceptKnownConfigurationKeys() {
        Input input = mapper.readValue(
                "{\"name\":\"<script>test</script>\",\"" + SPDX_DOCUMENT_ENABLED + "\":\"<script>test</script>\"}",
                Input.class);

        assertThat(input.name).isEqualTo("&lt;script&gt;test&lt;/script&gt;");
        assertThat(input.configValue).isEqualTo("<script>test</script>");
    }

    @Test
    void shouldPreserveErrorResponseContent() throws Exception {
        RestExceptionHandler.ErrorMessage error =
                new RestExceptionHandler.ErrorMessage(400, "Bad Request", "Invalid request");
        error.setTimestamp(Instant.parse("2026-01-02T03:04:05Z"));

        assertThat(mapper.readTree(mapper.writeValueAsString(error)))
                .isEqualTo(mapper.readTree("""
                        {"timestamp":"2026-01-02T03:04:05Z","status":400,"error":"Bad Request",
                         "message":"Invalid request"}
                        """));
    }

    static class Input {
        public String name;

        @JsonProperty(SPDX_DOCUMENT_ENABLED)
        public String configValue;
    }
}
