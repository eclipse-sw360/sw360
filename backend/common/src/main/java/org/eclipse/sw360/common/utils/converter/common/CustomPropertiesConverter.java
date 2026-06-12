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

import org.eclipse.sw360.datahandler.services.common.CustomProperties;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class CustomPropertiesConverter {

    private CustomPropertiesConverter() {}

    public static CustomProperties fromThrift(org.eclipse.sw360.datahandler.thrift.CustomProperties thrift) {
        if (thrift == null) {
            return null;
        }
        CustomProperties pojo = new CustomProperties();
        if (thrift.isSetDocumentType()) {
            pojo.setDocumentType(thrift.getDocumentType());
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetPropertyToValues()) {
            pojo.setPropertyToValues(ThriftCollectionConverter.mapMap(thrift.getPropertyToValues(), mapKey -> mapKey, mapValue -> ThriftCollectionConverter.mapSet(mapValue, e -> e)));
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.CustomProperties toThrift(CustomProperties pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.CustomProperties thrift = new org.eclipse.sw360.datahandler.thrift.CustomProperties();
        if (pojo.getDocumentType() != null) {
            thrift.setDocumentType(pojo.getDocumentType());
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getPropertyToValues() != null) {
            thrift.setPropertyToValues(ThriftCollectionConverter.mapMap(pojo.getPropertyToValues(), mapKey -> mapKey, mapValue -> ThriftCollectionConverter.mapSet(mapValue, e -> e)));
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        return thrift;
    }
}
