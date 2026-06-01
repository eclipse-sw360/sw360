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

import org.eclipse.sw360.datahandler.services.vmcomponents.VMComponent;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class VMComponentConverter {

    private VMComponentConverter() {}

    public static VMComponent fromThrift(org.eclipse.sw360.datahandler.thrift.vmcomponents.VMComponent thrift) {
        if (thrift == null) {
            return null;
        }
        VMComponent pojo = new VMComponent();
        if (thrift.isSetCpe()) {
            pojo.setCpe(thrift.getCpe());
        }
        if (thrift.isSetEolReached()) {
            pojo.setEolReached(thrift.isEolReached());
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetLastUpdateDate()) {
            pojo.setLastUpdateDate(thrift.getLastUpdateDate());
        }
        if (thrift.isSetMinPatchLevels()) {
            pojo.setMinPatchLevels(ThriftCollectionConverter.mapSet(thrift.getMinPatchLevels(), e -> org.eclipse.sw360.common.utils.converter.vmcomponents.VMMinPatchLevelConverter.fromThrift(e)));
        }
        if (thrift.isSetName()) {
            pojo.setName(thrift.getName());
        }
        if (thrift.isSetReceivedDate()) {
            pojo.setReceivedDate(thrift.getReceivedDate());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetSecurityUrl()) {
            pojo.setSecurityUrl(thrift.getSecurityUrl());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        if (thrift.isSetUrl()) {
            pojo.setUrl(thrift.getUrl());
        }
        if (thrift.isSetVendor()) {
            pojo.setVendor(thrift.getVendor());
        }
        if (thrift.isSetVersion()) {
            pojo.setVersion(thrift.getVersion());
        }
        if (thrift.isSetVmid()) {
            pojo.setVmid(thrift.getVmid());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.vmcomponents.VMComponent toThrift(VMComponent pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.vmcomponents.VMComponent thrift = new org.eclipse.sw360.datahandler.thrift.vmcomponents.VMComponent();
        if (pojo.getCpe() != null) {
            thrift.setCpe(pojo.getCpe());
        }
        if (pojo.getEolReached() != null) {
            thrift.setEolReached(pojo.getEolReached());
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getLastUpdateDate() != null) {
            thrift.setLastUpdateDate(pojo.getLastUpdateDate());
        }
        if (pojo.getMinPatchLevels() != null) {
            thrift.setMinPatchLevels(ThriftCollectionConverter.mapSet(pojo.getMinPatchLevels(), e -> org.eclipse.sw360.common.utils.converter.vmcomponents.VMMinPatchLevelConverter.toThrift(e)));
        }
        if (pojo.getName() != null) {
            thrift.setName(pojo.getName());
        }
        if (pojo.getReceivedDate() != null) {
            thrift.setReceivedDate(pojo.getReceivedDate());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getSecurityUrl() != null) {
            thrift.setSecurityUrl(pojo.getSecurityUrl());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        if (pojo.getUrl() != null) {
            thrift.setUrl(pojo.getUrl());
        }
        if (pojo.getVendor() != null) {
            thrift.setVendor(pojo.getVendor());
        }
        if (pojo.getVersion() != null) {
            thrift.setVersion(pojo.getVersion());
        }
        if (pojo.getVmid() != null) {
            thrift.setVmid(pojo.getVmid());
        }
        return thrift;
    }
}
