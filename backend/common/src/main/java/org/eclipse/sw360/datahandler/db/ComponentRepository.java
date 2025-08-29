/*
 * Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
 * Copyright Ritankar Saha <ritankar.saha786@gmail.com>. Part of the SW360 Portal Project.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import com.ibm.cloud.cloudant.v1.model.PostFindOptions;
import org.eclipse.sw360.components.summary.ComponentSummary;
import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.couchdb.SummaryAwareRepository;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentSortColumn;
import org.eclipse.sw360.datahandler.thrift.users.User;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import com.ibm.cloud.cloudant.v1.model.PostViewOptions;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;

import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.all;
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.and;
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.eq;

/**
 * CRUD access for the Component class
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 */
public class ComponentRepository extends SummaryAwareRepository<Component> {
    private static final String ALL = "function(doc) { if (doc.type == 'component') emit(null, doc._id) }";
    private static final String BY_CREATED_ON = "function(doc) { if(doc.type == 'component') { emit(doc.createdOn, doc._id) } }";
    private static final String USED_ATTACHMENT_CONTENTS = "function(doc) { " +
            "    if(doc.type == 'release' || doc.type == 'component' || doc.type == 'project') {" +
            "        for(var i in doc.attachments){" +
            "            emit(null, doc.attachments[i].attachmentContentId);" +
            "        }" +
            "    }" +
            "}";
    private static final String MY_COMPONENTS = "function(doc) {" +
            "  if (doc.type == 'component') {" +
            "    emit(doc.createdBy, doc._id);" +
            "  } " +
            "}";
    private static final String SUBSCRIBERS = "function(doc) {" +
            "  if (doc.type == 'component') {" +
            "    for(var i in doc.subscribers) {" +
            "      emit(doc.subscribers[i], doc._id);" +
            "    }" +
            "  }" +
            "}";
    private static final String BY_NAME = "function(doc) {" +
            "  if (doc.type == 'component') {" +
            "    emit(doc.name, doc._id);" +
            "  } " +
            "}";
    private static final String BY_COMPONENT_TYPE = "function(doc) {" +
            "  if (doc.type == 'component') {" +
            "    emit(doc.componentType, doc._id);" +
            "  } " +
            "}";
    private static final String FULL_BY_NAME = "function(doc) {" +
            "  if (doc.type == 'component') {" +
            "    emit(doc.name, doc._id);" +
            "  } " +
            "}";
    private static final String BY_LINKING_RELEASE = "function(doc) {" +
            "  if (doc.type == 'release') {" +
            "    for(var i in doc.releaseIdToRelationship) {" +
            "      emit(i, doc.componentId);" +
            "    }" +
            "  }" +
            "}";
    private static final String BY_FOSSOLOGY_ID = "function(doc) {\n" +
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
    private static final String BY_EXTERNAL_IDS = "function(doc) {" +
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
    private static final String BY_DEFAULT_VENDOR_ID = "function(doc) {" +
            "  if (doc.type == 'component') {" +
            "       emit( doc.defaultVendorId , doc._id);" +
            "  }" +
            "}";

    private static final String BY_NAME_LOWERCASE = "function(doc) {" +
            "  if (doc.type == 'component') {" +
            "    emit(doc.name.toLowerCase().trim(), 1);" +
            "  } " +
            "}";

    private static final String BY_MAIN_LICENSE = "function(doc) {" +
            "    if (doc.type == 'component') {" +
            "      if(doc.mainLicenseIds) {" +
            "            emit(doc.mainLicenseIds.join(), doc._id);" +
            "      } else {" +
            "            emit('', doc._id);" +
            "      }" +
            "    }" +
            "}";

    private static final String BY_VENDOR = "function(doc) {" +
            "    if (doc.type == 'component') {" +
            "      if(doc.vendorNames) {" +
            "          emit(doc.vendorNames.join(), doc._id);" +
            "      } else {" +
            "          emit('', doc._id);" +
            "      }" +
            "    }" +
            "}";

    private static final String BY_VCS = "function(doc) {" +
            "  if (doc.type == 'component' && doc.vcs) {" +
            "    emit(doc.vcs, doc._id);" +
            "  } " +
            "}";

    private static final String BY_VCS_LOWERCASE = "function(doc) {" +
            "  if (doc.type == 'component') {" +
            "    emit(doc.vcs.toLowerCase().trim(), doc._id);" +
            "  } " +
            "}";

