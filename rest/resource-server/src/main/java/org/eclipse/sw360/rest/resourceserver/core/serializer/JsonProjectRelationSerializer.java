/*
 * Copyright Siemens AG, 2018.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.core.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship;
import org.eclipse.sw360.rest.resourceserver.project.ProjectController;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@Component
public class JsonProjectRelationSerializer extends JsonSerializer<Map<String, ProjectRelationship>> {

    @Override
    public void serialize(Map<String, ProjectRelationship> projectRelationMap, JsonGenerator gen, SerializerProvider provider)
            throws IOException, JsonProcessingException {

        List<Map<String, String>> linkedProjects = new ArrayList<>();
        for (Map.Entry<String, ProjectRelationship> projectRelation : projectRelationMap.entrySet()) {
            String projectLink = linkTo(ProjectController.class).slash("api" +
                    ProjectController.PROJECTS_URL + "/" + projectRelation.getKey()).withSelfRel().getHref();

            Map<String, String> linkedProject = new HashMap<>();
            linkedProject.put("relation", projectRelation.getValue().name());
            linkedProject.put("project", projectLink);
            linkedProjects.add(linkedProject);

        }
        gen.writeObject(linkedProjects);
    }
}
