/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.configurations;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.eclipse.sw360.datahandler.services.common.ConfigContainer;
import org.eclipse.sw360.datahandler.services.common.ConfigFor;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.users.User;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * HTTP implementation of {@link ConfigurationsClient} with a short TTL cache for
 * {@link #getConfigByKey(String)} (hot path via {@code SW360Utils.readConfig}).
 */
public class ConfigurationsServiceRestClient implements ConfigurationsClient {

    private static final String BASE = "/configurations/api/configurations";
    private static final Duration CACHE_TTL = Duration.ofSeconds(30);
    private static final ParameterizedTypeReference<Map<String, String>> STRING_MAP =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final ConcurrentHashMap<String, CacheEntry> keyCache = new ConcurrentHashMap<>();

    private record CacheEntry(String value, long expiresAtMs) {
        boolean valid() {
            return System.currentTimeMillis() < expiresAtMs;
        }
    }

    public ConfigurationsServiceRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    private static <T> T call(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            throw new SW360Exception(body == null || body.isEmpty() ? e.getMessage() : body,
                    e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new SW360Exception(e.getMessage(), 503, e);
        }
    }

    private static void addUser(HttpHeaders headers, User user) {
        if (user == null) {
            return;
        }
        if (user.getEmail() != null) {
            headers.set("X-User-Email", user.getEmail());
        }
        if (user.getDepartment() != null) {
            headers.set("X-User-Department", user.getDepartment());
        }
        if (user.getUserGroup() != null) {
            headers.set("X-User-Group", user.getUserGroup().name());
        }
    }

    @Override
    public RequestStatus createSW360Configs(ConfigContainer newConfig) {
        invalidateCache();
        return call(() -> restClient.post().uri(BASE).body(newConfig).retrieve().body(RequestStatus.class));
    }

    @Override
    public Map<String, String> getSW360Configs() {
        Map<String, String> map = call(() -> restClient.get().uri(BASE).retrieve().body(STRING_MAP));
        return map == null ? Map.of() : map;
    }

    @Override
    public String getConfigByKey(String key) {
        CacheEntry cached = keyCache.get(key);
        if (cached != null && cached.valid()) {
            return cached.value();
        }
        String value = call(() -> restClient.get().uri(BASE + "/{key}", key).retrieve().body(String.class));
        keyCache.put(key, new CacheEntry(value, System.currentTimeMillis() + CACHE_TTL.toMillis()));
        return value;
    }

    @Override
    public Map<String, String> getConfigForContainer(ConfigFor configFor) {
        Map<String, String> map = call(() -> restClient.get()
                .uri(BASE + "/group/{configFor}", configFor)
                .retrieve()
                .body(STRING_MAP));
        return map == null ? Map.of() : map;
    }

    @Override
    public RequestStatus updateSW360Configs(Map<String, String> updatedConfigs, User user) {
        invalidateCache();
        return call(() -> restClient.put()
                .uri(BASE)
                .headers(h -> addUser(h, user))
                .body(updatedConfigs)
                .retrieve()
                .body(RequestStatus.class));
    }

    @Override
    public RequestStatus updateSW360ConfigForContainer(ConfigFor configFor, Map<String, String> updatedConfigs,
            User user) {
        invalidateCache();
        return call(() -> restClient.put()
                .uri(BASE + "/group/{configFor}", configFor)
                .headers(h -> addUser(h, user))
                .body(updatedConfigs)
                .retrieve()
                .body(RequestStatus.class));
    }

    @Override
    public void invalidateCache() {
        keyCache.clear();
    }
}
