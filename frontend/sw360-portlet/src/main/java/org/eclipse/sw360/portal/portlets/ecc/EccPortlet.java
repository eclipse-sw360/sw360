/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets.ecc;

import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.liferay.portal.kernel.json.JSONArray;
import org.eclipse.sw360.portal.common.datatables.PaginationParser;
import org.eclipse.sw360.portal.common.datatables.data.PaginationParameters;
import javax.servlet.http.HttpServletRequest;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import org.eclipse.sw360.portal.common.*;
import org.eclipse.sw360.datahandler.thrift.*;
import java.util.*;
import com.google.common.collect.*;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.common.*;

import javax.portlet.*;

import static org.eclipse.sw360.datahandler.common.CommonUtils.*;
import static com.liferay.portal.kernel.json.JSONFactoryUtil.createJSONArray;
import static com.liferay.portal.kernel.json.JSONFactoryUtil.createJSONObject;
import static java.lang.Math.min;
import static com.google.common.base.Strings.nullToEmpty;
import static org.eclipse.sw360.portal.common.PortalConstants.*;
import static org.eclipse.sw360.portal.common.PortalConstants.ECC_PORTLET_NAME;
import static org.eclipse.sw360.portal.common.PortalConstants.RELEASE_LIST;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
        "/org/eclipse/sw360/portal/portlets/base.properties",
        "/org/eclipse/sw360/portal/portlets/default.properties"
    },
    property = {
        "javax.portlet.name=" + ECC_PORTLET_NAME,

        "javax.portlet.display-name=ECC",
        "javax.portlet.info.short-title=ECC",
        "javax.portlet.info.title=ECC",
        "javax.portlet.resource-bundle=content.Language",
        "javax.portlet.init-param.view-template=/html/ecc/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class EccPortlet extends Sw360Portlet {

    private static final Logger log = LogManager.getLogger(EccPortlet.class);
    // ECC release view datatables, index of columns
    private static final int RELEASE_NO_SORT = -1;
    private static final int RELEASE_DT_ROW_ECC_STATUS = 0;
    private static final int RELEASE_DT_ROW_NAME = 1;
    private static final int RELEASE_DT_ROW_VERSION = 2;
    private static final int RELEASE_DT_ROW_GROUP = 3;
    private static final int RELEASE_DT_ROW_ASSESSOR_CONTACT_PERSON = 4;
    private static final int RELEASE_DT_ROW_ASSESSOR_DEPARTMENT = 5;
    private static final int RELEASE_DT_ROW_ASSESSMENT_DATE = 6;

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        prepareStandardView(request);
        // Proceed with page rendering
        super.doView(request, response);
    }

    private void prepareStandardView(RenderRequest request) {
        try {
            request.setAttribute(PortalConstants.EXACT_MATCH_CHECKBOX, nullToEmpty(request.getParameter(PortalConstants.EXACT_MATCH_CHECKBOX)));
        } catch (Exception e) {
            log.error("Could not fetch releases from backend", e);
        }

    }
    
    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        try {
            String action = request.getParameter(PortalConstants.ACTION);

            if (PortalConstants.LOAD_ECC_LIST.equals(action)) {
                serveECCReleaseList(request, response);
            }
        } catch (Exception e) {
            log.error("Could not fetch releases from backend", e);
            request.setAttribute(RELEASE_LIST, Collections.emptyList());
        }
        
        
    }
    
    private void serveECCReleaseList(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        HttpServletRequest originalServletRequest = PortalUtil.getOriginalServletRequest(PortalUtil.getHttpServletRequest(request));
        PaginationParameters paginationParameters = PaginationParser.parametersFrom(originalServletRequest);
        //PortletUtils.handlePaginationSortOrder(request, paginationParameters, releaseFilteredFields, RELEASE_NO_SORT);
        PaginationData pageData = new PaginationData();
        pageData.setRowsPerPage(paginationParameters.getDisplayLength());
        pageData.setDisplayStart(paginationParameters.getDisplayStart());
        pageData.setAscending(paginationParameters.isAscending().get());
        int sortParam = -1;
        if (paginationParameters.getSortingColumn().isPresent()) {
            sortParam = paginationParameters.getSortingColumn().get();
            if (sortParam == 1 && Integer.valueOf(paginationParameters.getEcho()) == 1) {
                pageData.setSortColumnNumber(-1);
            } else {
                pageData.setSortColumnNumber(sortParam);
            }
        }
        Map<PaginationData, List<Release>> releaseList = getReleaseList(request, pageData);
        JSONArray jsonReleases = getReleaseData(releaseList.values().iterator().next(), paginationParameters, request);
        JSONObject jsonResult = createJSONObject();
        int count = (int) (releaseList.keySet().iterator().next().getTotalRowCount());
        jsonResult.put(DATATABLE_RECORDS_TOTAL, count);
        jsonResult.put(DATATABLE_RECORDS_FILTERED, count);
        jsonResult.put(DATATABLE_DISPLAY_DATA, jsonReleases);

        try {
            writeJSON(request, response, jsonResult);
        } catch (IOException e) {
            log.error("Problem rendering RequestStatus", e);
        }
    }
    
    private Map<PaginationData, List<Release>> getReleaseList(PortletRequest request, PaginationData pageData) throws IOException {
        final User user = UserCacheHolder.getUserFromRequest(request);
        ComponentService.Iface client = thriftClients.makeComponentClient();
        Map<PaginationData, List<Release>> releaseSummary = Maps.newHashMap();
        try {
            releaseSummary = client.getAccessibleReleasesWithPagination(user, pageData);
            request.setAttribute(RELEASE_LIST, releaseSummary);
        }
        catch (TException e) {
            log.error("Could not search releases in backend ", e);
        }
        return releaseSummary;
    }
    
    public JSONArray getReleaseData(List<Release> releaseList, PaginationParameters releaseParameters, ResourceRequest request) {
        List<Release> sortedReleases = sortReleaseList(releaseList, releaseParameters);
        int count = getReleaseDataCount(releaseParameters, releaseList.size());
        final int start = 0;
        JSONArray releaseData = createJSONArray();
        for (int i = start; i < count; i++) {
            JSONObject jsonObject = JSONFactoryUtil.createJSONObject();
            Release release = sortedReleases.get(i);
            jsonObject.put("id", release.getId());
            jsonObject.put("DT_RowId", release.getId());
            jsonObject.put("status", nullToEmptyString(release.getEccInformation().getEccStatus()));
            jsonObject.put("name", SW360Utils.printName(release));
            jsonObject.put("version", nullToEmpty(release.getVersion()));
            jsonObject.put("group", nullToEmptyString(release.getCreatorDepartment()));
            jsonObject.put("assessor_contact_person", nullToEmptyString(release.getEccInformation().getAssessorContactPerson()));
            jsonObject.put("assessor_dept", nullToEmptyString(release.getEccInformation().getAssessorDepartment()));
            jsonObject.put("assessment_date", nullToEmptyString(release.getEccInformation().getAssessmentDate()));
            releaseData.put(jsonObject);
        }
        return releaseData;
    }
    
    public static int getReleaseDataCount(PaginationParameters releaseParameters, int maxSize) {
        if (releaseParameters.getDisplayLength() == -1) {
            return maxSize;
        } else {
            return min(releaseParameters.getDisplayStart() + releaseParameters.getDisplayLength(), maxSize);
        }
    }
    
    private List<Release> sortReleaseList(List<Release> releaseList, PaginationParameters releaseParameters) {
        boolean isAsc = releaseParameters.isAscending().orElse(true);

        switch (releaseParameters.getSortingColumn().orElse(RELEASE_DT_ROW_ECC_STATUS)) {
            case RELEASE_DT_ROW_ECC_STATUS:
                Collections.sort(releaseList, compareByECCStatus(isAsc));
                break;
            case RELEASE_DT_ROW_NAME:
                Collections.sort(releaseList, compareByName(isAsc));
                break;
            case RELEASE_DT_ROW_VERSION:
                Collections.sort(releaseList, compareByVersion(isAsc));
                break;
            case RELEASE_DT_ROW_GROUP:
                Collections.sort(releaseList, compareByCreatorGroup(isAsc));
                break;
            case RELEASE_DT_ROW_ASSESSOR_CONTACT_PERSON:
                Collections.sort(releaseList, compareByAssessorContactPerson(isAsc));
                break;
            case RELEASE_DT_ROW_ASSESSOR_DEPARTMENT:
                Collections.sort(releaseList, compareByAssessorDept(isAsc));
                break;
            case RELEASE_DT_ROW_ASSESSMENT_DATE:
                Collections.sort(releaseList, compareByAssessmentDate(isAsc));
                break;
            default:
                break;
        }

        return releaseList;
    }
    
    public static Comparator<Release> compareByECCStatus(boolean isAscending) {
        Comparator<Release> comparator = Comparator.comparing(r -> CommonUtils.nullToEmptyString(r.getEccInformation().getEccStatus()));
        return isAscending ? comparator : comparator.reversed();
    }
    
    public static Comparator<Release> compareByName(boolean isAscending) {
        Comparator<Release> comparator = Comparator.comparing(r -> CommonUtils.nullToEmptyString(r.getName()));
        return isAscending ? comparator : comparator.reversed();
    }

    public static Comparator<Release> compareByVersion(boolean isAscending) {
        Comparator<Release> comparator = Comparator.comparing(r -> CommonUtils.nullToEmptyString(r.getVersion()));
        return isAscending ? comparator : comparator.reversed();
    }
    
    public static Comparator<Release> compareByCreatorGroup(boolean isAscending) {
        Comparator<Release> comparator = Comparator.comparing(r -> CommonUtils.nullToEmptyString(r.getCreatorDepartment()));
        return isAscending ? comparator : comparator.reversed();
    }
    
    public static Comparator<Release> compareByAssessorContactPerson(boolean isAscending) {
        Comparator<Release> comparator = Comparator.comparing(r -> CommonUtils.nullToEmptyString(r.getEccInformation().getAssessorContactPerson()));
        return isAscending ? comparator : comparator.reversed();
    }
    
    public static Comparator<Release> compareByAssessorDept(boolean isAscending) {
        Comparator<Release> comparator = Comparator.comparing(r -> CommonUtils.nullToEmptyString(r.getEccInformation().getAssessorDepartment()));
        return isAscending ? comparator : comparator.reversed();
    }
    
    public static Comparator<Release> compareByAssessmentDate(boolean isAscending) {
        Comparator<Release> comparator = Comparator.comparing(r -> CommonUtils.nullToEmptyString(r.getEccInformation().getAssessmentDate()));
        return isAscending ? comparator : comparator.reversed();
    }
    
    
    


}
