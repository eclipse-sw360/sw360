/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.vulnerabilities;

import org.eclipse.sw360.datahandler.services.vulnerabilities.CVEReference;

public final class CVEReferenceConverter {

    private CVEReferenceConverter() {}

    public static CVEReference fromThrift(org.eclipse.sw360.datahandler.thrift.vulnerabilities.CVEReference thrift) {
        if (thrift == null) {
            return null;
        }
        CVEReference pojo = new CVEReference();
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetNumber()) {
            pojo.setNumber(thrift.getNumber());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        if (thrift.isSetYear()) {
            pojo.setYear(thrift.getYear());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.vulnerabilities.CVEReference toThrift(CVEReference pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.vulnerabilities.CVEReference thrift = new org.eclipse.sw360.datahandler.thrift.vulnerabilities.CVEReference();
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getNumber() != null) {
            thrift.setNumber(pojo.getNumber());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        if (pojo.getYear() != null) {
            thrift.setYear(pojo.getYear());
        }
        return thrift;
    }
}
