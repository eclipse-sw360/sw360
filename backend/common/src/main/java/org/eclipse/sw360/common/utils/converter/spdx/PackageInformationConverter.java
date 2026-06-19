/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.spdx;

import org.eclipse.sw360.datahandler.services.spdx.PackageInformation;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class PackageInformationConverter {

    private PackageInformationConverter() {}

    public static PackageInformation fromThrift(org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation thrift) {
        if (thrift == null) {
            return null;
        }
        PackageInformation pojo = new PackageInformation();
        if (thrift.isSetAnnotations()) {
            pojo.setAnnotations(ThriftCollectionConverter.mapSet(thrift.getAnnotations(), e -> org.eclipse.sw360.common.utils.converter.spdx.AnnotationsConverter.fromThrift(e)));
        }
        if (thrift.isSetAttributionText()) {
            pojo.setAttributionText(ThriftCollectionConverter.mapSet(thrift.getAttributionText(), e -> e));
        }
        if (thrift.isSetBuiltDate()) {
            pojo.setBuiltDate(thrift.getBuiltDate());
        }
        if (thrift.isSetChecksums()) {
            pojo.setChecksums(ThriftCollectionConverter.mapSet(thrift.getChecksums(), e -> org.eclipse.sw360.common.utils.converter.spdx.CheckSumConverter.fromThrift(e)));
        }
        if (thrift.isSetCopyrightText()) {
            pojo.setCopyrightText(thrift.getCopyrightText());
        }
        if (thrift.isSetCreatedBy()) {
            pojo.setCreatedBy(thrift.getCreatedBy());
        }
        if (thrift.isSetDescription()) {
            pojo.setDescription(thrift.getDescription());
        }
        if (thrift.isSetDocumentState()) {
            pojo.setDocumentState(org.eclipse.sw360.common.utils.converter.common.DocumentStateConverter.fromThrift(thrift.getDocumentState()));
        }
        if (thrift.isSetDownloadLocation()) {
            pojo.setDownloadLocation(thrift.getDownloadLocation());
        }
        if (thrift.isSetExternalRefs()) {
            pojo.setExternalRefs(ThriftCollectionConverter.mapSet(thrift.getExternalRefs(), e -> org.eclipse.sw360.common.utils.converter.spdx.ExternalReferenceConverter.fromThrift(e)));
        }
        if (thrift.isSetFilesAnalyzed()) {
            pojo.setFilesAnalyzed(thrift.isFilesAnalyzed());
        }
        if (thrift.isSetHomepage()) {
            pojo.setHomepage(thrift.getHomepage());
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetIndex()) {
            pojo.setIndex(thrift.getIndex());
        }
        if (thrift.isSetLicenseComments()) {
            pojo.setLicenseComments(thrift.getLicenseComments());
        }
        if (thrift.isSetLicenseConcluded()) {
            pojo.setLicenseConcluded(thrift.getLicenseConcluded());
        }
        if (thrift.isSetLicenseDeclared()) {
            pojo.setLicenseDeclared(thrift.getLicenseDeclared());
        }
        if (thrift.isSetLicenseInfoFromFiles()) {
            pojo.setLicenseInfoFromFiles(ThriftCollectionConverter.mapSet(thrift.getLicenseInfoFromFiles(), e -> e));
        }
        if (thrift.isSetModerators()) {
            pojo.setModerators(ThriftCollectionConverter.mapSet(thrift.getModerators(), e -> e));
        }
        if (thrift.isSetName()) {
            pojo.setName(thrift.getName());
        }
        if (thrift.isSetOriginator()) {
            pojo.setOriginator(thrift.getOriginator());
        }
        if (thrift.isSetPackageComment()) {
            pojo.setPackageComment(thrift.getPackageComment());
        }
        if (thrift.isSetPackageFileName()) {
            pojo.setPackageFileName(thrift.getPackageFileName());
        }
        if (thrift.isSetPackageVerificationCode()) {
            pojo.setPackageVerificationCode(org.eclipse.sw360.common.utils.converter.spdx.PackageVerificationCodeConverter.fromThrift(thrift.getPackageVerificationCode()));
        }
        if (thrift.isSetPermissions()) {
            pojo.setPermissions(ThriftCollectionConverter.mapMap(thrift.getPermissions(), mapKey -> EnumConverter.fromThrift(mapKey, org.eclipse.sw360.datahandler.services.users.RequestedAction.class), mapValue -> mapValue));
        }
        if (thrift.isSetPrimaryPackagePurpose()) {
            pojo.setPrimaryPackagePurpose(thrift.getPrimaryPackagePurpose());
        }
        if (thrift.isSetRelationships()) {
            pojo.setRelationships(ThriftCollectionConverter.mapSet(thrift.getRelationships(), e -> org.eclipse.sw360.common.utils.converter.spdx.RelationshipsBetweenSPDXElementsConverter.fromThrift(e)));
        }
        if (thrift.isSetReleaseDate()) {
            pojo.setReleaseDate(thrift.getReleaseDate());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetSourceInfo()) {
            pojo.setSourceInfo(thrift.getSourceInfo());
        }
        if (thrift.isSetSpdxDocumentId()) {
            pojo.setSpdxDocumentId(thrift.getSpdxDocumentId());
        }
        if (thrift.isSetSummary()) {
            pojo.setSummary(thrift.getSummary());
        }
        if (thrift.isSetSupplier()) {
            pojo.setSupplier(thrift.getSupplier());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        if (thrift.isSetValidUntilDate()) {
            pojo.setValidUntilDate(thrift.getValidUntilDate());
        }
        if (thrift.isSetVersionInfo()) {
            pojo.setVersionInfo(thrift.getVersionInfo());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation toThrift(PackageInformation pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation thrift = new org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation();
        if (pojo.getAnnotations() != null) {
            thrift.setAnnotations(ThriftCollectionConverter.mapSet(pojo.getAnnotations(), e -> org.eclipse.sw360.common.utils.converter.spdx.AnnotationsConverter.toThrift(e)));
        }
        if (pojo.getAttributionText() != null) {
            thrift.setAttributionText(ThriftCollectionConverter.mapSet(pojo.getAttributionText(), e -> e));
        }
        if (pojo.getBuiltDate() != null) {
            thrift.setBuiltDate(pojo.getBuiltDate());
        }
        if (pojo.getChecksums() != null) {
            thrift.setChecksums(ThriftCollectionConverter.mapSet(pojo.getChecksums(), e -> org.eclipse.sw360.common.utils.converter.spdx.CheckSumConverter.toThrift(e)));
        }
        if (pojo.getCopyrightText() != null) {
            thrift.setCopyrightText(pojo.getCopyrightText());
        }
        if (pojo.getCreatedBy() != null) {
            thrift.setCreatedBy(pojo.getCreatedBy());
        }
        if (pojo.getDescription() != null) {
            thrift.setDescription(pojo.getDescription());
        }
        if (pojo.getDocumentState() != null) {
            thrift.setDocumentState(org.eclipse.sw360.common.utils.converter.common.DocumentStateConverter.toThrift(pojo.getDocumentState()));
        }
        if (pojo.getDownloadLocation() != null) {
            thrift.setDownloadLocation(pojo.getDownloadLocation());
        }
        if (pojo.getExternalRefs() != null) {
            thrift.setExternalRefs(ThriftCollectionConverter.mapSet(pojo.getExternalRefs(), e -> org.eclipse.sw360.common.utils.converter.spdx.ExternalReferenceConverter.toThrift(e)));
        }
        if (pojo.getFilesAnalyzed() != null) {
            thrift.setFilesAnalyzed(pojo.getFilesAnalyzed());
        }
        if (pojo.getHomepage() != null) {
            thrift.setHomepage(pojo.getHomepage());
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getIndex() != null) {
            thrift.setIndex(pojo.getIndex());
        }
        if (pojo.getLicenseComments() != null) {
            thrift.setLicenseComments(pojo.getLicenseComments());
        }
        if (pojo.getLicenseConcluded() != null) {
            thrift.setLicenseConcluded(pojo.getLicenseConcluded());
        }
        if (pojo.getLicenseDeclared() != null) {
            thrift.setLicenseDeclared(pojo.getLicenseDeclared());
        }
        if (pojo.getLicenseInfoFromFiles() != null) {
            thrift.setLicenseInfoFromFiles(ThriftCollectionConverter.mapSet(pojo.getLicenseInfoFromFiles(), e -> e));
        }
        if (pojo.getModerators() != null) {
            thrift.setModerators(ThriftCollectionConverter.mapSet(pojo.getModerators(), e -> e));
        }
        if (pojo.getName() != null) {
            thrift.setName(pojo.getName());
        }
        if (pojo.getOriginator() != null) {
            thrift.setOriginator(pojo.getOriginator());
        }
        if (pojo.getPackageComment() != null) {
            thrift.setPackageComment(pojo.getPackageComment());
        }
        if (pojo.getPackageFileName() != null) {
            thrift.setPackageFileName(pojo.getPackageFileName());
        }
        if (pojo.getPackageVerificationCode() != null) {
            thrift.setPackageVerificationCode(org.eclipse.sw360.common.utils.converter.spdx.PackageVerificationCodeConverter.toThrift(pojo.getPackageVerificationCode()));
        }
        if (pojo.getPermissions() != null) {
            thrift.setPermissions(ThriftCollectionConverter.mapMap(pojo.getPermissions(), mapKey -> EnumConverter.toThrift(mapKey, org.eclipse.sw360.datahandler.thrift.users.RequestedAction.class), mapValue -> mapValue));
        }
        if (pojo.getPrimaryPackagePurpose() != null) {
            thrift.setPrimaryPackagePurpose(pojo.getPrimaryPackagePurpose());
        }
        if (pojo.getRelationships() != null) {
            thrift.setRelationships(ThriftCollectionConverter.mapSet(pojo.getRelationships(), e -> org.eclipse.sw360.common.utils.converter.spdx.RelationshipsBetweenSPDXElementsConverter.toThrift(e)));
        }
        if (pojo.getReleaseDate() != null) {
            thrift.setReleaseDate(pojo.getReleaseDate());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getSourceInfo() != null) {
            thrift.setSourceInfo(pojo.getSourceInfo());
        }
        if (pojo.getSpdxDocumentId() != null) {
            thrift.setSpdxDocumentId(pojo.getSpdxDocumentId());
        }
        if (pojo.getSummary() != null) {
            thrift.setSummary(pojo.getSummary());
        }
        if (pojo.getSupplier() != null) {
            thrift.setSupplier(pojo.getSupplier());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        if (pojo.getValidUntilDate() != null) {
            thrift.setValidUntilDate(pojo.getValidUntilDate());
        }
        if (pojo.getVersionInfo() != null) {
            thrift.setVersionInfo(pojo.getVersionInfo());
        }
        return thrift;
    }
}
