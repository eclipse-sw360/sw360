/*
 * Copyright Ritankar Saha<ritankar.saha786@gmail.com>, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.common;

/**
 * File search result data model for FOSSology file search operations
 */
public class FileSearchResult {
    private int uploadId;
    private int folderId;
    private String filename;
    private String sha1;
    private String md5;
    private Long size;

    public FileSearchResult() {
        this.uploadId = 0;
        this.folderId = 0;
        this.filename = "";
        this.sha1 = "";
        this.md5 = "";
        this.size = 0L;
    }

    public int getUploadId() {
        return uploadId;
    }

    public void setUploadId(int uploadId) {
        this.uploadId = uploadId;
    }

    public int getFolderId() {
        return folderId;
    }

    public void setFolderId(int folderId) {
        this.folderId = folderId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileSearchResult that = (FileSearchResult) o;

        if (uploadId != that.uploadId) return false;
        if (folderId != that.folderId) return false;
        if (!filename.equals(that.filename)) return false;
        if (!sha1.equals(that.sha1)) return false;
        if (!md5.equals(that.md5)) return false;
        return size.equals(that.size);
    }

    @Override
    public int hashCode() {
        int result = uploadId;
        result = 31 * result + folderId;
        result = 31 * result + filename.hashCode();
        result = 31 * result + sha1.hashCode();
        result = 31 * result + md5.hashCode();
        result = 31 * result + size.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "FileSearchResult{" +
                "uploadId=" + uploadId +
                ", folderId=" + folderId +
                ", filename='" + filename + '\'' +
                ", sha1='" + sha1 + '\'' +
                ", md5='" + md5 + '\'' +
                ", size=" + size +
                '}';
    }
}