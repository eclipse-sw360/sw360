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

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Headers;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.util.function.Consumer;

/**
 * <p>
 * Implementation of the {@code RequestBuilder} interface based on the builder
 * class of OkHttpClient.
 * </p>
 * <p>
 * This class allows defining the various properties of an HTTP request. It can
 * handle multipart requests as well. Note that you can either set a regular
 * request body using the {@link #body(Consumer)} method or define a multipart
 * request by invoking {@link #multiPart(String, Consumer)} an arbitrary number
 * of times; but it is not possible to call both of these methods.
 * </p>
 */
class RequestBuilderImpl implements RequestBuilder {
    /**
     * The mapper for doing JSON serialization.
     */
    private final ObjectMapper mapper;

    /**
     * The internal builder to delegate method calls to.
     */
    private final Request.Builder requestBuilder;

    /**
     * A builder for adding headers to the request.
     */
    private final Headers.Builder headersBuilder;

    /**
     * The name of the HTTP method to be invoked.
     */
    private String httpMethod;

    /**
     * The body of the request.
     */
    private RequestBody body;

    /**
     * A builder for constructing a multi-part request. This is used only if
     * the {@code bodyPart()} method is invoked.
     */
    private MultipartBody.Builder multipartBuilder;

    /**
     * Creates a new instance of {@code RequestBuilderImpl} to build a new
     * request.
     *
     * @param mapper the JSON object mapper
     */
    public RequestBuilderImpl(ObjectMapper mapper) {
        this.mapper = mapper;
        requestBuilder = new Request.Builder();
        headersBuilder = new Headers.Builder();
        httpMethod = Method.GET.name();
    }

    @Override
    public RequestBuilder method(Method method) {
        httpMethod = method.name();
        return this;
    }

    @Override
    public RequestBuilder uri(String uri) {
        requestBuilder.url(uri);
        return this;
    }

    @Override
    public RequestBuilder header(String name, String value) {
        headersBuilder.add(name, value);
        return this;
    }

    @Override
    public RequestBuilder body(Consumer<RequestBodyBuilder> bodyProducer) {
        if (multipartBuilder != null) {
            throw new IllegalStateException("A normal body cannot be added to a multipart request");
        }
        if (getBody() != null) {
            throw new IllegalStateException("A request can only have a single body");
        }

        RequestBodyBuilderImpl bodyBuilder = new RequestBodyBuilderImpl(mapper);
        bodyProducer.accept(bodyBuilder);

        body = bodyBuilder.getBody();
        return this;
    }

    @Override
    public RequestBuilder multiPart(String name, Consumer<RequestBodyBuilder> partProducer) {
        if (getBody() != null) {
            throw new IllegalStateException("The request already has a normal body. You can either " +
                    "have a body or a multipart request, but not both.");
        }

        if (multipartBuilder == null) {
            multipartBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        }
        RequestBodyBuilderImpl bodyBuilder = new RequestBodyBuilderImpl(mapper);
        partProducer.accept(bodyBuilder);
        multipartBuilder.addFormDataPart(name, bodyBuilder.getFileName(), bodyBuilder.getBody());
        return this;
    }

    /**
     * Returns the final request that has been configured so far.
     *
     * @return the request constructed by this builder
     */
    public Request build() {
        RequestBody requestBody = (multipartBuilder != null) ? multipartBuilder.build() : getBody();
        return requestBuilder.method(httpMethod, requestBody)
                .headers(getHeaders())
                .build();
    }

    /**
     * Returns the headers that have been defined using this builder.
     *
     * @return the request headers
     */
    Headers getHeaders() {
        return headersBuilder.build();
    }

    /**
     * Returns the request body that has been defined using this builder.
     *
     * @return the request body (may be <strong>null</strong>)
     */
    RequestBody getBody() {
        return body;
    }
}
