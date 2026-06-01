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

import org.eclipse.sw360.datahandler.services.projects.UsedReleaseRelations;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class UsedReleaseRelationsConverter {

    private UsedReleaseRelationsConverter() {}

    public static UsedReleaseRelations fromThrift(org.eclipse.sw360.datahandler.thrift.projects.UsedReleaseRelations thrift) {
        if (thrift == null) {
            return null;
        }
        UsedReleaseRelations pojo = new UsedReleaseRelations();
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetProjectId()) {
            pojo.setProjectId(thrift.getProjectId());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        if (thrift.isSetUsedProjectRelations()) {
            pojo.setUsedProjectRelations(ThriftCollectionConverter.mapSet(thrift.getUsedProjectRelations(), e -> EnumConverter.fromThrift(e, org.eclipse.sw360.datahandler.services.projects.ProjectRelationship.class)));
        }
        if (thrift.isSetUsedReleaseRelations()) {
            pojo.setUsedReleaseRelations(ThriftCollectionConverter.mapSet(thrift.getUsedReleaseRelations(), e -> EnumConverter.fromThrift(e, org.eclipse.sw360.datahandler.services.common.ReleaseRelationship.class)));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.projects.UsedReleaseRelations toThrift(UsedReleaseRelations pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.projects.UsedReleaseRelations thrift = new org.eclipse.sw360.datahandler.thrift.projects.UsedReleaseRelations();
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getProjectId() != null) {
            thrift.setProjectId(pojo.getProjectId());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        if (pojo.getUsedProjectRelations() != null) {
            thrift.setUsedProjectRelations(ThriftCollectionConverter.mapSet(pojo.getUsedProjectRelations(), e -> EnumConverter.toThrift(e, org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship.class)));
        }
        if (pojo.getUsedReleaseRelations() != null) {
            thrift.setUsedReleaseRelations(ThriftCollectionConverter.mapSet(pojo.getUsedReleaseRelations(), e -> EnumConverter.toThrift(e, org.eclipse.sw360.datahandler.thrift.ReleaseRelationship.class)));
        }
        return thrift;
    }
}
