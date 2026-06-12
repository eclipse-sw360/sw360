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

import org.eclipse.sw360.datahandler.services.vulnerabilities.VendorAdvisory;

public final class VendorAdvisoryConverter {

    private VendorAdvisoryConverter() {}

    public static VendorAdvisory fromThrift(org.eclipse.sw360.datahandler.thrift.vulnerabilities.VendorAdvisory thrift) {
        if (thrift == null) {
            return null;
        }
        VendorAdvisory pojo = new VendorAdvisory();
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetName()) {
            pojo.setName(thrift.getName());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        if (thrift.isSetUrl()) {
            pojo.setUrl(thrift.getUrl());
        }
        if (thrift.isSetVendor()) {
            pojo.setVendor(thrift.getVendor());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.vulnerabilities.VendorAdvisory toThrift(VendorAdvisory pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.vulnerabilities.VendorAdvisory thrift = new org.eclipse.sw360.datahandler.thrift.vulnerabilities.VendorAdvisory();
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getName() != null) {
            thrift.setName(pojo.getName());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        if (pojo.getUrl() != null) {
            thrift.setUrl(pojo.getUrl());
        }
        if (pojo.getVendor() != null) {
            thrift.setVendor(pojo.getVendor());
        }
        return thrift;
    }
}
