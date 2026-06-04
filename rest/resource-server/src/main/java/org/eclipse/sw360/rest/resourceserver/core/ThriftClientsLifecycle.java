/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.core;

import jakarta.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.springframework.stereotype.Component;

/**
 * Ensures the shared, pooled Thrift HTTP client is gracefully closed when the
 * Spring application context shuts down.
 *
 * <p>{@link ThriftClients} uses a JVM-wide static {@code CloseableHttpClient}
 * backed by a {@code PoolingHttpClientConnectionManager}. Without an explicit
 * lifecycle hook, the pooled connections and their underlying sockets would
 * only be released by GC — potentially leaving dangling TCP connections on
 * rapid restarts or during graceful shutdown. This component ensures
 * {@link ThriftClients#closeSharedClient()} is invoked deterministically before
 * the JVM exits.
 *
 * <p>This is intentionally a thin wrapper rather than a full conversion of
 * {@code ThriftClients} to a Spring-managed bean, to minimise the call-site
 * changes required across the codebase.
 */
@Component
public class ThriftClientsLifecycle {

    private static final Logger log = LogManager.getLogger(ThriftClientsLifecycle.class);

    @PreDestroy
    public void onShutdown() {
        log.info("Spring context shutting down — closing shared Thrift HTTP client pool.");
        ThriftClients.closeSharedClient();
    }
}
