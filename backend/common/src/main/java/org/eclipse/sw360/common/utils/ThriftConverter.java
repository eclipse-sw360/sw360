/*
 *  Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 * 
 *  This program and the accompanying materials are made
 *  available under the terms of the Eclipse Public License 2.0
 *  which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 *  SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.common.utils;

import org.eclipse.sw360.datahandler.services.changelogs.ChangedFields;
import org.eclipse.sw360.datahandler.services.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.services.changelogs.Operation;
import org.eclipse.sw360.datahandler.services.changelogs.ReferenceDocData;
import org.eclipse.sw360.datahandler.services.common.ConfigContainer;
import org.eclipse.sw360.datahandler.services.common.ConfigFor;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import java.util.stream.Collectors;

public final class ThriftConverter {

    private ThriftConverter() {}

    // ---- Shared: SW360Exception ----

    public static SW360Exception fromThriftException(
            org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
        return new SW360Exception(e.getWhy(), e.getErrorCode());
    }

    // ---- Shared: RequestStatus ----

    public static RequestStatus fromThriftRequestStatus(
            org.eclipse.sw360.datahandler.thrift.RequestStatus thrift) {
        if (thrift == null) return null;
        return RequestStatus.valueOf(thrift.name());
    }

    // ---- Shared: ConfigFor ----

    public static org.eclipse.sw360.datahandler.thrift.ConfigFor toThriftConfigFor(ConfigFor pojo) {
        if (pojo == null) return null;
        return org.eclipse.sw360.datahandler.thrift.ConfigFor.valueOf(pojo.name());
    }

    // ---- Shared: PaginationData ----

    public static org.eclipse.sw360.datahandler.thrift.PaginationData toThriftPaginationData(PaginationData pojo){
        if(pojo == null) return null;
        org.eclipse.sw360.datahandler.thrift.PaginationData thrift = new org.eclipse.sw360.datahandler.thrift.PaginationData();
        if(pojo.getAscending() != null)thrift.setAscending(pojo.getAscending());
        if(pojo.getDisplayStart() != null) thrift.setDisplayStart(pojo.getDisplayStart());
        if(pojo.getTotalRowCount() != null) thrift.setTotalRowCount(pojo.getTotalRowCount());
        if(pojo.getSortColumnNumber() != null) thrift.setSortColumnNumber(pojo.getSortColumnNumber());
        if(pojo.getRowsPerPage() != null) thrift.setRowsPerPage(pojo.getRowsPerPage());
        return thrift;
    }

    public static PaginationData fromThriftPaginationData(org.eclipse.sw360.datahandler.thrift.PaginationData thrift) {
        if(thrift == null) return null;
        PaginationData pojo = new PaginationData(); 
        if(thrift.isSetAscending())pojo.setAscending(thrift.isAscending());
        if(thrift.isSetDisplayStart()) pojo.setDisplayStart(thrift.getDisplayStart() );
        if(thrift.isSetTotalRowCount()) pojo.setTotalRowCount(thrift.getTotalRowCount());
        if(thrift.isSetSortColumnNumber()) pojo.setSortColumnNumber(thrift.getSortColumnNumber());
        if(thrift.isSetRowsPerPage()) pojo.setRowsPerPage(thrift.getRowsPerPage()); 
        return pojo;
    }

    // ---- Configurations: ConfigContainer ----

    public static org.eclipse.sw360.datahandler.thrift.ConfigContainer toThriftConfigContainer(ConfigContainer pojo) {
        if (pojo == null) return null;
        org.eclipse.sw360.datahandler.thrift.ConfigContainer thrift = new org.eclipse.sw360.datahandler.thrift.ConfigContainer();
        if (pojo.getId() != null) thrift.setId(pojo.getId());
        if (pojo.getRevision() != null) thrift.setRevision(pojo.getRevision());
        thrift.setConfigFor(toThriftConfigFor(pojo.getConfigFor()));
        thrift.setConfigKeyToValues(pojo.getConfigKeyToValues());
        return thrift;
    }

    // ---- Changelogs: ChangeLogs ----

    public static ChangedFields fromThriftChangedFields(
        org.eclipse.sw360.datahandler.thrift.changelogs.ChangedFields thrift) {
        if (thrift == null) return null;

        ChangedFields pojo = new ChangedFields();

        if (thrift.isSetFieldName())
            pojo.setFieldName(thrift.getFieldName());

        if (thrift.isSetFieldValueOld())
            pojo.setFieldValueOld(thrift.getFieldValueOld());

        if (thrift.isSetFieldValueNew())
            pojo.setFieldValueNew(thrift.getFieldValueNew());

        return pojo;
    }

    public static ReferenceDocData fromThriftReferenceDocData(
            org.eclipse.sw360.datahandler.thrift.changelogs.ReferenceDocData thrift) {
        if (thrift == null) return null;

        ReferenceDocData pojo = new ReferenceDocData();

        if (thrift.isSetRefDocId())
            pojo.setRefDocId(thrift.getRefDocId());

        if (thrift.isSetDbName())
            pojo.setDbName(thrift.getDbName());

        if (thrift.isSetRefDocType())
            pojo.setRefDocType(thrift.getRefDocType());

        if (thrift.isSetRefDocOperation())
            pojo.setRefDocOperation(fromThriftOperation(thrift.getRefDocOperation()));

        return pojo;
    }

    public static Operation fromThriftOperation(org.eclipse.sw360.datahandler.thrift.changelogs.Operation thrift) {
        if(thrift == null) return null;
        return Operation.valueOf(thrift.name());
    }

    public static ChangeLogs fromThriftChangeLogs(org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs thrift) {
        if(thrift == null) return null;
        ChangeLogs pojo = new ChangeLogs(); 

        if(thrift.isSetChangeTimestamp())pojo.setChangeTimestamp(thrift.getChangeTimestamp());
        if(thrift.isSetChanges())
        pojo.setChanges(
            thrift.getChanges()
            .stream()
            .map(ThriftConverter::fromThriftChangedFields)
            .collect(Collectors.toSet())
            );
        if(thrift.isSetDbName())pojo.setDbName(thrift.getDbName());
        if(thrift.isSetInfo())pojo.setInfo(thrift.getInfo());
        if(thrift.isSetDocumentId())pojo.setDocumentId(thrift.getDocumentId());
        if(thrift.isSetDocumentType())pojo.setDocumentType(thrift.getDocumentType());
        if(thrift.isSetOperation())pojo.setOperation(fromThriftOperation( thrift.getOperation()));
        if(thrift.isSetParentDocId())pojo.setParentDocId(thrift.getParentDocId());
        if(thrift.isSetReferenceDoc())
        pojo.setReferenceDoc(
            thrift.getReferenceDoc()
            .stream()
            .map(ThriftConverter::fromThriftReferenceDocData)
            .collect(Collectors.toSet())
            );
        if(thrift.isSetRevision())pojo.setRevision(thrift.getRevision());
        if(thrift.isSetUserEdited())pojo.setUserEdited(thrift.getUserEdited());
        if(thrift.isSetId())pojo.setId(thrift.getId());
        if(thrift.isSetType())pojo.setType(thrift.getType());
        return pojo;
    }

}
