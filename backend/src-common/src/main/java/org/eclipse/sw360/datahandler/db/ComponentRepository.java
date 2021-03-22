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
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.SummaryAwareRepository;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.users.User;

import org.ektorp.ViewQuery;
import org.ektorp.support.View;
import org.ektorp.support.Views;

import java.util.*;

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
        @View(name = "bynamelowercase",
                map = "function(doc) {" +
                    "  if (doc.type == 'component') {" +
                    "    emit(doc.name.toLowerCase(), doc._id);" +
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
                map = "function(doc) {\n" +
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
                    "}"),
        @View(name = "byExternalIds",
                map = "function(doc) {" +
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
                        "}"),
        @View(name = "byDefaultVendorId",
                map = "function(doc) {" +
                        "  if (doc.type == 'component') {" +
                        "       emit( doc.defaultVendorId , doc._id);" +
                        "  }" +
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
}
