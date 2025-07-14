/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.sw360.datahandler.postgres.ComponentPG;
import org.eclipse.sw360.datahandler.postgres.ProjectPG;
import org.eclipse.sw360.datahandler.postgres.ReleasePG;
import org.eclipse.sw360.datahandler.postgres.VendorPG;
import org.eclipse.sw360.datahandler.postgres.VulnerabilityPG;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityDTO;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HalResource<T> extends EntityModel<T> {

    private Map<String, Object> embeddedMap;

    public HalResource(T content, Link... links) {
        super(content, Arrays.asList(links));
        if (content instanceof Project) {
            ((Project) content).setType(null);
        } else if (content instanceof Component) {
            ((Component) content).setType(null);
        } else if (content instanceof Release) {
            ((Release) content).setType(null);
        } else if (content instanceof User) {
            ((User) content).setType(null);
        } else if (content instanceof License) {
            ((License) content).setType(null);
        } else if (content instanceof Obligation) {
            ((Obligation) content).setType(null);
        } else if (content instanceof Vendor) {
            ((Vendor) content).setType(null);
        } else if (content instanceof Vulnerability) {
            ((Vulnerability) content).setType(null);
        } else if (content instanceof VulnerabilityDTO) {
            ((VulnerabilityDTO) content).setType(null);
        } else if (content instanceof ClearingRequest) {
            ((ClearingRequest) content).setType(null);
        } else if (content instanceof ModerationRequest) {
            ((ModerationRequest) content).setType(null);
        } else if (content instanceof ReleasePG) {
            ((ReleasePG) content).setComponent(null);
            ((ReleasePG) content).setVulnerabilities(null);
            ((ReleasePG) content).setVulnerabilityRelation(null);
        } else if (content instanceof ProjectPG) {
            ((ProjectPG) content).setComponents(List.of());
        } else if (content instanceof VulnerabilityPG) {
            ((VulnerabilityPG) content).setReleasesRelation(Set.of());
            ((VulnerabilityPG) content).setReleases(null);
        } else if (content instanceof VendorPG) {
            ((VendorPG) content).setComponents(Set.of());
        } else if (content instanceof ComponentPG) {
            ((ComponentPG) content).setReleases(List.of());
            ((ComponentPG) content).setVendors(List.of());
        }
    }

    @SuppressWarnings("unchecked")
    public void addEmbeddedResource(String relation, Object embeddedResource) {
        if (embeddedMap == null) {
            embeddedMap = new HashMap<>();
        }

        Object embeddedResources = embeddedMap.get(relation);
        boolean isPluralRelation = relation.endsWith("s");

        // if a relation is plural, the content will always be rendered as an array
        if (isPluralRelation) {
            if (embeddedResources == null) {
                embeddedResources = new ArrayList<>();
            }
            ((List<Object>) embeddedResources).add(embeddedResource);

            // if a relation is singular, it would be a single object if there is only one object
            // available
            // Otherwise it would be rendered as array
        } else {
            if (embeddedResources == null) {
                embeddedResources = embeddedResource;
            } else {
                if (embeddedResources instanceof List) {
                    ((List<Object>) embeddedResources).add(embeddedResource);
                } else {
                    List<Object> embeddedResourcesList = new ArrayList<>();
                    embeddedResourcesList.add(embeddedResources);
                    embeddedResourcesList.add(embeddedResource);
                    embeddedResources = embeddedResourcesList;
                }
            }
        }
        embeddedMap.put(relation, embeddedResources);
    }

    @JsonProperty("_embedded")
    public Map<String, Object> getEmbeddedResources() {
        return embeddedMap;
    }
}
