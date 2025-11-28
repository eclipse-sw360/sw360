/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.db;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.thrift.TBase;
import org.apache.thrift.TFieldIdEnum;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.projects.Project;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

public abstract class AttachmentAwareDatabaseHandler {

    protected AttachmentDatabaseHandler attachmentDatabaseHandler;

    protected AttachmentAwareDatabaseHandler(AttachmentDatabaseHandler attachmentDatabaseHandler) {
        this.attachmentDatabaseHandler = attachmentDatabaseHandler;
    }

    protected Source toSource(Release release){
        return Source.releaseId(release.getId());
    }

    protected Source toSource(Component component){
        return Source.releaseId(component.getId());
    }

    protected Source toSource(Project project){
        return Source.releaseId(project.getId());
    }

    public Set<Attachment> getAllAttachmentsToKeep(Source owner, Set<Attachment> originalAttachments, Set<Attachment> changedAttachments) {
        Map<String, Attachment> attachmentsToKeep = nullToEmptySet(changedAttachments).stream()
                .collect(Collectors.toMap(Attachment::getAttachmentContentId, a -> a));
        Set<Attachment> actualAttachments = nullToEmptySet(originalAttachments);

        // prevent deletion of already accepted attachments
        Set<Attachment> checkedActualAttachments = actualAttachments.stream()
                .filter(a -> (a.getCheckStatus() == CheckStatus.ACCEPTED)).collect(Collectors.toSet());

        checkedActualAttachments.forEach(a -> attachmentsToKeep.putIfAbsent(a.getAttachmentContentId(), a));

        // prevent deletion of used attachments
        Set<String> attachmentContentIds = actualAttachments.stream().map(Attachment::getAttachmentContentId).collect(Collectors.toSet());
        ImmutableMap<Source, Set<String>> usageSearchParameter = ImmutableMap.of(owner, attachmentContentIds);
        Map<Map<Source, String>, Integer> attachmentUsageCount = attachmentDatabaseHandler.getAttachmentUsageCount(usageSearchParameter, null);
        Set<Attachment> usedActualAttachments = actualAttachments.stream()
                .filter(attachment -> attachmentUsageCount.getOrDefault(ImmutableMap.of(owner, attachment.getAttachmentContentId()), 0) > 0)
                .collect(Collectors.toSet());

        usedActualAttachments.forEach(a -> attachmentsToKeep.putIfAbsent(a.getAttachmentContentId(), a));

        return new HashSet<>(attachmentsToKeep.values());
    }

    protected void deleteAttachmentUsagesOfUnlinkedReleases(Source usedBy, Set<String> updatedLinkedReleaseIds, Set<String> actualLinkedReleaseIds) throws SW360Exception {
        Sets.SetView<String> deletedLinkedReleaseIds = Sets.difference(actualLinkedReleaseIds, updatedLinkedReleaseIds);
        Set<Source> owners = deletedLinkedReleaseIds.stream().map(Source::releaseId).collect(Collectors.toSet());
        attachmentDatabaseHandler.deleteUsagesBy(usedBy, owners);
    }

    protected <T extends TBase<T, ? extends TFieldIdEnum>> void updateModifiedFields(T type, String userEmail) {
        if (type instanceof Release) {
            Release release = (Release) type;
            release.setModifiedBy(userEmail);
            release.setModifiedOn(SW360Utils.getCreatedOn());
        } else if (type instanceof Component) {
            Component component = (Component) type;
            component.setModifiedBy(userEmail);
            component.setModifiedOn(SW360Utils.getCreatedOn());
        } else if (type instanceof Project) {
            Project project = (Project) type;
            project.setModifiedBy(userEmail);
            project.setModifiedOn(SW360Utils.getCreatedOn());
        } else if (type instanceof Package) {
            Package pkg = (Package) type;
            pkg.setModifiedBy(userEmail);
            pkg.setModifiedOn(SW360Utils.getCreatedOn());
        }
    }
}
