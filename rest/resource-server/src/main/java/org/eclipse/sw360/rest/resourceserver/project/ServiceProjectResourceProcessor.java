/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.project;

import org.eclipse.sw360.datahandler.services.projects.Project;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

/**
 * Adds the project self link for service-api projects and fills the boolean
 * flags that Thrift always emitted as {@code false}.
 */
@Component
class ServiceProjectResourceProcessor implements RepresentationModelProcessor<EntityModel<Project>> {

    @Override
    public EntityModel<Project> process(EntityModel<Project> resource) {
        Object content = ((EntityModel<?>) resource).getContent();
        if (!(content instanceof Project project)) {
            return resource;
        }
        applyWireDefaults(project);
        Link selfLink = linkTo(ProjectController.class)
                .slash("api" + ProjectController.PROJECTS_URL + "/" + project.getId()).withSelfRel();
        resource.add(selfLink);
        return resource;
    }

    /**
     * Thrift {@code bool} fields are primitives, so an unset flag was written
     * as {@code false}. Service-api flags are nullable and would otherwise
     * disappear.
     */
    static void applyWireDefaults(Project project) {
        if (project.getEnableSvm() == null) {
            project.setEnableSvm(Boolean.FALSE);
        }
        if (project.getEnableVulnerabilitiesDisplay() == null) {
            project.setEnableVulnerabilitiesDisplay(Boolean.FALSE);
        }
        if (project.getConsiderReleasesFromExternalList() == null) {
            project.setConsiderReleasesFromExternalList(Boolean.FALSE);
        }
    }
}
