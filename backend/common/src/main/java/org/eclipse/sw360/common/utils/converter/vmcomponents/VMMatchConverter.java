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

import org.eclipse.sw360.datahandler.services.vmcomponents.VMMatch;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class VMMatchConverter {

    private VMMatchConverter() {}

    public static VMMatch fromThrift(org.eclipse.sw360.datahandler.thrift.vmcomponents.VMMatch thrift) {
        if (thrift == null) {
            return null;
        }
        VMMatch pojo = new VMMatch();
        if (thrift.isSetComponentName()) {
            pojo.setComponentName(thrift.getComponentName());
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetMatchTypes()) {
            pojo.setMatchTypes(ThriftCollectionConverter.mapSet(thrift.getMatchTypes(), e -> EnumConverter.fromThrift(e, org.eclipse.sw360.datahandler.services.vmcomponents.VMMatchType.class)));
        }
        if (thrift.isSetMatchTypesUI()) {
            pojo.setMatchTypesUI(thrift.getMatchTypesUI());
        }
        if (thrift.isSetReleaseCpe()) {
            pojo.setReleaseCpe(thrift.getReleaseCpe());
        }
        if (thrift.isSetReleaseId()) {
            pojo.setReleaseId(thrift.getReleaseId());
        }
        if (thrift.isSetReleaseSvmId()) {
            pojo.setReleaseSvmId(thrift.getReleaseSvmId());
        }
        if (thrift.isSetReleaseVersion()) {
            pojo.setReleaseVersion(thrift.getReleaseVersion());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetState()) {
            pojo.setState(EnumConverter.fromThrift(thrift.getState(), org.eclipse.sw360.datahandler.services.vmcomponents.VMMatchState.class));
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        if (thrift.isSetVendorName()) {
            pojo.setVendorName(thrift.getVendorName());
        }
        if (thrift.isSetVmComponentCpe()) {
            pojo.setVmComponentCpe(thrift.getVmComponentCpe());
        }
        if (thrift.isSetVmComponentId()) {
            pojo.setVmComponentId(thrift.getVmComponentId());
        }
        if (thrift.isSetVmComponentName()) {
            pojo.setVmComponentName(thrift.getVmComponentName());
        }
        if (thrift.isSetVmComponentVendor()) {
            pojo.setVmComponentVendor(thrift.getVmComponentVendor());
        }
        if (thrift.isSetVmComponentVersion()) {
            pojo.setVmComponentVersion(thrift.getVmComponentVersion());
        }
        if (thrift.isSetVmComponentVmid()) {
            pojo.setVmComponentVmid(thrift.getVmComponentVmid());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.vmcomponents.VMMatch toThrift(VMMatch pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.vmcomponents.VMMatch thrift = new org.eclipse.sw360.datahandler.thrift.vmcomponents.VMMatch();
        if (pojo.getComponentName() != null) {
            thrift.setComponentName(pojo.getComponentName());
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getMatchTypes() != null) {
            thrift.setMatchTypes(ThriftCollectionConverter.mapSet(pojo.getMatchTypes(), e -> EnumConverter.toThrift(e, org.eclipse.sw360.datahandler.thrift.vmcomponents.VMMatchType.class)));
        }
        if (pojo.getMatchTypesUI() != null) {
            thrift.setMatchTypesUI(pojo.getMatchTypesUI());
        }
        if (pojo.getReleaseCpe() != null) {
            thrift.setReleaseCpe(pojo.getReleaseCpe());
        }
        if (pojo.getReleaseId() != null) {
            thrift.setReleaseId(pojo.getReleaseId());
        }
        if (pojo.getReleaseSvmId() != null) {
            thrift.setReleaseSvmId(pojo.getReleaseSvmId());
        }
        if (pojo.getReleaseVersion() != null) {
            thrift.setReleaseVersion(pojo.getReleaseVersion());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getState() != null) {
            thrift.setState(EnumConverter.toThrift(pojo.getState(), org.eclipse.sw360.datahandler.thrift.vmcomponents.VMMatchState.class));
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        if (pojo.getVendorName() != null) {
            thrift.setVendorName(pojo.getVendorName());
        }
        if (pojo.getVmComponentCpe() != null) {
            thrift.setVmComponentCpe(pojo.getVmComponentCpe());
        }
        if (pojo.getVmComponentId() != null) {
            thrift.setVmComponentId(pojo.getVmComponentId());
        }
        if (pojo.getVmComponentName() != null) {
            thrift.setVmComponentName(pojo.getVmComponentName());
        }
        if (pojo.getVmComponentVendor() != null) {
            thrift.setVmComponentVendor(pojo.getVmComponentVendor());
        }
        if (pojo.getVmComponentVersion() != null) {
            thrift.setVmComponentVersion(pojo.getVmComponentVersion());
        }
        if (pojo.getVmComponentVmid() != null) {
            thrift.setVmComponentVmid(pojo.getVmComponentVmid());
        }
        return thrift;
    }
}
