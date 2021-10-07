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

import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.spdx.annotations.*;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.*;
import org.eclipse.sw360.datahandler.thrift.spdx.fileinformation.*;
import org.eclipse.sw360.datahandler.thrift.spdx.otherlicensinginformationdetected.*;
import org.eclipse.sw360.datahandler.thrift.spdx.relationshipsbetweenspdxelements.*;
import org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.*;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.*;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.*;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SPDXDocumentFactory;
import org.spdx.rdfparser.model.*;
import org.spdx.rdfparser.model.pointer.*;
import org.spdx.rdfparser.license.ExtractedLicenseInfo;
import org.spdx.rdfparser.SpdxPackageVerificationCode;
import org.spdx.rdfparser.SPDXChecksum;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.isNotNullEmptyOrWhitespace;

public class SpdxBOMImporter {
    private static final Logger log = LogManager.getLogger(SpdxBOMImporter.class);
    private final SpdxBOMImporterSink sink;

    public SpdxBOMImporter(SpdxBOMImporterSink sink) {
        this.sink = sink;
    }

    public ImportBomRequestPreparation prepareImportSpdxBOMAsRelease(InputStream inputStream, AttachmentContent attachmentContent)
            throws InvalidSPDXAnalysisException, SW360Exception {
        final ImportBomRequestPreparation requestPreparation = new ImportBomRequestPreparation();
        final SpdxDocument spdxDocument = openAsSpdx(inputStream);
        final List<SpdxItem> describedPackages = Arrays.stream(spdxDocument.getDocumentDescribes())
                .filter(item -> item instanceof SpdxPackage)
                .collect(Collectors.toList());

        if (describedPackages.size() == 0) {
            requestPreparation.setMessage("The provided BOM did not contain any top level packages.");
            requestPreparation.setRequestStatus(RequestStatus.FAILURE);
            return requestPreparation;
        } else if (describedPackages.size() > 1) {
            requestPreparation.setMessage("The provided BOM file contained multiple described top level packages. This is not allowed here.");
            requestPreparation.setRequestStatus(RequestStatus.FAILURE);
            return requestPreparation;
        }

        final SpdxItem spdxItem = describedPackages.get(0);
        if (spdxItem instanceof SpdxPackage) {
            final SpdxPackage spdxPackage = (SpdxPackage) spdxItem;

            requestPreparation.setName(spdxPackage.getName());
            requestPreparation.setVersion(spdxPackage.getVersionInfo());
            requestPreparation.setRequestStatus(RequestStatus.SUCCESS);
        } else {
            requestPreparation.setMessage("Failed to get spdx package from the provided BOM file.");
            requestPreparation.setRequestStatus(RequestStatus.FAILURE);
        }
        return requestPreparation;
    }

    public RequestSummary importSpdxBOMAsRelease(InputStream inputStream, AttachmentContent attachmentContent, String newReleaseVersion, String releaseId)
            throws InvalidSPDXAnalysisException, SW360Exception {
        return importSpdxBOM(inputStream, attachmentContent, SW360Constants.TYPE_RELEASE, newReleaseVersion, releaseId);
    }

    public RequestSummary importSpdxBOMAsProject(InputStream inputStream, AttachmentContent attachmentContent)
            throws InvalidSPDXAnalysisException, SW360Exception {
        return importSpdxBOM(inputStream, attachmentContent, SW360Constants.TYPE_PROJECT);
    }

    private RequestSummary importSpdxBOM(InputStream inputStream, AttachmentContent attachmentContent, String type)
            throws InvalidSPDXAnalysisException, SW360Exception {
        return importSpdxBOM(inputStream, attachmentContent, type, null, null);
    }

    private RequestSummary importSpdxBOM(InputStream inputStream, AttachmentContent attachmentContent, String type, String newReleaseVersion, String releaseId)
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
            response = importAsRelease(spdxItem, attachmentContent, spdxDocument, newReleaseVersion, releaseId);
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

