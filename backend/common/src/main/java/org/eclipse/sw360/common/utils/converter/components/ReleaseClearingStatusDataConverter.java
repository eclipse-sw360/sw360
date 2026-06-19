/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.components;

import org.eclipse.sw360.datahandler.services.components.ReleaseClearingStatusData;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;

public final class ReleaseClearingStatusDataConverter {

    private ReleaseClearingStatusDataConverter() {}

    public static ReleaseClearingStatusData fromThrift(org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStatusData thrift) {
        if (thrift == null) {
            return null;
        }
        ReleaseClearingStatusData pojo = new ReleaseClearingStatusData();
        if (thrift.isSetAccessible()) {
            pojo.setAccessible(thrift.isAccessible());
        }
        if (thrift.isSetComponentType()) {
            pojo.setComponentType(EnumConverter.fromThrift(thrift.getComponentType(), org.eclipse.sw360.datahandler.services.components.ComponentType.class));
        }
        if (thrift.isSetMainlineStates()) {
            pojo.setMainlineStates(thrift.getMainlineStates());
        }
        if (thrift.isSetProjectNames()) {
            pojo.setProjectNames(thrift.getProjectNames());
        }
        if (thrift.isSetRelease()) {
            pojo.setRelease(org.eclipse.sw360.common.utils.converter.components.ReleaseConverter.fromThrift(thrift.getRelease()));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStatusData toThrift(ReleaseClearingStatusData pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStatusData thrift = new org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStatusData();
        if (pojo.getAccessible() != null) {
            thrift.setAccessible(pojo.getAccessible());
        }
        if (pojo.getComponentType() != null) {
            thrift.setComponentType(EnumConverter.toThrift(pojo.getComponentType(), org.eclipse.sw360.datahandler.thrift.components.ComponentType.class));
        }
        if (pojo.getMainlineStates() != null) {
            thrift.setMainlineStates(pojo.getMainlineStates());
        }
        if (pojo.getProjectNames() != null) {
            thrift.setProjectNames(pojo.getProjectNames());
        }
        if (pojo.getRelease() != null) {
            thrift.setRelease(org.eclipse.sw360.common.utils.converter.components.ReleaseConverter.toThrift(pojo.getRelease()));
        }
        return thrift;
    }
}
