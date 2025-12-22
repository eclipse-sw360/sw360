/*
 * Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import com.ibm.cloud.cloudant.v1.model.PostFindOptions;
import org.eclipse.sw360.components.summary.ReleaseSummary;
import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.couchdb.SummaryAwareRepository;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.components.ClearingState;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseSortColumn;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.PaginationData;

import java.util.*;
import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import com.ibm.cloud.cloudant.v1.model.PostViewOptions;
import com.ibm.cloud.cloudant.v1.model.ViewResult;

import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.and;
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.elemMatch;
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.eq;
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.in;

import com.google.common.collect.Maps;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * CRUD access for the Release class
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author stefan.jaeger@evosoft.com
 */
@Component
public class ReleaseRepository extends SummaryAwareRepository<Release> {

    private static final String ALL = "function(doc) { if (doc.type == 'release') emit(null, doc._id) }";
    private static final String BY_NAME = "function(doc) { if(doc.type == 'release') { emit(doc.name, doc._id) } }";
    private static final String BY_CREATED_ON = "function(doc) { if(doc.type == 'release') { emit(doc.createdOn, doc._id) } }";
    private static final String SUBSCRIBERS = "function(doc) {" +
            " if (doc.type == 'release'){" +
            "    for(var i in doc.subscribers) {" +
            "      emit(doc.subscribers[i], doc._id);" +
            "    }" +
            "  }" +
            "}";
    private static final String USED_IN_RELEASE_RELATION = "function(doc) {" +
            " if(doc.type == 'release') {" +
            "   for(var id in doc.releaseIdToRelationship) {" +
            "     emit(id, doc._id);" +
            "   }" +
            " }" +
            "}";
    private static final String RELEASE_BY_VENDOR_ID = "function(doc) {" +
            " if (doc.type == 'release'){" +
            "     emit(doc.vendorId, doc._id);" +
            "  }" +
            "}";
    private static final String RELEASES_BY_COMPONENT_ID = "function(doc) {" +
            " if (doc.type == 'release'){" +
            "      emit(doc.componentId, doc._id);" +
            "  }" +
            "}";

    private static final String RELEASE_IDS_BY_VENDOR_ID = "function(doc) {" +
            " if (doc.type == 'release'){" +
            "      emit(doc.vendorId, doc._id);" +
            "  }" +
            "}";

    private static final String RELEASE_IDS_BY_LICENSE_ID = "function(doc) {" +
            "  if (doc.type == 'release'){" +
            "    for(var i in doc.mainLicenseIds) {" +
            "      emit(doc.mainLicenseIds[i], doc._id);" +
            "    }" +
            "  }" +
              "}";
    private static final String BY_EXTERNAL_IDS = "function(doc) {" +
            "  if (doc.type == 'release') {" +
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

