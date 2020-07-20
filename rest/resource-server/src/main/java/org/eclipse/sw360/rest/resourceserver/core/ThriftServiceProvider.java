/*
 * Copyright Bosch.IO GmbH 2020.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.core;

import org.apache.thrift.transport.TTransportException;

/**
 * <p>
 * A generic interface used by the REST services layer to obtain references to
 * Thrift services.
 * </p>
 * <p>
 * Via concrete implementations of this interface, {@code Sw360XXXService}
 * classes can obtain their underlying Thrift service. The implementations are
 * managed and injected by Spring. This makes it possible to inject mock
 * services in tests.
 * </p>
 *
 * @param <T> the type of the Thrift service provided by this interface
 */
@FunctionalInterface
public interface ThriftServiceProvider<T> {
    /**
     * Returns a new instance of the underlying Thrift service that can be
     * reached under the given base URL. As such Thrift services are typically
     * not thread-safe, an implementation has to ensure that for each thread a
     * different service instance is returned.
     *
     * @param thriftServerUrl the base URL for all Thrift services
     * @return the Thrift service instance
     * @throws TTransportException if an error occurs
     */
    T getService(String thriftServerUrl) throws TTransportException;
}
