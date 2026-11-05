/*
 * Copyright (c) Siemens AG, 2026.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.cvesearch.datasource;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CveSearchApiImplHttpTest {

    private static HttpServer server;
    private static CveSearchApiImpl client;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);

        server.createContext("/api/cvefor", exchange -> {
            String path = exchange.getRequestURI().getRawPath();
            if (path.contains("notfound")) {
                byte[] bytes = "{\"error\":\"not found\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(404, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                String json = "[{\"id\":\"CVE-2021-1234\",\"summary\":\"Test vulnerability\"}]";
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        server.createContext("/api/cve/CVE-NOTFOUND", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });

        server.createContext("/api/error500", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });

        server.start();

        int port = server.getAddress().getPort();
        client = new CveSearchApiImpl("http://localhost:" + port);
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldReturnResultsWhenCpeFound() throws IOException {
        List<CveSearchData> results = client.cvefor("cpe:2.3:a:apache:log4net:1.2.9_beta");
        assertThat(results).isNotNull().hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo("CVE-2021-1234");
        assertThat(results.get(0).getSummary()).isEqualTo("Test vulnerability");
    }

    @Test
    void shouldReturnEmptyListWhenCpeReturns404() throws IOException {
        List<CveSearchData> results = client.cvefor("notfound");
        assertThat(results).isNotNull().isEmpty();
    }

    @Test
    void shouldReturnNullWhenSingleCveReturns404() throws IOException {
        CveSearchData result = client.cve("CVE-NOTFOUND");
        assertThat(result).isNull();
    }

    @Test
    void shouldThrowIOExceptionOn500ServerError() {
        CveSearchApiImpl brokenClient = new CveSearchApiImpl("http://localhost:" + server.getAddress().getPort() + "/api/error500");
        assertThatThrownBy(() -> brokenClient.cvefor("any"))
                .isInstanceOf(IOException.class);
    }
}
