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
 * Exception thrown when LicenseDB data validation fails.
 * 
 * <p>This exception is thrown when the data received from LicenseDB
 * does not match the expected format or fails validation. This could be due to:</p>
 * <ul>
 *   <li>Missing required fields in license data</li>
 *   <li>Invalid data types</li>
 *   <li>Data format conversion errors</li>
 *   <li>Schema validation failures</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>
 * try {
 *     License license = transformer.transform(licenseDbData);
 * } catch (LicenseDBDataException e) {
 *     log.error("Invalid license data: {}", e.getMessage());
 *     // Handle data validation error
 * }
 * </pre>
 */
public class LicenseDBDataException extends LicenseDBException {
    
    private static final long serialVersionUID = 1L;
    
    private static final String ERROR_CODE = "DATA_ERROR";
    
    /**
     * Constructs a new LicenseDBDataException with the specified message.
     *
     * @param message the error message describing the data validation failure
     */
    public LicenseDBDataException(String message) {
        super(message, ERROR_CODE);
    }
    
    /**
     * Constructs a new LicenseDBDataException with the specified message and cause.
     *
     * @param message the error message describing the data validation failure
     * @param cause the underlying cause of the data validation failure
     */
    public LicenseDBDataException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }
}