    private SPDXDocument createSPDXDocumentFromSpdxDocument(String releaseId, SpdxDocument spdxDocument) throws SW360Exception, MalformedURLException {
        final SPDXDocument doc = getSpdxDocumentFromRelease(releaseId);
        doc.setReleaseId(releaseId);
        try {
            final SpdxSnippet[] spdxSnippets = spdxDocument.getDocumentContainer().findAllSnippets().toArray(new SpdxSnippet[0]);
            final Relationship[] spdxRelationships = spdxDocument.getRelationships();
            final Annotation[] spdxAnnotations = spdxDocument.getAnnotations();
            final ExtractedLicenseInfo[] extractedLicenseInfos = spdxDocument.getExtractedLicenseInfos();

            final Set<SnippetInformation> snippetInfos = createSnippetsFromSpdxSnippets(spdxSnippets);
            final Set<RelationshipsBetweenSPDXElements> relationships = createRelationshipsFromSpdxRelationships(spdxRelationships);
            final Set<Annotations> annotations = createAnnotationsFromSpdxAnnotations(spdxAnnotations);
            final Set<OtherLicensingInformationDetected> otherLicenses = createOtherLicensesFromSpdxExtractedLicenses(extractedLicenseInfos);

            doc.setSnippets(snippetInfos)
                .setRelationships(relationships)
                .setAnnotations(annotations)
                .setOtherLicensingInformationDetecteds(otherLicenses);
        } catch (InvalidSPDXAnalysisException e) {
            log.error(e);
        }

        return doc;
    }

    private Set<Annotations> createAnnotationsFromSpdxAnnotations(Annotation[] spdxAnnotations) {
        Set<Annotations> annotations = new HashSet<Annotations>();
        int index = 0;

        for(Annotation spdxAnn : spdxAnnotations) {
            String annotator = spdxAnn.getAnnotator();
            String date = spdxAnn.getAnnotationDate();
            String type = spdxAnn.getAnnotationTypeTag();
            String comment = spdxAnn.getComment();

            Annotations ann = new Annotations();
            ann.setAnnotator(verifyOrSetDefault(annotator))
                .setAnnotationDate(verifyOrSetDefault(date))
                .setAnnotationType(verifyOrSetDefault(type))
                .setAnnotationComment(verifyOrSetDefault(comment))
                .setIndex(index);

            annotations.add(ann);
            index++;
        }

        return annotations;
    }

    private Set<SnippetInformation> createSnippetsFromSpdxSnippets(SpdxSnippet[] spdxSnippets) {
        Set<SnippetInformation> snippets = new HashSet<SnippetInformation>();
        int index = 0;

        try {
            for (SpdxSnippet spdxSnippet : spdxSnippets) {
                String id = spdxSnippet.getId();
                String snippetFromFile = spdxSnippet.getSnippetFromFile().getName();
                Set<SnippetRange> ranges = createSnippetRangesFromSpdxSnippet(spdxSnippet);
                String licenseConcluded = spdxSnippet.getLicenseConcluded().toString();
                Set<String> licenseInfoInFile = Arrays.stream(spdxSnippet.getLicenseInfoFromFiles())
                                            .map(license -> verifyOrSetDefault(license.toString()))
                                            .collect(Collectors.toSet());
                String licenseComment = spdxSnippet.getLicenseComment();
                String copyrightText = spdxSnippet.getCopyrightText();
                String comment = spdxSnippet.getComment();
                String name = spdxSnippet.getName();
                String attributionText = String.join("|", spdxSnippet.getAttributionText());

                SnippetInformation snippet = new SnippetInformation();
                snippet.setSPDXID(verifyOrSetDefault(id))
                        .setSnippetFromFile(verifyOrSetDefault(snippetFromFile))
                        .setSnippetRanges(ranges)
                        .setLicenseConcluded(verifyOrSetDefault(licenseConcluded))
                        .setLicenseInfoInSnippets(licenseInfoInFile)
                        .setLicenseComments(verifyOrSetDefault(licenseComment))
                        .setCopyrightText(verifyOrSetDefault(copyrightText))
                        .setComment(verifyOrSetDefault(comment))
                        .setName(verifyOrSetDefault(name))
                        .setSnippetAttributionText(verifyOrSetDefault(attributionText))
                        .setIndex(index);

                snippets.add(snippet);
                index++;
            }
        } catch (InvalidSPDXAnalysisException e) {
            log.error(e);
        }

        return snippets;
    }

