/*
 * Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import org.eclipse.sw360.components.summary.ComponentSummary;
import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.couchdb.SummaryAwareRepository;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.users.User;

import com.cloudant.client.api.model.DesignDocument.MapReduce;
import com.cloudant.client.api.views.Key;
import com.cloudant.client.api.views.UnpaginatedRequestBuilder;
import com.cloudant.client.api.views.ViewRequest;
import com.cloudant.client.api.views.ViewRequestBuilder;
import com.cloudant.client.api.views.ViewResponse;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.*;

/**
 * CRUD access for the Component class
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 */
public class ComponentRepository extends SummaryAwareRepository<Component> {
    private static final String ALL = "function(doc) { if (doc.type == 'component') emit(null, doc) }";
    private static final String BYCREATEDON = "function(doc) { if(doc.type == 'component') { emit(doc.createdOn, doc) } }";
    private static final String USEDATTACHMENTCONTENTS = "function(doc) { " +
            "    if(doc.type == 'release' || doc.type == 'component' || doc.type == 'project') {" +
            "        for(var i in doc.attachments){" +
            "            emit(null, doc.attachments[i].attachmentContentId);" +
            "        }" +
            "    }" +
            "}";
    private static final String MYCOMPONENTS = "function(doc) {" +
            "  if (doc.type == 'component') {" +
            "    emit(doc.createdBy, doc);" +
            "  } " +
            "}";
    private static final String SUBSCRIBERS = "function(doc) {" +
            "  if (doc.type == 'component') {" +
            "    for(var i in doc.subscribers) {" +
            "      emit(doc.subscribers[i], doc._id);" +
            "    }" +
            "  }" +
            "}";
    private static final String BYNAME = "function(doc) {" +
            "  if (doc.type == 'component') {" +
            "    emit(doc.name, doc._id);" +
            "  } " +
            "}";
    private static final String BYCOMPONENTTYPE = "function(doc) {" +
            "  if (doc.type == 'component') {" +
            "    emit(doc.componentType, doc._id);" +
            "  } " +
            "}";
    private static final String FULLBYNAME = "function(doc) {" +
            "  if (doc.type == 'component') {" +
            "    emit(doc.name, doc);" +
            "  } " +
            "}";
    private static final String BYLINKINGRELEASE = "function(doc) {" +
            "  if (doc.type == 'release') {" +
            "    for(var i in doc.releaseIdToRelationship) {" +
            "      emit(i, doc.componentId);" +
            "    }" +
            "  }" +
            "}";
    private static final String BYFOSSOLOGYID = "function(doc) {\n" +
            "  if (doc.type == 'release') {\n" +
            "    if (Array.isArray(doc.externalToolProcesses)) {\n" +
            "      for (var i = 0; i < doc.externalToolProcesses.length; i++) {\n" +
            "        externalToolProcess = doc.externalToolProcesses[i];\n" +
            "        if (externalToolProcess.externalTool === 'FOSSOLOGY' && Array.isArray(externalToolProcess.processSteps)) {\n" +
            "          for (var j = 0; j < externalToolProcess.processSteps.length; j++) {\n" +
            "            processStep = externalToolProcess.processSteps[j];\n" +
            "            if (processStep.stepName === '01_upload' && processStep.processStepIdInTool > 0) {\n" +
            "              emit(processStep.processStepIdInTool, doc.componentId);\n" +
            "              break;\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
    private static final String BYEXTERNALIDS = "function(doc) {" +
            "  if (doc.type == 'component') {" +
            "    for (var externalId in doc.externalIds) {" +
            "      try {" +
            "            var values = JSON.parse(doc.externalIds[externalId]);" +
            "            if(!isNaN(values)) {" +
            "               emit( [externalId, doc.externalIds[externalId]], doc._id);" +
            "               continue;" +
            "            }" +
            "            for (var idx in values) {" +
            "              emit( [externalId, values[idx]], doc._id);" +
            "            }" +
            "      } catch(error) {" +
            "          emit( [externalId, doc.externalIds[externalId]], doc._id);" +
            "      }" +
            "    }" +
            "  }" +
            "}";
    private static final String BYDEFAULTVENDORID = "function(doc) {" +
            "  if (doc.type == 'component') {" +
            "       emit( doc.defaultVendorId , doc._id);" +
            "  }" +
            "}";

