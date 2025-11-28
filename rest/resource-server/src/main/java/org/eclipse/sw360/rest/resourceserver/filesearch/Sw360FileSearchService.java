/*
 * Copyright Ritankar Saha<ritankar.saha786@gmail.com>, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */


package org.eclipse.sw360.rest.resourceserver.filesearch;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.eclipse.sw360.datahandler.common.FileSearchResult;
import org.eclipse.sw360.rest.resourceserver.filesearch.FileSearchController.FileSearchCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for handling file search operations through SW360 REST API
 */
@Service
public class Sw360FileSearchService {

    private static final Logger log = LoggerFactory.getLogger(Sw360FileSearchService.class);

    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${fossology.rest.url:http://localhost/repo}")
    private String fossologyBaseUrl;
    
    @Value("${fossology.rest.token:eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJleHAiOjE3NTQwMDYzOTksIm5iZiI6MTc1MjYyNDAwMCwianRpIjoiTVRJdU13PT0iLCJzY29wZSI6IndyaXRlIn0.XdTFQynG3a-HpMRyP_PyEzkd3lxh_Fhe_xWM3nMrV2g}")
    private String fossologyToken;

    /**
     * Search files by SHA1 checksums
     * 
     * @param sha1Values List of SHA1 checksums to search for
     * @param user Current user
     * @return Map of SHA1 -> List of FileSearchResult
     */
    @PreAuthorize("hasAuthority('READ') or hasAuthority('WRITE')")
    @Cacheable(value = "fileSearchBySha1", key = "#sha1Values.hashCode()")
    public Map<String, List<FileSearchResult>> searchFilesBySha1(List<String> sha1Values, User user) {
        if (CommonUtils.isNullOrEmptyCollection(sha1Values)) {
            log.warn("No SHA1 values provided for file search");
            return Collections.emptyMap();
        }

        try {
            String url = fossologyBaseUrl + "/api/v2/filesearch";
            
            // Create request body for FOSSology API
            List<Map<String, String>> requestBody = sha1Values.stream()
                    .map(sha1 -> Map.of("sha1", sha1))
                    .collect(Collectors.toList());
            
            HttpHeaders headers = createAuthenticatedHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<Map<String, String>>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, entity, JsonNode.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseFileSearchResults(response.getBody(), "sha1");
            }
            
            log.warn("No results from FOSSology API for SHA1 search");
            return Collections.emptyMap();
            
        } catch (Exception e) {
            log.error("Error during file search by SHA1 for user {}: {}", user.getEmail(), e.getMessage());
            return Collections.emptyMap();
        }

        //log.info("User {} searching for {} files by SHA1", user.getEmail(), sha1Values.size());
        
    }

    /**
     * Advanced file search with multiple criteria
     * 
     * @param criteria Search criteria
     * @param user Current user
     * @return List of FileSearchResult matching the criteria
     */
    @PreAuthorize("hasAuthority('READ') or hasAuthority('WRITE')")
    public List<FileSearchResult> advancedFileSearch(FileSearchCriteria criteria, User user) {
        if (criteria == null) {
            log.warn("No search criteria provided for advanced file search");
            return Collections.emptyList();
        }

        log.info("User {} performing advanced file search", user.getEmail());
        
        Set<FileSearchResult> combinedResults = new HashSet<>();
        
        // Search by SHA1 if provided
        if (!CommonUtils.isNullOrEmptyCollection(criteria.getSha1Values())) {
            Map<String, List<FileSearchResult>> sha1Results = searchFilesBySha1(criteria.getSha1Values(), user);
            sha1Results.values().forEach(combinedResults::addAll);
        }

        // Apply result limit
        List<FileSearchResult> finalResults = new ArrayList<>(combinedResults);
        if (criteria.getMaxResults() != null && finalResults.size() > criteria.getMaxResults()) {
            finalResults = finalResults.subList(0, criteria.getMaxResults());
        }

        return finalResults;
    }

