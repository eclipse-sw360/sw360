/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets.vulnerabilities;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import com.liferay.portal.kernel.portlet.LiferayPortletURL;
import com.liferay.portal.kernel.portlet.PortletURLFactoryUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.WebKeys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.*;
import org.eclipse.sw360.portal.common.*;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.portlets.components.ComponentPortletUtils;
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.springframework.security.access.AccessDeniedException;

import javax.portlet.*;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        "javax.portlet.resource-bundle=content.Language",
        "javax.portlet.init-param.view-template=/html/vulnerabilities/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class VulnerabilitiesPortlet extends Sw360Portlet {

    private static final Logger log = LogManager.getLogger(VulnerabilitiesPortlet.class);
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
        } else if (PAGENAME_EDIT.equals(pageName)) {
            prepareEditView(request, response);
            include("/html/vulnerabilities/edit.jsp", request, response);
        } else {
            prepareStandardView(request);
            super.doView(request, response);
        }
    }

    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String action = request.getParameter(ACTION);
        if (REMOVE_VULNERABILITY.equals(action)) {
            removeVulnerability(request, response);
        }
        if (FIND_BY_EXTERNAL_ID.equals(action)) {
            RequestStatus requestStatus = getVulnerabilityByExternalId(request, response);
            serveRequestStatus(request, response, requestStatus, "", log);
        }
    }

    private void removeVulnerability(ResourceRequest request, ResourceResponse response) {
        final RequestStatus requestStatus = deleteVulnerability(request, log);
        if (requestStatus == RequestStatus.SUCCESS) {
            serveRequestStatus(request, response, requestStatus, "Vulnerability " + request.getParameter(VULNERABILITY_ID) + " has been deleted", log);
        } else if (requestStatus == RequestStatus.IN_USE) {
            serveRequestStatus(request, response, requestStatus, ErrorMessages.ERROR_VULNERABILITY_USED_BY_RELEASE, log);
        } else {
            serveRequestStatus(request, response, requestStatus, ErrorMessages.ERROR_VULNERABILITY_DELETE, log);
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
            List<Release> releases = new ArrayList<>();
            ComponentService.Iface client = thriftClients.makeComponentClient();
            List<ReleaseVulnerabilityRelation> relations = vulnerabilityWithReleaseRelations.getReleaseRelation();

            Set<String> ids = relations.stream()
                    .map(ReleaseVulnerabilityRelation::getReleaseId)
                    .collect(Collectors.toSet());
            ids.stream().forEach(id -> {
                try {
                    Release release = client.getReleaseById(id, user);
                    if (release != null) {
                        releases.add(release);
                    }
                } catch (TException e) {
                    log.error("Error fetching releases from backend!", e.getMessage());
                }
            });
            return releases;
        }
        return ImmutableList.of();
    }

    private void prepareEditView(RenderRequest request, RenderResponse response) {
        User user = UserCacheHolder.getUserFromRequest(request);
        String id = request.getParameter(VULNERABILITY_ID);

        List<String> vulnerabilityImpacts = Stream.of(VulnerabilityImpact.values()).map(
                VulnerabilityImpact::name).collect( Collectors.toList());
        List<String> vulnerabilityAccessAuthentications = Stream.of(VulnerabilityAccessAuthentication.values()).map(
                VulnerabilityAccessAuthentication::name).collect( Collectors.toList());
        List<String> vulnerabilityAccessComplexities = Stream.of(VulnerabilityAccessComplexity.values()).map(
                VulnerabilityAccessComplexity::name).collect( Collectors.toList());
        List<String> vulnerabilityAccessVectors = Stream.of(VulnerabilityAccessVector.values()).map(
                VulnerabilityAccessVector::name).collect( Collectors.toList());

        if (id != null) {
            try {
                VulnerabilityService.Iface vulnerabilityClient = thriftClients.makeVulnerabilityClient();
                Vulnerability vulnerability = vulnerabilityClient.getById(id);
                request.setAttribute(VULNERABILITY, vulnerability);
            } catch (TException e) {
                log.error("Error fetching vulnerability from backend!", e);
            }
        }
        request.setAttribute("vulnerabilityImpacts", vulnerabilityImpacts);
        request.setAttribute("vulnerabilityAccessAuthentications", vulnerabilityAccessAuthentications);
        request.setAttribute("vulnerabilityAccessComplexities", vulnerabilityAccessComplexities);
        request.setAttribute("vulnerabilityAccessVectors", vulnerabilityAccessVectors);
    }

    @UsedAsLiferayAction
    public void updateVulnerability(ActionRequest request, ActionResponse response) throws PortletException {
        String id = request.getParameter(VULNERABILITY_ID);
        final User user = UserCacheHolder.getUserFromRequest(request);
        if (id != null) {
            try {
                VulnerabilityService.Iface vulnerabilityClient = thriftClients.makeVulnerabilityClient();
                Vulnerability vulnerability = vulnerabilityClient.getById(id);
                updateVulnerabilityFromRequest(request, vulnerability);
                vulnerability.setIsSetCvss(true);

                RequestStatus requestStatus = vulnerabilityClient.updateVulnerability(vulnerability, user);
                setSessionMessage(request, requestStatus, "Vulnerability", "update", vulnerability.getExternalId());
                removeParamUrl(request, response);
            } catch (TException | ParseException e) {
                log.error("Error fetching vulnerability from backend!", e);
                response.setRenderParameter(PAGENAME, PAGENAME_VIEW);
                setSW360SessionError(request, ErrorMessages.ERROR_UPDATE_VULNERABILITY);
            }
        } else {
            addVulnerability(request, response);
        }
    }

    private void addVulnerability(ActionRequest request, ActionResponse response) {
        Vulnerability vulnerability = new Vulnerability();
        final User user = UserCacheHolder.getUserFromRequest(request);
        try {
            VulnerabilityService.Iface vulnerabilityClient = thriftClients.makeVulnerabilityClient();
            updateVulnerabilityFromRequest(request, vulnerability);
            vulnerability.setIsSetCvss(true);
            RequestStatus requestStatus = vulnerabilityClient.addVulnerability(vulnerability, user);
            setSessionMessage(request, requestStatus, "Vulnerability", "adde", vulnerability.getExternalId());
            removeParamUrl(request, response);
        } catch (TException | ParseException e) {
            log.error("Error adding vulnerability", e);
            response.setRenderParameter(PAGENAME, PAGENAME_VIEW);
            setSW360SessionError(request, ErrorMessages.ERROR_ADD_VULNERABILITY);
        } catch (PortletException e) {
            e.printStackTrace();
            response.setRenderParameter(PAGENAME, PAGENAME_VIEW);
            setSW360SessionError(request, ErrorMessages.ERROR_ADD_VULNERABILITY);
        }
    }


    @UsedAsLiferayAction
    public void removeVulnerability(ActionRequest request, ActionResponse response) throws IOException, PortletException {
        final RequestStatus requestStatus = deleteVulnerability(request, log);
        if (requestStatus == RequestStatus.SUCCESS) {
            setSessionMessage(request, requestStatus, "Vulnerability", "remove");
        } else if (requestStatus == RequestStatus.IN_USE) {
            response.setRenderParameter(PAGENAME, PAGENAME_EDIT);
            response.setRenderParameter(VULNERABILITY_ID, request.getParameter(VULNERABILITY_ID));
            setSW360SessionError(request, ErrorMessages.ERROR_VULNERABILITY_USED_BY_RELEASE);
        } else if (requestStatus == RequestStatus.FAILURE) {
            setSessionMessage(request, requestStatus, "Vulnerability", "remove");
        }

    }

    public RequestStatus getVulnerabilityByExternalId(ResourceRequest request, ResourceResponse response) {
        String vulnerabilityExternalId = request.getParameter(VULNERABILITY_EXTERNAL_ID);
        final User user = UserCacheHolder.getUserFromRequest(request);
        VulnerabilityService.Iface vulnerabilityClient = thriftClients.makeVulnerabilityClient();
        try {
            Vulnerability vulnerabilityByExternalId = vulnerabilityClient.getVulnerabilityByExternalId(vulnerabilityExternalId, user);
            if (vulnerabilityByExternalId == null) {
                return RequestStatus.SUCCESS;
            } else {
                return RequestStatus.DUPLICATE;
            }
        } catch (TException e) {
            return RequestStatus.SUCCESS;
        }
    }

    public void removeParamUrl(ActionRequest request, ActionResponse response) {
        try {
            String portletId = (String) request.getAttribute(WebKeys.PORTLET_ID);
            ThemeDisplay tD = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
            long plid = tD.getPlid();
            LiferayPortletURL redirectUrl = PortletURLFactoryUtil.create(request, portletId, plid, PortletRequest.RENDER_PART);
            request.setAttribute(WebKeys.REDIRECT, redirectUrl.toString());
            response.sendRedirect(redirectUrl.toString());
        } catch (IOException e) {
            log.error("Error remove param url: {}", e.getMessage());
        }
    }

    public static void updateVulnerabilityFromRequest(PortletRequest request, Vulnerability vulnerability) throws ParseException {
        setFieldValue(request, vulnerability, Vulnerability._Fields.EXTERNAL_ID);
        setFieldValue(request, vulnerability, Vulnerability._Fields.DESCRIPTION);
        setFieldValue(request, vulnerability, Vulnerability._Fields.TITLE);
        setFieldValue(request, vulnerability, Vulnerability._Fields.ACCESS);
        setFieldValue(request, vulnerability, Vulnerability._Fields.PRIORITY);
        setFieldValue(request, vulnerability, Vulnerability._Fields.PRIORITY_TEXT);
        setFieldValue(request, vulnerability, Vulnerability._Fields.LEGAL_NOTICE);
        setFieldValue(request, vulnerability, Vulnerability._Fields.EXTENDED_DESCRIPTION);
        setFieldValue(request, vulnerability, Vulnerability._Fields.CWE);
        setFieldValue(request, vulnerability, Vulnerability._Fields.ACTION);
        setFieldValue(request, vulnerability, Vulnerability._Fields.ASSIGNED_EXT_COMPONENT_IDS);

        String urlParten = "(https?:\\/\\/(?:www\\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s]{2,}|www\\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s]{2,}|https?:\\/\\/(?:www\\.|(?!www))[a-zA-Z0-9]+\\.[^\\s]{2,}|www\\.[a-zA-Z0-9]+\\.[^\\s]{2,}|http:\\/\\/localhost.*)";
        // Get cvss date time from request
        String cvssDate = request.getParameter(PortalConstants.CVSS_DATE).trim();
        String cvssTime = request.getParameter(PortalConstants.CVSS_TIME).trim();

        SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat formatTime = new SimpleDateFormat("HH:mm:ss");

        if (cvssDate.equals("")) {
            cvssDate = formatDate.format(new Date());
        }

        if (cvssTime.equals("")) {
            cvssTime = formatTime.format(new Date());
        }
        String cvssDateTime = cvssDate + "T" + cvssTime + ".000000";

        // Get external update date time from request
        String externalUpdateDate = request.getParameter(PortalConstants.EXTERNAL_UPDATE_DATE).trim();
        String externalUpdateTime = request.getParameter(PortalConstants.EXTERNAL_UPDATE_TIME).trim();

        if (externalUpdateDate.equals("")) {
            externalUpdateDate = formatDate.format(new Date());
        }

        if (externalUpdateTime.equals("")) {
            externalUpdateTime = formatTime.format(new Date());
        }
        String externalUpdateDateTime = externalUpdateDate + "T" + externalUpdateTime + ".000000";

        // Get publish date time from request
        String publishDate = request.getParameter(PortalConstants.PUBLISH_DATE).trim();
        String publishTime = request.getParameter(PortalConstants.PUBLISH_TIME).trim();

        if (publishDate.equals("")) {
            publishDate = formatDate.format(new Date());
        }

        if (publishTime.equals("")) {
            publishTime = formatTime.format(new Date());
        }
        String publishDateTime = publishDate + "T" + publishTime + ".000000";

        // Get other information from request
        String cvss = request.getParameter(String.valueOf(Vulnerability._Fields.CVSS)).trim();

        String[] impactKeys = request.getParameterValues(PortalConstants.VULNERABILITY_IMPACT_KEY);
        String[] impactValues = request.getParameterValues(PortalConstants.VULNERABILITY_IMPACT_VALUE);

        String[] accessKeys = request.getParameterValues(PortalConstants.VULNERABILITY_ACCESS_KEY);
        String[] accessValues = request.getParameterValues(PortalConstants.VULNERABILITY_ACCESS_VALUE);

        String[] configKeys = request.getParameterValues(PortalConstants.VULNERABILITY_CONFIG_KEY);
        String[] configValues = request.getParameterValues(PortalConstants.VULNERABILITY_CONFIG_VALUE);

        String[] advisoryVendors = request.getParameterValues(PortalConstants.VULNERABILITY_ADVISORY_VENDOR);
        String[] advisoryNames = request.getParameterValues(PortalConstants.VULNERABILITY_ADVISORY_NAME);
        String[] advisoryUrls = request.getParameterValues(PortalConstants.VULNERABILITY_ADVISORY_URL);

        String[] cveYears = request.getParameterValues(PortalConstants.VULNERABILITY_CVE_YEAR);
        String[] cveNumbers = request.getParameterValues(PortalConstants.VULNERABILITY_CVE_NUMBER);

        String[] referenceArray = request.getParameterValues(String.valueOf(Vulnerability._Fields.REFERENCES));
        String[] externalComponentIdArray = request.getParameterValues(String.valueOf(Vulnerability._Fields.ASSIGNED_EXT_COMPONENT_IDS));

        String cwe = "CWE-" + request.getParameter(String.valueOf(Vulnerability._Fields.CWE));

        Map<String, String> impacts = new HashMap<>();
        Map<String, String> accesses = new HashMap<>();
        Map<String, String> configs = new HashMap<>();
        Set<VendorAdvisory> vendorAdvisories = new HashSet<>();
        Set<CVEReference> cveReferences = new HashSet<>();
        Set<String> externalComponentIds = new HashSet<String>();
        Set<String> references = new HashSet<String>();

        for (int i = 0; i < impactKeys.length; i++) {
            if (!impactKeys[i].trim().equals("") && !impactValues[i].trim().equals("")) {
                impacts.put(impactKeys[i].trim(), impactValues[i].trim());
            }
        }

        for (int i = 0; i < accessKeys.length; i++) {
            if (!accessKeys[i].equals("") && !accessValues[i].equals("")) {
                accesses.put(accessKeys[i].trim(), accessValues[i].trim());
            }
        }

        for (int i = 0; i < configKeys.length; i++) {
            if (!configKeys[i].equals("") || !configValues[i].equals("")) {
                configs.put(configKeys[i].trim(), configValues[i].trim());
            }
        }
        for (int i = 0; i < advisoryVendors.length; i++) {
            if (!advisoryVendors[i].equals("") && !advisoryNames[i].equals("") && !advisoryUrls[i].equals("")) {
                if(advisoryUrls[i].trim().matches(urlParten)) {
                    VendorAdvisory vendorAdvisory = new VendorAdvisory();
                    vendorAdvisory.setVendor(advisoryVendors[i].trim());
                    vendorAdvisory.setName(advisoryNames[i].trim());
                    vendorAdvisory.setUrl(advisoryUrls[i].trim());
                    vendorAdvisories.add(vendorAdvisory);
                }
            }
        }

        for (int i = 0; i < cveYears.length; i++) {
            if (!cveNumbers[i].equals("") && !cveNumbers[i].equals("")) {
                CVEReference cveReference = new CVEReference();
                cveReference.setYear(cveYears[i].trim());
                cveReference.setNumber(cveNumbers[i].trim());
                cveReferences.add(cveReference);
            }
        }

        for (String reference : referenceArray) {
            if (!reference.equals("") && reference.trim().matches(urlParten)) {
                references.add(reference.trim());
            }
        }

        for (String componentId : externalComponentIdArray) {
            if (!componentId.equals("")) {
                externalComponentIds.add(componentId.trim());
            }
        }

        vulnerability.setReferences(references);
        vulnerability.setImpact(impacts);
        vulnerability.setAccess(accesses);
        vulnerability.setVulnerableConfiguration(configs);
        vulnerability.setCvss(Double.parseDouble(cvss));
        vulnerability.setVendorAdvisories(vendorAdvisories);
        vulnerability.setCveReferences(cveReferences);
        vulnerability.setCwe(cwe);
        vulnerability.setAssignedExtComponentIds(externalComponentIds);

        SimpleDateFormat formatDateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
        if (formatDateTime.parse(publishDateTime) != null) {
            vulnerability.setPublishDate(publishDateTime);
        }
        if (formatDateTime.parse(cvssDateTime) != null) {
            vulnerability.setCvssTime(cvssDateTime);
        }
        if (formatDateTime.parse(externalUpdateDateTime) != null) {
            vulnerability.setLastExternalUpdate(externalUpdateDateTime);
        }
    }

    public static RequestStatus deleteVulnerability(PortletRequest request, Logger log) {
        String vulnerabilityId = request.getParameter(PortalConstants.VULNERABILITY_ID);
        if (vulnerabilityId != null) {
            try {
                User user = UserCacheHolder.getUserFromRequest(request);
                ThriftClients thriftClients = new ThriftClients();
                VulnerabilityService.Iface vulnerabilityClient = thriftClients.makeVulnerabilityClient();
                Vulnerability vulnerability = vulnerabilityClient.getById(vulnerabilityId);
                return vulnerabilityClient.deleteVulnerability(vulnerability, user);
            } catch (Exception e) {
                log.error("Cannot find vulnerability from DB", e.getMessage());
                return RequestStatus.FAILURE;
            }
        }
        return RequestStatus.FAILURE;
    }

    private static void setFieldValue(PortletRequest request, Vulnerability vulnerability, Vulnerability._Fields field) {
        PortletUtils.setFieldValue(request, vulnerability, field, Vulnerability.metaDataMap.get(field), "");
    }
}
