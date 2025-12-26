/*
 * Copyright Ritankar Saha <ritankar.saha786@gmail.com>. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.db.ChecksumRepository;
import org.eclipse.sw360.datahandler.db.ComponentRepository;
import org.eclipse.sw360.datahandler.db.ProjectRepository;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for checksum and FOSSology upload ID operations
 */
public class ChecksumService {
    
    private static final Logger log = LogManager.getLogger(ChecksumService.class);
    
    private final ChecksumRepository checksumRepository;
    private final ComponentRepository componentRepository;
    private final ProjectRepository projectRepository;
    
    public ChecksumService(ChecksumRepository checksumRepository, 
                          ComponentRepository componentRepository, 
                          ProjectRepository projectRepository) {
        this.checksumRepository = checksumRepository;
        this.componentRepository = componentRepository;
        this.projectRepository = projectRepository;
    }
    
    /**
     * Find all attachments with a specific checksum across all entities
     */
    public Map<String, List<Attachment>> findAttachmentsByChecksum(String checksum, String checksumType, User user) {
        Map<String, List<Attachment>> result = new HashMap<>();
        
        // Search in components
        Set<Component> components = componentRepository.getComponentsByAttachmentChecksum(checksum, checksumType);
        if (!components.isEmpty()) {
            List<Attachment> componentAttachments = components.stream()
                    .filter(comp -> comp.getAttachments() != null)
                    .flatMap(comp -> comp.getAttachments().stream())
                    .filter(att -> checksumMatches(att, checksum, checksumType))
                    .collect(Collectors.toList());
            if (!componentAttachments.isEmpty()) {
                result.put("components", componentAttachments);
            }
        }
        
        // Search in projects
        Set<Project> projects = projectRepository.getProjectsByAttachmentChecksum(checksum, checksumType, user);
        if (!projects.isEmpty()) {
            List<Attachment> projectAttachments = projects.stream()
                    .filter(proj -> proj.getAttachments() != null)
                    .flatMap(proj -> proj.getAttachments().stream())
                    .filter(att -> checksumMatches(att, checksum, checksumType))
                    .collect(Collectors.toList());
            if (!projectAttachments.isEmpty()) {
                result.put("projects", projectAttachments);
            }
        }
        
        return result;
    }
    
    /**
     * Map checksum to FOSSology upload ID across all entities
     */
    public void mapChecksumToFossologyUploadId(String checksum, String checksumType, String fossologyUploadId, User user) {
        log.info("Mapping {} checksum {} to FOSSology upload ID {}", checksumType, checksum, fossologyUploadId);
        
        Map<String, List<Attachment>> attachmentsByEntity = findAttachmentsByChecksum(checksum, checksumType, user);
        
        int mappedCount = 0;
        for (Map.Entry<String, List<Attachment>> entry : attachmentsByEntity.entrySet()) {
            for (Attachment attachment : entry.getValue()) {
                if (checksumMatches(attachment, checksum, checksumType)) {
                    attachment.setFossologyUploadId(fossologyUploadId);
                    mappedCount++;
                    log.debug("Mapped attachment {} to FOSSology upload ID {}", 
                        attachment.getAttachmentContentId(), fossologyUploadId);
                }
            }
        }
        
        log.info("Successfully mapped {} attachments with {} checksum {} to FOSSology upload ID {}", 
            mappedCount, checksumType, checksum, fossologyUploadId);
    }
    
    /**
     * Get FOSSology upload ID for a given checksum
     */
    public String getFossologyUploadIdByChecksum(String checksum, String checksumType) {
        return checksumRepository.getFossologyUploadIdByChecksum(checksum, checksumType);
    }
    
    /**
     * Get all checksums for a given FOSSology upload ID
     */
    public Map<String, String> getChecksumsByFossologyUploadId(String fossologyUploadId) {
        return checksumRepository.getChecksumsByFossologyUploadId(fossologyUploadId);
    }
    
