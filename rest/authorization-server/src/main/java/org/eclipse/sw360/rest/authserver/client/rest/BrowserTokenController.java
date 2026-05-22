/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.client.rest;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * Browser-friendly compatibility shim for legacy SW360 v18 token issuance.
 *
 * <p>The Spring Authorization Server token endpoint at
 * {@code POST /oauth2/token} requires HTTP Basic (or POST-body) client
 * authentication and only accepts POST per RFC 6749 §3.2. The legacy v18
 * authserver, by contrast, also accepted {@code GET} with credentials in
 * the query string, and the v20 communication to tool owners promises the
 * same browser-friendly URL during the 3-month parallel-auth window
 * (June 29 → Sept 4, 2026).
 *
 * <p>This controller bridges
 * {@code GET /token?grant_type=client_credentials&client_id=...&client_secret=...&scope=...}
 * to the standard {@code POST /oauth2/token} on the in-process Spring AS,
 * returning the same JSON access-token payload Spring AS produces.
 *
 * <p>Security notes:
 * <ul>
 *   <li>Exclusively {@code grant_type=client_credentials}. Any other grant
 *       type is rejected so the bridge cannot be abused for
 *       authorization-code or password flows.</li>
 *   <li>The {@code client_secret} travels in the query string and will
 *       therefore appear in browser history, web-server access logs, and
 *       any proxy logs in between. This is a deliberate, time-bounded
 *       trade-off matching the v18 behaviour; operators must scrub or
 *       restrict access to authserver access logs accordingly. Only the
 *       {@code client_id} is logged by this controller — never the
 *       secret.</li>
 *   <li>Every response carries RFC 8594 {@code Deprecation} and
 *       {@code Sunset} headers fixed at 2026-09-04T23:59:59 GMT — the
 *       cutoff after which the legacy authserver is retired and clients
 *       must use Keycloak.</li>
 * </ul>
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc8594">RFC 8594 — Sunset HTTP Header</a>
 */
@RestController
@RequiredArgsConstructor
public class BrowserTokenController {

    private static final Logger log = LogManager.getLogger(BrowserTokenController.class);

    /**
     * 2026-09-04 23:59:59 UTC — the moment the legacy authserver is retired
     * per the v20 migration plan. Formatted per RFC 8594 §3 (which
     * requires an IMF-fixdate per RFC 7231).
     */
    private static final String DEPRECATION_HEADER = "Fri, 04 Sep 2026 23:59:59 GMT";
    private static final String SUNSET_HEADER = DEPRECATION_HEADER;

    private static final String SUPPORTED_GRANT_TYPE = "client_credentials";

    /**
     * Authserver HTTP port. Defaults to 8080 to match Spring Boot's default
     * when {@code server.port} is unset.
     */
    @Value("${server.port:8080}")
    private int serverPort;

    /**
     * Servlet context path under which Spring AS endpoints live. The legacy
     * SW360 deployment hosts the authserver at {@code /authorization}; that
     * is the default we fall back to so the loopback URL is correct
     * out-of-the-box.
     */
    @Value("${server.servlet.context-path:/authorization}")
    private String contextPath;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Translate the legacy {@code GET /token} query-string flow into a
     * standard {@code POST /oauth2/token} call to the in-process Spring
     * Authorization Server. The token JSON returned by Spring AS is
     * forwarded verbatim to the caller.
     */
    @GetMapping(path = "/token", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> issueToken(
            @RequestParam(value = "grant_type", required = false) String grantType,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "client_secret", required = false) String clientSecret,
            @RequestParam(value = "scope", required = false) String scope
    ) {
        HttpHeaders responseHeaders = baseResponseHeaders();

        if (!SUPPORTED_GRANT_TYPE.equals(grantType)) {
            log.warn("Browser-token bridge rejected unsupported grant_type={}", grantType);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .headers(responseHeaders)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"unsupported_grant_type\","
                            + "\"error_description\":\"only client_credentials is supported by this endpoint\"}");
        }

        if (isBlank(clientId) || isBlank(clientSecret)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .headers(responseHeaders)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"invalid_request\","
                            + "\"error_description\":\"client_id and client_secret are required\"}");
        }

        log.info("Browser-token bridge: issuing client_credentials token for client_id={}", clientId);

        String loopbackUrl = "http://localhost:" + serverPort + contextPath + "/oauth2/token";

        HttpHeaders forwardHeaders = new HttpHeaders();
        forwardHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        forwardHeaders.setBasicAuth(clientId, clientSecret);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", SUPPORTED_GRANT_TYPE);
        if (!isBlank(scope)) {
            form.add("scope", scope);
        }

        try {
            ResponseEntity<String> upstream = restTemplate.exchange(
                    loopbackUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(form, forwardHeaders),
                    String.class);
            return ResponseEntity.status(upstream.getStatusCode())
                    .headers(responseHeaders)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(upstream.getBody());
        } catch (HttpStatusCodeException e) {
            // Spring AS returned 4xx/5xx (bad credentials, invalid scope,
            // unknown client, ...). Forward the standard OAuth2 error JSON
            // body verbatim so callers see Spring AS error semantics.
            return ResponseEntity.status(e.getStatusCode())
                    .headers(responseHeaders)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(e.getResponseBodyAsString());
        }
    }

    private static HttpHeaders baseResponseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Deprecation", DEPRECATION_HEADER);
        headers.add("Sunset", SUNSET_HEADER);
        // OAuth2 §5.1 — token responses must not be cached.
        headers.add("Cache-Control", "no-store");
        headers.add("Pragma", "no-cache");
        return headers;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
