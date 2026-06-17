/*
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.http.utils;

import java.io.IOException;

/**
 * <p>
 * A special exception class to report errors caused by requests that have an
 * unexpected (typically non-success) status code.
 * </p>
 * <p>
 * This exception class stores the response status code, so that it is
 * available for further investigation. A cause is not available because the
 * request completed normally. In order to identify the request that failed, a
 * tag can be provided that is added to the exception message. This is useful
 * if a future is constructed from the results of multiple requests.
 * </p>
 */
public class FailedRequestException extends IOException {
    /**
     * A tag name to identify the failed request.
     */
    private final String tag;

    /**
     * The status code of the response.
     */
    private final int statusCode;

    /**
     * Creates a new instance of {@code FailedRequestException} and initializes
     * it with a tag and the status code that caused the exception.
     *
     * @param tag        a tag to identify the failed request
     * @param statusCode the HTTP status code
     */
    public FailedRequestException(String tag, int statusCode) {
        this(tag, statusCode, null);
    }

    /**
     * Creates a new instance of {@code FailedRequestException} and initializes
     * it with a tag, the error status code, and the message sent from the
     * server as response to the failed request.
     *
     * @param tag           a tag to identify the failed request
     * @param statusCode    the HTTP status code
     * @param serverMessage the error message sent by the server
     */
    public FailedRequestException(String tag, int statusCode, String serverMessage) {
        super(generateMessage(tag, statusCode, serverMessage));
        this.tag = tag;
        this.statusCode = statusCode;
    }

    /**
     * Returns the response status code that caused this exception.
     *
     * @return the HTTP status code of the failed response
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns a tag to identify the failed request. Result can be
     * <strong>null</strong> if no tag has been provided.
     *
     * @return a tag for the failed request
     */
    public String getTag() {
        return tag;
    }

    /**
     * Generates a message for this exception based on the parameters passed
     * in.
     *
     * @param tag           a tag to identify the failed request
     * @param statusCode    the HTTP status code
     * @param serverMessage the error message sent by the server
     * @return the exception message
     */
    private static String generateMessage(String tag, int statusCode, String serverMessage) {
        StringBuilder buf = new StringBuilder();
        if (tag != null) {
            buf.append("The request '").append(tag).append('\'');
        } else {
            buf.append("A request");
        }
        buf.append(" failed with status code ").append(statusCode).append('.');

        if (serverMessage != null && !serverMessage.isEmpty()) {
            buf.append(" Message from the server:")
                    .append(System.lineSeparator())
                    .append('"').append(serverMessage).append('"');
        }
        return buf.toString();
    }
}
