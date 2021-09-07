package org.eclipse.sw360.spdx;

import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.spdx.annotations.*;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.*;
import org.eclipse.sw360.datahandler.thrift.spdx.otherlicensinginformationdetected.*;
import org.eclipse.sw360.datahandler.thrift.spdx.relationshipsbetweenspdxelements.*;
import org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.*;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.*;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.*;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SpdxDocumentContainer;
import org.spdx.rdfparser.model.*;
import org.spdx.rdfparser.model.Checksum.ChecksumAlgorithm;
import org.spdx.rdfparser.model.ExternalRef.ReferenceCategory;
import org.spdx.rdfparser.model.Relationship.RelationshipType;
import org.spdx.rdfparser.model.pointer.*;
import org.spdx.rdfparser.referencetype.ReferenceType;
import org.spdx.tools.SpdxConverter;
import org.spdx.tools.SpdxConverterException;
import org.spdx.tools.TagToRDF;

import org.spdx.tag.CommonCode;
import org.spdx.rdfparser.license.ExtractedLicenseInfo;
import org.spdx.rdfparser.SpdxPackageVerificationCode;
import org.spdx.rdfparser.SpdxRdfConstants;
import org.spdx.rdfparser.license.AnyLicenseInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.io.FileInputStream;

public class SpdxBOMExporter {
    private static final Logger log = LogManager.getLogger(SpdxBOMExporter.class);
    private final SpdxBOMExporterSink sink;
    private Set<ExtractedLicenseInfo> licenses = new HashSet<>();

    public SpdxBOMExporter(SpdxBOMExporterSink sink) {
        this.sink = sink;
    }

    public RequestSummary exportSPDXFile(String releaseId, String outputFormat) throws SW360Exception, MalformedURLException, InvalidSPDXAnalysisException, URISyntaxException {
        RequestSummary requestSummary = new RequestSummary();
        SpdxDocument doc = null;
        try {
            log.info("Creating SpdxDocument object from sw360spdx...");
            doc = createSpdxDocumentFromSw360Spdx(releaseId);
        } catch (Exception e) {
            log.error("Error create SpdxDocument: " + e.getMessage());
        }

        final String targetFileName = releaseId + "." + outputFormat.toLowerCase();
        log.info("Export to file: " + targetFileName);

        if (outputFormat.equals("SPDX")) {
            convertSpdxDocumentToTagFile(doc, targetFileName);
            return requestSummary.setRequestStatus(RequestStatus.SUCCESS);
        }

        String sourceFileName = releaseId + ".spdx";
        File sourceFile = convertSpdxDocumentToTagFile(doc, sourceFileName);
        if (outputFormat.equals("RDF")) {
            convertTagToRdf(sourceFile, targetFileName);
            return requestSummary.setRequestStatus(RequestStatus.SUCCESS);
        } else {
            // todo: can update when no need temporary file .rdf 
            try {
                String rdfFileName = releaseId+".rdf";
                convertTagToRdf(sourceFile, rdfFileName);
                SpdxConverter.convert(rdfFileName, targetFileName);
                return requestSummary.setRequestStatus(RequestStatus.SUCCESS);
            } catch (SpdxConverterException e) {
                e.printStackTrace();
            }
        }

        return requestSummary.setRequestStatus(RequestStatus.FAILURE);
    }

    private File convertSpdxDocumentToTagFile(SpdxDocument doc, String tagFileName) {
        File spdxTagFile = new File(tagFileName);
        PrintWriter out = null;
        List<String> verify = new LinkedList<String>();

        try {
            try {
                out = new PrintWriter(spdxTagFile, "UTF-8");
            } catch (IOException e1) {
                log.error("Could not write to the new SPDX Tag file "+ spdxTagFile.getPath() + "due to error " + e1.getMessage());
            }
            try {
                verify = doc.verify();
                if (!verify.isEmpty()) {
                    log.warn("This SPDX Document is not valid due to:");
                    for (int i = 0; i < verify.size(); i++) {
                        log.warn("\t" + verify.get(i));
                    }
                }

                Properties constants = CommonCode.getTextFromProperties("org/spdx/tag/SpdxTagValueConstants.properties");
                CommonCode.printDoc(doc, out, constants);
                
            } catch (Exception e) {
                log.error("Error transalting SPDX Document to tag-value format: " + e.getMessage());
            }
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }

        return spdxTagFile;
    }

