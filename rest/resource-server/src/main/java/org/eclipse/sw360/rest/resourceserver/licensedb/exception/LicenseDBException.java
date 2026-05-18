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
 * Base exception for all LicenseDB-related errors.
 * 
 * <p>This exception serves as the parent class for all LicenseDB-specific
 * exceptions in the SW360 application. It provides a common structure for
 * error handling when interacting with the LicenseDB service.</p>
 * 
 * <p>Subclasses include:</p>
 * <ul>
 *   <li>{@link LicenseDBConnectionException} - Connection errors</li>
 *   <li>{@link LicenseDBAuthenticationException} - OAuth2 authentication errors</li>
 *   <li>{@link LicenseDBDataException} - Data validation errors</li>
 * </ul>
 */
public class LicenseDBException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    private final String errorCode;
    
    /**
     * Constructs a new LicenseDBException with the specified message.
     *
     * @param message the error message
     */
    public LicenseDBException(String message) {
        super(message);
        this.errorCode = "UNKNOWN_ERROR";
    }
    
    /**
     * Constructs a new LicenseDBException with the specified message and error code.
     *
     * @param message the error message
     * @param errorCode the error code identifying the type of error
     */
    public LicenseDBException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * Constructs a new LicenseDBException with the specified message and cause.
     *
     * @param message the error message
     * @param cause the cause of the exception
     */
    public LicenseDBException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "UNKNOWN_ERROR";
    }
    
    /**
     * Constructs a new LicenseDBException with the specified message, error code, and cause.
     *
     * @param message the error message
     * @param errorCode the error code identifying the type of error
     * @param cause the cause of the exception
     */
    public LicenseDBException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * Returns the error code associated with this exception.
     *
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }
}