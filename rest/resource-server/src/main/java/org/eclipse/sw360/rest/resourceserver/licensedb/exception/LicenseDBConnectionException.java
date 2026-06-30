/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.licensedb.exception;

/**
 * Exception thrown when connection to LicenseDB server fails.
 * 
 * <p>This exception is thrown when the SW360 application cannot establish
 * a connection to the LicenseDB service. This could be due to:</p>
 * <ul>
 *   <li>Network connectivity issues</li>
 *   <li>LicenseDB server being unavailable</li>
 *   <li>Connection timeout</li>
 *   <li>DNS resolution failures</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>
 * try {
 *     licenses = licenseDBClient.getAllLicenses();
 * } catch (LicenseDBConnectionException e) {
 *     log.error("Failed to connect to LicenseDB: {}", e.getMessage());
 *     // Handle connection error
 * }
 * </pre>
 */
public class LicenseDBConnectionException extends LicenseDBException {
    
    private static final long serialVersionUID = 1L;
    
    private static final String ERROR_CODE = "CONNECTION_ERROR";
    
    /**
     * Constructs a new LicenseDBConnectionException with the specified message.
     *
     * @param message the error message describing the connection failure
     */
    public LicenseDBConnectionException(String message) {
        super(message, ERROR_CODE);
    }
    
    /**
     * Constructs a new LicenseDBConnectionException with the specified message and cause.
     *
     * @param message the error message describing the connection failure
     * @param cause the underlying cause of the connection failure
     */
    public LicenseDBConnectionException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }
}