    private void convertTagToRdf(File spdxTagFile, String targetFileName) {
        FileInputStream spdxTagStream = null;

        try {
            spdxTagStream = new FileInputStream(spdxTagFile);
        } catch (FileNotFoundException e2) {
            e2.printStackTrace();
        }

        File spdxRDFFile = new File(targetFileName);
        String outputFormat = "RDF/XML";
		FileOutputStream outStream = null;
		try {
			outStream = new FileOutputStream(spdxRDFFile);
		} catch (FileNotFoundException e1) {
			try {
				spdxTagStream.close();
			} catch (IOException e) {
                log.error("Warning: Unable to close input file on error.");
			}
			log.error("Could not write to the new SPDX RDF file "+ spdxRDFFile.getPath() + "due to error " + e1.getMessage());
        }
        
		List<String> warnings = new ArrayList<String>();
		try {
			TagToRDF.convertTagFileToRdf(spdxTagStream, outStream, outputFormat, warnings);
			if (!warnings.isEmpty()) {
				log.warn("The following warnings and or verification errors were found:");
				for (String warning:warnings) {
					log.warn("\t"+warning);
				}
            }            
		} catch (Exception e) {
			log.error("Error creating SPDX Analysis: " + e.getMessage());
		} finally {
			if (outStream != null) {
				try {
					outStream.close();
				} catch (IOException e) {
					log.error("Error closing RDF file: " + e.getMessage());
				}
			}
			if (spdxTagStream != null) {
				try {
					spdxTagStream.close();
				} catch (IOException e) {
					log.error("Error closing Tag/Value file: " + e.getMessage());
				}
			}
		}
    }

    private SpdxDocument createSpdxDocumentFromSw360Spdx(String releaseId) throws SW360Exception, MalformedURLException, InvalidSPDXAnalysisException, URISyntaxException {
        final SPDXDocument sw360SPDXDocument = getSpdxDocumentFromRelease(releaseId);

        SpdxDocument spdxDocument = createSpdxDocumentCreationInfoFromSw360DocumentCreationInfo(sw360SPDXDocument.getId(), sw360SPDXDocument);
        
        final Set<SnippetInformation> snippetInfos = sw360SPDXDocument.getSnippets();
        final Set<RelationshipsBetweenSPDXElements> relationships = sw360SPDXDocument.getRelationships();
        final Set<Annotations> annotations = sw360SPDXDocument.getAnnotations();


        final SpdxSnippet[] spdxSnippets = createSpdxSnippetsFromSw360Snippets(snippetInfos);
        final Relationship[] spdxRelationships = createSpdxRelationshipsFromSw360Relationships(relationships, sw360SPDXDocument.getId());
        final Annotation[] spdxAnnotations = createSpdxAnnotationsFromSw360Annotations(annotations);

        try{

            for (SpdxSnippet spdxSnippet : spdxSnippets) {
                spdxDocument.getDocumentContainer().addElement(spdxSnippet);
            }
            spdxDocument.setRelationships(spdxRelationships);
            spdxDocument.setAnnotations(spdxAnnotations);
        } catch (Exception e) {
            log.error("Error setSPDXDocument: " +e);
        }

        return spdxDocument;
    }

