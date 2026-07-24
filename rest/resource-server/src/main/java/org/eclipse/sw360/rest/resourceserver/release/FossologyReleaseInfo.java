/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 * Copyright Sandip Mandal<sandipmandal02.sm@gmail.com>, 2026.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.release;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Data Transfer Object for FOSSology release information.
 * Reflects the three-step FOSSology workflow: upload → scan → report.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FossologyReleaseInfo {

    /**
     * Report formats supported by FOSSology, available once scanning is complete.
     */
    public static final List<String> REPORT_FORMATS = List.of(
            "spdx2", "spdx2tv", "spdx3json", "spdx3rdf", "spdx3jsonld",
            "dep5", "readmeoss", "unifiedreport", "clixml", "cyclonedx"
    );

    @JsonProperty("uploadId")
    private String uploadId;

    @JsonProperty("uploadStatus")
    private String uploadStatus;

    @JsonProperty("scanStatus")
    private String scanStatus;

    @JsonProperty("reportStatus")
    private String reportStatus;

    @JsonProperty("processStatus")
    private String processStatus;

    @JsonProperty("sourceAttachmentId")
    private String sourceAttachmentId;

    @JsonProperty("reportAttachmentId")
    private String reportAttachmentId;

    @JsonProperty("availableReportFormats")
    private List<String> availableReportFormats;

    @JsonProperty("lastUpdated")
    private String lastUpdated;

    public FossologyReleaseInfo() {
    }

    public String getUploadId() { return uploadId; }
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }

    public String getUploadStatus() { return uploadStatus; }
    public void setUploadStatus(String uploadStatus) { this.uploadStatus = uploadStatus; }

    public String getScanStatus() { return scanStatus; }
    public void setScanStatus(String scanStatus) { this.scanStatus = scanStatus; }

    public String getReportStatus() { return reportStatus; }
    public void setReportStatus(String reportStatus) { this.reportStatus = reportStatus; }

    public String getProcessStatus() { return processStatus; }
    public void setProcessStatus(String processStatus) { this.processStatus = processStatus; }

    public String getSourceAttachmentId() { return sourceAttachmentId; }
    public void setSourceAttachmentId(String sourceAttachmentId) { this.sourceAttachmentId = sourceAttachmentId; }

    public String getReportAttachmentId() { return reportAttachmentId; }
    public void setReportAttachmentId(String reportAttachmentId) { this.reportAttachmentId = reportAttachmentId; }

    public List<String> getAvailableReportFormats() { return availableReportFormats; }
    public void setAvailableReportFormats(List<String> availableReportFormats) { this.availableReportFormats = availableReportFormats; }

    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String uploadId;
        private String uploadStatus;
        private String scanStatus;
        private String reportStatus;
        private String processStatus;
        private String sourceAttachmentId;
        private String reportAttachmentId;
        private List<String> availableReportFormats;
        private String lastUpdated;

        public Builder uploadId(String uploadId) { this.uploadId = uploadId; return this; }
        public Builder uploadStatus(String uploadStatus) { this.uploadStatus = uploadStatus; return this; }
        public Builder scanStatus(String scanStatus) { this.scanStatus = scanStatus; return this; }
        public Builder reportStatus(String reportStatus) { this.reportStatus = reportStatus; return this; }
        public Builder processStatus(String processStatus) { this.processStatus = processStatus; return this; }
        public Builder sourceAttachmentId(String sourceAttachmentId) { this.sourceAttachmentId = sourceAttachmentId; return this; }
        public Builder reportAttachmentId(String reportAttachmentId) { this.reportAttachmentId = reportAttachmentId; return this; }
        public Builder availableReportFormats(List<String> formats) { this.availableReportFormats = formats; return this; }
        public Builder lastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; return this; }

        public FossologyReleaseInfo build() {
            FossologyReleaseInfo info = new FossologyReleaseInfo();
            info.setUploadId(uploadId);
            info.setUploadStatus(uploadStatus);
            info.setScanStatus(scanStatus);
            info.setReportStatus(reportStatus);
            info.setProcessStatus(processStatus);
            info.setSourceAttachmentId(sourceAttachmentId);
            info.setReportAttachmentId(reportAttachmentId);
            info.setAvailableReportFormats(availableReportFormats == null ? null : List.copyOf(availableReportFormats));
            info.setLastUpdated(lastUpdated);
            return info;
        }
    }

    @Override
    public String toString() {
        return "FossologyReleaseInfo{" +
                "uploadId='" + uploadId + '\'' +
                ", uploadStatus='" + uploadStatus + '\'' +
                ", scanStatus='" + scanStatus + '\'' +
                ", reportStatus='" + reportStatus + '\'' +
                ", processStatus='" + processStatus + '\'' +
                '}';
    }
}
