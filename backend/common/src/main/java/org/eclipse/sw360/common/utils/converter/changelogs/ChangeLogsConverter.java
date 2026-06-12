/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.changelogs;

import org.eclipse.sw360.datahandler.services.changelogs.ChangeLogs;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class ChangeLogsConverter {

    private ChangeLogsConverter() {}

    public static ChangeLogs fromThrift(org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs thrift) {
        if (thrift == null) {
            return null;
        }
        ChangeLogs pojo = new ChangeLogs();
        if (thrift.isSetChangeTimestamp()) {
            pojo.setChangeTimestamp(thrift.getChangeTimestamp());
        }
        if (thrift.isSetChanges()) {
            pojo.setChanges(ThriftCollectionConverter.mapSet(thrift.getChanges(), e -> org.eclipse.sw360.common.utils.converter.changelogs.ChangedFieldsConverter.fromThrift(e)));
        }
        if (thrift.isSetDbName()) {
            pojo.setDbName(thrift.getDbName());
        }
        if (thrift.isSetDocumentId()) {
            pojo.setDocumentId(thrift.getDocumentId());
        }
        if (thrift.isSetDocumentType()) {
            pojo.setDocumentType(thrift.getDocumentType());
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetInfo()) {
            pojo.setInfo(thrift.getInfo());
        }
        if (thrift.isSetOperation()) {
            pojo.setOperation(EnumConverter.fromThrift(thrift.getOperation(), org.eclipse.sw360.datahandler.services.changelogs.Operation.class));
        }
        if (thrift.isSetParentDocId()) {
            pojo.setParentDocId(thrift.getParentDocId());
        }
        if (thrift.isSetReferenceDoc()) {
            pojo.setReferenceDoc(ThriftCollectionConverter.mapSet(thrift.getReferenceDoc(), e -> org.eclipse.sw360.common.utils.converter.changelogs.ReferenceDocDataConverter.fromThrift(e)));
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        if (thrift.isSetUserEdited()) {
            pojo.setUserEdited(thrift.getUserEdited());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs toThrift(ChangeLogs pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs thrift = new org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs();
        if (pojo.getChangeTimestamp() != null) {
            thrift.setChangeTimestamp(pojo.getChangeTimestamp());
        }
        if (pojo.getChanges() != null) {
            thrift.setChanges(ThriftCollectionConverter.mapSet(pojo.getChanges(), e -> org.eclipse.sw360.common.utils.converter.changelogs.ChangedFieldsConverter.toThrift(e)));
        }
        if (pojo.getDbName() != null) {
            thrift.setDbName(pojo.getDbName());
        }
        if (pojo.getDocumentId() != null) {
            thrift.setDocumentId(pojo.getDocumentId());
        }
        if (pojo.getDocumentType() != null) {
            thrift.setDocumentType(pojo.getDocumentType());
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getInfo() != null) {
            thrift.setInfo(pojo.getInfo());
        }
        if (pojo.getOperation() != null) {
            thrift.setOperation(EnumConverter.toThrift(pojo.getOperation(), org.eclipse.sw360.datahandler.thrift.changelogs.Operation.class));
        }
        if (pojo.getParentDocId() != null) {
            thrift.setParentDocId(pojo.getParentDocId());
        }
        if (pojo.getReferenceDoc() != null) {
            thrift.setReferenceDoc(ThriftCollectionConverter.mapSet(pojo.getReferenceDoc(), e -> org.eclipse.sw360.common.utils.converter.changelogs.ReferenceDocDataConverter.toThrift(e)));
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        if (pojo.getUserEdited() != null) {
            thrift.setUserEdited(pojo.getUserEdited());
        }
        return thrift;
    }
}
