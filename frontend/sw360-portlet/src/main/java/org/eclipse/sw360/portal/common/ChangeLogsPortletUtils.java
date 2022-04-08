/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.common;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.liferay.portal.kernel.json.JSONFactoryUtil.createJSONArray;
import static com.liferay.portal.kernel.json.JSONFactoryUtil.createJSONObject;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyString;
import static org.eclipse.sw360.portal.common.PortalConstants.DATATABLE_DISPLAY_DATA;
import static org.eclipse.sw360.portal.common.PortalConstants.DATATABLE_RECORDS_FILTERED;
import static org.eclipse.sw360.portal.common.PortalConstants.DATATABLE_RECORDS_TOTAL;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.portlet.PortletRequest;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.couchdb.DatabaseMixInForChangeLog.ChangeLogsMixin;
import org.eclipse.sw360.datahandler.couchdb.DatabaseMixInForChangeLog.ChangedFieldsMixin;
import org.eclipse.sw360.datahandler.couchdb.DatabaseMixInForChangeLog.ReferenceDocDataMixin;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogsService;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogsService.Iface;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocumentService;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangedFields;
import org.eclipse.sw360.datahandler.thrift.changelogs.ReferenceDocData;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.datatables.PaginationParser;
import org.eclipse.sw360.portal.common.datatables.data.PaginationParameters;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.portlet.LiferayPortletURL;
import com.liferay.portal.kernel.portlet.PortletURLFactoryUtil;
import com.liferay.portal.kernel.service.LayoutLocalServiceUtil;
import com.liferay.portal.kernel.util.PortalUtil;

public class ChangeLogsPortletUtils {
    private static final Logger log = LogManager.getLogger(ChangeLogsPortletUtils.class);

    private static final int CHANGELOGS_NO_SORT = -1;
    private static ObjectMapper mapper = initAndGetObjectMapper();
    private static final ImmutableList<ChangeLogs._Fields> changeLogsFields = ImmutableList.of(
            ChangeLogs._Fields.CHANGE_TIMESTAMP, ChangeLogs._Fields.ID, ChangeLogs._Fields.DOCUMENT_TYPE,
            ChangeLogs._Fields.USER_EDITED);

    private static final int CHANGELOG_CHANGE_TIMESTAMP = 0;
    private static final int CHANGELOG_ID = 1;
    private static final int CHANGELOG_DOCUMENT_TYPE = 2;
    private static final int CHANGELOG_USER_EDITED = 3;
    private ThriftClients thriftClients = null;

    public ChangeLogsPortletUtils() {
    }

    public ChangeLogsPortletUtils(ThriftClients clients) {
        thriftClients = clients;
    }

    public JSONObject serveResourceForChangeLogs(ResourceRequest request, ResourceResponse response, String action)
            throws IOException {
        ChangeLogsService.Iface changeLogsClient = thriftClients.makeChangeLogsClient();
        if (PortalConstants.LOAD_CHANGE_LOGS.equals(action)) {
            return serveChangeLogsList(request, response, changeLogsClient);
        } else if (PortalConstants.VIEW_CHANGE_LOGS.equals(action)) {
            ChangeLogs serveChangeLogsData = serveChangeLogsData(request, response, changeLogsClient);
            JSONObject resultData = null;
            try {
                resultData = createJSONObject(convertObjectToJsonStr(serveChangeLogsData));
            } catch (JSONException e) {
                log.error("Error while creating JSON Object : ", e);
            }
            return resultData;
        }
        return null;
    }

    private ChangeLogs serveChangeLogsData(ResourceRequest request, ResourceResponse response, Iface changeLogsClient) {
        String changeLogsId = request.getParameter("changelogid");
        try {
            return changeLogsClient.getChangeLogsById(changeLogsId);
        } catch (TException exp) {
            log.error("Error while retrieving changelogs Data : ", exp);
        }
        return null;
    }

