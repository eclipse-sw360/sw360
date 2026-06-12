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

import org.eclipse.sw360.datahandler.services.components.ExternalToolProcess;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class ExternalToolProcessConverter {

    private ExternalToolProcessConverter() {}

    public static ExternalToolProcess fromThrift(org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcess thrift) {
        if (thrift == null) {
            return null;
        }
        ExternalToolProcess pojo = new ExternalToolProcess();
        if (thrift.isSetAttachmentHash()) {
            pojo.setAttachmentHash(thrift.getAttachmentHash());
        }
        if (thrift.isSetAttachmentId()) {
            pojo.setAttachmentId(thrift.getAttachmentId());
        }
        if (thrift.isSetExternalTool()) {
            pojo.setExternalTool(EnumConverter.fromThrift(thrift.getExternalTool(), org.eclipse.sw360.datahandler.services.components.ExternalTool.class));
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetProcessIdInTool()) {
            pojo.setProcessIdInTool(thrift.getProcessIdInTool());
        }
        if (thrift.isSetProcessStatus()) {
            pojo.setProcessStatus(EnumConverter.fromThrift(thrift.getProcessStatus(), org.eclipse.sw360.datahandler.services.components.ExternalToolProcessStatus.class));
        }
        if (thrift.isSetProcessSteps()) {
            pojo.setProcessSteps(ThriftCollectionConverter.mapList(thrift.getProcessSteps(), e -> org.eclipse.sw360.common.utils.converter.components.ExternalToolProcessStepConverter.fromThrift(e)));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcess toThrift(ExternalToolProcess pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcess thrift = new org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcess();
        if (pojo.getAttachmentHash() != null) {
            thrift.setAttachmentHash(pojo.getAttachmentHash());
        }
        if (pojo.getAttachmentId() != null) {
            thrift.setAttachmentId(pojo.getAttachmentId());
        }
        if (pojo.getExternalTool() != null) {
            thrift.setExternalTool(EnumConverter.toThrift(pojo.getExternalTool(), org.eclipse.sw360.datahandler.thrift.components.ExternalTool.class));
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getProcessIdInTool() != null) {
            thrift.setProcessIdInTool(pojo.getProcessIdInTool());
        }
        if (pojo.getProcessStatus() != null) {
            thrift.setProcessStatus(EnumConverter.toThrift(pojo.getProcessStatus(), org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcessStatus.class));
        }
        if (pojo.getProcessSteps() != null) {
            thrift.setProcessSteps(ThriftCollectionConverter.mapList(pojo.getProcessSteps(), e -> org.eclipse.sw360.common.utils.converter.components.ExternalToolProcessStepConverter.toThrift(e)));
        }
        return thrift;
    }
}
