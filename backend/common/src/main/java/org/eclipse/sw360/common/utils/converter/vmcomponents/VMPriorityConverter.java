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

import org.eclipse.sw360.datahandler.services.vmcomponents.VMPriority;

public final class VMPriorityConverter {

    private VMPriorityConverter() {}

    public static VMPriority fromThrift(org.eclipse.sw360.datahandler.thrift.vmcomponents.VMPriority thrift) {
        if (thrift == null) {
            return null;
        }
        VMPriority pojo = new VMPriority();
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetLastUpdateDate()) {
            pojo.setLastUpdateDate(thrift.getLastUpdateDate());
        }
        if (thrift.isSetLongText()) {
            pojo.setLongText(thrift.getLongText());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetShortText()) {
            pojo.setShortText(thrift.getShortText());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        if (thrift.isSetVmid()) {
            pojo.setVmid(thrift.getVmid());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.vmcomponents.VMPriority toThrift(VMPriority pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.vmcomponents.VMPriority thrift = new org.eclipse.sw360.datahandler.thrift.vmcomponents.VMPriority();
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getLastUpdateDate() != null) {
            thrift.setLastUpdateDate(pojo.getLastUpdateDate());
        }
        if (pojo.getLongText() != null) {
            thrift.setLongText(pojo.getLongText());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getShortText() != null) {
            thrift.setShortText(pojo.getShortText());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        if (pojo.getVmid() != null) {
            thrift.setVmid(pojo.getVmid());
        }
        return thrift;
    }
}
