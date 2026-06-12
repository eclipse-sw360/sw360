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

import org.eclipse.sw360.datahandler.services.spdx.DocumentCreationInformation;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class DocumentCreationInformationConverter {

    private DocumentCreationInformationConverter() {}

    public static DocumentCreationInformation fromThrift(org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation thrift) {
        if (thrift == null) {
            return null;
        }
        DocumentCreationInformation pojo = new DocumentCreationInformation();
        if (thrift.isSetCreated()) {
            pojo.setCreated(thrift.getCreated());
        }
        if (thrift.isSetCreatedBy()) {
            pojo.setCreatedBy(thrift.getCreatedBy());
        }
        if (thrift.isSetCreator()) {
            pojo.setCreator(ThriftCollectionConverter.mapSet(thrift.getCreator(), e -> org.eclipse.sw360.common.utils.converter.spdx.CreatorConverter.fromThrift(e)));
        }
        if (thrift.isSetCreatorComment()) {
            pojo.setCreatorComment(thrift.getCreatorComment());
        }
        if (thrift.isSetDataLicense()) {
            pojo.setDataLicense(thrift.getDataLicense());
        }
        if (thrift.isSetDocumentComment()) {
            pojo.setDocumentComment(thrift.getDocumentComment());
        }
        if (thrift.isSetDocumentNamespace()) {
            pojo.setDocumentNamespace(thrift.getDocumentNamespace());
        }
        if (thrift.isSetDocumentState()) {
            pojo.setDocumentState(org.eclipse.sw360.common.utils.converter.common.DocumentStateConverter.fromThrift(thrift.getDocumentState()));
        }
        if (thrift.isSetExternalDocumentRefs()) {
            pojo.setExternalDocumentRefs(ThriftCollectionConverter.mapSet(thrift.getExternalDocumentRefs(), e -> org.eclipse.sw360.common.utils.converter.spdx.ExternalDocumentReferencesConverter.fromThrift(e)));
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetLicenseListVersion()) {
            pojo.setLicenseListVersion(thrift.getLicenseListVersion());
        }
        if (thrift.isSetModerators()) {
            pojo.setModerators(ThriftCollectionConverter.mapSet(thrift.getModerators(), e -> e));
        }
        if (thrift.isSetName()) {
            pojo.setName(thrift.getName());
        }
        if (thrift.isSetPermissions()) {
            pojo.setPermissions(ThriftCollectionConverter.mapMap(thrift.getPermissions(), mapKey -> EnumConverter.fromThrift(mapKey, org.eclipse.sw360.datahandler.services.users.RequestedAction.class), mapValue -> mapValue));
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetSpdxDocumentId()) {
            pojo.setSpdxDocumentId(thrift.getSpdxDocumentId());
        }
        if (thrift.isSetSpdxVersion()) {
            pojo.setSpdxVersion(thrift.getSpdxVersion());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation toThrift(DocumentCreationInformation pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation thrift = new org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation();
        if (pojo.getCreated() != null) {
            thrift.setCreated(pojo.getCreated());
        }
        if (pojo.getCreatedBy() != null) {
            thrift.setCreatedBy(pojo.getCreatedBy());
        }
        if (pojo.getCreator() != null) {
            thrift.setCreator(ThriftCollectionConverter.mapSet(pojo.getCreator(), e -> org.eclipse.sw360.common.utils.converter.spdx.CreatorConverter.toThrift(e)));
        }
        if (pojo.getCreatorComment() != null) {
            thrift.setCreatorComment(pojo.getCreatorComment());
        }
        if (pojo.getDataLicense() != null) {
            thrift.setDataLicense(pojo.getDataLicense());
        }
        if (pojo.getDocumentComment() != null) {
            thrift.setDocumentComment(pojo.getDocumentComment());
        }
        if (pojo.getDocumentNamespace() != null) {
            thrift.setDocumentNamespace(pojo.getDocumentNamespace());
        }
        if (pojo.getDocumentState() != null) {
            thrift.setDocumentState(org.eclipse.sw360.common.utils.converter.common.DocumentStateConverter.toThrift(pojo.getDocumentState()));
        }
        if (pojo.getExternalDocumentRefs() != null) {
            thrift.setExternalDocumentRefs(ThriftCollectionConverter.mapSet(pojo.getExternalDocumentRefs(), e -> org.eclipse.sw360.common.utils.converter.spdx.ExternalDocumentReferencesConverter.toThrift(e)));
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getLicenseListVersion() != null) {
            thrift.setLicenseListVersion(pojo.getLicenseListVersion());
        }
        if (pojo.getModerators() != null) {
            thrift.setModerators(ThriftCollectionConverter.mapSet(pojo.getModerators(), e -> e));
        }
        if (pojo.getName() != null) {
            thrift.setName(pojo.getName());
        }
        if (pojo.getPermissions() != null) {
            thrift.setPermissions(ThriftCollectionConverter.mapMap(pojo.getPermissions(), mapKey -> EnumConverter.toThrift(mapKey, org.eclipse.sw360.datahandler.thrift.users.RequestedAction.class), mapValue -> mapValue));
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getSpdxDocumentId() != null) {
            thrift.setSpdxDocumentId(pojo.getSpdxDocumentId());
        }
        if (pojo.getSpdxVersion() != null) {
            thrift.setSpdxVersion(pojo.getSpdxVersion());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        return thrift;
    }
}
