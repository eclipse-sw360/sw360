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

import org.eclipse.sw360.datahandler.services.components.BulkOperationNode;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class BulkOperationNodeConverter {

    private BulkOperationNodeConverter() {}

    public static BulkOperationNode fromThrift(org.eclipse.sw360.datahandler.thrift.components.BulkOperationNode thrift) {
        if (thrift == null) {
            return null;
        }
        BulkOperationNode pojo = new BulkOperationNode();
        if (thrift.isSetAdditionalData()) {
            pojo.setAdditionalData(thrift.getAdditionalData());
        }
        if (thrift.isSetChildList()) {
            pojo.setChildList(ThriftCollectionConverter.mapList(thrift.getChildList(), e -> org.eclipse.sw360.common.utils.converter.components.BulkOperationNodeConverter.fromThrift(e)));
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetName()) {
            pojo.setName(thrift.getName());
        }
        if (thrift.isSetParentId()) {
            pojo.setParentId(thrift.getParentId());
        }
        if (thrift.isSetState()) {
            pojo.setState(EnumConverter.fromThrift(thrift.getState(), org.eclipse.sw360.datahandler.services.components.BulkOperationResultState.class));
        }
        if (thrift.isSetType()) {
            pojo.setType(EnumConverter.fromThrift(thrift.getType(), org.eclipse.sw360.datahandler.services.components.BulkOperationNodeType.class));
        }
        if (thrift.isSetVersion()) {
            pojo.setVersion(thrift.getVersion());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.components.BulkOperationNode toThrift(BulkOperationNode pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.components.BulkOperationNode thrift = new org.eclipse.sw360.datahandler.thrift.components.BulkOperationNode();
        if (pojo.getAdditionalData() != null) {
            thrift.setAdditionalData(pojo.getAdditionalData());
        }
        if (pojo.getChildList() != null) {
            thrift.setChildList(ThriftCollectionConverter.mapList(pojo.getChildList(), e -> org.eclipse.sw360.common.utils.converter.components.BulkOperationNodeConverter.toThrift(e)));
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getName() != null) {
            thrift.setName(pojo.getName());
        }
        if (pojo.getParentId() != null) {
            thrift.setParentId(pojo.getParentId());
        }
        if (pojo.getState() != null) {
            thrift.setState(EnumConverter.toThrift(pojo.getState(), org.eclipse.sw360.datahandler.thrift.components.BulkOperationResultState.class));
        }
        if (pojo.getType() != null) {
            thrift.setType(EnumConverter.toThrift(pojo.getType(), org.eclipse.sw360.datahandler.thrift.components.BulkOperationNodeType.class));
        }
        if (pojo.getVersion() != null) {
            thrift.setVersion(pojo.getVersion());
        }
        return thrift;
    }
}
