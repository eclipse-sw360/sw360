/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.vmcomponents;

import org.eclipse.sw360.datahandler.services.vmcomponents.VMProcessReporting;

public final class VMProcessReportingConverter {

    private VMProcessReportingConverter() {}

    public static VMProcessReporting fromThrift(org.eclipse.sw360.datahandler.thrift.vmcomponents.VMProcessReporting thrift) {
        if (thrift == null) {
            return null;
        }
        VMProcessReporting pojo = new VMProcessReporting();
        if (thrift.isSetCompleted()) {
            pojo.setCompleted(thrift.getCompleted());
        }
        if (thrift.isSetElementType()) {
            pojo.setElementType(thrift.getElementType());
        }
        if (thrift.isSetEndDate()) {
            pojo.setEndDate(thrift.getEndDate());
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetIdsReceived()) {
            pojo.setIdsReceived(thrift.getIdsReceived());
        }
        if (thrift.isSetKnownReceived()) {
            pojo.setKnownReceived(thrift.getKnownReceived());
        }
        if (thrift.isSetNewReceived()) {
            pojo.setNewReceived(thrift.getNewReceived());
        }
        if (thrift.isSetProcessingSeconds()) {
            pojo.setProcessingSeconds(thrift.getProcessingSeconds());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetStartDate()) {
            pojo.setStartDate(thrift.getStartDate());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.vmcomponents.VMProcessReporting toThrift(VMProcessReporting pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.vmcomponents.VMProcessReporting thrift = new org.eclipse.sw360.datahandler.thrift.vmcomponents.VMProcessReporting();
        if (pojo.getCompleted() != null) {
            thrift.setCompleted(pojo.getCompleted());
        }
        if (pojo.getElementType() != null) {
            thrift.setElementType(pojo.getElementType());
        }
        if (pojo.getEndDate() != null) {
            thrift.setEndDate(pojo.getEndDate());
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getIdsReceived() != null) {
            thrift.setIdsReceived(pojo.getIdsReceived());
        }
        if (pojo.getKnownReceived() != null) {
            thrift.setKnownReceived(pojo.getKnownReceived());
        }
        if (pojo.getNewReceived() != null) {
            thrift.setNewReceived(pojo.getNewReceived());
        }
        if (pojo.getProcessingSeconds() != null) {
            thrift.setProcessingSeconds(pojo.getProcessingSeconds());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getStartDate() != null) {
            thrift.setStartDate(pojo.getStartDate());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        return thrift;
    }
}