    private SpdxSnippet[] createSpdxSnippetsFromSw360Snippets (Set<SnippetInformation> sw360SnippetInfos) throws InvalidSPDXAnalysisException {
        List<SpdxSnippet> spdxSnippets = new ArrayList<>();
        for (SnippetInformation sw360SnippetInfo : sw360SnippetInfos) {
            SpdxSnippet snippet = new SpdxSnippet("snippetName", null, null, null, null, null, null, null, null, null, null); // name is updated at line 270

            String spdxId = sw360SnippetInfo.getSPDXID();
            snippet.setId(spdxId);

            String spdxSnippetFromFileName = sw360SnippetInfo.getSnippetFromFile();
            SpdxFile spdxSnippetFromFile = new SpdxFile(spdxSnippetFromFileName, null, null, null, null, null, SpdxRdfConstants.NOASSERTION_VALUE,
            null, null, null, null, null, null);
            //spdxSnippetFromFile.setName(spdxSnippetFromFileName);
            
            // todo:
            // fix Misssing sha and checksum workaroud
            // fix Missing required concluded license
            // value is hardcode
            // ChecksumAlgorithm algorithm = Checksum.CHECKSUM_TAG_TO_ALGORITHM.get("SHA1:");
            // Checksum checksum = new Checksum(algorithm, "d6a770ba38583ed4bb4525bd96e50461655d1997");

            // spdxSnippetFromFile.addChecksum(checksum);
            // ExtractedLicenseInfo spdxlicenseConcluded1 = new ExtractedLicenseInfo("LicenseRef-namnp", "");
            // spdxSnippetFromFile.setLicenseConcluded(spdxlicenseConcluded1);
            
            snippet.setSnippetFromFile(spdxSnippetFromFile);

            StartEndPointer spdxByteRange;
            Integer OFFSET1_1 = new Integer(new ArrayList<>(sw360SnippetInfo.getSnippetRanges()).get(0).getStartPointer());
            Integer OFFSET1_2 = new Integer(new ArrayList<>(sw360SnippetInfo.getSnippetRanges()).get(0).getEndPointer());
            ByteOffsetPointer BOP_POINTER1_1 = new ByteOffsetPointer(spdxSnippetFromFile, OFFSET1_1);
            ByteOffsetPointer BOP_POINTER1_2 = new ByteOffsetPointer(spdxSnippetFromFile, OFFSET1_2);
            spdxByteRange = new StartEndPointer(BOP_POINTER1_1, BOP_POINTER1_2);
            snippet.setByteRange(spdxByteRange);

            StartEndPointer spdxLineRange;
            Integer LINE1_1 = new Integer(new ArrayList<>(sw360SnippetInfo.getSnippetRanges()).get(1).getStartPointer());
            Integer LINE1_2 = new Integer(new ArrayList<>(sw360SnippetInfo.getSnippetRanges()).get(1).getEndPointer());
            LineCharPointer LCP_POINTER1_1 = new LineCharPointer(spdxSnippetFromFile, LINE1_1);
            LineCharPointer LCP_POINTER1_2 = new LineCharPointer(spdxSnippetFromFile, LINE1_2);
            spdxLineRange = new StartEndPointer(LCP_POINTER1_1, LCP_POINTER1_2);
            snippet.setLineRange(spdxLineRange);

            ExtractedLicenseInfo spdxlicenseConcluded = new ExtractedLicenseInfo(sw360SnippetInfo.getLicenseConcluded(), "");
            snippet.setLicenseConcluded(existedLicense(spdxlicenseConcluded));

            List<AnyLicenseInfo> spdxLicenseInfoFromFiles = new ArrayList<>();
            for (String sw360LicenseInfoInSnippet : sw360SnippetInfo.getLicenseInfoInSnippets()) {
                ExtractedLicenseInfo license = new ExtractedLicenseInfo(sw360LicenseInfoInSnippet, "");
                spdxLicenseInfoFromFiles.add(existedLicense(license));
            }
            snippet.setLicenseInfosFromFiles(spdxLicenseInfoFromFiles.toArray(AnyLicenseInfo[]::new));

            String spdxLicenseComment = sw360SnippetInfo.getLicenseComments();
            snippet.setLicenseComment(spdxLicenseComment);

            String copyrightText = sw360SnippetInfo.getCopyrightText();
            snippet.setCopyrightText(copyrightText);

            String comment = sw360SnippetInfo.getComment();
            snippet.setComment(comment);

            String name= sw360SnippetInfo.getName();
            snippet.setName(name);
            
            String[] attributionText= sw360SnippetInfo.getSnippetAttributionText().split("|");
            snippet.setAttributionText(attributionText);

            spdxSnippets.add(snippet);
        }
        return spdxSnippets.toArray(SpdxSnippet[]::new);
    }


