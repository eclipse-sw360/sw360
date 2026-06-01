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

import org.eclipse.sw360.datahandler.services.components.ExternalToolProcessStep;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;

public final class ExternalToolProcessStepConverter {

    private ExternalToolProcessStepConverter() {}

    public static ExternalToolProcessStep fromThrift(org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcessStep thrift) {
        if (thrift == null) {
            return null;
        }
        ExternalToolProcessStep pojo = new ExternalToolProcessStep();
        if (thrift.isSetFinishedOn()) {
            pojo.setFinishedOn(thrift.getFinishedOn());
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetLinkToStep()) {
            pojo.setLinkToStep(thrift.getLinkToStep());
        }
        if (thrift.isSetProcessStepIdInTool()) {
            pojo.setProcessStepIdInTool(thrift.getProcessStepIdInTool());
        }
        if (thrift.isSetResult()) {
            pojo.setResult(thrift.getResult());
        }
        if (thrift.isSetStartedBy()) {
            pojo.setStartedBy(thrift.getStartedBy());
        }
        if (thrift.isSetStartedByGroup()) {
            pojo.setStartedByGroup(thrift.getStartedByGroup());
        }
        if (thrift.isSetStartedOn()) {
            pojo.setStartedOn(thrift.getStartedOn());
        }
        if (thrift.isSetStepName()) {
            pojo.setStepName(thrift.getStepName());
        }
        if (thrift.isSetStepStatus()) {
            pojo.setStepStatus(EnumConverter.fromThrift(thrift.getStepStatus(), org.eclipse.sw360.datahandler.services.components.ExternalToolProcessStatus.class));
        }
        if (thrift.isSetUserCredentialsInTool()) {
            pojo.setUserCredentialsInTool(thrift.getUserCredentialsInTool());
        }
        if (thrift.isSetUserGroupInTool()) {
            pojo.setUserGroupInTool(thrift.getUserGroupInTool());
        }
        if (thrift.isSetUserIdInTool()) {
            pojo.setUserIdInTool(thrift.getUserIdInTool());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcessStep toThrift(ExternalToolProcessStep pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcessStep thrift = new org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcessStep();
        if (pojo.getFinishedOn() != null) {
            thrift.setFinishedOn(pojo.getFinishedOn());
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getLinkToStep() != null) {
            thrift.setLinkToStep(pojo.getLinkToStep());
        }
        if (pojo.getProcessStepIdInTool() != null) {
            thrift.setProcessStepIdInTool(pojo.getProcessStepIdInTool());
        }
        if (pojo.getResult() != null) {
            thrift.setResult(pojo.getResult());
        }
        if (pojo.getStartedBy() != null) {
            thrift.setStartedBy(pojo.getStartedBy());
        }
        if (pojo.getStartedByGroup() != null) {
            thrift.setStartedByGroup(pojo.getStartedByGroup());
        }
        if (pojo.getStartedOn() != null) {
            thrift.setStartedOn(pojo.getStartedOn());
        }
        if (pojo.getStepName() != null) {
            thrift.setStepName(pojo.getStepName());
        }
        if (pojo.getStepStatus() != null) {
            thrift.setStepStatus(EnumConverter.toThrift(pojo.getStepStatus(), org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcessStatus.class));
        }
        if (pojo.getUserCredentialsInTool() != null) {
            thrift.setUserCredentialsInTool(pojo.getUserCredentialsInTool());
        }
        if (pojo.getUserGroupInTool() != null) {
            thrift.setUserGroupInTool(pojo.getUserGroupInTool());
        }
        if (pojo.getUserIdInTool() != null) {
            thrift.setUserIdInTool(pojo.getUserIdInTool());
        }
        return thrift;
    }
}