    private Set<SnippetRange> createSnippetRangesFromSpdxSnippet(SpdxSnippet spdxSnippet) throws InvalidSPDXAnalysisException {
        StartEndPointer spdxByteRange = spdxSnippet.getByteRange();
        String[] byteRanges = rangeToStrs(spdxByteRange);
        SnippetRange snippetByteRange = new SnippetRange();
        snippetByteRange.setRangeType("BYTE")
                .setStartPointer(byteRanges[0])
                .setEndPointer(byteRanges[1])
                .setReference(spdxByteRange.getStartPointer().getReference().toString())
                .setIndex(0);

        StartEndPointer spdxLineRange = spdxSnippet.getLineRange();
        String[] lineRanges = rangeToStrs(spdxLineRange);
        SnippetRange snippetLineRange = new SnippetRange();
        snippetLineRange.setRangeType("LINE")
                .setStartPointer(lineRanges[0])
                .setEndPointer(lineRanges[1])
                .setReference(spdxLineRange.getStartPointer().getReference().toString())
                .setIndex(1);

        return new HashSet<SnippetRange>(Arrays.asList(snippetByteRange, snippetLineRange));
    }

    // refer to rangeToStr function of spdx-tools
    private String[] rangeToStrs(StartEndPointer rangePointer) throws InvalidSPDXAnalysisException {
        SinglePointer startPointer = rangePointer.getStartPointer();
        if (startPointer == null) {
            throw new InvalidSPDXAnalysisException("Missing start pointer");
        }
        SinglePointer endPointer = rangePointer.getEndPointer();
        if (endPointer == null) {
            throw new InvalidSPDXAnalysisException("Missing end pointer");
        }
        String start = null;
        if (startPointer instanceof ByteOffsetPointer) {
            start = String.valueOf(((ByteOffsetPointer)startPointer).getOffset());
        } else if (startPointer instanceof LineCharPointer) {
            start = String.valueOf(((LineCharPointer)startPointer).getLineNumber());
        } else {
            log.error("Unknown pointer type for start pointer "+startPointer.toString());
            throw new InvalidSPDXAnalysisException("Unknown pointer type for start pointer");
        }
        String end = null;
        if (endPointer instanceof ByteOffsetPointer) {
            end = String.valueOf(((ByteOffsetPointer)endPointer).getOffset());
        } else if (endPointer instanceof LineCharPointer) {
            end = String.valueOf(((LineCharPointer)endPointer).getLineNumber());
        } else {
            log.error("Unknown pointer type for start pointer "+startPointer.toString());
            throw new InvalidSPDXAnalysisException("Unknown pointer type for start pointer");
        }
        return new String[] { start, end };
    }

    private Set<RelationshipsBetweenSPDXElements> createRelationshipsFromSpdxRelationships(Relationship[] spdxRelationships) {
        Set<RelationshipsBetweenSPDXElements> relationships = new HashSet<RelationshipsBetweenSPDXElements>();
        int index = 0;

        for (Relationship spdxRelationship : spdxRelationships) {
            String relatedSpdxElementId = spdxRelationship.getRelatedSpdxElement().getId();
            String type = spdxRelationship.getRelationshipType().toTag();

            /// ?????
            String relatedSpdxElement = spdxRelationship.getRelatedSpdxElement().toString();
            String comment = spdxRelationship.getComment();

            RelationshipsBetweenSPDXElements relationship = new RelationshipsBetweenSPDXElements();
            relationship.setSpdxElementId(verifyOrSetDefault(relatedSpdxElementId))
                        .setRelationshipType(verifyOrSetDefault(type))
                        .setRelatedSpdxElement(verifyOrSetDefault(relatedSpdxElement))
                        .setRelationshipComment(verifyOrSetDefault(comment))
                        .setIndex(index);

            relationships.add(relationship);
            index++;
        }

        return relationships;
    }

