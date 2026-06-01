/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.licenses;

import org.eclipse.sw360.datahandler.services.licenses.License;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class LicenseConverter {

    private LicenseConverter() {}

    public static License fromThrift(org.eclipse.sw360.datahandler.thrift.licenses.License thrift) {
        if (thrift == null) {
            return null;
        }
        License pojo = new License();
        if (thrift.isSetAdditionalData()) {
            pojo.setAdditionalData(thrift.getAdditionalData());
        }
        if (thrift.isSetChecked()) {
            pojo.setChecked(thrift.isChecked());
        }
        if (thrift.isSetDocumentState()) {
            pojo.setDocumentState(org.eclipse.sw360.common.utils.converter.common.DocumentStateConverter.fromThrift(thrift.getDocumentState()));
        }
        if (thrift.isSetExternalIds()) {
            pojo.setExternalIds(thrift.getExternalIds());
        }
        if (thrift.isSetExternalLicenseLink()) {
            pojo.setExternalLicenseLink(thrift.getExternalLicenseLink());
        }
        if (thrift.isSetFullname()) {
            pojo.setFullname(thrift.getFullname());
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetLicenseType()) {
            pojo.setLicenseType(org.eclipse.sw360.common.utils.converter.licenses.LicenseTypeConverter.fromThrift(thrift.getLicenseType()));
        }
        if (thrift.isSetLicenseTypeDatabaseId()) {
            pojo.setLicenseTypeDatabaseId(thrift.getLicenseTypeDatabaseId());
        }
        if (thrift.isSetNote()) {
            pojo.setNote(thrift.getNote());
        }
        if (thrift.isSetObligationDatabaseIds()) {
            pojo.setObligationDatabaseIds(ThriftCollectionConverter.mapSet(thrift.getObligationDatabaseIds(), e -> e));
        }
        if (thrift.isSetObligationListId()) {
            pojo.setObligationListId(thrift.getObligationListId());
        }
        if (thrift.isSetObligations()) {
            pojo.setObligations(ThriftCollectionConverter.mapList(thrift.getObligations(), e -> org.eclipse.sw360.common.utils.converter.licenses.ObligationConverter.fromThrift(e)));
        }
        if (thrift.isSetPermissions()) {
            pojo.setPermissions(ThriftCollectionConverter.mapMap(thrift.getPermissions(), mapKey -> EnumConverter.fromThrift(mapKey, org.eclipse.sw360.datahandler.services.users.RequestedAction.class), mapValue -> mapValue));
        }
        if (thrift.isSetReviewdate()) {
            pojo.setReviewdate(thrift.getReviewdate());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetShortname()) {
            pojo.setShortname(thrift.getShortname());
        }
        if (thrift.isSetText()) {
            pojo.setText(thrift.getText());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.licenses.License toThrift(License pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.licenses.License thrift = new org.eclipse.sw360.datahandler.thrift.licenses.License();
        if (pojo.getAdditionalData() != null) {
            thrift.setAdditionalData(pojo.getAdditionalData());
        }
        if (pojo.getChecked() != null) {
            thrift.setChecked(pojo.getChecked());
        }
        if (pojo.getDocumentState() != null) {
            thrift.setDocumentState(org.eclipse.sw360.common.utils.converter.common.DocumentStateConverter.toThrift(pojo.getDocumentState()));
        }
        if (pojo.getExternalIds() != null) {
            thrift.setExternalIds(pojo.getExternalIds());
        }
        if (pojo.getExternalLicenseLink() != null) {
            thrift.setExternalLicenseLink(pojo.getExternalLicenseLink());
        }
        if (pojo.getFullname() != null) {
            thrift.setFullname(pojo.getFullname());
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getLicenseType() != null) {
            thrift.setLicenseType(org.eclipse.sw360.common.utils.converter.licenses.LicenseTypeConverter.toThrift(pojo.getLicenseType()));
        }
        if (pojo.getLicenseTypeDatabaseId() != null) {
            thrift.setLicenseTypeDatabaseId(pojo.getLicenseTypeDatabaseId());
        }
        if (pojo.getNote() != null) {
            thrift.setNote(pojo.getNote());
        }
        if (pojo.getObligationDatabaseIds() != null) {
            thrift.setObligationDatabaseIds(ThriftCollectionConverter.mapSet(pojo.getObligationDatabaseIds(), e -> e));
        }
        if (pojo.getObligationListId() != null) {
            thrift.setObligationListId(pojo.getObligationListId());
        }
        if (pojo.getObligations() != null) {
            thrift.setObligations(ThriftCollectionConverter.mapList(pojo.getObligations(), e -> org.eclipse.sw360.common.utils.converter.licenses.ObligationConverter.toThrift(e)));
        }
        if (pojo.getPermissions() != null) {
            thrift.setPermissions(ThriftCollectionConverter.mapMap(pojo.getPermissions(), mapKey -> EnumConverter.toThrift(mapKey, org.eclipse.sw360.datahandler.thrift.users.RequestedAction.class), mapValue -> mapValue));
        }
        if (pojo.getReviewdate() != null) {
            thrift.setReviewdate(pojo.getReviewdate());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getShortname() != null) {
            thrift.setShortname(pojo.getShortname());
        }
        if (pojo.getText() != null) {
            thrift.setText(pojo.getText());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        return thrift;
    }
}
