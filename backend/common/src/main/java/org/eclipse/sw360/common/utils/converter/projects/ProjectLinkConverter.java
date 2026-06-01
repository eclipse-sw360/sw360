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

import org.eclipse.sw360.datahandler.services.projects.ProjectLink;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class ProjectLinkConverter {

    private ProjectLinkConverter() {}

    public static ProjectLink fromThrift(org.eclipse.sw360.datahandler.thrift.projects.ProjectLink thrift) {
        if (thrift == null) {
            return null;
        }
        ProjectLink pojo = new ProjectLink();
        if (thrift.isSetClearingState()) {
            pojo.setClearingState(EnumConverter.fromThrift(thrift.getClearingState(), org.eclipse.sw360.datahandler.services.projects.ProjectClearingState.class));
        }
        if (thrift.isSetEnableSvm()) {
            pojo.setEnableSvm(thrift.isEnableSvm());
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetLinkedReleases()) {
            pojo.setLinkedReleases(ThriftCollectionConverter.mapList(thrift.getLinkedReleases(), e -> org.eclipse.sw360.common.utils.converter.components.ReleaseLinkConverter.fromThrift(e)));
        }
        if (thrift.isSetName()) {
            pojo.setName(thrift.getName());
        }
        if (thrift.isSetNodeId()) {
            pojo.setNodeId(thrift.getNodeId());
        }
        if (thrift.isSetParentNodeId()) {
            pojo.setParentNodeId(thrift.getParentNodeId());
        }
        if (thrift.isSetProjectType()) {
            pojo.setProjectType(EnumConverter.fromThrift(thrift.getProjectType(), org.eclipse.sw360.datahandler.services.projects.ProjectType.class));
        }
        if (thrift.isSetRelation()) {
            pojo.setRelation(EnumConverter.fromThrift(thrift.getRelation(), org.eclipse.sw360.datahandler.services.projects.ProjectRelationship.class));
        }
        if (thrift.isSetState()) {
            pojo.setState(EnumConverter.fromThrift(thrift.getState(), org.eclipse.sw360.datahandler.services.projects.ProjectState.class));
        }
        if (thrift.isSetSubprojects()) {
            pojo.setSubprojects(ThriftCollectionConverter.mapList(thrift.getSubprojects(), e -> org.eclipse.sw360.common.utils.converter.projects.ProjectLinkConverter.fromThrift(e)));
        }
        if (thrift.isSetTreeLevel()) {
            pojo.setTreeLevel(thrift.getTreeLevel());
        }
        if (thrift.isSetVersion()) {
            pojo.setVersion(thrift.getVersion());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.projects.ProjectLink toThrift(ProjectLink pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.projects.ProjectLink thrift = new org.eclipse.sw360.datahandler.thrift.projects.ProjectLink();
        if (pojo.getClearingState() != null) {
            thrift.setClearingState(EnumConverter.toThrift(pojo.getClearingState(), org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState.class));
        }
        if (pojo.getEnableSvm() != null) {
            thrift.setEnableSvm(pojo.getEnableSvm());
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getLinkedReleases() != null) {
            thrift.setLinkedReleases(ThriftCollectionConverter.mapList(pojo.getLinkedReleases(), e -> org.eclipse.sw360.common.utils.converter.components.ReleaseLinkConverter.toThrift(e)));
        }
        if (pojo.getName() != null) {
            thrift.setName(pojo.getName());
        }
        if (pojo.getNodeId() != null) {
            thrift.setNodeId(pojo.getNodeId());
        }
        if (pojo.getParentNodeId() != null) {
            thrift.setParentNodeId(pojo.getParentNodeId());
        }
        if (pojo.getProjectType() != null) {
            thrift.setProjectType(EnumConverter.toThrift(pojo.getProjectType(), org.eclipse.sw360.datahandler.thrift.projects.ProjectType.class));
        }
        if (pojo.getRelation() != null) {
            thrift.setRelation(EnumConverter.toThrift(pojo.getRelation(), org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship.class));
        }
        if (pojo.getState() != null) {
            thrift.setState(EnumConverter.toThrift(pojo.getState(), org.eclipse.sw360.datahandler.thrift.projects.ProjectState.class));
        }
        if (pojo.getSubprojects() != null) {
            thrift.setSubprojects(ThriftCollectionConverter.mapList(pojo.getSubprojects(), e -> org.eclipse.sw360.common.utils.converter.projects.ProjectLinkConverter.toThrift(e)));
        }
        if (pojo.getTreeLevel() != null) {
            thrift.setTreeLevel(pojo.getTreeLevel());
        }
        if (pojo.getVersion() != null) {
            thrift.setVersion(pojo.getVersion());
        }
        return thrift;
    }
}