    private Relationship[] createSpdxRelationshipsFromSw360Relationships(Set<RelationshipsBetweenSPDXElements> sw360Relationships, String SPDXDocId) throws InvalidSPDXAnalysisException {
        List<Relationship> spdxRelationships = new ArrayList<>();
        List<RelationshipsBetweenSPDXElements> list = new ArrayList(sw360Relationships);
        Collections.reverse(list);
        Set<RelationshipsBetweenSPDXElements> resultSet = new LinkedHashSet(list);
        boolean checkIsPackageInfo = false;

        for (RelationshipsBetweenSPDXElements sw360Relationship : resultSet) {
            // todo: setId for relatedSpdxElement
            // relatedSpdxElement.setId(sw360Relationship.getSpdxElementId());
            
            RelationshipType relationshipType;
            relationshipType = RelationshipType.fromString(sw360Relationship.getRelationshipType());
        
            String comment = sw360Relationship.getRelationshipComment();

            if (relationshipType == Relationship.RelationshipType.DESCRIBES && checkIsPackageInfo == false) {
                SpdxPackage relatedSpdxPackage = new SpdxPackage(sw360Relationship.getRelatedSpdxElement(), null,null, null, 
                null, null, null, null, null, null,null, null, null, null, null, null, null, null, null, null, null, true, null);
                try {
                    createSpdxPackageInfoFromSw360PackageInfo(relatedSpdxPackage, SPDXDocId);
                } catch (SW360Exception | MalformedURLException | URISyntaxException e) {
                    e.printStackTrace();
                }

                Relationship relationship = new Relationship(relatedSpdxPackage, relationshipType, comment);
                
                spdxRelationships.add(relationship);
                checkIsPackageInfo = true;
            } else {
                SpdxElement relatedSpdxElement = new SpdxElement(sw360Relationship.getRelatedSpdxElement(), null, null, null);
                Relationship relationship = new Relationship(relatedSpdxElement, relationshipType, comment);
                spdxRelationships.add(relationship);
            }
        }

        return spdxRelationships.toArray(Relationship[]::new);
    }


    private Annotation[] createSpdxAnnotationsFromSw360Annotations(Set<Annotations> sw360Annotations) throws InvalidSPDXAnalysisException {
        List<Annotation> spdxAnnotations = new ArrayList<>();
        for (Annotations sw360Annotation : sw360Annotations) {
            Annotation annotation = new Annotation(null, null, null, null);
            annotation.setAnnotator(sw360Annotation.getAnnotator());
            annotation.setAnnotationDate(sw360Annotation.getAnnotationDate());
            annotation.setAnnotationType(Annotation.TAG_TO_ANNOTATION_TYPE.get(sw360Annotation.getAnnotationType()));
            annotation.setComment(sw360Annotation.getAnnotationComment());

            spdxAnnotations.add(annotation);
        }

        return spdxAnnotations.toArray(Annotation[]::new);
    }

    private ExtractedLicenseInfo[] createSpdxExtractedLicenseInfoFromSw360ExtractedLicenseInfo(Set<OtherLicensingInformationDetected> sw360OtherLicenses) {
        List<ExtractedLicenseInfo> spdxExtractedLicenseInfo = new ArrayList<>();
        for (OtherLicensingInformationDetected sw360OtherLicense: sw360OtherLicenses) {
            ExtractedLicenseInfo extractedLicenseInfo = new ExtractedLicenseInfo(null, null, null, null, null);
            extractedLicenseInfo.setLicenseId(sw360OtherLicense.getLicenseId());
            extractedLicenseInfo.setExtractedText(sw360OtherLicense.getExtractedText());
            extractedLicenseInfo.setName(sw360OtherLicense.getLicenseName());
            extractedLicenseInfo.setCrossRef(sw360OtherLicense.getLicenseCrossRefs().toArray(String[]::new));
            extractedLicenseInfo.setComment(sw360OtherLicense.getLicenseComment());
            spdxExtractedLicenseInfo.add(existedLicense(extractedLicenseInfo));
        }

        return spdxExtractedLicenseInfo.toArray(ExtractedLicenseInfo[]::new);
    }