    private static final String COMPONENT_BY_ALL_IDX = "ComponentByAllIdx";
    private static final String BY_ATTACHMENT_CHECKSUM = "function(doc) {" +
            "  if (doc.type == 'component' && doc.attachments) {" +
            "    for(var i in doc.attachments) {" +
            "      var att = doc.attachments[i];" +
            "      if(att.sha1) emit(['sha1', att.sha1], doc._id);" +
            "      if(att.md5) emit(['md5', att.md5], doc._id);" +
            "      if(att.sha256) emit(['sha256', att.sha256], doc._id);" +
            "    }" +
            "  }" +
            "}";

    private static final String BY_FOSSOLOGY_UPLOAD_ID = "function(doc) {" +
            "  if (doc.type == 'component' && doc.attachments) {" +
            "    for(var i in doc.attachments) {" +
            "      if(doc.attachments[i].fossologyUploadId) {" +
            "        emit(doc.attachments[i].fossologyUploadId, doc._id);" +
            "      }" +
            "    }" +
            "  }" +
            "}";

    public ComponentRepository(DatabaseConnectorCloudant db, ReleaseRepository releaseRepository, VendorRepository vendorRepository) {
        super(Component.class, db, new ComponentSummary(releaseRepository, vendorRepository));
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("all", createMapReduce(ALL, null));
        views.put("byCreatedOn", createMapReduce(BY_CREATED_ON, null));
        views.put("usedAttachmentContents", createMapReduce(USED_ATTACHMENT_CONTENTS, null));
        views.put("mycomponents", createMapReduce(MY_COMPONENTS, null));
        views.put("subscribers", createMapReduce(SUBSCRIBERS, null));
        views.put("byname", createMapReduce(BY_NAME, null));
        views.put("bycomponenttype", createMapReduce(BY_COMPONENT_TYPE, null));
        views.put("fullbyname", createMapReduce(FULL_BY_NAME, null));
        views.put("byLinkingRelease", createMapReduce(BY_LINKING_RELEASE, null));
        views.put("byFossologyId", createMapReduce(BY_FOSSOLOGY_ID, null));
        views.put("byExternalIds", createMapReduce(BY_EXTERNAL_IDS, null));
        views.put("byDefaultVendorId", createMapReduce(BY_DEFAULT_VENDOR_ID, null));
        views.put("bynamelowercase", createMapReduce(BY_NAME_LOWERCASE, "_sum"));
        views.put("bymainlicense", createMapReduce(BY_MAIN_LICENSE, null));
        views.put("byvendor", createMapReduce(BY_VENDOR, null));
        views.put("byVCS", createMapReduce(BY_VCS, null));
        views.put("byVCSLowercase", createMapReduce(BY_VCS_LOWERCASE, null));
        views.put("byAttachmentChecksum", createMapReduce(BY_ATTACHMENT_CHECKSUM, null));
        views.put("byFossologyUploadId", createMapReduce(BY_FOSSOLOGY_UPLOAD_ID, null));
        initStandardDesignDocument(views, db);

        createIndex(COMPONENT_BY_ALL_IDX, "compByAll", new String[] {
                Component._Fields.NAME.getFieldName(),
                Component._Fields.CATEGORIES.getFieldName(),
                Component._Fields.COMPONENT_TYPE.getFieldName(),
                Component._Fields.LANGUAGES.getFieldName(),
                Component._Fields.SOFTWARE_PLATFORMS.getFieldName(),
                Component._Fields.OPERATING_SYSTEMS.getFieldName(),
                Component._Fields.VENDOR_NAMES.getFieldName(),
                Component._Fields.MAIN_LICENSE_IDS.getFieldName(),
                Component._Fields.CREATED_BY.getFieldName(),
                Component._Fields.CREATED_ON.getFieldName()
        }, db);
    }

    public List<Component> getRecentComponentsSummary(int limit, User user) {
        PostViewOptions.Builder queryBuilder = getConnector().getPostViewQueryBuilder(Component.class, "byCreatedOn")
                .includeDocs(true)
                .descending(true);
        if (limit >= 0){
            queryBuilder.limit(limit);
        }

        List<Component> components = new ArrayList<>(getFullDocsById(queryForIdsAsValue(queryBuilder.build())));
        return makeSummaryWithPermissionsFromFullDocs(SummaryType.SUMMARY, components, user);
    }

