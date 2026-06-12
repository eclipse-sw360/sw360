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

import org.eclipse.sw360.datahandler.services.spdx.SnippetRange;

public final class SnippetRangeConverter {

    private SnippetRangeConverter() {}

    public static SnippetRange fromThrift(org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetRange thrift) {
        if (thrift == null) {
            return null;
        }
        SnippetRange pojo = new SnippetRange();
        if (thrift.isSetEndPointer()) {
            pojo.setEndPointer(thrift.getEndPointer());
        }
        if (thrift.isSetIndex()) {
            pojo.setIndex(thrift.getIndex());
        }
        if (thrift.isSetRangeType()) {
            pojo.setRangeType(thrift.getRangeType());
        }
        if (thrift.isSetReference()) {
            pojo.setReference(thrift.getReference());
        }
        if (thrift.isSetStartPointer()) {
            pojo.setStartPointer(thrift.getStartPointer());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetRange toThrift(SnippetRange pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetRange thrift = new org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetRange();
        if (pojo.getEndPointer() != null) {
            thrift.setEndPointer(pojo.getEndPointer());
        }
        if (pojo.getIndex() != null) {
            thrift.setIndex(pojo.getIndex());
        }
        if (pojo.getRangeType() != null) {
            thrift.setRangeType(pojo.getRangeType());
        }
        if (pojo.getReference() != null) {
            thrift.setReference(pojo.getReference());
        }
        if (pojo.getStartPointer() != null) {
            thrift.setStartPointer(pojo.getStartPointer());
        }
        return thrift;
    }
}
