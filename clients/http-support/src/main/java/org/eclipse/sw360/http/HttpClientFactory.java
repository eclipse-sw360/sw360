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
package org.eclipse.sw360.http;

import org.eclipse.sw360.http.config.HttpClientConfig;

/**
 * <p>
 * Definition of an interface that allows the creation of an {@link HttpClient}
 * object.
 * </p>
 * <p>
 * This interface defines a factory method that can be used to create new,
 * fully configured instances of {@link HttpClient}. The new instances are
 * created based on a configuration object.
 * </p>
 */
public interface HttpClientFactory {
    /**
     * Creates a new instance of {@code HttpClient} and configures it
     * according to the passed in configuration object.
     *
     * @param config the configuration for the new client
     * @return the new {@code HttpClient} instance
     */
    HttpClient newHttpClient(HttpClientConfig config);
}
