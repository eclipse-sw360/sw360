/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 * Copyright TOSHIBA CORPORATION, 2021. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.spdx;

import com.google.gson.Gson;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.db.DatabaseHandlerUtil;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLinkJSON;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.spdx.library.model.enumerations.RelationshipType;
import org.spdx.library.model.*;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.tools.InvalidFileNameException;
import org.spdx.tools.SpdxToolsHelper;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.isNullEmptyOrWhitespace;

public class SpdxBOMImporter {
    private static final Logger log = LogManager.getLogger(SpdxBOMImporter.class);
    private final SpdxBOMImporterSink sink;

    public SpdxBOMImporter(SpdxBOMImporterSink sink) {
        this.sink = sink;
    }

    public ImportBomRequestPreparation prepareImportSpdxBOMAsRelease(File targetFile) {
        final ImportBomRequestPreparation requestPreparation = new ImportBomRequestPreparation();
        final SpdxDocument spdxDocument = openAsSpdx(targetFile);
        if (spdxDocument == null) {
            requestPreparation.setRequestStatus(RequestStatus.FAILURE);
            requestPreparation.setMessage("error-read-file");
            return requestPreparation;
        }
        try {
            final List<SpdxElement> describedPackages = spdxDocument.getDocumentDescribes().stream().collect(Collectors.toList());
            final List<SpdxElement> packages =  describedPackages.stream()
                    .filter(SpdxPackage.class::isInstance)
                    .collect(Collectors.toList());
            if (packages.isEmpty()) {
                requestPreparation.setMessage("The provided BOM did not contain any top level packages.");
                requestPreparation.setRequestStatus(RequestStatus.FAILURE);
                return requestPreparation;
            } else if (packages.size() > 1) {
                requestPreparation.setMessage("The provided BOM file contained multiple described top level packages. This is not allowed here.");
                requestPreparation.setRequestStatus(RequestStatus.FAILURE);
                return requestPreparation;
            }
            final SpdxElement spdxElement = packages.get(0);
            if (spdxElement instanceof SpdxPackage) {
                final SpdxPackage spdxPackage = (SpdxPackage) spdxElement;
                requestPreparation.setName(getValue(spdxPackage.getName()));
                requestPreparation.setVersion(getValue(spdxPackage.getVersionInfo()));
                requestPreparation.setRequestStatus(RequestStatus.SUCCESS);
            } else {
                requestPreparation.setMessage("Failed to get spdx package from the provided BOM file.");
                requestPreparation.setRequestStatus(RequestStatus.FAILURE);
            }
        } catch (InvalidSPDXAnalysisException e) {
            requestPreparation.setRequestStatus(RequestStatus.FAILURE);
            e.printStackTrace();
        }
        return requestPreparation;
    }

    private SpdxDocument openAsSpdx(File file) {
        try {
            log.info("Read file: " + file.getName());
            return SpdxToolsHelper.deserializeDocument(file);
        } catch (InvalidSPDXAnalysisException | IOException | InvalidFileNameException e) {
            log.error("Error read file " + file.getName() + " to SpdxDocument:" + e.getMessage());
            return null;
        }
    }

    private RequestSummary importSpdxBOM(AttachmentContent attachmentContent, String type, InputStream inputStream, User user)
            throws TException, IOException, InvalidSPDXAnalysisException {
        return importSpdxBOM(inputStream, attachmentContent, type, user);
    }

    public RequestSummary importSpdxBOMAsRelease(InputStream inputStream, AttachmentContent attachmentContent, User user)
            throws InvalidSPDXAnalysisException, TException, IOException {
        return importSpdxBOM(inputStream, attachmentContent, SW360Constants.TYPE_RELEASE, user);
    }

    public RequestSummary importSpdxBOMAsProject(InputStream inputStream, AttachmentContent attachmentContent, User user)
            throws InvalidSPDXAnalysisException, TException, IOException {
        return importSpdxBOM(inputStream, attachmentContent, SW360Constants.TYPE_PROJECT, user);
    }

