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

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validator for Fossology scan options to ensure only supported options are provided.
 */
public class ScanOptionsValidator {

    /**
     * Validates the provided scan options against the supported options.
     * 
     * @param scanOptions The scan options to validate
     * @throws ResponseStatusException if validation fails
     */
    public static void validateScanOptions(ScanOptionsRequest scanOptions) {
        if (scanOptions == null) {
            return; // null options are allowed and will use defaults
        }

        List<String> errors = new ArrayList<>();

        // Validate analysis options
        if (scanOptions.getAnalysis() != null) {
            Set<String> invalidAnalysisOptions = findInvalidOptions(
                scanOptions.getAnalysis(), 
                ScanOptionsRequest.DEFAULT_ANALYSIS_OPTIONS.keySet()
            );
            if (!invalidAnalysisOptions.isEmpty()) {
                errors.add("Invalid analysis options: " + invalidAnalysisOptions + 
                          ". Supported options: " + ScanOptionsRequest.DEFAULT_ANALYSIS_OPTIONS.keySet());
            }
        }

        // Validate decider options
        if (scanOptions.getDecider() != null) {
            Set<String> invalidDeciderOptions = findInvalidOptions(
                scanOptions.getDecider(), 
                ScanOptionsRequest.DEFAULT_DECIDER_OPTIONS.keySet()
            );
            if (!invalidDeciderOptions.isEmpty()) {
                errors.add("Invalid decider options: " + invalidDeciderOptions + 
                          ". Supported options: " + ScanOptionsRequest.DEFAULT_DECIDER_OPTIONS.keySet());
            }
        }

        // Validate reuse options
        if (scanOptions.getReuse() != null) {
            Set<String> invalidReuseOptions = findInvalidOptions(
                scanOptions.getReuse(), 
                ScanOptionsRequest.DEFAULT_REUSE_OPTIONS.keySet()
            );
            if (!invalidReuseOptions.isEmpty()) {
                errors.add("Invalid reuse options: " + invalidReuseOptions + 
                          ". Supported options: " + ScanOptionsRequest.DEFAULT_REUSE_OPTIONS.keySet());
            }
        }

        if (!errors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Invalid scan options: " + String.join("; ", errors));
        }
    }

    /**
     * Finds options that are not in the supported set.
     */
    private static Set<String> findInvalidOptions(Map<String, Boolean> providedOptions, Set<String> supportedOptions) {
        return providedOptions.keySet().stream()
                .filter(option -> !supportedOptions.contains(option))
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Validates that at least one analysis agent is enabled.
     * 
     * @param scanOptions The scan options to validate
     * @throws ResponseStatusException if no analysis agents are enabled
     */
    public static void validateAtLeastOneAnalysisAgent(ScanOptionsRequest scanOptions) {
        if (scanOptions == null || scanOptions.getAnalysis() == null) {
            return; // Will use defaults which have at least one enabled
        }

        boolean hasEnabledAgent = scanOptions.getAnalysis().values().stream()
                .anyMatch(Boolean::booleanValue);

        if (!hasEnabledAgent) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "At least one analysis agent must be enabled");
        }
    }

    /**
     * Performs comprehensive validation of scan options.
     * 
     * @param scanOptions The scan options to validate
     * @throws ResponseStatusException if validation fails
     */
    public static void validateComprehensive(ScanOptionsRequest scanOptions) {
        validateScanOptions(scanOptions);
        validateAtLeastOneAnalysisAgent(scanOptions);
    }
}