    private JSONObject serveChangeLogsList(ResourceRequest request, ResourceResponse response,
            ChangeLogsService.Iface changeLogsClient) {
        HttpServletRequest originalServletRequest = PortalUtil
                .getOriginalServletRequest(PortalUtil.getHttpServletRequest(request));
        PaginationParameters paginationParameters = PaginationParser.parametersFrom(originalServletRequest);

        if (!paginationParameters.getSortingColumn().isPresent()) {
            for (ChangeLogs._Fields filteredField : changeLogsFields) {
                if (!isNullOrEmpty(request.getParameter(filteredField.toString()))) {
                    paginationParameters.setSortingColumn(Optional.of(CHANGELOGS_NO_SORT));
                    break;
                }
            }
        }

        List<ChangeLogs> changeLogsList = getFilteredChangeLogList(request, changeLogsClient, null);

        if (isReleaseChangesLog(changeLogsList)) {
            changeLogsList = getChangesLogsForSPDX(request, changeLogsList, changeLogsClient);
        }
        changeLogsList.stream().forEach(cl -> cl.setChangeTimestamp(cl.getChangeTimestamp().split(" ")[0]));
        JSONArray jsonProjects = getChangeLogData(changeLogsList, paginationParameters, request);
        JSONObject jsonResult = createJSONObject();
        jsonResult.put(DATATABLE_RECORDS_TOTAL, changeLogsList.size());
        jsonResult.put(DATATABLE_RECORDS_FILTERED, changeLogsList.size());
        jsonResult.put(DATATABLE_DISPLAY_DATA, jsonProjects);

        return jsonResult;
    }

    private boolean isReleaseChangesLog(List<ChangeLogs> changeLogsList) {
        for (ChangeLogs changeLogs : changeLogsList) {
            if (changeLogs.documentType.equals("release")){
                return true;
            }
        }
        return false;
    }

    private List<ChangeLogs> getChangesLogsForSPDX(ResourceRequest request, List<ChangeLogs> SPDXChangeLogsList, ChangeLogsService.Iface changeLogsClient) {
        ComponentService.Iface componentClient = thriftClients.makeComponentClient();
        SPDXDocumentService.Iface SPDXClient = thriftClients.makeSPDXClient();
        User user = UserCacheHolder.getUserFromRequest(request);
        String releaseId = request.getParameter(PortalConstants.DOCUMENT_ID);
        try {
            Release release = componentClient.getReleaseById(releaseId, user);
            String spdxId = release.getSpdxId();
            if (!isNullOrEmpty(spdxId)) {
                SPDXDocument spdxDocument = SPDXClient.getSPDXDocumentById(spdxId, user);
                String spdxDocumentCreationInfoId = spdxDocument.getSpdxDocumentCreationInfoId();
                Set<String> packageInfoIds = spdxDocument.getSpdxPackageInfoIds();
                List<ChangeLogs> spdxChangeLogsList = getFilteredChangeLogList(request, changeLogsClient, spdxId);
                SPDXChangeLogsList.addAll(spdxChangeLogsList);
                if (!isNullOrEmpty(spdxDocumentCreationInfoId)) {
                    List<ChangeLogs> spdxDocumentChangeLogsList = getFilteredChangeLogList(request, changeLogsClient, spdxDocumentCreationInfoId);
                    SPDXChangeLogsList.addAll(spdxDocumentChangeLogsList);
                }
                if (packageInfoIds != null) {
                    List<ChangeLogs> packagesChangeLogsList = Lists.newArrayList();
                    for (String packageInfoId : packageInfoIds) {
                        List<ChangeLogs> packageChangeLogsList = getFilteredChangeLogList(request, changeLogsClient, packageInfoId);
                        packagesChangeLogsList.addAll(packageChangeLogsList);
                    }
                    SPDXChangeLogsList.addAll(packagesChangeLogsList);
                }
                Collections.sort(SPDXChangeLogsList, Comparator.comparing(ChangeLogs::getChangeTimestamp).reversed());
            }
        } catch (TException e) {
            log.error("Error while getting change logs for SPDX" + e);
        }
        return SPDXChangeLogsList;
    }

    private LiferayPortletURL getModerationPortletUrl(PortletRequest request) {
        Optional<Layout> layout = LayoutLocalServiceUtil.getLayouts(QueryUtil.ALL_POS, QueryUtil.ALL_POS).stream()
                .filter(l -> ("/moderation").equals(l.getFriendlyURL())).findFirst();
        if (layout.isPresent()) {
            long plId = layout.get().getPlid();
            LiferayPortletURL modUrl = PortletURLFactoryUtil.create(request, PortalConstants.MODERATION_PORTLET_NAME,
                    plId, PortletRequest.RENDER_PHASE);
            modUrl.setParameter(PortalConstants.PAGENAME, PortalConstants.PAGENAME_EDIT);
            return modUrl;
        }
        return null;
    }

