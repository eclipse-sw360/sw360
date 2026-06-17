/*
 * Copyright Siemens AG, 2018,2026.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.core.serializer;

import org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship;
import org.eclipse.sw360.rest.resourceserver.project.ProjectController;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@Component
public class Json3ProjectRelationSerializer extends ValueSerializer<Map<String, ProjectProjectRelationship>> {

    @Override
    public void serialize(Map<String, ProjectProjectRelationship> projectRelationMap,
                          JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        List<Map<String, String>> linkedProjects = new ArrayList<>();
        for (Map.Entry<String, ProjectProjectRelationship> projectRelation : projectRelationMap.entrySet()) {
            String projectLink = linkTo(ProjectController.class).slash("api"
                    + ProjectController.PROJECTS_URL + "/" + projectRelation.getKey()).withSelfRel().getHref();

            Map<String, String> linkedProject = new HashMap<>();
            linkedProject.put("relation", projectRelation.getValue().getProjectRelationship().name());
            linkedProject.put("enableSvm", String.valueOf(projectRelation.getValue().isEnableSvm()));
            linkedProject.put("project", projectLink);
            linkedProjects.add(linkedProject);

        }
        gen.writePOJO(linkedProjects);
    }
}
