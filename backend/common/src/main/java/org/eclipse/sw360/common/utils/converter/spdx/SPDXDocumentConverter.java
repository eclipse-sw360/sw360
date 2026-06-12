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

import org.eclipse.sw360.datahandler.services.spdx.SPDXDocument;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class SPDXDocumentConverter {

    private SPDXDocumentConverter() {}

    public static SPDXDocument fromThrift(org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument thrift) {
        if (thrift == null) {
            return null;
        }
        SPDXDocument pojo = new SPDXDocument();
        if (thrift.isSetAnnotations()) {
            pojo.setAnnotations(ThriftCollectionConverter.mapSet(thrift.getAnnotations(), e -> org.eclipse.sw360.common.utils.converter.spdx.AnnotationsConverter.fromThrift(e)));
        }
        if (thrift.isSetCreatedBy()) {
            pojo.setCreatedBy(thrift.getCreatedBy());
        }
        if (thrift.isSetDocumentState()) {
            pojo.setDocumentState(org.eclipse.sw360.common.utils.converter.common.DocumentStateConverter.fromThrift(thrift.getDocumentState()));
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetModerators()) {
            pojo.setModerators(ThriftCollectionConverter.mapSet(thrift.getModerators(), e -> e));
        }
        if (thrift.isSetOtherLicensingInformationDetecteds()) {
            pojo.setOtherLicensingInformationDetecteds(ThriftCollectionConverter.mapSet(thrift.getOtherLicensingInformationDetecteds(), e -> org.eclipse.sw360.common.utils.converter.spdx.OtherLicensingInformationDetectedConverter.fromThrift(e)));
        }
        if (thrift.isSetPermissions()) {
            pojo.setPermissions(ThriftCollectionConverter.mapMap(thrift.getPermissions(), mapKey -> EnumConverter.fromThrift(mapKey, org.eclipse.sw360.datahandler.services.users.RequestedAction.class), mapValue -> mapValue));
        }
        if (thrift.isSetRelationships()) {
            pojo.setRelationships(ThriftCollectionConverter.mapSet(thrift.getRelationships(), e -> org.eclipse.sw360.common.utils.converter.spdx.RelationshipsBetweenSPDXElementsConverter.fromThrift(e)));
        }
        if (thrift.isSetReleaseId()) {
            pojo.setReleaseId(thrift.getReleaseId());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetSnippets()) {
            pojo.setSnippets(ThriftCollectionConverter.mapSet(thrift.getSnippets(), e -> org.eclipse.sw360.common.utils.converter.spdx.SnippetInformationConverter.fromThrift(e)));
        }
        if (thrift.isSetSpdxDocumentCreationInfoId()) {
            pojo.setSpdxDocumentCreationInfoId(thrift.getSpdxDocumentCreationInfoId());
        }
        if (thrift.isSetSpdxFileInfoIds()) {
            pojo.setSpdxFileInfoIds(ThriftCollectionConverter.mapSet(thrift.getSpdxFileInfoIds(), e -> e));
        }
        if (thrift.isSetSpdxPackageInfoIds()) {
            pojo.setSpdxPackageInfoIds(ThriftCollectionConverter.mapSet(thrift.getSpdxPackageInfoIds(), e -> e));
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument toThrift(SPDXDocument pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument thrift = new org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument();
        if (pojo.getAnnotations() != null) {
            thrift.setAnnotations(ThriftCollectionConverter.mapSet(pojo.getAnnotations(), e -> org.eclipse.sw360.common.utils.converter.spdx.AnnotationsConverter.toThrift(e)));
        }
        if (pojo.getCreatedBy() != null) {
            thrift.setCreatedBy(pojo.getCreatedBy());
        }
        if (pojo.getDocumentState() != null) {
            thrift.setDocumentState(org.eclipse.sw360.common.utils.converter.common.DocumentStateConverter.toThrift(pojo.getDocumentState()));
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getModerators() != null) {
            thrift.setModerators(ThriftCollectionConverter.mapSet(pojo.getModerators(), e -> e));
        }
        if (pojo.getOtherLicensingInformationDetecteds() != null) {
            thrift.setOtherLicensingInformationDetecteds(ThriftCollectionConverter.mapSet(pojo.getOtherLicensingInformationDetecteds(), e -> org.eclipse.sw360.common.utils.converter.spdx.OtherLicensingInformationDetectedConverter.toThrift(e)));
        }
        if (pojo.getPermissions() != null) {
            thrift.setPermissions(ThriftCollectionConverter.mapMap(pojo.getPermissions(), mapKey -> EnumConverter.toThrift(mapKey, org.eclipse.sw360.datahandler.thrift.users.RequestedAction.class), mapValue -> mapValue));
        }
        if (pojo.getRelationships() != null) {
            thrift.setRelationships(ThriftCollectionConverter.mapSet(pojo.getRelationships(), e -> org.eclipse.sw360.common.utils.converter.spdx.RelationshipsBetweenSPDXElementsConverter.toThrift(e)));
        }
        if (pojo.getReleaseId() != null) {
            thrift.setReleaseId(pojo.getReleaseId());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getSnippets() != null) {
            thrift.setSnippets(ThriftCollectionConverter.mapSet(pojo.getSnippets(), e -> org.eclipse.sw360.common.utils.converter.spdx.SnippetInformationConverter.toThrift(e)));
        }
        if (pojo.getSpdxDocumentCreationInfoId() != null) {
            thrift.setSpdxDocumentCreationInfoId(pojo.getSpdxDocumentCreationInfoId());
        }
        if (pojo.getSpdxFileInfoIds() != null) {
            thrift.setSpdxFileInfoIds(ThriftCollectionConverter.mapSet(pojo.getSpdxFileInfoIds(), e -> e));
        }
        if (pojo.getSpdxPackageInfoIds() != null) {
            thrift.setSpdxPackageInfoIds(ThriftCollectionConverter.mapSet(pojo.getSpdxPackageInfoIds(), e -> e));
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        return thrift;
    }
}
