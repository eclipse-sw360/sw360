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

import org.eclipse.sw360.datahandler.services.vmcomponents.VMMinPatchLevel;

public final class VMMinPatchLevelConverter {

    private VMMinPatchLevelConverter() {}

    public static VMMinPatchLevel fromThrift(org.eclipse.sw360.datahandler.thrift.vmcomponents.VMMinPatchLevel thrift) {
        if (thrift == null) {
            return null;
        }
        VMMinPatchLevel pojo = new VMMinPatchLevel();
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetPriority()) {
            pojo.setPriority(thrift.getPriority());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        if (thrift.isSetVersion()) {
            pojo.setVersion(thrift.getVersion());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.vmcomponents.VMMinPatchLevel toThrift(VMMinPatchLevel pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.vmcomponents.VMMinPatchLevel thrift = new org.eclipse.sw360.datahandler.thrift.vmcomponents.VMMinPatchLevel();
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getPriority() != null) {
            thrift.setPriority(pojo.getPriority());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        if (pojo.getVersion() != null) {
            thrift.setVersion(pojo.getVersion());
        }
        return thrift;
    }
}