    private Set<OtherLicensingInformationDetected> createOtherLicensesFromSpdxExtractedLicenses(ExtractedLicenseInfo[] spdxExtractedLicenses) {
        Set<OtherLicensingInformationDetected> otherLicenses = new HashSet<OtherLicensingInformationDetected>();
        int index = 0;

        for (ExtractedLicenseInfo spdxExtractedLicense : spdxExtractedLicenses) {
            String licenseId = spdxExtractedLicense.getLicenseId();
            String extractedText = spdxExtractedLicense.getExtractedText();
            String name = spdxExtractedLicense.getName();
            Set<String> crossRef = new HashSet<String>(Arrays.asList(verifyOrSetDefault(spdxExtractedLicense.getCrossRef())));
            String comment = spdxExtractedLicense.getComment();

            OtherLicensingInformationDetected otherLicense = new OtherLicensingInformationDetected();
            otherLicense.setLicenseId(verifyOrSetDefault(licenseId))
                        .setExtractedText(verifyOrSetDefault(extractedText))
                        .setLicenseName(verifyOrSetDefault(name))
                        .setLicenseCrossRefs(crossRef)
                        .setLicenseComment(verifyOrSetDefault(comment))
                        .setIndex(index);

            otherLicenses.add(otherLicense);
            index++;
        }

        return otherLicenses;
    }

    private DocumentCreationInformation createDocumentCreationInfoFromSpdxDocument(String spdxDocId, SpdxDocument spdxDocument) throws SW360Exception, MalformedURLException {
        final DocumentCreationInformation info = getDocCreationInfoFromSpdxDocument(spdxDocId);
        info.setSpdxDocumentId(spdxDocId);

        try {
            final String spdxVersion = spdxDocument.getSpecVersion();
            final String dataLicense = spdxDocument.getDataLicense().toString();
            final String spdxId = spdxDocument.getId();
            final String name = spdxDocument.getName();
            final String documentNamespace = spdxDocument.getDocumentContainer().getDocumentNamespace();
            final Set<ExternalDocumentReferences> refs = createExternalDocumentRefsFromSpdxDocument(spdxDocument);
            final String licenseListVersion = spdxDocument.getCreationInfo().getLicenseListVersion();
            final Set<Creator> creators = createCreatorFromSpdxDocument(spdxDocument);
            final String createdDate = spdxDocument.getCreationInfo().getCreated();
            final String creatorComment = spdxDocument.getCreationInfo().getComment();
            final String documentComment = spdxDocument.getDocumentComment();

            info.setSpdxVersion(verifyOrSetDefault(spdxVersion))
                .setDataLicense(verifyOrSetDefault(dataLicense))
                .setSPDXID(verifyOrSetDefault(spdxId))
                .setName(verifyOrSetDefault(name))
                .setDocumentNamespace(verifyOrSetDefault(documentNamespace))
                .setExternalDocumentRefs(refs)
                .setLicenseListVersion(verifyOrSetDefault(licenseListVersion))
                .setCreator(creators)
                .setCreated(verifyOrSetDefault(createdDate))
                .setCreatorComment(verifyOrSetDefault(creatorComment))
                .setDocumentComment(verifyOrSetDefault(documentComment));
        } catch (InvalidSPDXAnalysisException e) {
            log.error(e);
        }

        return info;
    }

