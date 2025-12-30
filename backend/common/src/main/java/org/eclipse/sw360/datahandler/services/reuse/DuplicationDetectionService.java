/*
 * Copyright Ritankar Saha<ritankar.saha786@gmail.com>, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.services.reuse;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.db.ChecksumRepository;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for detecting duplicate files based on checksums.
 * Uses multiple checksum algorithms (MD5, SHA-1, SHA-256) to identify
 * identical file content across different attachments and releases.
 */
public class DuplicationDetectionService {

    private static final Logger log = LoggerFactory.getLogger(DuplicationDetectionService.class);

    private final ChecksumRepository checksumRepository;

    public DuplicationDetectionService(ChecksumRepository checksumRepository) {
        this.checksumRepository = checksumRepository;
    }

    /**
     * Detect duplicate files within a single release based on checksums
     * 
     * @param release Release to analyze for duplicates
     * @return Map of checksum -> List of duplicate attachments
     */
    public Map<String, List<Attachment>> detectDuplicatesInRelease(Release release) {
        Map<String, List<Attachment>> duplicates = new HashMap<>();
        
        if (release.getAttachments() == null || release.getAttachments().isEmpty()) {
            log.debug("No attachments found in release {}", release.getId());
            return duplicates;
        }
        
        log.info("Detecting duplicates in release {} with {} attachments", 
                 release.getId(), release.getAttachments().size());
        
        // Group attachments by SHA1 checksum
        Map<String, List<Attachment>> sha1Groups = release.getAttachments().stream()
                .filter(attachment -> !CommonUtils.isNullEmptyOrWhitespace(attachment.getSha1()))
                .collect(Collectors.groupingBy(Attachment::getSha1));
        
        // Find groups with more than one attachment (duplicates)
        for (Map.Entry<String, List<Attachment>> entry : sha1Groups.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicates.put(entry.getKey(), entry.getValue());
                log.debug("Found {} duplicates for SHA1: {}", entry.getValue().size(), entry.getKey());
            }
        }
        
