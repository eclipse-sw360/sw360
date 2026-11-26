/*
 * Copyright Siemens AG, 2026.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.http;

import okio.Buffer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Jackson3RequestBodyTest {
    @Test
    void shouldUseLowerCamelCaseWithoutMutatingCustomMapper() throws Exception {
        ObjectMapper mapper = JsonMapper.builderWithJackson2Defaults()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
        RequestBodyBuilderImpl builder = new RequestBodyBuilderImpl(mapper);

        builder.json(new Payload());
        Buffer buffer = new Buffer();
        builder.getBody().writeTo(buffer);

        assertEquals("{\"firstName\":\"Example\"}", buffer.readUtf8());
        assertEquals("{\"first_name\":\"Example\"}", mapper.writeValueAsString(new Payload()));
    }

    static class Payload {
        public String firstName = "Example";
    }
}
