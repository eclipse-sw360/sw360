/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.common;

import org.eclipse.sw360.datahandler.services.common.ConfigContainer;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class ConfigContainerConverter {

    private ConfigContainerConverter() {}

    public static ConfigContainer fromThrift(org.eclipse.sw360.datahandler.thrift.ConfigContainer thrift) {
        if (thrift == null) {
            return null;
        }
        ConfigContainer pojo = new ConfigContainer();
        if (thrift.isSetConfigFor()) {
            pojo.setConfigFor(EnumConverter.fromThrift(thrift.getConfigFor(), org.eclipse.sw360.datahandler.services.common.ConfigFor.class));
        }
        if (thrift.isSetConfigKeyToValues()) {
            pojo.setConfigKeyToValues(ThriftCollectionConverter.mapMap(thrift.getConfigKeyToValues(), mapKey -> mapKey, mapValue -> ThriftCollectionConverter.mapSet(mapValue, e -> e)));
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.ConfigContainer toThrift(ConfigContainer pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.ConfigContainer thrift = new org.eclipse.sw360.datahandler.thrift.ConfigContainer();
        if (pojo.getConfigFor() != null) {
            thrift.setConfigFor(EnumConverter.toThrift(pojo.getConfigFor(), org.eclipse.sw360.datahandler.thrift.ConfigFor.class));
        }
        if (pojo.getConfigKeyToValues() != null) {
            thrift.setConfigKeyToValues(ThriftCollectionConverter.mapMap(pojo.getConfigKeyToValues(), mapKey -> mapKey, mapValue -> ThriftCollectionConverter.mapSet(mapValue, e -> e)));
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        return thrift;
    }
}
