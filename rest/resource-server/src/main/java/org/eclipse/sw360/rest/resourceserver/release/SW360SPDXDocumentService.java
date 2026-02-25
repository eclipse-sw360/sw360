/*
 * Copyright TOSHIBA CORPORATION, 2024. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.release;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.components.Release;
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
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

@Service
@Slf4j
@RequiredArgsConstructor
public class SW360SPDXDocumentService {
    @NonNull
    private final com.fasterxml.jackson.databind.Module sw360Module;
    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public SPDXDocument updateSPDXDocumentFromRequest(SPDXDocument spdxDocumentRequest,
                                                      SPDXDocument spdxDocumentActual,
                                                      Set<String> moderators) {
        if(CommonUtils.isNotEmpty(spdxDocumentRequest.getSnippets())) {
            if(!checkIndexSnippetInformations(spdxDocumentRequest.getSnippets())) {
                throw new BadRequestClientException("Index of SnippetInformations invalid!");
            }
            if(!checkIndexSnippetRanges(spdxDocumentRequest.getSnippets())) {
                throw new BadRequestClientException("Index of SnippetRanges invalid!");
            }
        }
        if(CommonUtils.isNotEmpty(spdxDocumentRequest.getRelationships()) && !checkIndexRelationships(spdxDocumentRequest.getRelationships())) {
            throw new BadRequestClientException("Index of Relationships SPDXDocument invalid!");
        }
        if(CommonUtils.isNotEmpty(spdxDocumentRequest.getAnnotations()) && !checkIndexAnnotations(spdxDocumentRequest.getAnnotations())) {
            throw new BadRequestClientException("Index of Annotations SPDXDocument invalid!");
        }
        if(CommonUtils.isNotEmpty(spdxDocumentRequest.getOtherLicensingInformationDetecteds())
                && !checkIndexOtherLicensingInformationDetected(spdxDocumentRequest.getOtherLicensingInformationDetecteds())) {
            throw new BadRequestClientException("Index of OtherLicensingInformationDetecteds invalid!");
        }

        return spdxDocumentRequest.setModerators(moderators)
                .setId(spdxDocumentActual.getId())
                .setSpdxDocumentCreationInfoId(spdxDocumentActual.getSpdxDocumentCreationInfoId())
                .setSpdxPackageInfoIds(spdxDocumentActual.getSpdxPackageInfoIds())
                .setRevision(spdxDocumentActual.getRevision());
    }

    public DocumentCreationInformation updateDocumentCreationInformationFromRequest(DocumentCreationInformation documentCreationInformation,
                                                                                    SPDXDocument spdxDocumentActual,
                                                                                    Set<String> moderators) {
        if(CommonUtils.isNotEmpty(documentCreationInformation.getExternalDocumentRefs()) &&
                !checkIndexExternalDocumentReferences(documentCreationInformation.getExternalDocumentRefs())) {
            throw new BadRequestClientException("Index of xternalDocumentReferences invalid!");
        }
        if(CommonUtils.isNotEmpty(documentCreationInformation.getCreator()) &&
                !checkIndexCreator(documentCreationInformation.getCreator())) {
            throw new BadRequestClientException("Index of Creators invalid!");
        }
        return documentCreationInformation.setModerators(moderators)
                .setId(spdxDocumentActual.getSpdxDocumentCreationInfoId());
    }

    public PackageInformation updatePackageInformationFromRequest(PackageInformation packageInformation,
                                                                  SPDXDocument spdxDocumentActual,
                                                                  Set<String> moderators) {
        if(packageInformation.getIndex() != 0) {
            throw new BadRequestClientException("Index of PackageInformation invalid!");
        }

        if(CommonUtils.isNotEmpty(packageInformation.getExternalRefs()) &&
                !checkIndexExternalReference(packageInformation.getExternalRefs())) {
            throw new BadRequestClientException("Index of ExternalReference invalid!");
        }

        if(CommonUtils.isNotEmpty(packageInformation.getAnnotations()) &&
                !checkIndexAnnotations(packageInformation.getAnnotations())) {
            throw new BadRequestClientException("Index of Annotations PackageInformation invalid!");
        }

        if(CommonUtils.isNotEmpty(packageInformation.getRelationships()) &&
                !checkIndexRelationships(packageInformation.getRelationships())) {
            throw new BadRequestClientException("Index of Relationships PackageInformation invalid!");
        }

        if(CommonUtils.isNotEmpty(packageInformation.getChecksums()) &&
                !checkIndexChecksums(packageInformation.getChecksums())) {
            throw new BadRequestClientException("Index of Checksums PackageInformation invalid!");
        }

        packageInformation.setModerators(moderators);
        if (!CommonUtils.isNullOrEmptyCollection(spdxDocumentActual.getSpdxPackageInfoIds())) {
            packageInformation.setId(spdxDocumentActual.getSpdxPackageInfoIds().stream().findFirst().get());
        }
        return packageInformation;
    }

    public String addSPDXDocument(Release release, User user) throws TException {
        SPDXDocumentService.Iface spdxClient = new ThriftClients().makeSPDXClient();
        String spdxId = "";
        SPDXDocument spdxDocumentGenerate = SW360Utils.generateSpdxDocument();
        spdxDocumentGenerate.setModerators(release.getModerators());
        spdxDocumentGenerate.setReleaseId(release.getId());
        if (CommonUtils.isNullEmptyOrWhitespace(spdxDocumentGenerate.getId())) {
            spdxId = spdxClient.addSPDXDocument(spdxDocumentGenerate, user).getId();
        }
        return spdxId;
    }

    public void addDocumentCreationInformation(String spdxId, Set<String> moderators, User user) throws TException {
        DocumentCreationInformationService.Iface documentClient = new ThriftClients().makeSPDXDocumentInfoClient();
        DocumentCreationInformation documentCreationInformation = SW360Utils.generateDocumentCreationInformation();
        documentCreationInformation.setModerators(moderators);
        if (isNullOrEmpty(documentCreationInformation.getSpdxDocumentId())) {
            documentCreationInformation.setSpdxDocumentId(spdxId);
        }
        if (isNullOrEmpty(documentCreationInformation.getId())) {
            documentClient.addDocumentCreationInformation(documentCreationInformation, user);
        }
    }

    public void addPackageInformation(String spdxId, Set<String> moderators, User user) throws TException {
        PackageInformationService.Iface packageClient = new ThriftClients().makeSPDXPackageInfoClient();
        PackageInformation packageInformation = SW360Utils.generatePackageInformation();
        packageInformation.setModerators(moderators);
        if (isNullOrEmpty(packageInformation.getSpdxDocumentId())) {
            packageInformation.setSpdxDocumentId(spdxId);
        }
        if (isNullOrEmpty(packageInformation.getId())) {
            packageClient.addPackageInformation(packageInformation, user);
        }
    }

    public String addSPDX(Release release, User user) throws TException {
        String spdxId = addSPDXDocument(release, user);
        if (CommonUtils.isNullEmptyOrWhitespace(spdxId)) {
            throw new BadRequestClientException("Add SPDXDocument Failed!");
        }
        addDocumentCreationInformation(spdxId, release.getModerators(), user);
        addPackageInformation(spdxId, release.getModerators(), user);
        return spdxId;
    }

    private boolean checkIndexCreator(Set<Creator> creators) {
        List<Integer> indexOfCreators = creators.stream().map(Creator::getIndex).collect(Collectors.toList());
        if(creators.size() != indexOfCreators.size()) {
            return false;
        }
        return checkDuplicateIndex(indexOfCreators);
    }

    private boolean checkIndexExternalDocumentReferences(Set<ExternalDocumentReferences> externalDocumentReferences) {
        List<Integer> indexOfExternalDocumentReferences = externalDocumentReferences.stream().map(ExternalDocumentReferences::getIndex).collect(Collectors.toList());
        if(externalDocumentReferences.size() != indexOfExternalDocumentReferences.size()) {
            return false;
        }
        return checkDuplicateIndex(indexOfExternalDocumentReferences);
    }

    private boolean checkIndexExternalReference(Set<ExternalReference> externalReferences) {
        List<Integer> indexOfExternalReference = externalReferences.stream().map(ExternalReference::getIndex).collect(Collectors.toList());
        if(externalReferences.size() != indexOfExternalReference.size()) {
            return false;
        }
        return checkDuplicateIndex(indexOfExternalReference);
    }

    private boolean checkIndexOtherLicensingInformationDetected(Set<OtherLicensingInformationDetected> otherLicensingInformationDetecteds) {
        List<Integer> indexOfOtherLicensingInformationDetecteds = otherLicensingInformationDetecteds.stream().map(OtherLicensingInformationDetected::getIndex).collect(Collectors.toList());
        if(otherLicensingInformationDetecteds.size() != indexOfOtherLicensingInformationDetecteds.size()) {
            return false;
        }
        return checkDuplicateIndex(indexOfOtherLicensingInformationDetecteds);
    }

    private boolean checkIndexSnippetInformations(Set<SnippetInformation> snippetInformations) {
        List<Integer> indexOfSnippetInformations = snippetInformations.stream().map(SnippetInformation::getIndex).collect(Collectors.toList());
        if(snippetInformations.size() != indexOfSnippetInformations.size()) {
            return false;
        }
        return checkDuplicateIndex(indexOfSnippetInformations);
    }

    public boolean checkIndexSnippetRanges(Set<SnippetInformation> snippetInformations) {
        if(CommonUtils.isNullOrEmptyCollection(snippetInformations)) {
            return true;
        }
        Set<SnippetRange> snippetRanges = new HashSet<>();
        for (SnippetInformation snippetInformation: snippetInformations) {
            if(!CommonUtils.isNullOrEmptyCollection(snippetInformation.getSnippetRanges())) {
                if(!checkDuplicateIndex(snippetInformation.getSnippetRanges().stream().map(SnippetRange::getIndex).collect(Collectors.toList()))) {
                    return false;
                }
                snippetRanges.addAll(snippetInformation.getSnippetRanges());
            }
        }
        List<Integer> indexOfSnippetRanges = snippetRanges.stream().map(SnippetRange::getIndex).collect(Collectors.toList());
        if(snippetRanges.size() != indexOfSnippetRanges.size()) {
            return false;
        }
        return true;
    }

    private boolean checkIndexRelationships(Set<RelationshipsBetweenSPDXElements> relationships) {
        List<Integer> indexOfRelationShips = relationships.stream().map(RelationshipsBetweenSPDXElements::getIndex).collect(Collectors.toList());
        if(relationships.size() != indexOfRelationShips.size()) {
            return false;
        }
        return checkDuplicateIndex(indexOfRelationShips);
    }

    private boolean checkIndexAnnotations(Set<Annotations> annotations) {
        List<Integer> indexOfAnnotaions = annotations.stream().map(Annotations::getIndex).collect(Collectors.toList());
        if(annotations.size() != indexOfAnnotaions.size()) {
            return false;
        }
        return checkDuplicateIndex(indexOfAnnotaions);
    }

    private boolean checkIndexChecksums(Set<CheckSum> checkSums) {
        List<Integer> indexOfCheckSums = checkSums.stream().map(CheckSum::getIndex).collect(Collectors.toList());
        if(checkSums.size() != indexOfCheckSums.size()) {
            return false;
        }
        return checkDuplicateIndex(indexOfCheckSums);
    }

    private boolean checkDuplicateIndex(List<Integer> indexes) {
        int sizeIndexes = indexes.size();
        if(sizeIndexes == 1 && indexes.get(0) == 0) {
            return true;
        }
        int sumIndex = indexes.stream().mapToInt(Integer::intValue).sum();
        int sumActualIndex = ((sizeIndexes -1) * ((sizeIndexes -1) + 1))/2;
        return sumIndex == sumActualIndex;
    }

    public void sortSectionForSPDXDocument(SPDXDocument spdxDocument) {
        Set<SnippetInformation> snippetInformations = spdxDocument.getSnippets().stream()
                .sorted(Comparator.comparing(SnippetInformation::getIndex)) // sort while streaming
                .collect(Collectors.toCollection(LinkedHashSet::new));
        spdxDocument.setSnippets(snippetInformations);

        for (SnippetInformation snippetInformation: spdxDocument.getSnippets()) {
            Set<SnippetRange> snippetRanges = snippetInformation.getSnippetRanges().stream()
                    .sorted(Comparator.comparing(SnippetRange::getIndex)) // sort while streaming
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            snippetInformation.setSnippetRanges(snippetRanges);
        }

        Set<OtherLicensingInformationDetected> otherLicensingInformationDetecteds = spdxDocument.getOtherLicensingInformationDetecteds().stream()
                .sorted(Comparator.comparing(OtherLicensingInformationDetected::getIndex)) // sort while streaming
                .collect(Collectors.toCollection(LinkedHashSet::new));
        spdxDocument.setOtherLicensingInformationDetecteds(otherLicensingInformationDetecteds);

        Set<RelationshipsBetweenSPDXElements> relationshipsBetweenSPDXElements = spdxDocument.getRelationships().stream()
                .sorted(Comparator.comparing(RelationshipsBetweenSPDXElements::getIndex)) // sort while streaming
                .collect(Collectors.toCollection(LinkedHashSet::new));
        spdxDocument.setRelationships(relationshipsBetweenSPDXElements);

        Set<Annotations> annotations = spdxDocument.getAnnotations().stream()
                .sorted(Comparator.comparing(Annotations::getIndex)) // sort while streaming
                .collect(Collectors.toCollection(LinkedHashSet::new));
        spdxDocument.setAnnotations(annotations);
    }

    public void sortSectionForPackageInformation(PackageInformation packageInformation) {
        Set<CheckSum> checkSums = packageInformation.getChecksums().stream()
                .sorted(Comparator.comparing(CheckSum::getIndex)) // sort while streaming
                .collect(Collectors.toCollection(LinkedHashSet::new));
        packageInformation.setChecksums(checkSums);

        Set<ExternalReference> externalReferences = packageInformation.getExternalRefs().stream()
                .sorted(Comparator.comparing(ExternalReference::getIndex)) // sort while streaming
                .collect(Collectors.toCollection(LinkedHashSet::new));
        packageInformation.setExternalRefs(externalReferences);

        Set<RelationshipsBetweenSPDXElements> relationshipsBetweenSPDXElements = packageInformation.getRelationships().stream()
                .sorted(Comparator.comparing(RelationshipsBetweenSPDXElements::getIndex)) // sort while streaming
                .collect(Collectors.toCollection(LinkedHashSet::new));
        packageInformation.setRelationships(relationshipsBetweenSPDXElements);

        Set<Annotations> annotations = packageInformation.getAnnotations().stream()
                .sorted(Comparator.comparing(Annotations::getIndex)) // sort while streaming
                .collect(Collectors.toCollection(LinkedHashSet::new));
        packageInformation.setAnnotations(annotations);
    }

    public void sortSectionForDocumentCreation(DocumentCreationInformation documentCreationInformation) {
        Set<ExternalDocumentReferences> externalDocumentReferences = documentCreationInformation.getExternalDocumentRefs().stream()
                .sorted(Comparator.comparing(ExternalDocumentReferences::getIndex)) // sort while streaming
                .collect(Collectors.toCollection(LinkedHashSet::new));
        documentCreationInformation.setExternalDocumentRefs(externalDocumentReferences);
    }


    public SPDXDocument convertToSPDXDocument(Object object) {
        mapper.registerModule(sw360Module);
        return mapper.convertValue(object, SPDXDocument.class);
    }

    public DocumentCreationInformation convertToDocumentCreationInformation(Object object) {
        mapper.registerModule(sw360Module);
        return mapper.convertValue(object, DocumentCreationInformation.class);
    }

    public PackageInformation convertToPackageInformation(Object object) {
        mapper.registerModule(sw360Module);
        return mapper.convertValue(object, PackageInformation.class);
    }
}
