/*
 * Copyright Ritankar Saha<ritankar.saha786@gmail.com>, 2025. Part of the SW360 Portal Project.
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
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Complete collection of response models for FOSSology v2 API
 */
public class FossologyV2Models {

    /**
     * Response model for combined upload+job operation in v2 API
     */
    @Getter
    @Setter
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
    }

    /**
     * Enhanced job status response for v2 API
     */
    @Getter
    @Setter
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

        /**
         * Job progress details
         */
        @Getter
        @Setter
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class JobProgress {
            @JsonProperty("completed")
            private int completed;

            @JsonProperty("total")
            private int total;

            @JsonProperty("percentage")
            private double percentage;
        }
    }

    /**
     * Upload response for v2 API
     */
    @Getter
    @Setter
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
    }

    /**
     * Report response for v2 API
     */
    @Getter
    @Setter
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
    }

    /**
     * File search response for v2 API
     */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FileSearchResponse {
        @JsonProperty("results")
        private List<FileSearchResult> results;

        @JsonProperty("uploads")
        private List<String> uploads;
    }

    /**
     * File search result item for v2 API
     */
    @Getter
    @Setter
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
    }

    /**
     * API Info response for v2 API
     */
    @Getter
    @Setter
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

        @Getter
        @Setter
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
        }
    }

    /**
     * Folder response for v2 API
     */
    @Getter
    @Setter
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
    }

    /**
     * Generic Info response for v2 API
     */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InfoResponse {
        @JsonProperty("code")
        private int code;

        @JsonProperty("message")
        private String message;

        @JsonProperty("type")
        private String type;
    }
}
