/*
 * Copyright Ritankar Saha<ritankar.saha786@gmail.com>, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.jobs;

import org.eclipse.sw360.datahandler.db.ChecksumRepository;
import org.eclipse.sw360.datahandler.services.reuse.ReuseManager;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Background job for processing reuse operations asynchronously.
 * Handles bulk reuse analysis, duplicate detection, and clearing decision inheritance
 * for multiple releases or projects.
 */
public class ReuseProcessingJob {

    private static final Logger log = LoggerFactory.getLogger(ReuseProcessingJob.class);

    // Job execution configuration
    private static final int DEFAULT_THREAD_POOL_SIZE = 5;
    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final long DEFAULT_TIMEOUT_MINUTES = 30;

    // Job processing statistics
    private final Map<String, JobExecutionStatus> runningJobs = new ConcurrentHashMap<>();
    private final ExecutorService executorService;
    private final ReuseManager reuseManager;
    private final ChecksumRepository checksumRepository;

    public ReuseProcessingJob(ChecksumRepository checksumRepository) {
        this.checksumRepository = checksumRepository;
        this.reuseManager = new ReuseManager(checksumRepository);
        this.executorService = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE);
    }

    /**
     * Submit a bulk reuse analysis job for multiple releases
     */
    public String submitBulkReuseAnalysis(List<String> releaseIds, User user, BulkReuseOptions options) {
        String jobId = generateJobId("bulk-reuse-analysis");
        
        log.info("Submitting bulk reuse analysis job {} for {} releases by user {}", 
                 jobId, releaseIds.size(), user.getEmail());

        JobExecutionStatus status = new JobExecutionStatus(jobId, JobType.BULK_REUSE_ANALYSIS);
        status.setTotalItems(releaseIds.size());
        status.setSubmittedBy(user.getEmail());
        runningJobs.put(jobId, status);

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            executeBulkReuseAnalysis(jobId, releaseIds, user, options);
        }, executorService);

        // Set timeout
        future.orTimeout(DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES).whenComplete((result, throwable) -> {
            if (throwable instanceof TimeoutException) {
                log.warn("Bulk reuse analysis job {} timed out after {} minutes", jobId, DEFAULT_TIMEOUT_MINUTES);
                updateJobStatus(jobId, JobStatus.TIMEOUT, "Job timed out after " + DEFAULT_TIMEOUT_MINUTES + " minutes");
            }
        });

        return jobId;
    }

    /**
     * Submit a duplicate detection job across releases
     */
    public String submitDuplicateDetectionJob(List<String> releaseIds, User user) {
        String jobId = generateJobId("duplicate-detection");
        
        log.info("Submitting duplicate detection job {} for {} releases by user {}", 
                 jobId, releaseIds.size(), user.getEmail());

        JobExecutionStatus status = new JobExecutionStatus(jobId, JobType.DUPLICATE_DETECTION);
        status.setTotalItems(releaseIds.size());
        status.setSubmittedBy(user.getEmail());
        runningJobs.put(jobId, status);

        CompletableFuture.runAsync(() -> {
            executeDuplicateDetection(jobId, releaseIds, user);
        }, executorService).orTimeout(DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES);

        return jobId;
    }

    /**
     * Submit a clearing decision inheritance job
     */
    public String submitClearingDecisionInheritanceJob(String targetReleaseId, List<String> sourceReleaseIds, 
                                                      User user, InheritanceOptions options) {
        String jobId = generateJobId("clearing-inheritance");
        
        log.info("Submitting clearing decision inheritance job {} for target release {} with {} source releases by user {}", 
                 jobId, targetReleaseId, sourceReleaseIds.size(), user.getEmail());

        JobExecutionStatus status = new JobExecutionStatus(jobId, JobType.CLEARING_INHERITANCE);
        status.setTotalItems(sourceReleaseIds.size());
        status.setSubmittedBy(user.getEmail());
        status.getMetadata().put("targetReleaseId", targetReleaseId);
        runningJobs.put(jobId, status);

        CompletableFuture.runAsync(() -> {
            executeClearingDecisionInheritance(jobId, targetReleaseId, sourceReleaseIds, user, options);
        }, executorService).orTimeout(DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES);

        return jobId;
    }

    /**
     * Submit a reuse statistics generation job
     */
    public String submitReuseStatisticsJob(List<String> releaseIds, User user, StatisticsOptions options) {
        String jobId = generateJobId("reuse-statistics");
        
        log.info("Submitting reuse statistics job {} for {} releases by user {}", 
                 jobId, releaseIds.size(), user.getEmail());

        JobExecutionStatus status = new JobExecutionStatus(jobId, JobType.REUSE_STATISTICS);
        status.setTotalItems(releaseIds.size());
        status.setSubmittedBy(user.getEmail());
        runningJobs.put(jobId, status);

        CompletableFuture.runAsync(() -> {
            executeReuseStatisticsGeneration(jobId, releaseIds, user, options);
        }, executorService).orTimeout(DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES);

        return jobId;
    }

    /**
     * Get job execution status
     */
    public JobExecutionStatus getJobStatus(String jobId) {
        return runningJobs.get(jobId);
    }

    /**
     * Get all job statuses for a user
     */
    public List<JobExecutionStatus> getJobStatusesForUser(String userEmail) {
        return runningJobs.values().stream()
                .filter(status -> userEmail.equals(status.getSubmittedBy()))
                .collect(Collectors.toList());
    }

    /**
     * Cancel a running job
     */
    public boolean cancelJob(String jobId) {
        JobExecutionStatus status = runningJobs.get(jobId);
        if (status != null && status.getJobStatus() == JobStatus.RUNNING) {
            updateJobStatus(jobId, JobStatus.CANCELLED, "Job cancelled by user request");
            return true;
        }
        return false;
    }

    /**
     * Clean up completed jobs older than specified days
     */
    public int cleanupCompletedJobs(int olderThanDays) {
        long cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L);
        
        List<String> toRemove = runningJobs.entrySet().stream()
                .filter(entry -> {
                    JobExecutionStatus status = entry.getValue();
                    return (status.getJobStatus() == JobStatus.COMPLETED || 
                            status.getJobStatus() == JobStatus.FAILED ||
                            status.getJobStatus() == JobStatus.CANCELLED) &&
                           status.getEndTime() != null &&
                           status.getEndTime() < cutoffTime;
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        toRemove.forEach(runningJobs::remove);
        
        log.info("Cleaned up {} completed reuse processing jobs older than {} days", toRemove.size(), olderThanDays);
        return toRemove.size();
    }

    // Private execution methods

    private void executeBulkReuseAnalysis(String jobId, List<String> releaseIds, User user, BulkReuseOptions options) {
        updateJobStatus(jobId, JobStatus.RUNNING, "Starting bulk reuse analysis");
        
        try {
            // Process releases in batches
            List<List<String>> batches = partitionList(releaseIds, 
                    options != null && options.getBatchSize() > 0 ? options.getBatchSize() : DEFAULT_BATCH_SIZE);
            
            int processedCount = 0;
            Map<String, Object> aggregatedResults = new HashMap<>();
            
            for (List<String> batch : batches) {
                if (isJobCancelled(jobId)) {
                    return;
                }
                
                // Process batch (placeholder - would integrate with actual release service)
                log.info("Processing batch of {} releases for job {}", batch.size(), jobId);
                
                // Simulate processing time
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    updateJobStatus(jobId, JobStatus.FAILED, "Job interrupted");
                    return;
                }
                
                processedCount += batch.size();
                updateJobProgress(jobId, processedCount, "Processed " + processedCount + " releases");
            }
            
            // Finalize results
            JobExecutionStatus status = runningJobs.get(jobId);
            status.getResults().put("totalReleases", releaseIds.size());
            status.getResults().put("processedReleases", processedCount);
            status.getResults().put("aggregatedResults", aggregatedResults);
            
            updateJobStatus(jobId, JobStatus.COMPLETED, "Bulk reuse analysis completed successfully");
            
        } catch (Exception e) {
            log.error("Error executing bulk reuse analysis job {}: {}", jobId, e.getMessage(), e);
            updateJobStatus(jobId, JobStatus.FAILED, "Error: " + e.getMessage());
        }
    }

    private void executeDuplicateDetection(String jobId, List<String> releaseIds, User user) {
        updateJobStatus(jobId, JobStatus.RUNNING, "Starting duplicate detection");
        
        try {
            // Process duplicate detection (placeholder - would integrate with actual services)
            log.info("Executing duplicate detection for {} releases in job {}", releaseIds.size(), jobId);
            
            int processedCount = 0;
            Map<String, Object> duplicateResults = new HashMap<>();
            
            for (String releaseId : releaseIds) {
                if (isJobCancelled(jobId)) {
                    return;
                }
                
                // Process individual release
                log.debug("Processing duplicate detection for release {} in job {}", releaseId, jobId);
                
                processedCount++;
                updateJobProgress(jobId, processedCount, "Analyzed " + processedCount + " releases for duplicates");
            }
            
            // Store results
            JobExecutionStatus status = runningJobs.get(jobId);
            status.getResults().put("duplicatesFound", duplicateResults);
            status.getResults().put("releasesAnalyzed", processedCount);
            
            updateJobStatus(jobId, JobStatus.COMPLETED, "Duplicate detection completed successfully");
            
        } catch (Exception e) {
            log.error("Error executing duplicate detection job {}: {}", jobId, e.getMessage(), e);
            updateJobStatus(jobId, JobStatus.FAILED, "Error: " + e.getMessage());
        }
    }

    private void executeClearingDecisionInheritance(String jobId, String targetReleaseId, List<String> sourceReleaseIds, 
                                                   User user, InheritanceOptions options) {
        updateJobStatus(jobId, JobStatus.RUNNING, "Starting clearing decision inheritance");
        
        try {
            log.info("Executing clearing decision inheritance for target {} with {} sources in job {}", 
                     targetReleaseId, sourceReleaseIds.size(), jobId);
            
            int processedCount = 0;
            List<String> successfulInheritances = new ArrayList<>();
            List<String> failedInheritances = new ArrayList<>();
            
            for (String sourceReleaseId : sourceReleaseIds) {
                if (isJobCancelled(jobId)) {
                    return;
                }
                
                try {
                    // Process inheritance (placeholder)
                    log.debug("Processing inheritance from source {} to target {} in job {}", 
                             sourceReleaseId, targetReleaseId, jobId);
                    
                    successfulInheritances.add(sourceReleaseId);
                    
                } catch (Exception e) {
                    log.warn("Failed to inherit from source {} to target {}: {}", 
                             sourceReleaseId, targetReleaseId, e.getMessage());
                    failedInheritances.add(sourceReleaseId);
                }
                
                processedCount++;
                updateJobProgress(jobId, processedCount, "Processed " + processedCount + " inheritance operations");
            }
            
            // Store results
            JobExecutionStatus status = runningJobs.get(jobId);
            status.getResults().put("targetReleaseId", targetReleaseId);
            status.getResults().put("successfulInheritances", successfulInheritances);
            status.getResults().put("failedInheritances", failedInheritances);
            status.getResults().put("totalProcessed", processedCount);
            
            updateJobStatus(jobId, JobStatus.COMPLETED, 
                    String.format("Clearing decision inheritance completed: %d successful, %d failed", 
                                  successfulInheritances.size(), failedInheritances.size()));
            
        } catch (Exception e) {
            log.error("Error executing clearing decision inheritance job {}: {}", jobId, e.getMessage(), e);
            updateJobStatus(jobId, JobStatus.FAILED, "Error: " + e.getMessage());
        }
    }

    private void executeReuseStatisticsGeneration(String jobId, List<String> releaseIds, User user, StatisticsOptions options) {
        updateJobStatus(jobId, JobStatus.RUNNING, "Starting reuse statistics generation");
        
        try {
            log.info("Executing reuse statistics generation for {} releases in job {}", releaseIds.size(), jobId);
            
            Map<String, Object> statisticsResults = new HashMap<>();
            int processedCount = 0;
            
            for (String releaseId : releaseIds) {
                if (isJobCancelled(jobId)) {
                    return;
                }
                
                // Generate statistics for individual release
                log.debug("Generating statistics for release {} in job {}", releaseId, jobId);
                
                processedCount++;
                updateJobProgress(jobId, processedCount, "Generated statistics for " + processedCount + " releases");
            }
            
            // Store aggregated results
            JobExecutionStatus status = runningJobs.get(jobId);
            status.getResults().put("statisticsResults", statisticsResults);
            status.getResults().put("releasesAnalyzed", processedCount);
            
            updateJobStatus(jobId, JobStatus.COMPLETED, "Reuse statistics generation completed successfully");
            
        } catch (Exception e) {
            log.error("Error executing reuse statistics generation job {}: {}", jobId, e.getMessage(), e);
            updateJobStatus(jobId, JobStatus.FAILED, "Error: " + e.getMessage());
        }
    }

    // Utility methods

    private String generateJobId(String jobType) {
        return jobType + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void updateJobStatus(String jobId, JobStatus status, String message) {
        JobExecutionStatus jobStatus = runningJobs.get(jobId);
        if (jobStatus != null) {
            jobStatus.setJobStatus(status);
            jobStatus.setStatusMessage(message);
            if (status == JobStatus.RUNNING && jobStatus.getStartTime() == null) {
                jobStatus.setStartTime(System.currentTimeMillis());
            }
            if (status == JobStatus.COMPLETED || status == JobStatus.FAILED || status == JobStatus.CANCELLED) {
                jobStatus.setEndTime(System.currentTimeMillis());
            }
            log.info("Job {} status updated to {}: {}", jobId, status, message);
        }
    }

    private void updateJobProgress(String jobId, int processedItems, String progressMessage) {
        JobExecutionStatus status = runningJobs.get(jobId);
        if (status != null) {
            status.setProcessedItems(processedItems);
            status.setStatusMessage(progressMessage);
            
            if (status.getTotalItems() > 0) {
                double progress = (double) processedItems / status.getTotalItems() * 100.0;
                status.setProgressPercentage(Math.round(progress * 100.0) / 100.0);
            }
        }
    }

    private boolean isJobCancelled(String jobId) {
        JobExecutionStatus status = runningJobs.get(jobId);
        return status != null && status.getJobStatus() == JobStatus.CANCELLED;
    }

    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return partitions;
    }

    public void shutdown() {
        log.info("Shutting down reuse processing job executor");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
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
        CANCELLED,
        TIMEOUT
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
        private final Map<String, String> metadata = new HashMap<>();

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
        public Map<String, String> getMetadata() { return metadata; }
    }

    public static class BulkReuseOptions {
        private int batchSize = DEFAULT_BATCH_SIZE;
        private boolean includeFileDetails = false;
        private boolean generateReport = false;
        private String reportFormat = "json";

        // Getters and setters
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        public boolean isIncludeFileDetails() { return includeFileDetails; }
        public void setIncludeFileDetails(boolean includeFileDetails) { this.includeFileDetails = includeFileDetails; }
        public boolean isGenerateReport() { return generateReport; }
        public void setGenerateReport(boolean generateReport) { this.generateReport = generateReport; }
        public String getReportFormat() { return reportFormat; }
        public void setReportFormat(String reportFormat) { this.reportFormat = reportFormat; }
    }

    public static class InheritanceOptions {
        private boolean requireApproval = true;
        private boolean copyLicenseConclusions = true;
        private boolean copyCopyrightStatements = true;
        private boolean copyExportRestrictions = false;
        private String inheritanceComment;

        // Getters and setters
        public boolean isRequireApproval() { return requireApproval; }
        public void setRequireApproval(boolean requireApproval) { this.requireApproval = requireApproval; }
        public boolean isCopyLicenseConclusions() { return copyLicenseConclusions; }
        public void setCopyLicenseConclusions(boolean copyLicenseConclusions) { this.copyLicenseConclusions = copyLicenseConclusions; }
        public boolean isCopyCopyrightStatements() { return copyCopyrightStatements; }
        public void setCopyCopyrightStatements(boolean copyCopyrightStatements) { this.copyCopyrightStatements = copyCopyrightStatements; }
        public boolean isCopyExportRestrictions() { return copyExportRestrictions; }
        public void setCopyExportRestrictions(boolean copyExportRestrictions) { this.copyExportRestrictions = copyExportRestrictions; }
        public String getInheritanceComment() { return inheritanceComment; }
        public void setInheritanceComment(String inheritanceComment) { this.inheritanceComment = inheritanceComment; }
    }

    public static class StatisticsOptions {
        private boolean includeHistoricalData = false;
        private boolean generateTrends = false;
        private String aggregationLevel = "release"; // release, component, project, organization
        private List<String> metricsToInclude = Arrays.asList("reuse_percentage", "duplicate_count");

        // Getters and setters
        public boolean isIncludeHistoricalData() { return includeHistoricalData; }
        public void setIncludeHistoricalData(boolean includeHistoricalData) { this.includeHistoricalData = includeHistoricalData; }
        public boolean isGenerateTrends() { return generateTrends; }
        public void setGenerateTrends(boolean generateTrends) { this.generateTrends = generateTrends; }
        public String getAggregationLevel() { return aggregationLevel; }
        public void setAggregationLevel(String aggregationLevel) { this.aggregationLevel = aggregationLevel; }
        public List<String> getMetricsToInclude() { return metricsToInclude; }
        public void setMetricsToInclude(List<String> metricsToInclude) { this.metricsToInclude = metricsToInclude; }
    }
}