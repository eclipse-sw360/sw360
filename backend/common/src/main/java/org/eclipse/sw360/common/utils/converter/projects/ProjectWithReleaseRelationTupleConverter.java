/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.projects;

import org.eclipse.sw360.datahandler.services.projects.ProjectWithReleaseRelationTuple;

public final class ProjectWithReleaseRelationTupleConverter {

    private ProjectWithReleaseRelationTupleConverter() {}

    public static ProjectWithReleaseRelationTuple fromThrift(org.eclipse.sw360.datahandler.thrift.projects.ProjectWithReleaseRelationTuple thrift) {
        if (thrift == null) {
            return null;
        }
        ProjectWithReleaseRelationTuple pojo = new ProjectWithReleaseRelationTuple();
        if (thrift.isSetProject()) {
            pojo.setProject(org.eclipse.sw360.common.utils.converter.projects.ProjectConverter.fromThrift(thrift.getProject()));
        }
        if (thrift.isSetRelation()) {
            pojo.setRelation(org.eclipse.sw360.common.utils.converter.common.ProjectReleaseRelationshipConverter.fromThrift(thrift.getRelation()));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.projects.ProjectWithReleaseRelationTuple toThrift(ProjectWithReleaseRelationTuple pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.projects.ProjectWithReleaseRelationTuple thrift = new org.eclipse.sw360.datahandler.thrift.projects.ProjectWithReleaseRelationTuple();
        if (pojo.getProject() != null) {
            thrift.setProject(org.eclipse.sw360.common.utils.converter.projects.ProjectConverter.toThrift(pojo.getProject()));
        }
        if (pojo.getRelation() != null) {
            thrift.setRelation(org.eclipse.sw360.common.utils.converter.common.ProjectReleaseRelationshipConverter.toThrift(pojo.getRelation()));
        }
        return thrift;
    }
}
