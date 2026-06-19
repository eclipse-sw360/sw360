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

import org.eclipse.sw360.datahandler.services.components.Component;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class ComponentConverter {

    private ComponentConverter() {}

    public static Component fromThrift(org.eclipse.sw360.datahandler.thrift.components.Component thrift) {
        if (thrift == null) {
            return null;
        }
        Component pojo = new Component();
        if (thrift.isSetAdditionalData()) {
            pojo.setAdditionalData(thrift.getAdditionalData());
        }
        if (thrift.isSetAttachments()) {
            pojo.setAttachments(ThriftCollectionConverter.mapSet(thrift.getAttachments(), e -> org.eclipse.sw360.common.utils.converter.attachments.AttachmentConverter.fromThrift(e)));
        }
        if (thrift.isSetBlog()) {
            pojo.setBlog(thrift.getBlog());
        }
        if (thrift.isSetBusinessUnit()) {
            pojo.setBusinessUnit(thrift.getBusinessUnit());
        }
        if (thrift.isSetCategories()) {
            pojo.setCategories(ThriftCollectionConverter.mapSet(thrift.getCategories(), e -> e));
        }
        if (thrift.isSetCdxComponentType()) {
            pojo.setCdxComponentType(EnumConverter.fromThrift(thrift.getCdxComponentType(), org.eclipse.sw360.datahandler.services.common.CycloneDxComponentType.class));
        }
        if (thrift.isSetComponentOwner()) {
            pojo.setComponentOwner(thrift.getComponentOwner());
        }
        if (thrift.isSetComponentType()) {
            pojo.setComponentType(EnumConverter.fromThrift(thrift.getComponentType(), org.eclipse.sw360.datahandler.services.components.ComponentType.class));
        }
        if (thrift.isSetCreatedBy()) {
            pojo.setCreatedBy(thrift.getCreatedBy());
        }
        if (thrift.isSetCreatedOn()) {
            pojo.setCreatedOn(thrift.getCreatedOn());
        }
        if (thrift.isSetDefaultVendor()) {
            pojo.setDefaultVendor(org.eclipse.sw360.common.utils.converter.vendors.VendorConverter.fromThrift(thrift.getDefaultVendor()));
        }
        if (thrift.isSetDefaultVendorId()) {
            pojo.setDefaultVendorId(thrift.getDefaultVendorId());
        }
        if (thrift.isSetDescription()) {
            pojo.setDescription(thrift.getDescription());
        }
        if (thrift.isSetDocumentState()) {
            pojo.setDocumentState(org.eclipse.sw360.common.utils.converter.common.DocumentStateConverter.fromThrift(thrift.getDocumentState()));
        }
        if (thrift.isSetExternalIds()) {
            pojo.setExternalIds(thrift.getExternalIds());
        }
        if (thrift.isSetHomepage()) {
            pojo.setHomepage(thrift.getHomepage());
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetLanguages()) {
            pojo.setLanguages(ThriftCollectionConverter.mapSet(thrift.getLanguages(), e -> e));
        }
        if (thrift.isSetMailinglist()) {
            pojo.setMailinglist(thrift.getMailinglist());
        }
        if (thrift.isSetMainLicenseIds()) {
            pojo.setMainLicenseIds(ThriftCollectionConverter.mapSet(thrift.getMainLicenseIds(), e -> e));
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
        if (thrift.isSetOpenHub()) {
            pojo.setOpenHub(thrift.getOpenHub());
        }
        if (thrift.isSetOperatingSystems()) {
            pojo.setOperatingSystems(ThriftCollectionConverter.mapSet(thrift.getOperatingSystems(), e -> e));
        }
        if (thrift.isSetOwnerAccountingUnit()) {
            pojo.setOwnerAccountingUnit(thrift.getOwnerAccountingUnit());
        }
        if (thrift.isSetOwnerCountry()) {
            pojo.setOwnerCountry(thrift.getOwnerCountry());
        }
        if (thrift.isSetOwnerGroup()) {
            pojo.setOwnerGroup(thrift.getOwnerGroup());
        }
        if (thrift.isSetPermissions()) {
            pojo.setPermissions(ThriftCollectionConverter.mapMap(thrift.getPermissions(), mapKey -> EnumConverter.fromThrift(mapKey, org.eclipse.sw360.datahandler.services.users.RequestedAction.class), mapValue -> mapValue));
        }
        if (thrift.isSetReleaseIds()) {
            pojo.setReleaseIds(ThriftCollectionConverter.mapSet(thrift.getReleaseIds(), e -> e));
        }
        if (thrift.isSetReleases()) {
            pojo.setReleases(ThriftCollectionConverter.mapList(thrift.getReleases(), e -> org.eclipse.sw360.common.utils.converter.components.ReleaseConverter.fromThrift(e)));
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
        if (thrift.isSetSubscribers()) {
            pojo.setSubscribers(ThriftCollectionConverter.mapSet(thrift.getSubscribers(), e -> e));
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        if (thrift.isSetVcs()) {
            pojo.setVcs(thrift.getVcs());
        }
        if (thrift.isSetVendorNames()) {
            pojo.setVendorNames(ThriftCollectionConverter.mapSet(thrift.getVendorNames(), e -> e));
        }
        if (thrift.isSetVisbility()) {
            pojo.setVisbility(EnumConverter.fromThrift(thrift.getVisbility(), org.eclipse.sw360.datahandler.services.common.Visibility.class));
        }
        if (thrift.isSetWiki()) {
            pojo.setWiki(thrift.getWiki());
        }
        if (thrift.isSetWikipedia()) {
            pojo.setWikipedia(thrift.getWikipedia());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.components.Component toThrift(Component pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.components.Component thrift = new org.eclipse.sw360.datahandler.thrift.components.Component();
        if (pojo.getAdditionalData() != null) {
            thrift.setAdditionalData(pojo.getAdditionalData());
        }
        if (pojo.getAttachments() != null) {
            thrift.setAttachments(ThriftCollectionConverter.mapSet(pojo.getAttachments(), e -> org.eclipse.sw360.common.utils.converter.attachments.AttachmentConverter.toThrift(e)));
        }
        if (pojo.getBlog() != null) {
            thrift.setBlog(pojo.getBlog());
        }
        if (pojo.getBusinessUnit() != null) {
            thrift.setBusinessUnit(pojo.getBusinessUnit());
        }
        if (pojo.getCategories() != null) {
            thrift.setCategories(ThriftCollectionConverter.mapSet(pojo.getCategories(), e -> e));
        }
        if (pojo.getCdxComponentType() != null) {
            thrift.setCdxComponentType(EnumConverter.toThrift(pojo.getCdxComponentType(), org.eclipse.sw360.datahandler.thrift.CycloneDxComponentType.class));
        }
        if (pojo.getComponentOwner() != null) {
            thrift.setComponentOwner(pojo.getComponentOwner());
        }
        if (pojo.getComponentType() != null) {
            thrift.setComponentType(EnumConverter.toThrift(pojo.getComponentType(), org.eclipse.sw360.datahandler.thrift.components.ComponentType.class));
        }
        if (pojo.getCreatedBy() != null) {
            thrift.setCreatedBy(pojo.getCreatedBy());
        }
        if (pojo.getCreatedOn() != null) {
            thrift.setCreatedOn(pojo.getCreatedOn());
        }
        if (pojo.getDefaultVendor() != null) {
            thrift.setDefaultVendor(org.eclipse.sw360.common.utils.converter.vendors.VendorConverter.toThrift(pojo.getDefaultVendor()));
        }
        if (pojo.getDefaultVendorId() != null) {
            thrift.setDefaultVendorId(pojo.getDefaultVendorId());
        }
        if (pojo.getDescription() != null) {
            thrift.setDescription(pojo.getDescription());
        }
        if (pojo.getDocumentState() != null) {
            thrift.setDocumentState(org.eclipse.sw360.common.utils.converter.common.DocumentStateConverter.toThrift(pojo.getDocumentState()));
        }
        if (pojo.getExternalIds() != null) {
            thrift.setExternalIds(pojo.getExternalIds());
        }
        if (pojo.getHomepage() != null) {
            thrift.setHomepage(pojo.getHomepage());
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getLanguages() != null) {
            thrift.setLanguages(ThriftCollectionConverter.mapSet(pojo.getLanguages(), e -> e));
        }
        if (pojo.getMailinglist() != null) {
            thrift.setMailinglist(pojo.getMailinglist());
        }
        if (pojo.getMainLicenseIds() != null) {
            thrift.setMainLicenseIds(ThriftCollectionConverter.mapSet(pojo.getMainLicenseIds(), e -> e));
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
        if (pojo.getOpenHub() != null) {
            thrift.setOpenHub(pojo.getOpenHub());
        }
        if (pojo.getOperatingSystems() != null) {
            thrift.setOperatingSystems(ThriftCollectionConverter.mapSet(pojo.getOperatingSystems(), e -> e));
        }
        if (pojo.getOwnerAccountingUnit() != null) {
            thrift.setOwnerAccountingUnit(pojo.getOwnerAccountingUnit());
        }
        if (pojo.getOwnerCountry() != null) {
            thrift.setOwnerCountry(pojo.getOwnerCountry());
        }
        if (pojo.getOwnerGroup() != null) {
            thrift.setOwnerGroup(pojo.getOwnerGroup());
        }
        if (pojo.getPermissions() != null) {
            thrift.setPermissions(ThriftCollectionConverter.mapMap(pojo.getPermissions(), mapKey -> EnumConverter.toThrift(mapKey, org.eclipse.sw360.datahandler.thrift.users.RequestedAction.class), mapValue -> mapValue));
        }
        if (pojo.getReleaseIds() != null) {
            thrift.setReleaseIds(ThriftCollectionConverter.mapSet(pojo.getReleaseIds(), e -> e));
        }
        if (pojo.getReleases() != null) {
            thrift.setReleases(ThriftCollectionConverter.mapList(pojo.getReleases(), e -> org.eclipse.sw360.common.utils.converter.components.ReleaseConverter.toThrift(e)));
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
        if (pojo.getSubscribers() != null) {
            thrift.setSubscribers(ThriftCollectionConverter.mapSet(pojo.getSubscribers(), e -> e));
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        if (pojo.getVcs() != null) {
            thrift.setVcs(pojo.getVcs());
        }
        if (pojo.getVendorNames() != null) {
            thrift.setVendorNames(ThriftCollectionConverter.mapSet(pojo.getVendorNames(), e -> e));
        }
        if (pojo.getVisbility() != null) {
            thrift.setVisbility(EnumConverter.toThrift(pojo.getVisbility(), org.eclipse.sw360.datahandler.thrift.Visibility.class));
        }
        if (pojo.getWiki() != null) {
            thrift.setWiki(pojo.getWiki());
        }
        if (pojo.getWikipedia() != null) {
            thrift.setWikipedia(pojo.getWikipedia());
        }
        return thrift;
    }
}