    /**
     * Get file search statistics for monitoring and analytics
     * 
     * @param user Current user
     * @return Map containing search statistics
     */
    @PreAuthorize("hasAuthority('READ') or hasAuthority('WRITE')")
    public Map<String, Object> getSearchStatistics(User user) {
        log.info("User {} requesting file search statistics", user.getEmail());
        
        try {
            Map<String, Object> statistics = new HashMap<>();
            
            // Check if FOSSology connection is available
            String healthUrl = fossologyBaseUrl + "/api/v2/info";
            try {
                ResponseEntity<String> healthResponse = restTemplate.getForEntity(healthUrl, String.class);
                statistics.put("fossologyConnectionStatus", healthResponse.getStatusCode().is2xxSuccessful());
            } catch (Exception e) {
                statistics.put("fossologyConnectionStatus", false);
            }
            
            statistics.put("fileSearchEnabled", true);
            statistics.put("maxResults", "100");
            statistics.put("timeout", "30");
            statistics.put("cacheTtl", "30");
            
            return statistics;
            
        } catch (Exception e) {
            log.error("Error getting search statistics for user {}: {}", user.getEmail(), e.getMessage());
            Map<String, Object> errorStats = new HashMap<>();
            errorStats.put("error", "Failed to generate statistics: " + e.getMessage());
            return errorStats;
        }
    }

    /**
     * Clear file search cache
     * 
     * @param user Current user
     * @return Success status
     */
    @PreAuthorize("hasAuthority('WRITE')")
    @CacheEvict(value = {"fileSearchBySha1"}, allEntries = true)
    public boolean clearSearchCache(User user) {
        log.info("User {} clearing file search cache", user.getEmail());
        
        try {
            // Cache is cleared by the @CacheEvict annotation
            log.info("All file search caches cleared successfully by user {}", user.getEmail());
            return true;
        } catch (Exception e) {
            log.error("Error clearing search cache for user {}: {}", user.getEmail(), e.getMessage());
            return false;
        }
    }

    /**
     * Parse FOSSology file search results for SHA1 searches
     */
    private Map<String, List<FileSearchResult>> parseFileSearchResults(JsonNode response, String searchType) {
        Map<String, List<FileSearchResult>> results = new HashMap<>();
        
        if (response.isArray()) {
            for (JsonNode searchResult : response) {
                JsonNode hashNode = searchResult.path("hash");
                String sha1 = hashNode.path("sha1").asText();
                List<FileSearchResult> fileResults = new ArrayList<>();
                
                JsonNode uploads = searchResult.path("uploads");
                if (uploads.isArray()) {
                    for (JsonNode upload : uploads) {
                        int uploadId = upload.asInt();
                        
                        // Get detailed upload info
                        FileSearchResult result = createFileSearchResultFromUpload(uploadId, hashNode);
                        if (result != null) {
                            fileResults.add(result);
                        }
                    }
                }
                
                if (!fileResults.isEmpty()) {
                    results.put(sha1, fileResults);
                }
            }
        }
        
        return results;
    }
    
    
    
    
    /**
     * Create FileSearchResult from upload ID with detailed info from Fossology
     */
    private FileSearchResult createFileSearchResultFromUpload(int uploadId, JsonNode hashNode) {
        try {
            String url = fossologyBaseUrl + "/api/v2/uploads/" + uploadId;
            
            HttpHeaders headers = createAuthenticatedHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode uploadData = response.getBody();
                
                FileSearchResult result = new FileSearchResult();
                result.setUploadId(uploadId);
                result.setFolderId(uploadData.path("folderId").asInt(0));
                result.setFilename(uploadData.path("uploadName").asText(""));
                
                // Use hash info from search result if available, otherwise from upload data
                if (hashNode != null && !hashNode.isMissingNode()) {
                    result.setSha1(hashNode.path("sha1").asText(""));
                    result.setMd5(hashNode.path("md5").asText(""));
                    result.setSize(hashNode.path("size").asLong(0L));
                } else {
                    JsonNode uploadHashNode = uploadData.path("hash");
                    if (!uploadHashNode.isMissingNode()) {
                        result.setSha1(uploadHashNode.path("sha1").asText(""));
                        result.setMd5(uploadHashNode.path("md5").asText(""));
                        result.setSize(uploadHashNode.path("size").asLong(0L));
                    }
                }
                
                return result;
            }
            
        } catch (Exception e) {
            log.warn("Could not fetch upload details for ID {}: {}", uploadId, e.getMessage());
        }
        
        // Fallback: create basic result with available info
        FileSearchResult result = new FileSearchResult();
        result.setUploadId(uploadId);
        if (hashNode != null && !hashNode.isMissingNode()) {
            result.setSha1(hashNode.path("sha1").asText(""));
            result.setMd5(hashNode.path("md5").asText(""));
            result.setSize(hashNode.path("size").asLong(0L));
        }
        return result;
    }
    
    /**
     * Create HTTP headers with FOSSology authentication
     */
    private HttpHeaders createAuthenticatedHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(fossologyToken);
        return headers;
    }

}