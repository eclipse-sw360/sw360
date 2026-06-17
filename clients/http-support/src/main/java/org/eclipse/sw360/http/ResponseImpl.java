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

import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Set;

/**
 * <p>
 * An adapter class that implements the {@code Response} interface of the
 * SW360 HTTP library on top of a response object from OkHttpClient.
 * </p>
 */
class ResponseImpl implements org.eclipse.sw360.http.Response {
    /** An empty stream to be returned if a response has no body.*/
    private static final InputStream EMPTY_STREAM = new ByteArrayInputStream(new byte[0]);

    /** The underlying response wrapped by this instance.*/
    private final Response response;

    /**
     * Creates a new instance of {@code ResponseImpl} and initializes it with
     * the original response to be wrapped.
     * @param response the underlying response from OkHttpClient
     */
    public ResponseImpl(Response response) {
        this.response = response;
    }

    @Override
    public int statusCode() {
        return response.code();
    }

    @Override
    public boolean isSuccess() {
        return response.isSuccessful();
    }

    @Override
    public Set<String> headerNames() {
        return response.headers().names();
    }

    @Override
    public String header(String name) {
        return response.header(name);
    }

    @Override
    public InputStream bodyStream() {
        ResponseBody body = response.body();
        return (body != null) ? body.byteStream() : EMPTY_STREAM;
    }
}