    private RequestSummary importSpdxBOM(InputStream inputStream, AttachmentContent attachmentContent, String type, User user)
            throws  TException, IOException {
        final RequestSummary requestSummary = new RequestSummary();
        List<SpdxElement> describedPackages = new ArrayList<>();
        String fileType = getFileType(attachmentContent.getFilename());
        final String ext = "." + fileType;

        final File sourceFile = DatabaseHandlerUtil.saveAsTempFile( inputStream, attachmentContent.getId(), ext);
        try {
            SpdxDocument spdxDocument = openAsSpdx(sourceFile);
            if (spdxDocument == null) {
                requestSummary.setRequestStatus(RequestStatus.FAILURE);
                return requestSummary;
            }
            describedPackages = spdxDocument.getDocumentDescribes().stream().collect(Collectors.toList());
            List<SpdxElement> packages = describedPackages.stream()
                    .filter(SpdxPackage.class::isInstance)
                    .collect(Collectors.toList());
            if (packages.isEmpty()) {
                requestSummary.setTotalAffectedElements(0);
                requestSummary.setTotalElements(0);
                requestSummary.setMessage("The provided BOM did not contain any top level packages.");
                requestSummary.setRequestStatus(RequestStatus.FAILURE);
                return requestSummary;
            } else if (packages.size() > 1) {
                requestSummary.setTotalAffectedElements(0);
                requestSummary.setTotalElements(0);
                requestSummary.setMessage("The provided BOM file contained multiple described top level packages. This is not allowed here.");
                requestSummary.setRequestStatus(RequestStatus.FAILURE);
                return requestSummary;
            }

            final SpdxItem spdxItem = (SpdxItem) describedPackages.get(0);
            final Optional<SpdxBOMImporterSink.Response> response;
            if (SW360Constants.TYPE_PROJECT.equals(type)) {
                response = importAsProject(spdxItem, attachmentContent, user);
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
        } catch (InvalidSPDXAnalysisException | NullPointerException e) {
            log.error("Can not open file to SpdxDocument " +e);
        }
        return requestSummary;
    }

    private Component createComponentFromSpdxPackage(SpdxPackage spdxPackage) throws InvalidSPDXAnalysisException {
        final Component component = new Component();
        String name = "";
        name = getValue(spdxPackage.getName());
        component.setName(name);
        return component;
    }

    private SpdxBOMImporterSink.Response importAsComponent(SpdxPackage spdxPackage) throws SW360Exception, InvalidSPDXAnalysisException {
        final Component component = createComponentFromSpdxPackage(spdxPackage);
        component.setComponentType(ComponentType.OSS);
        return sink.addComponent(component);
    }

    private Release createReleaseFromSpdxPackage(SpdxPackage spdxPackage) throws InvalidSPDXAnalysisException {
        final Release release = new Release();
        final String name = getValue(spdxPackage.getName());
        final String version = getValue(spdxPackage.getVersionInfo());
        release.setName(name);
        release.setVersion(version);
        return release;
    }

    private Attachment makeAttachmentFromContent(AttachmentContent attachmentContent) {
        Attachment attachment = new Attachment();
        attachment.setAttachmentContentId(attachmentContent.getId());
        attachment.setAttachmentType(AttachmentType.SBOM);
        attachment.setCreatedComment("Used for SPDX Bom import");
        attachment.setFilename(attachmentContent.getFilename());
        attachment.setCheckStatus(CheckStatus.NOTCHECKED);

        return attachment;
    }

    private Optional<SpdxBOMImporterSink.Response> importAsRelease(SpdxElement relatedSpdxElement) throws SW360Exception, InvalidSPDXAnalysisException {
        return importAsRelease(relatedSpdxElement, null);
    }

    private Optional<SpdxBOMImporterSink.Response> importAsRelease(SpdxElement relatedSpdxElement, AttachmentContent attachmentContent
                                                                  ) throws SW360Exception, InvalidSPDXAnalysisException {
        if (relatedSpdxElement instanceof SpdxPackage) {
            final SpdxPackage spdxPackage = (SpdxPackage) relatedSpdxElement;
            final Release release;
            SpdxBOMImporterSink.Response component;

            component = importAsComponent(spdxPackage);
            final String componentId = component.getId();

            release = createReleaseFromSpdxPackage(spdxPackage);
            release.setComponentId(componentId);

            final Relationship[] relationships = spdxPackage.getRelationships().toArray(new Relationship[spdxPackage.getRelationships().size()]);
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

    private Project creatProjectFromSpdxPackage(SpdxPackage spdxPackage) throws InvalidSPDXAnalysisException {
        Project project = new Project();
        final String name = getValue(spdxPackage.getName());
        final String version = getValue(spdxPackage.getVersionInfo());
        project.setName(name);
        project.setVersion(version);
        return project;
    }

    private List<SpdxBOMImporterSink.Response> importAsReleases(Relationship[] relationships) throws SW360Exception, InvalidSPDXAnalysisException {
        List<SpdxBOMImporterSink.Response> releases = new ArrayList<>();

        Map<RelationshipType, ReleaseRelationship> typeToSupplierMap = new HashMap<>();
        typeToSupplierMap.put(RelationshipType.CONTAINS,  ReleaseRelationship.CONTAINED);

        for (Relationship relationship : relationships) {
            final RelationshipType relationshipType = relationship.getRelationshipType();
            if(! typeToSupplierMap.keySet().contains(relationshipType)) {
                log.debug("Unsupported RelationshipType: " + relationshipType.toString());
                continue;
            }

            final SpdxElement relatedSpdxElement = relationship.getRelatedSpdxElement().get();
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

    private Optional<SpdxBOMImporterSink.Response> importAsProject(SpdxElement spdxElement, AttachmentContent attachmentContent, User user) throws SW360Exception, InvalidSPDXAnalysisException {
        if (spdxElement instanceof SpdxPackage) {
            final SpdxPackage spdxPackage = (SpdxPackage) spdxElement;
            final Project project = creatProjectFromSpdxPackage(spdxPackage);

            final Relationship[] relationships = spdxPackage.getRelationships().toArray(new Relationship[spdxPackage.getRelationships().size()]);
            List<ReleaseLinkJSON> releaseLinkJSONS = getReleaseLinkJSONByRelationship(relationships, user);

            List<SpdxBOMImporterSink.Response> releases = new ArrayList<>();
            releaseLinkJSONS.forEach(releaseLinkJSON -> {
                SpdxBOMImporterSink.Response response = new SpdxBOMImporterSink.Response(releaseLinkJSON.releaseId);
                response.setReleaseRelationship(ReleaseRelationship.valueOf(releaseLinkJSON.releaseRelationship));
                releases.add(response);
            });
            project.setReleaseRelationNetwork(new Gson().toJson(releaseLinkJSONS));

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

    private String getValue(Optional<String> value) {
        if (value.isPresent()) {
            return value.get();
        } else {
            return "";
        }
    }

    private String getFileType(String fileName) {
        if (isNullEmptyOrWhitespace(fileName) || !fileName.contains(".")) {
            log.error("Can not get file type from file name - no file extension");
            return null;
        }
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        if ("xml".equals(ext)) {
            if (fileName.endsWith("rdf.xml")) {
                ext = "rdf";
            }
        }
        return ext;
    }

    private List<ReleaseLinkJSON> getReleaseLinkJSONByRelationship(Relationship[] relationships, User user){
        List<ReleaseLinkJSON> releaseLinkJSONS = new ArrayList<>();
        Arrays.stream(relationships).forEach(
                relationship -> {
                    try {
                        SpdxElement relatedSpdxElement = relationship.getRelatedSpdxElement().get();
                        SpdxPackage spdxPackage = (SpdxPackage) relatedSpdxElement;

                        Optional<SpdxBOMImporterSink.Response> response = importAsProjectRelease(relatedSpdxElement);
                        releaseLinkJSONS.add(buildReleaseLinkJSONByRelationship(new ReleaseLinkJSON(
                                response.get().getId()), spdxPackage.getRelationships().toArray(new Relationship[spdxPackage.getRelationships().size()]), user));
                    } catch (SW360Exception | InvalidSPDXAnalysisException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
        return releaseLinkJSONS;
    }

    private ReleaseLinkJSON buildReleaseLinkJSONByRelationship(ReleaseLinkJSON releaseLinkJSON,
                                                               Relationship[] relationships, User user) throws SW360Exception, InvalidSPDXAnalysisException {
        List<ReleaseLinkJSON> linkedReleasesJSON = new ArrayList<>();
        releaseLinkJSON.setMainlineState(MainlineState.OPEN.toString());
        releaseLinkJSON.setReleaseRelationship(ReleaseRelationship.CONTAINED.toString());
        releaseLinkJSON.setComment("");
        releaseLinkJSON.setCreateOn(SW360Utils.getCreatedOn());
        releaseLinkJSON.setCreateBy(user.getEmail());

        Map<RelationshipType, ReleaseRelationship> typeToSupplierMap = new HashMap<>();
        typeToSupplierMap.put(RelationshipType.CONTAINS,  ReleaseRelationship.CONTAINED);

        for (Relationship relationship : relationships) {
            final RelationshipType relationshipType = relationship.getRelationshipType();
            if(! typeToSupplierMap.keySet().contains(relationshipType)) {
                log.debug("Unsupported RelationshipType: " + relationshipType.toString());
                continue;
            }
            final SpdxElement relatedSpdxElement = relationship.getRelatedSpdxElement().get();
            final SpdxPackage spdxPackage = (SpdxPackage) relatedSpdxElement;

            Optional<SpdxBOMImporterSink.Response> response = importAsProjectRelease(relatedSpdxElement);

            ReleaseLinkJSON linkJSON = new ReleaseLinkJSON(response.get().getId());
            linkJSON.setMainlineState(MainlineState.OPEN.toString());
            linkJSON.setReleaseRelationship(ReleaseRelationship.CONTAINED.toString());
            linkJSON.setComment("");
            linkJSON.setCreateOn(SW360Utils.getCreatedOn());
            linkJSON.setCreateBy(user.getEmail());
            linkedReleasesJSON.add(buildReleaseLinkJSONByRelationship(linkJSON, spdxPackage.getRelationships().toArray(new Relationship[spdxPackage.getRelationships().size()]), user));
        }
        releaseLinkJSON.setReleaseLink(linkedReleasesJSON);

        return releaseLinkJSON;
    }

    /**
     * Import as project release
     *
     * @param relatedSpdxElement element related spdx
     * @return response
     * @throws SW360Exception exception throws when build response
     */
    private Optional<SpdxBOMImporterSink.Response> importAsProjectRelease(SpdxElement relatedSpdxElement) throws SW360Exception, InvalidSPDXAnalysisException {
        return importAsProjectRelease(relatedSpdxElement, null);
    }

    private Optional<SpdxBOMImporterSink.Response> importAsProjectRelease(SpdxElement relatedSpdxElement, AttachmentContent attachmentContent) throws SW360Exception, InvalidSPDXAnalysisException {
        if (relatedSpdxElement instanceof SpdxPackage) {
            final SpdxPackage spdxPackage = (SpdxPackage) relatedSpdxElement;

            SpdxBOMImporterSink.Response component = importAsComponent(spdxPackage);
            final String componentId = component.getId();

            final Release release = createReleaseFromSpdxPackage(spdxPackage);
            release.setComponentId(componentId);

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
}
