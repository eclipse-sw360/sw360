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
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.services.common.ProjectReleaseRelationship;
import org.eclipse.sw360.rest.resourceserver.project.ProjectController;
import org.eclipse.sw360.rest.resourceserver.release.ReleaseController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

public class JsonServiceReleaseRelationSerializer extends JsonSerializer<Map<String, ProjectReleaseRelationship>> {

    @Override
    public void serialize(Map<String, ProjectReleaseRelationship> releaseRelationMap, JsonGenerator gen,
            SerializerProvider provider) throws IOException {
        List<Map<String, String>> linkedReleases = new ArrayList<>();
        if (releaseRelationMap != null) {
            for (Map.Entry<String, ProjectReleaseRelationship> releaseRelation : releaseRelationMap.entrySet()) {
                String releaseLink = linkTo(ProjectController.class).slash("api"
                        + ReleaseController.RELEASES_URL + "/" + releaseRelation.getKey()).withSelfRel().getHref();
                ProjectReleaseRelationship relation = releaseRelation.getValue();
                Map<String, String> linkedRelease = new HashMap<>();
                linkedRelease.put("relation", relation == null || relation.getReleaseRelation() == null
                        ? "" : relation.getReleaseRelation().name());
                String mainline = relation == null || relation.getMainlineState() == null
                        ? "" : relation.getMainlineState().name();
                linkedRelease.put("mainlineState", mainline);
                linkedRelease.put("projectMainlineState", mainline);
                linkedRelease.put("comment", CommonUtils.nullToEmptyString(relation == null ? null : relation.getComment()));
                linkedRelease.put("createdBy", CommonUtils.nullToEmptyString(relation == null ? null : relation.getCreatedBy()));
                linkedRelease.put("createdOn", CommonUtils.nullToEmptyString(relation == null ? null : relation.getCreatedOn()));
                linkedRelease.put("release", releaseLink);
                linkedReleases.add(linkedRelease);
            }
        }
        gen.writeObject(linkedReleases);
    }
}
