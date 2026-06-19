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

import org.eclipse.sw360.datahandler.services.projects.ProjectData;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class ProjectDataConverter {

    private ProjectDataConverter() {}

    public static ProjectData fromThrift(org.eclipse.sw360.datahandler.thrift.projects.ProjectData thrift) {
        if (thrift == null) {
            return null;
        }
        ProjectData pojo = new ProjectData();
        if (thrift.isSetFirst250Projects()) {
            pojo.setFirst250Projects(ThriftCollectionConverter.mapList(thrift.getFirst250Projects(), e -> org.eclipse.sw360.common.utils.converter.projects.ProjectConverter.fromThrift(e)));
        }
        if (thrift.isSetProjectIdsOfRemainingProject()) {
            pojo.setProjectIdsOfRemainingProject(ThriftCollectionConverter.mapList(thrift.getProjectIdsOfRemainingProject(), e -> e));
        }
        if (thrift.isSetTotalNumberOfProjects()) {
            pojo.setTotalNumberOfProjects(thrift.getTotalNumberOfProjects());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.projects.ProjectData toThrift(ProjectData pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.projects.ProjectData thrift = new org.eclipse.sw360.datahandler.thrift.projects.ProjectData();
        if (pojo.getFirst250Projects() != null) {
            thrift.setFirst250Projects(ThriftCollectionConverter.mapList(pojo.getFirst250Projects(), e -> org.eclipse.sw360.common.utils.converter.projects.ProjectConverter.toThrift(e)));
        }
        if (pojo.getProjectIdsOfRemainingProject() != null) {
            thrift.setProjectIdsOfRemainingProject(ThriftCollectionConverter.mapList(pojo.getProjectIdsOfRemainingProject(), e -> e));
        }
        if (pojo.getTotalNumberOfProjects() != null) {
            thrift.setTotalNumberOfProjects(pojo.getTotalNumberOfProjects());
        }
        return thrift;
    }
}
