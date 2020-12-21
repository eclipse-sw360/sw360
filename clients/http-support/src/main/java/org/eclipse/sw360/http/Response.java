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
package org.eclipse.sw360.http;

import java.io.InputStream;
import java.util.Set;

/**
 * <p>
 * An interface describing the response of an HTTP request.
 * </p>
 * <p>
 * This interface is used by {@link ResponseProcessor} objects to access the
 * data received from the HTTP server. It provides query methods for the
 * typical information, such as the HTTP status code, the headers, or the
 * response body as a stream.
 * </p>
 * <p>
 * Note that underlying HTTP libraries used to implement this interface
 * typically place some restrictions on the usage of the methods provided here,
 * especially with regards to the response body. So an implementation should
 * expect that the body stream can be consumed only once. On the other hand, it
 * is not necessary to explicitly release any of the resources that might be
 * hold by a response object; this is done by the framework.
 * </p>
 */
public interface Response {
    /**
     * Returns the HTTP status code of this response.
     *
     * @return the HTTP status code
     */
    int statusCode();

    /**
     * Returns a flag whether the request was successful. This is a shortcut
     * for testing the {@link #statusCode()} against a range of well-known HTTP
     * status codes. For certain use cases with special requirements for the
     * codes returned by the server, it may be necessary to do the check
     * manually.
     *
     * @return <strong>true</strong> if the request has been successful;
     * <strong>false</strong> otherwise
     */
    boolean isSuccess();

    /**
     * Returns a set with the names of the headers contained in the response.
     *
     * @return a set with the names of the defined headers
     */
    Set<String> headerNames();

    /**
     * Returns the value of the header with the given name.
     *
     * @param name the name of the header in question
     * @return the value of this header or <strong>null</strong> if it is not
     * present
     */
    String header(String name);

    /**
     * Returns an {@code InputStream} for the body of this response; so the
     * data received from the server can be streamed. Result is never
     * <strong>null</strong>; if the request did not yield a response body, an
     * empty stream is returned.
     *
     * @return an {@code InputStream} to access the response body
     */
    InputStream bodyStream();
}
