/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.components;

import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class ReleaseConverter {

    private ReleaseConverter() {}

    public static Release fromThrift(org.eclipse.sw360.datahandler.thrift.components.Release thrift) {
        if (thrift == null) {
            return null;
        }
        Release pojo = new Release();
        if (thrift.isSetAdditionalData()) {
            pojo.setAdditionalData(thrift.getAdditionalData());
        }
        if (thrift.isSetAttachments()) {
            pojo.setAttachments(ThriftCollectionConverter.mapSet(thrift.getAttachments(), e -> org.eclipse.sw360.common.utils.converter.attachments.AttachmentConverter.fromThrift(e)));
        }
        if (thrift.isSetBinaryDownloadurl()) {
            pojo.setBinaryDownloadurl(thrift.getBinaryDownloadurl());
        }
        if (thrift.isSetClearingInformation()) {
            pojo.setClearingInformation(org.eclipse.sw360.common.utils.converter.components.ClearingInformationConverter.fromThrift(thrift.getClearingInformation()));
        }
        if (thrift.isSetClearingState()) {
            pojo.setClearingState(EnumConverter.fromThrift(thrift.getClearingState(), org.eclipse.sw360.datahandler.services.components.ClearingState.class));
        }
        if (thrift.isSetComponentId()) {
            pojo.setComponentId(thrift.getComponentId());
        }
        if (thrift.isSetComponentType()) {
            pojo.setComponentType(EnumConverter.fromThrift(thrift.getComponentType(), org.eclipse.sw360.datahandler.services.components.ComponentType.class));
        }
        if (thrift.isSetContributors()) {
            pojo.setContributors(ThriftCollectionConverter.mapSet(thrift.getContributors(), e -> e));
        }
        if (thrift.isSetCotsDetails()) {
            pojo.setCotsDetails(org.eclipse.sw360.common.utils.converter.components.COTSDetailsConverter.fromThrift(thrift.getCotsDetails()));
        }
        if (thrift.isSetCpeid()) {
            pojo.setCpeid(thrift.getCpeid());
        }
        if (thrift.isSetCreatedBy()) {
            pojo.setCreatedBy(thrift.getCreatedBy());
        }
        if (thrift.isSetCreatedOn()) {
            pojo.setCreatedOn(thrift.getCreatedOn());
        }
        if (thrift.isSetCreatorDepartment()) {
            pojo.setCreatorDepartment(thrift.getCreatorDepartment());
        }
        if (thrift.isSetDocumentState()) {
            pojo.setDocumentState(org.eclipse.sw360.common.utils.converter.common.DocumentStateConverter.fromThrift(thrift.getDocumentState()));
        }
        if (thrift.isSetEccInformation()) {
            pojo.setEccInformation(org.eclipse.sw360.common.utils.converter.components.EccInformationConverter.fromThrift(thrift.getEccInformation()));
        }
        if (thrift.isSetExternalIds()) {
            pojo.setExternalIds(thrift.getExternalIds());
        }
        if (thrift.isSetExternalToolProcesses()) {
            pojo.setExternalToolProcesses(ThriftCollectionConverter.mapSet(thrift.getExternalToolProcesses(), e -> org.eclipse.sw360.common.utils.converter.components.ExternalToolProcessConverter.fromThrift(e)));
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetLanguages()) {
            pojo.setLanguages(ThriftCollectionConverter.mapSet(thrift.getLanguages(), e -> e));
        }
        if (thrift.isSetMainLicenseIds()) {
            pojo.setMainLicenseIds(ThriftCollectionConverter.mapSet(thrift.getMainLicenseIds(), e -> e));
        }
        if (thrift.isSetMainlineState()) {
            pojo.setMainlineState(EnumConverter.fromThrift(thrift.getMainlineState(), org.eclipse.sw360.datahandler.services.common.MainlineState.class));
        }
        if (thrift.isSetModerators()) {
            pojo.setModerators(ThriftCollectionConverter.mapSet(thrift.getModerators(), e -> e));
        }
        if (thrift.isSetModifiedBy()) {
            pojo.setModifiedBy(thrift.getModifiedBy());
        }
        if (thrift.isSetModifiedOn()) {
            pojo.setModifiedOn(thrift.getModifiedOn());
        }
        if (thrift.isSetName()) {
            pojo.setName(thrift.getName());
        }
        if (thrift.isSetOperatingSystems()) {
            pojo.setOperatingSystems(ThriftCollectionConverter.mapSet(thrift.getOperatingSystems(), e -> e));
        }
        if (thrift.isSetOtherLicenseIds()) {
            pojo.setOtherLicenseIds(ThriftCollectionConverter.mapSet(thrift.getOtherLicenseIds(), e -> e));
        }
        if (thrift.isSetPackageIds()) {
            pojo.setPackageIds(ThriftCollectionConverter.mapSet(thrift.getPackageIds(), e -> e));
        }
        if (thrift.isSetPermissions()) {
            pojo.setPermissions(ThriftCollectionConverter.mapMap(thrift.getPermissions(), mapKey -> EnumConverter.fromThrift(mapKey, org.eclipse.sw360.datahandler.services.users.RequestedAction.class), mapValue -> mapValue));
        }
        if (thrift.isSetProjectMainlineState()) {
            pojo.setProjectMainlineState(EnumConverter.fromThrift(thrift.getProjectMainlineState(), org.eclipse.sw360.datahandler.services.common.MainlineState.class));
        }
        if (thrift.isSetReleaseDate()) {
            pojo.setReleaseDate(thrift.getReleaseDate());
        }
        if (thrift.isSetReleaseIdToRelationship()) {
            pojo.setReleaseIdToRelationship(ThriftCollectionConverter.mapMap(thrift.getReleaseIdToRelationship(), mapKey -> mapKey, mapValue -> EnumConverter.fromThrift(mapValue, org.eclipse.sw360.datahandler.services.common.ReleaseRelationship.class)));
        }
        if (thrift.isSetRepository()) {
            pojo.setRepository(org.eclipse.sw360.common.utils.converter.components.RepositoryConverter.fromThrift(thrift.getRepository()));
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetRoles()) {
            pojo.setRoles(ThriftCollectionConverter.mapMap(thrift.getRoles(), mapKey -> mapKey, mapValue -> ThriftCollectionConverter.mapSet(mapValue, e -> e)));
        }
        if (thrift.isSetSoftwarePlatforms()) {
            pojo.setSoftwarePlatforms(ThriftCollectionConverter.mapSet(thrift.getSoftwarePlatforms(), e -> e));
        }
        if (thrift.isSetSourceCodeDownloadurl()) {
            pojo.setSourceCodeDownloadurl(thrift.getSourceCodeDownloadurl());
        }
        if (thrift.isSetSpdxId()) {
            pojo.setSpdxId(thrift.getSpdxId());
        }
        if (thrift.isSetSubscribers()) {
            pojo.setSubscribers(ThriftCollectionConverter.mapSet(thrift.getSubscribers(), e -> e));
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        if (thrift.isSetVendor()) {
            pojo.setVendor(org.eclipse.sw360.common.utils.converter.vendors.VendorConverter.fromThrift(thrift.getVendor()));
        }
        if (thrift.isSetVendorId()) {
            pojo.setVendorId(thrift.getVendorId());
        }
        if (thrift.isSetVersion()) {
            pojo.setVersion(thrift.getVersion());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.components.Release toThrift(Release pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.components.Release thrift = new org.eclipse.sw360.datahandler.thrift.components.Release();
        if (pojo.getAdditionalData() != null) {
            thrift.setAdditionalData(pojo.getAdditionalData());
        }
        if (pojo.getAttachments() != null) {
            thrift.setAttachments(ThriftCollectionConverter.mapSet(pojo.getAttachments(), e -> org.eclipse.sw360.common.utils.converter.attachments.AttachmentConverter.toThrift(e)));
        }
        if (pojo.getBinaryDownloadurl() != null) {
            thrift.setBinaryDownloadurl(pojo.getBinaryDownloadurl());
        }
        if (pojo.getClearingInformation() != null) {
            thrift.setClearingInformation(org.eclipse.sw360.common.utils.converter.components.ClearingInformationConverter.toThrift(pojo.getClearingInformation()));
        }
        if (pojo.getClearingState() != null) {
            thrift.setClearingState(EnumConverter.toThrift(pojo.getClearingState(), org.eclipse.sw360.datahandler.thrift.components.ClearingState.class));
        }
        if (pojo.getComponentId() != null) {
            thrift.setComponentId(pojo.getComponentId());
        }
        if (pojo.getComponentType() != null) {
            thrift.setComponentType(EnumConverter.toThrift(pojo.getComponentType(), org.eclipse.sw360.datahandler.thrift.components.ComponentType.class));
        }
        if (pojo.getContributors() != null) {
            thrift.setContributors(ThriftCollectionConverter.mapSet(pojo.getContributors(), e -> e));
        }
        if (pojo.getCotsDetails() != null) {
            thrift.setCotsDetails(org.eclipse.sw360.common.utils.converter.components.COTSDetailsConverter.toThrift(pojo.getCotsDetails()));
        }
        if (pojo.getCpeid() != null) {
            thrift.setCpeid(pojo.getCpeid());
        }
        if (pojo.getCreatedBy() != null) {
            thrift.setCreatedBy(pojo.getCreatedBy());
        }
        if (pojo.getCreatedOn() != null) {
            thrift.setCreatedOn(pojo.getCreatedOn());
        }
        if (pojo.getCreatorDepartment() != null) {
            thrift.setCreatorDepartment(pojo.getCreatorDepartment());
        }
        if (pojo.getDocumentState() != null) {
            thrift.setDocumentState(org.eclipse.sw360.common.utils.converter.common.DocumentStateConverter.toThrift(pojo.getDocumentState()));
        }
        if (pojo.getEccInformation() != null) {
            thrift.setEccInformation(org.eclipse.sw360.common.utils.converter.components.EccInformationConverter.toThrift(pojo.getEccInformation()));
        }
        if (pojo.getExternalIds() != null) {
            thrift.setExternalIds(pojo.getExternalIds());
        }
        if (pojo.getExternalToolProcesses() != null) {
            thrift.setExternalToolProcesses(ThriftCollectionConverter.mapSet(pojo.getExternalToolProcesses(), e -> org.eclipse.sw360.common.utils.converter.components.ExternalToolProcessConverter.toThrift(e)));
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getLanguages() != null) {
            thrift.setLanguages(ThriftCollectionConverter.mapSet(pojo.getLanguages(), e -> e));
        }
        if (pojo.getMainLicenseIds() != null) {
            thrift.setMainLicenseIds(ThriftCollectionConverter.mapSet(pojo.getMainLicenseIds(), e -> e));
        }
        if (pojo.getMainlineState() != null) {
            thrift.setMainlineState(EnumConverter.toThrift(pojo.getMainlineState(), org.eclipse.sw360.datahandler.thrift.MainlineState.class));
        }
        if (pojo.getModerators() != null) {
            thrift.setModerators(ThriftCollectionConverter.mapSet(pojo.getModerators(), e -> e));
        }
        if (pojo.getModifiedBy() != null) {
            thrift.setModifiedBy(pojo.getModifiedBy());
        }
        if (pojo.getModifiedOn() != null) {
            thrift.setModifiedOn(pojo.getModifiedOn());
        }
        if (pojo.getName() != null) {
            thrift.setName(pojo.getName());
        }
        if (pojo.getOperatingSystems() != null) {
            thrift.setOperatingSystems(ThriftCollectionConverter.mapSet(pojo.getOperatingSystems(), e -> e));
        }
        if (pojo.getOtherLicenseIds() != null) {
            thrift.setOtherLicenseIds(ThriftCollectionConverter.mapSet(pojo.getOtherLicenseIds(), e -> e));
        }
        if (pojo.getPackageIds() != null) {
            thrift.setPackageIds(ThriftCollectionConverter.mapSet(pojo.getPackageIds(), e -> e));
        }
        if (pojo.getPermissions() != null) {
            thrift.setPermissions(ThriftCollectionConverter.mapMap(pojo.getPermissions(), mapKey -> EnumConverter.toThrift(mapKey, org.eclipse.sw360.datahandler.thrift.users.RequestedAction.class), mapValue -> mapValue));
        }
        if (pojo.getProjectMainlineState() != null) {
            thrift.setProjectMainlineState(EnumConverter.toThrift(pojo.getProjectMainlineState(), org.eclipse.sw360.datahandler.thrift.MainlineState.class));
        }
        if (pojo.getReleaseDate() != null) {
            thrift.setReleaseDate(pojo.getReleaseDate());
        }
        if (pojo.getReleaseIdToRelationship() != null) {
            thrift.setReleaseIdToRelationship(ThriftCollectionConverter.mapMap(pojo.getReleaseIdToRelationship(), mapKey -> mapKey, mapValue -> EnumConverter.toThrift(mapValue, org.eclipse.sw360.datahandler.thrift.ReleaseRelationship.class)));
        }
        if (pojo.getRepository() != null) {
            thrift.setRepository(org.eclipse.sw360.common.utils.converter.components.RepositoryConverter.toThrift(pojo.getRepository()));
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getRoles() != null) {
            thrift.setRoles(ThriftCollectionConverter.mapMap(pojo.getRoles(), mapKey -> mapKey, mapValue -> ThriftCollectionConverter.mapSet(mapValue, e -> e)));
        }
        if (pojo.getSoftwarePlatforms() != null) {
            thrift.setSoftwarePlatforms(ThriftCollectionConverter.mapSet(pojo.getSoftwarePlatforms(), e -> e));
        }
        if (pojo.getSourceCodeDownloadurl() != null) {
            thrift.setSourceCodeDownloadurl(pojo.getSourceCodeDownloadurl());
        }
        if (pojo.getSpdxId() != null) {
            thrift.setSpdxId(pojo.getSpdxId());
        }
        if (pojo.getSubscribers() != null) {
            thrift.setSubscribers(ThriftCollectionConverter.mapSet(pojo.getSubscribers(), e -> e));
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        if (pojo.getVendor() != null) {
            thrift.setVendor(org.eclipse.sw360.common.utils.converter.vendors.VendorConverter.toThrift(pojo.getVendor()));
        }
        if (pojo.getVendorId() != null) {
            thrift.setVendorId(pojo.getVendorId());
        }
        if (pojo.getVersion() != null) {
            thrift.setVersion(pojo.getVersion());
        }
        return thrift;
    }
}
