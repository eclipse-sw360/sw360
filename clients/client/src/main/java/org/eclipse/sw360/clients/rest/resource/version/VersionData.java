/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.clients.rest.resource.version;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class VersionData {
    private String apiVersion;
    private String buildTime;
    private String buildNumber;
    private String sw360Version;
    private String gitBranch;

    public String getApiVersion() {
        return apiVersion;
    }

    public VersionData setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    public String getBuildTime() {
        return buildTime;
    }

    public VersionData setBuildTime(String buildTime) {
        this.buildTime = buildTime;
        return this;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public VersionData setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
        return this;
    }

    public String getSw360Version() {
        return sw360Version;
    }

    public VersionData setSw360Version(String sw360Version) {
        this.sw360Version = sw360Version;
        return this;
    }

    public String getGitBranch() {
        return gitBranch;
    }

    public VersionData setGitBranch(String gitBranch) {
        this.gitBranch = gitBranch;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionData that = (VersionData) o;
        return Objects.equals(apiVersion, that.apiVersion) &&
                Objects.equals(buildTime, that.buildTime) &&
                Objects.equals(buildNumber, that.buildNumber) &&
                Objects.equals(sw360Version, that.sw360Version) &&
                Objects.equals(gitBranch, that.gitBranch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiVersion, buildTime, buildNumber, sw360Version, gitBranch);
    }
}
