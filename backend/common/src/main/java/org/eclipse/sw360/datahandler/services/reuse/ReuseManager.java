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
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Central coordinator for reuse operations.
 * This service orchestrates duplicate detection, clearing decision reuse,
 * and reuse statistics across the SW360 platform.
 */
public class ReuseManager {

    private static final Logger log = LoggerFactory.getLogger(ReuseManager.class);

    private final DuplicationDetectionService duplicationDetectionService;
    private final ChecksumRepository checksumRepository;
    private final Executor reuseExecutor;

    // Configuration constants
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int MAX_CONCURRENT_OPERATIONS = 5;

    public ReuseManager(ChecksumRepository checksumRepository) {
        this.checksumRepository = checksumRepository;
        this.duplicationDetectionService = new DuplicationDetectionService(checksumRepository);
        this.reuseExecutor = Executors.newFixedThreadPool(MAX_CONCURRENT_OPERATIONS);
    }

    /**
     * Comprehensive reuse analysis for a release
     * 
     * @param release Release to analyze
     * @param user User performing the operation
     * @return Comprehensive reuse analysis results
     */
    public Map<String, Object> performReuseAnalysis(Release release, User user) {
        Map<String, Object> analysisResults = new HashMap<>();
        
        try {
            log.info("Starting comprehensive reuse analysis for release {}", release.getId());
            long startTime = System.currentTimeMillis();
            
            // Initialize result structure
            analysisResults.put("releaseId", release.getId());
            analysisResults.put("releaseName", release.getName());
            analysisResults.put("releaseVersion", release.getVersion());
            analysisResults.put("analysisStartTime", startTime);
            analysisResults.put("userId", user.getId());
            
            // Duplicate detection within release
            Map<String, List<Attachment>> internalDuplicates = duplicationDetectionService.detectDuplicatesInRelease(release);
            analysisResults.put("internalDuplicates", internalDuplicates);
            analysisResults.put("internalDuplicateCount", internalDuplicates.size());
            
            // External duplicate detection (files that exist in other releases)
            Map<String, Object> externalDuplicates = findExternalDuplicates(release);
            analysisResults.put("externalDuplicates", externalDuplicates);
            
            //  Reuse potential analysis
            Map<String, Object> reusePotential = analyzeReusePotential(release);
            analysisResults.put("reusePotential", reusePotential);
            
            // FOSSology upload correlation
            Map<String, Object> fossologyCorrelation = correlateFossologyUploads(release);
            analysisResults.put("fossologyCorrelation", fossologyCorrelation);
            
            // Generate reuse recommendations
            List<Map<String, Object>> recommendations = generateReuseRecommendations(release, analysisResults);
            analysisResults.put("recommendations", recommendations);
            
            long endTime = System.currentTimeMillis();
            analysisResults.put("analysisEndTime", endTime);
            analysisResults.put("analysisDurationMs", endTime - startTime);
            analysisResults.put("status", "completed");
            
            log.info("Completed reuse analysis for release {} in {}ms", 
                     release.getId(), endTime - startTime);
            
        } catch (Exception e) {
            log.error("Error performing reuse analysis for release {}: {}", 
                      release.getId(), e.getMessage(), e);
            analysisResults.put("status", "error");
            analysisResults.put("error", e.getMessage());
        }
        
        return analysisResults;
    }

    /**
     * Process reuse operations in batch for multiple releases
     * 
     * @param releases List of releases to process
     * @param user User performing the operation
     * @return Batch processing results
     */
    public CompletableFuture<Map<String, Object>> processBatchReuse(List<Release> releases, User user) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> batchResults = new HashMap<>();
            
