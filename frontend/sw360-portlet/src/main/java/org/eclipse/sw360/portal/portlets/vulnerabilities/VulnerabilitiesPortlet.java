/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.vulnerabilities;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.*;
import org.eclipse.sw360.portal.common.CustomFieldHelper;
import org.eclipse.sw360.portal.common.UsedAsLiferayAction;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.portlet.*;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.eclipse.sw360.datahandler.common.SW360Utils.printName;
import static org.eclipse.sw360.portal.common.PortalConstants.*;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
        "/org/eclipse/sw360/portal/portlets/base.properties",
        "/org/eclipse/sw360/portal/portlets/default.properties"
    },
    property = {
        "javax.portlet.name=" + VULNERABILITIES_PORTLET_NAME,

        "javax.portlet.display-name=Vulnerabilities",
        "javax.portlet.info.short-title=Vulnerabilities",
        "javax.portlet.info.title=Vulnerabilities",

        "javax.portlet.init-param.view-template=/html/vulnerabilities/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class VulnerabilitiesPortlet extends Sw360Portlet {

    private static final Logger log = Logger.getLogger(VulnerabilitiesPortlet.class);
    private static final String YEAR_MONTH_DAY_REGEX = "\\d\\d\\d\\d-\\d\\d-\\d\\d.*";

    private static final String EXTERNAL_ID = Vulnerability._Fields.EXTERNAL_ID.toString();
    private static final String VULNERABLE_CONFIGURATION = Vulnerability._Fields.VULNERABLE_CONFIGURATION.toString();

    public static final Set<Vulnerability._Fields> FILTERED_FIELDS = ImmutableSet.of(
            Vulnerability._Fields.EXTERNAL_ID,
            Vulnerability._Fields.VULNERABLE_CONFIGURATION
    );

    private static final int DEFAULT_VIEW_SIZE = 200;

    //Helper methods
    private void addVulnerabilityBreadcrumb(RenderRequest request, RenderResponse response, Vulnerability vulnerability) {
        PortletURL url = response.createRenderURL();
        url.setParameter(PAGENAME, PAGENAME_DETAIL);
        url.setParameter(VULNERABILITY_ID, vulnerability.getExternalId());

        addBreadcrumbEntry(request, printName(vulnerability), url);
    }

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        String pageName = request.getParameter(PAGENAME);
        if (PAGENAME_DETAIL.equals(pageName)) {
            prepareDetailView(request, response);
            include("/html/vulnerabilities/detail.jsp", request, response);
        } else {
            prepareStandardView(request);
            super.doView(request, response);
        }
    }

    @UsedAsLiferayAction
    public void applyFilters(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        for (Vulnerability._Fields field : FILTERED_FIELDS) {
            response.setRenderParameter(field.toString(), nullToEmpty(request.getParameter(field.toString())));
        }
    }

    private void prepareStandardView(RenderRequest request) throws IOException {
        getFilteredVulnerabilityList(request);
    }

    private void getFilteredVulnerabilityList(PortletRequest request) throws IOException {
        List<Vulnerability> vulnerabilities = Collections.emptyList();
        int totalRows = 0;

        String externalId = request.getParameter(EXTERNAL_ID);
        String vulnerableConfig = request.getParameter(VULNERABLE_CONFIGURATION);

        try {
            final User user = UserCacheHolder.getUserFromRequest(request);
            int limit = CustomFieldHelper.loadAndStoreStickyViewSize(request, user, CUSTOM_FIELD_VULNERABILITIES_VIEW_SIZE);

            VulnerabilityService.Iface vulnerabilityClient = thriftClients.makeVulnerabilityClient();
            if (!isNullOrEmpty(externalId) || !isNullOrEmpty(vulnerableConfig)) {
                vulnerabilities = vulnerabilityClient.getVulnerabilitiesByExternalIdOrConfiguration(externalId, vulnerableConfig, user);
                totalRows = vulnerabilities.size();

                if(limit>0) {
                    vulnerabilities = vulnerabilities.stream().limit(limit).collect(Collectors.toList());
                }
            } else {
                vulnerabilities = vulnerabilityClient.getLatestVulnerabilities(user, limit);
                totalRows = vulnerabilityClient.getTotalVulnerabilityCount(user);
            }
        } catch (TException e) {
            log.error("Could not search components in backend ", e);
        }

        shortenTimeStampsToDates(vulnerabilities);

        for (Vulnerability._Fields field : FILTERED_FIELDS) {
            request.setAttribute(field.getFieldName(), nullToEmpty(request.getParameter(field.toString())));
        }
        request.setAttribute(TOTAL_ROWS, totalRows);
        request.setAttribute(VULNERABILITY_LIST, vulnerabilities);
    }


    private void shortenTimeStampsToDates(List<Vulnerability> vulnerabilities) {
        vulnerabilities.stream().forEach(v -> {
            if (isFormattedTimeStamp(v.getPublishDate())) {
                v.setPublishDate(getDateFromFormattedTimeStamp(v.getPublishDate()));
            }
            if (isFormattedTimeStamp(v.getLastExternalUpdate())) {
                v.setLastExternalUpdate(getDateFromFormattedTimeStamp(v.getLastExternalUpdate()));
            }
            if (v.isSetCvssTime() && isFormattedTimeStamp(v.getCvssTime())) {
                v.setCvssTime(getDateFromFormattedTimeStamp(v.getCvssTime()));
            }
        });
    }

    private String getDateFromFormattedTimeStamp(String formattedTimeStamp) {
        return formattedTimeStamp.substring(0, 10);
    }

    private boolean isFormattedTimeStamp(String potentialTimestamp) {
        if (isNullOrEmpty(potentialTimestamp)) {
            return false;
        } else {
            return potentialTimestamp.matches(YEAR_MONTH_DAY_REGEX);
        }
    }

    private void prepareDetailView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        User user = UserCacheHolder.getUserFromRequest(request);
        String externalId = request.getParameter(VULNERABILITY_ID);
        if (externalId != null) {
            try {
                VulnerabilityService.Iface client = thriftClients.makeVulnerabilityClient();
                VulnerabilityWithReleaseRelations vulnerabilityWithReleaseRelations = client.getVulnerabilityWithReleaseRelationsByExternalId(externalId, user);

                if (vulnerabilityWithReleaseRelations != null) {
                    Vulnerability vulnerability = vulnerabilityWithReleaseRelations.getVulnerability();
                    List<Release> releases = getReleasesFromRelations(user, vulnerabilityWithReleaseRelations);

                    request.setAttribute(VULNERABILITY, vulnerability);
                    request.setAttribute(DOCUMENT_ID, externalId);
                    request.setAttribute(USING_RELEASES, releases);

                    addVulnerabilityBreadcrumb(request, response, vulnerability);
                }

            } catch (TException e) {
                log.error("Error fetching vulnerability from backend!", e);
            }
        }
    }

    private List<Release> getReleasesFromRelations(User user, VulnerabilityWithReleaseRelations vulnerabilityWithReleaseRelations) {
        if (vulnerabilityWithReleaseRelations != null) {
            List<ReleaseVulnerabilityRelation> relations = vulnerabilityWithReleaseRelations.getReleaseRelation();

            Set<String> ids = relations.stream()
                    .map(ReleaseVulnerabilityRelation::getReleaseId)
                    .collect(Collectors.toSet());

            try {
                ComponentService.Iface client = thriftClients.makeComponentClient();
                return client.getReleasesById(ids, user);
            } catch (TException e) {
                log.error("Error fetching releases from backend!", e);
            }
        }
        return ImmutableList.of();
    }
}
