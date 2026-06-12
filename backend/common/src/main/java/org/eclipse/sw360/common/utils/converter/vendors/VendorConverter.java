/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.vendors;

import org.eclipse.sw360.datahandler.services.vendors.Vendor;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class VendorConverter {

    private VendorConverter() {}

    public static Vendor fromThrift(org.eclipse.sw360.datahandler.thrift.vendors.Vendor thrift) {
        if (thrift == null) {
            return null;
        }
        Vendor pojo = new Vendor();
        if (thrift.isSetFullname()) {
            pojo.setFullname(thrift.getFullname());
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetPermissions()) {
            pojo.setPermissions(ThriftCollectionConverter.mapMap(thrift.getPermissions(), mapKey -> EnumConverter.fromThrift(mapKey, org.eclipse.sw360.datahandler.services.users.RequestedAction.class), mapValue -> mapValue));
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetShortname()) {
            pojo.setShortname(thrift.getShortname());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        if (thrift.isSetUrl()) {
            pojo.setUrl(thrift.getUrl());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.vendors.Vendor toThrift(Vendor pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.vendors.Vendor thrift = new org.eclipse.sw360.datahandler.thrift.vendors.Vendor();
        if (pojo.getFullname() != null) {
            thrift.setFullname(pojo.getFullname());
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getPermissions() != null) {
            thrift.setPermissions(ThriftCollectionConverter.mapMap(pojo.getPermissions(), mapKey -> EnumConverter.toThrift(mapKey, org.eclipse.sw360.datahandler.thrift.users.RequestedAction.class), mapValue -> mapValue));
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getShortname() != null) {
            thrift.setShortname(pojo.getShortname());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        if (pojo.getUrl() != null) {
            thrift.setUrl(pojo.getUrl());
        }
        return thrift;
    }
}
