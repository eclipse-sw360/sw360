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

package org.eclipse.sw360.rest.resourceserver.core;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimpleAuthenticationEntryPointTest {
    private final SimpleAuthenticationEntryPoint entryPoint = new SimpleAuthenticationEntryPoint();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final BadCredentialsException exception = new BadCredentialsException("Bad credentials");

    @Test
    void shouldPreserveUnauthorizedResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        entryPoint.commence(request, response, exception);

        JsonNode body = JsonMapper.shared().readTree(response.getContentAsString());
        assertEquals(401, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        assertEquals(4, body.size());
        assertEquals(401, body.get("status").asInt());
        assertEquals("Unauthorized", body.get("error").asText());
        assertEquals("Bad credentials", body.get("message").asText());
        assertNotNull(Instant.parse(body.get("timestamp").asText()));
    }

    @Test
    void shouldPropagateWriteFailure() {
        assertThrows(IOException.class,
                () -> entryPoint.commence(request, responseFailingWith("disk error"), exception));
    }

    @Test
    void shouldIgnoreDisconnectedClient() {
        assertDoesNotThrow(() -> entryPoint.commence(request, responseFailingWith("Broken pipe"), exception));
    }

    private static MockHttpServletResponse responseFailingWith(String message) {
        return new MockHttpServletResponse() {
            @Override
            public ServletOutputStream getOutputStream() {
                return new ServletOutputStream() {
                    @Override
                    public void write(int value) throws IOException {
                        throw new IOException(message);
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setWriteListener(WriteListener listener) {
                    }
                };
            }
        };
    }
}