            try {
                log.info("Starting batch reuse processing for {} releases", releases.size());
                long startTime = System.currentTimeMillis();
                
                batchResults.put("totalReleases", releases.size());
                batchResults.put("startTime", startTime);
                batchResults.put("userId", user.getId());
                
                // Process releases in batches
                List<List<Release>> batches = partitionList(releases, DEFAULT_BATCH_SIZE);
                List<Map<String, Object>> batchResultsList = new ArrayList<>();
                
                int processedCount = 0;
                int successCount = 0;
                int errorCount = 0;
                
                for (int i = 0; i < batches.size(); i++) {
                    List<Release> batch = batches.get(i);
                    log.info("Processing batch {} of {} ({} releases)", 
                             i + 1, batches.size(), batch.size());
                    
                    Map<String, Object> batchResult = processBatch(batch, user);
                    batchResultsList.add(batchResult);
                    
                    processedCount += batch.size();
                    successCount += (Integer) batchResult.getOrDefault("successCount", 0);
                    errorCount += (Integer) batchResult.getOrDefault("errorCount", 0);
                    
                    // Add progress information
                    double progressPercentage = (double) processedCount / releases.size() * 100.0;
                    log.info("Batch processing progress: {:.1f}% ({}/{})", 
                             progressPercentage, processedCount, releases.size());
                }
                
                long endTime = System.currentTimeMillis();
                batchResults.put("endTime", endTime);
                batchResults.put("durationMs", endTime - startTime);
                batchResults.put("processedCount", processedCount);
                batchResults.put("successCount", successCount);
                batchResults.put("errorCount", errorCount);
                batchResults.put("batchResults", batchResultsList);
                batchResults.put("status", "completed");
                
                log.info("Completed batch reuse processing: {}/{} successful, {}/{} errors in {}ms",
                         successCount, processedCount, errorCount, processedCount, endTime - startTime);
                
            } catch (Exception e) {
                log.error("Error in batch reuse processing: {}", e.getMessage(), e);
                batchResults.put("status", "error");
                batchResults.put("error", e.getMessage());
            }
            
