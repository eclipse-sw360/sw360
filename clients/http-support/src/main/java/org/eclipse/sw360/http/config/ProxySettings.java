/*
 * Copyright (c) Bosch Software Innovations GmbH 2016-2019.
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

import java.util.Objects;

/**
 * <p>
 * A class defining proxy settings for HTTP clients.
 * </p>
 * <p>
 * This class allows configuring a proxy to be used for HTTP connections. The
 * proxy's host address and port number can be configured. It is also possible
 * to indicate that no proxy should be used.
 * </p>
 */
public final class ProxySettings {
    /**
     * Constant representing an undefined proxy host. If this value is set for
     * the host, this is interpreted as the default proxy selector.
     */
    private static final String UNDEFINED_HOST = "";

    /**
     * Constant representing an undefined port.
     */
    private static final int UNDEFINED_PORT = -1;

    /**
     * Constant for an instance pointing to the default proxy selector.
     */
    private static final ProxySettings DEFAULT_SELECTOR_SETTINGS = new ProxySettings(UNDEFINED_HOST, 0);

    /**
     * Constant for an instance that disables proxy usage.
     */
    private static final ProxySettings NO_PROXY_SETTINGS = new ProxySettings(null, UNDEFINED_PORT);

    private final String proxyHost;
    private final int proxyPort;

    /**
     * Creates a new instance of {@code ProxySettings} with the proxy
     * parameters specified.
     *
     * @param proxyHost the proxy host
     * @param proxyPort the proxy port
     */
    private ProxySettings(String proxyHost, int proxyPort) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
    }

    /**
     * Returns an instance of {@code ProxySettings} that indicates that no
     * proxy is to be used.
     *
     * @return an instance with an empty proxy configuration
     */
    public static ProxySettings noProxy() {
        return NO_PROXY_SETTINGS;
    }

    /**
     * Returns an instance of {@code ProxySettings} that indicates that the
     * default {@code ProxySelector} (obtained via the
     * {@code ProxySelector.getDefault()} method) should be queried for each
     * request. In this mode, the usage of proxies can be enabled or disabled
     * on a per URL basis.
     *
     * @return an instance enabling the default {@code ProxySelector}
     */
    public static ProxySettings defaultProxySelector() {
        return DEFAULT_SELECTOR_SETTINGS;
    }

    /**
     * Creates a new instance of {@code ProxySettings} that uses the specified
     * settings for the proxy.
     *
     * @param host the host address of the proxy
     * @param port the port of the proxy
     * @return the new {@code ProxySettings} instance
     */
    public static ProxySettings useProxy(String host, int port) {
        return new ProxySettings(host, port);
    }

    /**
     * Creates a new instance of {@code ProxySettings} that is initialized from
     * configuration settings. In the configuration, it can be stated
     * explicitly whether a proxy is to be used or not. So it is possible that
     * valid settings for the proxy host and port are provided, but the
     * resulting settings should nevertheless refer to an undefined proxy. If
     * proxy usage is explicitly enabled, but no valid proxy host or port are
     * configured, the resulting settings point to the default proxy selector.
     *
     * @param useProxy flag whether a proxy should be used
     * @param host     the proxy host (may be undefined)
     * @param port     the proxy port (may be undefined)
     * @return the new {@code ProxySettings} instance
     */
    public static ProxySettings fromConfig(boolean useProxy, String host, int port) {
        if (!useProxy) {
            return noProxy();
        }

        return host != null && !UNDEFINED_HOST.equals(host) && port > 0 ? useProxy(host, port) :
                defaultProxySelector();
    }

    /**
     * Returns a flag whether a proxy should be used.
     *
     * @return <strong>true</strong> if the proxy server defined by this object
     * should be used; <strong>false</strong> for a direct Internet connection
     */
    public boolean isProxyUse() {
        return getProxyHost() != null && getProxyPort() != UNDEFINED_PORT;
    }

    /**
     * Returns a flag whether the default proxy selector should be used. In
     * this mode, from the {@code ProxySelector.getDefault()} method the
     * default proxy selector is obtained. This selector is then called for
     * each request to obtain a proxy on a per URL basis.
     *
     * @return <strong>true</strong> if the default proxy selector should be
     * used; <strong>false</strong> otherwise
     */
    public boolean isDefaultProxySelectorUse() {
        return UNDEFINED_HOST.equals(getProxyHost());
    }

    /**
     * Returns a flag whether a proxy should be completely disabled. In this
     * mode, a direct Internet connection is enforced.
     *
     * @return <strong>true</strong> if direct Internet connections are used;
     * <strong>false</strong> whether requests go via a proxy
     */
    public boolean isNoProxy() {
        return !isProxyUse() && !isDefaultProxySelectorUse();
    }

    /**
     * Returns the proxy host. This method only returns a defined value if
     * {@link #isProxyUse()} returns <strong>true</strong>.
     *
     * @return the proxy host address
     */
    public String getProxyHost() {
        return proxyHost;
    }

    /**
     * Returns the proxy port. This method only returns a defined value if
     * {@link #isProxyUse()} returns <strong>true</strong>.
     *
     * @return the proxy port
     */
    public int getProxyPort() {
        return proxyPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProxySettings settings = (ProxySettings) o;
        return getProxyPort() == settings.getProxyPort() &&
                Objects.equals(getProxyHost(), settings.getProxyHost());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getProxyHost(), getProxyPort());
    }

    @Override
    public String toString() {
        return "ProxySettings{" +
                ", proxyHost='" + proxyHost + '\'' +
                ", proxyPort=" + proxyPort +
                '}';
    }

}
