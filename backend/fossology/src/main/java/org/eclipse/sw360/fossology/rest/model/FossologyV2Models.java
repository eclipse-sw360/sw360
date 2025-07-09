/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.fossology.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Complete collection of response models for FOSSology v2 API
 */
public class FossologyV2Models {

    /**
     * Response model for combined upload+job operation in v2 API
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CombinedUploadJobResponse {
        @JsonProperty("uploadId")
        private int uploadId;
        
        @JsonProperty("jobId")
        private int jobId;
        
        @JsonProperty("status")
        private String status;
        
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("queuePosition")
        private Integer queuePosition;

        public CombinedUploadJobResponse() {
            this.uploadId = 0;
            this.jobId = 0;
            this.status = "";
            this.message = "";
        }

        // Getters and Setters
        public int getUploadId() { return uploadId; }
        public void setUploadId(int uploadId) { this.uploadId = uploadId; }
        
        public int getJobId() { return jobId; }
        public void setJobId(int jobId) { this.jobId = jobId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Integer getQueuePosition() { return queuePosition; }
        public void setQueuePosition(Integer queuePosition) { this.queuePosition = queuePosition; }
    }

    /**
     * Enhanced job status response for v2 API
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JobStatusResponse {
        @JsonProperty("id")
        private int jobId;
        
        @JsonProperty("status")
        private String status;
        
        @JsonProperty("eta")
        private int eta;
        
        @JsonProperty("queuePosition")
        private Integer queuePosition;
        
        @JsonProperty("progress")
        private JobProgress progress;

        @JsonProperty("name")
        private String name;

        @JsonProperty("queueDate")
        private String queueDate;

        @JsonProperty("uploadId")
        private Integer uploadId;

        // Getters and Setters
        public int getJobId() { return jobId; }
        public void setJobId(int jobId) { this.jobId = jobId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public int getEta() { return eta; }
        public void setEta(int eta) { this.eta = eta; }
        
        public Integer getQueuePosition() { return queuePosition; }
        public void setQueuePosition(Integer queuePosition) { this.queuePosition = queuePosition; }
        
        public JobProgress getProgress() { return progress; }
        public void setProgress(JobProgress progress) { this.progress = progress; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getQueueDate() { return queueDate; }
        public void setQueueDate(String queueDate) { this.queueDate = queueDate; }

        public Integer getUploadId() { return uploadId; }
        public void setUploadId(Integer uploadId) { this.uploadId = uploadId; }

        /**
         * Job progress details
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class JobProgress {
            @JsonProperty("completed")
            private int completed;
            
            @JsonProperty("total")
            private int total;
            
            @JsonProperty("percentage")
            private double percentage;

            // Getters and Setters
            public int getCompleted() { return completed; }
            public void setCompleted(int completed) { this.completed = completed; }
            
            public int getTotal() { return total; }
            public void setTotal(int total) { this.total = total; }
            
            public double getPercentage() { return percentage; }
            public void setPercentage(double percentage) { this.percentage = percentage; }
        }
    }

    /**
     * Upload response for v2 API
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UploadResponse {
        @JsonProperty("id")
        private int uploadId;
        
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("status")
        private String status;

        @JsonProperty("description")
        private String description;

        @JsonProperty("uploadName")
        private String uploadName;

        @JsonProperty("uploadDate")
        private String uploadDate;

        @JsonProperty("folderId")
        private Integer folderId;

        @JsonProperty("folderName")
        private String folderName;

        // Getters and Setters
        public int getUploadId() { return uploadId; }
        public void setUploadId(int uploadId) { this.uploadId = uploadId; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getUploadName() { return uploadName; }
        public void setUploadName(String uploadName) { this.uploadName = uploadName; }

        public String getUploadDate() { return uploadDate; }
        public void setUploadDate(String uploadDate) { this.uploadDate = uploadDate; }

        public Integer getFolderId() { return folderId; }
        public void setFolderId(Integer folderId) { this.folderId = folderId; }

        public String getFolderName() { return folderName; }
        public void setFolderName(String folderName) { this.folderName = folderName; }
    }

    /**
     * Report response for v2 API
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReportResponse {
        @JsonProperty("reportId")
        private int reportId;
        
        @JsonProperty("status")
        private String status;
        
        @JsonProperty("format")
        private String format;
        
        @JsonProperty("message")
        private String message;

        @JsonProperty("code")
        private Integer code;

        @JsonProperty("type")
        private String type;

        // Getters and Setters
        public int getReportId() { return reportId; }
        public void setReportId(int reportId) { this.reportId = reportId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Integer getCode() { return code; }
        public void setCode(Integer code) { this.code = code; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    /**
     * File search response for v2 API
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FileSearchResponse {
        @JsonProperty("results")
        private List<FileSearchResult> results;

        @JsonProperty("uploads")
        private List<String> uploads;

        // Getters and Setters
        public List<FileSearchResult> getResults() { return results; }
        public void setResults(List<FileSearchResult> results) { this.results = results; }

        public List<String> getUploads() { return uploads; }
        public void setUploads(List<String> uploads) { this.uploads = uploads; }
    }

    /**
     * File search result item for v2 API
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FileSearchResult {
        @JsonProperty("uploadId")
        private int uploadId;
        
        @JsonProperty("folderId")
        private int folderId;
        
        @JsonProperty("filename")
        private String filename;
        
        @JsonProperty("sha1")
        private String sha1;

        @JsonProperty("md5")
        private String md5;

        @JsonProperty("size")
        private Long size;

        // Getters and Setters
        public int getUploadId() { return uploadId; }
        public void setUploadId(int uploadId) { this.uploadId = uploadId; }
        
        public int getFolderId() { return folderId; }
        public void setFolderId(int folderId) { this.folderId = folderId; }
        
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        
        public String getSha1() { return sha1; }
        public void setSha1(String sha1) { this.sha1 = sha1; }

        public String getMd5() { return md5; }
        public void setMd5(String md5) { this.md5 = md5; }

        public Long getSize() { return size; }
        public void setSize(Long size) { this.size = size; }
    }

    /**
     * API Info response for v2 API
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApiInfoResponse {
        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        @JsonProperty("version")
        private String version;

        @JsonProperty("security")
        private List<String> security;

        @JsonProperty("contact")
        private String contact;

        @JsonProperty("fossology")
        private FossologyInfo fossology;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public List<String> getSecurity() { return security; }
        public void setSecurity(List<String> security) { this.security = security; }

        public String getContact() { return contact; }
        public void setContact(String contact) { this.contact = contact; }

        public FossologyInfo getFossology() { return fossology; }
        public void setFossology(FossologyInfo fossology) { this.fossology = fossology; }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class FossologyInfo {
            @JsonProperty("version")
            private String version;

            @JsonProperty("branchName")
            private String branchName;

            @JsonProperty("commitHash")
            private String commitHash;

            @JsonProperty("commitDate")
            private String commitDate;

            @JsonProperty("buildDate")
            private String buildDate;

            // Getters and Setters
            public String getVersion() { return version; }
            public void setVersion(String version) { this.version = version; }

            public String getBranchName() { return branchName; }
            public void setBranchName(String branchName) { this.branchName = branchName; }

            public String getCommitHash() { return commitHash; }
            public void setCommitHash(String commitHash) { this.commitHash = commitHash; }

            public String getCommitDate() { return commitDate; }
            public void setCommitDate(String commitDate) { this.commitDate = commitDate; }

            public String getBuildDate() { return buildDate; }
            public void setBuildDate(String buildDate) { this.buildDate = buildDate; }
        }
    }

    /**
     * Folder response for v2 API
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FolderResponse {
        @JsonProperty("id")
        private int id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        @JsonProperty("parent")
        private Integer parent;

        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Integer getParent() { return parent; }
        public void setParent(Integer parent) { this.parent = parent; }
    }

    /**
     * Generic Info response for v2 API
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InfoResponse {
        @JsonProperty("code")
        private int code;

        @JsonProperty("message")
        private String message;

        @JsonProperty("type")
        private String type;

        // Getters and Setters
        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}