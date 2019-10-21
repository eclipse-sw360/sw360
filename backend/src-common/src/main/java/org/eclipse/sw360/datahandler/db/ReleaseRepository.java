/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.db;

import org.eclipse.sw360.components.summary.ReleaseSummary;
import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.SummaryAwareRepository;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.ektorp.ViewQuery;
import org.ektorp.support.View;
import org.ektorp.support.Views;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * CRUD access for the Release class
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 */
@Views({
        @View(name = "all",
                map = "function(doc) { if (doc.type == 'release') emit(null, doc._id) }"),
        @View(name = "byname",
                map = "function(doc) { if(doc.type == 'release') { emit(doc.name, doc) } }"),
        @View(name = "byCreatedOn",
                map = "function(doc) { if(doc.type == 'release') { emit(doc.createdOn, doc._id) } }"),
        @View(name = "subscribers",
                map = "function(doc) {" +
                    " if (doc.type == 'release'){" +
                    "    for(var i in doc.subscribers) {" +
                    "      emit(doc.subscribers[i], doc._id);" +
                    "    }" +
                    "  }" +
                    "}"),
        @View(name = "usedInReleaseRelation",
                map = "function(doc) {" +
                    " if(doc.type == 'release') {" +
                    "   for(var id in doc.releaseIdToRelationship) {" +
                    "     emit(id, doc);" + 
                    "   }" + 
                    " }" +
                    "}"),
        @View(name = "releaseByVendorId",
                map = "function(doc) {" +
                    " if (doc.type == 'release'){" +
                    "     emit(doc.vendorId, doc);" +
                    "  }" +
                    "}"),
        @View(name = "releasesByComponentId",
                map = "function(doc) {" +
                    " if (doc.type == 'release'){" +
                    "      emit(doc.componentId, doc);" +
                    "  }" +
                    "}"),
        @View(name = "releaseIdsByLicenseId",
                map = "function(doc) {" +
                      "  if (doc.type == 'release'){" +
                      "    for(var i in doc.mainLicenseIds) {" +
                      "      emit(doc.mainLicenseIds[i], doc);" +
                      "    }" +
                      "  }" +
                        "}"),
        @View(name = "byExternalIds",
                map = "function(doc) {" +
                        "  if (doc.type == 'release') {" +
                        "    for (var externalId in doc.externalIds) {" +
                        "       emit( [externalId, doc.externalIds[externalId]] , doc._id);" +
                        "    }" +
                        "  }" +
                        "}")

})
public class ReleaseRepository extends SummaryAwareRepository<Release> {

    public ReleaseRepository(DatabaseConnector db, VendorRepository vendorRepository) {
        super(Release.class, db, new ReleaseSummary(vendorRepository));

        initStandardDesignDocument();
    }

    public List<Release> searchByNamePrefix(String name) {
        return makeSummary(SummaryType.SHORT, queryForIdsByPrefix("byname", name));
    }

    public List<Release> searchByNameAndVersion(String name, String version){
        List<Release> releasesMatchingName = queryView("byname", name);
        List<Release> releasesMatchingNameAndVersion = releasesMatchingName.stream()
                .filter(r -> isNullOrEmpty(version) ? isNullOrEmpty(r.getVersion()) : version.equals(r.getVersion()))
                .collect(Collectors.toList());
        return releasesMatchingNameAndVersion;
    }

    public List<Release> getReleaseSummary() {
        return makeSummary(SummaryType.SUMMARY, getAllIds());
    }

    public List<Release> getRecentReleases() {
        ViewQuery query = createQuery("byCreatedOn");
        // Get the 5 last documents
        query.limit(5).descending(true).includeDocs(false);
        return makeSummary(SummaryType.SHORT, queryForIds(query));
    }

    public List<Release> getSubscribedReleases(String email) {
        Set<String> ids = queryForIds("subscribers", email);
        return makeSummary(SummaryType.SHORT, ids);
    }

    public List<Release> getReleasesFromVendorId(String id, User user) {
        return  makeSummaryWithPermissionsFromFullDocs(SummaryType.SUMMARY, queryView("releaseByVendorId", id), user);
    }

    public List<Release> getReleasesFromComponentId(String id) {
         return queryView("releasesByComponentId", id);
    }

    public List<Release> getReleasesFromComponentId(String id, User user) {
        return makeSummaryWithPermissionsFromFullDocs(SummaryType.SUMMARY, queryView("releasesByComponentId", id), user);
    }

    public List<Release> getReleasesFromVendorIds(Set<String> ids) {
        return makeSummaryFromFullDocs(SummaryType.SHORT, queryByIds("releaseByVendorId", ids));
    }

    public Set<Release> getReleasesByVendorId(String vendorId) {
        return new HashSet<>(queryView("releaseByVendorId", vendorId));
    }

    public List<Release> searchReleasesByUsingLicenseId(String licenseId) {
        return queryView("releaseIdsByLicenseId", licenseId);
    }

    public Set<Release> searchByExternalIds(Map<String, Set<String>> externalIds) {
        RepositoryUtils repositoryUtils = new RepositoryUtils();
        Set<String> searchIds = repositoryUtils.searchByExternalIds(this, "byExternalIds", externalIds);
        return new HashSet<>(get(searchIds));
    }

    public List<Release> getReferencingReleases(String releaseId) {
        return queryView("usedInReleaseRelation", releaseId);
    }
}
