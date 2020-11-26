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

import org.eclipse.sw360.antenna.http.utils.FailedRequestException;
import org.eclipse.sw360.antenna.http.utils.HttpConstants;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AccessTokenProviderTest {
    /**
     * Constant for a test token.
     */
    private static final String TOKEN = "a_test_access_token";

    /**
     * Mock for the authentication client.
     */
    private SW360AuthenticationClient authClient;

    /**
     * The token provider to be tested.
     */
    private AccessTokenProvider tokenProvider;

    @Before
    public void setUp() {
        authClient = mock(SW360AuthenticationClient.class);
        tokenProvider = new AccessTokenProvider(authClient);
    }

    /**
     * Prepares the mock for the authentication client to expect requests for
     * access tokens and sets the results to be returned.
     *
     * @param result      the first result
     * @param moreResults an arbitrary number of further results
     */
    @SafeVarargs
    private final void expectTokenRequest(CompletableFuture<String> result, CompletableFuture<String>... moreResults) {
        when(authClient.getOAuth2AccessToken()).thenReturn(result, moreResults);
    }

    @Test
    public void testAccessTokenCanBeObtained() {
        CompletableFuture<String> tokenFuture = CompletableFuture.completedFuture(TOKEN);
        expectTokenRequest(tokenFuture);

        AccessToken accessToken = tokenProvider.obtainAccessToken().join();
        assertThat(accessToken.getToken()).isEqualTo(TOKEN);
    }

    @Test
    public void testAccessTokenIsCached() {
        CompletableFuture<String> tokenFuture = CompletableFuture.completedFuture(TOKEN);
        expectTokenRequest(tokenFuture);
        AccessToken accessToken1 = tokenProvider.obtainAccessToken().join();

        AccessToken accessToken2 = tokenProvider.obtainAccessToken().join();
        assertThat(accessToken2).isSameAs(accessToken1);
        verify(authClient).getOAuth2AccessToken();  // exactly once
    }

    @Test
    public void testCachedTokenCanBeReset() {
        final String otherToken = "another_test_token";
        CompletableFuture<String> tokenFuture1 = CompletableFuture.completedFuture(TOKEN);
        CompletableFuture<String> tokenFuture2 = CompletableFuture.completedFuture(otherToken);
        expectTokenRequest(tokenFuture1, tokenFuture2);
        tokenProvider.obtainAccessToken().join();

        tokenProvider.invalidate(new AccessToken(TOKEN));
        AccessToken accessToken2 = tokenProvider.obtainAccessToken().join();
        assertThat(accessToken2.getToken()).isEqualTo(otherToken);
    }

    @Test
    public void testInvalidateOfAnotherToken() {
        CompletableFuture<String> tokenFuture = CompletableFuture.completedFuture(TOKEN);
        expectTokenRequest(tokenFuture);
        tokenProvider.obtainAccessToken().join();

        tokenProvider.invalidate(new AccessToken("some other token"));
        AccessToken accessToken = tokenProvider.obtainAccessToken().join();
        assertThat(accessToken.getToken()).isEqualTo(TOKEN);
        verify(authClient).getOAuth2AccessToken();  // exactly once
    }

    @Test
    public void testMultipleInvalidatesForTheSameTokenAreHandledCorrectly() {
        final String token2 = "refreshed_access_token";
        CompletableFuture<String> tokenFuture1 = CompletableFuture.completedFuture(TOKEN);
        CompletableFuture<String> tokenFuture2 = new CompletableFuture<>();
        CompletableFuture<String> tokenFutureUndesired = CompletableFuture.completedFuture("not_wanted");
        AccessToken expiredToken = new AccessToken(TOKEN);
        expectTokenRequest(tokenFuture1, tokenFuture2, tokenFutureUndesired);
        tokenProvider.obtainAccessToken().join();
        tokenProvider.invalidate(expiredToken);

        CompletableFuture<AccessToken> tokenResult1 = tokenProvider.obtainAccessToken();
        tokenProvider.invalidate(expiredToken);  // should be a noop
        CompletableFuture<AccessToken> tokenResult2 = tokenProvider.obtainAccessToken();
        tokenFuture2.complete(token2);
        assertThat(tokenResult1.join().getToken()).isEqualTo(token2);
        assertThat(tokenResult2.join().getToken()).isEqualTo(token2);
    }

    @Test
    public void testFailedResultIsNotCached() {
        CompletableFuture<String> tokenFutureFailed = new CompletableFuture<>();
        tokenFutureFailed.completeExceptionally(new IOException("no token"));
        CompletableFuture<String> tokenFutureSuccess = CompletableFuture.completedFuture(TOKEN);
        expectTokenRequest(tokenFutureFailed, tokenFutureSuccess);
        tokenProvider.obtainAccessToken();

        AccessToken accessToken = tokenProvider.obtainAccessToken().join();
        assertThat(accessToken.getToken()).isEqualTo(TOKEN);
    }

    @Test
    public void testDoWithTokenFailedAccessToken() {
        Throwable expException = new IOException("Cannot obtain access token");
        RuntimeException otherException = new RuntimeException("Function failed");
        Function<AccessToken, CompletableFuture<Object>> func = token -> {
            throw otherException;
        };
        CompletableFuture<String> tokenFuture = new CompletableFuture<>();
        tokenFuture.completeExceptionally(expException);
        expectTokenRequest(tokenFuture);

        try {
            tokenProvider.doWithToken(func).join();
            fail("No exception thrown!");
        } catch (CompletionException ex) {
            assertThat(ex.getCause()).isEqualTo(expException);
        }
    }

    @Test
    public void testDoWithTokenSuccess() {
        CompletableFuture<String> tokenFuture = CompletableFuture.completedFuture(TOKEN);
        Function<AccessToken, CompletableFuture<Object>> func =
                token -> CompletableFuture.completedFuture(token.getToken());
        expectTokenRequest(tokenFuture);

        Object result = tokenProvider.doWithToken(func).join();
        assertThat(result).isEqualTo(TOKEN);
    }

    /**
     * Returns a function to be passed to {@code doWithToken()} that produces
     * a future failing with the passed in exception.
     *
     * @param exception the exception to fail the future with
     * @return the future simulating a failed request
     */
    private static Function<AccessToken, CompletableFuture<Object>> failedRequestFunction(Throwable exception) {
        return token -> {
            CompletableFuture<Object> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(exception);
            return failedFuture;
        };
    }

    @Test
    public void testDoWithTokenFailedRequest() {
        Throwable exception = new FailedRequestException("failure", HttpConstants.STATUS_ERR_NOT_FOUND);
        Function<AccessToken, CompletableFuture<Object>> func = failedRequestFunction(exception);
        expectTokenRequest(CompletableFuture.completedFuture(TOKEN), CompletableFuture.completedFuture("other"));

        try {
            tokenProvider.doWithToken(func).join();
            fail("No exception thrown!");
        } catch (CompletionException ex) {
            assertThat(ex.getCause()).isEqualTo(exception);
        }
        assertThat(tokenProvider.obtainAccessToken().join().getToken()).isEqualTo(TOKEN);
    }

    @Test
    public void testDoWithTokenFailedRequestTokenExpired() {
        String freshToken = "refreshed_access_token";
        Throwable exception = new FailedRequestException("failure", HttpConstants.STATUS_ERR_UNAUTHORIZED);
        Function<AccessToken, CompletableFuture<Object>> func = failedRequestFunction(exception);
        expectTokenRequest(CompletableFuture.completedFuture(TOKEN),
                CompletableFuture.completedFuture(freshToken));

        try {
            tokenProvider.doWithToken(func).join();
            fail("No exception thrown!");
        } catch (CompletionException ex) {
            assertThat(ex.getCause()).isEqualTo(exception);
        }
        assertThat(tokenProvider.obtainAccessToken().join().getToken()).isEqualTo(freshToken);
    }
}
