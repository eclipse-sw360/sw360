/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.db;

import org.eclipse.sw360.components.summary.ComponentSummary;
import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.SummaryAwareRepository;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.ektorp.ViewQuery;
import org.ektorp.support.View;
import org.ektorp.support.Views;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CRUD access for the Component class
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 */
@Views({
        @View(name = "all",
                map = "function(doc) { if (doc.type == 'component') emit(null, doc) }"),
        @View(name = "byCreatedOn",
                map = "function(doc) { if(doc.type == 'component') { emit(doc.createdOn, doc) } }"),
        @View(name = "usedAttachmentContents",
                map = "function(doc) { " +
                    "    if(doc.type == 'release' || doc.type == 'component' || doc.type == 'project') {" +
                    "        for(var i in doc.attachments){" +
                    "            emit(null, doc.attachments[i].attachmentContentId);" +
                    "        }" +
                    "    }" +
                    "}"),
        @View(name = "mycomponents",
                map = "function(doc) {" +
                    "  if (doc.type == 'component') {" +
                    "    emit(doc.createdBy, doc);" +
                    "  } " +
                    "}"),
        @View(name = "subscribers",
                map = "function(doc) {" +
                    "  if (doc.type == 'component') {" +
                    "    for(var i in doc.subscribers) {" +
                    "      emit(doc.subscribers[i], doc._id);" +
                    "    }" +
                    "  }" +
                    "}"),
        @View(name = "byname",
                map = "function(doc) {" +
                    "  if (doc.type == 'component') {" +
                    "    emit(doc.name, doc._id);" +
                    "  } " +
                    "}"),
        @View(name = "fullbyname",
                map = "function(doc) {" +
                    "  if (doc.type == 'component') {" +
                    "    emit(doc.name, doc);" +
                    "  } " +
                    "}"),
        @View(name = "byLinkingRelease",
                map = "function(doc) {" +
                    "  if (doc.type == 'release') {" +
                    "    for(var i in doc.releaseIdToRelationship) {" +
                    "      emit(i, doc.componentId);" +
                    "    }" +
                    "  }" +
                    "}"),
        @View(name = "byFossologyId",
                map = "function(doc) {  " +
                    "  if (doc.type == 'release') { " +
                    "     if(doc.fossologyId) {    " +
                    "         emit(doc.fossologyId, doc.componentId);  " +
                    "     } " +
                    "  } " +
                    "}")
})
public class ComponentRepository extends SummaryAwareRepository<Component> {

    public ComponentRepository(DatabaseConnector db, ReleaseRepository releaseRepository, VendorRepository vendorRepository) {
        super(Component.class, db, new ComponentSummary(releaseRepository, vendorRepository));

        initStandardDesignDocument();
    }

    public List<Component> getRecentComponentsSummary(int limit, User user) {
        ViewQuery query = createQuery("byCreatedOn").includeDocs(true).descending(true);
        if (limit >= 0){
            query.limit(limit);
        }
        List<Component> components = db.queryView(query, Component.class);

        return makeSummaryWithPermissionsFromFullDocs(SummaryType.SUMMARY, components, user);
    }

    public Set<String> getUsedAttachmentContents() {
        return queryForIdsAsValue(createQuery("usedAttachmentContents"));
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

    public Set<String> getComponentIdsByName(String name) {
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
}
