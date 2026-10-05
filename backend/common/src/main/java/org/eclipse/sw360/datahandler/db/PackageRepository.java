/*
 * Copyright Siemens Healthineers GmBH, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.*;
import java.util.stream.Collectors;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.packages.Package;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import com.ibm.cloud.cloudant.v1.model.PostFindOptions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.*;

/**
 * CRUD access for the Package class
 *
 * @author abdul.kapti@siemens-healthineers.com
 */

public class PackageRepository extends DatabaseRepositoryCloudantClient<Package> {

    private static final String PACKAGE_BY_ALL_IDX = "PackageByAllIdx";

    private static final String ALL = "function(doc) { if (doc.type == 'package') emit(null, doc._id) }";
    private static final String ORPHAN = "function(doc) { if (doc.type == 'package' && !doc.releaseId) emit(null, doc._id) }";
    private static final String BY_NAME = "function(doc) { if (doc.type == 'package') { emit(doc.name.trim(), doc._id) } }";
    private static final String BY_NAME_LOWERCASE = "function(doc) { if (doc.type == 'package') { emit(doc.name.toLowerCase().trim(), doc._id) } }";
    private static final String BY_PKG_MANAGER = "function(doc) { if (doc.type == 'package') { emit(doc.packageManager.toLowerCase().trim(), doc._id) } }";
    private static final String BY_CREATOR = "function(doc) { if (doc.type == 'package') { emit(doc.createdBy, doc._id) } }";
    private static final String BY_CREATED_ON = "function(doc) { if (doc.type == 'package') { emit(doc.createdOn, doc._id) } }";
    private static final String BY_RELEASE_ID = "function(doc) { if (doc.type == 'package') { emit(doc.releaseId, doc._id); } }";
    private static final String BY_LICENSE_IDS = "function(doc) { if (doc.type == 'package') { if (doc.licenseIds) { emit(doc.licenseIds.join(), doc._id); } else { emit('', doc._id); } } }";
    private static final String BY_PURL = "function(doc) { if (doc.type == 'package') { emit(doc.purl.trim(), doc._id) } }";
    private static final String BY_PURL_LOWERCASE = "function(doc) { if (doc.type == 'package') { emit(doc.purl.toLowerCase().trim(), doc._id) } }";
    private static final String BY_VERSION = "function(doc) { if (doc.type == 'package') { emit(doc.version.trim(), doc._id) } }";

    public PackageRepository(DatabaseConnectorCloudant db) {
        super(db, Package.class);
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("all", createMapReduce(ALL, null));
        views.put("orphan", createMapReduce(ORPHAN, null));
        views.put("byName", createMapReduce(BY_NAME, null));
        views.put("byNameLowerCase", createMapReduce(BY_NAME_LOWERCASE, null));
        views.put("byPackageManager", createMapReduce(BY_PKG_MANAGER, null));
        views.put("byCreator", createMapReduce(BY_CREATOR, null));
        views.put("byCreatedOn", createMapReduce(BY_CREATED_ON, null));
        views.put("byReleaseId", createMapReduce(BY_RELEASE_ID, null));
        views.put("byLicenseIds", createMapReduce(BY_LICENSE_IDS, null));
        views.put("byPurl", createMapReduce(BY_PURL, null));
        views.put("byPurlLowercase", createMapReduce(BY_PURL_LOWERCASE, null));
        views.put("byVersion", createMapReduce(BY_VERSION, null));
        initStandardDesignDocument(views, db);

        createPartialTypeIndex(PACKAGE_BY_ALL_IDX, "pkgByAll", "package", new String[] {
                Package._Fields.NAME.getFieldName(),
                Package._Fields.VERSION.getFieldName(),
                Package._Fields.PURL.getFieldName(),
                Package._Fields.CREATED_ON.getFieldName(),
                Package._Fields.CREATED_BY.getFieldName(),
                Package._Fields.LICENSE_IDS.getFieldName(),
                Package._Fields.PACKAGE_MANAGER.getFieldName(),
        }, db);
    }

    public List<Package> getOrphanPackage() {
        return queryView("orphan");
    }