    private static final String BYNAMELOWERCASE = "function(doc) {" +
            "  if (doc.type == 'component') {" +
            "    emit(doc.name.toLowerCase(), doc._id);" +
            "  } " +
            "}";

    private static final String BYMAINLICENSE = "function(doc) {" +
            "    if (doc.type == 'component') {" +
            "      if(doc.mainLicenseIds) {" +
            "            emit(doc.mainLicenseIds.join(), doc._id);" +
            "      } else {" +
            "            emit('', doc._id);" +
            "      }" +
            "    }" +
            "}";

    private static final String BYVENDOR = "function(doc) {" +
            "    if (doc.type == 'component') {" +
            "      if(doc.vendorNames) {" +
            "          emit(doc.vendorNames.join(), doc._id);" +
            "      } else {" +
            "          emit('', doc._id);" +
            "      }" +
            "    }" +
            "}";

    public ComponentRepository(DatabaseConnectorCloudant db, ReleaseRepository releaseRepository, VendorRepository vendorRepository) {
        super(Component.class, db, new ComponentSummary(releaseRepository, vendorRepository));
        Map<String, MapReduce> views = new HashMap<String, MapReduce>();
        views.put("all", createMapReduce(ALL, null));
        views.put("byCreatedOn", createMapReduce(BYCREATEDON, null));
        views.put("usedAttachmentContents", createMapReduce(USEDATTACHMENTCONTENTS, null));
        views.put("mycomponents", createMapReduce(MYCOMPONENTS, null));
        views.put("subscribers", createMapReduce(SUBSCRIBERS, null));
        views.put("byname", createMapReduce(BYNAME, null));
        views.put("bycomponenttype", createMapReduce(BYCOMPONENTTYPE, null));
        views.put("fullbyname", createMapReduce(FULLBYNAME, null));
        views.put("byLinkingRelease", createMapReduce(BYLINKINGRELEASE, null));
        views.put("byFossologyId", createMapReduce(BYFOSSOLOGYID, null));
        views.put("byExternalIds", createMapReduce(BYEXTERNALIDS, null));
        views.put("byDefaultVendorId", createMapReduce(BYDEFAULTVENDORID, null));
        views.put("bynamelowercase", createMapReduce(BYNAMELOWERCASE, null));
        views.put("bymainlicense", createMapReduce(BYMAINLICENSE, null));
        views.put("byvendor", createMapReduce(BYVENDOR, null));
        initStandardDesignDocument(views, db);
    }

    public List<Component> getRecentComponentsSummary(int limit, User user) {
        ViewRequestBuilder query = getConnector().createQuery(Component.class, "byCreatedOn");
        UnpaginatedRequestBuilder<String, Object> unPagnReques = query.newRequest(Key.Type.STRING, Object.class).includeDocs(true).descending(true);
        if (limit >= 0){
            unPagnReques.limit(limit);
        }
        List<Component> components = queryView(unPagnReques);

        return makeSummaryWithPermissionsFromFullDocs(SummaryType.SUMMARY, components, user);
    }

    public Set<String> getUsedAttachmentContents() {
        return queryForIdsAsValue(getConnector().createQuery(Component.class, "usedAttachmentContents"));
    }

    public Collection<Component> getMyComponents(String user) {
        return queryByPrefix("mycomponents", user);
    }

    public List<Component> getSubscribedComponents(String user) {
        Set<String> ids = queryForIds("subscribers", user);
        return makeSummary(SummaryType.SHORT, ids);
    }

    public List<Component> getSummaryForExport() {
        final List<Component> componentList = getAll();
        return makeSummaryFromFullDocs(SummaryType.EXPORT_SUMMARY, componentList);
    }

    public List<Component> getDetailedSummaryForExport() {
        final List<Component> componentList = getAll();
        return makeSummaryFromFullDocs(SummaryType.DETAILED_EXPORT_SUMMARY, componentList);
    }

