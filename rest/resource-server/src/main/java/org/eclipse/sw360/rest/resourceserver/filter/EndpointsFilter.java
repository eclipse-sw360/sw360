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

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class EndpointsFilter extends OncePerRequestFilter {

    @Value("${blacklist.sw360.rest.api.endpoints}")
    String endpointsTobeBlackListed;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        String[] endpointMethodPairs = endpointsTobeBlackListed.split(",");
        Map<String, Set<String>> endpointHttpMethods = getMapOfEndpointToHttpMethods(endpointMethodPairs);
        boolean isAMatch = verifyMatchingOfRequestURIToEndpoints(requestURI, endpointHttpMethods);

        if (!isAMatch) {
            filterChain.doFilter(request, response);
        } else {
            Set<String> httpMethodsToBeBlocked = new HashSet<>();
            Optional<Entry<String, Set<String>>> matchedEndpointToHttpMethods = endpointHttpMethods.entrySet().stream().filter(es -> {
                String endpointURI = es.getKey();
                return getRequestURIMatcher().test(endpointURI, requestURI);
            }).findFirst();
            if (matchedEndpointToHttpMethods.isPresent()) {
                httpMethodsToBeBlocked = matchedEndpointToHttpMethods.get().getValue();
            }

            if (CommonUtils.isNullOrEmptyCollection(httpMethodsToBeBlocked)) {
                response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value());
            } else {
                if (httpMethodsToBeBlocked.contains(method)) {
                    response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value());
                } else {
                    filterChain.doFilter(request, response);
                }
            }
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