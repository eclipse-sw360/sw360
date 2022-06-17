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

import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SPDXDocumentFactory;
import org.spdx.rdfparser.model.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class SpdxBOMImporter {
    private static final Logger log = LogManager.getLogger(SpdxBOMImporter.class);
    private final SpdxBOMImporterSink sink;

    public SpdxBOMImporter(SpdxBOMImporterSink sink) {
        this.sink = sink;
    }

    public RequestSummary importSpdxBOMAsRelease(InputStream inputStream, AttachmentContent attachmentContent)
            throws InvalidSPDXAnalysisException, SW360Exception {
        return importSpdxBOM(inputStream, attachmentContent, SW360Constants.TYPE_RELEASE);
    }

    public RequestSummary importSpdxBOMAsProject(InputStream inputStream, AttachmentContent attachmentContent)
            throws InvalidSPDXAnalysisException, SW360Exception {
        return importSpdxBOM(inputStream, attachmentContent, SW360Constants.TYPE_PROJECT);
    }

    private RequestSummary importSpdxBOM(InputStream inputStream, AttachmentContent attachmentContent, String type)
            throws InvalidSPDXAnalysisException, SW360Exception {
        final RequestSummary requestSummary = new RequestSummary();
        final SpdxDocument spdxDocument = openAsSpdx(inputStream);
        final List<SpdxItem> describedPackages = Arrays.stream(spdxDocument.getDocumentDescribes())
                .filter(item -> item instanceof SpdxPackage)
                .collect(Collectors.toList());

        if (describedPackages.size() == 0) {
            requestSummary.setTotalAffectedElements(0);
            requestSummary.setTotalElements(0);
            requestSummary.setMessage("The provided BOM did not contain any top level packages.");
            requestSummary.setRequestStatus(RequestStatus.FAILURE);
            return requestSummary;
        } else if (describedPackages.size() > 1) {
            requestSummary.setTotalAffectedElements(0);
            requestSummary.setTotalElements(0);
            requestSummary.setMessage("The provided BOM file contained multiple described top level packages. This is not allowed here.");
            requestSummary.setRequestStatus(RequestStatus.FAILURE);
            return requestSummary;
        }

        final SpdxItem spdxItem = describedPackages.get(0);
        final Optional<SpdxBOMImporterSink.Response> response;
        if (SW360Constants.TYPE_PROJECT.equals(type)) {
            response = importAsProject(spdxItem, attachmentContent);
        } else if (SW360Constants.TYPE_RELEASE.equals(type)) {
            response = importAsRelease(spdxItem, attachmentContent);
        } else {
            throw new SW360Exception("Unsupported type=[" + type + "], can not import BOM");
        }

        if (response.isPresent()) {
            requestSummary.setRequestStatus(RequestStatus.SUCCESS);
            requestSummary.setTotalAffectedElements(response.get().countAffected());
            requestSummary.setTotalElements(response.get().count());
            requestSummary.setMessage(response.get().getId());
        } else {
            requestSummary.setRequestStatus(RequestStatus.FAILURE);
            requestSummary.setTotalAffectedElements(-1);
            requestSummary.setTotalElements(-1);
            requestSummary.setMessage("Failed to import the BOM as type=[" + type + "].");
        }
        return requestSummary;
    }

    private SpdxDocument openAsSpdx(InputStream inputStream) throws InvalidSPDXAnalysisException {
        String FILETYPE_SPDX_INTERNAL = "RDF/XML";
        return SPDXDocumentFactory
                .createSpdxDocument(inputStream,
                        "http://localhost/",
                        FILETYPE_SPDX_INTERNAL);
    }

    private Component createComponentFromSpdxPackage(SpdxPackage spdxPackage) {
        final Component component = new Component();
        final String name = spdxPackage.getName();
        component.setName(name);
        return component;
    }

    private SpdxBOMImporterSink.Response importAsComponent(SpdxPackage spdxPackage) throws SW360Exception {
        final Component component = createComponentFromSpdxPackage(spdxPackage);
        return sink.addComponent(component);
    }

    private Release createReleaseFromSpdxPackage(SpdxPackage spdxPackage) {
        final Release release = new Release();
        final String name = spdxPackage.getName();
        final String version = spdxPackage.getVersionInfo();
        release.setName(name);
        release.setVersion(version);
        return release;
    }

    private Attachment makeAttachmentFromContent(AttachmentContent attachmentContent) {
        Attachment attachment = new Attachment();
        attachment.setAttachmentContentId(attachmentContent.getId());
        attachment.setAttachmentType(AttachmentType.OTHER);
        attachment.setCreatedComment("Used for SPDX Bom import");
        attachment.setFilename(attachmentContent.getFilename());

        return attachment;
    }

    private Optional<SpdxBOMImporterSink.Response> importAsRelease(SpdxElement relatedSpdxElement) throws SW360Exception {
        return importAsRelease(relatedSpdxElement, null);
    }

    private Optional<SpdxBOMImporterSink.Response> importAsRelease(SpdxElement relatedSpdxElement, AttachmentContent attachmentContent) throws SW360Exception {
        if (relatedSpdxElement instanceof SpdxPackage) {
            final SpdxPackage spdxPackage = (SpdxPackage) relatedSpdxElement;

            SpdxBOMImporterSink.Response component = importAsComponent(spdxPackage);
            final String componentId = component.getId();

            final Release release = createReleaseFromSpdxPackage(spdxPackage);
            release.setComponentId(componentId);

            final Relationship[] relationships = spdxPackage.getRelationships();
            List<SpdxBOMImporterSink.Response> releases = importAsReleases(relationships);
            Map<String, ReleaseRelationship> releaseIdToRelationship = makeReleaseIdToRelationship(releases);
            release.setReleaseIdToRelationship(releaseIdToRelationship);

            if(attachmentContent != null) {
                Attachment attachment = makeAttachmentFromContent(attachmentContent);
                release.setAttachments(Collections.singleton(attachment));
            }


            final SpdxBOMImporterSink.Response response = sink.addRelease(release);
            response.addChild(component);
            return Optional.of(response);
        } else {
            log.debug("Unsupported SpdxElement: " + relatedSpdxElement.getClass().getCanonicalName());
            return Optional.empty();
        }
    }

    private Map<String, ReleaseRelationship> makeReleaseIdToRelationship(List<SpdxBOMImporterSink.Response> releases) {
        return releases.stream()
                .collect(Collectors.toMap(SpdxBOMImporterSink.Response::getId, SpdxBOMImporterSink.Response::getReleaseRelationship));
    }

    private Project creatProjectFromSpdxPackage(SpdxPackage spdxPackage) {
        Project project = new Project();
        final String name = spdxPackage.getName();
        final String version = spdxPackage.getVersionInfo();
        project.setName(name);
        project.setVersion(version);
        return project;
    }

    private List<SpdxBOMImporterSink.Response> importAsReleases(Relationship[] relationships) throws SW360Exception {
        List<SpdxBOMImporterSink.Response> releases = new ArrayList<>();

        Map<Relationship.RelationshipType, ReleaseRelationship> typeToSupplierMap = new HashMap<>();
        typeToSupplierMap.put(Relationship.RelationshipType.CONTAINS,  ReleaseRelationship.CONTAINED);

        for (Relationship relationship : relationships) {
            final Relationship.RelationshipType relationshipType = relationship.getRelationshipType();
            if(! typeToSupplierMap.keySet().contains(relationshipType)) {
                log.debug("Unsupported RelationshipType: " + relationshipType.toString());
                continue;
            }

            final SpdxElement relatedSpdxElement = relationship.getRelatedSpdxElement();
            final Optional<SpdxBOMImporterSink.Response> releaseId = importAsRelease(relatedSpdxElement);
            releaseId.map(response -> {
                response.setReleaseRelationship(typeToSupplierMap.get(relationshipType));
                return response;
            }).ifPresent(releases::add);
        }
        return releases;
    }

    private Map<String, ProjectReleaseRelationship> makeReleaseIdToProjectRelationship(List<SpdxBOMImporterSink.Response> releases) {
        return makeReleaseIdToRelationship(releases).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    final ProjectReleaseRelationship projectReleaseRelationship = new ProjectReleaseRelationship();
                    projectReleaseRelationship.setMainlineState(MainlineState.OPEN);
                    projectReleaseRelationship.setReleaseRelation(e.getValue());
                    return projectReleaseRelationship;
                }));
    }


    private Optional<SpdxBOMImporterSink.Response> importAsProject(SpdxElement spdxElement, AttachmentContent attachmentContent) throws SW360Exception {
        if (spdxElement instanceof SpdxPackage) {
            final SpdxPackage spdxPackage = (SpdxPackage) spdxElement;

            final Project project = creatProjectFromSpdxPackage(spdxPackage);

            final Relationship[] relationships = spdxPackage.getRelationships();
            List<SpdxBOMImporterSink.Response> releases = importAsReleases(relationships);
            Map<String, ProjectReleaseRelationship> releaseIdToProjectRelationship = makeReleaseIdToProjectRelationship(releases);
            project.setReleaseIdToUsage(releaseIdToProjectRelationship);

            if(attachmentContent != null) {
                Attachment attachment = makeAttachmentFromContent(attachmentContent);
                project.setAttachments(Collections.singleton(attachment));
            }

            final SpdxBOMImporterSink.Response response = sink.addProject(project);
            response.addChilds(releases);
            return Optional.of(response);
        } else {
            log.debug("Unsupported SpdxElement: " + spdxElement.getClass().getCanonicalName());
            return Optional.empty();
        }
    }
}
