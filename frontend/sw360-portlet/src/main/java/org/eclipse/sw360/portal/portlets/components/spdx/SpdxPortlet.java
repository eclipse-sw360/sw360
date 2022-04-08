/*
 * Copyright TOSHIBA CORPORATION, 2021. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.portal.portlets.components.spdx;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.spdx.annotations.Annotations;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.*;
import org.eclipse.sw360.datahandler.thrift.spdx.otherlicensinginformationdetected.OtherLicensingInformationDetected;
import org.eclipse.sw360.datahandler.thrift.spdx.relationshipsbetweenspdxelements.RelationshipsBetweenSPDXElements;
import org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetRange;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocumentService;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.ExternalReference;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformationService;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageVerificationCode;
import org.eclipse.sw360.datahandler.thrift.users.User;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;

public abstract class SpdxPortlet {

    private SpdxPortlet() {
        // Utility class with only static functions
    }

    private static final Logger log = LogManager.getLogger(SpdxPortlet.class);
    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static SPDXDocument parseSPDXDocumentFromRequest(String jsonData) {
        SPDXDocument spdx = new SPDXDocument();
        if (jsonData == null) {
            return null;
        }
        try {
            JSONObject json = JSONFactoryUtil.createJSONObject(jsonData);
            Set<SnippetInformation> snippets = parseSnippets(json);
            json.remove("snippets");
            Set<RelationshipsBetweenSPDXElements> relationships = parseRelationships(json);
            json.remove("relationships");
            Set<Annotations> annotations = parseAnnotations(json);
            json.remove("annotations");
            Set<OtherLicensingInformationDetected> licensingInfo = parseLicensingInfo(json);
            json.remove("otherLicensingInformationDetecteds");
            json.remove("documentState");
            json.remove("permissions");
            try {
                spdx = mapper.readValue(json.toJSONString(), SPDXDocument.class);
            } catch (JsonProcessingException e) {
                log.error("Error when read value json",e);
            }
            spdx.setSnippets(snippets);
            spdx.setRelationships(relationships);
            spdx.setAnnotations(annotations);
            spdx.setOtherLicensingInformationDetecteds(licensingInfo);
        } catch (JSONException e) {
            log.error("Error when parse SPDXDocument",e);
        }
        return spdx;
    }

    private static DocumentCreationInformation parseDocumentCreationInfoFromRequest(String jsonData) {
        DocumentCreationInformation documentCreationInfo = new DocumentCreationInformation();
        if (jsonData == null) {
            return null;
        }
        try {
            JSONObject json = JSONFactoryUtil.createJSONObject(jsonData);
            Set<ExternalDocumentReferences> externalDocumentRefs = parseExternalDocumentReferences(json);
            json.remove("externalDocumentRefs");
            Set<Creator> creator = parseCreator(json);
            json.remove("creator");
            json.remove("documentState");
            json.remove("permissions");
            try {
                documentCreationInfo = mapper.readValue(json.toJSONString(), DocumentCreationInformation.class);
            } catch (JsonProcessingException e) {
                log.error("Error when read value json",e);
            }
            documentCreationInfo.setExternalDocumentRefs(externalDocumentRefs);
            documentCreationInfo.setCreator(creator);
        } catch (JSONException e) {
            log.error("Error when parse documentCreationInfo",e);
        }
        return documentCreationInfo;
    }

    private static PackageInformation parsePackageInfoFromRequest(String jsonData) {
        PackageInformation packageInfo = new PackageInformation();
        if (jsonData == null) {
            return null;
        }
        try {
            JSONObject json = JSONFactoryUtil.createJSONObject(jsonData);
            PackageVerificationCode packageVerificationCode = parsePackageVerificationCode(json);
            json.remove("packageVerificationCode");
            Set<CheckSum> checksums = parseChecksum(json);
            json.remove("checksums");
            Set<ExternalReference> externalRefs = parseExternalReference(json);
            json.remove("externalReferences");
            Set<Annotations> annotations = parseAnnotations(json);
            json.remove("annotations");
            json.remove("documentState");
            json.remove("permissions");
            try {
                packageInfo = mapper.readValue(json.toJSONString(), PackageInformation.class);
            } catch (JsonProcessingException e) {
                log.error("Error when read value json",e);
            }
            packageInfo.setPackageVerificationCode(packageVerificationCode);
            packageInfo.setChecksums(checksums);
            packageInfo.setExternalRefs(externalRefs);
            packageInfo.setAnnotations(annotations);
        } catch (JSONException e) {
            log.error("Error when parse packageInfo",e);
        }
        return packageInfo;
    }

    private static Set<PackageInformation> parsePackageInfosFromRequest(String jsonData) {
        Set<PackageInformation> packageInfos = new HashSet<>();
        if (jsonData == null) {
            return null;
        }
        try {
            JSONArray arrayPackages = JSONFactoryUtil.createJSONArray(jsonData);
            if (arrayPackages == null) {
                return packageInfos;
            }
            for (int i = 0; i < arrayPackages.length(); i++) {
                PackageInformation packageInfo = parsePackageInfoFromRequest(arrayPackages.getJSONObject(i).toJSONString());
                if (packageInfo != null) {
                    packageInfos.add(packageInfo);
                }
            }
        } catch (JSONException e) {
            log.error("Error when parse packageInfo",e);
        }
        return packageInfos;
    }

    public static void updateSPDX(ActionRequest request, ActionResponse response, User user, String releaseId, boolean addNew) throws TException {
        String spdxDocumentData;
        String documentCreationInfoData;
        String packageInfoData;
        String spdxDocumentId = "";

        if (addNew) {
            spdxDocumentData = (String) request.getAttribute(SPDXDocument._Fields.TYPE.toString());
            documentCreationInfoData = (String) request.getAttribute(SPDXDocument._Fields.SPDX_DOCUMENT_CREATION_INFO_ID.toString());
            packageInfoData = (String) request.getAttribute(SPDXDocument._Fields.SPDX_PACKAGE_INFO_IDS.toString());
        } else {
            spdxDocumentData = request.getParameter(SPDXDocument._Fields.TYPE.toString());
            documentCreationInfoData = request.getParameter(SPDXDocument._Fields.SPDX_DOCUMENT_CREATION_INFO_ID.toString());
            packageInfoData = request.getParameter(SPDXDocument._Fields.SPDX_PACKAGE_INFO_IDS.toString());
        }
        if (!isNullOrEmpty(spdxDocumentData)) {
            SPDXDocument spdx = parseSPDXDocumentFromRequest(spdxDocumentData);
            SPDXDocumentService.Iface spdxClient = new ThriftClients().makeSPDXClient();
            if (spdx != null) {
                if (isNullOrEmpty(spdx.getReleaseId()) && !isNullOrEmpty(releaseId)) {
                    spdx.setReleaseId(releaseId);
                }
                if (isNullOrEmpty(spdx.getId())) {
                    spdx.unsetId();
                    spdx.unsetRevision();
                    spdxDocumentId = spdxClient.addSPDXDocument(spdx, user).getId();
                } else {
                    spdxClient.updateSPDXDocument(spdx, user);
                    spdxDocumentId = spdx.getId();
                }
            }
        }
        if (!isNullOrEmpty(documentCreationInfoData)) {
            DocumentCreationInformation document = parseDocumentCreationInfoFromRequest(documentCreationInfoData);
            if (document != null) {
                DocumentCreationInformationService.Iface documentClient = new ThriftClients().makeSPDXDocumentInfoClient();
                if (isNullOrEmpty(document.getSpdxDocumentId())) {
                    document.setSpdxDocumentId(spdxDocumentId);
                }
                if (isNullOrEmpty(document.getId())) {
                    document.unsetId();
                    document.unsetRevision();
                    documentClient.addDocumentCreationInformation(document, user);
                } else {
                    documentClient.updateDocumentCreationInformation(document, user);
                }
            }
        }
        if (!isNullOrEmpty(packageInfoData)) {
            Set<PackageInformation> packageInfos = parsePackageInfosFromRequest(packageInfoData);
            SPDXDocumentService.Iface SPDXClient = new ThriftClients().makeSPDXClient();
            SPDXDocument spdxDocument = SPDXClient.getSPDXDocumentById(spdxDocumentId, user);
            idDeletePackageInfo(packageInfos, spdxDocument, user);
            if (packageInfos != null) {
                PackageInformationService.Iface packageClient = new ThriftClients().makeSPDXPackageInfoClient();
                for (PackageInformation packageInfo : packageInfos) {
                    if (isNullOrEmpty(packageInfo.getSpdxDocumentId())) {
                        packageInfo.setSpdxDocumentId(spdxDocumentId);
                    }
                    if (isNullOrEmpty(packageInfo.getId())) {
                        packageInfo.unsetId();
                        packageInfo.unsetRevision();
                        packageClient.addPackageInformation(packageInfo, user);
                    } else {
                        packageClient.updatePackageInformation(packageInfo, user);
                    }
                }
            }
        }
    }

    private static void idDeletePackageInfo(Set<PackageInformation> packageInfos, SPDXDocument spdxDocument, User user) {
        Set<String> spdxDocumentId = spdxDocument.getSpdxPackageInfoIds();
        Set<String> listIdPackageInfos = new HashSet<>();
        packageInfos.forEach(pInfo -> listIdPackageInfos.add(pInfo.getId()));
        for (String s : spdxDocumentId) {
            if (!listIdPackageInfos.contains(s)) {
                PackageInformationService.Iface packageClient = new ThriftClients().makeSPDXPackageInfoClient();
                try {
                    packageClient.deletePackageInformation(s, user);
                } catch (Exception e) {
                    log.error("Could not delete SDPX Package Info {}", e.getMessage());
                }
            }
        }
    }

    private static Set<SnippetInformation> parseSnippets(JSONObject json) {
        Set<SnippetInformation> snippets = new HashSet<>();
        JSONArray arraySnippets = json.getJSONArray("snippets");
        if (arraySnippets == null) {
            return snippets;
        }
        for (int i = 0; i < arraySnippets.length(); i++) {
            try {
                JSONObject objectSnippet = arraySnippets.getJSONObject(i);
                JSONArray arraySnippet = objectSnippet.getJSONArray("snippetRanges");
                objectSnippet.remove("snippetRanges");
                SnippetInformation snippet = mapper.readValue(objectSnippet.toJSONString(), SnippetInformation.class);
                Set<SnippetRange> snippetRanges = new HashSet<>();
                for (int j = 0; j < arraySnippet.length(); j++) {
                    snippetRanges.add(mapper.readValue(arraySnippet.getString(j), SnippetRange.class));
                }
                snippet.setSnippetRanges(snippetRanges);
                snippets.add(snippet);
            } catch (JsonProcessingException e) {
                log.error("Error when parse snippets",e);
            }
        }
        return snippets;
    }

    private static Set<RelationshipsBetweenSPDXElements> parseRelationships(JSONObject json) {
        Set<RelationshipsBetweenSPDXElements> relationships = new HashSet<>();
        JSONArray arrayRelationships = json.getJSONArray("relationships");
        if (arrayRelationships == null) {
            return relationships;
        }
        for (int i = 0; i < arrayRelationships.length(); i++) {
            try {
                relationships
                        .add(mapper.readValue(arrayRelationships.getString(i), RelationshipsBetweenSPDXElements.class));
            } catch (JsonProcessingException e) {
                log.error("Error when parse relationship",e);
            }
        }
        return relationships;
    }

    private static Set<Annotations> parseAnnotations(JSONObject json) {
        Set<Annotations> annotations = new HashSet<>();
        JSONArray arrayAnnotations = json.getJSONArray("annotations");
        if (arrayAnnotations == null) {
            return annotations;
        }
        for (int i = 0; i < arrayAnnotations.length(); i++) {
            try {
                annotations.add(mapper.readValue(arrayAnnotations.getString(i), Annotations.class));
            } catch (JsonProcessingException e) {
                log.error("Error when parse annotations",e);
            }
        }
        return annotations;
    }

    private static Set<OtherLicensingInformationDetected> parseLicensingInfo(JSONObject json) {
        Set<OtherLicensingInformationDetected> licensingInfo = new HashSet<>();
        JSONArray arrayLicensingInfo = json.getJSONArray("otherLicensingInformationDetecteds");
        if (arrayLicensingInfo == null) {
            return licensingInfo;
        }
        for (int i = 0; i < arrayLicensingInfo.length(); i++) {
            try {
                licensingInfo.add(
                        mapper.readValue(arrayLicensingInfo.getString(i), OtherLicensingInformationDetected.class));
            } catch (JsonProcessingException e) {
                log.error("Error when parse licensingInfo",e);
            }
        }
        return licensingInfo;
    }

    private static Set<ExternalDocumentReferences> parseExternalDocumentReferences(JSONObject json) {
        Set<ExternalDocumentReferences> externalDocumentRefs = new HashSet<>();
        JSONArray arrayExternalDocumentRefs = json.getJSONArray("externalDocumentRefs");
        if (arrayExternalDocumentRefs == null) {
            return externalDocumentRefs;
        }
        for (int i = 0; i < arrayExternalDocumentRefs.length(); i++) {
            try {
                JSONObject objectExternalDocumentRef = arrayExternalDocumentRefs.getJSONObject(i);
                JSONObject objectChecksum = objectExternalDocumentRef.getJSONObject("checksum");
                objectExternalDocumentRef.remove("checksum");
                ExternalDocumentReferences externalDocumentRef = mapper.readValue(objectExternalDocumentRef.toJSONString(), ExternalDocumentReferences.class);
                CheckSum checksum = mapper.readValue(objectChecksum.toJSONString(), CheckSum.class);
                externalDocumentRef.setChecksum(checksum);
                externalDocumentRefs.add(externalDocumentRef);
            } catch (JsonProcessingException e) {
                log.error("Error when parse ExternalDocumentReferences",e);
            }
        }
        return externalDocumentRefs;
    }

    private static Set<Creator> parseCreator(JSONObject json) {
        Set<Creator> creator = new HashSet<>();
        JSONArray arrayCreator = json.getJSONArray("creator");
        if (arrayCreator == null) {
            return creator;
        }
        for (int i = 0; i < arrayCreator.length(); i++) {
            try {
                creator.add(mapper.readValue(arrayCreator.getString(i), Creator.class));
            } catch (JsonProcessingException e) {
                log.error("Error when parse Creator",e);
            }
        }
        return creator;
    }

    private static PackageVerificationCode parsePackageVerificationCode(JSONObject json) {
        PackageVerificationCode packageVerificationCode = new PackageVerificationCode();
        JSONObject objectPackageVerificationCode = json.getJSONObject("packageVerificationCode");
        if (objectPackageVerificationCode == null) {
            return packageVerificationCode;
        }
        try {
            packageVerificationCode = mapper.readValue(objectPackageVerificationCode.toJSONString(),
                    PackageVerificationCode.class);
        } catch (JsonProcessingException e) {
            log.error("Error when parse PackageVerificationCode",e);
        }
        return packageVerificationCode;
    }

    private static Set<CheckSum> parseChecksum(JSONObject json) {
        Set<CheckSum> checkSums = new HashSet<>();
        JSONArray arrayCheckSums = json.getJSONArray("checksums");
        if (arrayCheckSums == null) {
            return checkSums;
        }
        for (int i = 0; i < arrayCheckSums.length(); i++) {
            try {
                checkSums.add(mapper.readValue(arrayCheckSums.getString(i), CheckSum.class));
            } catch (JsonProcessingException e) {
                log.error("Error when parse parse Checksum",e);
            }
        }
        return checkSums;
    }

    private static Set<ExternalReference> parseExternalReference(JSONObject json) {
        Set<ExternalReference> externalRefs = new HashSet<>();
        JSONArray arrayExternalRefs = json.getJSONArray("externalRefs");
        if (arrayExternalRefs == null) {
            return externalRefs;
        }
        for (int i = 0; i < arrayExternalRefs.length(); i++) {
            try {
                externalRefs.add(mapper.readValue(arrayExternalRefs.getString(i), ExternalReference.class));
            } catch (JsonProcessingException e) {
                log.error("Error when parse ExternalReference",e);
            }
        }
        return externalRefs;
    }

}
