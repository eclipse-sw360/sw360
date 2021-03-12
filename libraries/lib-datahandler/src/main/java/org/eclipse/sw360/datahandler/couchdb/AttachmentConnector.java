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

import com.cloudant.client.api.CloudantClient;
import com.google.common.collect.Sets;
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
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.datahandler.common.CommonUtils.closeQuietly;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyCollection;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotEmpty;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;

import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;

/**
 * Ektorp connector for uploading attachments
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
    public AttachmentConnector(Supplier<CloudantClient> httpClient, String dbName, Duration downloadTimeout) throws MalformedURLException {
        this(new DatabaseConnectorCloudant(httpClient, dbName), downloadTimeout);
    }

    /**
     * Update the database with new attachment metadata
     */
    public void updateAttachmentContent(AttachmentContent attachment) throws SW360Exception {
        connector.update(attachment);
    }

    /**
     * Get attachment metadata from attachmentId
     */
    public AttachmentContent getAttachmentContent(String attachmentContentId) throws SW360Exception {
        assertNotEmpty(attachmentContentId);

        return connector.get(AttachmentContent.class, attachmentContentId);
    }

    public void deleteAttachment(String id) {
        connector.deleteById(AttachmentContent.class, id);
    }

    public void deleteAttachments(Collection<Attachment> attachments) {
        Set<String> attachmentContentIds = getAttachmentContentIds(attachments);
        deleteAttachmentsByIds(attachmentContentIds);
    }

    private void deleteAttachmentsByIds(Collection<String> attachmentContentIds) {
        connector.deleteIds(AttachmentContent.class, attachmentContentIds);
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
        boolean duplicateSha1 = attachments.parallelStream().collect(Collectors.groupingBy(Attachment::getSha1)).size() < attachments.size();
        boolean duplicateFileName = attachments.parallelStream().collect(Collectors.groupingBy(Attachment::getFilename)).size() < attachments.size();
        return (duplicateSha1 || duplicateFileName);
    }
}