    public Set<String> getUsedAttachmentContents() {
        return queryForIdsAsValue(getConnector().getPostViewQueryBuilder(Component.class, "usedAttachmentContents").build());
    }

    public Collection<Component> getMyComponents(String user) {
        Set<String> componentIds = queryForIdsAsValueByPrefix("mycomponents", user);
        return getFullDocsById(componentIds);
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

    public Set<String> getComponentIdsByVCS(String vcs, boolean caseInsenstive){
        if(caseInsenstive){
            return queryForIdsAsValue("byVCSLowercase", vcs.toLowerCase());
        }
        return queryForIdsAsValue("byVCS", vcs);
    }

    public List<Component> searchComponentByName(String name, boolean caseSensitive) {
        Set<String> componentIds;
        if (caseSensitive) {
            componentIds = queryForIdsAsValueByPrefix("fullbyname", name);
        } else {
            componentIds = queryForIdsAsValueByPrefix("bynamelowercase", name);
        }
        final List<Component> componentList = new ArrayList<Component>(getFullDocsById(componentIds));
        return makeSummaryFromFullDocs(SummaryType.EXPORT_SUMMARY, componentList);
    }

    public Set<Component> getUsingComponents(String releaseId) {
        final Set<String> componentIdsByLinkingRelease = queryForIdsAsValue("byLinkingRelease", releaseId);
        return new HashSet<>(get(componentIdsByLinkingRelease));
    }

    public Component getComponentFromFossologyUploadId(String fossologyUploadId) {
        final Set<String> componentIdList = queryForIdsAsValue("byFossologyId", fossologyUploadId);
        if (componentIdList != null && !componentIdList.isEmpty())
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
        Map<PaginationData, List<Component>> result = Maps.newHashMap();
        List<Component> components = queryViewPaginated(getViewFromPagination(pageData), pageData, false);

        result.put(pageData, makeSummaryWithPermissionsFromFullDocs(SummaryType.SUMMARY, components, user));
        return result;
    }

    public Map<PaginationData, List<Component>> searchComponentByNamePrefixPaginated(User user, String name, PaginationData pageData) {
        Map<PaginationData, List<Component>> result = Maps.newHashMap();
        List<Component> components = queryByPrefixPaginated("bynamelowercase", name, pageData, true);
        result.put(pageData, makeSummaryWithPermissionsFromFullDocs(SummaryType.SUMMARY, components, user));
        return result;
    }

    public Map<PaginationData, List<Component>> searchComponentByExactNamePaginated(User user, String name, PaginationData pageData) {
        Map<PaginationData, List<Component>> result = Maps.newHashMap();
        List<Component> components = queryViewPaginated("bynamelowercase", name, pageData, true);
        result.put(pageData, makeSummaryWithPermissionsFromFullDocs(SummaryType.SUMMARY, components, user));
        return result;
    }

    /**
     * Get Components matching exact values for the given subQueryRestrictions with pagination.
     * @param subQueryRestrictions Map of field names to sets of values to match against.
     * @param user User for permission checks.
     * @param pageData Pagination data
     * @return Map containing pagination data as key and list of components as value.
     */
    public Map<PaginationData, List<Component>> searchComponentByExactValues(
            Map<String,Set<String>> subQueryRestrictions, User user, @NotNull PaginationData pageData
    ) {
        final boolean ascending = pageData.isAscending();
        final Map<String, Object> typeSelector = eq("type", "component");
        final Map<String, Object> restrictionsSelector = getQueryFromRestrictions(subQueryRestrictions);
        final Map<String, Object> finalSelector = and(List.of(typeSelector, restrictionsSelector));

        final Map<String, String> sortSelector = getSortSelector(pageData, ascending);

        PostFindOptions.Builder qb = getConnector().getQueryBuilder()
                .selector(finalSelector)
                .useIndex(Collections.singletonList(COMPONENT_BY_ALL_IDX));

        List<Component> components = getConnector().getQueryResultPaginated(
                qb, Component.class, pageData, sortSelector
        );

        return Collections.singletonMap(
                pageData, makeSummaryWithPermissionsFromFullDocs(SummaryType.SUMMARY, components, user)
        );
    }

    private static @Nonnull String getViewFromPagination(PaginationData pageData) {
        return switch (ComponentSortColumn.findByValue(pageData.getSortColumnNumber())) {
            case ComponentSortColumn.BY_CREATEDON -> "byCreatedOn";
            case ComponentSortColumn.BY_VENDOR -> "byvendor";
            case ComponentSortColumn.BY_NAME -> "byname";
            case ComponentSortColumn.BY_MAINLICENSE -> "bymainlicense";
            case ComponentSortColumn.BY_TYPE -> "bycomponenttype";
            case null -> "all";
        };
    }

    private static @NotNull Map<String, String> getSortSelector(PaginationData pageData, boolean ascending) {
        return switch (ComponentSortColumn.findByValue(pageData.getSortColumnNumber())) {
            case ComponentSortColumn.BY_VENDOR ->
                    Collections.singletonMap("vendorNames", ascending ? "asc" : "desc");
            case ComponentSortColumn.BY_TYPE ->
                    Collections.singletonMap("componentType", ascending ? "asc" : "desc");
            case ComponentSortColumn.BY_MAINLICENSE ->
                    Collections.singletonMap("mainLicenseIds", ascending ? "asc" : "desc");
            case null, default ->
                    Collections.singletonMap("name", ascending ? "asc" : "desc"); // Default sort by name
        };
    }

    public List<Component> getComponentsByVCS() {
        return queryView("byVCS");
    }

    /**
     * Get components by attachment checksum
     */
    public Set<Component> getComponentsByAttachmentChecksum(String checksum, String checksumType) {
        List<Object> key = List.of(checksumType.toLowerCase(), checksum);
        PostViewOptions viewQuery = getConnector()
                .getPostViewQueryBuilder(Component.class, "byAttachmentChecksum")
                .includeDocs(false)
                .keys(List.of(key))
                .build();
        Set<String> componentIds = queryForIdsAsValue(viewQuery);
        return new HashSet<>(get(componentIds));
    }

    /**
     * Get components by FOSSology upload ID
     */
    public Set<Component> getComponentsByFossologyUploadId(String fossologyUploadId) {
        Set<String> componentIds = queryForIdsAsValue("byFossologyUploadId", fossologyUploadId);
        return new HashSet<>(get(componentIds));
    }

    /**
     * Find components with attachments that have checksums but no FOSSology upload ID
     */
    public List<Component> getComponentsWithUnprocessedAttachments() {
        
        List<Component> allComponents = getAll();
        return allComponents.stream()
                .filter(component -> component.getAttachments() != null)
                .filter(component -> component.getAttachments().stream()
                        .anyMatch(att -> (att.getSha1() != null || att.getMd5() != null || att.getSha256() != null) 
                                      && att.getFossologyUploadId() == null))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get components by multiple checksums for batch processing
     */
    public Map<String, Set<Component>> getComponentsByChecksums(Set<String> checksums, String checksumType) {
        Map<String, Set<Component>> result = new HashMap<>();
        for (String checksum : checksums) {
            Set<Component> components = getComponentsByAttachmentChecksum(checksum, checksumType);
            if (!components.isEmpty()) {
                result.put(checksum, components);
            }
        }
        return result;
    }

    private Map<String, Object> getQueryFromRestrictions(Map<String, Set<String>> subQueryRestrictions) {
        List<Map<String, Object>> andConditions = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : subQueryRestrictions.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                if (Component._Fields.CATEGORIES.getFieldName().equals(entry.getKey()) ||
                        Component._Fields.LANGUAGES.getFieldName().equals(entry.getKey()) ||
                        Component._Fields.SOFTWARE_PLATFORMS.getFieldName().equals(entry.getKey()) ||
                        Component._Fields.OPERATING_SYSTEMS.getFieldName().equals(entry.getKey()) ||
                        Component._Fields.VENDOR_NAMES.getFieldName().equals(entry.getKey()) ||
                        Component._Fields.MAIN_LICENSE_IDS.getFieldName().equals(entry.getKey())
                ) {
                    andConditions.add(all(entry.getKey(), entry.getValue().stream().toList()));
                } else if (!entry.getValue().stream().findFirst().orElse("").isEmpty()) {
                    andConditions.add(eq(entry.getKey(), entry.getValue().stream().findFirst().get()));
                }
            }
        }
        return and(andConditions);
    }
}
