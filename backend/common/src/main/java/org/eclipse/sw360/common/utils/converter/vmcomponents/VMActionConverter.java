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

import org.eclipse.sw360.datahandler.services.vmcomponents.VMAction;

public final class VMActionConverter {

    private VMActionConverter() {}

    public static VMAction fromThrift(org.eclipse.sw360.datahandler.thrift.vmcomponents.VMAction thrift) {
        if (thrift == null) {
            return null;
        }
        VMAction pojo = new VMAction();
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetLastUpdateDate()) {
            pojo.setLastUpdateDate(thrift.getLastUpdateDate());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetText()) {
            pojo.setText(thrift.getText());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        if (thrift.isSetVmid()) {
            pojo.setVmid(thrift.getVmid());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.vmcomponents.VMAction toThrift(VMAction pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.vmcomponents.VMAction thrift = new org.eclipse.sw360.datahandler.thrift.vmcomponents.VMAction();
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getLastUpdateDate() != null) {
            thrift.setLastUpdateDate(pojo.getLastUpdateDate());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getText() != null) {
            thrift.setText(pojo.getText());
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
