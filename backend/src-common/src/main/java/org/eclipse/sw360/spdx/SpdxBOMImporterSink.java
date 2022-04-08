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
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.ProjectDatabaseHandler;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Component;

import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation;

import org.eclipse.sw360.datahandler.db.spdx.document.SpdxDocumentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.spdx.documentcreationinfo.SpdxDocumentCreationInfoDatabaseHandler;
import org.eclipse.sw360.datahandler.db.spdx.packageinfo.SpdxPackageInfoDatabaseHandler;

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
    private final SpdxDocumentDatabaseHandler spdxDocumentDatabaseHandler;
    private final SpdxDocumentCreationInfoDatabaseHandler creationInfoDatabaseHandler;
    private final SpdxPackageInfoDatabaseHandler packageInfoDatabaseHandler;
    private final User user;

    public SpdxBOMImporterSink(User user, ProjectDatabaseHandler projectDatabaseHandler, ComponentDatabaseHandler componentDatabaseHandler) throws MalformedURLException {
        this.projectDatabaseHandler = projectDatabaseHandler;
        this.componentDatabaseHandler = componentDatabaseHandler;
        this.spdxDocumentDatabaseHandler = new SpdxDocumentDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_SPDX);
        this.creationInfoDatabaseHandler = new SpdxDocumentCreationInfoDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_SPDX);
        this.packageInfoDatabaseHandler = new SpdxPackageInfoDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_SPDX);
        this.user = user;
    }

    public Response addComponent(Component component) throws SW360Exception {
        log.debug("create Component { name='" + component.getName() + "' }");
        
        if (CommonUtils.isNotNullEmptyOrWhitespace(user.getDepartment())) {
            component.setBusinessUnit(user.getDepartment());
        } else {
            log.error("Could not get the user department. component name=" +  component.getName());
        }
        final AddDocumentRequestSummary addDocumentRequestSummary = componentDatabaseHandler.addComponent(component,
                user.getEmail());

        final String componentId = addDocumentRequestSummary.getId();
        if (componentId == null || componentId.isEmpty()) {
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

    public Response addOrUpdateSpdxDocument(SPDXDocument spdxDocument) throws SW360Exception {
        log.debug("create or update SPDXDocument");
        RequestStatus requestStatus;
        String spdxDocId;
        if (spdxDocument.isSetId()) {
            requestStatus = spdxDocumentDatabaseHandler.updateSPDXDocument(spdxDocument, user);
            spdxDocId = spdxDocument.getId();
        } else {
            AddDocumentRequestSummary addDocumentRequestSummary = spdxDocumentDatabaseHandler.addSPDXDocument(spdxDocument, user);
            requestStatus = RequestStatus.findByValue(addDocumentRequestSummary.getRequestStatus().getValue());
            spdxDocId = addDocumentRequestSummary.getId();
        }

        if(spdxDocId == null || spdxDocId.isEmpty()) {
            throw new SW360Exception("Id of spdx document should not be empty. " + requestStatus.toString());
        }
        return new Response(spdxDocId, RequestStatus.SUCCESS.equals(requestStatus));
    }

    public Response addOrUpdateDocumentCreationInformation(DocumentCreationInformation documentCreationInfo) throws SW360Exception {
        log.debug("create or update DocumentCreationInformation { name='" + documentCreationInfo.getName() + "' }");
        RequestStatus requestStatus;
        String docCreationInfoId;
        if (documentCreationInfo.isSetId()) {
            requestStatus = creationInfoDatabaseHandler.updateDocumentCreationInformation(documentCreationInfo, user);
            docCreationInfoId = documentCreationInfo.getId();
        } else {
            AddDocumentRequestSummary addDocumentRequestSummary = creationInfoDatabaseHandler.addDocumentCreationInformation(documentCreationInfo, user);
            requestStatus = RequestStatus.findByValue(addDocumentRequestSummary.getRequestStatus().getValue());
            docCreationInfoId = addDocumentRequestSummary.getId();
        }

        if(docCreationInfoId == null || docCreationInfoId.isEmpty()) {
            throw new SW360Exception("Id of added document creation information should not be empty. " + requestStatus.toString());
        }
        return new Response(docCreationInfoId, RequestStatus.SUCCESS.equals(requestStatus));
    }

    public Response addOrUpdatePackageInformation(PackageInformation packageInfo) throws SW360Exception {
        log.debug("create or update PackageInfomation { name='" + packageInfo.getName() + "' }");
        RequestStatus requestStatus;
        String packageInfoId;
        if (packageInfo.isSetId()) {
            requestStatus = packageInfoDatabaseHandler.updatePackageInformation(packageInfo, user);
            packageInfoId = packageInfo.getId();
        } else {
            AddDocumentRequestSummary addDocumentRequestSummary = packageInfoDatabaseHandler.addPackageInformation(packageInfo, user);
            requestStatus = RequestStatus.findByValue(addDocumentRequestSummary.getRequestStatus().getValue());
            packageInfoId = addDocumentRequestSummary.getId();
        }
        if (packageInfoId == null || packageInfoId.isEmpty()) {
            throw new SW360Exception("Id of added package information should not be empty. " + requestStatus.toString());
        }
        return new Response(packageInfoId, RequestStatus.SUCCESS.equals(requestStatus));
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

    public Release getRelease(String id) throws SW360Exception {
        return componentDatabaseHandler.getRelease(id, user);
    }

    public Component searchComponent(String name)throws SW360Exception {
        List<Component> components = componentDatabaseHandler.searchComponentByNameForExport(name, true);
        if (components.isEmpty())
            return null;
        else {
            for (Component component : components) {
                if (component.getName().equals(name))
                    return component;
            }
        }
        return null;
    }

    public Release searchRelease(String name)throws SW360Exception {
        List<Release> releases = componentDatabaseHandler.searchReleaseByNamePrefix(name);
        if (releases.isEmpty())
            return null;
        else {
            for (Release release : releases) {
                if (release.getName().equals(name))
                    return release;
            }
        }
        return null;
    }

    public SPDXDocument getSPDXDocument(String id)  throws SW360Exception {
        return spdxDocumentDatabaseHandler.getSPDXDocumentById(id, user);
    }

    public DocumentCreationInformation getDocumentCreationInfo(String id)  throws SW360Exception {
        return creationInfoDatabaseHandler.getDocumentCreationInformationById(id, user);
    }

    public PackageInformation getPackageInfo(String id)  throws SW360Exception {
        return packageInfoDatabaseHandler.getPackageInformationById(id, user);
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
