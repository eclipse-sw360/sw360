/*
 * Copyright Siemens AG, 2014-2016. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.couchdb;

import com.google.common.collect.Sets;
import com.ibm.cloud.cloudant.v1.Cloudant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.Duration;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.datahandler.common.CommonUtils.closeQuietly;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyCollection;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotEmpty;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;

import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;

/**
 * Connector for uploading attachments
 *
 * @author cedric.bodet@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class AttachmentConnector extends AttachmentStreamConnector {

    private static Logger log = LogManager.getLogger(AttachmentConnector.class);

    public AttachmentConnector(DatabaseConnectorCloudant databaseConnectorCloudant, Duration downloadTimeout) {
        super(databaseConnectorCloudant, downloadTimeout);
    }

    /**
     * @todo remove this mess of constructors and use dependency injection
     */
    public AttachmentConnector(Cloudant client, String dbName, Duration downloadTimeout) throws MalformedURLException {
        this(new DatabaseConnectorCloudant(client, dbName), downloadTimeout);
    }

    /**
     * Get attachment metadata from attachmentId
     */
    public AttachmentContent getAttachmentContent(String attachmentContentId) throws SW360Exception {
        assertNotEmpty(attachmentContentId);

        return connector.get(AttachmentContent.class, attachmentContentId);
    }

    public void deleteAttachment(String id) {
        connector.deleteById(id);
    }

    public void deleteAttachments(Collection<Attachment> attachments) {
        Set<String> attachmentContentIds = getAttachmentContentIds(attachments);
        deleteAttachmentsByIds(attachmentContentIds);
    }

    public void deleteAttachmentsByIds(Collection<String> attachmentContentIds) {
        connector.deleteIds(attachmentContentIds);
    }

    public Set<String> getAttachmentContentIds(Collection<Attachment> attachments) {
        return nullToEmptyCollection(attachments).stream()
                .map(Attachment::getAttachmentContentId)
                .collect(Collectors.toSet());
    }

    public void deleteAttachmentDifference(Set<Attachment> attachmentsBefore, Set<Attachment> attachmentsAfter) {
        // it is important to take the set difference between sets of ids, not of attachments themselves
        // otherwise, when `attachmentsAfter` contains the same attachment (with the same id), but with one field changed (e.g. sha1),
        // then they are considered unequal and the set difference will contain this attachment and therefore
        // deleteAttachments(Collection<Attachment>) will delete an attachment that is present in `attachmentsAfter`
        deleteAttachmentsByIds(getAttachentContentIdsToBeDeleted(attachmentsBefore,attachmentsAfter));
    }

    public Set<String> getAttachentContentIdsToBeDeleted(Set<Attachment> attachmentsBefore,
            Set<Attachment> attachmentsAfter) {
        Set<Attachment> nonAcceptedAttachmentsBefore = nullToEmptySet(attachmentsBefore).stream()
                .filter(a -> a.getCheckStatus() != CheckStatus.ACCEPTED).collect(Collectors.toSet());
        return Sets.difference(getAttachmentContentIds(nonAcceptedAttachmentsBefore),
                getAttachmentContentIds(attachmentsAfter));
    }

    public String getSha1FromAttachmentContentId(String attachmentContentId) {
        InputStream attachmentStream = null;
        try {
            AttachmentContent attachmentContent = getAttachmentContent(attachmentContentId);
            attachmentStream = readAttachmentStream(attachmentContent);
            return sha1Hex(attachmentStream);
        } catch (SW360Exception e) {
            log.error("Problem retrieving content of attachment", e);
            return "";
        } catch (IOException e) {
            log.error("Problem computing the sha1 checksum", e);
            return "";
        } finally {
            closeQuietly(attachmentStream, log);
        }
    }

    public void setSha1ForAttachments(Set<Attachment> attachments){
        for(Attachment attachment : attachments){
            if(isNullOrEmpty(attachment.getSha1())){
                String sha1 = getSha1FromAttachmentContentId(attachment.getAttachmentContentId());
                attachment.setSha1(sha1);
            }
        }
    }

    public static boolean isDuplicateAttachment(Set<Attachment> attachments) {
        return introducesDuplicateAttachment(Collections.emptySet(), attachments);
    }

    /**
     * Checks whether the update introduces a duplicate attachment (same SHA1 or same filename).
     * Only attachments that are new or renamed compared to {@code existingAttachments} are validated,
     * so duplicates that already exist in the database (legacy data) do not block unrelated updates,
     * status changes or the removal of one of the duplicates.
     */
    public static boolean introducesDuplicateAttachment(Set<Attachment> existingAttachments,
                                                        Set<Attachment> updatedAttachments) {
        Set<Attachment> attachments = nullToEmptySet(updatedAttachments);
        Set<AttachmentIdentity> existingNames = nullToEmptySet(existingAttachments).stream()
                .map(AttachmentIdentity::of)
                .collect(Collectors.toSet());
        Set<String> existingContentIds = nullToEmptySet(existingAttachments).stream()
                .map(Attachment::getAttachmentContentId)
                .filter(id -> !isNullOrEmpty(id))
                .collect(Collectors.toSet());
        Map<String, Long> sha1Counts = countNonEmptyValues(attachments, Attachment::getSha1);
        Map<String, Long> filenameCounts = countNonEmptyValues(attachments, Attachment::getFilename);

        return attachments.stream()
                .anyMatch(attachment -> (!existingContentIds.contains(attachment.getAttachmentContentId())
                        && sha1Counts.getOrDefault(attachment.getSha1(), 0L) > 1)
                        || (!existingNames.contains(AttachmentIdentity.of(attachment))
                        && filenameCounts.getOrDefault(attachment.getFilename(), 0L) > 1));
    }

    private static Map<String, Long> countNonEmptyValues(Set<Attachment> attachments,
                                                         Function<Attachment, String> property) {
        return attachments.stream()
                .map(property)
                .filter(value -> !isNullOrEmpty(value))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    // Content is immutable for a content id; a rename must only be checked for filename conflicts.
    private record AttachmentIdentity(String attachmentContentId, String filename) {
        static AttachmentIdentity of(Attachment attachment) {
            return new AttachmentIdentity(attachment.getAttachmentContentId(), attachment.getFilename());
        }
    }
}
