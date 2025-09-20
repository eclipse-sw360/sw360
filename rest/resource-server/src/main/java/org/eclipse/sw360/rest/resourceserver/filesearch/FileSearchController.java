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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.common.*;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller for FOSSology file search functionality
 */
@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
public class FileSearchController implements RepresentationModelProcessor<RepositoryLinksResource> {

    public static final String FILESEARCH_URL = "/filesearch";

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    private final Sw360FileSearchService fileSearchService;

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(FileSearchController.class).slash(FILESEARCH_URL).withRel("filesearch"));
        return resource;
    }

    @Operation(
            summary = "Search files by SHA1 checksums.",
            description = "Search for files in FOSSology using SHA1 checksums. Returns upload information for matching files.",
            tags = {"File Search"}
    )
    @PreAuthorize("hasAuthority('READ') or hasAuthority('WRITE')")
    @RequestMapping(value = FILESEARCH_URL + "/sha1", method = RequestMethod.POST)
    @ApiResponse(
            responseCode = "200", 
            description = "Search results",
            content = @Content(mediaType = "application/json")
    )
    public ResponseEntity<Map<String, List<FileSearchResult>>> searchFilesBySha1(
            HttpServletRequest request,
            @Parameter(description = "List of SHA1 checksums to search for")
            @RequestBody List<String> sha1Values) {
        
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Map<String, List<FileSearchResult>> results = fileSearchService.searchFilesBySha1(sha1Values, user);
        
        return ResponseEntity.ok(results);
    }




    @Operation(
            summary = "Advanced file search with multiple criteria.",
            description = "Search for files using multiple criteria including SHA1, filename, content, and upload scope.",
            tags = {"File Search"}
    )
    @PreAuthorize("hasAuthority('READ') or hasAuthority('WRITE')")
    @RequestMapping(value = FILESEARCH_URL + "/advanced", method = RequestMethod.POST)
    @ApiResponse(
            responseCode = "200", 
            description = "Advanced search results",
            content = @Content(mediaType = "application/json")
    )
    public ResponseEntity<List<FileSearchResult>> advancedFileSearch(
            HttpServletRequest request,
            @Parameter(description = "Advanced search criteria")
            @RequestBody FileSearchCriteria criteria) {
        
        User user = restControllerHelper.getSw360UserFromAuthentication();
        List<FileSearchResult> results = fileSearchService.advancedFileSearch(criteria, user);
        
        return ResponseEntity.ok(results);
    }

    @Operation(
            summary = "Get file search statistics.",
            description = "Get statistics about file search operations for monitoring and analytics.",
            tags = {"File Search"}
    )
    @PreAuthorize("hasAuthority('READ') or hasAuthority('WRITE')")
    @RequestMapping(value = FILESEARCH_URL + "/statistics", method = RequestMethod.GET)
    @ApiResponse(
            responseCode = "200", 
            description = "Search statistics",
            content = @Content(mediaType = "application/json")
    )
    public ResponseEntity<Map<String, Object>> getSearchStatistics(HttpServletRequest request) {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Map<String, Object> statistics = fileSearchService.getSearchStatistics(user);
        
        return ResponseEntity.ok(statistics);
    }

    @Operation(
            summary = "Clear file search cache.",
            description = "Clear all cached file search results. Requires WRITE permission.",
            tags = {"File Search"}
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = FILESEARCH_URL + "/cache/clear", method = RequestMethod.POST)
    @ApiResponse(
            responseCode = "200", 
            description = "Cache clear status",
            content = @Content(mediaType = "application/json")
    )
    public ResponseEntity<Map<String, Object>> clearSearchCache(HttpServletRequest request) {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        boolean success = fileSearchService.clearSearchCache(user);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "Cache cleared successfully" : "Failed to clear cache");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Search criteria for advanced file search
     */
    public static class FileSearchCriteria {
        @Schema(description = "List of SHA1 checksums to search for")
        private List<String> sha1Values;
        
        @Schema(description = "Filename pattern to search for")
        private String filename;
        
        @Schema(description = "Content pattern to search for")
        private String content;
        
        @Schema(description = "Upload ID to limit search scope")
        private Integer uploadId;
        
        @Schema(description = "Maximum number of results to return", defaultValue = "100")
        private Integer maxResults = 100;
        
        @Schema(description = "Whether to include file content in results", defaultValue = "false")
        private boolean includeContent = false;

        // Getters and setters
        public List<String> getSha1Values() { return sha1Values; }
        public void setSha1Values(List<String> sha1Values) { this.sha1Values = sha1Values; }
        
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public Integer getUploadId() { return uploadId; }
        public void setUploadId(Integer uploadId) { this.uploadId = uploadId; }
        
        public Integer getMaxResults() { return maxResults; }
        public void setMaxResults(Integer maxResults) { this.maxResults = maxResults; }
        
        public boolean isIncludeContent() { return includeContent; }
        public void setIncludeContent(boolean includeContent) { this.includeContent = includeContent; }
    }
}