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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.ektorp.http.HttpClient;

import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyCollection;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;

public abstract class AttachmentAwareDatabaseHandler {

    private static final String SEPARATOR = " -> ";
    protected AttachmentDatabaseHandler attachmentDatabaseHandler;

    protected AttachmentAwareDatabaseHandler(AttachmentDatabaseHandler attachmentDatabaseHandler) {
        this.attachmentDatabaseHandler = attachmentDatabaseHandler;
    }

    protected AttachmentAwareDatabaseHandler(Supplier<HttpClient> httpClient, String dbName, String attachmentDbName) throws MalformedURLException {
        this(new AttachmentDatabaseHandler(httpClient, dbName, attachmentDbName));
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

    public static <T, R> String getCyclicLinkedPath(T obj, R handler, User user) throws TException {
        Map<String, String> linkedPath = new LinkedHashMap<>();
        String firstElementFullName = null;
        String id = null;
        if (obj instanceof Project) {
            Project proj = (Project) obj;
            firstElementFullName = SW360Utils.printName(proj);
            id = proj.getId();
        } else if (obj instanceof Release) {
            Release release = (Release) obj;
            firstElementFullName = SW360Utils.printName(release);
            id = release.getId();
        }
        linkedPath.put(id, firstElementFullName);
        Object[] cyclicLinkPresenceAndLastElementInCycle = getCyclicLinkPresenceAndLastElementInCycle(obj, handler,
                user, linkedPath);
        String cyclicHierarchy = "";
        boolean isCyclicLinkPresent = (Boolean) cyclicLinkPresenceAndLastElementInCycle[0];
        if (isCyclicLinkPresent) {
            String[] arrayOfLinkedPath = linkedPath.values().toArray(new String[0]);
            String lastElementInCycle = (String) cyclicLinkPresenceAndLastElementInCycle[1];
            cyclicHierarchy = String.join(SEPARATOR, arrayOfLinkedPath);
            cyclicHierarchy = cyclicHierarchy.concat(SEPARATOR).concat(lastElementInCycle);
        }
        return cyclicHierarchy;
    }

    protected void deleteAttachmentUsagesOfUnlinkedReleases(Source usedBy, Set<String> updatedLinkedReleaseIds, Set<String> actualLinkedReleaseIds) throws SW360Exception {
        Sets.SetView<String> deletedLinkedReleaseIds = Sets.difference(actualLinkedReleaseIds, updatedLinkedReleaseIds);
        Set<Source> owners = deletedLinkedReleaseIds.stream().map(Source::releaseId).collect(Collectors.toSet());
        attachmentDatabaseHandler.deleteUsagesBy(usedBy, owners);
    }

    private static <T, R> Object[] getCyclicLinkPresenceAndLastElementInCycle(T obj, R handler, User user,
            Map<String, String> linkedPath) throws TException {
        Map linkedElementsMap = null;
        if (obj instanceof Project) {
            Project proj = (Project) obj;
            linkedElementsMap = proj.getLinkedProjects();
        } else if (obj instanceof Release) {
            Release release = (Release) obj;
            linkedElementsMap = release.getReleaseIdToRelationship();
        }

        if (linkedElementsMap != null) {
            Iterator<String> linkedElementIterator = linkedElementsMap.keySet().iterator();
            while (linkedElementIterator.hasNext()) {
                String linkedElementId = linkedElementIterator.next();
                T linkedElement = null;
                String elementFullName = null;
                if (handler instanceof ProjectDatabaseHandler) {
                    ProjectDatabaseHandler projDBHandler = (ProjectDatabaseHandler) handler;
                    Project project = projDBHandler.getProjectById(linkedElementId, user);
                    elementFullName = SW360Utils.printName(project);
                    linkedElement = (T) project;
                } else if (handler instanceof ComponentDatabaseHandler) {
                    ComponentDatabaseHandler compDBHandler = (ComponentDatabaseHandler) handler;
                    Release release = compDBHandler.getRelease(linkedElementId, user);
                    elementFullName = SW360Utils.printName(release);
                    linkedElement = (T) release;
                }

                if (linkedPath.containsKey(linkedElementId)) {
                    return new Object[] { Boolean.TRUE, elementFullName };
                }

                linkedPath.put(linkedElementId, elementFullName);
                Object[] cyclicLinkPresenceAndLastElementInCycle = getCyclicLinkPresenceAndLastElementInCycle(
                        linkedElement, handler, user, linkedPath);
                boolean isCyclicLinkPresent = (Boolean) cyclicLinkPresenceAndLastElementInCycle[0];

                if (isCyclicLinkPresent) {
                    return cyclicLinkPresenceAndLastElementInCycle;
                }
                linkedPath.remove(linkedElementId);
            }
        }
        return new Object[] { Boolean.FALSE, null };
    }
}
