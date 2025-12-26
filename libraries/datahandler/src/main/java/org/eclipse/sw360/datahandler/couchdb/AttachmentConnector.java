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
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.datahandler.common.CommonUtils.closeQuietly;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyCollection;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotEmpty;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;

import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.attachments.ChecksumService;

import java.util.Map;

/**
 * Connector for uploading attachments
 *
 * @author cedric.bodet@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class AttachmentConnector extends AttachmentStreamConnector {

    private static Logger log = LogManager.getLogger(AttachmentConnector.class);
    private final ChecksumService checksumService = new ChecksumService();

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

    /**
     * Compute all supported checksums for an attachment
     */
    public Map<ChecksumService.ChecksumType, String> getAllChecksumsFromAttachmentContentId(String attachmentContentId) {
        InputStream attachmentStream = null;
        try {
            AttachmentContent attachmentContent = getAttachmentContent(attachmentContentId);
            attachmentStream = readAttachmentStream(attachmentContent);
            return checksumService.computeAllChecksums(attachmentStream);
        } catch (SW360Exception e) {
            log.error("Problem retrieving content of attachment", e);
            return Map.of();
        } finally {
            closeQuietly(attachmentStream, log);
        }
    }

    /**
     * Compute MD5 checksum for an attachment
     */
    public String getMd5FromAttachmentContentId(String attachmentContentId) {
        InputStream attachmentStream = null;
        try {
            AttachmentContent attachmentContent = getAttachmentContent(attachmentContentId);
            attachmentStream = readAttachmentStream(attachmentContent);
            return checksumService.computeChecksum(attachmentStream, ChecksumService.ChecksumType.MD5);
        } catch (SW360Exception e) {
            log.error("Problem retrieving content of attachment or computing MD5", e);
            return "";
        } finally {
            closeQuietly(attachmentStream, log);
        }
    }

    /**
     * Compute SHA-256 checksum for an attachment
     */
    public String getSha256FromAttachmentContentId(String attachmentContentId) {
        InputStream attachmentStream = null;
        try {
            AttachmentContent attachmentContent = getAttachmentContent(attachmentContentId);
            attachmentStream = readAttachmentStream(attachmentContent);
            return checksumService.computeChecksum(attachmentStream, ChecksumService.ChecksumType.SHA256);
        } catch (SW360Exception e) {
            log.error("Problem retrieving content of attachment or computing SHA-256", e);
            return "";
        } finally {
            closeQuietly(attachmentStream, log);
        }
    }

    /**
     * Set all checksums for a set of attachments
     */
    public void setAllChecksumsForAttachments(Set<Attachment> attachments) {
        for (Attachment attachment : attachments) {
            setAllChecksumsForAttachment(attachment);
        }
    }

    /**
     * Set all checksums for a single attachment
     */
    public void setAllChecksumsForAttachment(Attachment attachment) {
        String attachmentContentId = attachment.getAttachmentContentId();
        Map<ChecksumService.ChecksumType, String> checksums = getAllChecksumsFromAttachmentContentId(attachmentContentId);
        
        // Set checksums if they're not already present
        if (isNullOrEmpty(attachment.getSha1())) {
            attachment.setSha1(checksums.get(ChecksumService.ChecksumType.SHA1));
        }
        if (isNullOrEmpty(attachment.getMd5())) {
            attachment.setMd5(checksums.get(ChecksumService.ChecksumType.MD5));
        }
        if (isNullOrEmpty(attachment.getSha256())) {
            attachment.setSha256(checksums.get(ChecksumService.ChecksumType.SHA256));
        }
    }

    /**
     * Validate attachment checksums against expected values
     */
    public boolean validateAttachmentChecksums(Attachment attachment) {
        String attachmentContentId = attachment.getAttachmentContentId();
        Map<ChecksumService.ChecksumType, String> computedChecksums = getAllChecksumsFromAttachmentContentId(attachmentContentId);
        
        boolean isValid = true;
        
        // Validate SHA1 if present
        if (!isNullOrEmpty(attachment.getSha1())) {
            String computedSha1 = computedChecksums.get(ChecksumService.ChecksumType.SHA1);
            if (!checksumService.validateChecksum(computedSha1, attachment.getSha1(), ChecksumService.ChecksumType.SHA1)) {
                isValid = false;
            }
        }
        
        // Validate MD5 if present
        if (!isNullOrEmpty(attachment.getMd5())) {
            String computedMd5 = computedChecksums.get(ChecksumService.ChecksumType.MD5);
            if (!checksumService.validateChecksum(computedMd5, attachment.getMd5(), ChecksumService.ChecksumType.MD5)) {
                isValid = false;
            }
        }
        
        // Validate SHA256 if present
        if (!isNullOrEmpty(attachment.getSha256())) {
            String computedSha256 = computedChecksums.get(ChecksumService.ChecksumType.SHA256);
            if (!checksumService.validateChecksum(computedSha256, attachment.getSha256(), ChecksumService.ChecksumType.SHA256)) {
                isValid = false;
            }
        }
        
        return isValid;
    }

    public static boolean isDuplicateAttachment(Set<Attachment> attachments) {
        boolean duplicateSha1 = attachments.parallelStream().collect(Collectors.groupingBy(Attachment::getSha1)).size() < attachments.size();
        boolean duplicateFileName = attachments.parallelStream().collect(Collectors.groupingBy(Attachment::getFilename)).size() < attachments.size();
        return (duplicateSha1 || duplicateFileName);
    }

    /**
     * Set FOSSology upload ID for attachment
     */
    public void setFossologyUploadIdForAttachment(String attachmentContentId, String fossologyUploadId) {
        try {
            // Validate that the attachment exists
            getAttachmentContent(attachmentContentId);
            
            log.info("Setting FOSSology upload ID {} for attachment {}", fossologyUploadId, attachmentContentId);
        } catch (SW360Exception e) {
            log.error("Error setting FOSSology upload ID for attachment {}", attachmentContentId, e);
        }
    }

    /**
     * Get attachments by checksum (any supported type)
     */
    public Set<Attachment> getAttachmentsByChecksum(String checksum, String checksumType) {

        // returning empty set, depends on CouchDB View Setup, need to migrate scripts
        log.info("Searching for attachments with {} checksum: {}", checksumType, checksum);
        return Set.of();
    }

    /**
     * Update attachment with FOSSology upload ID based on checksum match
     */
    public void mapChecksumToFossologyUploadId(Set<Attachment> attachments, String checksum, String checksumType, String fossologyUploadId) {
        for (Attachment attachment : attachments) {
            boolean checksumMatches = false;
            
            switch (checksumType.toLowerCase()) {
                case "sha1":
                    checksumMatches = checksum.equals(attachment.getSha1());
                    break;
                case "md5":
                    checksumMatches = checksum.equals(attachment.getMd5());
                    break;
                case "sha256":
                    checksumMatches = checksum.equals(attachment.getSha256());
                    break;
                default:
                    log.warn("Unsupported checksum type: {}", checksumType);
                    continue;
            }
            
            if (checksumMatches) {
                attachment.setFossologyUploadId(fossologyUploadId);
                log.info("Mapped {} checksum {} to FOSSology upload ID {} for attachment {}", 
                    checksumType, checksum, fossologyUploadId, attachment.getAttachmentContentId());
            }
        }
    }

    /**
     * Find attachments with matching checksums for FOSSology processing
     */
    public Set<Attachment> findAttachmentsForFossologyProcessing(Set<Attachment> attachments) {
        return attachments.stream()
            .filter(attachment -> !isNullOrEmpty(attachment.getSha1()) || 
                                !isNullOrEmpty(attachment.getMd5()) || 
                                !isNullOrEmpty(attachment.getSha256()))
            .filter(attachment -> isNullOrEmpty(attachment.getFossologyUploadId()))
            .collect(Collectors.toSet());
    }

    /**
     * Validate and update attachment checksums, ensuring FOSSology upload ID mapping
     */
    public boolean validateAndUpdateAttachmentWithFossologyMapping(Attachment attachment, String fossologyUploadId) {
        // First validate existing checksums
        boolean isValid = validateAttachmentChecksums(attachment);
        
        if (isValid) {
            // Set checksums if not present
            setAllChecksumsForAttachment(attachment);
            
            // Map to FOSSology upload ID
            if (!isNullOrEmpty(fossologyUploadId)) {
                attachment.setFossologyUploadId(fossologyUploadId);
                log.info("Mapped attachment {} to FOSSology upload ID {}", 
                    attachment.getAttachmentContentId(), fossologyUploadId);
            }
        }
        
        return isValid;
    }
}
