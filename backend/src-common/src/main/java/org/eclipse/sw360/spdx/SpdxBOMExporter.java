/*
 * Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2022. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.spdx;

import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.spdx.annotations.*;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.*;
import org.eclipse.sw360.datahandler.thrift.spdx.otherlicensinginformationdetected.*;
import org.eclipse.sw360.datahandler.thrift.spdx.relationshipsbetweenspdxelements.*;
import org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.*;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.*;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.license.ExtractedLicenseInfo;
import org.spdx.tools.SpdxConverter;
import org.spdx.tools.SpdxConverterException;
import org.spdx.tools.RdfToTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.eclipse.sw360.datahandler.couchdb.DatabaseMixInForSPDXDocument.*;
public class SpdxBOMExporter {
    private static final Logger log = LogManager.getLogger(SpdxBOMExporter.class);
    private final SpdxBOMExporterSink sink;
    private Set<ExtractedLicenseInfo> licenses = new HashSet<>();

    public SpdxBOMExporter(SpdxBOMExporterSink sink) {
        this.sink = sink;
    }

    public RequestSummary exportSPDXFile(String releaseId, String outputFormat) throws SW360Exception, MalformedURLException, InvalidSPDXAnalysisException {
        RequestSummary requestSummary = new RequestSummary();
        String verifyMessage = "";
        final String targetFileName = releaseId + "." + outputFormat.toLowerCase();
        log.info("Export to file: " + targetFileName);

        if (createSPDXJsonFomatFromSW360SPDX(releaseId)) {
            if (outputFormat.equals("JSON")) {
                //verifyMessage = convertJSONtoOutputFormat(targetFileName, releaseId + ".RDF");
                requestSummary.setMessage("Export to JSON format successfully !");
                return requestSummary.setRequestStatus(RequestStatus.SUCCESS);
            } else {
                verifyMessage = convertJSONtoOutputFormat(releaseId + ".json", targetFileName);
                if (verifyMessage.isEmpty()) {
                    log.info("Export to " + targetFileName + " sucessfully");
                    requestSummary.setMessage("Export to " + outputFormat + " format successfully !");
                    return requestSummary.setRequestStatus(RequestStatus.SUCCESS);
                } else {
                    log.error("Export to " + targetFileName + " error");
                    requestSummary.setMessage(verifyMessage);
                    return requestSummary.setRequestStatus(RequestStatus.FAILURE);
                }
            }
        } else {
            log.error("Export to " + targetFileName + " error !!!");
            requestSummary.setMessage("Export to " + targetFileName + " error !!!");
            return requestSummary.setRequestStatus(RequestStatus.FAILURE);
        }
    }

    private String convertJSONtoOutputFormat(String jsonFileName, String outputFileName ) {
        try {
            log.info("Convert " + jsonFileName + " to " + outputFileName);
            if (outputFileName.split("\\.")[1].equals("spdx")) {
                SpdxConverter.convert(jsonFileName, "tmp.rdf");
                RdfToTag.main(new String[] {"tmp.rdf", outputFileName});
            } else {
                SpdxConverter.convert(jsonFileName, outputFileName);
            }
        } catch (SpdxConverterException e) {
            log.error("Convert to " + outputFileName + " file error !!!");
            e.printStackTrace();
            return e.getMessage();
        }
        return "";
    }

    private boolean createSPDXJsonFomatFromSW360SPDX(String releaseId) throws SW360Exception {
        final SPDXDocument sw360SPDXDocument = getSpdxDocumentFromRelease(releaseId);
        final Set<PackageInformation> sw360PackageInformations = getPackagesInformationFromSpdxDocument(sw360SPDXDocument.getId());
        final DocumentCreationInformation sw360CreationInfo = getDocCreationInfoFromSpdxDocument(sw360SPDXDocument.getId());

        // creating JSONObject
        JSONObject SPDXJson = new JSONObject();
        Map<String, String> m = new LinkedHashMap<>();
        JSONParser parser = new JSONParser();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.addMixInAnnotations(Annotations.class, AnnotationsMixin.class);
        objectMapper.addMixInAnnotations(CheckSum.class, CheckSumMixin.class);
        objectMapper.addMixInAnnotations(ExternalReference.class, ExternalReferenceMixin.class);
        objectMapper.addMixInAnnotations(PackageInformation.class, PackageInformationMixin.class);
        objectMapper.addMixInAnnotations(ExternalDocumentReferences.class, ExternalDocumentReferencesMixin.class);
        objectMapper.addMixInAnnotations(SnippetInformation.class, SnippetInformationMixin.class);
        objectMapper.addMixInAnnotations(SnippetRange.class, SnippetRangeMixin.class);
        objectMapper.addMixInAnnotations(RelationshipsBetweenSPDXElements.class, RelationshipsBetweenSPDXElementsMixin.class);
        objectMapper.addMixInAnnotations(OtherLicensingInformationDetected.class, OtherLicensingInformationDetectedMixin.class);
        objectMapper.addMixInAnnotations(PackageVerificationCode.class, PackageVerificationCodeMixin.class);

        try {
            // put package infomation to SPDX json
            JSONArray SPDXPackageInfo = new JSONArray();
            JSONArray SDPXRelationships = (JSONArray) parser.parse(objectMapper.writeValueAsString(sw360SPDXDocument.getRelationships()));
            for (PackageInformation sw360PackageInfo : sw360PackageInformations) {
                log.info("Export Package Infomation: " + sw360PackageInfo.getName());
                JSONObject SW360SPDXPackageInfo = (JSONObject) parser.parse(objectMapper.writeValueAsString(sw360PackageInfo));

                if (sw360PackageInfo.getPackageVerificationCode() != null) {
                    JSONObject packageVerificationCode = new JSONObject();
                    JSONObject sw360packageVerificationCode = (JSONObject) parser.parse(objectMapper.writeValueAsString(sw360PackageInfo.getPackageVerificationCode()));
                    packageVerificationCode.put("packageVerificationCodeExcludedFiles", sw360packageVerificationCode.get("excludedFiles"));
                    packageVerificationCode.put("packageVerificationCodeValue", sw360packageVerificationCode.get("value"));
                    SW360SPDXPackageInfo.remove("packageVerificationCode");
                    SW360SPDXPackageInfo.put("packageVerificationCode", packageVerificationCode);
                }

                if (!sw360PackageInfo.getRelationships().isEmpty()) {
                    for (RelationshipsBetweenSPDXElements relationship : sw360PackageInfo.getRelationships()) {
                        JSONObject packageReleationship = (JSONObject) parser.parse(objectMapper.writeValueAsString(relationship));
                        SDPXRelationships.add(packageReleationship);
                    }
                }

                SW360SPDXPackageInfo.remove("relationships");
                SPDXPackageInfo.add(SW360SPDXPackageInfo);
            }
            SPDXJson.put("packages", SPDXPackageInfo);

            // put document creation infomation to SPDX json
            JSONObject SW360SPDXCreationInfo = (JSONObject) parser.parse(objectMapper.writeValueAsString(sw360CreationInfo));
            Set<String> keys = new HashSet<>(Arrays.asList("spdxVersion", "dataLicense", "SPDXID", "name", "documentNamespace", "externalDocumentRefs",
            "documentComment"));

            for (String key : keys) {
                if (key.equals("documentNamespace")) {
                    String documentNamespace =  SW360SPDXCreationInfo.get(key).toString();
                    if (documentNamespace.substring(documentNamespace.length() - 1).equals("#")) {
                        SPDXJson.put(key, documentNamespace.substring(0, documentNamespace.length() - 1));
                    } else {
                        SPDXJson.put(key, documentNamespace);
                    }
                } else {
                    SPDXJson.put(key, SW360SPDXCreationInfo.get(key));
                }
            }

            JSONObject creationInfo = new JSONObject();
            creationInfo.put("comment", SW360SPDXCreationInfo.get("creatorComment"));
            creationInfo.put("created", SW360SPDXCreationInfo.get("created"));
            creationInfo.put("licenseListVersion", SW360SPDXCreationInfo.get("licenseListVersion"));

            JSONArray SW360SPDXCreationInfoCreator = (JSONArray) parser.parse(objectMapper.writeValueAsString(sw360CreationInfo.getCreator()));
            JSONArray creators = new JSONArray();
            SW360SPDXCreationInfoCreator.forEach(c -> {
                JSONObject obj = (JSONObject) c;
                String type = (String) obj.get("type");
                String value = (String) obj.get("value");
                creators.add(type + ": " +value);
            });
            creationInfo.put("creators", creators);
            SPDXJson.put("creationInfo", creationInfo);


            // put spdx document to SPDX json
            JSONArray files = new JSONArray();
            for (SnippetInformation snippet : sw360SPDXDocument.getSnippets()) {
                if (!snippet.getSnippetFromFile().isEmpty()) {
                    JSONObject file = new JSONObject();
                    file.put("SPDXID", snippet.getSnippetFromFile());
                    file.put("fileName", snippet.getSnippetFromFile());
                    file.put("comment", "File information is generated from snippet and only SPDXID is correct information");
                    files.add(file);
                }
            }
            SPDXJson.put("files", files);

            JSONArray snippets = (JSONArray) parser.parse(objectMapper.writeValueAsString(sw360SPDXDocument.getSnippets()));
            snippets.forEach(s -> {
                JSONObject snippet = (JSONObject) s;
                JSONArray ranges = new JSONArray();

                JSONArray snippetRanges = (JSONArray) snippet.get("snippetRanges");
                snippetRanges.forEach(r -> {
                    JSONObject rangeElement = (JSONObject) r;
                    JSONObject range = new JSONObject();
                    if (rangeElement.get("rangeType").equals("LINE")) {
                        JSONObject startPointer = new JSONObject();
                        startPointer.put("lineNumber", rangeElement.get("startPointer"));
                        startPointer.put("reference", rangeElement.get("reference"));
                        range.put("startPointer", startPointer);

                        JSONObject endPointer = new JSONObject();
                        endPointer.put("lineNumber", rangeElement.get("endPointer"));
                        endPointer.put("reference", rangeElement.get("reference"));
                        range.put("endPointer", endPointer);
                    } else {
                        JSONObject startPointer = new JSONObject();
                        startPointer.put("offset", rangeElement.get("startPointer"));
                        startPointer.put("reference", rangeElement.get("reference"));
                        range.put("startPointer", startPointer);

                        JSONObject endPointer = new JSONObject();
                        endPointer.put("offset", rangeElement.get("endPointer"));
                        endPointer.put("reference", rangeElement.get("reference"));
                        range.put("endPointer", endPointer);
                    }
                    ranges.add(range);
                });
                snippet.remove("snippetRanges");
                snippet.put("ranges", ranges);
            });
            SPDXJson.put("snippets", snippets);

            SPDXJson.put("relationships", SDPXRelationships);
            SPDXJson.put("annotations", (JSONArray) parser.parse(objectMapper.writeValueAsString(sw360SPDXDocument.getAnnotations())));
            SPDXJson.put("hasExtractedLicensingInfos", (JSONArray) parser.parse(objectMapper.writeValueAsString(sw360SPDXDocument.getOtherLicensingInformationDetecteds())));

            PrintWriter pw = new PrintWriter(releaseId + ".json");
            pw.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(SPDXJson));
            pw.flush();
            pw.close();
        } catch (ParseException | JsonProcessingException | FileNotFoundException e) {
            log.error("Can not convert SW360 SPDX Document to Json");
            e.printStackTrace();
            return false;
        }

        return true;
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

    private Set<PackageInformation> getPackagesInformationFromSpdxDocument(String spdxDocId) throws SW360Exception {
        Set<PackageInformation> infos = new HashSet<>();
        final SPDXDocument spdxDoc = sink.getSPDXDocument(spdxDocId);
        for (String packageId : spdxDoc.getSpdxPackageInfoIds()) {
            PackageInformation info = sink.getPackageInfo(packageId);
            infos.add(info);
        }
        return infos;
    }

}
