/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.attachments;

import org.eclipse.sw360.datahandler.services.attachments.DatabaseAddress;

public final class DatabaseAddressConverter {

    private DatabaseAddressConverter() {}

    public static DatabaseAddress fromThrift(org.eclipse.sw360.datahandler.thrift.attachments.DatabaseAddress thrift) {
        if (thrift == null) {
            return null;
        }
        DatabaseAddress pojo = new DatabaseAddress();
        if (thrift.isSetDbName()) {
            pojo.setDbName(thrift.getDbName());
        }
        if (thrift.isSetUrl()) {
            pojo.setUrl(thrift.getUrl());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.attachments.DatabaseAddress toThrift(DatabaseAddress pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.attachments.DatabaseAddress thrift = new org.eclipse.sw360.datahandler.thrift.attachments.DatabaseAddress();
        if (pojo.getDbName() != null) {
            thrift.setDbName(pojo.getDbName());
        }
        if (pojo.getUrl() != null) {
            thrift.setUrl(pojo.getUrl());
        }
        return thrift;
    }
}
