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
 * Exception thrown when OAuth2 authentication with LicenseDB fails.
 * 
 * <p>This exception is thrown when the SW360 application cannot authenticate
 * with the LicenseDB service using OAuth2 Machine-to-Machine credentials.
 * This could be due to:</p>
 * <ul>
 *   <li>Invalid client credentials</li>
 *   <li>Expired access token</li>
 *   <li>Missing or invalid OAuth2 configuration</li>
 *   <li>Insufficient permissions</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>
 * try {
 *     accessToken = oauth2Service.getAccessToken();
 * } catch (LicenseDBAuthenticationException e) {
 *     log.error("OAuth2 authentication failed: {}", e.getMessage());
 *     // Handle authentication error
 * }
 * </pre>
 */
public class LicenseDBAuthenticationException extends LicenseDBException {
    
    private static final long serialVersionUID = 1L;
    
    private static final String ERROR_CODE = "AUTHENTICATION_ERROR";
    
    /**
     * Constructs a new LicenseDBAuthenticationException with the specified message.
     *
     * @param message the error message describing the authentication failure
     */
    public LicenseDBAuthenticationException(String message) {
        super(message, ERROR_CODE);
    }
    
    /**
     * Constructs a new LicenseDBAuthenticationException with the specified message and cause.
     *
     * @param message the error message describing the authentication failure
     * @param cause the underlying cause of the authentication failure
     */
    public LicenseDBAuthenticationException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }
}