/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <p>
 * Implementation of the {@code RequestBuilder} interface based on the builder
 * class of HttpClient.
 * </p>
 * <p>
 * This class allows defining the various properties of an HTTP request. Note
 * that you can either set a regular request body using the
 * {@link #body(Consumer)} method or define a multipart
 * request by invoking {@link #multiPart(String, Consumer)} an arbitrary number
 * of times; but it is not possible to call both of these methods.
 * </p>
 */
class NewRequestBuilderImpl implements RequestBuilder {
    /**
     * The mapper for doing JSON serialization.
     */
    private final ObjectMapper mapper;

    /**
     * The internal builder to delegate method calls to.
     */
    private final HttpRequest.Builder requestBuilder;

    /**
     * The name of the HTTP method to be invoked.
     */
    private String httpMethod;

    /**
     * The body of the request.
     */

    private BodyPublisher body;

    /**
     * Creates a new instance of {@code NewRequestBuilderImpl} to build a new
     * request.
     *
     * @param mapper the JSON object mapper
     */
    public NewRequestBuilderImpl(ObjectMapper mapper) {
        this.mapper = mapper;
        requestBuilder = HttpRequest.newBuilder();
        httpMethod = Method.GET.name();
    }

    @Override
    public RequestBuilder method(Method method) {
        httpMethod = method.name();
        return this;
    }

    @Override
    public RequestBuilder uri(String uri) {
        try {
            requestBuilder.uri(new URI(uri));
        } catch (URISyntaxException e) {

        }
        return this;
    }

    @Override
    public RequestBuilder header(String name, String value) {
        requestBuilder.header(name, value);
        return this;
    }

    @Override
    public RequestBuilder body(Consumer<RequestBodyBuilder> bodyProducer) {
        if (getBody() != null) {
            throw new IllegalStateException("A request can only have a single body");
        }
        NewRequestBodyBuilderImpl bodyBuilder = new NewRequestBodyBuilderImpl(mapper);
        bodyProducer.accept(bodyBuilder);
        body = bodyBuilder.getBody();
        return this;
    }

    @Override
    public RequestBuilder multiPart(String name, Consumer<RequestBodyBuilder> partProducer) {
        if (getBody() != null) {
            throw new IllegalStateException("The request already has a normal body. You can either "
                    + "have a body or a multipart request, but not both.");
        }

        NewRequestBodyBuilderImpl bodyBuilder = new NewRequestBodyBuilderImpl(mapper);
        partProducer.accept(bodyBuilder);
        return this;
    }

    /**
     * Returns the final request that has been configured so far.
     *
     * @return the request constructed by this builder
     */
    public HttpRequest build() {
        BodyPublisher requestBody = getBody();
        return requestBuilder.method(httpMethod, requestBody).header("Content-Type", "application/json").build();
    }

    /**
     * Returns the request body that has been defined using this builder.
     *
     * @return the request body (may be <strong>null</strong>)
     */
    BodyPublisher getBody() {
        return body;
    }
}
