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
package org.eclipse.sw360.http.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * A class collecting all configuration options supported by the HTTP client
 * library.
 * </p>
 * <p>
 * An object of this class is needed to create a new {@code HttpClient}
 * instance. There is a default instance available, which can be adapted by
 * changing configuration options. Instances are immutable; changing an option
 * returns a modified copy.
 * </p>
 */
public final class HttpClientConfig {
    /**
     * Constant for the basic configuration instance.
     */
    private static final HttpClientConfig BASIC_CONFIG =
            new HttpClientConfig(null, ProxySettings.defaultProxySelector());

    /**
     * Stores a custom JSON object mapper. The field is null if no custom
     * mapper has been set.
     */
    private final ObjectMapper customObjectMapper;

    /**
     * Stores the proxy settings for this configuration.
     */
    private final ProxySettings proxySettings;

    /**
     * Creates a new instance of {@code HttpClientConfig} with the parameters
     * specified.
     *
     * @param customObjectMapper an optional custom JSON mapper
     * @param proxySettings      the proxy settings
     */
    private HttpClientConfig(ObjectMapper customObjectMapper, ProxySettings proxySettings) {
        this.customObjectMapper = customObjectMapper;
        this.proxySettings = proxySettings;
    }

    /**
     * Returns a basic {@code HttpClientConfig} instance. This instance
     * contains a minimum configuration. It can be used for simple use cases
     * or serve as a starting point for constructing custom configurations.
     *
     * @return the basic configuration instance
     */
    public static HttpClientConfig basicConfig() {
        return BASIC_CONFIG;
    }

    /**
     * Returns an {@code Optional} with a custom JSON object mapper to be used
     * for dealing with JSON payload. If this {@code Optional} is empty, a new
     * mapper (without a special configuration) is created and used by the HTTP
     * client.
     *
     * @return an {@code Optional} with a custom JSON object mapper
     */
    public Optional<ObjectMapper> customObjectMapper() {
        return Optional.ofNullable(customObjectMapper);
    }

    /**
     * Returns an {@code ObjectMapper} instance that is either the custom
     * mapper which has been set explicitly or a newly created instance.
     * This method can be used to obtain a correct and non-null JSON mapper
     * instance.
     *
     * @return the JSON mapper configured by this class
     */
    public ObjectMapper getOrCreateObjectMapper() {
        return customObjectMapper().orElseGet(ObjectMapper::new);
    }

    /**
     * Returns a {@code ProxySettings} object with the proxy configuration to
     * be used.
     *
     * @return the proxy configuration for the HTTP library
     */
    public ProxySettings proxySettings() {
        return proxySettings;
    }

    /**
     * Returns a new instance of {@code HttpClientConfig} that contains the
     * same settings as this instance, but with the JSON object mapper set to
     * the parameter specified.
     *
     * @param newMapper the JSON object mapper (can be <strong>null</strong> to
     *                  remove a special custom object mapper)
     * @return the new {@code HttpClientConfig} instance
     */
    public HttpClientConfig withObjectMapper(ObjectMapper newMapper) {
        return new HttpClientConfig(newMapper, proxySettings);
    }

    /**
     * Returns a new instance of {@code HttpClientConfig} that contains the
     * same settings as this instance, but with the proxy configuration set to
     * the parameter specified.
     *
     * @param newProxySettings the proxy configuration to be set
     * @return the new {@code HttpClientConfig} instance
     */
    public HttpClientConfig withProxySettings(ProxySettings newProxySettings) {
        return new HttpClientConfig(customObjectMapper, newProxySettings);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HttpClientConfig that = (HttpClientConfig) o;
        return Objects.equals(customObjectMapper, that.customObjectMapper) &&
                Objects.equals(proxySettings, that.proxySettings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customObjectMapper, proxySettings);
    }
}