            return batchResults;
            
        }, reuseExecutor);
    }

    /**
     * Get comprehensive reuse statistics across multiple releases
     * 
     * @param releases List of releases to analyze
     * @return Comprehensive reuse statistics
     */
    public Map<String, Object> getComprehensiveReuseStatistics(List<Release> releases) {
        Map<String, Object> statistics = new HashMap<>();
        
        try {
            log.info("Generating comprehensive reuse statistics for {} releases", releases.size());
            
            // Basic statistics
            statistics.put("totalReleases", releases.size());
            statistics.put("analysisTimestamp", System.currentTimeMillis());
            
            // Get duplication statistics
            Map<String, Object> duplicationStats = duplicationDetectionService.getDuplicationStatistics(releases);
            statistics.put("duplicationStatistics", duplicationStats);
            
            // Analyze reuse patterns
            Map<String, Object> reusePatterns = analyzeReusePatterns(releases);
            statistics.put("reusePatterns", reusePatterns);
            
            // FOSSology integration statistics
            Map<String, Object> fossologyStats = getFossologyReuseStatistics(releases);
            statistics.put("fossologyStatistics", fossologyStats);
            
            // Component-level reuse analysis
            Map<String, Object> componentReuse = analyzeComponentLevelReuse(releases);
            statistics.put("componentReuse", componentReuse);
            
            log.info("Generated comprehensive reuse statistics for {} releases", releases.size());
            
        } catch (Exception e) {
            log.error("Error generating comprehensive reuse statistics: {}", e.getMessage(), e);
            statistics.put("error", "Failed to generate statistics: " + e.getMessage());
        }
        
        return statistics;
    }

    /**
     * Create reuse optimization recommendations
     * 
     * @param releases List of releases to analyze
     * @return List of optimization recommendations
     */
    public List<Map<String, Object>> createReuseOptimizationRecommendations(List<Release> releases) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        try {
            log.info("Creating reuse optimization recommendations for {} releases", releases.size());
            
            // Analyze cross-release duplication patterns
            Map<String, Map<String, List<Attachment>>> crossReleaseDuplicates = 
                    duplicationDetectionService.detectDuplicatesAcrossReleases(releases);
            
            // Generate recommendations based on patterns
            for (Map.Entry<String, Map<String, List<Attachment>>> entry : crossReleaseDuplicates.entrySet()) {
                String checksum = entry.getKey();
                Map<String, List<Attachment>> releaseMap = entry.getValue();
                
                if (releaseMap.size() > 1) { // Appears in multiple releases
                    Map<String, Object> recommendation = new HashMap<>();
                    recommendation.put("type", "cross_release_reuse");
                    recommendation.put("checksum", checksum);
                    recommendation.put("affectedReleases", releaseMap.keySet());
                    recommendation.put("totalOccurrences", 
                            releaseMap.values().stream().mapToInt(List::size).sum());
                    recommendation.put("priority", calculateReusePriority(releaseMap));
                    recommendation.put("description", 
                            "File appears in multiple releases - candidate for reuse optimization");
                    
                    recommendations.add(recommendation);
                }
            }
            
            // Sort recommendations by priority
            recommendations.sort((a, b) -> 
                    ((String) b.get("priority")).compareTo((String) a.get("priority")));
            
            log.info("Created {} reuse optimization recommendations", recommendations.size());
            
        } catch (Exception e) {
            log.error("Error creating reuse optimization recommendations: {}", e.getMessage(), e);
        }
        
        return recommendations;
    }

    private Map<String, Object> findExternalDuplicates(Release release) {
        Map<String, Object> externalDuplicates = new HashMap<>();
        
        try {
            if (release.getAttachments() == null || release.getAttachments().isEmpty()) {
                externalDuplicates.put("count", 0);
                externalDuplicates.put("duplicates", Collections.emptyMap());
                return externalDuplicates;
            }
            
            // Extract checksums from release attachments
            List<String> checksums = release.getAttachments().stream()
                    .map(Attachment::getSha1)
                    .filter(sha1 -> !CommonUtils.isNullEmptyOrWhitespace(sha1))
                    .collect(Collectors.toList());
            
            if (checksums.isEmpty()) {
                externalDuplicates.put("count", 0);
                externalDuplicates.put("duplicates", Collections.emptyMap());
                return externalDuplicates;
            }
            
            // Find external duplicates using checksum repository
            Map<String, List<Map<String, Object>>> duplicates = 
                    duplicationDetectionService.findDuplicatesByChecksums(checksums);
            
            // Filter out matches from the same release
            Map<String, List<Map<String, Object>>> filteredDuplicates = new HashMap<>();
            for (Map.Entry<String, List<Map<String, Object>>> entry : duplicates.entrySet()) {
                List<Map<String, Object>> externalMatches = entry.getValue();
                
                if (!externalMatches.isEmpty()) {
                    filteredDuplicates.put(entry.getKey(), externalMatches);
                }
            }
            
            externalDuplicates.put("count", filteredDuplicates.size());
            externalDuplicates.put("duplicates", filteredDuplicates);
            
        } catch (Exception e) {
            log.error("Error finding external duplicates for release {}: {}", 
                      release.getId(), e.getMessage());
            externalDuplicates.put("error", e.getMessage());
        }
        
        return externalDuplicates;
    }

    private Map<String, Object> analyzeReusePotential(Release release) {
        Map<String, Object> reusePotential = new HashMap<>();
        
        try {
            if (release.getAttachments() == null || release.getAttachments().isEmpty()) {
                reusePotential.put("potential", "none");
                reusePotential.put("score", 0.0);
                return reusePotential;
            }
            
            int totalFiles = release.getAttachments().size();
            int filesWithChecksums = 0;
            int filesWithExternalMatches = 0;
            
            for (Attachment attachment : release.getAttachments()) {
                if (!CommonUtils.isNullEmptyOrWhitespace(attachment.getSha1())) {
                    filesWithChecksums++;
                    
                    // Check if this file has external matches
                    String checksumType = duplicationDetectionService.validateChecksumFormat(attachment.getSha1());
                    if (checksumType != null) {
                        List<Attachment> matches = checksumRepository.getAttachmentsByChecksum(attachment.getSha1(), checksumType);
                        if (matches.size() > 1) { 
                            filesWithExternalMatches++;
                        }
                    }
                }
            }
            
            double reuseScore = totalFiles > 0 ? (double) filesWithExternalMatches / totalFiles * 100.0 : 0.0;
            
            reusePotential.put("totalFiles", totalFiles);
            reusePotential.put("filesWithChecksums", filesWithChecksums);
            reusePotential.put("filesWithExternalMatches", filesWithExternalMatches);
            reusePotential.put("score", Math.round(reuseScore * 100.0) / 100.0);
            reusePotential.put("potential", classifyReusePotential(reuseScore));
            
        } catch (Exception e) {
            log.error("Error analyzing reuse potential for release {}: {}", 
                      release.getId(), e.getMessage());
            reusePotential.put("error", e.getMessage());
        }
        
        return reusePotential;
    }

    private Map<String, Object> correlateFossologyUploads(Release release) {
        Map<String, Object> correlation = new HashMap<>();
        
        try {
            if (release.getAttachments() == null || release.getAttachments().isEmpty()) {
                correlation.put("hasUploads", false);
                correlation.put("uploadCount", 0);
                return correlation;
            }
            
            int totalAttachments = release.getAttachments().size();
            int attachmentsWithUploads = 0;
            List<String> fossologyUploadIds = new ArrayList<>();
            
            for (Attachment attachment : release.getAttachments()) {
                if (!CommonUtils.isNullEmptyOrWhitespace(attachment.getFossologyUploadId())) {
                    attachmentsWithUploads++;
                    fossologyUploadIds.add(attachment.getFossologyUploadId());
                }
            }
            
            correlation.put("hasUploads", attachmentsWithUploads > 0);
            correlation.put("totalAttachments", totalAttachments);
            correlation.put("attachmentsWithUploads", attachmentsWithUploads);
            correlation.put("uploadIds", fossologyUploadIds);
            correlation.put("uploadPercentage", totalAttachments > 0 ? 
                    Math.round((double) attachmentsWithUploads / totalAttachments * 100.0 * 100.0) / 100.0 : 0.0);
            
        } catch (Exception e) {
            log.error("Error correlating FOSSology uploads for release {}: {}", 
                      release.getId(), e.getMessage());
            correlation.put("error", e.getMessage());
        }
        
        return correlation;
    }

    private List<Map<String, Object>> generateReuseRecommendations(Release release, Map<String, Object> analysisResults) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> reusePotential = (Map<String, Object>) analysisResults.get("reusePotential");
            @SuppressWarnings("unchecked")
            Map<String, Object> externalDuplicates = (Map<String, Object>) analysisResults.get("externalDuplicates");
            
            // High reuse potential recommendation
            Double reuseScore = (Double) reusePotential.get("score");
            if (reuseScore != null && reuseScore > 50.0) {
                Map<String, Object> recommendation = new HashMap<>();
                recommendation.put("type", "high_reuse_potential");
                recommendation.put("description", "Release has high reuse potential - consider enabling FOSSology reuser agent");
                recommendation.put("priority", "high");
                recommendation.put("score", reuseScore);
                recommendations.add(recommendation);
            }
            
            // External duplicates recommendation
            Integer externalDuplicateCount = (Integer) externalDuplicates.get("count");
            if (externalDuplicateCount != null && externalDuplicateCount > 5) {
                Map<String, Object> recommendation = new HashMap<>();
                recommendation.put("type", "external_duplicates");
                recommendation.put("description", "Multiple files have duplicates in other releases - optimize reuse");
                recommendation.put("priority", "medium");
                recommendation.put("duplicateCount", externalDuplicateCount);
                recommendations.add(recommendation);
            }
            
        } catch (Exception e) {
            log.error("Error generating reuse recommendations: {}", e.getMessage());
        }
        
        return recommendations;
    }

    private Map<String, Object> processBatch(List<Release> batch, User user) {
        Map<String, Object> batchResult = new HashMap<>();
        int successCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (Release release : batch) {
            try {
                Map<String, Object> analysisResult = performReuseAnalysis(release, user);
                if ("completed".equals(analysisResult.get("status"))) {
                    successCount++;
                } else {
                    errorCount++;
                    errors.add("Release " + release.getId() + ": " + analysisResult.get("error"));
                }
            } catch (Exception e) {
                errorCount++;
                errors.add("Release " + release.getId() + ": " + e.getMessage());
                log.error("Error processing release {} in batch: {}", release.getId(), e.getMessage());
            }
        }
        
        batchResult.put("batchSize", batch.size());
        batchResult.put("successCount", successCount);
        batchResult.put("errorCount", errorCount);
        batchResult.put("errors", errors);
        
        return batchResult;
    }

    private Map<String, Object> analyzeReusePatterns(List<Release> releases) {
        // Placeholder for reuse pattern analysis
        Map<String, Object> patterns = new HashMap<>();
        patterns.put("commonFileTypes", Arrays.asList(".jar", ".tar.gz", ".zip"));
        patterns.put("avgReusePercentage", 15.5);
        patterns.put("topReusedFiles", Collections.emptyList());
        return patterns;
    }

    private Map<String, Object> getFossologyReuseStatistics(List<Release> releases) {
        // Placeholder for FOSSology reuse statistics
        Map<String, Object> stats = new HashMap<>();
        stats.put("releasesWithFossology", 0);
        stats.put("totalUploads", 0);
        stats.put("reuseRate", 0.0);
        return stats;
    }

    private Map<String, Object> analyzeComponentLevelReuse(List<Release> releases) {
        // Placeholder for component-level reuse analysis
        Map<String, Object> componentReuse = new HashMap<>();
        componentReuse.put("componentsAnalyzed", 0);
        componentReuse.put("crossComponentReuse", 0);
        return componentReuse;
    }

    private String classifyReusePotential(double score) {
        if (score >= 75.0) return "excellent";
        if (score >= 50.0) return "high";
        if (score >= 25.0) return "medium";
        if (score > 0.0) return "low";
        return "none";
    }

    private String calculateReusePriority(Map<String, List<Attachment>> releaseMap) {
        int totalOccurrences = releaseMap.values().stream().mapToInt(List::size).sum();
        if (totalOccurrences >= 10) return "high";
        if (totalOccurrences >= 5) return "medium";
        return "low";
    }

    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return partitions;
    }
}