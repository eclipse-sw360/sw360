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

/**
 * <p>
 * A class defining some frequently used constants related to HTTP requests and
 * responses.
 * </p>
 */
public final class HttpConstants {
    /**
     * Private constructor to prevent instance creation.
     */
    private HttpConstants() {
    }

    /**
     * Constant for the HTTP status code 200 OK, indicating a successful
     * request.
     */
    public static final int STATUS_OK = 200;

    /**
     * Constant for the HTTP status code 201 CREATED indicating that a resource
     * has been created successfully.
     */
    public static final int STATUS_CREATED = 201;

    /**
     * Constant for the HTTP status code 202 ACCEPTED indicating that a
     * request is processed asynchronously.
     */
    public static final int STATUS_ACCEPTED = 202;

    /**
     * Constant for the HTTP status code 204 NO_CONTENT indicating that a
     * request has a null body.
     */
    public static final int STATUS_NO_CONTENT = 204;

    /**
     * Constant for the HTTP status code 207 MULTI_STATUS that is returned for
     * operations affecting multiple entities. The response body typically
     * contains more information about the single operations and their outcome.
     */
    public static final int STATUS_MULTI_STATUS = 207;

    /**
     * Constant for the HTTP status code 400 BAD REQUEST indicating a general
     * problem with a request sent by a client.
     */
    public static final int STATUS_ERR_BAD_REQUEST = 400;

    /**
     * Constant for the HTTP status code 401 UNAUTHORIZED indicating missing or
     * invalid access credentials.
     */
    public static final int STATUS_ERR_UNAUTHORIZED = 401;

    /**
     * Constant for the HTTP status code 404 NOT FOUND indicating that the
     * resource requested does not exist.
     */
    public static final int STATUS_ERR_NOT_FOUND = 404;

    /**
     * Constant for the HTTP status code 500 INTERNAL SERVER ERROR indicating a
     * general problem on server side.
     */
    public static final int STATUS_ERR_SERVER = 500;

    /**
     * Constant for the HTTP header for setting the content type.
     */
    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    /**
     * Constant for the HTTP header for defining the media type(s) accepted by
     * the client.
     */
    public static final String HEADER_ACCEPT = "Accept";

    /**
     * Constant for the HTTP Authorization header.
     */
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /**
     * Constant for the authentication scheme Basic Auth.
     */
    public static final String AUTH_BASIC = "Basic ";

    /**
     * Constant for the authentication scheme used for bearer tokens. This
     * scheme is used when an OAuth 2.0 access token needs to be passed to the
     * server.
     */
    public static final String AUTH_BEARER = "Bearer ";

    /**
     * Constant for the charset UTF-8. This declaration can be appended to
     * some content types to make the charset used explicit.
     */
    public static final String CHARSET_UTF8 = "; charset=UTF-8";

    /**
     * Constant for the content type application/octet-stream.
     */
    public static final String CONTENT_OCTET_STREAM = "application/octet-stream";

    /**
     * Constant for the content type application/json.
     */
    public static final String CONTENT_JSON = "application/json";

    /**
     * Constant for the content type application/json with UTF-8 encoding.
     */
    public static final String CONTENT_JSON_UTF8 = CONTENT_JSON + CHARSET_UTF8;

    /**
     * Constant for the content type text/plain.
     */
    public static final String CONTENT_TEXT_PLAIN = "text/plain";

    /**
     * Constant for the content type for URL-encoded forms.
     */
    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";

    /**
     * Constant for the character used as separator between URL path segments.
     */
    public static final String URL_PATH_SEPARATOR = "/";
}
