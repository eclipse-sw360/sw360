/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.core;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestClientException extends RuntimeException {
    public BadRequestClientException(String message) {
        super(message);
    }

    public BadRequestClientException(String message, Throwable e) {
        super(message, e);
    }

    public BadRequestClientException(Throwable e) {
        super(e);
    }
}