    private Set<ExternalDocumentReferences> createExternalDocumentRefsFromSpdxDocument(SpdxDocument spdxDocument) {
        Set<ExternalDocumentReferences> refs = new HashSet<ExternalDocumentReferences>();
        int index = 0;

        try {
            ExternalDocumentRef[] externalDocumentRefs = spdxDocument.getDocumentContainer().getExternalDocumentRefs();

            for (ExternalDocumentRef externalDocumentRef : externalDocumentRefs) {
                Checksum spdxChecksum = externalDocumentRef.getChecksum();

                String externalDocumentId = externalDocumentRef.getExternalDocumentId();
                String spdxDocumentNamespace = externalDocumentRef.getSpdxDocumentNamespace();
                CheckSum checksum = new CheckSum();
                checksum.setAlgorithm(org.spdx.rdfparser.model.Checksum.CHECKSUM_ALGORITHM_TO_TAG.get(spdxChecksum.getAlgorithm()).replace(":", ""))
                        .setChecksumValue(spdxChecksum.getValue());

                ExternalDocumentReferences ref = new ExternalDocumentReferences();
                ref.setExternalDocumentId(verifyOrSetDefault(externalDocumentId))
                    .setChecksum(checksum)
                    .setSpdxDocument(verifyOrSetDefault(spdxDocumentNamespace))
                    .setIndex(index);

                refs.add(ref);
                index++;
            }
        } catch (InvalidSPDXAnalysisException e) {
            log.error(e);
        }

        return refs;
    }

    private Set<Creator> createCreatorFromSpdxDocument(SpdxDocument spdxDocument) {
        Set<Creator> creators = new HashSet<Creator>();
        int index = 0;

        try {
            String[] spdxCreators = spdxDocument.getCreationInfo().getCreators();

            for (String spdxCreator : spdxCreators) {
                String[] data = spdxCreator.split(":");
                if (data.length < 2) {
                    log.error("Failed to get SPDX creator from " + spdxCreator + "!");
                    continue;
                }
                String type = data[0].trim();
                String value = spdxCreator.substring(data[0].length()+1).trim();

                Creator creator = new Creator();
                creator.setType(verifyOrSetDefault(type));
                creator.setValue(verifyOrSetDefault(value));
                creator.setIndex(index);

                creators.add(creator);
                index++;
            }
        } catch (InvalidSPDXAnalysisException e) {
            log.error(e);
        }

        return creators;
    }