    public List<Package> getPackagesByReleaseId(String id) {
        return queryView("byReleaseId", id);
    }

    public List<Package> searchByName(String name) {
        return queryView("byName", name);
    }

    public List<Package> searchByNameLowerCase(String name) {
        return queryView("byNameLowerCase", name.toLowerCase());
    }

    public List<Package> searchByPackageManager(String pkgType) {
        return queryView("byPackageManager", pkgType.toLowerCase());
    }

    public List<Package> searchByCreator(String email) {
        return queryView("byCreator", email);
    }

    public List<Package> searchByLicenseeId(String id) {
        return queryView("byLicenseIds", id);
    }

    public List<Package> searchByVersion(String version) {
        return queryView("byVersion", version);
    }

    public List<Package> searchByNameAndVersion(String name, String version, boolean caseInsenstive) {
        List<Package> packagesMatchingName;
        if (caseInsenstive) {
            packagesMatchingName = new ArrayList<Package>(queryView("byNameLowerCase", name.toLowerCase()));
        } else {
            packagesMatchingName = new ArrayList<Package>(queryView("byName", name));
        }
        List<Package> releasesMatchingNameAndVersion = packagesMatchingName.stream()
                .filter(r -> isNullOrEmpty(version) ? isNullOrEmpty(r.getVersion()) : version.equalsIgnoreCase(r.getVersion()))
                .collect(Collectors.toList());
        return releasesMatchingNameAndVersion;
    }

    public List<Package> searchByPurl(String purl, boolean caseInsenstive) {
        List<Package> packagesMatchingPurl;
        if(caseInsenstive){
            packagesMatchingPurl = new ArrayList<Package>(queryView("byPurlLowercase", purl.toLowerCase()));
        }else{
            packagesMatchingPurl = new ArrayList<Package>(queryView("byPurl", purl));
        }
        return packagesMatchingPurl;
    }

    public Map<PaginationData, List<Package>> getPackagesWithPagination(PaginationData pageData) {
        Map<PaginationData, List<Package>> result = Maps.newHashMap();
        List<Package> packages = Lists.newArrayList();
        final boolean ascending = pageData.isAscending();
        final Map<String, String> sortSelector = getSortSelector(pageData, ascending);
        final Map<String, Object> selector = eq(Package._Fields.TYPE.getFieldName(), "package");
        PostFindOptions.Builder queryBuilder = getConnector().getQueryBuilder()
                .selector(selector)
                .useIndex(Collections.singletonList(PACKAGE_BY_ALL_IDX));

        try {
             log.debug("getPackagesWithPagination: using Mango query with index '{}', sort={}, page={}, size={}",
                     PACKAGE_BY_ALL_IDX, sortSelector, pageData.getDisplayStart(), pageData.getRowsPerPage());
             packages = getConnector().getQueryResultPaginated(queryBuilder, Package.class, pageData, sortSelector, PACKAGE_BY_ALL_IDX);
             log.debug("getPackagesWithPagination: Mango query returned {} packages, totalCount={}",
                     packages.size(), pageData.getTotalRowCount());
        } catch (Exception e) {
            log.error("Error getting packages from repository: ", e);
        }
        result.put(pageData, packages);
        return result;
    }

    private static Map<String, String> getSortSelector(PaginationData pageData, boolean ascending) {
        String direction = ascending ? "asc" : "desc";
        return switch (pageData.getSortColumnNumber()) {
            case -1 -> Collections.singletonMap(Package._Fields.CREATED_ON.getFieldName(), direction);
            case 3 -> Collections.singletonMap(Package._Fields.LICENSE_IDS.getFieldName(), direction);
            case 4 -> Collections.singletonMap(Package._Fields.PACKAGE_MANAGER.getFieldName(), direction);
            case 0 -> Collections.singletonMap(Package._Fields.NAME.getFieldName(), direction);
            default -> Collections.singletonMap(Package._Fields.NAME.getFieldName(), direction);
        };
    }
    public List<Package> getPackagesByReleaseIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return queryByIds("byReleaseId", ids);
    }
}