/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.common;

import org.eclipse.sw360.datahandler.services.common.PaginationData;

public final class PaginationDataConverter {

    private PaginationDataConverter() {}

    public static PaginationData fromThrift(org.eclipse.sw360.datahandler.thrift.PaginationData thrift) {
        if (thrift == null) {
            return null;
        }
        PaginationData pojo = new PaginationData();
        if (thrift.isSetAscending()) {
            pojo.setAscending(thrift.isAscending());
        }
        if (thrift.isSetDisplayStart()) {
            pojo.setDisplayStart(thrift.getDisplayStart());
        }
        if (thrift.isSetRowsPerPage()) {
            pojo.setRowsPerPage(thrift.getRowsPerPage());
        }
        if (thrift.isSetSortColumnNumber()) {
            pojo.setSortColumnNumber(thrift.getSortColumnNumber());
        }
        if (thrift.isSetTotalRowCount()) {
            pojo.setTotalRowCount(thrift.getTotalRowCount());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.PaginationData toThrift(PaginationData pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.PaginationData thrift = new org.eclipse.sw360.datahandler.thrift.PaginationData();
        if (pojo.getAscending() != null) {
            thrift.setAscending(pojo.getAscending());
        }
        if (pojo.getDisplayStart() != null) {
            thrift.setDisplayStart(pojo.getDisplayStart());
        }
        if (pojo.getRowsPerPage() != null) {
            thrift.setRowsPerPage(pojo.getRowsPerPage());
        }
        if (pojo.getSortColumnNumber() != null) {
            thrift.setSortColumnNumber(pojo.getSortColumnNumber());
        }
        if (pojo.getTotalRowCount() != null) {
            thrift.setTotalRowCount(pojo.getTotalRowCount());
        }
        return thrift;
    }
}