    private PackageInformation createPackageInfoFromSpdxPackage(String spdxDocId, SpdxPackage spdxPackage) throws SW360Exception, MalformedURLException {
        final PackageInformation pInfo = getPackageInformationFromSpdxDocument(spdxDocId);
        pInfo.setSpdxDocumentId(spdxDocId);

        try {
            final String name = spdxPackage.getName();
            final String spdxId = spdxPackage.getId();
            final String versionInfo = spdxPackage.getVersionInfo();
            final String packageFileName = spdxPackage.getPackageFileName();
            final String supplier = spdxPackage.getSupplier();
            final String originator = spdxPackage.getOriginator();
            final String downloadLocation = spdxPackage.getDownloadLocation();
            final boolean fileAnalyzed = spdxPackage.isFilesAnalyzed();
            final PackageVerificationCode PVC = createPVCFromSpdxPackage(spdxPackage);
            final Set<CheckSum> checksums = createCheckSumsFromSpdxChecksums(spdxPackage.getChecksums());
            final String homepage = spdxPackage.getHomepage();
            final String sourceInfo = spdxPackage.getSourceInfo();
            final String licenseConcluded = spdxPackage.getLicenseConcluded().toString();
            final Set<String> licenseInfosFromFiles = Arrays.stream(spdxPackage.getLicenseInfoFromFiles())
                                                        .map(license -> license.toString())
                                                        .collect(Collectors.toSet());
            final String licenseDeclared = spdxPackage.getLicenseDeclared().toString();
            final String licenseComment = spdxPackage.getLicenseComment();
            final String copyrightText = spdxPackage.getCopyrightText();
            final String summary = spdxPackage.getSummary();
            final String description = spdxPackage.getDescription();
            final String comment = spdxPackage.getComment();
            final Set<ExternalReference> externalRefs = createExternalReferenceFromSpdxPackage(spdxPackage);
            final Set<String> attributionText = new HashSet<String>(Arrays.asList(verifyOrSetDefault(spdxPackage.getAttributionText())));
            final Set<Annotations> annotations = createAnnotationsFromSpdxAnnotations(spdxPackage.getAnnotations());

            pInfo.setName(verifyOrSetDefault(name))
                .setSPDXID(verifyOrSetDefault(spdxId))
                .setVersionInfo(verifyOrSetDefault(versionInfo))
                .setPackageFileName(verifyOrSetDefault(packageFileName))
                .setSupplier(verifyOrSetDefault(supplier))
                .setOriginator(verifyOrSetDefault(originator))
                .setDownloadLocation(verifyOrSetDefault(downloadLocation))
                .setFilesAnalyzed(fileAnalyzed)
                .setPackageVerificationCode(PVC)
                .setChecksums(checksums)
                .setHomepage(verifyOrSetDefault(homepage))
                .setSourceInfo(verifyOrSetDefault(sourceInfo))
                .setLicenseConcluded(verifyOrSetDefault(licenseConcluded))
                .setLicenseInfoFromFiles(licenseInfosFromFiles)
                .setLicenseDeclared(verifyOrSetDefault(licenseDeclared))
                .setLicenseComments(verifyOrSetDefault(licenseComment))
                .setCopyrightText(verifyOrSetDefault(copyrightText))
                .setSummary(verifyOrSetDefault(summary))
                .setDescription(verifyOrSetDefault(description))
                .setPackageComment(verifyOrSetDefault(comment))
                .setExternalRefs(externalRefs)
                .setAttributionText(attributionText)
                .setAnnotations(annotations);
        } catch (InvalidSPDXAnalysisException e) {
            log.error(e);
        }

        return pInfo;
    }

    private PackageVerificationCode createPVCFromSpdxPackage(SpdxPackage spdxPackage) {
        final PackageVerificationCode PVC = new PackageVerificationCode();

        try {
            final SpdxPackageVerificationCode spdxPVC = spdxPackage.getPackageVerificationCode();
            final Set<String> excludedFileNames = new HashSet<String>(Arrays.asList(verifyOrSetDefault(spdxPVC.getExcludedFileNames())));
            final String value = spdxPVC.getValue();

            PVC.setExcludedFiles(excludedFileNames)
                .setValue(verifyOrSetDefault(value));
        } catch (InvalidSPDXAnalysisException e) {
            log.error(e);
        }

        return PVC;
    }

    private Set<ExternalReference> createExternalReferenceFromSpdxPackage(SpdxPackage spdxPackage) {
        Set<ExternalReference> refs = new HashSet<ExternalReference>();
        int index = 0;

        try {
            ExternalRef[] spdxExternalRefs = spdxPackage.getExternalRefs();
            for (ExternalRef spdxRef : spdxExternalRefs) {
                String category = spdxRef.getReferenceCategory().getTag();
                String locator = spdxRef.getReferenceLocator();
                String type = spdxRef.getReferenceType().toString();
                String comment = spdxRef.getComment();

                ExternalReference ref = new ExternalReference();
                ref.setReferenceCategory(verifyOrSetDefault(category))
                    .setReferenceLocator(verifyOrSetDefault(locator))
                    .setReferenceType(verifyOrSetDefault(type))
                    .setComment(verifyOrSetDefault(comment))
                    .setIndex(index);

                refs.add(ref);
                index++;
            }
        } catch (InvalidSPDXAnalysisException e) {
            log.error(e);
        }

        return refs;
    }

