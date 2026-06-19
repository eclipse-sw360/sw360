/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.common;

import org.eclipse.sw360.datahandler.services.common.ProjectReleaseRelationship;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;

public final class ProjectReleaseRelationshipConverter {

    private ProjectReleaseRelationshipConverter() {}

    public static ProjectReleaseRelationship fromThrift(org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship thrift) {
        if (thrift == null) {
            return null;
        }
        ProjectReleaseRelationship pojo = new ProjectReleaseRelationship();
        if (thrift.isSetComment()) {
            pojo.setComment(thrift.getComment());
        }
        if (thrift.isSetCreatedBy()) {
            pojo.setCreatedBy(thrift.getCreatedBy());
        }
        if (thrift.isSetCreatedOn()) {
            pojo.setCreatedOn(thrift.getCreatedOn());
        }
        if (thrift.isSetMainlineState()) {
            pojo.setMainlineState(EnumConverter.fromThrift(thrift.getMainlineState(), org.eclipse.sw360.datahandler.services.common.MainlineState.class));
        }
        if (thrift.isSetReleaseRelation()) {
            pojo.setReleaseRelation(EnumConverter.fromThrift(thrift.getReleaseRelation(), org.eclipse.sw360.datahandler.services.common.ReleaseRelationship.class));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship toThrift(ProjectReleaseRelationship pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship thrift = new org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship();
        if (pojo.getComment() != null) {
            thrift.setComment(pojo.getComment());
        }
        if (pojo.getCreatedBy() != null) {
            thrift.setCreatedBy(pojo.getCreatedBy());
        }
        if (pojo.getCreatedOn() != null) {
            thrift.setCreatedOn(pojo.getCreatedOn());
        }
        if (pojo.getMainlineState() != null) {
            thrift.setMainlineState(EnumConverter.toThrift(pojo.getMainlineState(), org.eclipse.sw360.datahandler.thrift.MainlineState.class));
        }
        if (pojo.getReleaseRelation() != null) {
            thrift.setReleaseRelation(EnumConverter.toThrift(pojo.getReleaseRelation(), org.eclipse.sw360.datahandler.thrift.ReleaseRelationship.class));
        }
        return thrift;
    }
}
