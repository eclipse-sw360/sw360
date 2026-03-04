/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Writes API responses to cache on MISS.
 * Serialization is synchronous, file write is async.
 */
@ControllerAdvice
public class CachedResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private static final Logger log = LogManager.getLogger(CachedResponseBodyAdvice.class);

    private final ApiResponseCacheManager cacheManager;
    private final CacheVariantResolver variantResolver;
    private final ObjectMapper objectMapper;
    private final Executor cacheExecutor;
    private final Map<CachedEndpoint, CacheCondition> conditionMap;

    public CachedResponseBodyAdvice(ApiResponseCacheManager cacheManager,
                                    CacheVariantResolver variantResolver,
                                    ObjectMapper objectMapper,
                                    Executor cacheExecutor,
                                    List<CacheCondition> conditions) {
        this.cacheManager = cacheManager;
        this.variantResolver = variantResolver;
        this.objectMapper = objectMapper;
        this.cacheExecutor = cacheExecutor;
        this.conditionMap = conditions != null
                ? conditions.stream().collect(Collectors.toMap(CacheCondition::endpoint, Function.identity()))
                : Collections.emptyMap();
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.hasMethodAnnotation(CachedResponse.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null) {
            return null;
        }

        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return body;
        }

        CachedResponse annotation = returnType.getMethodAnnotation(CachedResponse.class);
        if (annotation == null) {
            return body;
        }

        // Find the actual endpoint to cache by matching CacheCondition
        CachedEndpoint endpoint = findMatchingEndpoint(annotation.endpoints(), servletRequest.getServletRequest());
        if (endpoint == null) {
            return body;
        }

        if (!cacheManager.isEndpointEnabled(endpoint)) {
            return body;
        }

        String variant = cacheManager.isPerRoleCachingEnabled(endpoint)
                ? variantResolver.resolve()
                : ApiResponseCacheManager.DEFAULT_VARIANT;

        ResponseCache<?> cache = cacheManager.getCache(endpoint, variant);
        if (cache.isPresent()) {
            return body;
        }

        try {
            long start = System.currentTimeMillis();
            String json = objectMapper.writeValueAsString(body);
            long duration = System.currentTimeMillis() - start;
            log.info("Cache MISS for {} variant={} — serialized {} bytes in {}ms, writing async",
                    endpoint, variant, json.length(), duration);

            cacheExecutor.execute(() -> {
                try {
                    cacheManager.getCache(endpoint, variant).put(json);
                    log.info("Cache written for {} variant={} — {} bytes", endpoint, variant, json.length());
                } catch (Exception e) {
                    log.error("Failed to write cache for {}: {}", endpoint, e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            log.error("Failed to serialize response for {}: {}", endpoint, e.getMessage(), e);
        }

        return body;
    }

    /**
     * Find the matching endpoint from the annotation's endpoints array
     * by checking which CacheCondition matches the request.
     *
     * @param endpoints array of potential endpoints from annotation
     * @param request the servlet request
     * @return the matching endpoint, or null if none match
     */
    private CachedEndpoint findMatchingEndpoint(CachedEndpoint[] endpoints, jakarta.servlet.http.HttpServletRequest request) {
        for (CachedEndpoint endpoint : endpoints) {
            CacheCondition condition = conditionMap.get(endpoint);
            if (condition != null && condition.isCacheable(request)) {
                return endpoint;
            }
        }
        return null;
    }
}