    private Set<FileInformation> createFilesInformationFromSpdxFiles(SpdxFile[] spdxFiles) {
        Set<FileInformation> files = new HashSet<FileInformation>();

        for (SpdxFile spdxFile : spdxFiles) {
            String name = spdxFile.getName();
            String id = spdxFile.getId();
            Set<String> fileTypes = Arrays.stream(spdxFile.getFileTypes())
                                        .map(type -> verifyOrSetDefault(type.getTag()))
                                        .collect(Collectors.toSet());
            Set<CheckSum> checksums = createCheckSumsFromSpdxChecksums(spdxFile.getChecksums());
            String licenseConcluded = spdxFile.getLicenseConcluded().toString();
            Set<String> licenseInfoFromFiles = Arrays.stream(spdxFile.getLicenseInfoFromFiles())
                                        .map(license -> verifyOrSetDefault(license.toString()))
                                        .collect(Collectors.toSet());
            String licenseComment = spdxFile.getLicenseComments();
            String copyrightText = spdxFile.getCopyrightText();
            String comment = spdxFile.getComment();
            String noticeText = spdxFile.getNoticeText();
            Set<String> fileContributors = new HashSet<String>(Arrays.asList(verifyOrSetDefault(spdxFile.getFileContributors())));
            Set<String> attributionText = new HashSet<String>(Arrays.asList(verifyOrSetDefault(spdxFile.getAttributionText())));
            Set<Annotations> annotations = createAnnotationsFromSpdxAnnotations(spdxFile.getAnnotations());

            FileInformation file = new FileInformation();
            file.setFileName(verifyOrSetDefault(name))
                .setSPDXID(verifyOrSetDefault(id))
                .setFileTypes(fileTypes)
                .setChecksums(checksums)
                .setLicenseConcluded(verifyOrSetDefault(licenseConcluded))
                .setLicenseInfoInFiles(licenseInfoFromFiles)
                .setLicenseComments(verifyOrSetDefault(licenseComment))
                .setCopyrightText(verifyOrSetDefault(copyrightText))
                .setFileComment(verifyOrSetDefault(comment))
                .setNoticeText(verifyOrSetDefault(noticeText))
                .setFileContributors(attributionText)
                .setFileAttributionText(attributionText)
                .setAnnotations(annotations);

            files.add(file);
        }

        return files;
    }

