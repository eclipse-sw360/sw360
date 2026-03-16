/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.http;

import java.io.FileNotFoundException;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.file.Path;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

/**
 * <p>
 * Implementation of the {@code RequestBodyBuilder} interface on top of the
 * Native Http library.
 * </p>
 * <p>
 * The class allows defining a request body in different flavours. For a
 * specific request, only a single variant can be used; attempts to call
 * multiple defining methods yield an {@code IllegalStateException} exception.
 * </p>
 */
class NewRequestBodyBuilderImpl implements RequestBodyBuilder {
    /**
     * The mapper for doing JSON serialization.
     */
    private final ObjectMapper mapper;

    /**
     * Stores a representation of the body that has been set so far.
     */
    private BodyPublisher body;

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
    public NewRequestBodyBuilderImpl(ObjectMapper mapper) {
        this.mapper = mapper;
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
    }

    @Override
    public void string(String str, String mediaType) {
        initBody(BodyPublishers.ofString(str));
    }

    /**
     * Sets the request body to the contents of the given file.
     * Throws an IllegalStateException if the file does not exist or is not readable.
     *
     * @param path      the path to the file
     * @param mediaType the media type (currently unused, set header in request builder)
     * @throws IllegalStateException if the file does not exist or is not readable
     */
    @Override
    public void file(Path path, String mediaType) {
        if (path == null) {
            throw new IllegalStateException("File path must not be null");
        }
        if (!java.nio.file.Files.exists(path)) {
            throw new IllegalStateException("File does not exist: " + path);
        }
        if (!java.nio.file.Files.isReadable(path)) {
            throw new IllegalStateException("File is not readable: " + path);
        }
        try {
            initBody(BodyPublishers.ofFile(path));
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        }
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
    public BodyPublisher getBody() {
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
    private void initBody(BodyPublisher b) {
        if (body != null) {
            throw new IllegalStateException("Multiple body definitions. Only a single request body can " +
                    "be defined using a RequestBodyBuilder.");
        }
        body = b;
    }
}
