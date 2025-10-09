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
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.*;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
@Tag(name = "Reuse Jobs", description = "Background job operations for reuse processing")
public class ReuseJobController {

    public static final String REUSE_JOBS_URL = "/reuse/jobs";

    @NonNull
    private final RestControllerHelper restControllerHelper;

    // Job management
    private final Map<String, JobExecutionStatus> activeJobs = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Operation(
            summary = "Submit bulk reuse analysis job.",
            description = "Submit a background job to analyze reuse potential for multiple releases.",
            tags = {"Reuse Jobs"}
    )
    @PostMapping(value = REUSE_JOBS_URL + "/bulk-analysis", 
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('WRITE')")
    public ResponseEntity<Map<String, Object>> submitBulkReuseAnalysis(
            @Parameter(description = "Bulk reuse analysis request")
            @RequestBody BulkReuseAnalysisRequest request
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate request
            if (request.getReleaseIds() == null || request.getReleaseIds().isEmpty()) {
                response.put("error", "No release IDs provided");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Create job
            String jobId = generateJobId("bulk-reuse-analysis");
            JobExecutionStatus jobStatus = new JobExecutionStatus(jobId, JobType.BULK_REUSE_ANALYSIS);
            jobStatus.setTotalItems(request.getReleaseIds().size());
            jobStatus.setSubmittedBy(sw360User.getEmail());
            activeJobs.put(jobId, jobStatus);

            // Submit job for execution
            CompletableFuture.runAsync(() -> {
                executeBulkReuseAnalysis(jobId, request, sw360User);
            }, executorService);

            response.put("jobId", jobId);
            response.put("status", "submitted");
            response.put("message", "Bulk reuse analysis job submitted successfully");
            response.put("releaseCount", request.getReleaseIds().size());
            response.put("submittedBy", sw360User.getEmail());
            response.put("submittedAt", System.currentTimeMillis());

            return new ResponseEntity<>(response, HttpStatus.ACCEPTED);

        } catch (Exception e) {
            log.error("Error submitting bulk reuse analysis job: {}", e.getMessage(), e);
            response.put("error", "Failed to submit job: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Submit duplicate detection job.",
            description = "Submit a background job to detect duplicates across multiple releases.",
            tags = {"Reuse Jobs"}
    )
    @PostMapping(value = REUSE_JOBS_URL + "/duplicate-detection", 
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('WRITE')")
    public ResponseEntity<Map<String, Object>> submitDuplicateDetectionJob(
            @Parameter(description = "Duplicate detection request")
            @RequestBody DuplicateDetectionRequest request
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate request
            if (request.getReleaseIds() == null || request.getReleaseIds().isEmpty()) {
                response.put("error", "No release IDs provided");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Create job
            String jobId = generateJobId("duplicate-detection");
            JobExecutionStatus jobStatus = new JobExecutionStatus(jobId, JobType.DUPLICATE_DETECTION);
            jobStatus.setTotalItems(request.getReleaseIds().size());
            jobStatus.setSubmittedBy(sw360User.getEmail());
            activeJobs.put(jobId, jobStatus);

            // Submit job for execution
            CompletableFuture.runAsync(() -> {
                executeDuplicateDetection(jobId, request, sw360User);
            }, executorService);

            response.put("jobId", jobId);
            response.put("status", "submitted");
            response.put("message", "Duplicate detection job submitted successfully");
            response.put("releaseCount", request.getReleaseIds().size());
            response.put("submittedBy", sw360User.getEmail());
            response.put("submittedAt", System.currentTimeMillis());

            return new ResponseEntity<>(response, HttpStatus.ACCEPTED);

        } catch (Exception e) {
            log.error("Error submitting duplicate detection job: {}", e.getMessage(), e);
            response.put("error", "Failed to submit job: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get job status.",
            description = "Get the current status and progress of a background reuse job.",
            tags = {"Reuse Jobs"}
    )
    @GetMapping(value = REUSE_JOBS_URL + "/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<Map<String, Object>> getJobStatus(
            @Parameter(description = "ID of the job to check")
            @PathVariable("jobId") String jobId
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);

        Map<String, Object> response = new HashMap<>();

        try {
            JobExecutionStatus status = activeJobs.get(jobId);

            if (status == null) {
                response.put("error", "Job not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            // Check if user can access this job
            if (!sw360User.getEmail().equals(status.getSubmittedBy()) && 
                !hasAdminAccess(sw360User)) {
                response.put("error", "Access denied - not authorized to view this job");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

            response.put("jobId", status.getJobId());
            response.put("jobType", status.getJobType().toString());
            response.put("status", status.getJobStatus().toString());
            response.put("statusMessage", status.getStatusMessage());
            response.put("submittedBy", status.getSubmittedBy());
            response.put("createdTime", status.getCreatedTime());
            response.put("startTime", status.getStartTime());
            response.put("endTime", status.getEndTime());
            response.put("totalItems", status.getTotalItems());
            response.put("processedItems", status.getProcessedItems());
            response.put("progressPercentage", status.getProgressPercentage());
            response.put("results", status.getResults());

            // Calculate duration if applicable
            Long duration = null;
            if (status.getStartTime() != null) {
                long endTime = status.getEndTime() != null ? status.getEndTime() : System.currentTimeMillis();
                duration = endTime - status.getStartTime();
            }
            response.put("durationMs", duration);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error getting job status for {}: {}", jobId, e.getMessage(), e);
            response.put("error", "Failed to get job status: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get user's jobs.",
            description = "Get all background reuse jobs submitted by the current user.",
            tags = {"Reuse Jobs"}
    )
    @GetMapping(value = REUSE_JOBS_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<Map<String, Object>> getUserJobs() throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);

        Map<String, Object> response = new HashMap<>();

        try {
            List<JobExecutionStatus> userJobs = new ArrayList<>();
            for (JobExecutionStatus status : activeJobs.values()) {
                if (sw360User.getEmail().equals(status.getSubmittedBy()) || hasAdminAccess(sw360User)) {
                    userJobs.add(status);
                }
            }

            List<Map<String, Object>> jobSummaries = new ArrayList<>();
            for (JobExecutionStatus status : userJobs) {
                Map<String, Object> jobSummary = new HashMap<>();
                jobSummary.put("jobId", status.getJobId());
                jobSummary.put("jobType", status.getJobType().toString());
                jobSummary.put("status", status.getJobStatus().toString());
                jobSummary.put("statusMessage", status.getStatusMessage());
                jobSummary.put("submittedBy", status.getSubmittedBy());
                jobSummary.put("createdTime", status.getCreatedTime());
                jobSummary.put("startTime", status.getStartTime());
                jobSummary.put("endTime", status.getEndTime());
                jobSummary.put("totalItems", status.getTotalItems());
                jobSummary.put("processedItems", status.getProcessedItems());
                jobSummary.put("progressPercentage", status.getProgressPercentage());
                
                jobSummaries.add(jobSummary);
            }

            response.put("jobs", jobSummaries);
            response.put("totalJobs", jobSummaries.size());
            response.put("userEmail", sw360User.getEmail());

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error getting user jobs: {}", e.getMessage(), e);
            response.put("error", "Failed to get user jobs: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Cancel job.",
            description = "Cancel a running background reuse job.",
            tags = {"Reuse Jobs"}
    )
    @PostMapping(value = REUSE_JOBS_URL + "/{jobId}/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('WRITE')")
    public ResponseEntity<Map<String, Object>> cancelJob(
            @Parameter(description = "ID of the job to cancel")
            @PathVariable("jobId") String jobId
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);

        Map<String, Object> response = new HashMap<>();

        try {
            JobExecutionStatus status = activeJobs.get(jobId);

            if (status == null) {
                response.put("error", "Job not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            // Check if user can cancel this job
            if (!sw360User.getEmail().equals(status.getSubmittedBy()) && 
                !hasAdminAccess(sw360User)) {
                response.put("error", "Access denied - not authorized to cancel this job");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

            if (status.getJobStatus() == JobStatus.RUNNING || status.getJobStatus() == JobStatus.QUEUED) {
                status.setJobStatus(JobStatus.CANCELLED);
                status.setStatusMessage("Job cancelled by user request");
                status.setEndTime(System.currentTimeMillis());

                response.put("jobId", jobId);
                response.put("status", "cancelled");
                response.put("message", "Job cancelled successfully");
            } else {
                response.put("jobId", jobId);
                response.put("status", "not_cancelled");
                response.put("message", "Job could not be cancelled (current status: " + status.getJobStatus() + ")");
            }

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error cancelling job {}: {}", jobId, e.getMessage(), e);
            response.put("error", "Failed to cancel job: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Job execution methods

    private void executeBulkReuseAnalysis(String jobId, BulkReuseAnalysisRequest request, User user) {
        JobExecutionStatus status = activeJobs.get(jobId);
        if (status == null) return;

        try {
            status.setJobStatus(JobStatus.RUNNING);
            status.setStartTime(System.currentTimeMillis());
            status.setStatusMessage("Starting bulk reuse analysis");

            log.info("Starting bulk reuse analysis job {} for {} releases by user {}", 
                     jobId, request.getReleaseIds().size(), user.getEmail());

            int batchSize = request.getBatchSize() != null && request.getBatchSize() > 0 ? 
                           request.getBatchSize() : 50;
            
            List<List<String>> batches = partitionList(request.getReleaseIds(), batchSize);
            int processedCount = 0;
            Map<String, Object> results = new HashMap<>();

            for (int i = 0; i < batches.size(); i++) {
                if (status.getJobStatus() == JobStatus.CANCELLED) {
                    return;
                }

                List<String> batch = batches.get(i);
                log.info("Processing batch {} of {} ({} releases) for job {}", 
                         i + 1, batches.size(), batch.size(), jobId);

                // Process each release in the batch
                for (String releaseId : batch) {
                    if (status.getJobStatus() == JobStatus.CANCELLED) {
                        return;
                    }

                    try {
                        // Perform actual reuse analysis here
                        Map<String, Object> releaseResult = analyzeReleaseReuse(releaseId, user);
                        results.put(releaseId, releaseResult);
                        
                        processedCount++;
                        updateJobProgress(status, processedCount, 
                                        "Analyzed " + processedCount + " of " + request.getReleaseIds().size() + " releases");

                        // Small delay to prevent overwhelming the system
                        Thread.sleep(100);

                    } catch (Exception e) {
                        log.warn("Failed to analyze release {}: {}", releaseId, e.getMessage());
                        results.put(releaseId, Map.of("error", e.getMessage()));
                        processedCount++;
                    }
                }
            }

            // Store final results
            status.getResults().put("releaseAnalysis", results);
            status.getResults().put("totalReleases", request.getReleaseIds().size());
            status.getResults().put("successfulAnalysis", results.size());
            
            status.setJobStatus(JobStatus.COMPLETED);
            status.setStatusMessage("Bulk reuse analysis completed successfully");
            status.setEndTime(System.currentTimeMillis());

            log.info("Completed bulk reuse analysis job {} - analyzed {} releases", jobId, processedCount);

        } catch (Exception e) {
            log.error("Error executing bulk reuse analysis job {}: {}", jobId, e.getMessage(), e);
            status.setJobStatus(JobStatus.FAILED);
            status.setStatusMessage("Job failed: " + e.getMessage());
            status.setEndTime(System.currentTimeMillis());
        }
    }

    private void executeDuplicateDetection(String jobId, DuplicateDetectionRequest request, User user) {
        JobExecutionStatus status = activeJobs.get(jobId);
        if (status == null) return;

        try {
            status.setJobStatus(JobStatus.RUNNING);
            status.setStartTime(System.currentTimeMillis());
            status.setStatusMessage("Starting duplicate detection");

            log.info("Starting duplicate detection job {} for {} releases by user {}", 
                     jobId, request.getReleaseIds().size(), user.getEmail());

            Map<String, List<String>> checksumToReleases = new HashMap<>();
            int processedCount = 0;

            // Process each release
            for (String releaseId : request.getReleaseIds()) {
                if (status.getJobStatus() == JobStatus.CANCELLED) {
                    return;
                }

                try {
                    // Get checksums for this release
                    List<String> releaseChecksums = getReleaseChecksums(releaseId, user);
                    
                    // Track which releases have each checksum
                    for (String checksum : releaseChecksums) {
                        checksumToReleases.computeIfAbsent(checksum, k -> new ArrayList<>()).add(releaseId);
                    }
                    
                    processedCount++;
                    updateJobProgress(status, processedCount, 
                                    "Processed " + processedCount + " of " + request.getReleaseIds().size() + " releases");
                    
                    Thread.sleep(50);

                } catch (Exception e) {
                    log.warn("Failed to process release {} for duplicates: {}", releaseId, e.getMessage());
                    processedCount++;
                }
            }

            // Find duplicates (checksums that appear in multiple releases)
            Map<String, List<String>> duplicates = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : checksumToReleases.entrySet()) {
                if (entry.getValue().size() > 1) {
                    duplicates.put(entry.getKey(), entry.getValue());
                }
            }

            // Store results
            status.getResults().put("duplicates", duplicates);
            status.getResults().put("totalChecksums", checksumToReleases.size());
            status.getResults().put("duplicateChecksums", duplicates.size());
            status.getResults().put("releasesProcessed", processedCount);

            status.setJobStatus(JobStatus.COMPLETED);
            status.setStatusMessage("Duplicate detection completed - found " + duplicates.size() + " duplicate checksums");
            status.setEndTime(System.currentTimeMillis());

            log.info("Completed duplicate detection job {} - found {} duplicates across {} releases", 
                     jobId, duplicates.size(), processedCount);

        } catch (Exception e) {
            log.error("Error executing duplicate detection job {}: {}", jobId, e.getMessage(), e);
            status.setJobStatus(JobStatus.FAILED);
            status.setStatusMessage("Job failed: " + e.getMessage());
            status.setEndTime(System.currentTimeMillis());
        }
    }

    // Helper methods

    private String generateJobId(String jobType) {
        return jobType + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void updateJobProgress(JobExecutionStatus status, int processedItems, String message) {
        status.setProcessedItems(processedItems);
        status.setStatusMessage(message);
        
        if (status.getTotalItems() > 0) {
            double progress = (double) processedItems / status.getTotalItems() * 100.0;
            status.setProgressPercentage(Math.round(progress * 100.0) / 100.0);
        }
    }

    private boolean hasAdminAccess(User user) {
        return user.getUserGroup() != null && 
               (user.getUserGroup().toString().contains("ADMIN") || 
                user.getUserGroup().toString().contains("CLEARING_ADMIN"));
    }

    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return partitions;
    }

    private Map<String, Object> analyzeReleaseReuse(String releaseId, User user) {
        // This would integrate with actual reuse analysis services
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("releaseId", releaseId);
        analysis.put("totalFiles", 0);
        analysis.put("reusableFiles", 0);
        analysis.put("reusePercentage", 0.0);
        analysis.put("analyzedAt", System.currentTimeMillis());
        
        // Simulate some processing time
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return analysis;
    }

    private List<String> getReleaseChecksums(String releaseId, User user) {
        // This would integrate with actual checksum retrieval services
        List<String> checksums = new ArrayList<>();
        
        // Simulate getting checksums for a release
        Random random = new Random(releaseId.hashCode());
        int numChecksums = random.nextInt(10) + 1;
        
        for (int i = 0; i < numChecksums; i++) {
            // Generate pseudo-random checksums based on release ID
            String checksum = "sha1-" + releaseId + "-" + i + "-" + random.nextInt(1000);
            checksums.add(checksum);
        }
        
        return checksums;
    }

    // Supporting classes and enums

    public enum JobType {
        BULK_REUSE_ANALYSIS,
        DUPLICATE_DETECTION,
        CLEARING_INHERITANCE,
        REUSE_STATISTICS
    }

    public enum JobStatus {
        QUEUED,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    public static class JobExecutionStatus {
        private final String jobId;
        private final JobType jobType;
        private final long createdTime;
        private JobStatus jobStatus;
        private String statusMessage;
        private String submittedBy;
        private Long startTime;
        private Long endTime;
        private int totalItems;
        private int processedItems;
        private double progressPercentage;
        private final Map<String, Object> results = new HashMap<>();

        public JobExecutionStatus(String jobId, JobType jobType) {
            this.jobId = jobId;
            this.jobType = jobType;
            this.createdTime = System.currentTimeMillis();
            this.jobStatus = JobStatus.QUEUED;
        }

        // Getters and setters
        public String getJobId() { return jobId; }
        public JobType getJobType() { return jobType; }
        public long getCreatedTime() { return createdTime; }
        public JobStatus getJobStatus() { return jobStatus; }
        public void setJobStatus(JobStatus jobStatus) { this.jobStatus = jobStatus; }
        public String getStatusMessage() { return statusMessage; }
        public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
        public String getSubmittedBy() { return submittedBy; }
        public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
        public Long getStartTime() { return startTime; }
        public void setStartTime(Long startTime) { this.startTime = startTime; }
        public Long getEndTime() { return endTime; }
        public void setEndTime(Long endTime) { this.endTime = endTime; }
        public int getTotalItems() { return totalItems; }
        public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
        public int getProcessedItems() { return processedItems; }
        public void setProcessedItems(int processedItems) { this.processedItems = processedItems; }
        public double getProgressPercentage() { return progressPercentage; }
        public void setProgressPercentage(double progressPercentage) { this.progressPercentage = progressPercentage; }
        public Map<String, Object> getResults() { return results; }
    }

    // Request DTOs

    @Schema(description = "Request for bulk reuse analysis job")
    public static class BulkReuseAnalysisRequest {
        @Schema(description = "List of release IDs to analyze for reuse", 
                example = "[\"release-id-1\", \"release-id-2\"]",
                required = true)
        private List<String> releaseIds;
        
        @Schema(description = "Batch size for processing releases", 
                example = "50", 
                defaultValue = "50")
        private Integer batchSize;
        
        @Schema(description = "Include detailed file analysis in results", 
                example = "true", 
                defaultValue = "false")
        private Boolean includeFileDetails;
        
        @Schema(description = "Generate comprehensive report", 
                example = "false", 
                defaultValue = "false")
        private Boolean generateReport;

        // Getters and setters
        public List<String> getReleaseIds() { return releaseIds; }
        public void setReleaseIds(List<String> releaseIds) { this.releaseIds = releaseIds; }
        public Integer getBatchSize() { return batchSize; }
        public void setBatchSize(Integer batchSize) { this.batchSize = batchSize; }
        public Boolean getIncludeFileDetails() { return includeFileDetails; }
        public void setIncludeFileDetails(Boolean includeFileDetails) { this.includeFileDetails = includeFileDetails; }
        public Boolean getGenerateReport() { return generateReport; }
        public void setGenerateReport(Boolean generateReport) { this.generateReport = generateReport; }
    }

    @Schema(description = "Request for duplicate detection job")
    public static class DuplicateDetectionRequest {
        @Schema(description = "List of release IDs to analyze for duplicates", 
                example = "[\"release-id-1\", \"release-id-2\"]",
                required = true)
        private List<String> releaseIds;
        
        @Schema(description = "List of checksum types to analyze", 
                example = "[\"SHA1\", \"MD5\", \"SHA256\"]",
                defaultValue = "[\"SHA1\"]")
        private List<String> checksumTypes;
        
        @Schema(description = "Enable cross-component duplicate analysis", 
                example = "true", 
                defaultValue = "false")
        private Boolean crossComponentAnalysis;

        // Getters and setters
        public List<String> getReleaseIds() { return releaseIds; }
        public void setReleaseIds(List<String> releaseIds) { this.releaseIds = releaseIds; }
        public List<String> getChecksumTypes() { return checksumTypes; }
        public void setChecksumTypes(List<String> checksumTypes) { this.checksumTypes = checksumTypes; }
        public Boolean getCrossComponentAnalysis() { return crossComponentAnalysis; }
        public void setCrossComponentAnalysis(Boolean crossComponentAnalysis) { this.crossComponentAnalysis = crossComponentAnalysis; }
    }
}