    private Set<CheckSum> createCheckSumsFromSpdxChecksums(Checksum[] spdxChecksums) {
        Set<CheckSum> checksums = new HashSet<CheckSum>();
        int index = 0;

        for (Checksum spdxChecksum : spdxChecksums) {
            String algorithm = org.spdx.rdfparser.model.Checksum.CHECKSUM_ALGORITHM_TO_TAG.get(spdxChecksum.getAlgorithm()).replace(":", "");
            String value = spdxChecksum.getValue();

            CheckSum checksum = new CheckSum();
            checksum.setAlgorithm(verifyOrSetDefault(algorithm))
                    .setChecksumValue(verifyOrSetDefault(value))
                    .setIndex(index);

            checksums.add(checksum);
            index++;
        }

        return checksums;
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

    private Optional<SpdxBOMImporterSink.Response> importAsRelease(SpdxElement relatedSpdxElement) throws SW360Exception {
        return importAsRelease(relatedSpdxElement, null, null, null, null);
    }

    private Optional<SpdxBOMImporterSink.Response> importAsRelease(SpdxElement relatedSpdxElement, AttachmentContent attachmentContent,
            SpdxDocument spdxDocument, String newReleaseVersion, String releaseId) throws SW360Exception {
        if (relatedSpdxElement instanceof SpdxPackage) {
            final SpdxPackage spdxPackage = (SpdxPackage) relatedSpdxElement;

            final Release release;
            SpdxBOMImporterSink.Response component;
            if (isNotNullEmptyOrWhitespace(releaseId)) {
                release = sink.getRelease(releaseId);
                component = new SpdxBOMImporterSink.Response(release.getComponentId(), true);
            } else {
                component = importAsComponent(spdxPackage);
                final String componentId = component.getId();

                release = createReleaseFromSpdxPackage(spdxPackage);
                if (isNotNullEmptyOrWhitespace(newReleaseVersion))
                    release.setVersion(newReleaseVersion);
                release.setComponentId(componentId);
            }

            final Relationship[] relationships = spdxPackage.getRelationships();
            List<SpdxBOMImporterSink.Response> releases = importAsReleases(relationships);
            Map<String, ReleaseRelationship> releaseIdToRelationship = makeReleaseIdToRelationship(releases);
            release.setReleaseIdToRelationship(releaseIdToRelationship);

            if(attachmentContent != null) {
                Attachment attachment = makeAttachmentFromContent(attachmentContent);
                release.setAttachments(Collections.singleton(attachment));
            }


            final SpdxBOMImporterSink.Response response = sink.addRelease(release);

            try {
                importSpdxDocument(response.getId(), spdxDocument, spdxPackage);
            } catch (MalformedURLException e) {
                log.error(e);
            }

            response.addChild(component);
            return Optional.of(response);
        } else {
            log.debug("Unsupported SpdxElement: " + relatedSpdxElement.getClass().getCanonicalName());
            return Optional.empty();
        }
    }

    private void importSpdxDocument(String releaseId, SpdxDocument spdxDocument, SpdxPackage spdxPackage) throws SW360Exception, MalformedURLException {
        final SPDXDocument spdxDoc = createSPDXDocumentFromSpdxDocument(releaseId, spdxDocument);
        final SpdxBOMImporterSink.Response spdxDocRes = sink.addOrUpdateSpdxDocument(spdxDoc);
        final String spdxDocId = spdxDocRes.getId();

        final DocumentCreationInformation docCreationInfo = createDocumentCreationInfoFromSpdxDocument(spdxDocId, spdxDocument);
        final SpdxBOMImporterSink.Response docCreationInfoRes = sink.addOrUpdateDocumentCreationInformation(docCreationInfo);
        final String docCreationInfoId = docCreationInfoRes.getId();

        final PackageInformation packageInfo = createPackageInfoFromSpdxPackage(spdxDocId, spdxPackage);
        final SpdxBOMImporterSink.Response packageInfoRes = sink.addOrUpdatePackageInformation(packageInfo);
        final String packageInfoId = packageInfoRes.getId();
    }

    private SPDXDocument getSpdxDocumentFromRelease(String releaseId) throws SW360Exception, MalformedURLException {
        SPDXDocument spdxDoc;
        final Release release = sink.getRelease(releaseId);
        if (release.isSetSpdxId()) {
            spdxDoc = sink.getSPDXDocument(release.getSpdxId());
        } else {
            spdxDoc = new SPDXDocument();
        }
        return spdxDoc;
    }

    private DocumentCreationInformation getDocCreationInfoFromSpdxDocument(String spdxDocId) throws SW360Exception, MalformedURLException {
        DocumentCreationInformation info;
        final SPDXDocument spdxDoc = sink.getSPDXDocument(spdxDocId);
        if (spdxDoc.isSetSpdxDocumentCreationInfoId()) {
            info = sink.getDocumentCreationInfo(spdxDoc.getSpdxDocumentCreationInfoId());
        } else {
            info = new DocumentCreationInformation();
        }
        return info;
    }

    private PackageInformation getPackageInformationFromSpdxDocument(String spdxDocId) throws SW360Exception, MalformedURLException {
        PackageInformation info;
        final SPDXDocument spdxDoc = sink.getSPDXDocument(spdxDocId);
        if (spdxDoc.getSpdxPackageInfoIdsSize() > 0) {
            info = sink.getPackageInfo(spdxDoc.getSpdxPackageInfoIds().iterator().next());
        } else {
            info = new PackageInformation();
        }
        return info;
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


    private String verifyOrSetDefault(String value) {
        return (isNotNullEmptyOrWhitespace(value)) ? value : "";
    }

    private String[] verifyOrSetDefault(String[] values) {
        return (values != null && values.length > 0) ? values : new String[0];
    }
}