    private SpdxDocument createSpdxDocumentCreationInfoFromSw360DocumentCreationInfo(String sw360SpdxDocId, SPDXDocument sw360SPDXDocument) throws SW360Exception, MalformedURLException, InvalidSPDXAnalysisException {
        DocumentCreationInformation sw360DocumentCreationInformation = getDocCreationInfoFromSpdxDocument(sw360SpdxDocId);        

        SpdxDocumentContainer documentContainer = new SpdxDocumentContainer(sw360DocumentCreationInformation.getDocumentNamespace(), sw360DocumentCreationInformation.getSpdxVersion());
        SpdxDocument spdxDocument = documentContainer.getSpdxDocument();

        // set other license firstly because only in this license has text in sw360 spdx
        final Set<OtherLicensingInformationDetected> otherLicenses = sw360SPDXDocument.getOtherLicensingInformationDetecteds();
        final ExtractedLicenseInfo[] extractedLicenseInfos = createSpdxExtractedLicenseInfoFromSw360ExtractedLicenseInfo(otherLicenses);
        spdxDocument.setExtractedLicenseInfos(extractedLicenseInfos);

        spdxDocument.setSpecVersion(sw360DocumentCreationInformation.getSpdxVersion());

        ExtractedLicenseInfo dataLicense = new ExtractedLicenseInfo(sw360DocumentCreationInformation.getDataLicense(), "");
        spdxDocument.setDataLicense(existedLicense(dataLicense));
        
        // todo: can not set a file ID for an SPDX element already in an RDF Model. You must create a new SPDX File with this ID.
        // spdxDocument.setId(sw360DocumentCreationInformation.getSPDXID());

        spdxDocument.setName(sw360DocumentCreationInformation.getName());

        // documentNamespace set in new SpdxDocumentContainer()

        Set<ExternalDocumentReferences> sw360Refs = sw360DocumentCreationInformation.getExternalDocumentRefs();
        List<ExternalDocumentRef> externalDocumentRefs = new ArrayList<>();
        for (ExternalDocumentReferences sw360Ref : sw360Refs) { 
            ChecksumAlgorithm algorithm = org.spdx.rdfparser.model.Checksum.CHECKSUM_TAG_TO_ALGORITHM.get(sw360Ref.getChecksum().getAlgorithm() +":");
            org.spdx.rdfparser.model.Checksum checksum = new org.spdx.rdfparser.model.Checksum(algorithm, sw360Ref.getChecksum().getChecksumValue());
            ExternalDocumentRef externalDocumentRef = new ExternalDocumentRef(sw360Ref.getSpdxDocument(), checksum, sw360Ref.getExternalDocumentId());

            externalDocumentRefs.add(externalDocumentRef);
        }
        spdxDocument.getDocumentContainer().setExternalDocumentRefs(externalDocumentRefs.toArray(ExternalDocumentRef[]::new));

        spdxDocument.getCreationInfo().setLicenseListVersion(sw360DocumentCreationInformation.getLicenseListVersion());

        List<String> creators = new ArrayList<>();
        for (Creator sw360Creator : sw360DocumentCreationInformation.getCreator()) {
            String creator = sw360Creator.getType() + ": " +sw360Creator.getValue();
            creators.add(creator);
        }
        spdxDocument.getCreationInfo().setCreators(creators.toArray(String[]::new));

        spdxDocument.getCreationInfo().setCreated(sw360DocumentCreationInformation.getCreated());

        spdxDocument.getCreationInfo().setComment(sw360DocumentCreationInformation.getCreatorComment());

        spdxDocument.setComment(sw360DocumentCreationInformation.getDocumentComment());


        return spdxDocument;
    }
    
    
    private void createSpdxPackageInfoFromSw360PackageInfo(SpdxPackage spdxPackage, String sw360SpdxDocId) throws SW360Exception, MalformedURLException, InvalidSPDXAnalysisException, URISyntaxException {
        final PackageInformation sw360PackageInfo = getPackageInformationFromSpdxDocument(sw360SpdxDocId);

        spdxPackage.setId(sw360PackageInfo.getSPDXID());

        spdxPackage.setVersionInfo(sw360PackageInfo.getVersionInfo());

        spdxPackage.setPackageFileName(sw360PackageInfo.getPackageFileName());

        spdxPackage.setSupplier(sw360PackageInfo.getSupplier());

        spdxPackage.setOriginator(sw360PackageInfo.getOriginator());

        spdxPackage.setDownloadLocation(sw360PackageInfo.getDownloadLocation());

        spdxPackage.setFilesAnalyzed(sw360PackageInfo.isFilesAnalyzed());

        SpdxPackageVerificationCode packageVerificationCode = new SpdxPackageVerificationCode(null, sw360PackageInfo.getPackageVerificationCode().getExcludedFiles().toArray(String[]::new));
        packageVerificationCode.setValue(sw360PackageInfo.getPackageVerificationCode().getValue());
        spdxPackage.setPackageVerificationCode(packageVerificationCode);

        spdxPackage.setHomepage(sw360PackageInfo.getHomepage());

        spdxPackage.setSourceInfo(sw360PackageInfo.getSourceInfo());

        ExtractedLicenseInfo licenseConcluded = new ExtractedLicenseInfo(sw360PackageInfo.getLicenseConcluded(), "");
        spdxPackage.setLicenseConcluded(existedLicense(licenseConcluded));

        List<AnyLicenseInfo> licenseInfoFromFiles = new ArrayList<>();
        for (String sw360licenseInfoFromFile : sw360PackageInfo.getLicenseInfoFromFiles()) {
            ExtractedLicenseInfo license = new ExtractedLicenseInfo(sw360licenseInfoFromFile, "");
            licenseInfoFromFiles.add(existedLicense(license));
        }
        spdxPackage.setLicenseInfosFromFiles(licenseInfoFromFiles.toArray(AnyLicenseInfo[]::new));

        ExtractedLicenseInfo licenseDeclared = new ExtractedLicenseInfo(sw360PackageInfo.getLicenseDeclared(), "");
        spdxPackage.setLicenseDeclared(existedLicense(licenseDeclared));

        spdxPackage.setLicenseComment(sw360PackageInfo.getLicenseComments());

        spdxPackage.setCopyrightText(sw360PackageInfo.getCopyrightText());

        spdxPackage.setSummary(sw360PackageInfo.getSummary());

        spdxPackage.setDescription(sw360PackageInfo.getDescription());

        spdxPackage.setComment(sw360PackageInfo.getPackageComment());

        List<ExternalRef> externalRefs = new ArrayList<>();
        for (ExternalReference sw360Ref : sw360PackageInfo.getExternalRefs()) {
            ReferenceCategory referenceCategory = ReferenceCategory.fromTag(sw360Ref.getReferenceCategory());
            URI uri = new URI(sw360Ref.getReferenceType());
            ReferenceType referenceType = new ReferenceType(uri, null, null, null);
            ExternalRef externalRef = new ExternalRef(referenceCategory, referenceType , sw360Ref.getReferenceLocator(), sw360Ref.getComment());
            externalRefs.add(externalRef);
        }
        spdxPackage.setExternalRefs(externalRefs.toArray(ExternalRef[]::new));

        spdxPackage.setAttributionText(sw360PackageInfo.getAttributionText().toArray(String[]::new));

        List<Annotation> annotations = new ArrayList<>();
        for (Annotations sw360Annotation: sw360PackageInfo.getAnnotations()) {
            Annotation annotation = new Annotation(sw360Annotation.getAnnotator(), Annotation.TAG_TO_ANNOTATION_TYPE.get(sw360Annotation.getAnnotationType()), 
            sw360Annotation.getAnnotationDate(), sw360Annotation.getAnnotationComment());
            annotations.add(annotation);
        }

        spdxPackage.setAnnotations(annotations.toArray(Annotation[]::new));
    }

