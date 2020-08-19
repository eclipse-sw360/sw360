/*
 * Copyright Siemens AG, 2018.
 * Part of the SW360 Portal Project.
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

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.rest.resourceserver.project.ProjectController;
import org.eclipse.sw360.rest.resourceserver.release.ReleaseController;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@Component
public class JsonReleaseRelationSerializer extends JsonSerializer<Map<String, ProjectReleaseRelationship>> {

    @Override
    public void serialize(Map<String, ProjectReleaseRelationship> releaseRelationMap, JsonGenerator gen, SerializerProvider provider)
            throws IOException {

        List<Map<String, String>> linkedReleases = new ArrayList<>();
        for (Map.Entry<String, ProjectReleaseRelationship> releaseRelation : releaseRelationMap.entrySet()) {
            String releaseLink = linkTo(ProjectController.class).slash("api" +
                    ReleaseController.RELEASES_URL + "/" + releaseRelation.getKey()).withSelfRel().getHref();

            Map<String, String> linkedRelease = new HashMap<>();
            ProjectReleaseRelationship projectReleaseRelationship = releaseRelation.getValue();
            linkedRelease.put("relation", projectReleaseRelationship.getReleaseRelation().name());
            linkedRelease.put("mainlineState", projectReleaseRelationship.getMainlineState().name());
            linkedRelease.put("comment", CommonUtils.nullToEmptyString(projectReleaseRelationship.getComment()));
            linkedRelease.put("createdBy", CommonUtils.nullToEmptyString(projectReleaseRelationship.getCreatedBy()));
            linkedRelease.put("createdOn", CommonUtils.nullToEmptyString(projectReleaseRelationship.getCreatedOn()));
            linkedRelease.put("release", releaseLink);
            linkedReleases.add(linkedRelease);
        }
        gen.writeObject(linkedReleases);
    }
}
