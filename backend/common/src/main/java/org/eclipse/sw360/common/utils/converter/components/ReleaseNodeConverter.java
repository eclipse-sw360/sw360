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

import org.eclipse.sw360.datahandler.services.components.ReleaseNode;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class ReleaseNodeConverter {

    private ReleaseNodeConverter() {}

    public static ReleaseNode fromThrift(org.eclipse.sw360.datahandler.thrift.components.ReleaseNode thrift) {
        if (thrift == null) {
            return null;
        }
        ReleaseNode pojo = new ReleaseNode();
        if (thrift.isSetComment()) {
            pojo.setComment(thrift.getComment());
        }
        if (thrift.isSetComponentId()) {
            pojo.setComponentId(thrift.getComponentId());
        }
        if (thrift.isSetCreateBy()) {
            pojo.setCreateBy(thrift.getCreateBy());
        }
        if (thrift.isSetCreateOn()) {
            pojo.setCreateOn(thrift.getCreateOn());
        }
        if (thrift.isSetMainlineState()) {
            pojo.setMainlineState(thrift.getMainlineState());
        }
        if (thrift.isSetReleaseId()) {
            pojo.setReleaseId(thrift.getReleaseId());
        }
        if (thrift.isSetReleaseLink()) {
            pojo.setReleaseLink(ThriftCollectionConverter.mapList(thrift.getReleaseLink(), e -> org.eclipse.sw360.common.utils.converter.components.ReleaseNodeConverter.fromThrift(e)));
        }
        if (thrift.isSetReleaseName()) {
            pojo.setReleaseName(thrift.getReleaseName());
        }
        if (thrift.isSetReleaseRelationship()) {
            pojo.setReleaseRelationship(thrift.getReleaseRelationship());
        }
        if (thrift.isSetReleaseVersion()) {
            pojo.setReleaseVersion(thrift.getReleaseVersion());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.components.ReleaseNode toThrift(ReleaseNode pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.components.ReleaseNode thrift = new org.eclipse.sw360.datahandler.thrift.components.ReleaseNode();
        if (pojo.getComment() != null) {
            thrift.setComment(pojo.getComment());
        }
        if (pojo.getComponentId() != null) {
            thrift.setComponentId(pojo.getComponentId());
        }
        if (pojo.getCreateBy() != null) {
            thrift.setCreateBy(pojo.getCreateBy());
        }
        if (pojo.getCreateOn() != null) {
            thrift.setCreateOn(pojo.getCreateOn());
        }
        if (pojo.getMainlineState() != null) {
            thrift.setMainlineState(pojo.getMainlineState());
        }
        if (pojo.getReleaseId() != null) {
            thrift.setReleaseId(pojo.getReleaseId());
        }
        if (pojo.getReleaseLink() != null) {
            thrift.setReleaseLink(ThriftCollectionConverter.mapList(pojo.getReleaseLink(), e -> org.eclipse.sw360.common.utils.converter.components.ReleaseNodeConverter.toThrift(e)));
        }
        if (pojo.getReleaseName() != null) {
            thrift.setReleaseName(pojo.getReleaseName());
        }
        if (pojo.getReleaseRelationship() != null) {
            thrift.setReleaseRelationship(pojo.getReleaseRelationship());
        }
        if (pojo.getReleaseVersion() != null) {
            thrift.setReleaseVersion(pojo.getReleaseVersion());
        }
        return thrift;
    }
}
