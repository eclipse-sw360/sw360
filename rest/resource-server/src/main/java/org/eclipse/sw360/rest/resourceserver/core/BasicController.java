/*
 * Copyright Bosch Software Innovations GmbH, 2019.
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

import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.stream.Collectors;

public abstract class BasicController<T, S extends ResourceSupport> implements ResourceProcessor<S> {
    protected ResponseEntity<Resources<Resource<T>>> mkResponse(List<T> objects) {
        Resources<Resource<T>> resources = new Resources<>(objects.stream()
                .map(r -> new Resource<>(r))
                .collect(Collectors.toList()));
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }
}
