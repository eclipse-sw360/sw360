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

import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;
import org.eclipse.sw360.datahandler.services.attachments.ProjectAttachmentUsage;

import java.util.ArrayList;
import java.util.HashSet;

public final class ProjectAttachmentUsageConverter {

    private ProjectAttachmentUsageConverter() {}

    public static ProjectAttachmentUsage fromThrift(
            org.eclipse.sw360.datahandler.thrift.attachments.ProjectAttachmentUsage thrift) {
        if (thrift == null) {
            return null;
        }
        ProjectAttachmentUsage pojo = new ProjectAttachmentUsage();
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        if (thrift.isSetVisible()) {
            pojo.setVisible(thrift.getVisible() != 0);
        }
        if (thrift.isSetRestricted()) {
            pojo.setRestricted(thrift.getRestricted() != 0);
        }
        if (thrift.isSetProjectUsages()) {
            pojo.setProjectUsages(new ArrayList<>(
                    ThriftCollectionConverter.mapSet(thrift.getProjectUsages(), ProjectUsageConverter::fromThrift)));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.attachments.ProjectAttachmentUsage toThrift(
            ProjectAttachmentUsage pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.attachments.ProjectAttachmentUsage thrift =
                new org.eclipse.sw360.datahandler.thrift.attachments.ProjectAttachmentUsage();
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        if (pojo.getVisible() != null) {
            thrift.setVisible(pojo.getVisible() ? 1L : 0L);
        }
        if (pojo.getRestricted() != null) {
            thrift.setRestricted(pojo.getRestricted() ? 1L : 0L);
        }
        if (pojo.getProjectUsages() != null) {
            thrift.setProjectUsages(new HashSet<>(
                    ThriftCollectionConverter.mapList(pojo.getProjectUsages(), ProjectUsageConverter::toThrift)));
        }
        return thrift;
    }
}
