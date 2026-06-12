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

import org.eclipse.sw360.datahandler.services.common.ProjectPackageRelationship;

public final class ProjectPackageRelationshipConverter {

    private ProjectPackageRelationshipConverter() {}

    public static ProjectPackageRelationship fromThrift(org.eclipse.sw360.datahandler.thrift.ProjectPackageRelationship thrift) {
        if (thrift == null) {
            return null;
        }
        ProjectPackageRelationship pojo = new ProjectPackageRelationship();
        if (thrift.isSetComment()) {
            pojo.setComment(thrift.getComment());
        }
        if (thrift.isSetCreatedBy()) {
            pojo.setCreatedBy(thrift.getCreatedBy());
        }
        if (thrift.isSetCreatedOn()) {
            pojo.setCreatedOn(thrift.getCreatedOn());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.ProjectPackageRelationship toThrift(ProjectPackageRelationship pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.ProjectPackageRelationship thrift = new org.eclipse.sw360.datahandler.thrift.ProjectPackageRelationship();
        if (pojo.getComment() != null) {
            thrift.setComment(pojo.getComment());
        }
        if (pojo.getCreatedBy() != null) {
            thrift.setCreatedBy(pojo.getCreatedBy());
        }
        if (pojo.getCreatedOn() != null) {
            thrift.setCreatedOn(pojo.getCreatedOn());
        }
        return thrift;
    }
}
