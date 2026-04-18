/*
 * Copyright Siemens AG, 2017,2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.core.serializer;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.time.Instant;

@Component
public class Json3InstantSerializer extends ValueSerializer<Instant> {
    @Override
    public void serialize(Instant instant, JsonGenerator gen,
                          SerializationContext ctxt) throws JacksonException {
        String timeStamp = instant.toString();
        gen.writeString(timeStamp);
    }
}
