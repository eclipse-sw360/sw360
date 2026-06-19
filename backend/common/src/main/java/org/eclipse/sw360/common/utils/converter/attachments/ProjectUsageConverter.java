/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.attachments;

import org.eclipse.sw360.datahandler.services.attachments.ProjectUsage;

public final class ProjectUsageConverter {

    private ProjectUsageConverter() {}

    public static ProjectUsage fromThrift(org.eclipse.sw360.datahandler.thrift.attachments.ProjectUsage thrift) {
        if (thrift == null) {
            return null;
        }
        ProjectUsage pojo = new ProjectUsage();
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetProjectId()) {
            pojo.setProjectId(thrift.getProjectId());
        }
        if (thrift.isSetProjectName()) {
            pojo.setProjectName(thrift.getProjectName());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.attachments.ProjectUsage toThrift(ProjectUsage pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.attachments.ProjectUsage thrift = new org.eclipse.sw360.datahandler.thrift.attachments.ProjectUsage();
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getProjectId() != null) {
            thrift.setProjectId(pojo.getProjectId());
        }
        if (pojo.getProjectName() != null) {
            thrift.setProjectName(pojo.getProjectName());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        return thrift;
    }
}
