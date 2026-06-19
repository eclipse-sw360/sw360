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

import org.eclipse.sw360.datahandler.services.components.ReleaseLink;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class ReleaseLinkConverter {

    private ReleaseLinkConverter() {}

    public static ReleaseLink fromThrift(org.eclipse.sw360.datahandler.thrift.components.ReleaseLink thrift) {
        if (thrift == null) {
            return null;
        }
        ReleaseLink pojo = new ReleaseLink();
        if (thrift.isSetAccessible()) {
            pojo.setAccessible(thrift.isAccessible());
        }
        if (thrift.isSetAttachments()) {
            pojo.setAttachments(ThriftCollectionConverter.mapList(thrift.getAttachments(), e -> org.eclipse.sw360.common.utils.converter.attachments.AttachmentConverter.fromThrift(e)));
        }
        if (thrift.isSetClearingReport()) {
            pojo.setClearingReport(org.eclipse.sw360.common.utils.converter.components.ClearingReportConverter.fromThrift(thrift.getClearingReport()));
        }
        if (thrift.isSetClearingState()) {
            pojo.setClearingState(EnumConverter.fromThrift(thrift.getClearingState(), org.eclipse.sw360.datahandler.services.components.ClearingState.class));
        }
        if (thrift.isSetComment()) {
            pojo.setComment(thrift.getComment());
        }
        if (thrift.isSetComponentId()) {
            pojo.setComponentId(thrift.getComponentId());
        }
        if (thrift.isSetComponentType()) {
            pojo.setComponentType(EnumConverter.fromThrift(thrift.getComponentType(), org.eclipse.sw360.datahandler.services.components.ComponentType.class));
        }
        if (thrift.isSetCreatedBy()) {
            pojo.setCreatedBy(thrift.getCreatedBy());
        }
        if (thrift.isSetDefaultValue()) {
            pojo.setDefaultValue(thrift.getDefaultValue());
        }
        if (thrift.isSetHasSubreleases()) {
            pojo.setHasSubreleases(thrift.isHasSubreleases());
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetIndex()) {
            pojo.setIndex(thrift.getIndex());
        }
        if (thrift.isSetLayer()) {
            pojo.setLayer(thrift.getLayer());
        }
        if (thrift.isSetLicenseIds()) {
            pojo.setLicenseIds(ThriftCollectionConverter.mapSet(thrift.getLicenseIds(), e -> e));
        }
        if (thrift.isSetLicenseNames()) {
            pojo.setLicenseNames(ThriftCollectionConverter.mapSet(thrift.getLicenseNames(), e -> e));
        }
        if (thrift.isSetLongName()) {
            pojo.setLongName(thrift.getLongName());
        }
        if (thrift.isSetMainlineState()) {
            pojo.setMainlineState(EnumConverter.fromThrift(thrift.getMainlineState(), org.eclipse.sw360.datahandler.services.common.MainlineState.class));
        }
        if (thrift.isSetName()) {
            pojo.setName(thrift.getName());
        }
        if (thrift.isSetNodeId()) {
            pojo.setNodeId(thrift.getNodeId());
        }
        if (thrift.isSetOtherLicenseIds()) {
            pojo.setOtherLicenseIds(ThriftCollectionConverter.mapSet(thrift.getOtherLicenseIds(), e -> e));
        }
        if (thrift.isSetParentNodeId()) {
            pojo.setParentNodeId(thrift.getParentNodeId());
        }
        if (thrift.isSetProjectId()) {
            pojo.setProjectId(thrift.getProjectId());
        }
        if (thrift.isSetReleaseMainLineState()) {
            pojo.setReleaseMainLineState(EnumConverter.fromThrift(thrift.getReleaseMainLineState(), org.eclipse.sw360.datahandler.services.common.MainlineState.class));
        }
        if (thrift.isSetReleaseRelationship()) {
            pojo.setReleaseRelationship(EnumConverter.fromThrift(thrift.getReleaseRelationship(), org.eclipse.sw360.datahandler.services.common.ReleaseRelationship.class));
        }
        if (thrift.isSetReleaseWithSameComponent()) {
            pojo.setReleaseWithSameComponent(ThriftCollectionConverter.mapList(thrift.getReleaseWithSameComponent(), e -> org.eclipse.sw360.common.utils.converter.components.ReleaseConverter.fromThrift(e)));
        }
        if (thrift.isSetVendor()) {
            pojo.setVendor(thrift.getVendor());
        }
        if (thrift.isSetVersion()) {
            pojo.setVersion(thrift.getVersion());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.components.ReleaseLink toThrift(ReleaseLink pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.components.ReleaseLink thrift = new org.eclipse.sw360.datahandler.thrift.components.ReleaseLink();
        if (pojo.getAccessible() != null) {
            thrift.setAccessible(pojo.getAccessible());
        }
        if (pojo.getAttachments() != null) {
            thrift.setAttachments(ThriftCollectionConverter.mapList(pojo.getAttachments(), e -> org.eclipse.sw360.common.utils.converter.attachments.AttachmentConverter.toThrift(e)));
        }
        if (pojo.getClearingReport() != null) {
            thrift.setClearingReport(org.eclipse.sw360.common.utils.converter.components.ClearingReportConverter.toThrift(pojo.getClearingReport()));
        }
        if (pojo.getClearingState() != null) {
            thrift.setClearingState(EnumConverter.toThrift(pojo.getClearingState(), org.eclipse.sw360.datahandler.thrift.components.ClearingState.class));
        }
        if (pojo.getComment() != null) {
            thrift.setComment(pojo.getComment());
        }
        if (pojo.getComponentId() != null) {
            thrift.setComponentId(pojo.getComponentId());
        }
        if (pojo.getComponentType() != null) {
            thrift.setComponentType(EnumConverter.toThrift(pojo.getComponentType(), org.eclipse.sw360.datahandler.thrift.components.ComponentType.class));
        }
        if (pojo.getCreatedBy() != null) {
            thrift.setCreatedBy(pojo.getCreatedBy());
        }
        if (pojo.getDefaultValue() != null) {
            thrift.setDefaultValue(pojo.getDefaultValue());
        }
        if (pojo.getHasSubreleases() != null) {
            thrift.setHasSubreleases(pojo.getHasSubreleases());
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getIndex() != null) {
            thrift.setIndex(pojo.getIndex());
        }
        if (pojo.getLayer() != null) {
            thrift.setLayer(pojo.getLayer());
        }
        if (pojo.getLicenseIds() != null) {
            thrift.setLicenseIds(ThriftCollectionConverter.mapSet(pojo.getLicenseIds(), e -> e));
        }
        if (pojo.getLicenseNames() != null) {
            thrift.setLicenseNames(ThriftCollectionConverter.mapSet(pojo.getLicenseNames(), e -> e));
        }
        if (pojo.getLongName() != null) {
            thrift.setLongName(pojo.getLongName());
        }
        if (pojo.getMainlineState() != null) {
            thrift.setMainlineState(EnumConverter.toThrift(pojo.getMainlineState(), org.eclipse.sw360.datahandler.thrift.MainlineState.class));
        }
        if (pojo.getName() != null) {
            thrift.setName(pojo.getName());
        }
        if (pojo.getNodeId() != null) {
            thrift.setNodeId(pojo.getNodeId());
        }
        if (pojo.getOtherLicenseIds() != null) {
            thrift.setOtherLicenseIds(ThriftCollectionConverter.mapSet(pojo.getOtherLicenseIds(), e -> e));
        }
        if (pojo.getParentNodeId() != null) {
            thrift.setParentNodeId(pojo.getParentNodeId());
        }
        if (pojo.getProjectId() != null) {
            thrift.setProjectId(pojo.getProjectId());
        }
        if (pojo.getReleaseMainLineState() != null) {
            thrift.setReleaseMainLineState(EnumConverter.toThrift(pojo.getReleaseMainLineState(), org.eclipse.sw360.datahandler.thrift.MainlineState.class));
        }
        if (pojo.getReleaseRelationship() != null) {
            thrift.setReleaseRelationship(EnumConverter.toThrift(pojo.getReleaseRelationship(), org.eclipse.sw360.datahandler.thrift.ReleaseRelationship.class));
        }
        if (pojo.getReleaseWithSameComponent() != null) {
            thrift.setReleaseWithSameComponent(ThriftCollectionConverter.mapList(pojo.getReleaseWithSameComponent(), e -> org.eclipse.sw360.common.utils.converter.components.ReleaseConverter.toThrift(e)));
        }
        if (pojo.getVendor() != null) {
            thrift.setVendor(pojo.getVendor());
        }
        if (pojo.getVersion() != null) {
            thrift.setVersion(pojo.getVersion());
        }
        return thrift;
    }
}