        log.info("Found {} duplicate groups in release {}", duplicates.size(), release.getId());
        return duplicates;
    }

    /**
     * Detect duplicate files across multiple releases
     * 
     * @param releases List of releases to analyze
     * @return Map of checksum -> Map of releaseId -> List of attachments
     */
    public Map<String, Map<String, List<Attachment>>> detectDuplicatesAcrossReleases(List<Release> releases) {
        Map<String, Map<String, List<Attachment>>> duplicates = new HashMap<>();
        
        log.info("Detecting duplicates across {} releases", releases.size());
        
        // Collect all attachments with their release context
        Map<String, Map<String, List<Attachment>>> checksumToReleaseMap = new HashMap<>();
        
        for (Release release : releases) {
            if (release.getAttachments() != null) {
                for (Attachment attachment : release.getAttachments()) {
                    String sha1 = attachment.getSha1();
                    if (!CommonUtils.isNullEmptyOrWhitespace(sha1)) {
                        checksumToReleaseMap
                                .computeIfAbsent(sha1, k -> new HashMap<>())
                                .computeIfAbsent(release.getId(), k -> new ArrayList<>())
                                .add(attachment);
                    }
                }
            }
        }
        
        // Find checksums that appear in multiple releases or multiple times in same release
        for (Map.Entry<String, Map<String, List<Attachment>>> checksumEntry : checksumToReleaseMap.entrySet()) {
            String checksum = checksumEntry.getKey();
            Map<String, List<Attachment>> releaseMap = checksumEntry.getValue();
            
            // Check if appears in multiple releases or multiple times
            boolean isDuplicate = releaseMap.size() > 1 || 
                    releaseMap.values().stream().anyMatch(list -> list.size() > 1);
            
            if (isDuplicate) {
                duplicates.put(checksum, releaseMap);
            }
        }
        
        log.info("Found {} duplicate checksums across {} releases", duplicates.size(), releases.size());
        return duplicates;
    }

    /**
     * Find duplicate files using database checksum views
     * 
     * @param checksums List of checksums to search for
     * @return Map of checksum -> List of matching attachment information
     */
    public Map<String, List<Map<String, Object>>> findDuplicatesByChecksums(List<String> checksums) {
        Map<String, List<Map<String, Object>>> results = new HashMap<>();
        
        if (CommonUtils.isNullOrEmptyCollection(checksums)) {
            return results;
        }
        
        log.info("Finding duplicates for {} checksums using database views", checksums.size());
        
        try {
            // Search by SHA1 checksums
            for (String checksum : checksums) {
                if (CommonUtils.isNullEmptyOrWhitespace(checksum)) {
                    continue;
                }
                
                List<Map<String, Object>> matches = new ArrayList<>();
                
                // Search in checksum repository by checksum type
                String checksumType = validateChecksumFormat(checksum);
                if (checksumType != null) {
                    List<Attachment> attachmentMatches = checksumRepository.getAttachmentsByChecksum(checksum, checksumType);
                    for (Attachment attachment : attachmentMatches) {
                        Map<String, Object> match = createMatchInfo(attachment, checksumType, checksum);
                        matches.add(match);
                    }
                }
                
                if (!matches.isEmpty()) {
                    results.put(checksum, matches);
                    log.debug("Found {} matches for checksum: {}", matches.size(), checksum);
                }
            }
            
            log.info("Found duplicates for {} out of {} checksums", results.size(), checksums.size());
            
        } catch (Exception e) {
            log.error("Error finding duplicates by checksums: {}", e.getMessage(), e);
        }
        
        return results;
    }

    /**
     * Get duplication statistics for a set of releases
     * 
     * @param releases List of releases to analyze
     * @return Map containing duplication statistics
     */
    public Map<String, Object> getDuplicationStatistics(List<Release> releases) {
        Map<String, Object> statistics = new HashMap<>();
        
        try {
            log.info("Generating duplication statistics for {} releases", releases.size());
            
            int totalAttachments = 0;
            int uniqueFiles = 0;
            int duplicateFiles = 0;
            Set<String> allChecksums = new HashSet<>();
            Map<String, Integer> checksumCounts = new HashMap<>();
            
            // Collect all checksums and count occurrences
            for (Release release : releases) {
                if (release.getAttachments() != null) {
                    totalAttachments += release.getAttachments().size();
                    
                    for (Attachment attachment : release.getAttachments()) {
                        String sha1 = attachment.getSha1();
                        if (!CommonUtils.isNullEmptyOrWhitespace(sha1)) {
                            allChecksums.add(sha1);
                            checksumCounts.merge(sha1, 1, Integer::sum);
                        }
                    }
                }
            }
            
            // Analyze duplication
            for (Map.Entry<String, Integer> entry : checksumCounts.entrySet()) {
                if (entry.getValue() == 1) {
                    uniqueFiles++;
                } else {
                    duplicateFiles += entry.getValue();
                }
            }
            
            double duplicationPercentage = totalAttachments > 0 ? 
                    (double) duplicateFiles / totalAttachments * 100.0 : 0.0;
            
            statistics.put("totalAttachments", totalAttachments);
            statistics.put("uniqueFiles", uniqueFiles);
            statistics.put("duplicateFiles", duplicateFiles);
            statistics.put("uniqueChecksums", allChecksums.size());
            statistics.put("duplicationPercentage", Math.round(duplicationPercentage * 100.0) / 100.0);
            statistics.put("analyzedReleases", releases.size());
            
            // Find most duplicated files
            List<Map.Entry<String, Integer>> sortedDuplicates = checksumCounts.entrySet().stream()
                    .filter(entry -> entry.getValue() > 1)
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(10)
                    .collect(Collectors.toList());
            
            List<Map<String, Object>> topDuplicates = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : sortedDuplicates) {
                Map<String, Object> duplicate = new HashMap<>();
                duplicate.put("checksum", entry.getKey());
                duplicate.put("occurrences", entry.getValue());
                topDuplicates.add(duplicate);
            }
            statistics.put("topDuplicates", topDuplicates);
            
            log.info("Duplication statistics: {:.2f}% duplication ({} unique, {} duplicate files)", 
                     duplicationPercentage, uniqueFiles, duplicateFiles);
            
        } catch (Exception e) {
            log.error("Error generating duplication statistics: {}", e.getMessage(), e);
            statistics.put("error", "Failed to generate statistics: " + e.getMessage());
        }
        
        return statistics;
    }

    /**
     * Validate checksum format and type
     * 
     * @param checksum Checksum string to validate
     * @return Checksum type (md5, sha1, sha256) or null if invalid
     */
    public String validateChecksumFormat(String checksum) {
        if (CommonUtils.isNullEmptyOrWhitespace(checksum)) {
            return null;
        }
        
        // Remove any whitespace and convert to lowercase
        String cleanChecksum = checksum.trim().toLowerCase();
        
        // Validate format based on length and characters
        if (cleanChecksum.matches("^[a-f0-9]{32}$")) {
            return "md5";
        } else if (cleanChecksum.matches("^[a-f0-9]{40}$")) {
            return "sha1";
        } else if (cleanChecksum.matches("^[a-f0-9]{64}$")) {
            return "sha256";
        }
        
        return null;
    }

    /**
     * Find potential duplicates based on filename similarity and size
     * This is useful when checksums are not available
     * 
     * @param releases List of releases to analyze
     * @return Map of similarity key -> List of similar attachments
     */
    public Map<String, List<Attachment>> findPotentialDuplicatesByMetadata(List<Release> releases) {
        Map<String, List<Attachment>> potentialDuplicates = new HashMap<>();
        
        log.info("Finding potential duplicates by metadata for {} releases", releases.size());
        
        // Group by filename and size combination
        Map<String, List<Attachment>> metadataGroups = new HashMap<>();
        
        for (Release release : releases) {
            if (release.getAttachments() != null) {
                for (Attachment attachment : release.getAttachments()) {
                    String filename = attachment.getFilename();
                    
                    if (!CommonUtils.isNullEmptyOrWhitespace(filename)) {
                        // Create a similarity key based on filename and size
                        String similarityKey = createSimilarityKey(filename);
                        metadataGroups.computeIfAbsent(similarityKey, k -> new ArrayList<>()).add(attachment);
                    }
                }
            }
        }
        
        // Find groups with multiple attachments
        for (Map.Entry<String, List<Attachment>> entry : metadataGroups.entrySet()) {
            if (entry.getValue().size() > 1) {
                potentialDuplicates.put(entry.getKey(), entry.getValue());
            }
        }
        
        log.info("Found {} potential duplicate groups by metadata", potentialDuplicates.size());
        return potentialDuplicates;
    }

    private Map<String, Object> createMatchInfo(Attachment attachment, String checksumType, String checksumValue) {
        Map<String, Object> matchInfo = new HashMap<>();
        matchInfo.put("attachmentId", attachment.getAttachmentContentId());
        matchInfo.put("filename", attachment.getFilename());
        matchInfo.put("checksumType", checksumType);
        matchInfo.put("checksumValue", checksumValue);
        matchInfo.put("attachmentType", attachment.getAttachmentType());
        
        // Add other available checksums for cross-verification
        if (!CommonUtils.isNullEmptyOrWhitespace(attachment.getSha1())) {
            matchInfo.put("sha1", attachment.getSha1());
        }
        if (!CommonUtils.isNullEmptyOrWhitespace(attachment.getMd5())) {
            matchInfo.put("md5", attachment.getMd5());
        }
        if (!CommonUtils.isNullEmptyOrWhitespace(attachment.getSha256())) {
            matchInfo.put("sha256", attachment.getSha256());
        }
        
        return matchInfo;
    }

    private String createSimilarityKey(String filename) {
        // Normalize filename for similarity comparison
        String normalized = filename.toLowerCase();
        
        // Remove version numbers and common suffixes
        normalized = normalized.replaceAll("-\\d+\\.\\d+(\\.\\d+)?", "");
        normalized = normalized.replaceAll("\\.(tar\\.gz|tar\\.bz2|zip|tgz)$", "");
        
        // Extract base name
        int lastDot = normalized.lastIndexOf('.');
        if (lastDot > 0) {
            normalized = normalized.substring(0, lastDot);
        }
        
        return normalized;
    }
}