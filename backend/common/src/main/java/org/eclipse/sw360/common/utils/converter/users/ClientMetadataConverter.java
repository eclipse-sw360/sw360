/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.users;

import org.eclipse.sw360.datahandler.services.users.ClientMetadata;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;

public final class ClientMetadataConverter {

    private ClientMetadataConverter() {}

    public static ClientMetadata fromThrift(org.eclipse.sw360.datahandler.thrift.users.ClientMetadata thrift) {
        if (thrift == null) {
            return null;
        }
        ClientMetadata pojo = new ClientMetadata();
        if (thrift.isSetAccess()) {
            pojo.setAccess(EnumConverter.fromThrift(thrift.getAccess(), org.eclipse.sw360.datahandler.services.users.UserAccess.class));
        }
        if (thrift.isSetName()) {
            pojo.setName(thrift.getName());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.users.ClientMetadata toThrift(ClientMetadata pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.users.ClientMetadata thrift = new org.eclipse.sw360.datahandler.thrift.users.ClientMetadata();
        if (pojo.getAccess() != null) {
            thrift.setAccess(EnumConverter.toThrift(pojo.getAccess(), org.eclipse.sw360.datahandler.thrift.users.UserAccess.class));
        }
        if (pojo.getName() != null) {
            thrift.setName(pojo.getName());
        }
        return thrift;
    }
}