    /**
     * Find all entities with unprocessed attachments (have checksums but no FOSSology upload ID)
     */
    public Map<String, Object> getEntitiesWithUnprocessedAttachments(User user) {
        Map<String, Object> result = new HashMap<>();
        
        // Get components with unprocessed attachments
        List<Component> componentsWithUnprocessed = componentRepository.getComponentsWithUnprocessedAttachments();
        if (!componentsWithUnprocessed.isEmpty()) {
            result.put("components", componentsWithUnprocessed);
        }
        
        // Get projects with unprocessed attachments
        List<Project> projectsWithUnprocessed = projectRepository.getProjectsWithUnprocessedAttachments(user);
        if (!projectsWithUnprocessed.isEmpty()) {
            result.put("projects", projectsWithUnprocessed);
        }
        
        return result;
    }
    
    /**
     * Find duplicate checksums across the system
     */
    public Map<String, Map<String, List<Attachment>>> findDuplicateChecksums(String checksumType) {
        Map<String, List<Attachment>> duplicates = checksumRepository.findDuplicateChecksums(checksumType);
        Map<String, Map<String, List<Attachment>>> result = new HashMap<>();
        
        for (Map.Entry<String, List<Attachment>> entry : duplicates.entrySet()) {
            String checksum = entry.getKey();
            List<Attachment> attachments = entry.getValue();
            
            Map<String, List<Attachment>> entitiesWithDuplicate = new HashMap<>();
            
            // returning all duplicates under a single key. 
            entitiesWithDuplicate.put("all", attachments);
            result.put(checksum, entitiesWithDuplicate);
        }
        
        return result;
    }
    
    /**
     * Validate checksum integrity across all entities
     */
    public Map<String, Object> validateChecksumIntegrity(User user) {
        Map<String, Object> report = new HashMap<>();
        
        // Find attachments without checksums
        Map<String, Object> unprocessedEntities = getEntitiesWithUnprocessedAttachments(user);
        if (!unprocessedEntities.isEmpty()) {
            report.put("unprocessedAttachments", unprocessedEntities);
        }
        
        // Find duplicate checksums
        for (String checksumType : Arrays.asList("sha1", "md5", "sha256")) {
            Map<String, Map<String, List<Attachment>>> duplicates = findDuplicateChecksums(checksumType);
            if (!duplicates.isEmpty()) {
                report.put("duplicates_" + checksumType, duplicates);
            }
        }
        
        return report;
    }
    
    /**
     * Batch process checksums for FOSSology integration
     */
    public Map<String, String> batchProcessChecksumsForFossology(Map<String, String> checksumToUploadIdMap, User user) {
        Map<String, String> processedResults = new HashMap<>();
        
        for (Map.Entry<String, String> entry : checksumToUploadIdMap.entrySet()) {
            String checksum = entry.getKey();
            String fossologyUploadId = entry.getValue();
            
            try {
                // Determine checksum type
                String checksumType = determineChecksumType(checksum);
                mapChecksumToFossologyUploadId(checksum, checksumType, fossologyUploadId, user);
                processedResults.put(checksum, "SUCCESS");
            } catch (Exception e) {
                log.error("Failed to process checksum {} to FOSSology upload ID {}", checksum, fossologyUploadId, e);
                processedResults.put(checksum, "FAILED: " + e.getMessage());
            }
        }
        
        return processedResults;
    }
    
    private boolean checksumMatches(Attachment attachment, String checksum, String checksumType) {
        switch (checksumType.toLowerCase()) {
            case "sha1":
                return checksum.equals(attachment.getSha1());
            case "md5":
                return checksum.equals(attachment.getMd5());
            case "sha256":
                return checksum.equals(attachment.getSha256());
            default:
                return false;
        }
    }
    
    private String determineChecksumType(String checksum) {
        if (checksum == null) return "unknown";
        
        switch (checksum.length()) {
            case 32:
                return "md5";
            case 40:
                return "sha1";
            case 64:
                return "sha256";
            default:
                return "unknown";
        }
    }
}