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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import okhttp3.MediaType;
import okhttp3.RequestBody;

import java.nio.file.Path;

/**
 * <p>
 * Implementation of the {@code RequestBodyBuilder} interface on top of the
 * OkHttpClient library.
 * </p>
 * <p>
 * The class allows defining a request body in different flavours. For a
 * specific request, only a single variant can be used; attempts to call
 * multiple defining methods yield an {@code IllegalStateException} exception.
 * </p>
 */
class RequestBodyBuilderImpl implements RequestBodyBuilder {
    /**
     * The mapper for doing JSON serialization.
     */
    private final ObjectMapper mapper;

    /**
     * Stores a representation of the body that has been set so far.
     */
    private RequestBody body;

    /**
     * Stores the name of a file to be uploaded. This field is only defined if
     * a body of type file has been set.
     */
    private String fileName;

    /**
     * Creates a new {@code RequestBodyBuilderImpl} object and initializes it
     * with the JSON object mapper.
     *
     * @param mapper the JSON mapper
     */
    public RequestBodyBuilderImpl(ObjectMapper mapper) {
        this.mapper = mapper;
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE);
    }

    @Override
    public void string(String str, String mediaType) {
        initBody(RequestBody.create(str, MediaType.parse(mediaType)));
    }

    @Override
    public void file(Path path, String mediaType) {
        initBody(RequestBody.create(path.toFile(), MediaType.parse(mediaType)));
        Path fileNamePath = path.getFileName();
        fileName = (fileNamePath != null) ? fileNamePath.toString() : null;
    }

    @Override
    public void json(Object payload) {
        try {
            string(mapper.writeValueAsString(payload), "application/json");
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the request body that has been generated based on the
     * interactions with this builder. Throws an exception if no body has been
     * set. (We assume that it is an error in the usage of this library to
     * request a body builder, but do not actually define a body.)
     *
     * @return the {@code RequestBody}
     * @throws IllegalStateException if the request body is undefined
     */
    public RequestBody getBody() {
        if (body == null) {
            throw new IllegalStateException("A RequestBodyBuilder was requested, but no body was defined.");
        }
        return body;
    }

    /**
     * Returns the file name for a file upload request. A file name is only
     * defined if the {@code file()} method was called.
     *
     * @return a file name for an upload request
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Initializes the body created by this builder. Throws an exception if a
     * body has already been defined.
     *
     * @param b the new request body
     * @throws IllegalStateException if there is already a body
     */
    private void initBody(RequestBody b) {
        if (body != null) {
            throw new IllegalStateException("Multiple body definitions. Only a single request body can " +
                    "be defined using a RequestBodyBuilder.");
        }

        body = b;
    }
}
