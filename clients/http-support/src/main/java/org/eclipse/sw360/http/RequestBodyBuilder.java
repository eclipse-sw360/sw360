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

import java.nio.file.Path;

/**
 * <p>
 * A builder interface for defining the body of a request.
 * </p>
 * <p>
 * This interface provides several methods for defining a request body with
 * different content. It is used for both regular request bodies and the bodies
 * of the parts of a multipart request. An implementation is passed to a
 * consumer which then invokes the desired method to set the body. Note that
 * the methods are mutually exclusive; a concrete builder instance will allow
 * only a single method call.
 * </p>
 */
public interface RequestBodyBuilder {
    /**
     * Sets the request body as a string. Based on the media type, the content
     * header is set.
     *
     * @param body      the request body as string
     * @param mediaType the media type of the content
     */
    void string(String body, String mediaType);

    /**
     * Sets the request body as a file. This can be used to upload files to a
     * server. Based on the media type, the content header is set.
     *
     * @param path      the path to the file to be uploaded
     * @param mediaType the media type of the content
     */
    void file(Path path, String mediaType);

    /**
     * Sets the request body as an object that is serialized to JSON. This
     * method uses an internal JSON object mapper to generate a JSON
     * representation from the object passed in. It also automatically sets a
     * correct {@code Content-Type} header.
     *
     * @param payload the object to be used as request payload
     */
    void json(Object payload);
}
