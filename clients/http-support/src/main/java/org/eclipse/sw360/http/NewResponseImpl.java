/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.http;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.http.HttpResponse;
import java.util.Set;


/**
 * <p>
 * An adapter class that implements the {@code Response} interface of the
 * SW360 HTTP library on top of a response object from HttpClient.
 * </p>
 */
class NewResponseImpl<T extends Serializable> implements org.eclipse.sw360.http.Response {

    /** An empty stream to be returned if a response has no body. */
    private static final InputStream EMPTY_STREAM = new ByteArrayInputStream(new byte[0]);

    /** The underlying response wrapped by this instance. */
    private final HttpResponse response;

    NewResponseImpl(HttpResponse response) {
        this.response = response;
    }

    @Override
    public int statusCode() {
        return response.statusCode();
    }

    @Override
    public boolean isSuccess() {
        return String.valueOf(response.statusCode()).startsWith("2");
    }

    @Override
    public Set<String> headerNames() {
        return response.headers().map().keySet();
    }

    @Override
    public String header(String name) {
        return response.headers().firstValue(name).orElse("");
    }

    @Override
    public InputStream bodyStream() {
        String responseInString = response.body().toString();
        return responseInString.equals("") ? EMPTY_STREAM : new ByteArrayInputStream(responseInString.getBytes());
    }
}
