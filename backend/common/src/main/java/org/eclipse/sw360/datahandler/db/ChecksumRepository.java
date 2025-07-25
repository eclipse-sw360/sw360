/*
 * Copyright Ritankar Saha <ritankar.saha786@gmail.com>. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import com.ibm.cloud.cloudant.v1.model.PostViewOptions;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Repository for checksum-based operations on attachments
 */
public class ChecksumRepository extends DatabaseRepositoryCloudantClient<Attachment> {
    
    private static final String BY_MD5_VIEW_NAME = "function(doc) { if (doc.type == 'project' || doc.type == 'component' || doc.type == 'release') { " +
            "for(var i in doc.attachments) { " +
            "emit(doc.attachments[i].md5, doc.attachments[i]); } } }";
    
    private static final String BY_SHA256_VIEW_NAME = "function(doc) { if (doc.type == 'project' || doc.type == 'component' || doc.type == 'release') { " +
            "for(var i in doc.attachments) { " +
            "emit(doc.attachments[i].sha256, doc.attachments[i]); } } }";
    
    private static final String BY_FOSSOLOGY_UPLOAD_ID_VIEW_NAME = "function(doc) { if (doc.type == 'project' || doc.type == 'component' || doc.type == 'release') { " +
            "for(var i in doc.attachments) { " +
            "if(doc.attachments[i].fossologyUploadId) { " +
            "emit(doc.attachments[i].fossologyUploadId, doc.attachments[i]); } } } }";
    
    private static final String BY_CHECKSUM_TYPE_VIEW_NAME = "function(doc) { if (doc.type == 'project' || doc.type == 'component' || doc.type == 'release') { " +
            "for(var i in doc.attachments) { " +
            "if(doc.attachments[i].sha1) emit(['sha1', doc.attachments[i].sha1], doc.attachments[i]); " +
            "if(doc.attachments[i].md5) emit(['md5', doc.attachments[i].md5], doc.attachments[i]); " +
            "if(doc.attachments[i].sha256) emit(['sha256', doc.attachments[i].sha256], doc.attachments[i]); " +
            "} } }";

    public ChecksumRepository(DatabaseConnectorCloudant db) {
        super(db, Attachment.class);
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("bymd5", createMapReduce(BY_MD5_VIEW_NAME, null));
        views.put("bysha256", createMapReduce(BY_SHA256_VIEW_NAME, null));
        views.put("byfossologyuploadid", createMapReduce(BY_FOSSOLOGY_UPLOAD_ID_VIEW_NAME, null));
        views.put("bychecksumtype", createMapReduce(BY_CHECKSUM_TYPE_VIEW_NAME, null));
        initStandardDesignDocument(views, db);
    }

    /**
     * Get attachments by MD5 checksums
     */
    public List<Attachment> getAttachmentsByMd5s(@NotNull Set<String> md5s) {
        PostViewOptions viewQuery = getConnector()
                .getPostViewQueryBuilder(Attachment.class, "bymd5")
                .includeDocs(false)
                .keys(md5s.stream().map(r -> (Object)r).toList())
                .build();
        return queryViewForAttachment(viewQuery);
    }

    /**
     * Get attachments by SHA-256 checksums
     */
    public List<Attachment> getAttachmentsBySha256s(@NotNull Set<String> sha256s) {
        PostViewOptions viewQuery = getConnector()
                .getPostViewQueryBuilder(Attachment.class, "bysha256")
                .includeDocs(false)
                .keys(sha256s.stream().map(r -> (Object)r).toList())
                .build();
        return queryViewForAttachment(viewQuery);
    }

    /**
     * Get attachments by FOSSology upload IDs
     */
    public List<Attachment> getAttachmentsByFossologyUploadIds(@NotNull Set<String> fossologyUploadIds) {
        PostViewOptions viewQuery = getConnector()
                .getPostViewQueryBuilder(Attachment.class, "byfossologyuploadid")
                .includeDocs(false)
                .keys(fossologyUploadIds.stream().map(r -> (Object)r).toList())
                .build();
        return queryViewForAttachment(viewQuery);
    }

    /**
     * Get attachments by checksum and type
     */
    public List<Attachment> getAttachmentsByChecksum(@NotNull String checksum, @NotNull String checksumType) {
        PostViewOptions viewQuery = getConnector()
                .getPostViewQueryBuilder(Attachment.class, "bychecksumtype")
                .includeDocs(false)
                .keys(List.of(List.of(checksumType.toLowerCase(), checksum)))
                .build();
        return queryViewForAttachment(viewQuery);
    }

    /**
     * Get FOSSology upload ID for a given checksum
     */
    public String getFossologyUploadIdByChecksum(@NotNull String checksum, @NotNull String checksumType) {
        List<Attachment> attachments = getAttachmentsByChecksum(checksum, checksumType);
        return attachments.stream()
                .filter(att -> att.getFossologyUploadId() != null)
                .map(Attachment::getFossologyUploadId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get all checksums for a given FOSSology upload ID
     */
    public Map<String, String> getChecksumsByFossologyUploadId(@NotNull String fossologyUploadId) {
        List<Attachment> attachments = getAttachmentsByFossologyUploadIds(Set.of(fossologyUploadId));
        if (attachments.isEmpty()) {
            return Map.of();
        }
        
        Attachment attachment = attachments.get(0);
        Map<String, String> checksums = new HashMap<>();
        
        if (attachment.getSha1() != null) {
            checksums.put("sha1", attachment.getSha1());
        }
        if (attachment.getMd5() != null) {
            checksums.put("md5", attachment.getMd5());
        }
        if (attachment.getSha256() != null) {
            checksums.put("sha256", attachment.getSha256());
        }
        
        return checksums;
    }

    /**
     * Find attachments that have checksums but no FOSSology upload ID
     */
    public List<Attachment> getAttachmentsWithoutFossologyUploadId() {
        
        return getAllAttachments().stream()
                .filter(att -> (att.getSha1() != null || att.getMd5() != null || att.getSha256() != null))
                .filter(att -> att.getFossologyUploadId() == null)
                .collect(Collectors.toList());
    }

    /**
     * Find duplicate checksums across different attachment types
     */
    public Map<String, List<Attachment>> findDuplicateChecksums(String checksumType) {
        List<Attachment> allAttachments = getAllAttachments();
        
        return allAttachments.stream()
                .filter(att -> getChecksumByType(att, checksumType) != null)
                .collect(Collectors.groupingBy(att -> getChecksumByType(att, checksumType)))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String getChecksumByType(Attachment attachment, String checksumType) {
        switch (checksumType.toLowerCase()) {
            case "sha1":
                return attachment.getSha1();
            case "md5":
                return attachment.getMd5();
            case "sha256":
                return attachment.getSha256();
            default:
                return null;
        }
    }

    private List<Attachment> getAllAttachments() {
        
        PostViewOptions viewQuery = getConnector()
                .getPostViewQueryBuilder(Attachment.class, "all")
                .includeDocs(false)
                .build();
        return queryViewForAttachment(viewQuery);
    }
}