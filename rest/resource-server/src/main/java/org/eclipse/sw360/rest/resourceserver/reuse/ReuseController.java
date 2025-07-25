/*
 * Copyright Ritankar Saha<ritankar.saha786@gmail.com>, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.reuse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.component.Sw360ComponentService;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
@Tag(name = "Reuse", description = "Reuse and clearing decision inheritance operations")
public class ReuseController {

    public static final String REUSE_URL = "/reuse";

    @NonNull
    private final Sw360AttachmentService attachmentService;

    @NonNull
    private final Sw360ProjectService projectService;

    @NonNull
    private final Sw360ReleaseService releaseService;

    @NonNull
    private final Sw360ComponentService componentService;

    @Value("${fossology.rest.url:http://localhost/repo}")
    private String fossologyBaseUrl;

    @Value("${fossology.rest.token:${FOSSOLOGY_TOKEN:eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJleHAiOjE3NTQwMDYzOTksIm5iZiI6MTc1MjYyNDAwMCwianRpIjoiTVRJdU13PT0iLCJzY29wZSI6IndyaXRlIn0.XdTFQynG3a-HpMRyP_PyEzkd3lxh_Fhe_xWM3nMrV2g}}")
    private String fossologyToken;

    @Autowired
    private RestTemplate restTemplate;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @Operation(
            summary = "Analyze reuse potential for a release.",
            description = "Perform comprehensive reuse analysis for a release including duplicate detection and clearing decision reuse potential.",
            tags = {"Reuse"}
    )
    @GetMapping(value = REUSE_URL + "/releases/{releaseId}/analyze", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('WRITE')")
    public ResponseEntity<Map<String, Object>> analyzeReleaseReuse(
            @Parameter(description = "ID of the release to analyze")
            @PathVariable("releaseId") String releaseId,
            @Parameter(description = "Include detailed file analysis")
            @RequestParam(defaultValue = "false") boolean includeFileDetails,
            @Parameter(description = "Maximum number of source releases to analyze against")
            @RequestParam(defaultValue = "100") int maxSourceReleases
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);

        Map<String, Object> analysisResult = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            Release targetRelease = releaseService.getReleaseForUserById(releaseId, sw360User);
            
            // Get reuse statistics from FOSSology API
            Map<String, Object> reuseStats = getReuseStatisticsFromFossology(releaseId);
            
            // Find reuse candidates
            Map<String, List<Integer>> candidates = findReuseCandidatesFromFossology(releaseId);
            
            // Analyze attachment-level reuse
            Map<String, Object> attachmentAnalysis = analyzeAttachmentReuse(targetRelease, includeFileDetails);
            
            // Calculate comprehensive metrics
            int totalFiles = targetRelease.getAttachments() != null ? targetRelease.getAttachments().size() : 0;
            int reusableFiles = candidates.size();
            double reusePercentage = totalFiles > 0 ? (double) reusableFiles / totalFiles * 100.0 : 0.0;
            
            analysisResult.put("releaseId", releaseId);
            analysisResult.put("releaseName", targetRelease.getName());
            analysisResult.put("releaseVersion", targetRelease.getVersion());
            analysisResult.put("componentId", targetRelease.getComponentId());
            analysisResult.put("totalFiles", totalFiles);
            analysisResult.put("reusableFiles", reusableFiles);
            analysisResult.put("reusePercentage", Math.round(reusePercentage * 100.0) / 100.0);
            analysisResult.put("reuseStatistics", reuseStats);
            analysisResult.put("reuseCandidates", candidates);
            analysisResult.put("attachmentAnalysis", attachmentAnalysis);
            analysisResult.put("analysisConfiguration", Map.of(
                    "includeFileDetails", includeFileDetails,
                    "maxSourceReleases", maxSourceReleases
            ));
            
            // Generate recommendations
            List<String> recommendations = generateReuseRecommendations(reusePercentage, totalFiles, reusableFiles);
            analysisResult.put("recommendations", recommendations);
            
            long endTime = System.currentTimeMillis();
            analysisResult.put("analysisMetadata", Map.of(
                    "analyzedBy", sw360User.getEmail(),
                    "analyzedAt", System.currentTimeMillis(),
                    "processingTimeMs", endTime - startTime,
                    "success", true
            ));

            return new ResponseEntity<>(analysisResult, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error analyzing reuse for release {}: {}", releaseId, e.getMessage(), e);
            analysisResult.put("error", "Analysis failed: " + e.getMessage());
            analysisResult.put("analysisMetadata", Map.of(
                    "analyzedBy", sw360User.getEmail(),
                    "analyzedAt", System.currentTimeMillis(),
                    "processingTimeMs", System.currentTimeMillis() - startTime,
                    "success", false
            ));
            return new ResponseEntity<>(analysisResult, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Apply clearing decision reuse for a release.",
            description = "Apply clearing decision inheritance from other releases with identical file content using FOSSology reuser agent.",
            tags = {"Reuse"}
    )
    @PostMapping(value = REUSE_URL + "/releases/{releaseId}/apply", 
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('WRITE')")
    public ResponseEntity<Map<String, Object>> applyClearingDecisionReuse(
            @Parameter(description = "ID of the release to apply reuse to")
            @PathVariable("releaseId") String releaseId,
            @Parameter(description = "Reuse configuration")
            @RequestBody ReuseApplicationRequest reuseRequest
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);

        Map<String, Object> applicationResult = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            releaseService.getReleaseForUserById(releaseId, sw360User);
            
            // Validate reuse request
            List<String> validationErrors = validateReuseRequest(reuseRequest);
            if (!validationErrors.isEmpty()) {
                applicationResult.put("error", "Invalid reuse request");
                applicationResult.put("validationErrors", validationErrors);
                return new ResponseEntity<>(applicationResult, HttpStatus.BAD_REQUEST);
            }
            
            // Apply FOSSology reuser agent
            String sourceUploadId = reuseRequest.getSourceUploadId() != null ? 
                    reuseRequest.getSourceUploadId().toString() : "0";
            
            boolean successful = enableReuseForReleaseViaFossology(releaseId, Integer.parseInt(sourceUploadId));
            
            if (successful) {
                // Get updated statistics after reuse application
                Map<String, Object> updatedStats = getReuseStatisticsFromFossology(releaseId);
                
                applicationResult.put("status", "success");
                applicationResult.put("message", "Clearing decision reuse applied successfully");
                applicationResult.put("updatedStatistics", updatedStats);
            } else {
                applicationResult.put("status", "failed");
                applicationResult.put("message", "Failed to apply clearing decision reuse");
            }
            
            long endTime = System.currentTimeMillis();
            applicationResult.put("releaseId", releaseId);
            applicationResult.put("reuseRequest", reuseRequest);
            applicationResult.put("applicationMetadata", Map.of(
                    "appliedBy", sw360User.getEmail(),
                    "appliedAt", System.currentTimeMillis(),
                    "processingTimeMs", endTime - startTime,
                    "successful", successful
            ));

            HttpStatus responseStatus = successful ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
            return new ResponseEntity<>(applicationResult, responseStatus);

        } catch (Exception e) {
            log.error("Error applying clearing decision reuse for release {}: {}", releaseId, e.getMessage(), e);
            applicationResult.put("status", "error");
            applicationResult.put("error", "Application failed: " + e.getMessage());
            applicationResult.put("applicationMetadata", Map.of(
                    "appliedBy", sw360User.getEmail(),
                    "appliedAt", System.currentTimeMillis(),
                    "processingTimeMs", System.currentTimeMillis() - startTime,
                    "successful", false
            ));
            return new ResponseEntity<>(applicationResult, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get reuse statistics across multiple releases.",
            description = "Get comprehensive reuse statistics and patterns across multiple releases or entire projects.",
            tags = {"Reuse"}
    )
    @PostMapping(value = REUSE_URL + "/statistics", 
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<Map<String, Object>> getComprehensiveReuseStatistics(
            @Parameter(description = "Statistics request configuration")
            @RequestBody ReuseStatisticsRequest statisticsRequest
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);

        Map<String, Object> statistics = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // Validate request
            if (statisticsRequest.getReleaseIds() == null || statisticsRequest.getReleaseIds().isEmpty()) {
                statistics.put("error", "No release IDs provided");
                return new ResponseEntity<>(statistics, HttpStatus.BAD_REQUEST);
            }

            List<String> releaseIds = statisticsRequest.getReleaseIds();
            Map<String, Object> releaseStatistics = new HashMap<>();
            Map<String, Integer> overallMetrics = new HashMap<>();
            
            int totalReleases = releaseIds.size();
            int processedReleases = 0;
            int totalFiles = 0;
            int totalReusableFiles = 0;
            
            // Process each release
            for (String releaseId : releaseIds) {
                try {
                    Release release = releaseService.getReleaseForUserById(releaseId, sw360User);
                    Map<String, Object> releaseStats = getReuseStatisticsFromFossology(releaseId);
                    
                    releaseStatistics.put(releaseId, Map.of(
                            "releaseName", release.getName(),
                            "releaseVersion", release.getVersion(),
                            "statistics", releaseStats
                    ));
                    
                    // Aggregate metrics
                    Object totalFilesObj = releaseStats.get("totalFiles");
                    Object reusableFilesObj = releaseStats.get("reusableFiles");
                    
                    if (totalFilesObj instanceof Number) {
                        totalFiles += ((Number) totalFilesObj).intValue();
                    }
                    if (reusableFilesObj instanceof Number) {
                        totalReusableFiles += ((Number) reusableFilesObj).intValue();
                    }
                    
                    processedReleases++;
                    
                } catch (Exception e) {
                    log.warn("Failed to get statistics for release {}: {}", releaseId, e.getMessage());
                    releaseStatistics.put(releaseId, Map.of(
                            "error", "Failed to analyze: " + e.getMessage()
                    ));
                }
            }
            
            double overallReusePercentage = totalFiles > 0 ? 
                    (double) totalReusableFiles / totalFiles * 100.0 : 0.0;
            
            overallMetrics.put("totalReleases", totalReleases);
            overallMetrics.put("processedReleases", processedReleases);
            overallMetrics.put("totalFiles", totalFiles);
            overallMetrics.put("totalReusableFiles", totalReusableFiles);
            
            statistics.put("overallMetrics", overallMetrics);
            statistics.put("overallReusePercentage", Math.round(overallReusePercentage * 100.0) / 100.0);
            statistics.put("releaseStatistics", releaseStatistics);
            statistics.put("request", statisticsRequest);
            
            // Generate insights
            List<String> insights = generateReuseInsights(overallReusePercentage, totalReleases, processedReleases);
            statistics.put("insights", insights);
            
            long endTime = System.currentTimeMillis();
            statistics.put("statisticsMetadata", Map.of(
                    "generatedBy", sw360User.getEmail(),
                    "generatedAt", System.currentTimeMillis(),
                    "processingTimeMs", endTime - startTime,
                    "successful", true
            ));

            return new ResponseEntity<>(statistics, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error generating comprehensive reuse statistics: {}", e.getMessage(), e);
            statistics.put("error", "Statistics generation failed: " + e.getMessage());
            statistics.put("statisticsMetadata", Map.of(
                    "generatedBy", sw360User.getEmail(),
                    "generatedAt", System.currentTimeMillis(),
                    "processingTimeMs", System.currentTimeMillis() - startTime,
                    "successful", false
            ));
            return new ResponseEntity<>(statistics, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get reuse candidates for a project.",
            description = "Find reuse candidates across all releases in a project.",
            tags = {"Reuse"}
    )
    @GetMapping(value = REUSE_URL + "/projects/{projectId}/candidates", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<Map<String, Object>> getProjectReuseCandidates(
            @Parameter(description = "ID of the project to analyze")
            @PathVariable("projectId") String projectId,
            @Parameter(description = "Maximum number of candidates per release")
            @RequestParam(defaultValue = "50") int maxCandidatesPerRelease
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);

        Map<String, Object> candidatesResult = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            Project project = projectService.getProjectForUserById(projectId, sw360User);
            
            if (project.getReleaseIdToUsage() == null || project.getReleaseIdToUsage().isEmpty()) {
                candidatesResult.put("message", "Project has no releases");
                candidatesResult.put("candidates", Collections.emptyMap());
                return new ResponseEntity<>(candidatesResult, HttpStatus.OK);
            }

            Map<String, Object> projectCandidates = new HashMap<>();
            int totalCandidatesFound = 0;
            
            // Analyze each release in the project
            for (String releaseId : project.getReleaseIdToUsage().keySet()) {
                try {
                    Release release = releaseService.getReleaseForUserById(releaseId, sw360User);
                    Map<String, List<Integer>> releaseCandidates = findReuseCandidatesFromFossology(releaseId);
                    
                    // Limit candidates per release
                    Map<String, List<Integer>> limitedCandidates = releaseCandidates.entrySet().stream()
                            .limit(maxCandidatesPerRelease)
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue
                            ));
                    
                    projectCandidates.put(releaseId, Map.of(
                            "releaseName", release.getName(),
                            "releaseVersion", release.getVersion(),
                            "candidates", limitedCandidates,
                            "candidateCount", limitedCandidates.size()
                    ));
                    
                    totalCandidatesFound += limitedCandidates.size();
                    
                } catch (Exception e) {
                    log.warn("Failed to get candidates for release {}: {}", releaseId, e.getMessage());
                    projectCandidates.put(releaseId, Map.of(
                            "error", "Failed to analyze: " + e.getMessage()
                    ));
                }
            }
            
            candidatesResult.put("projectId", projectId);
            candidatesResult.put("projectName", project.getName());
            candidatesResult.put("totalReleases", project.getReleaseIdToUsage().size());
            candidatesResult.put("totalCandidatesFound", totalCandidatesFound);
            candidatesResult.put("candidates", projectCandidates);
            candidatesResult.put("configuration", Map.of(
                    "maxCandidatesPerRelease", maxCandidatesPerRelease
            ));
            
            long endTime = System.currentTimeMillis();
            candidatesResult.put("analysisMetadata", Map.of(
                    "analyzedBy", sw360User.getEmail(),
                    "analyzedAt", System.currentTimeMillis(),
                    "processingTimeMs", endTime - startTime,
                    "successful", true
            ));

            return new ResponseEntity<>(candidatesResult, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error getting project reuse candidates for {}: {}", projectId, e.getMessage(), e);
            candidatesResult.put("error", "Candidate analysis failed: " + e.getMessage());
            candidatesResult.put("analysisMetadata", Map.of(
                    "analyzedBy", sw360User.getEmail(),
                    "analyzedAt", System.currentTimeMillis(),
                    "processingTimeMs", System.currentTimeMillis() - startTime,
                    "successful", false
            ));
            return new ResponseEntity<>(candidatesResult, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Validate reuse configuration.",
            description = "Validate reuse configuration before applying clearing decision inheritance.",
            tags = {"Reuse"}
    )
    @PostMapping(value = REUSE_URL + "/validate", 
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<Map<String, Object>> validateReuseConfiguration(
            @Parameter(description = "Reuse configuration to validate")
            @RequestBody ReuseValidationRequest validationRequest
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);

        Map<String, Object> validationResult = new HashMap<>();

        try {
            List<String> validationErrors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            
            // Validate target release
            if (validationRequest.getTargetReleaseId() == null || validationRequest.getTargetReleaseId().isEmpty()) {
                validationErrors.add("Target release ID is required");
            } else {
                try {
                    Release targetRelease = releaseService.getReleaseForUserById(validationRequest.getTargetReleaseId(), sw360User);
                    validationResult.put("targetRelease", Map.of(
                            "id", targetRelease.getId(),
                            "name", targetRelease.getName(),
                            "version", targetRelease.getVersion(),
                            "accessible", true
                    ));
                } catch (Exception e) {
                    validationErrors.add("Target release not accessible: " + e.getMessage());
                }
            }
            
            // Validate source upload ID if provided
            if (validationRequest.getSourceUploadId() != null) {
                try {
                    String uploadIdStr = validationRequest.getSourceUploadId().toString().trim();
                    if (uploadIdStr.equalsIgnoreCase("string") || uploadIdStr.isEmpty()) {
                        validationErrors.add("Source upload ID must be a valid integer (e.g., 12345). Use 0 for global reuse.");
                    } else {
                        int sourceUploadId = Integer.parseInt(uploadIdStr);
                        if (sourceUploadId < 0) {
                            warnings.add("Negative source upload ID will use global reuse (0)");
                        }
                    }
                } catch (NumberFormatException e) {
                    validationErrors.add("Source upload ID must be a valid integer (e.g., 12345). Use 0 for global reuse.");
                }
            }
            
            // Validate reuse options
            ReuseOptions options = validationRequest.getReuseOptions();
            if (options != null) {
                if (options.getReuseMain() == null) {
                    warnings.add("Main license reuse not specified - will default to true");
                }
                if (options.getReuseEnhanced() == null) {
                    warnings.add("Enhanced reuse not specified - will default to true");
                }
                if (options.getReuseCopyright() == null) {
                    warnings.add("Copyright reuse not specified - will default to true");
                }
            } else {
                warnings.add("Reuse options not specified - will use default settings");
            }
            
            // Validate user permissions
            if (!hasReusePermissions(sw360User)) {
                validationErrors.add("User does not have sufficient permissions for clearing decision reuse");
            }
            
            boolean isValid = validationErrors.isEmpty();
            String recommendation = generateValidationRecommendation(isValid, warnings.size());
            
            validationResult.put("valid", isValid);
            validationResult.put("recommendation", recommendation);
            validationResult.put("validationErrors", validationErrors);
            validationResult.put("warnings", warnings);
            validationResult.put("request", validationRequest);
            validationResult.put("validatedBy", sw360User.getEmail());
            validationResult.put("validatedAt", System.currentTimeMillis());

            return new ResponseEntity<>(validationResult, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error validating reuse configuration: {}", e.getMessage(), e);
            validationResult.put("error", "Validation failed: " + e.getMessage());
            return new ResponseEntity<>(validationResult, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Helper methods

    private Map<String, Object> analyzeAttachmentReuse(Release release, boolean includeFileDetails) {
        Map<String, Object> analysis = new HashMap<>();
        
        if (release.getAttachments() == null || release.getAttachments().isEmpty()) {
            analysis.put("totalAttachments", 0);
            analysis.put("attachmentsWithChecksums", 0);
            analysis.put("attachmentDetails", Collections.emptyList());
            return analysis;
        }
        
        int totalAttachments = release.getAttachments().size();
        int attachmentsWithChecksums = 0;
        List<Map<String, Object>> attachmentDetails = new ArrayList<>();
        
        for (var attachment : release.getAttachments()) {
            boolean hasChecksum = attachment.getSha1() != null && !attachment.getSha1().isEmpty();
            if (hasChecksum) {
                attachmentsWithChecksums++;
            }
            
            if (includeFileDetails) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("attachmentId", attachment.getAttachmentContentId());
                detail.put("filename", attachment.getFilename());
                detail.put("hasChecksum", hasChecksum);
                detail.put("sha1", attachment.getSha1());
                detail.put("attachmentType", attachment.getAttachmentType());
                attachmentDetails.add(detail);
            }
        }
        
        analysis.put("totalAttachments", totalAttachments);
        analysis.put("attachmentsWithChecksums", attachmentsWithChecksums);
        analysis.put("checksumCoverage", totalAttachments > 0 ? 
                Math.round((double) attachmentsWithChecksums / totalAttachments * 100.0 * 100.0) / 100.0 : 0.0);
        
        if (includeFileDetails) {
            analysis.put("attachmentDetails", attachmentDetails);
        }
        
        return analysis;
    }

    private List<String> generateReuseRecommendations(double reusePercentage, int totalFiles, int reusableFiles) {
        List<String> recommendations = new ArrayList<>();
        
        if (totalFiles == 0) {
            recommendations.add("No files found in release - upload source attachments for analysis");
            return recommendations;
        }
        
        if (reusePercentage >= 75.0) {
            recommendations.add("Excellent reuse potential - strongly recommend applying FOSSology reuser agent");
            recommendations.add("Consider establishing this release as a reuse baseline for similar components");
        } else if (reusePercentage >= 50.0) {
            recommendations.add("Good reuse potential - recommend applying clearing decision inheritance");
            recommendations.add("Review non-reusable files for unique clearing requirements");
        } else if (reusePercentage >= 25.0) {
            recommendations.add("Moderate reuse potential - selective reuse may be beneficial");
            recommendations.add("Focus reuse efforts on high-confidence matches");
        } else if (reusePercentage > 0.0) {
            recommendations.add("Limited reuse potential - manual clearing may be more efficient");
            recommendations.add("Consider building reuse potential by clearing similar components first");
        } else {
            recommendations.add("No immediate reuse potential found");
            recommendations.add("This release may serve as a future reuse source after clearing");
        }
        
        if (reusableFiles > 20) {
            recommendations.add("Large number of reusable files - batch processing recommended");
        }
        
        return recommendations;
    }

    private List<String> generateReuseInsights(double overallReusePercentage, int totalReleases, int processedReleases) {
        List<String> insights = new ArrayList<>();
        
        if (processedReleases < totalReleases) {
            insights.add(String.format("Analysis completed for %d out of %d releases - some data may be incomplete", 
                                     processedReleases, totalReleases));
        }
        
        if (overallReusePercentage >= 60.0) {
            insights.add("High overall reuse potential across analyzed releases");
            insights.add("Organization shows good reuse practices and component similarity");
        } else if (overallReusePercentage >= 30.0) {
            insights.add("Moderate reuse potential - opportunities for improvement exist");
            insights.add("Consider standardizing on common components and libraries");
        } else {
            insights.add("Low overall reuse potential - may indicate diverse technology stack");
            insights.add("Focus on building reuse baselines for commonly used components");
        }
        
        if (totalReleases >= 20) {
            insights.add("Large release dataset provides good statistical confidence");
        } else if (totalReleases < 5) {
            insights.add("Small dataset - results may not be representative of overall patterns");
        }
        
        return insights;
    }

    private List<String> validateReuseRequest(ReuseApplicationRequest request) {
        List<String> errors = new ArrayList<>();
        
        if (request.getTargetReleaseId() == null || request.getTargetReleaseId().isEmpty()) {
            errors.add("Target release ID is required");
        }
        
        if (request.getSourceUploadId() != null) {
            try {
                String uploadIdStr = request.getSourceUploadId().toString().trim();
                if (uploadIdStr.equalsIgnoreCase("string") || uploadIdStr.isEmpty()) {
                    errors.add("Source upload ID must be a valid integer (e.g., 12345). Use 0 for global reuse.");
                } else {
                    int uploadId = Integer.parseInt(uploadIdStr);
                    if (uploadId < 0) {
                        errors.add("Source upload ID must be non-negative. Use 0 for global reuse.");
                    }
                }
            } catch (NumberFormatException e) {
                errors.add("Source upload ID must be a valid integer (e.g., 12345). Use 0 for global reuse.");
            }
        }
        
        return errors;
    }

    private boolean hasReusePermissions(User user) {
        return user.getUserGroup() != null && 
               (user.getUserGroup().toString().contains("ADMIN") || 
                user.getUserGroup().toString().contains("CLEARING"));
    }

    private String generateValidationRecommendation(boolean isValid, int warningCount) {
        if (!isValid) {
            return "Configuration invalid - resolve errors before proceeding";
        } else if (warningCount > 0) {
            return "Configuration valid with warnings - review recommended";
        } else {
            return "Configuration valid - safe to proceed with reuse application";
        }
    }

    // FOSSology Integration Methods

    private Map<String, Object> getReuseStatisticsFromFossology(String releaseId) {
        Map<String, Object> statistics = new HashMap<>();
        
        try {
            Release release = releaseService.getReleaseForUserById(releaseId, restControllerHelper.getSw360UserFromAuthentication());
            
            if (release.getAttachments() == null || release.getAttachments().isEmpty()) {
                statistics.put("totalFiles", 0);
                statistics.put("reusableFiles", 0);
                statistics.put("reusePercentage", 0.0);
                return statistics;
            }

            // Get upload IDs from attachments
            List<String> uploadIds = new ArrayList<>();
            for (Attachment attachment : release.getAttachments()) {
                if (!CommonUtils.isNullEmptyOrWhitespace(attachment.getFossologyUploadId())) {
                    uploadIds.add(attachment.getFossologyUploadId());
                }
            }

            if (uploadIds.isEmpty()) {
                statistics.put("totalFiles", release.getAttachments().size());
                statistics.put("reusableFiles", 0);
                statistics.put("reusePercentage", 0.0);
                statistics.put("warning", "No FOSSology uploads found for this release");
                return statistics;
            }

            // Query FOSSology for reuse statistics
            int totalFiles = release.getAttachments().size();
            int reusableFiles = 0;

            for (String uploadId : uploadIds) {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(fossologyToken);
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<String> entity = new HttpEntity<>(headers);

                    String url = fossologyBaseUrl + "/api/v2/uploads/" + uploadId + "/reuse";
                    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

                    if (response.getStatusCode().is2xxSuccessful()) {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode responseNode = mapper.readTree(response.getBody());
                        
                        if (responseNode.has("reusableCount")) {
                            reusableFiles += responseNode.get("reusableCount").asInt();
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to get reuse statistics for upload {}: {}", uploadId, e.getMessage());
                }
            }

            double reusePercentage = totalFiles > 0 ? (double) reusableFiles / totalFiles * 100.0 : 0.0;

            statistics.put("totalFiles", totalFiles);
            statistics.put("reusableFiles", reusableFiles);
            statistics.put("reusePercentage", Math.round(reusePercentage * 100.0) / 100.0);
            statistics.put("uploadsAnalyzed", uploadIds.size());

        } catch (Exception e) {
            log.error("Error getting reuse statistics from FOSSology for release {}: {}", releaseId, e.getMessage());
            statistics.put("error", "Failed to get reuse statistics: " + e.getMessage());
        }

        return statistics;
    }

    private Map<String, List<Integer>> findReuseCandidatesFromFossology(String releaseId) {
        Map<String, List<Integer>> candidates = new HashMap<>();
        
        try {
            Release release = releaseService.getReleaseForUserById(releaseId, restControllerHelper.getSw360UserFromAuthentication());
            
            if (release.getAttachments() == null || release.getAttachments().isEmpty()) {
                return candidates;
            }

            // Get upload IDs from attachments
            List<String> uploadIds = new ArrayList<>();
            for (Attachment attachment : release.getAttachments()) {
                if (!CommonUtils.isNullEmptyOrWhitespace(attachment.getFossologyUploadId())) {
                    uploadIds.add(attachment.getFossologyUploadId());
                }
            }

            if (uploadIds.isEmpty()) {
                return candidates;
            }

            // Query FOSSology for reuse candidates
            for (String uploadId : uploadIds) {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(fossologyToken);
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<String> entity = new HttpEntity<>(headers);

                    String url = fossologyBaseUrl + "/api/v2/uploads/" + uploadId + "/reuse/candidates";
                    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

                    if (response.getStatusCode().is2xxSuccessful()) {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode responseNode = mapper.readTree(response.getBody());
                        
                        if (responseNode.isArray()) {
                            List<Integer> candidateIds = new ArrayList<>();
                            for (JsonNode candidateNode : responseNode) {
                                if (candidateNode.has("candidateUploadId")) {
                                    candidateIds.add(candidateNode.get("candidateUploadId").asInt());
                                }
                            }
                            if (!candidateIds.isEmpty()) {
                                candidates.put(uploadId, candidateIds);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to get reuse candidates for upload {}: {}", uploadId, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error finding reuse candidates from FOSSology for release {}: {}", releaseId, e.getMessage());
        }

        return candidates;
    }

    private boolean enableReuseForReleaseViaFossology(String releaseId, int sourceUploadId) {
        try {
            Release release = releaseService.getReleaseForUserById(releaseId, restControllerHelper.getSw360UserFromAuthentication());
            
            if (release.getAttachments() == null || release.getAttachments().isEmpty()) {
                return false;
            }

            // Get target upload IDs from attachments
            List<String> targetUploadIds = new ArrayList<>();
            for (Attachment attachment : release.getAttachments()) {
                if (!CommonUtils.isNullEmptyOrWhitespace(attachment.getFossologyUploadId())) {
                    targetUploadIds.add(attachment.getFossologyUploadId());
                }
            }

            if (targetUploadIds.isEmpty()) {
                log.warn("No FOSSology uploads found for release {}", releaseId);
                return false;
            }

            boolean allSuccessful = true;

            // Apply reuse for each upload in the release
            for (String targetUploadId : targetUploadIds) {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(fossologyToken);
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    Map<String, Object> reuseRequest = new HashMap<>();
                    reuseRequest.put("targetUploadId", Integer.parseInt(targetUploadId));
                    if (sourceUploadId > 0) {
                        reuseRequest.put("sourceUploadId", sourceUploadId);
                    }
                    reuseRequest.put("reuseMain", true);
                    reuseRequest.put("reuseEnhanced", true);
                    reuseRequest.put("reuseCopyright", true);

                    ObjectMapper mapper = new ObjectMapper();
                    String requestBody = mapper.writeValueAsString(reuseRequest);
                    HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

                    String url = fossologyBaseUrl + "/api/v2/uploads/" + targetUploadId + "/reuse";
                    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

                    if (!response.getStatusCode().is2xxSuccessful()) {
                        log.warn("Failed to enable reuse for upload {}: HTTP {}", targetUploadId, response.getStatusCode());
                        allSuccessful = false;
                    } else {
                        log.info("Successfully enabled reuse for upload {}", targetUploadId);
                    }

                } catch (Exception e) {
                    log.error("Error enabling reuse for upload {}: {}", targetUploadId, e.getMessage());
                    allSuccessful = false;
                }
            }

            return allSuccessful;

        } catch (Exception e) {
            log.error("Error enabling reuse for release {}: {}", releaseId, e.getMessage());
            return false;
        }
    }

    // Request/Response DTOs

    @Schema(description = "Request for applying clearing decision reuse")
    public static class ReuseApplicationRequest {
        @Schema(description = "Target release ID for reuse application", 
                example = "target-release-id",
                required = true)
        private String targetReleaseId;
        
        @Schema(description = "Source upload ID for reuse (integer value, use 0 for global reuse)", 
                type = "integer",
                example = "12345")
        private Object sourceUploadId;
        
        @Schema(description = "Reuse options configuration")
        private ReuseOptions reuseOptions;
        
        @Schema(description = "Optional comment for the reuse application", 
                example = "Applying reuse for license clearing acceleration")
        private String comment;

        // Getters and setters
        public String getTargetReleaseId() { return targetReleaseId; }
        public void setTargetReleaseId(String targetReleaseId) { this.targetReleaseId = targetReleaseId; }

        public Object getSourceUploadId() { return sourceUploadId; }
        public void setSourceUploadId(Object sourceUploadId) { this.sourceUploadId = sourceUploadId; }

        public ReuseOptions getReuseOptions() { return reuseOptions; }
        public void setReuseOptions(ReuseOptions reuseOptions) { this.reuseOptions = reuseOptions; }

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }

    @Schema(description = "Reuse configuration options")
    public static class ReuseOptions {
        @Schema(description = "Enable main license reuse", 
                example = "true", 
                defaultValue = "true")
        private Boolean reuseMain;
        
        @Schema(description = "Enable enhanced reuse analysis", 
                example = "true", 
                defaultValue = "true")
        private Boolean reuseEnhanced;
        
        @Schema(description = "Enable copyright reuse", 
                example = "true", 
                defaultValue = "true")
        private Boolean reuseCopyright;
        
        @Schema(description = "Generate reuse report", 
                example = "false", 
                defaultValue = "false")
        private Boolean reuseReport;

        // Getters and setters
        public Boolean getReuseMain() { return reuseMain; }
        public void setReuseMain(Boolean reuseMain) { this.reuseMain = reuseMain; }

        public Boolean getReuseEnhanced() { return reuseEnhanced; }
        public void setReuseEnhanced(Boolean reuseEnhanced) { this.reuseEnhanced = reuseEnhanced; }

        public Boolean getReuseCopyright() { return reuseCopyright; }
        public void setReuseCopyright(Boolean reuseCopyright) { this.reuseCopyright = reuseCopyright; }

        public Boolean getReuseReport() { return reuseReport; }
        public void setReuseReport(Boolean reuseReport) { this.reuseReport = reuseReport; }
    }

    @Schema(description = "Request for comprehensive reuse statistics")
    public static class ReuseStatisticsRequest {
        @Schema(description = "List of release IDs to analyze", 
                example = "[\"release-id-1\", \"release-id-2\"]",
                required = true)
        private List<String> releaseIds;
        
        @Schema(description = "Optional project ID for project-wide analysis", 
                example = "project-id")
        private String projectId;
        
        @Schema(description = "Include detailed file analysis in results", 
                example = "true", 
                defaultValue = "false")
        private Boolean includeFileDetails;
        
        @Schema(description = "Maximum number of results to return (0 for unlimited)", 
                example = "100", 
                defaultValue = "0")
        private Integer maxResults;

        // Getters and setters
        public List<String> getReleaseIds() { return releaseIds; }
        public void setReleaseIds(List<String> releaseIds) { this.releaseIds = releaseIds; }

        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }

        public Boolean getIncludeFileDetails() { return includeFileDetails; }
        public void setIncludeFileDetails(Boolean includeFileDetails) { this.includeFileDetails = includeFileDetails; }

        public Integer getMaxResults() { return maxResults; }
        public void setMaxResults(Integer maxResults) { this.maxResults = maxResults; }
    }

    @Schema(description = "Request for validating reuse configuration")
    public static class ReuseValidationRequest {
        @Schema(description = "Target release ID for reuse application", 
                example = "target-release-id",
                required = true)
        private String targetReleaseId;
        
        @Schema(description = "Source upload ID for reuse (integer value, use 0 for global reuse)", 
                type = "integer",
                example = "12345")
        private Object sourceUploadId;
        
        @Schema(description = "Reuse options configuration")
        private ReuseOptions reuseOptions;

        // Getters and setters
        public String getTargetReleaseId() { return targetReleaseId; }
        public void setTargetReleaseId(String targetReleaseId) { this.targetReleaseId = targetReleaseId; }

        public Object getSourceUploadId() { return sourceUploadId; }
        public void setSourceUploadId(Object sourceUploadId) { this.sourceUploadId = sourceUploadId; }

        public ReuseOptions getReuseOptions() { return reuseOptions; }
        public void setReuseOptions(ReuseOptions reuseOptions) { this.reuseOptions = reuseOptions; }
    }
}