    private static final String BY_LOWERCASE_RELEASE_CPE_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'release' && doc.cpeid != null) {" +
                    "    emit(doc.cpeid.toLowerCase(), doc._id);" +
                    "  } " +
                    "}";

    private static final String RELEASES_BY_SVM_ID_RELEASE_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'release' && doc.externalIds) {" +
                    "    var svmId = doc.externalIds['" + SW360Constants.SVM_COMPONENT_ID + "'];" +
                    "    if (svmId != null) {" +
                    "      emit(svmId, doc._id);" +
                    "    }" +
                    "  } " +
                    "}";

    private static final String BY_LOWERCASE_RELEASE_NAME_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'release' && doc.name != null) {" +
                    "    emit(doc.name.toLowerCase(), doc._id);" +
                    "  } " +
                    "}";

    private static final String BY_LOWERCASE_RELEASE_VERSION_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'release' && doc.version != null) {" +
                    "    emit(doc.version.toLowerCase(), doc._id);" +
                    "  } " +
                    "}";
    private static final String BY_STATUS_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'release') {" +
                    "    emit(doc.eccInformation.eccStatus, doc._id);" +
                    "  } " +
                    "}";

    private static final String BY_ASSESSOR_CONTACT_PERSON_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'release') {" +
                    "    emit(doc.eccInformation.assessorContactPerson, doc._id);" +
                    "  } " +
                    "}";

    private static final String BY_ASSESSOR_DEPARTMENT_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'release') {" +
                    "    emit(doc.eccInformation.assessorDepartment, doc._id);" +
                    "  } " +
                    "}";

    private static final String BY_ASSESSMENT_DATE_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'release') {" +
                    "    emit(doc.eccInformation.assessmentDate, doc._id);" +
                    "  } " +
                    "}";

    private static final String BY_CREATOR_DEPARTMENT_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'release') {" +
                    "    emit(doc.creatorDepartment, doc._id);" +
                    "  } " +
                    "}";

    private static final String RELEASE_BY_ALL_IDX = "ReleaseByAllIdx";
    private static final String RELEASE_BY_ATTACHMENT_TYPE_IDX = "ReleaseByAttachmentTypeIdx";

    @Autowired
    public ReleaseRepository(
            @Qualifier("CLOUDANT_DB_CONNECTOR_DATABASE") DatabaseConnectorCloudant db,
            ReleaseSummary releaseSummary
    ) {
        super(Release.class, db, releaseSummary);
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("all", createMapReduce(ALL, null));
        views.put("byname", createMapReduce(BY_NAME, null));
        views.put("byCreatedOn", createMapReduce(BY_CREATED_ON, null));
        views.put("subscribers", createMapReduce(SUBSCRIBERS, null));
        views.put("usedInReleaseRelation", createMapReduce(USED_IN_RELEASE_RELATION, null));
        views.put("releaseByVendorId", createMapReduce(RELEASE_BY_VENDOR_ID, null));
        views.put("releasesByComponentId", createMapReduce(RELEASES_BY_COMPONENT_ID, null));
        views.put("releaseIdsByLicenseId", createMapReduce(RELEASE_IDS_BY_LICENSE_ID, null));
        views.put("byExternalIds", createMapReduce(BY_EXTERNAL_IDS, null));
        views.put("releaseByCpeId", createMapReduce(BY_LOWERCASE_RELEASE_CPE_VIEW, null));
        views.put("releaseBySvmId", createMapReduce(RELEASES_BY_SVM_ID_RELEASE_VIEW, null));
        views.put("releaseByName", createMapReduce(BY_LOWERCASE_RELEASE_NAME_VIEW, null));
        views.put("releaseByVersion", createMapReduce(BY_LOWERCASE_RELEASE_VERSION_VIEW, null));
        views.put("releaseIdsByVendorId", createMapReduce(RELEASE_IDS_BY_VENDOR_ID, null));
        views.put("byStatus", createMapReduce(BY_STATUS_VIEW, null));
        views.put("byECCAssessorContactPerson", createMapReduce(BY_ASSESSOR_CONTACT_PERSON_VIEW, null));
        views.put("byECCAssessorGroup", createMapReduce(BY_ASSESSOR_DEPARTMENT_VIEW, null));
        views.put("byECCAssessmentDate", createMapReduce(BY_ASSESSMENT_DATE_VIEW, null));
        views.put("byCreatorGroup", createMapReduce(BY_CREATOR_DEPARTMENT_VIEW, null));
        initStandardDesignDocument(views, db);

        createIndex(RELEASE_BY_ALL_IDX, "releaseByAll", new String[] {
                Release._Fields.NAME.getFieldName(),
                Release._Fields.CREATED_ON.getFieldName(),
                Release._Fields.VERSION.getFieldName()
        }, db);

        createIndex(RELEASE_BY_ATTACHMENT_TYPE_IDX, "releaseByAttachmentType", new String[] {
                Release._Fields.TYPE.getFieldName(),
                Release._Fields.CLEARING_STATE.getFieldName(),
                Release._Fields.ATTACHMENTS.getFieldName() + "." + Attachment._Fields.ATTACHMENT_TYPE.getFieldName()
        }, db);
    }

    public List<Release> searchByNamePrefix(String name) {
        return makeSummary(SummaryType.SHORT, queryForIdsByPrefix("byname", name));
    }

    public Map<PaginationData, List<Release>> searchReleaseByNamePaginated(String name, PaginationData pageData) {
        final Map<String, Object> typeSelector = eq("type", "release");
        final Map<String, Object> finalSelector;
        if (CommonUtils.isNotNullEmptyOrWhitespace(name)) {
            final Map<String, Object> restrictionsSelector = eq(Release._Fields.NAME.getFieldName(), name);
            finalSelector = and(List.of(typeSelector, restrictionsSelector));
        } else {
            finalSelector = and(List.of(typeSelector));
        }

        final Map<String, String> sortSelector = getSortSelector(pageData);

        PostFindOptions.Builder qb = getConnector().getQueryBuilder()
                .selector(finalSelector)
                .useIndex(Collections.singletonList(RELEASE_BY_ALL_IDX));

        List<Release> releases = getConnector().getQueryResultPaginated(
                qb, Release.class, pageData, sortSelector
        );

        return Collections.singletonMap(
                pageData, makeSummaryFromFullDocs(SummaryType.SHORT, releases)
        );
    }

    public Map<PaginationData, List<Release>> getAccessibleNewReleasesWithSrc(PaginationData pageData) {
        final Map<String, Object> typeSelector = eq("type", "release");
        final Map<String, Object> clearingStateNew = eq(Release._Fields.CLEARING_STATE.getFieldName(),
                ClearingState.NEW_CLEARING.name());
        final Map<String, Object> sourceAttachments = in(Attachment._Fields.ATTACHMENT_TYPE.getFieldName(),
                List.of(AttachmentType.SOURCE.name(), AttachmentType.SOURCE_SELF.name()));
        final Map<String, Object> attachmentFilter = elemMatch(Release._Fields.ATTACHMENTS.getFieldName(),
                sourceAttachments);

        final Map<String, Object> finalSelector = and(List.of(typeSelector, clearingStateNew, attachmentFilter));

        final Map<String, String> sortSelector = getSortSelector(pageData);

        PostFindOptions.Builder qb = getConnector().getQueryBuilder()
                .selector(finalSelector)
                .useIndex(Collections.singletonList(RELEASE_BY_ATTACHMENT_TYPE_IDX));

        List<Release> releases = getConnector().getQueryResultPaginated(
                qb, Release.class, pageData, sortSelector
        );

        return Collections.singletonMap(
                pageData, makeSummaryFromFullDocs(SummaryType.SHORT, releases)
        );
    }

    public List<Release> searchByNameAndVersion(String name, String version, boolean caseInsenstive){
        List<Release> releasesMatchingName;
        if (caseInsenstive) {
            releasesMatchingName = new ArrayList<Release>(queryView("releaseByName", name.toLowerCase()));
        } else {
            releasesMatchingName = new ArrayList<Release>(queryView("byname", name));
        }
        List<Release> releasesMatchingNameAndVersion = releasesMatchingName.stream()
                .filter(r -> isNullOrEmpty(version) ? isNullOrEmpty(r.getVersion()) : version.equalsIgnoreCase(r.getVersion()))
                .collect(Collectors.toList());
        return releasesMatchingNameAndVersion;
    }

    public List<Release> getReleaseSummary() {
        return makeSummary(SummaryType.SUMMARY, getAllIds());
    }

    public List<Release> getRecentReleases() {
        // Get the 5 last documents
        PostViewOptions query = getConnector().getPostViewQueryBuilder(Release.class, "byCreatedOn")
                .limit(5).descending(true).includeDocs(false).build();
        return makeSummary(SummaryType.SHORT, queryForIds(query));
    }

    public List<Release> getSubscribedReleases(String email) {
        Set<String> ids = queryForIds("subscribers", email);
        return makeSummary(SummaryType.SHORT, ids);
    }

    public List<Release> getReleasesFromVendorId(String id, User user) {
        Set<String> releaseIds = queryForIdsAsValue("releaseByVendorId", id);
        return makeSummaryWithPermissionsFromFullDocs(SummaryType.SUMMARY,
                new ArrayList<Release>(getFullDocsById(releaseIds)), user);
    }

    public List<Release> getReleasesFromComponentId(String id) {
         Set<String> releaseIds = queryForIdsAsValue("releasesByComponentId", id);
         return new ArrayList<Release>(getFullDocsById(releaseIds));
    }

    public List<Release> getReleasesFullDocsFromComponentId(String id, User user) {
        Set<String> releaseIds = queryForIdsAsValue("releasesByComponentId", id);
        return new ArrayList<Release>(getFullDocsById(releaseIds));
    }

    public List<Release> getReleasesFromComponentId(String id, User user) {
        Set<String> releaseIds = queryForIdsAsValue("releasesByComponentId", id);
        return makeSummaryWithPermissionsFromFullDocs(SummaryType.SUMMARY,
                new ArrayList<Release>(getFullDocsById(releaseIds)), user);
    }

    public List<String> getReleaseIdsFromComponentId(String id, User user) {
        Set<String> releaseIds = queryForIdsAsValue("releasesByComponentId", id);
        return new ArrayList<String>(releaseIds);
    }

    public List<Release> getReleasesIgnoringNotFound(Collection<String> ids) {
        return getConnector().get(Release.class, ids, true);
    }

    public List<Release> getReleasesFromVendorIds(Set<String> ids) {
        Set<String> releaseIds = queryForIdsAsValue("releaseByVendorId", ids);
        return makeSummaryFromFullDocs(SummaryType.SHORT, new ArrayList<Release>(getFullDocsById(releaseIds)));
    }

    public Set<String> getReleaseIdsFromVendorIds(Set<String> ids) {
    	PostViewOptions query = getConnector().getPostViewQueryBuilder(Release.class, "releaseIdsByVendorId")
                .keys(ids.stream().map(r -> (Object)r).toList())
                .build();
        return queryForIds(query);
    }

    public Set<Release> getReleasesByVendorId(String vendorId) {
        Set<String> releaseIds = queryForIdsAsValue("releaseByVendorId", vendorId);
        return getFullDocsById(releaseIds);
    }

    public List<Release> searchReleasesByUsingLicenseId(String licenseId) {
        Set<String> releaseIds = queryForIdsAsValue("releaseIdsByLicenseId", licenseId);
        return new ArrayList<Release>(getFullDocsById(releaseIds));
    }

    public Set<String> getReleaseIdsBySvmId(String svmId) {
        return queryForIdsAsValue("releaseBySvmId", svmId != null ? svmId.toLowerCase() : svmId);
    }

    public Set<String> getReleaseByLowercaseCpe(String cpeid) {
        return queryForIdsAsValue("releaseByCpeId", cpeid != null ? cpeid.toLowerCase() : cpeid);
    }

    public Set<String> getReleaseByLowercaseNamePrefix(String namePrefix) {
        return queryForIdsByPrefix("releaseByName", namePrefix != null ? namePrefix.toLowerCase() : namePrefix);
    }

    public Set<String> getReleaseByLowercaseVersionPrefix(String versionPrefix) {
        return queryForIdsByPrefix("releaseByVersion", versionPrefix != null ? versionPrefix.toLowerCase() : versionPrefix);
    }

    public Set<Release> searchByExternalIds(Map<String, Set<String>> externalIds) {
        RepositoryUtils repositoryUtils = new RepositoryUtils();
        Set<String> searchIds = repositoryUtils.searchByExternalIds(this, "byExternalIds", externalIds);
        return new HashSet<>(get(searchIds));
    }

    public List<Release> getReferencingReleases(String releaseId) {
        return queryView("usedInReleaseRelation", releaseId);
    }

    public Map<PaginationData, List<Release>> getAccessibleReleasesWithPagination(User user, PaginationData pageData) {
        final int rowsPerPage = pageData.getRowsPerPage();
        Map<PaginationData, List<Release>> result = Maps.newHashMap();
        List<Release> releases = Lists.newArrayList();
        final boolean ascending = pageData.isAscending();
        final int sortColumnNo = pageData.getSortColumnNumber();

        PostViewOptions.Builder query;
        switch (sortColumnNo) {
            case -1:
                query = getConnector().getPostViewQueryBuilder(Release.class, "byCreatedOn");
                break;
            case 0:
                query = getConnector().getPostViewQueryBuilder(Release.class, "byStatus");
                break;
            case 1:
                query = getConnector().getPostViewQueryBuilder(Release.class, "byname");
                break;
            case 2:
                query = getConnector().getPostViewQueryBuilder(Release.class, "releaseByVersion");
                break;
            case 3:
                query = getConnector().getPostViewQueryBuilder(Release.class, "byCreatorGroup");
                break;
            case 4:
                query = getConnector().getPostViewQueryBuilder(Release.class, "byECCAssessorContactPerson");
                break;
            case 5:
                query = getConnector().getPostViewQueryBuilder(Release.class, "byECCAssessorGroup");
                break;
            case 6:
                query = getConnector().getPostViewQueryBuilder(Release.class, "byECCAssessmentDate");
                break;
            default:
                query = getConnector().getPostViewQueryBuilder(Release.class, "all");
                break;
        }

        PostViewOptions request = null;
        if (rowsPerPage == -1) {
            request = query.descending(!ascending).includeDocs(true).build();
        } else {
            request = query.limit(rowsPerPage).skip(pageData.getDisplayStart())
                    .descending(!ascending).includeDocs(true).build();
        }

        try {
            ViewResult response = getConnector().getPostViewQueryResponse(request);
            if (response != null) {
                releases = getPojoFromViewResponse(response);
                pageData.setTotalRowCount(response.getTotalRows());
            }
        } catch (Exception e) {
            log.error("Error getting recent releases", e);
        }
        releases = makeSummaryWithPermissionsFromFullDocs(SummaryType.SUMMARY, releases, user);
        result.put(pageData, releases);
        return result;
    }

    private static @NotNull Map<String, String> getSortSelector(PaginationData pageData) {
        boolean ascending = pageData.isAscending();
        return switch (ReleaseSortColumn.findByValue(pageData.getSortColumnNumber())) {
            case ReleaseSortColumn.BY_NAME ->
                    Collections.singletonMap(Release._Fields.NAME.getFieldName(), ascending ? "asc" : "desc");
            case ReleaseSortColumn.BY_VERSION ->
                    Collections.singletonMap(Release._Fields.VERSION.getFieldName(), ascending ? "asc" : "desc");
            case null, default ->
                    Collections.singletonMap(Release._Fields.CREATED_ON.getFieldName(), ascending ? "asc" : "desc"); // Default sort by creation date
        };
    }
}
