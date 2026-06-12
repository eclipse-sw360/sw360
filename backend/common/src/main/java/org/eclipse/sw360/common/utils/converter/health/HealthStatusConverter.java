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

import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.datahandler.services.health.HealthStatus;

public final class HealthStatusConverter {

    private HealthStatusConverter() {}

    public static HealthStatus fromThrift(org.eclipse.sw360.datahandler.thrift.health.Status thrift) {
        return EnumConverter.fromThrift(thrift, HealthStatus.class);
    }

    public static org.eclipse.sw360.datahandler.thrift.health.Status toThrift(HealthStatus pojo) {
        return EnumConverter.toThrift(pojo, org.eclipse.sw360.datahandler.thrift.health.Status.class);
    }
}
