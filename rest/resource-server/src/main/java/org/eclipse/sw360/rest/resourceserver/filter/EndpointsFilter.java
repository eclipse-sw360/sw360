/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.resourceserver.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


@Component
public class EndpointsFilter extends OncePerRequestFilter {

    private static final Logger log = LogManager.getLogger(EndpointsFilter.class);

    private static final List<String> MUTATION_METHODS = List.of("POST", "PATCH", "PUT", "DELETE");

    /** GET paths blocked for VIEWER — defense-in-depth alongside controller guards. */
    private static final List<String> VIEWER_BLOCKED_GET_PREFIXES = List.of(
            "/api/clearingrequest",
            "/api/moderationrequest",
            "/api/ecc",
            "/api/vulnerabilities",
            "/api/obligations"
    );

    /** POST endpoints whitelisted for VIEWER (e.g., READ token creation). */
    private static final String VIEWER_TOKEN_ENDPOINT = "/api/users/tokens";

    @Value("${blacklist.sw360.rest.api.endpoints}")
    String endpointsTobeBlackListed;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    public EndpointsFilter(RestControllerHelper restControllerHelper) {
        this.restControllerHelper = restControllerHelper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        String[] endpointMethodPairs = endpointsTobeBlackListed.split(",");
        Map<String, Set<String>> endpointHttpMethods = getMapOfEndpointToHttpMethods(endpointMethodPairs);
        boolean isAMatch = verifyMatchingOfRequestURIToEndpoints(requestURI, endpointHttpMethods);

        if (MUTATION_METHODS.contains(method) && !isAMatch) {
            User user = restControllerHelper.getSw360UserFromAuthentication();
            if (PermissionUtils.isSecurityUser(user) || PermissionUtils.isViewer(user)) {
                // Allow VIEWER to create READ-only API tokens
                if (PermissionUtils.isViewer(user) && "POST".equals(method)
                        && requestURI.startsWith(VIEWER_TOKEN_ENDPOINT)) {
                    filterChain.doFilter(request, response);
                } else {
                    log.info("Blocked {} {} for read-only user: {}", method, requestURI, user.getEmail());
                    response.sendError(HttpStatus.FORBIDDEN.value());
                }
            } else {
                filterChain.doFilter(request, response);
            }
        } else if (!isAMatch) {
            // For GET requests, check VIEWER blocked paths
            if (isViewerBlockedGetPath(requestURI)) {
                User user = restControllerHelper.getSw360UserFromAuthentication();
                if (PermissionUtils.isViewer(user)) {
                    log.info("Blocked GET {} for VIEWER: {}", requestURI, user.getEmail());
                    response.sendError(HttpStatus.FORBIDDEN.value());
                    return;
                }
            }
            filterChain.doFilter(request, response);
        } else {
            handleBlacklistedEndpoint(request, response, filterChain, endpointHttpMethods, requestURI, method);
        }
    }


    private boolean isViewerBlockedGetPath(String requestURI) {
        return VIEWER_BLOCKED_GET_PREFIXES.stream()
                .anyMatch(requestURI::startsWith);
    }

    private void handleBlacklistedEndpoint(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain, Map<String, Set<String>> endpointHttpMethods,
            String requestURI, String method) throws ServletException, IOException {
        Set<String> httpMethodsToBeBlocked = new HashSet<>();
        Optional<Entry<String, Set<String>>> matchedEndpoint = endpointHttpMethods.entrySet().stream()
                .filter(es -> getRequestURIMatcher().test(es.getKey(), requestURI))
                .findFirst();
        if (matchedEndpoint.isPresent()) {
            httpMethodsToBeBlocked = matchedEndpoint.get().getValue();
        }
        if (CommonUtils.isNullOrEmptyCollection(httpMethodsToBeBlocked)
                || httpMethodsToBeBlocked.contains(method)) {
            response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value());
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private boolean verifyMatchingOfRequestURIToEndpoints(String requestURI,
            Map<String, Set<String>> endpointHttpMethods) {
        long count = endpointHttpMethods.entrySet().stream().filter(es -> {
            return getRequestURIMatcher().test(es.getKey(), requestURI);
        }).count();
        return count != 0;
    }

    private Map<String, Set<String>> getMapOfEndpointToHttpMethods(String[] endpointMethodPairs) {
        Map<String, Set<String>> endpointHttpMethods = new HashMap<>();
        Arrays.stream(endpointMethodPairs).forEach(pair -> {
            String[] parts = pair.trim().split(":");
            String endpointPath = parts[0];
            if (endpointPath.contains("{")) {
                endpointPath = endpointPath.replaceAll("\\{\\w+\\}", "\\\\w+");
            }
            if (parts.length == 2) {
                String httpMethod = parts[1];
                endpointHttpMethods.computeIfAbsent(endpointPath, k -> new HashSet<>()).add(httpMethod);
            } else {
                endpointHttpMethods.put(endpointPath, Collections.emptySet());
            }
        });
        return endpointHttpMethods;
    }

    private BiPredicate<String, String> getRequestURIMatcher() {
        return (endpointURI, requestURI) -> {
            Pattern endpointPattern = Pattern.compile(endpointURI);
            Matcher requestUriMatcher = endpointPattern.matcher(requestURI);
            return requestUriMatcher.matches();
        };
    }
}
