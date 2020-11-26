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
package org.eclipse.sw360.antenna.sw360.client.auth;

import org.eclipse.sw360.antenna.http.utils.HttpConstants;
import org.eclipse.sw360.antenna.sw360.client.utils.FutureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * <p>
 * A class managing the OAuth access token required for a request to a SW360
 * server.
 * </p>
 * <p>
 * An instance of this class can be asked to execute a request with a valid
 * access token. The request to be executed is specified using a function that
 * accepts an access token and returns a future result. The management of the
 * token is fully handled by this class: On first access, the token is obtained
 * asynchronously and then cached. The token is valid for a certain time span,
 * typically one hour. When a request with the token yields a 401 Unauthorized
 * error, this is detected by inspecting the future for the result of the
 * request execution. The cache for the token is then invalidated. This causes
 * a new token to be requested the next time a client asks for one.
 * </p>
 * <p>
 * Instances of this class are thread-safe; so requests requiring an access
 * token can be triggered from multiple threads. If multiple threads ask for an
 * access token concurrently, the class ensures that only a single token
 * request is sent to the authentication server.
 * </p>
 */
public class AccessTokenProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AccessTokenProvider.class);

    /**
     * The client for obtaining new access tokens.
     */
    private final SW360AuthenticationClient authClient;

    /**
     * Stores the future with the current access token. Once a token request
     * has been initiated, all requesting clients can be passed this object;
     * they are then notified as soon as the future is completed (or
     * immediately if the future is already completed).
     */
    private CompletableFuture<AccessToken> tokenFuture;

    /**
     * Stores the current access token once it becomes available. This is
     * needed by the {@code invalidate()} method, to make sure that only the
     * correct token gets invalidated.
     */
    private AccessToken accessToken;

    /**
     * Creates a new instance of {@code AccessTokenProvider} that uses the
     * provided {@code SW360AuthenticationClient} to obtain access tokens.
     *
     * @param authClient the underlying authentication client
     */
    public AccessTokenProvider(SW360AuthenticationClient authClient) {
        this.authClient = authClient;
    }

    /**
     * Returns the {@code SW360AuthenticationClient} used by this object.
     *
     * @return the {@code SW360AuthenticationClient}
     */
    public SW360AuthenticationClient getAuthClient() {
        return authClient;
    }

    /**
     * Obtains an access token first and then invokes a function to consume it
     * and produce a future result. This can be used for instance to send a
     * request as soon as a token becomes available.
     *
     * @param func the function producing the future result with the access
     *             token
     * @param <T>  the result type of the function
     * @return the resulting future
     */
    public <T> CompletableFuture<T> doWithToken(Function<? super AccessToken, ? extends CompletableFuture<T>> func) {
        return obtainAccessToken()
                .thenCompose(token -> func.apply(token)
                        .whenComplete((result, exception) -> onRequestComplete(token, exception)));
    }

    /**
     * Returns a future with an {@code AccessToken} to be used to authenticate
     * against the SW360 server. If necessary, the underlying authentication
     * client is asked to fetch a new token. A successful token result is
     * cached and passed directly to clients asking later. In case of an error,
     * the result is not cached, so that a subsequent token request causes
     * another invocation of the {@code SW360AuthenticationClient}.
     *
     * @return a future with the {@code AccessToken}
     */
    protected synchronized CompletableFuture<AccessToken> obtainAccessToken() {
        // The variable is needed to make sure that always a non-null result is
        // returned; even if whenComplete() runs in the same thread.
        CompletableFuture<AccessToken> result = tokenFuture;
        if (tokenFuture == null) {
            LOG.debug("Obtaining a new access token for SW360.");
            tokenFuture = result = getAuthClient().getOAuth2AccessToken()
                    .thenApply(AccessToken::new);
            tokenFuture.whenComplete(this::handleTokenResult);
        }

        return result;
    }

    /**
     * Invalidates the token cached internally if it equals the token
     * specified. This method is called automatically if a request using a
     * token fails with a 401 Unauthorized error. The cache is then cleared, so
     * the next time a client asks for a token, a new one is requested from the
     * server. The token parameter is required to deal with a race condition:
     * If there are multiple concurrent requests that all fail with a 401
     * error, this method will be called multiple times. In this constellation,
     * it has to be prevented that a token that has just been renewed is
     * invalidated immediately again; this is achieved by comparing the cached
     * token against the specified one.
     *
     * @param token the token that should be invalidated
     */
    protected synchronized void invalidate(AccessToken token) {
        if (token.equals(accessToken)) {
            LOG.debug("Invalidating access token for SW360.");
            clearCache();
        }
    }

    /**
     * Handles the result of a request for a new access token. Depending on the
     * outcome, the new token is either cached or the cache is cleared.
     *
     * @param token the token that was received
     * @param error an exception if the operation failed
     */
    private synchronized void handleTokenResult(AccessToken token, Throwable error) {
        if (error != null) {
            clearCache();
            LOG.warn("Could not obtain an access token for SW360.");
            LOG.debug("Stacktrace:", error);
        } else {
            accessToken = token;
        }
    }

    /**
     * An action that is called for each request after its completion. It
     * checks whether the request failed with an exception indicating that the
     * access token has expired. If so, the token cache is invalidated.
     *
     * @param token     the current access token
     * @param exception the exception from the request future
     */
    private void onRequestComplete(AccessToken token, Throwable exception) {
        LOG.debug("Request with access token completed. Exception is {}.", String.valueOf(exception));
        if (FutureUtils.isFailedRequestWithStatus(exception, HttpConstants.STATUS_ERR_UNAUTHORIZED)) {
            invalidate(token);
        }
    }

    /**
     * Clears the cached access token unconditionally. Note that this method
     * must be called from a synchronized block.
     */
    private void clearCache() {
        tokenFuture = null;
        accessToken = null;
    }
}
