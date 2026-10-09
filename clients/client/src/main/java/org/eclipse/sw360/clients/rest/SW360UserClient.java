/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.clients.rest;

import org.apache.commons.io.IOUtils;
import org.eclipse.sw360.http.RequestBuilder;
import org.eclipse.sw360.http.utils.HttpConstants;
import org.eclipse.sw360.http.utils.HttpUtils;
import org.eclipse.sw360.clients.auth.AccessTokenProvider;
import org.eclipse.sw360.clients.config.SW360ClientConfig;
import org.eclipse.sw360.clients.rest.resource.users.SW360User;
import org.eclipse.sw360.clients.rest.resource.users.SW360UserList;
import org.eclipse.sw360.clients.utils.SW360ResourceUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * An SW360 REST client implementation providing basic functionality related to
 * the {@code /users} endpoint, including profile lookup and API token
 * management for the authenticated user.
 * </p>
 */
public class SW360UserClient extends SW360Client {
    static final String TAG_GET_USERS = "get_users";

    static final String TAG_GET_USER_PROFILE = "get_user_profile";

    static final String TAG_GET_USER_BY_EMAIL = "get_user_by_email";

    static final String TAG_CREATE_TOKEN = "post_create_token";

    static final String TAG_REVOKE_TOKEN = "delete_token";

    private static final String USERS_ENDPOINT = "users";

    private static final String PROFILE_ENDPOINT = "profile";

    private static final String TOKENS_ENDPOINT = "tokens";

    public SW360UserClient(SW360ClientConfig config, AccessTokenProvider provider) {
        super(config, provider);
    }

    public CompletableFuture<List<SW360User>> getUsers() {
        return executeJsonRequestWithDefault(HttpUtils.get(resourceUrl(USERS_ENDPOINT)), SW360UserList.class,
                TAG_GET_USERS, SW360UserList::new)
                .thenApply(SW360ResourceUtils::getSw360Users);
    }

    public CompletableFuture<SW360User> getUserProfile() {
        return executeJsonRequest(HttpUtils.get(resourceUrl(USERS_ENDPOINT, PROFILE_ENDPOINT)), SW360User.class,
                TAG_GET_USER_PROFILE);
    }

    public CompletableFuture<SW360User> getUserByEmail(String email) {
        return executeJsonRequest(HttpUtils.get(resourceUrl(USERS_ENDPOINT, HttpUtils.urlEncode(email))),
                SW360User.class, TAG_GET_USER_BY_EMAIL);
    }

    public CompletableFuture<String> createToken(String name, Set<String> authorities, String expirationDate) {
        Map<String, Object> payload = Map.of(
                "name", name,
                "authorities", authorities,
                "expirationDate", expirationDate);
        return executeRequest(builder -> builder.method(RequestBuilder.Method.POST)
                        .uri(resourceUrl(USERS_ENDPOINT, TOKENS_ENDPOINT))
                        .body(body -> body.json(payload)),
                HttpUtils.checkResponse(response -> IOUtils.toString(response.bodyStream(), StandardCharsets.UTF_8),
                        HttpUtils.hasStatus(HttpConstants.STATUS_CREATED), TAG_CREATE_TOKEN),
                TAG_CREATE_TOKEN);
    }

    public CompletableFuture<Integer> revokeToken(String name) {
        String url = HttpUtils.addQueryParameter(resourceUrl(USERS_ENDPOINT, TOKENS_ENDPOINT), "name", name);
        return executeRequest(builder -> builder.uri(url).method(RequestBuilder.Method.DELETE),
                HttpUtils.checkResponse(response -> response.statusCode(),
                        HttpUtils.hasStatus(HttpConstants.STATUS_NO_CONTENT), TAG_REVOKE_TOKEN),
                TAG_REVOKE_TOKEN);
    }
}
