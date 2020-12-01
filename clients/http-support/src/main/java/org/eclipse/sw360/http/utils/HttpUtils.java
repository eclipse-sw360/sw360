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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.eclipse.sw360.http.HttpClient;
import org.eclipse.sw360.http.RequestBuilder;
import org.eclipse.sw360.http.Response;
import org.eclipse.sw360.http.ResponseProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * <p>
 * A class providing static utility functions related to the usage of
 * {@link HttpClient} objects.
 * </p>
 */
public final class HttpUtils {
    /**
     * A predicate that can be used by the
     * {@link #checkResponse(ResponseProcessor, Predicate)} method to check
     * whether a response was successful. This predicate returns
     * <strong>true</strong> if and only if the passed in {@code Response}
     * reports itself as successful.
     *
     * @see Response#isSuccess()
     */
    public static final Predicate<Response> SUCCESS_STATUS = Response::isSuccess;

    /**
     * Separator character for multiple URL query parameters.
     */
    private static final String PARAMETER_SEPARATOR = "&";

    /**
     * Separator between a parameter key and its value.
     */
    private static final char KEY_VALUE_SEPARATOR = '=';

    /**
     * The character that separates the query string from the URL.
     */
    private static final char QUERY_PREFIX = '?';

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);

    /**
     * Private constructor to prevent the creation of instances.
     */
    private HttpUtils() {
    }

    /**
     * Blocks until the given future is completed, returns its result, and
     * handles occurring exceptions. {@code HttpClient} has an asynchronous
     * API, but this method offers an easy way to transform this to a blocking
     * programming model by just waiting for the result to become available.
     * To simplify exception handling for the caller, the various checked
     * exceptions thrown by {@code Future.get()} are wrapped into
     * {@code IOException} exceptions.
     *
     * @param future the future to wait for
     * @param <T>    the result type of the future
     * @return the result produced by the future
     * @throws IOException if the future failed or waiting was interrupted
     */
    public static <T> T waitFor(Future<? extends T> future) throws IOException {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();  // reset interrupted flag
            throw new IOException(e);
        } catch (ExecutionException e) {
            throw wrapInIOException(e.getCause());
        }
    }

    /**
     * Returns the first exception in a chain that is not a
     * {@code CompletionException}. When dealing with {@code CompletableFuture}
     * objects that have been completed with an exception (for instance by
     * processing futures via methods like {@code whenComplete()} or
     * {@code handle()}), the representation of these exceptions is not always
     * consistent. They are sometimes wrapped in a {@code CompletionException}
     * and sometimes not. This method can be used to obtain the actual cause of
     * a failure by removing all enclosing {@code CompletionException}
     * instances.
     *
     * @param ex the exception to be unwrapped
     * @return the unwrapped exception or <strong>null</strong> if none can be
     * found
     */
    public static Throwable unwrapCompletionException(Throwable ex) {
        Throwable cause = ex;
        while (cause instanceof CompletionException) {
            cause = cause.getCause();
        }

        return cause;
    }

    /**
     * Returns a flag whether the passed in response status code indicates a
     * successful response. This is the case if the status code is in the range
     * of [200, 300).
     *
     * @param status the status code to check
     * @return <strong>true</strong> if the status code indicates a successful
     * response; <strong>false</strong> otherwise
     */
    public static boolean isSuccessStatus(int status) {
        return status >= HttpConstants.STATUS_OK && status < 300;
    }

    /**
     * Returns a {@code ResponseProcessor} that checks whether a request was
     * successful based on a given predicate and allows tagging the request.
     * The resulting processor invokes the given predicate on the response
     * received from the server. If the predicate yields
     * <strong>false</strong>, a {@link FailedRequestException} is thrown
     * that is initialized with the tag and the response status code.
     * Otherwise, the original {@code ResponseProcessor} is invoked, which can
     * now safely generate its result.
     *
     * @param processor        the {@code ResponseProcessor} to wrap
     * @param successPredicate a predicate to determine whether the response is
     *                         successful
     * @param tag              a tag to identify the request
     * @param <T>              the result type of the {@code ResponseProcessor}
     * @return the {@code ResponseProcessor} checking the response
     */
    public static <T> ResponseProcessor<T> checkResponse(ResponseProcessor<T> processor,
                                                         Predicate<Response> successPredicate,
                                                         String tag) {
        return response -> {
            if (!successPredicate.test(response)) {
                throw createExceptionForResponse(response, tag);
            }
            return processor.process(response);
        };
    }

    /**
     * Returns a {@code ResponseProcessor} that checks whether a request was
     * successful based on a given predicate. This variant does not add a tag
     * to the request.
     *
     * @param processor        the {@code ResponseProcessor} to wrap
     * @param successPredicate a predicate to determine whether the response is
     *                         successful
     * @param <T>              the result type of the {@code ResponseProcessor}
     * @return the {@code ResponseProcessor} checking the response
     */
    public static <T> ResponseProcessor<T> checkResponse(ResponseProcessor<T> processor,
                                                         Predicate<Response> successPredicate) {
        return checkResponse(processor, successPredicate, null);
    }

    /**
     * Returns a {@code ResponseProcessor} that checks the HTTP status code to
     * determine whether a request was successful and allows tagging the
     * request. This method is equivalent to the overloaded
     * {@code checkResponse()} method using {@link #SUCCESS_STATUS} as
     * predicate.
     *
     * @param processor the {@code ResponseProcessor} to wrap
     * @param tag       a tag to identify the request
     * @param <T>       the result type of the {@code ResponseProcessor}
     * @return the {@code ResponseProcessor} checking the response
     */
    public static <T> ResponseProcessor<T> checkResponse(ResponseProcessor<T> processor, String tag) {
        return checkResponse(processor, SUCCESS_STATUS, tag);
    }

    /**
     * Returns a {@code ResponseProcessor} that checks the HTTP status code to
     * determine whether a request was successful. This method is equivalent to
     * the overloaded {@code checkResponse()} method using
     * {@link #SUCCESS_STATUS} as predicate. This variant does not add a tag
     * to the request.
     *
     * @param processor the {@code ResponseProcessor} to wrap
     * @param <T>       the result type of the {@code ResponseProcessor}
     * @return the {@code ResponseProcessor} checking the response
     */
    public static <T> ResponseProcessor<T> checkResponse(ResponseProcessor<T> processor) {
        return checkResponse(processor, SUCCESS_STATUS);
    }

    /**
     * Returns a predicate that checks a response for a specific status code.
     * The response is considered successful if and only if the status code
     * matches exactly the expected code.
     *
     * @param status the expected status code for the response
     * @return a predicate checking for this response status code
     */
    public static Predicate<Response> hasStatus(int status) {
        return response -> response.statusCode() == status;
    }

    /**
     * Creates a {@code FailedRequestException} based on the passed in response
     * and request tag. The properties of the exception are initialized
     * accordingly. The response entity is read as well and stored in the
     * exception.
     *
     * @param response the failing response
     * @param tag      a tag to identify the request
     * @return the exception reporting a failed request
     */
    public static FailedRequestException createExceptionForResponse(Response response, String tag) {
        StringBuilder buf = new StringBuilder();
        InputStream bodyStream = response.bodyStream();
        if (bodyStream != null) {
            try (Reader reader = new InputStreamReader(bodyStream, StandardCharsets.UTF_8)) {
                IOUtils.copy(reader, buf);
            } catch (IOException e) {
                LOGGER.warn("Could not read server message when handling failed request '{}'.", tag, e);
            }
        }
        return new FailedRequestException(tag, response.statusCode(), buf.toString());
    }

    /**
     * Returns a {@code ResponseProcessor} that uses the {@code ObjectMapper}
     * specified to map the JSON payload of a response to an object of the
     * given result class. The resulting processor directly accesses the
     * payload of the response; it can be combined with one of the
     * {@code checkResponse()} methods to make sure that the response is
     * successful before it is processed.
     *
     * @param mapper      the JSON mapper
     * @param resultClass the result class
     * @param <T>         the type of the resulting object
     * @return the {@code ResponseProcessor} doing a JSON de-serialization
     */
    public static <T> ResponseProcessor<T> jsonResult(ObjectMapper mapper, Class<T> resultClass) {
        return response -> mapper.readValue(response.bodyStream(), resultClass);
    }

    /**
     * Returns a {@code ResponseProcessor} that uses the {@code ObjectMapper}
     * specified to map the JSON payload of a response to an object of the
     * type defined by the given reference. This is analogous to the overloaded
     * method, but allows for more flexibility  to specify the result type.
     *
     * @param mapper        the JSON mapper
     * @param typeReference the reference defining the target type
     * @param <T>           the type of the resulting object
     * @return the {@code ResponseProcessor} doing a JSON de-serialization
     */
    public static <T> ResponseProcessor<T> jsonResult(ObjectMapper mapper, TypeReference<T> typeReference) {
        return response -> mapper.readValue(response.bodyStream(), typeReference);
    }

    /**
     * Returns a very simple {@code Consumer} for a {@code RequestBuilder} that
     * just configures the builder with the given URI. This causes an HTTP GET
     * request to this URI without further properties.
     *
     * @param uri the URI to be retrieved
     * @return the {@code Consumer} generating this GET request
     */
    public static Consumer<RequestBuilder> get(String uri) {
        return builder -> builder.uri(uri);
    }

    /**
     * Returns a special {@code ResponseProcessor} that does not do any
     * processing, but just returns the value <strong>null</strong>. Such a
     * processor can be useful for requests that do not return a response body,
     * e.g. POST requests to create or manipulate entities. When combined with
     * a processor produced by one of the {@code checkResponse()} methods it is
     * possible to have some error handling, but skip the evaluation of the
     * response body.
     *
     * @return a {@code ResponseProcessor} returning <strong>null</strong>
     */
    public static <T> ResponseProcessor<T> nullProcessor() {
        return response -> null;
    }

    /**
     * Performs URL encoding on the given string.
     *
     * @param src the string to be encoded
     * @return the encoded string (<strong>null</strong> if the input string
     * was <strong>null</strong>)
     */
    public static String urlEncode(String src) {
        if (src == null) {
            return null;
        }

        try {
            return URLEncoder.encode(src, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // this can never happen
            throw new AssertionError("UTF-8 charset not supported!");
        }
    }

    /**
     * Adds the given query parameters to a URL without applying any filtering.
     *
     * @param url    the URL
     * @param params a map with the query parameters to append
     * @return the resulting URL string with query parameters
     * @throws NullPointerException if the URL or the map with parameters is
     *                              <strong>null</strong>
     * @see #addQueryParameters(String, Map, boolean)
     */
    public static String addQueryParameters(String url, Map<String, ?> params) {
        return addQueryParameters(url, params, false);
    }

    /**
     * Adds the given query parameters to a URL. Each parameter value is
     * encoded. The parameters are appended to the URL string using the correct
     * separator characters. If the {@code filterUndefined} flag is set to
     * <strong>true</strong>, parameters having a null or empty string value
     * are filtered out.
     *
     * @param url             the URL
     * @param params          a map with the query parameters to append
     * @param filterUndefined flag whether undefined parameters should be
     *                        ignored
     * @return the resulting URL string with query parameters
     * @throws NullPointerException if the URL or the map with parameters is
     *                              <strong>null</strong>
     */
    public static String addQueryParameters(String url, Map<String, ?> params, boolean filterUndefined) {
        if (url == null) {
            throw new NullPointerException("URL must not be null");
        }
        String paramStr = params.entrySet().stream()
                .filter(entry -> !filterUndefined || isParameterDefined(entry.getValue()))
                .map(entry -> encodeQueryParameter(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(PARAMETER_SEPARATOR));
        return paramStr.isEmpty() ? url : url + QUERY_PREFIX + paramStr;
    }

    /**
     * Adds a single query parameter to a URL. This is a convenience function
     * for the case that there is only a single query parameter needed. Note
     * that this method does not filter out undefined parameters.
     *
     * @param url   the URL
     * @param key   the parameter key
     * @param value the parameter value
     * @return the URL with the query parameter added
     * @throws NullPointerException if the URL is <strong>null</strong>
     */
    public static String addQueryParameter(String url, String key, Object value) {
        return addQueryParameters(url, Collections.singletonMap(key, value));
    }

    /**
     * Wraps the given exception into an {@code IOException} if necessary. If
     * the exception is already an {@code IOException}, it is returned
     * directly.
     *
     * @param e the exception to be wrapped
     * @return the IOException wrapping the exception
     */
    private static IOException wrapInIOException(Throwable e) {
        return e instanceof IOException ? (IOException) e :
                new IOException(e);
    }

    /**
     * Generates the encoded form of a single query parameter.
     *
     * @param key   the parameter key
     * @param value the value
     * @return the encoded form of this parameter
     */
    private static String encodeQueryParameter(String key, Object value) {
        String encValue = value == null ? "" : urlEncode(value.toString());
        return urlEncode(key) + KEY_VALUE_SEPARATOR + encValue;
    }

    /**
     * Checks whether the value of a parameter is defined. This is used to
     * filter out undefined query parameters.
     *
     * @param value the value to check
     * @return a flag whether this value is defined
     */
    private static boolean isParameterDefined(Object value) {
        return value != null && !value.toString().isEmpty();
    }
}
