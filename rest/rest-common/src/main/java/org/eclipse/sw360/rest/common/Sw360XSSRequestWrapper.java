/*
SPDX-FileCopyrightText: © 2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to sanitize the input from the user to prevent XSS attacks.
 *
 * @author smruti.sahoo@siemens.com
 */
public class Sw360XSSRequestWrapper extends HttpServletRequestWrapper {

    private final Map<String, String[]> sanitizedParameterMap;

	public Sw360XSSRequestWrapper(HttpServletRequest request) {
		super(request);
        this.sanitizedParameterMap = sanitizeParameters(request);
	}

    /**
     * Sanitizes all request parameters (query parameters and form data) once during construction.
     *
     * @param request The original HttpServletRequest.
     * @return A map of sanitized parameter names to arrays of sanitized values.
     */
    private Map<String, String[]> sanitizeParameters(HttpServletRequest request) {
        Map<String, String[]> originalParameters = request.getParameterMap();
        Map<String, String[]> sanitizedMap = new HashMap<>();

        for (Map.Entry<String, String[]> entry : originalParameters.entrySet()) {
            String paramName = entry.getKey();
            String[] originalValues = entry.getValue();
            String[] sanitizedValues = new String[originalValues.length];
            for (int i = 0; i < originalValues.length; i++) {
                sanitizedValues[i] = stripXSS(originalValues[i]);
            }
            sanitizedMap.put(paramName, sanitizedValues);
        }
        return Collections.unmodifiableMap(sanitizedMap);
    }

    /**
     * Overrides getParameter to return a sanitized parameter value.
     */
    @Override
    public String getParameter(String name) {
        String[] values = sanitizedParameterMap.get(name);
        return (values != null && values.length > 0) ? values[0] : null;
    }

     /**
     * Overrides getParameterValues to return sanitized parameter values.
     */
    @Override
    public String[] getParameterValues(String name) {
        String[] values = sanitizedParameterMap.get(name);
        return (values != null) ? values.clone() : null;
    }

    /**
     * Overrides getParameterNames to return the names of the sanitized parameters.
     */
    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(sanitizedParameterMap.keySet());
    }

    /**
     * Overrides getParameterMap to return the map of sanitized parameters.
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> defensiveCopy = new HashMap<>();
        for (Map.Entry<String, String[]> entry : sanitizedParameterMap.entrySet()) {
            defensiveCopy.put(entry.getKey(), entry.getValue().clone());
        }
        return Collections.unmodifiableMap(defensiveCopy);
    }

    /**
     * Overrides getHeader to return a sanitized header value.
     */
    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        if (value == null) {
            return null;
        }
        // Only sanitize headers that are not in the ignore list.
        if (shouldIgnoreHeaderForXSS(name)) {
            return value;
        }
        return stripXSS(value);
    }

	private boolean shouldIgnoreHeaderForXSS(String headerName) {
		// Headers essential for HTTP operations and negotiation
		return "Range".equalsIgnoreCase(headerName) ||
				"Accept".equalsIgnoreCase(headerName) ||
				"Accept-Encoding".equalsIgnoreCase(headerName) ||
				"Accept-Language".equalsIgnoreCase(headerName) ||
				"Content-Type".equalsIgnoreCase(headerName) ||
				"Authorization".equalsIgnoreCase(headerName) ||
				"Content-Length".equalsIgnoreCase(headerName) ||
				"Host".equalsIgnoreCase(headerName) ||
				"User-Agent".equalsIgnoreCase(headerName) ||
				"Referer".equalsIgnoreCase(headerName) ||
				"Cookie".equalsIgnoreCase(headerName) ||
				"Set-Cookie".equalsIgnoreCase(headerName) ||
                "If-None-Match".equalsIgnoreCase(headerName) ||
                "If-Modified-Since".equalsIgnoreCase(headerName) ||
                "X-Requested-With".equalsIgnoreCase(headerName) ||
                "Origin".equalsIgnoreCase(headerName) ||
                "Connection".equalsIgnoreCase(headerName);
	}

    /**
     * Applies HTML encoding to a string value using OWASP ESAPI Encoder.
     * The function unescape the input first to prevent double encoding (`&lt;` => `&amp;lt;`).
     *
     * @implNote `.forHtmlContent` is chosen over `.forHtml` to prevent encoding quotes.
     * @param value The string to sanitize.
     * @return The HTML-encoded string, or null if the input was null.
     */
    public static String stripXSS(String value) {
        if (value == null) {
            return null;
        }
        String canonicalValue = org.apache.commons.text.StringEscapeUtils.unescapeHtml4(value);
        return org.owasp.encoder.Encode.forHtmlContent(canonicalValue);
    }
}