    public List<Component> getComponentSummary(User user) {
        final List<Component> componentList = getAll();
        return makeSummaryWithPermissionsFromFullDocs(SummaryType.SUMMARY, componentList, user);
    }

    public Set<String> getComponentIdsByName(String name, boolean caseInsenstive) {
        if(caseInsenstive) {
            return queryForIdsAsValue("bynamelowercase", name.toLowerCase());
        }
        return queryForIdsAsValue("byname", name);
    }

    public List<Component> searchByNameForExport(String name) {
        final List<Component> componentList = queryByPrefix("fullbyname", name);
        return makeSummaryFromFullDocs(SummaryType.EXPORT_SUMMARY, componentList);
    }

    public Set<Component> getUsingComponents(String releaseId) {
        final Set<String> componentIdsByLinkingRelease = queryForIdsAsValue("byLinkingRelease", releaseId);
        return new HashSet<>(get(componentIdsByLinkingRelease));
    }

    public Component getComponentFromFossologyUploadId(String fossologyUploadId) {
        final Set<String> componentIdList = queryForIdsAsValue("byFossologyId", fossologyUploadId);
        if (componentIdList != null && componentIdList.size() > 0)
            return get(CommonUtils.getFirst(componentIdList));
        return null;
    }

    public Set<Component> getUsingComponents(Set<String> releaseIds) {
        final Set<String> componentIdsByLinkingRelease = queryForIdsAsValue("byLinkingRelease", releaseIds);
        return new HashSet<>(get(componentIdsByLinkingRelease));
    }

    public Set<Component> getComponentsByDefaultVendorId(String defaultVendorId) {
        final Set<String> componentIds = queryForIdsAsValue("byDefaultVendorId", defaultVendorId);
        return new HashSet<>(get(componentIds));
    }

    public Set<Component> searchByExternalIds(Map<String, Set<String>> externalIds) {
        RepositoryUtils repositoryUtils = new RepositoryUtils();
        Set<String> searchIds = repositoryUtils.searchByExternalIds(this, "byExternalIds", externalIds);
        return new HashSet<>(get(searchIds));
    }

    public Map<PaginationData, List<Component>> getRecentComponentsSummary(User user, PaginationData pageData) {
        final int rowsPerPage = pageData.getRowsPerPage();
        Map<PaginationData, List<Component>> result = Maps.newHashMap();
        List<Component> components = Lists.newArrayList();
        final boolean ascending = pageData.isAscending();
        final int sortColumnNo = pageData.getSortColumnNumber();

        ViewRequestBuilder query;
        switch (sortColumnNo) {
        case -1:
            query = getConnector().createQuery(Component.class, "byCreatedOn");
            break;
        case 0:
            query = getConnector().createQuery(Component.class, "byvendor");
            break;
        case 1:
            query = getConnector().createQuery(Component.class, "byname");
            break;
        case 2:
            query = getConnector().createQuery(Component.class, "bymainlicense");
            break;
        case 3:
            query = getConnector().createQuery(Component.class, "bycomponenttype");
            break;
        default:
            query = getConnector().createQuery(Component.class, "all");
            break;
        }

        ViewRequest<String, Object> request = null;
        if (rowsPerPage == -1) {
            request = query.newRequest(Key.Type.STRING, Object.class).descending(!ascending).includeDocs(true).build();
        } else {
            request = query.newPaginatedRequest(Key.Type.STRING, Object.class).rowsPerPage(rowsPerPage)
                    .descending(!ascending).includeDocs(true).build();
        }

        ViewResponse<String, Object> response = null;
        try {
            response = request.getResponse();
            int pageNo = pageData.getDisplayStart() / rowsPerPage;
            int i = 1;
            while (i <= pageNo) {
                response = response.nextPage();
                i++;
            }
            components = response.getDocsAs(Component.class);
        } catch (Exception e) {
            log.error("Error getting recent components", e);
        }
        components = makeSummaryWithPermissionsFromFullDocs(SummaryType.SUMMARY, components, user);
        pageData.setTotalRowCount(response.getTotalRowCount());
        result.put(pageData, components);
        return result;
    }
}
