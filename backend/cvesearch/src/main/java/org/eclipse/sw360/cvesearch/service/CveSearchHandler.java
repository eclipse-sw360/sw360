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

import org.eclipse.sw360.cvesearch.datasink.VulnerabilityConnector;
import org.eclipse.sw360.cvesearch.datasource.CveSearchApiImpl;
import org.eclipse.sw360.cvesearch.datasource.CveSearchData;
import org.eclipse.sw360.cvesearch.datasource.CveSearchWrapper;
import org.eclipse.sw360.cvesearch.entitytranslation.CveSearchDataTranslator;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.cvesearch.CveSearchService;
import org.eclipse.sw360.datahandler.thrift.cvesearch.UpdateType;
import org.eclipse.sw360.datahandler.thrift.cvesearch.VulnerabilityUpdateStatus;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.sw360.cvesearch.helper.VulnerabilityUtils.*;

public class CveSearchHandler implements CveSearchService.Iface {

    private static final Logger log = LogManager.getLogger(CveSearchHandler.class);

    public static final String CVESEARCH_HOST_PROPERTY = "cvesearch.host";
    private VulnerabilityConnector vulnerabilityConnector;
    private CveSearchWrapper cveSearchWrapper;


    public CveSearchHandler() {
        try {
            vulnerabilityConnector = new VulnerabilityConnector();
        } catch (IOException ioe) {
            log.error("Exception when creating CveSearchHandler", ioe);
        }

        Properties props = CommonUtils.loadProperties(CveSearchHandler.class, "/cvesearch.properties");
        String host = props.getProperty(CVESEARCH_HOST_PROPERTY, "https://localhost:5000");

        log.info("Using " + host + " for CVE search...");

        cveSearchWrapper = new CveSearchWrapper(new CveSearchApiImpl(host));
    }

    private VulnerabilityUpdateStatus updateForRelease(Release release) {
        Optional<List<CveSearchData>> cveSearchDatas = cveSearchWrapper.searchForRelease(release);
        if(!cveSearchDatas.isPresent()) {
            return new VulnerabilityUpdateStatus().setRequestStatus(RequestStatus.FAILURE);
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
            updateStatus = vulnerabilityConnector.addOrUpdate(vulnerabilityWithRelation.vulnerability,
                    vulnerabilityWithRelation.relation,
                    updateStatus);
        }

        return updateStatus;
    }

    @Override
    public VulnerabilityUpdateStatus updateForRelease(String releaseId) {
        Optional<Release> release = vulnerabilityConnector.getRelease(releaseId);
        Optional<VulnerabilityUpdateStatus> updateStatus = release.map(this::updateForRelease);
        return updateStatus.orElse(getEmptyVulnerabilityUpdateStatus(RequestStatus.FAILURE));
    }

    @Override
    public VulnerabilityUpdateStatus updateForComponent(String componentId) throws TException {
        Optional<Component> component = vulnerabilityConnector.getComponent(componentId);

        return component.map(
                c -> c.isSetReleaseIds()
                        ? c.getReleaseIds().stream()
                                .map(this::updateForRelease)
                                .reduce(getEmptyVulnerabilityUpdateStatus(),
                                        (r1, r2) -> reduceVulnerabilityUpdateStatus(r1,r2))
                        : getEmptyVulnerabilityUpdateStatus()
            ).orElse(getEmptyVulnerabilityUpdateStatus(RequestStatus.FAILURE));
    }

    @Override
    public VulnerabilityUpdateStatus updateForProject(String projectId) throws TException {
        Optional<Project> project = vulnerabilityConnector.getProject(projectId);

        return project.map(
                r -> r.isSetReleaseIdToUsage()
                        ? r.getReleaseIdToUsage().keySet().stream()
                                .map(this::updateForRelease)
                                .reduce(getEmptyVulnerabilityUpdateStatus(),
                                    (r1, r2) -> reduceVulnerabilityUpdateStatus(r1,r2))
                        : getEmptyVulnerabilityUpdateStatus()
            ).orElse(getEmptyVulnerabilityUpdateStatus(RequestStatus.FAILURE));
    }

    @Override
    public VulnerabilityUpdateStatus fullUpdate() throws TException {
        List<Release> allReleases = vulnerabilityConnector.getAllReleases();

        return allReleases.stream()
                .map(this::updateForRelease)
                .reduce(getEmptyVulnerabilityUpdateStatus(),
                        (r1, r2) -> reduceVulnerabilityUpdateStatus(r1,r2));
    }

    @Override
    public RequestStatus update() throws TException {
        log.info("Starting CveSearch update...");
        VulnerabilityUpdateStatus vulnerabilityUpdateStatus = fullUpdate();
        log.info("CveSearch update finished with status:" + vulnerabilityUpdateStatus.getRequestStatus());
        log.info("The following vulnerability/ies could not be imported:" + vulnerabilityUpdateStatus.getStatusToVulnerabilityIds().get(UpdateType.FAILED) + "\n"+
                        "The following vulnerability/ies were updated:" + vulnerabilityUpdateStatus.getStatusToVulnerabilityIds().get(UpdateType.UPDATED) + "\n"+
                        "The following vulnerability/ies were added:" + vulnerabilityUpdateStatus.getStatusToVulnerabilityIds().get(UpdateType.NEW));

        return vulnerabilityUpdateStatus.getRequestStatus();
    }

    @Override
    public Set<String> findCpes(String vendor, String product, String version) throws TException {
        if(vendor == null || product == null || version == null) {
            return Collections.emptySet();
        }
        vendor = vendor.trim();
        product = product.trim();
        version = version.trim().toLowerCase();
        if(vendor.isEmpty() || product.isEmpty()) {
            return Collections.emptySet();
        }
       try {
           List<CveSearchData> cveSearchDataList = cveSearchWrapper.search(vendor, product);
           String finalVersion = version;
           var cpeStream = cveSearchDataList.stream()
                   .map(CveSearchData::getVulnerable_configuration)
                   .filter(Objects::nonNull)
                   .flatMap(m -> m.keySet().stream());
           if(finalVersion.isEmpty() || finalVersion.equals("*")) {
               return cpeStream.collect(Collectors.toSet());
           }
           return cpeStream.filter(cpe -> finalVersion.equals(getVersionFromCpe(cpe)))
                   .collect(Collectors.toSet());

       } catch (IOException e) {
           throw new TException("CVE search failed: " + e.getMessage(), e);
       }
    }

    private String getVersionFromCpe(String cpe) {
        if(cpe == null || cpe.isEmpty()) {
            return "";
        }
        String[] split = cpe.split(":");
        if(split.length <= 5) {
            return "";
        }
        return split[5].toLowerCase();
    }
}
