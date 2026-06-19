/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.common.utils.converter.health;

import org.eclipse.sw360.datahandler.services.health.HealthResponse;

public final class HealthResponseConverter {

    private HealthResponseConverter() {}

    public static HealthResponse fromThrift(org.eclipse.sw360.datahandler.thrift.health.Health thrift) {
        if (thrift == null) {
            return null;
        }
        HealthResponse pojo = new HealthResponse();
        if (thrift.isSetDetails()) {
            pojo.setDetails(thrift.getDetails());
        }
        if (thrift.isSetStatus()) {
            pojo.setStatus(HealthStatusConverter.fromThrift(thrift.getStatus()));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.health.Health toThrift(HealthResponse pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.health.Health thrift = new org.eclipse.sw360.datahandler.thrift.health.Health();
        if (pojo.getDetails() != null) {
            thrift.setDetails(pojo.getDetails());
        }
        if (pojo.getStatus() != null) {
            thrift.setStatus(HealthStatusConverter.toThrift(pojo.getStatus()));
        }
        return thrift;
    }
}
