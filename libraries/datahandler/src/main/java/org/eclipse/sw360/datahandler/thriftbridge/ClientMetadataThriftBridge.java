/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.thriftbridge;

import org.eclipse.sw360.datahandler.services.users.ClientMetadata;
import org.eclipse.sw360.datahandler.services.users.UserAccess;

public final class ClientMetadataThriftBridge {

    private ClientMetadataThriftBridge() {}

    public static ClientMetadata toPojo(org.eclipse.sw360.datahandler.thrift.users.ClientMetadata thrift) {
        if (thrift == null) {
            return null;
        }
        ClientMetadata pojo = new ClientMetadata();
        pojo.setName(thrift.getName());
        if (thrift.isSetAccess()) {
            pojo.setAccess(UserAccess.valueOf(thrift.getAccess().name()));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.users.ClientMetadata toThrift(ClientMetadata pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.users.ClientMetadata thrift =
                new org.eclipse.sw360.datahandler.thrift.users.ClientMetadata();
        thrift.setName(pojo.getName());
        if (pojo.getAccess() != null) {
            thrift.setAccess(org.eclipse.sw360.datahandler.thrift.users.UserAccess.valueOf(pojo.getAccess().name()));
        }
        return thrift;
    }
}
