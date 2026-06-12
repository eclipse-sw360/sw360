/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * With modifications by Siemens AG, 2016.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.cvesearch.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.cvesearch.datasink.VulnerabilityConnector;
import org.eclipse.sw360.cvesearch.datasource.CveSearchApiImpl;
import org.eclipse.sw360.cvesearch.datasource.CveSearchData;
import org.eclipse.sw360.cvesearch.datasource.CveSearchWrapper;
import org.eclipse.sw360.cvesearch.entitytranslation.CveSearchDataTranslator;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.components.Component;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.cvesearch.UpdateType;
import org.eclipse.sw360.datahandler.services.cvesearch.VulnerabilityUpdateStatus;
import org.eclipse.sw360.datahandler.services.projects.Project;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.sw360.cvesearch.helper.VulnerabilityUtils;

import static org.eclipse.sw360.cvesearch.helper.VulnerabilityUtils.getEmptyVulnerabilityUpdateStatus;

@Service
public class CveSearchHandler {

    private static final Logger log = LogManager.getLogger(CveSearchHandler.class);

    public static final String CVESEARCH_HOST_PROPERTY = "cvesearch.host";

    private final VulnerabilityConnector vulnerabilityConnector;
    private final CveSearchWrapper cveSearchWrapper;

    public CveSearchHandler() throws IOException {
        this(new VulnerabilityConnector());
    }

    CveSearchHandler(VulnerabilityConnector vulnerabilityConnector) throws IOException {
        this.vulnerabilityConnector = vulnerabilityConnector;

        Properties props = CommonUtils.loadProperties(CveSearchHandler.class, "/cvesearch.properties");
        String host = props.getProperty(CVESEARCH_HOST_PROPERTY, "https://localhost:5000");

        log.info("Using {} for CVE search...", host);

        cveSearchWrapper = new CveSearchWrapper(new CveSearchApiImpl(host));
    }

    private VulnerabilityUpdateStatus updateForRelease(Release release) {
        Optional<List<CveSearchData>> cveSearchDatas = cveSearchWrapper.searchForRelease(release);
        if (cveSearchDatas.isEmpty()) {
            return getEmptyVulnerabilityUpdateStatus(RequestStatus.FAILURE);
        }

        CveSearchDataTranslator cveSearchDataTranslator = new CveSearchDataTranslator();
        List<CveSearchDataTranslator.VulnerabilityWithRelation> translated = cveSearchDatas.get().stream()
                .map(cveSearchDataTranslator)
                .map(vulnerabilityWithRelation -> {
                    vulnerabilityWithRelation.relation.setReleaseId(release.getId());
                    return vulnerabilityWithRelation;
                })
                .collect(Collectors.toList());

        VulnerabilityUpdateStatus updateStatus = getEmptyVulnerabilityUpdateStatus();
        for (CveSearchDataTranslator.VulnerabilityWithRelation vulnerabilityWithRelation : translated) {
            updateStatus = vulnerabilityConnector.addOrUpdate(
                    vulnerabilityWithRelation.vulnerability,
                    vulnerabilityWithRelation.relation,
                    updateStatus);
        }

        return updateStatus;
    }

    public VulnerabilityUpdateStatus updateForRelease(String releaseId) {
        Optional<Release> release = vulnerabilityConnector.getRelease(releaseId);
        return release.map(this::updateForRelease)
                .orElseGet(() -> getEmptyVulnerabilityUpdateStatus(RequestStatus.FAILURE));
    }

    public VulnerabilityUpdateStatus updateForComponent(String componentId) {
        Optional<Component> component = vulnerabilityConnector.getComponent(componentId);

        return component.map(c -> c.getReleaseIds() != null
                        ? c.getReleaseIds().stream()
                                .map(this::updateForRelease)
                                .reduce(getEmptyVulnerabilityUpdateStatus(), (a, b) -> VulnerabilityUtils.reduceVulnerabilityUpdateStatus(a, b))
                        : getEmptyVulnerabilityUpdateStatus())
                .orElseGet(() -> getEmptyVulnerabilityUpdateStatus(RequestStatus.FAILURE));
    }

    public VulnerabilityUpdateStatus updateForProject(String projectId) {
        Optional<Project> project = vulnerabilityConnector.getProject(projectId);

        return project.map(p -> p.getReleaseIdToUsage() != null
                        ? p.getReleaseIdToUsage().keySet().stream()
                                .map(this::updateForRelease)
                                .reduce(getEmptyVulnerabilityUpdateStatus(), (a, b) -> VulnerabilityUtils.reduceVulnerabilityUpdateStatus(a, b))
                        : getEmptyVulnerabilityUpdateStatus())
                .orElseGet(() -> getEmptyVulnerabilityUpdateStatus(RequestStatus.FAILURE));
    }

    public VulnerabilityUpdateStatus fullUpdate() {
        List<Release> allReleases = vulnerabilityConnector.getAllReleases();

        return allReleases.stream()
                .map(this::updateForRelease)
                .reduce(getEmptyVulnerabilityUpdateStatus(), (a, b) -> VulnerabilityUtils.reduceVulnerabilityUpdateStatus(a, b));
    }

    public RequestStatus update() {
        log.info("Starting CveSearch update...");
        VulnerabilityUpdateStatus vulnerabilityUpdateStatus = fullUpdate();
        log.info("CveSearch update finished with status: {}", vulnerabilityUpdateStatus.getRequestStatus());
        log.info("The following vulnerability/ies could not be imported: {}\n"
                        + "The following vulnerability/ies were updated: {}\n"
                        + "The following vulnerability/ies were added: {}",
                vulnerabilityUpdateStatus.getStatusToVulnerabilityIds().get(UpdateType.FAILED),
                vulnerabilityUpdateStatus.getStatusToVulnerabilityIds().get(UpdateType.UPDATED),
                vulnerabilityUpdateStatus.getStatusToVulnerabilityIds().get(UpdateType.NEW));

        return vulnerabilityUpdateStatus.getRequestStatus();
    }

    public Set<String> findCpes(String vendor, String product, String version) {
        log.warn("findCpes is not implemented (vendor={}, product={}, version={})", vendor, product, version);
        return Set.of();
    }
}
