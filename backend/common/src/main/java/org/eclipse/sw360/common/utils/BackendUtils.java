/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.common.utils;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.apache.http.HttpHeaders;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class BackendUtils {

    private static final String PROPERTIES_FILE_PATH = "/sw360.properties";
    protected static final Properties loadedProperties;

    static {
        loadedProperties = CommonUtils.loadProperties(BackendUtils.class, PROPERTIES_FILE_PATH);
    }

    protected BackendUtils() {
        // Utility class with only static functions
    }

    static public Mono<String> getUriBody(URI uri) {
        // Set timeout of 5 seconds
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofMillis(5000))
                .baseUrl(uri.toString())
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .baseUrl(uri.toString())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build()
                .get()
                .retrieve()
                .onStatus(
                        HttpStatus.NOT_FOUND::equals,
                        response -> Mono.error(new HttpClientErrorException(HttpStatus.NOT_FOUND))
                )
                .onStatus(
                        HttpStatus.INTERNAL_SERVER_ERROR::equals,
                        response -> Mono.error(new ResourceClassNotFoundException("OSADL error"))
                )
                .bodyToMono(String.class);
    }
}
