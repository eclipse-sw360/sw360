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

import org.eclipse.sw360.datahandler.services.spdx.ExternalReference;

public final class ExternalReferenceConverter {

    private ExternalReferenceConverter() {}

    public static ExternalReference fromThrift(org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.ExternalReference thrift) {
        if (thrift == null) {
            return null;
        }
        ExternalReference pojo = new ExternalReference();
        if (thrift.isSetComment()) {
            pojo.setComment(thrift.getComment());
        }
        if (thrift.isSetIndex()) {
            pojo.setIndex(thrift.getIndex());
        }
        if (thrift.isSetReferenceCategory()) {
            pojo.setReferenceCategory(thrift.getReferenceCategory());
        }
        if (thrift.isSetReferenceLocator()) {
            pojo.setReferenceLocator(thrift.getReferenceLocator());
        }
        if (thrift.isSetReferenceType()) {
            pojo.setReferenceType(thrift.getReferenceType());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.ExternalReference toThrift(ExternalReference pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.ExternalReference thrift = new org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.ExternalReference();
        if (pojo.getComment() != null) {
            thrift.setComment(pojo.getComment());
        }
        if (pojo.getIndex() != null) {
            thrift.setIndex(pojo.getIndex());
        }
        if (pojo.getReferenceCategory() != null) {
            thrift.setReferenceCategory(pojo.getReferenceCategory());
        }
        if (pojo.getReferenceLocator() != null) {
            thrift.setReferenceLocator(pojo.getReferenceLocator());
        }
        if (pojo.getReferenceType() != null) {
            thrift.setReferenceType(pojo.getReferenceType());
        }
        return thrift;
    }
}
