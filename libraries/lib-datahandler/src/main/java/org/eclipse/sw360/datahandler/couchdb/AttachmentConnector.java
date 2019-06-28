/*
 * Copyright Siemens AG, 2014-2016. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.couchdb;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.Duration;
import org.eclipse.sw360.datahandler.common.SW360Assert;
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

import org.apache.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.ektorp.http.HttpClient;

/**
 * Ektorp connector for uploading attachments
 *
 * @author cedric.bodet@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class AttachmentConnector extends AttachmentStreamConnector {

    private static Logger log = Logger.getLogger(AttachmentConnector.class);

    public AttachmentConnector(DatabaseConnector connector, Duration downloadTimeout) {
        super(connector, downloadTimeout);
    }

    /**
     * @todo remove this mess of constructors and use dependency injection
     */
    public AttachmentConnector(Supplier<HttpClient> httpClient, String dbName, Duration downloadTimeout) throws MalformedURLException {
        this(new DatabaseConnector(httpClient.get(), dbName), downloadTimeout);
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
        SW360Assert.assertNotEmpty(attachmentContentId);

        return connector.get(AttachmentContent.class, attachmentContentId);
    }

    public void deleteAttachment(String id) {
        connector.deleteById(id);
    }

    public void deleteAttachments(Collection<Attachment> attachments) {
        Set<String> attachmentContentIds = getAttachmentContentIds(attachments);
        deleteAttachmentsByIds(attachmentContentIds);
    }

    private void deleteAttachmentsByIds(Collection<String> attachmentContentIds) {
        connector.deleteIds(attachmentContentIds, AttachmentContent.class);
    }

    private Set<String> getAttachmentContentIds(Collection<Attachment> attachments) {
        return CommonUtils.nullToEmptyCollection(attachments).stream()
                .map(Attachment::getAttachmentContentId)
                .collect(Collectors.toSet());
    }

    public void deleteAttachmentDifference(Set<Attachment> attachmentsBefore, Set<Attachment> attachmentsAfter) {
        // it is important to take the set difference between sets of ids, not of attachments themselves
        // otherwise, when `attachmentsAfter` contains the same attachment (with the same id), but with one field changed (e.g. sha1),
        // then they are considered unequal and the set difference will contain this attachment and therefore
        // deleteAttachments(Collection<Attachment>) will delete an attachment that is present in `attachmentsAfter`
        Set<Attachment> nonAcceptedAttachmentsBefore = CommonUtils.nullToEmptySet(attachmentsBefore).stream().filter(a -> a.getCheckStatus() != CheckStatus.ACCEPTED).collect(Collectors.toSet());
        deleteAttachmentsByIds(Sets.difference(getAttachmentContentIds(nonAcceptedAttachmentsBefore), getAttachmentContentIds(attachmentsAfter)));
    }

    public String getSha1FromAttachmentContentId(String attachmentContentId) {
        try {
            AttachmentContent attachmentContent = getAttachmentContent(attachmentContentId);
            try (InputStream attachmentStream = readAttachmentStream(attachmentContent)) {
                return DigestUtils.sha1Hex(attachmentStream);
            }
        } catch (SW360Exception e) {
            log.error("Problem retrieving content of attachment", e);
            return "";
        } catch (IOException e) {
            log.error("Problem computing the sha1 checksum", e);
            return "";
        }
    }

    public void setSha1ForAttachments(Set<Attachment> attachments){
        attachments.stream()
                .filter(attachment -> Strings.isNullOrEmpty(attachment.getSha1()))
                .forEach(attachment -> {
                    String sha1 = getSha1FromAttachmentContentId(attachment.getAttachmentContentId());
                    attachment.setSha1(sha1);
                });
    }

    public static boolean isDuplicateAttachment(Set<Attachment> attachments) {
        boolean duplicateSha1 = attachments.parallelStream()
                .collect(Collectors.groupingBy(Attachment::getSha1))
                .size() < attachments.size();
        boolean duplicateFileName = attachments.parallelStream()
                .collect(Collectors.groupingBy(Attachment::getFilename))
                .size() < attachments.size();
        return duplicateSha1 || duplicateFileName;
    }
}
