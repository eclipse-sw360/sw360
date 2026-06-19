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

import org.eclipse.sw360.datahandler.services.projects.ProjectProjectRelationship;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;

public final class ProjectProjectRelationshipConverter {

    private ProjectProjectRelationshipConverter() {}

    public static ProjectProjectRelationship fromThrift(org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship thrift) {
        if (thrift == null) {
            return null;
        }
        ProjectProjectRelationship pojo = new ProjectProjectRelationship();
        if (thrift.isSetEnableSvm()) {
            pojo.setEnableSvm(thrift.isEnableSvm());
        }
        if (thrift.isSetProjectRelationship()) {
            pojo.setProjectRelationship(EnumConverter.fromThrift(thrift.getProjectRelationship(), org.eclipse.sw360.datahandler.services.projects.ProjectRelationship.class));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship toThrift(ProjectProjectRelationship pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship thrift = new org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship();
        if (pojo.getEnableSvm() != null) {
            thrift.setEnableSvm(pojo.getEnableSvm());
        }
        if (pojo.getProjectRelationship() != null) {
            thrift.setProjectRelationship(EnumConverter.toThrift(pojo.getProjectRelationship(), org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship.class));
        }
        return thrift;
    }
}