    private SPDXDocument getSpdxDocumentFromRelease(String releaseId) throws SW360Exception {
        SPDXDocument spdxDoc;
        spdxDoc = null;
        final Release release = sink.getRelease(releaseId);
        if (release.isSetSpdxId()) {
            spdxDoc = sink.getSPDXDocument(release.getSpdxId());
        }
        return spdxDoc;
    }

    private DocumentCreationInformation getDocCreationInfoFromSpdxDocument(String spdxDocId) throws SW360Exception {
        DocumentCreationInformation info = null;
        final SPDXDocument spdxDoc = sink.getSPDXDocument(spdxDocId);
        if (spdxDoc.isSetSpdxDocumentCreationInfoId()) {
            info = sink.getDocumentCreationInfo(spdxDoc.getSpdxDocumentCreationInfoId());
        }
        return info;
    }

    private PackageInformation getPackageInformationFromSpdxDocument(String spdxDocId) throws SW360Exception {
        PackageInformation info = null;
        final SPDXDocument spdxDoc = sink.getSPDXDocument(spdxDocId);
        if (spdxDoc.getSpdxPackageInfoIdsSize() > 0) {
            info = sink.getPackageInfo(spdxDoc.getSpdxPackageInfoIds().iterator().next());
        }
        return info;
    }

    private ExtractedLicenseInfo existedLicense(ExtractedLicenseInfo license) {
        if (!licenses.isEmpty()) {
            for (ExtractedLicenseInfo existedLicense : licenses) {
                if (existedLicense.getLicenseId().equals(license.getLicenseId())) {
                    return existedLicense;
                }
            }
            licenses.add(license);
        } else {
            licenses.add(license);
        }
        return license;
    }
}
