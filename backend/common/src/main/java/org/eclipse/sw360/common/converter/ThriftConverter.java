/*
 *  Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 * 
 *  This program and the accompanying materials are made
 *  available under the terms of the Eclipse Public License 2.0
 *  which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 *  SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.common.converter;

import org.eclipse.sw360.datahandler.services.common.ConfigContainer;
import org.eclipse.sw360.datahandler.services.common.ConfigFor;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;

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
}
