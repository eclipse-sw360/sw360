/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.spdx;

import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.ProjectDatabaseHandler;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Component;

import org.eclipse.sw360.datahandler.db.spdx.document.SpdxDocumentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.spdx.documentcreationinfo.SpdxDocumentCreationInfoDatabaseHandler;
import org.eclipse.sw360.datahandler.db.spdx.packageinfo.SpdxPackageInfoDatabaseHandler;

import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.thrift.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.spdxpackageinfo.PackageInformation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.net.MalformedURLException;
import java.util.*;
import java.util.stream.Collectors;

public class SpdxBOMImporterSink {
    private static final Logger log = LogManager.getLogger(SpdxBOMImporterSink.class);

    private final ProjectDatabaseHandler projectDatabaseHandler;
    private final ComponentDatabaseHandler componentDatabaseHandler;
    private final User user;

    public SpdxBOMImporterSink(User user, ProjectDatabaseHandler projectDatabaseHandler, ComponentDatabaseHandler componentDatabaseHandler) {
        this.projectDatabaseHandler = projectDatabaseHandler;
        this.componentDatabaseHandler = componentDatabaseHandler;
        this.user = user;
    }

    public Response addComponent(Component component) throws SW360Exception {
        log.debug("create Component { name='" + component.getName() + "' }");
        final AddDocumentRequestSummary addDocumentRequestSummary = componentDatabaseHandler.addComponent(component,
                user.getEmail());

        final String componentId = addDocumentRequestSummary.getId();
        if(componentId == null || componentId.isEmpty()) {
            throw new SW360Exception("Id of added component should not be empty. " + addDocumentRequestSummary.toString());
        }
        return new Response(componentId, AddDocumentRequestStatus.SUCCESS.equals(addDocumentRequestSummary.getRequestStatus()));
    }

    public Response addRelease(Release release) throws SW360Exception {
        log.debug("create Release { name='" + release.getName() + "', version='" + release.getVersion() + "' }");
        final AddDocumentRequestSummary addDocumentRequestSummary = componentDatabaseHandler.addRelease(release,
                user);

        final String releaseId = addDocumentRequestSummary.getId();
        if(releaseId == null || releaseId.isEmpty()) {
            throw new SW360Exception("Id of added release should not be empty. " + addDocumentRequestSummary.toString());
        }
        return new Response(releaseId, AddDocumentRequestStatus.SUCCESS.equals(addDocumentRequestSummary.getRequestStatus()));
    }

    public Response addSpdxDocument(SPDXDocument spdxDocument) throws SW360Exception, MalformedURLException {
        log.debug("create SPDXDocument");
        SpdxDocumentDatabaseHandler handler = new SpdxDocumentDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_SPDX);
        final AddDocumentRequestSummary addDocumentRequestSummary = handler.addSPDXDocument(spdxDocument, user);

        final String spdxDocId = addDocumentRequestSummary.getId();
        if(spdxDocId == null || spdxDocId.isEmpty()) {
            throw new SW360Exception("Id of added spdx document should not be empty. " + addDocumentRequestSummary.toString());
        }
        return new Response(spdxDocId, AddDocumentRequestStatus.SUCCESS.equals(addDocumentRequestSummary.getRequestStatus()));
    }

    public Response addDocumentCreationInformation(DocumentCreationInformation documentCreationInfo) throws SW360Exception, MalformedURLException {
        log.debug("create DocumentCreationInformation { name='" + documentCreationInfo.getName() + "' }");
        SpdxDocumentCreationInfoDatabaseHandler handler = new SpdxDocumentCreationInfoDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_SPDX);
        final AddDocumentRequestSummary addDocumentRequestSummary = handler.addDocumentCreationInformation(documentCreationInfo, user);

        final String docCreationInfoId = addDocumentRequestSummary.getId();
        if(docCreationInfoId == null || docCreationInfoId.isEmpty()) {
            throw new SW360Exception("Id of added document creation information should not be empty. " + addDocumentRequestSummary.toString());
        }
        return new Response(docCreationInfoId, AddDocumentRequestStatus.SUCCESS.equals(addDocumentRequestSummary.getRequestStatus()));
    }

    public Response addPackageInformation(PackageInformation packageInfo) throws SW360Exception, MalformedURLException {
        log.debug("create PackageInfomation { name='" + packageInfo.getName() + "' }");
        SpdxPackageInfoDatabaseHandler handler = new SpdxPackageInfoDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_SPDX);
        final AddDocumentRequestSummary addDocumentRequestSummary = handler.addPackageInformation(packageInfo, user);

        final String packageInfoId = addDocumentRequestSummary.getId();
        if(packageInfoId == null || packageInfoId.isEmpty()) {
            throw new SW360Exception("Id of added package information should not be empty. " + addDocumentRequestSummary.toString());
        }
        return new Response(packageInfoId, AddDocumentRequestStatus.SUCCESS.equals(addDocumentRequestSummary.getRequestStatus()));
    }

    public Response addProject(Project project) throws SW360Exception {
        log.debug("create Project { name='" + project.getName() + "', version='" + project.getVersion() + "' }");

        if (projectDatabaseHandler == null) {
            throw new SW360Exception("ProjectDatabaseHandler was not set, not able to add a project");
        }

        final Set<Attachment> attachments = project.getAttachments();
        if(attachments != null && attachments.size() > 0) {
            project.setAttachments(attachments.stream()
                    .map(a -> a.setCreatedBy(user.getEmail()))
                    .collect(Collectors.toSet()));
        }
        final AddDocumentRequestSummary addDocumentRequestSummary = projectDatabaseHandler.addProject(project,
                user);

        final String projectId = addDocumentRequestSummary.getId();
        if(projectId == null || projectId.isEmpty()) {
            throw new SW360Exception("Id of added project should not be empty. " + addDocumentRequestSummary.toString());
        }
        return new Response(projectId, AddDocumentRequestStatus.SUCCESS.equals(addDocumentRequestSummary.getRequestStatus()));
    }

    public static class Response {
        private final String id;
        private final List<Response> childs;
        private final boolean isAffected;
        private ReleaseRelationship releaseRelationship = ReleaseRelationship.UNKNOWN;

        public Response(String id) {
            this.id = id;
            this.childs = new ArrayList<>();
            this.isAffected = true;
        }

        public Response(String id, boolean isAffected) {
            this.id = id;
            this.childs = new ArrayList<>();
            this.isAffected = isAffected;
        }
        public void addChild(Response child) {
            this.childs.add(child);
        }


        public void addChilds(Collection<Response> childs) {
            this.childs.addAll(childs);
        }

        public String getId() {
            return id;
        }

        public int count() {
            return childs.stream()
                    .map(Response::count)
                    .reduce(1, Integer::sum);
        }

        public int countAffected() {
            return childs.stream()
                    .map(Response::countAffected)
                    .reduce((isAffected ? 1 : 0), Integer::sum);
        }

        public void setReleaseRelationship(ReleaseRelationship releaseRelationship) {
            this.releaseRelationship = releaseRelationship;
        }

        public ReleaseRelationship getReleaseRelationship() {
            return releaseRelationship;
        }
    }
}
