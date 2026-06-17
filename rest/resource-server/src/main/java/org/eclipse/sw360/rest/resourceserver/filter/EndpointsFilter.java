/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.resourceserver.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.NonNull;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class EndpointsFilter extends OncePerRequestFilter {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PATCH", "PUT", "DELETE");

    private final Map<Pattern, Set<String>> endpointHttpMethods;
    private final RestControllerHelper<?> restControllerHelper;

    public EndpointsFilter(@NonNull RestControllerHelper<?> restControllerHelper,
            @Value("${blacklist.sw360.rest.api.endpoints:}") String endpointsTobeBlackListed) {
        this.restControllerHelper = restControllerHelper;
        this.endpointHttpMethods = parseEndpointHttpMethods(endpointsTobeBlackListed);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Optional<Entry<Pattern, Set<String>>> matchedEndpoint = findMatchingEndpoint(request.getRequestURI());
        if (matchedEndpoint.isEmpty()) {
            if (isWriteMethod(request.getMethod()) && PermissionUtils.isSecurityUser(restControllerHelper.getSw360UserFromAuthentication())) {
                sendServiceUnavailable(response);
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        Set<String> blockedMethods = matchedEndpoint.get().getValue();
        if (CommonUtils.isNullOrEmptyCollection(blockedMethods)
                || blockedMethods.contains(request.getMethod().toUpperCase(Locale.ROOT))) {
            sendServiceUnavailable(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isWriteMethod(String method) {
        return WRITE_METHODS.contains(method.toUpperCase(Locale.ROOT));
    }

    private Optional<Entry<Pattern, Set<String>>> findMatchingEndpoint(String requestUri) {
        return endpointHttpMethods.entrySet().stream()
                .filter(entry -> entry.getKey().matcher(requestUri).matches())
                .findFirst();
    }

    private Map<Pattern, Set<String>> parseEndpointHttpMethods(String endpointsTobeBlackListed) {
        Map<Pattern, Set<String>> parsedEndpointHttpMethods = new HashMap<>();
        if (CommonUtils.isNullEmptyOrWhitespace(endpointsTobeBlackListed)) {
            return parsedEndpointHttpMethods;
        }

        for (String endpointMethodPair : endpointsTobeBlackListed.split(",")) {
            if (CommonUtils.isNullEmptyOrWhitespace(endpointMethodPair)) {
                continue;
            }
            String[] parts = endpointMethodPair.trim().split(":");
            String endpointPath = parts[0];
            if (endpointPath.contains("{")) {
                endpointPath = endpointPath.replaceAll("\\{\\w+\\}", "\\\\w+");
            }
            Pattern endpointPattern = Pattern.compile(endpointPath);
            if (parts.length == 2) {
                parsedEndpointHttpMethods.computeIfAbsent(endpointPattern, ignored -> new HashSet<>())
                        .add(parts[1].trim().toUpperCase(Locale.ROOT));
            } else {
                parsedEndpointHttpMethods.put(endpointPattern, Set.of());
            }
        }

        return parsedEndpointHttpMethods;
    }

    private void sendServiceUnavailable(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value());
    }
}
