/*
 * Copyright Ritankar Saha<ritankar.saha786@gmail.com>, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.fossology.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.fossology.config.FossologyRestConfig;
import org.eclipse.sw360.fossology.rest.FossologyRestClient;
import org.eclipse.sw360.datahandler.common.FileSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for FOSSology v2 file search functionality
 */
@Service
public class FossologyFileSearchService {

    private static final Logger log = LoggerFactory.getLogger(FossologyFileSearchService.class);
    
    @Autowired
    private FossologyRestClient fossologyRestClient;

    @Autowired
    private FossologyRestConfig restConfig;

    /**
     * Search for files by SHA1 checksum using FOSSology v2 /filesearch endpoint
     * 
     * @param sha1Values List of SHA1 checksums to search for
     * @return Map of SHA1 -> List of FileSearchResult
     */
    @Cacheable(value = "fileSearchBySha1", key = "#sha1Values.toString()")
    public Map<String, List<FileSearchResult>> searchFilesBySha1(List<String> sha1Values) {
        if (CommonUtils.isNullOrEmptyCollection(sha1Values)) {
            log.warn("No SHA1 values provided for file search");
            return Collections.emptyMap();
        }

        if (!restConfig.isFileSearchEnabled()) {
            log.warn("File search is disabled in configuration");
            return Collections.emptyMap();
        }

        log.info("Searching for {} files by SHA1 using FOSSology v2 API", sha1Values.size());

        try {
            Map<String, List<Integer>> fossologyResults = fossologyRestClient.searchFilesBySha1(sha1Values);
            Map<String, List<FileSearchResult>> results = new HashMap<>();

            for (Map.Entry<String, List<Integer>> entry : fossologyResults.entrySet()) {
                String sha1 = entry.getKey();
                List<Integer> uploadIds = entry.getValue();
                
                List<FileSearchResult> fileResults = new ArrayList<>();
                for (Integer uploadId : uploadIds) {
                    // Get detailed file information from each upload
                    JsonNode filesInUpload = fossologyRestClient.getFilesByUpload(uploadId);
                    if (filesInUpload != null && filesInUpload.isArray()) {
                        for (JsonNode fileNode : filesInUpload) {
                            String fileSha1 = fileNode.path("sha1").asText();
                            if (sha1.equals(fileSha1)) {
                                FileSearchResult result = createFileSearchResult(fileNode, uploadId);
                                if (result != null) {
                                    fileResults.add(result);
                                }
                            }
                        }
                    }
                }
                results.put(sha1, fileResults);
            }

            log.info("Found {} results for SHA1 search", results.size());
            return results;

        } catch (Exception e) {
            log.error("Error during FOSSology file search by SHA1: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Search for files by filename pattern using FOSSology v2 /filesearch endpoint
     * 
     * @param filename Filename pattern to search for
     * @param uploadId Optional upload ID to limit search scope
     * @return List of FileSearchResult matching the filename pattern
     */
    @Cacheable(value = "fileSearchByName", key = "#filename + '_' + (#uploadId != null ? #uploadId : 'all')")
    public List<FileSearchResult> searchFilesByName(String filename, Integer uploadId) {
        if (CommonUtils.isNullEmptyOrWhitespace(filename)) {
            log.warn("No filename provided for file search");
            return Collections.emptyList();
        }

        if (!restConfig.isFileSearchEnabled()) {
            log.warn("File search is disabled in configuration");
            return Collections.emptyList();
        }

        log.info("Searching for files by filename pattern: {} (uploadId: {})", filename, uploadId);

        try {
            JsonNode fossologyResults = fossologyRestClient.searchFilesByName(filename, uploadId);
            if (fossologyResults == null || !fossologyResults.isArray()) {
                log.info("No results found for filename pattern: {}", filename);
                return Collections.emptyList();
            }

            List<FileSearchResult> results = new ArrayList<>();
            for (JsonNode searchResult : fossologyResults) {
                JsonNode uploads = searchResult.path("uploads");
                if (uploads.isArray()) {
                    for (JsonNode upload : uploads) {
                        int currentUploadId = upload.asInt();
                        
                        // Get detailed file information from the upload
                        JsonNode filesInUpload = fossologyRestClient.getFilesByUpload(currentUploadId);
                        if (filesInUpload != null && filesInUpload.isArray()) {
                            for (JsonNode fileNode : filesInUpload) {
                                String fileName = fileNode.path("filename").asText();
                                if (matchesFilenamePattern(fileName, filename)) {
                                    FileSearchResult result = createFileSearchResult(fileNode, currentUploadId);
                                    if (result != null) {
                                        results.add(result);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            log.info("Found {} results for filename search: {}", results.size(), filename);
            return results;

        } catch (Exception e) {
            log.error("Error during FOSSology file search by name '{}': {}", filename, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Search for files by content pattern using FOSSology v2 /filesearch endpoint
     * 
     * @param content Content pattern to search for
     * @param uploadId Optional upload ID to limit search scope
     * @return List of FileSearchResult containing the content pattern
     */
    @Cacheable(value = "fileSearchByContent", key = "#content + '_' + (#uploadId != null ? #uploadId : 'all')")
    public List<FileSearchResult> searchFilesByContent(String content, Integer uploadId) {
        if (CommonUtils.isNullEmptyOrWhitespace(content)) {
            log.warn("No content pattern provided for file search");
            return Collections.emptyList();
        }

        if (!restConfig.isFileSearchEnabled()) {
            log.warn("File search is disabled in configuration");
            return Collections.emptyList();
        }

        log.info("Searching for files by content pattern: {} (uploadId: {})", content, uploadId);

        try {
            JsonNode fossologyResults = fossologyRestClient.searchFilesByContent(content, uploadId);
            if (fossologyResults == null || !fossologyResults.isArray()) {
                log.info("No results found for content pattern: {}", content);
                return Collections.emptyList();
            }

            List<FileSearchResult> results = new ArrayList<>();
            for (JsonNode searchResult : fossologyResults) {
                JsonNode uploads = searchResult.path("uploads");
                if (uploads.isArray()) {
                    for (JsonNode upload : uploads) {
                        int currentUploadId = upload.asInt();
                        
                        // Get detailed file information from the upload
                        JsonNode filesInUpload = fossologyRestClient.getFilesByUpload(currentUploadId);
                        if (filesInUpload != null && filesInUpload.isArray()) {
                            for (JsonNode fileNode : filesInUpload) {
                                FileSearchResult result = createFileSearchResult(fileNode, currentUploadId);
                                if (result != null) {
                                    results.add(result);
                                }
                            }
                        }
                    }
                }
            }

            log.info("Found {} results for content search: {}", results.size(), content);
            return results;

        } catch (Exception e) {
            log.error("Error during FOSSology file search by content '{}': {}", content, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get files from a specific upload using FOSSology v2 API
     * 
     * @param uploadId Upload ID to get files from
     * @return List of FileSearchResult from the upload
     */
    @Cacheable(value = "filesByUpload", key = "#uploadId")
    public List<FileSearchResult> getFilesByUpload(int uploadId) {
        if (uploadId <= 0) {
            log.warn("Invalid upload ID: {}", uploadId);
            return Collections.emptyList();
        }

        if (!restConfig.isFileSearchEnabled()) {
            log.warn("File search is disabled in configuration");
            return Collections.emptyList();
        }

        log.info("Getting files from upload: {}", uploadId);

        try {
            JsonNode filesInUpload = fossologyRestClient.getFilesByUpload(uploadId);
            if (filesInUpload == null || !filesInUpload.isArray()) {
                log.info("No files found in upload: {}", uploadId);
                return Collections.emptyList();
            }

            List<FileSearchResult> results = new ArrayList<>();
            for (JsonNode fileNode : filesInUpload) {
                FileSearchResult result = createFileSearchResult(fileNode, uploadId);
                if (result != null) {
                    results.add(result);
                }
            }

            log.info("Found {} files in upload: {}", results.size(), uploadId);
            return results;

        } catch (Exception e) {
            log.error("Error getting files from upload {}: {}", uploadId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get file search statistics
     * 
     * @return Map containing search statistics
     */
    public Map<String, Object> getSearchStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        try {
            // Check if FOSSology connection is available
            boolean connectionStatus = fossologyRestClient.checkConnection();
            statistics.put("fossologyConnectionStatus", connectionStatus);
            statistics.put("fileSearchEnabled", restConfig.isFileSearchEnabled());
            
            // Get configuration values
            String maxResults = restConfig.getFileSearchMaxResults();
            statistics.put("maxResults", maxResults != null ? maxResults : "100");
            
            String timeout = restConfig.getFileSearchTimeout();
            statistics.put("timeout", timeout != null ? timeout : "30");
            
            String cacheTtl = restConfig.getFileSearchCacheTtl();
            statistics.put("cacheTtl", cacheTtl != null ? cacheTtl : "30");
            
            log.debug("Generated file search statistics: {}", statistics);
            return statistics;
            
        } catch (Exception e) {
            log.error("Error generating search statistics: {}", e.getMessage(), e);
            statistics.put("error", "Failed to generate statistics: " + e.getMessage());
            return statistics;
        }
    }

    /**
     * Advanced file search with multiple criteria using FOSSology v2 API
     * 
     * @param sha1Values Optional SHA1 checksums to search for
     * @param filename Optional filename pattern to search for
     * @param content Optional content pattern to search for
     * @param uploadId Optional upload ID to limit search scope
     * @param maxResults Maximum number of results to return
     * @return Set of FileSearchResult matching the criteria
     */
    public Set<FileSearchResult> advancedFileSearch(List<String> sha1Values, String filename, 
                                                   String content, Integer uploadId, Integer maxResults) {
        Set<FileSearchResult> combinedResults = new HashSet<>();
        
        if (!restConfig.isFileSearchEnabled()) {
            log.warn("File search is disabled in configuration");
            return combinedResults;
        }

        log.info("Performing advanced file search with multiple criteria");

        try {
            // Search by SHA1 if provided
            if (!CommonUtils.isNullOrEmptyCollection(sha1Values)) {
                Map<String, List<FileSearchResult>> sha1Results = searchFilesBySha1(sha1Values);
                sha1Results.values().forEach(combinedResults::addAll);
                log.debug("Added {} results from SHA1 search", sha1Results.size());
            }

            // Search by filename if provided
            if (!CommonUtils.isNullEmptyOrWhitespace(filename)) {
                List<FileSearchResult> filenameResults = searchFilesByName(filename, uploadId);
                combinedResults.addAll(filenameResults);
                log.debug("Added {} results from filename search", filenameResults.size());
            }

            // Search by content if provided
            if (!CommonUtils.isNullEmptyOrWhitespace(content)) {
                List<FileSearchResult> contentResults = searchFilesByContent(content, uploadId);
                combinedResults.addAll(contentResults);
                log.debug("Added {} results from content search", contentResults.size());
            }

            // If upload ID is specified and no other criteria provided, get all files from upload
            if (uploadId != null && 
                CommonUtils.isNullOrEmptyCollection(sha1Values) && 
                CommonUtils.isNullEmptyOrWhitespace(filename) && 
                CommonUtils.isNullEmptyOrWhitespace(content)) {
                List<FileSearchResult> uploadResults = getFilesByUpload(uploadId);
                combinedResults.addAll(uploadResults);
                log.debug("Added {} results from upload search", uploadResults.size());
            }

            // Apply result limit
            if (maxResults != null && combinedResults.size() > maxResults) {
                combinedResults = combinedResults.stream()
                        .limit(maxResults)
                        .collect(Collectors.toSet());
                log.debug("Limited results to {}", maxResults);
            }

            log.info("Advanced file search returned {} results", combinedResults.size());
            return combinedResults;

        } catch (Exception e) {
            log.error("Error during advanced file search: {}", e.getMessage(), e);
            return Collections.emptySet();
        }
    }

    /**
     * Create FileSearchResult from FOSSology JSON response
     */
    private FileSearchResult createFileSearchResult(JsonNode fileNode, int uploadId) {
        try {
            FileSearchResult result = new FileSearchResult();
            result.setUploadId(uploadId);
            
            // Extract folder ID if available
            if (fileNode.has("folderId")) {
                result.setFolderId(fileNode.get("folderId").asInt(0));
            } else {
                // Try to get folder ID from upload if not in file node
                int folderId = fossologyRestClient.getFolderId(uploadId);
                result.setFolderId(folderId > 0 ? folderId : 0);
            }
            
            result.setFilename(fileNode.path("filename").asText(""));
            result.setSha1(fileNode.path("sha1").asText(""));
            result.setMd5(fileNode.path("md5").asText(""));
            
            // Handle size field - could be string or number
            JsonNode sizeNode = fileNode.path("size");
            if (sizeNode.isNumber()) {
                result.setSize(sizeNode.asLong(0L));
            } else if (sizeNode.isTextual()) {
                try {
                    result.setSize(Long.parseLong(sizeNode.asText("0")));
                } catch (NumberFormatException e) {
                    result.setSize(0L);
                }
            } else {
                result.setSize(0L);
            }

            return result;
            
        } catch (Exception e) {
            log.error("Error creating FileSearchResult from JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if filename matches pattern (supports wildcards)
     */
    private boolean matchesFilenamePattern(String filename, String pattern) {
        if (CommonUtils.isNullEmptyOrWhitespace(filename) || CommonUtils.isNullEmptyOrWhitespace(pattern)) {
            return false;
        }
        
        // Convert wildcard pattern to regex
        String regexPattern = pattern
                .replace(".", "\\.")  // Escape dots
                .replace("*", ".*")   // Convert * to .*
                .replace("?", ".");   // Convert ? to .
        
        return filename.matches(regexPattern);
    }

    /**
     * Clear all file search caches
     */
    @CacheEvict(value = {"fileSearchBySha1", "fileSearchByName", "fileSearchByContent", "filesByUpload"}, allEntries = true)
    public void clearAllCaches() {
        log.info("Cleared all file search caches");
    }

    /**
     * Clear SHA1 search cache
     */
    @CacheEvict(value = "fileSearchBySha1", allEntries = true)
    public void clearSha1Cache() {
        log.info("Cleared SHA1 file search cache");
    }

    /**
     * Clear filename search cache
     */
    @CacheEvict(value = "fileSearchByName", allEntries = true)
    public void clearFilenameCache() {
        log.info("Cleared filename file search cache");
    }

    /**
     * Clear content search cache
     */
    @CacheEvict(value = "fileSearchByContent", allEntries = true)
    public void clearContentCache() {
        log.info("Cleared content file search cache");
    }

    /**
     * Clear upload files cache
     */
    @CacheEvict(value = "filesByUpload", allEntries = true)
    public void clearUploadFilesCache() {
        log.info("Cleared upload files cache");
    }
}