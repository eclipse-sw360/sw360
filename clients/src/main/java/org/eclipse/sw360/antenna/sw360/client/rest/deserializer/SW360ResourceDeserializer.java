/*
 * Copyright (c) Bosch Software Innovations GmbH 2018.
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.client.rest.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.Embedded;

/**
 * <p>
 * Implementation of a custom JSON deserializer for the hierarchy of
 * {@code Embedded} classes.
 * </p>
 * <p>
 * Normally, the Jackson deserializer has sufficient information to derive the
 * class of an object to be de-serialized from the context, i.e. the object
 * that embeds another one. Only in the special case that no concrete value is
 * available, is this implementation invoked. Therefore, this implementation
 * always returns <strong>null</strong>; this will cause a dummy embedded
 * object to be used.
 * </p>
 */
public class SW360ResourceDeserializer extends JsonDeserializer<Embedded> {
    @Override
    public Embedded deserialize(JsonParser p, DeserializationContext ctxt) {
        return null;
    }
}
