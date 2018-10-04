/*
 * Copyright Bosch Software Innovations GmbH, 2018.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.core;

import org.springframework.http.HttpStatus;

public class MultiStatus {

    private String resourceId;
    private HttpStatus status;

    public MultiStatus() {}

    public MultiStatus(String resourceId, HttpStatus status) {
        this.resourceId = resourceId;
        this.status = status;
    }

    public String getResourceId() {
        return resourceId;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    public int getStatusCode() {
        return status.value();
    }

}
