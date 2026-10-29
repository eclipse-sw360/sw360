/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.core.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.eclipse.sw360.datahandler.services.projects.ProjectProjectRelationship;
import org.eclipse.sw360.rest.resourceserver.project.ProjectController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

public class JsonServiceProjectRelationSerializer extends JsonSerializer<Map<String, ProjectProjectRelationship>> {

    @Override
    public void serialize(Map<String, ProjectProjectRelationship> projectRelationMap, JsonGenerator gen,
            SerializerProvider provider) throws IOException {
        List<Map<String, String>> linkedProjects = new ArrayList<>();
        if (projectRelationMap != null) {
            for (Map.Entry<String, ProjectProjectRelationship> projectRelation : projectRelationMap.entrySet()) {
                String projectLink = linkTo(ProjectController.class).slash("api"
                        + ProjectController.PROJECTS_URL + "/" + projectRelation.getKey()).withSelfRel().getHref();
                Map<String, String> linkedProject = new HashMap<>();
                ProjectProjectRelationship relation = projectRelation.getValue();
                linkedProject.put("relation", relation == null || relation.getProjectRelationship() == null
                        ? "" : relation.getProjectRelationship().name());
                boolean enableSvm = relation == null || !Boolean.FALSE.equals(relation.getEnableSvm());
                linkedProject.put("enableSvm", String.valueOf(enableSvm));
                linkedProject.put("project", projectLink);
                linkedProjects.add(linkedProject);
            }
        }
        gen.writeObject(linkedProjects);
    }
}
