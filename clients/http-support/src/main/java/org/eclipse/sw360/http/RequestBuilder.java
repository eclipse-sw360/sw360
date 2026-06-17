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

import java.util.function.Consumer;

/**
 * <p>
 * An interface that allows the definition of HTTP requests to be executed.
 * </p>
 * <p>
 * This interface provides methods to set the single properties of an HTTP
 * request. This can be done in a convenient way using method chaining. A
 * client only has to set the properties that are relevant for a specific
 * request; as a minimum, the request URI must be set. Note that there is no
 * {@code build()} method to conclude the definition of the request; this is
 * not necessary as the HTTP client can figure out itself when to collect the
 * properties that have been defined.
 * </p>
 */
public interface RequestBuilder {
    /**
     * Sets the HTTP method to be used for the request. If no method is set
     * explicitly, the default is GET.
     *
     * @param method the HTTP method for the request
     * @return this request builder
     */
    RequestBuilder method(Method method);

    /**
     * Set the URI for the request.
     *
     * @param uri the request URI
     * @return this request builder
     */
    RequestBuilder uri(String uri);

    /**
     * Sets a request header.
     *
     * @param name  the header name
     * @param value the header value
     * @return this request builder
     */
    RequestBuilder header(String name, String value);

    /**
     * Adds a request body to this builder. The consumer passed to this method
     * is invoked with a {@code RequestBodyBuilder} which can be used to define
     * the request body. Use this method to define request bodies for simple
     * requests; for multi-part requests, {@link #multiPart(String, Consumer)}
     * has to be used instead.
     *
     * @param bodyProducer the producer for the request body
     * @return this request builder
     */
    RequestBuilder body(Consumer<RequestBodyBuilder> bodyProducer);

    /**
     * Adds a part of a multi-part request to this builder. When using this
     * method, a multi-part request is generated. It has to be called for each
     * part. The single parts are defined via consumer objects that are passed
     * {@code RequestPartBuilder} instances for the definition of the parts.
     *
     * @param name         the name of the part
     * @param partProducer the producer for the request part
     * @return this request builder
     */
    RequestBuilder multiPart(String name, Consumer<RequestBodyBuilder> partProducer);

    /**
     * An enumeration class for the HTTP methods supported by the HTTP client.
     */
    enum Method {
        GET, POST, PUT, PATCH, DELETE
    }
}