    private List<ChangeLogs> getFilteredChangeLogList(ResourceRequest request,
            ChangeLogsService.Iface changeLogsClient, String docId) {
        final User user = UserCacheHolder.getUserFromRequest(request);
        if (docId == null) {
            docId = request.getParameter(PortalConstants.DOCUMENT_ID);
        }
        try {
            return changeLogsClient.getChangeLogsByDocumentId(user, docId);
        } catch (TException exp) {
            log.error("Error while retrieving changelogs List : ", exp);
        }
        return Lists.newArrayList();
    }

    private List<ChangeLogs> sortChangeLogsList(List<ChangeLogs> changeLogsList,
            PaginationParameters changeLogsParameters) {
        boolean isAsc = changeLogsParameters.isAscending().orElse(true);

        switch (changeLogsParameters.getSortingColumn().orElse(CHANGELOG_CHANGE_TIMESTAMP)) {
        case CHANGELOG_CHANGE_TIMESTAMP:
            if (isAsc) {
                Collections.reverse(changeLogsList);
            }
            break;
        case CHANGELOG_ID:
            Collections.sort(changeLogsList, compareById(isAsc));
            break;
        case CHANGELOG_DOCUMENT_TYPE:
            Collections.sort(changeLogsList, compareByType(isAsc));
            break;
        case CHANGELOG_USER_EDITED:
            Collections.sort(changeLogsList, compareByUser(isAsc));
            break;
        default:
            break;
        }
        return changeLogsList;
    }

    private JSONArray getChangeLogData(List<ChangeLogs> changeLogsList, PaginationParameters changeLogsParameters,
            PortletRequest request) {
        List<ChangeLogs> sortedProjects = sortChangeLogsList(changeLogsList, changeLogsParameters);
        int count = PortletUtils.getProjectDataCount(changeLogsParameters, changeLogsList.size());

        JSONArray changeLogData = createJSONArray();
        for (int i = changeLogsParameters.getDisplayStart(); i < count; i++) {
            JSONObject jsonObject = JSONFactoryUtil.createJSONObject();
            ChangeLogs changeLog = sortedProjects.get(i);
            String documentType = changeLog.getDocumentType();
            jsonObject.put("id", changeLog.getId());
            jsonObject.put("changeTimestamp", changeLog.getChangeTimestamp());
            jsonObject.put("documentType", documentType);
            jsonObject.put("documentId", changeLog.documentId);
            jsonObject.put("user", changeLog.getUserEdited());

            if (documentType.equals("moderation")) {
                LiferayPortletURL moderationPortletUrl = getModerationPortletUrl(request);
                moderationPortletUrl.setParameter(PortalConstants.MODERATION_ID, changeLog.getDocumentId());
                jsonObject.put("moderationUrl", moderationPortletUrl.toString());
            }
            changeLogData.put(jsonObject);
        }

        return changeLogData;
    }

    private String convertObjectToJsonStr(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Error occured while converting Object to Json : ", e);
            throw new RuntimeException(e);
        }
    }

    private Comparator<ChangeLogs> compareByUser(boolean isAscending) {

        Comparator<ChangeLogs> comparator = Comparator.comparing(cl -> nullToEmptyString(cl.getUserEdited()));
        return isAscending ? comparator : comparator.reversed();
    }

    private Comparator<ChangeLogs> compareById(boolean isAscending) {
        Comparator<ChangeLogs> comparator = Comparator.comparing(cl -> nullToEmptyString(cl.getId()));
        return isAscending ? comparator : comparator.reversed();
    }

    private Comparator<ChangeLogs> compareByType(boolean isAscending) {
        Comparator<ChangeLogs> comparator = Comparator.comparing(cl -> nullToEmptyString(cl.getDocumentType()));
        return isAscending ? comparator : comparator.reversed();
    }

    private static ObjectMapper initAndGetObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.addMixInAnnotations(ChangeLogs.class, ChangeLogsMixin.class);
        mapper.addMixInAnnotations(ReferenceDocData.class, ReferenceDocDataMixin.class);
        mapper.addMixInAnnotations(ChangedFields.class, ChangedFieldsMixin.class);
        return mapper;
    }
}
