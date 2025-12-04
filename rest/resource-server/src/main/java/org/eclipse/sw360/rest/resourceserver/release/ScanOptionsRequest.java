/*
 * Copyright Ritankar Saha<ritankar.saha786@gmail.com>, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.release;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Request object for configurable Fossology scan options.
 * Contains analysis agents, decider agents, and reuse options that can be customized.
 */
public class ScanOptionsRequest {

    @JsonProperty("analysis")
    private Map<String, Boolean> analysis = new HashMap<>();

    @JsonProperty("decider")
    private Map<String, Boolean> decider = new HashMap<>();

    @JsonProperty("reuse")
    private Map<String, Boolean> reuse = new HashMap<>();

    // Available analysis options with defaults
    public static final Map<String, Boolean> DEFAULT_ANALYSIS_OPTIONS = createDefaultAnalysisOptions();

    private static Map<String, Boolean> createDefaultAnalysisOptions() {
        Map<String, Boolean> options = new HashMap<>();
        options.put("bucket", true);
        options.put("copyrightEmailAuthor", true);
        options.put("ecc", true);
        options.put("ipra", false);
        options.put("keyword", true);
        options.put("mime", true);
        options.put("monk", true);
        options.put("nomos", true);
        options.put("ojo", true);
        options.put("pkgagent", true);
        options.put("reso", false);
        return options;
    }

    // Available decider options with defaults
    public static final Map<String, Boolean> DEFAULT_DECIDER_OPTIONS = createDefaultDeciderOptions();

    private static Map<String, Boolean> createDefaultDeciderOptions() {
        Map<String, Boolean> options = new HashMap<>();
        options.put("nomosMonk", true);
        options.put("bulkReused", true);
        options.put("newScanner", true);
        options.put("ojoDecider", false);
        return options;
    }

    // Available reuse options with defaults
    public static final Map<String, Boolean> DEFAULT_REUSE_OPTIONS = createDefaultReuseOptions();

    private static Map<String, Boolean> createDefaultReuseOptions() {
        Map<String, Boolean> options = new HashMap<>();
        options.put("reuseMain", true);
        options.put("reuseEnhanced", true);
        options.put("reuseReport", false);
        options.put("reuseCopyright", true);
        return options;
    }

    public ScanOptionsRequest() {
        // Initialize with defaults
        this.analysis = new HashMap<>(DEFAULT_ANALYSIS_OPTIONS);
        this.decider = new HashMap<>(DEFAULT_DECIDER_OPTIONS);
        this.reuse = new HashMap<>(DEFAULT_REUSE_OPTIONS);
    }

    public Map<String, Boolean> getAnalysis() {
        return analysis;
    }

    public void setAnalysis(Map<String, Boolean> analysis) {
        this.analysis = analysis != null ? analysis : new HashMap<>();
    }

    public Map<String, Boolean> getDecider() {
        return decider;
    }

    public void setDecider(Map<String, Boolean> decider) {
        this.decider = decider != null ? decider : new HashMap<>();
    }

    public Map<String, Boolean> getReuse() {
        return reuse;
    }

    public void setReuse(Map<String, Boolean> reuse) {
        this.reuse = reuse != null ? reuse : new HashMap<>();
    }

    /**
     * Merges provided options with defaults. If an option is not provided,
     * the default value will be used.
     */
    public void mergeWithDefaults() {
        Map<String, Boolean> mergedAnalysis = new HashMap<>(DEFAULT_ANALYSIS_OPTIONS);
        if (analysis != null) {
            mergedAnalysis.putAll(analysis);
        }
        this.analysis = mergedAnalysis;

        Map<String, Boolean> mergedDecider = new HashMap<>(DEFAULT_DECIDER_OPTIONS);
        if (decider != null) {
            mergedDecider.putAll(decider);
        }
        this.decider = mergedDecider;

        Map<String, Boolean> mergedReuse = new HashMap<>(DEFAULT_REUSE_OPTIONS);
        if (reuse != null) {
            mergedReuse.putAll(reuse);
        }
        this.reuse = mergedReuse;
    }

    /**
     * Validates that all provided options are supported.
     * 
     * @return true if all options are valid, false otherwise
     */
    public boolean isValid() {
        return isValidAnalysisOptions() && isValidDeciderOptions() && isValidReuseOptions();
    }

    private boolean isValidAnalysisOptions() {
        if (analysis == null) return true;
        return DEFAULT_ANALYSIS_OPTIONS.keySet().containsAll(analysis.keySet());
    }

    private boolean isValidDeciderOptions() {
        if (decider == null) return true;
        return DEFAULT_DECIDER_OPTIONS.keySet().containsAll(decider.keySet());
    }

    private boolean isValidReuseOptions() {
        if (reuse == null) return true;
        return DEFAULT_REUSE_OPTIONS.keySet().containsAll(reuse.keySet());
    }

    @Override
    public String toString() {
        return "ScanOptionsRequest{" +
                "analysis=" + analysis +
                ", decider=" + decider +
                ", reuse=" + reuse +
                '}';
    }
}