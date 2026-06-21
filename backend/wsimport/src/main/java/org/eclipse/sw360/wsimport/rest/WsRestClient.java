/*
 * Copyright (c) Verifa Oy, 2018-2019. Part of the SW360 Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.wsimport.rest;

import com.github.cliftonlabs.json_simple.JsonObject;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.services.projectimport.TokenCredentials;
import org.eclipse.sw360.wsimport.utility.TranslationConstants;
import org.eclipse.sw360.wsimport.utility.WsTokenType;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author: ksoranko@verifa.io
 */
@Component
public class WsRestClient {

    private static final Logger LOGGER = LogManager.getLogger(WsRestClient.class);

    private String generateRequestBody(String requestType, String userKey, WsTokenType tokenType, String token) {
        JsonObject json = new JsonObject();
        json.put("requestType", requestType);
        json.put("userKey", userKey);
        json.put(tokenType.toString(), token);
        return json.toString();
    }

    String getData(String requestString, String token, WsTokenType type, TokenCredentials tokenCredentials) throws IOException, HttpException {
        LOGGER.info("Making REST call to {} with request: {} and token: {} and userKey: {}", tokenCredentials.getServerUrl(), requestString, token, tokenCredentials.getUserKey());
        String input = generateRequestBody(requestString, tokenCredentials.getUserKey(), type, token);

        HttpPost request = new HttpPost(tokenCredentials.getServerUrl());
        request.addHeader(HttpHeaders.CONTENT_TYPE, TranslationConstants.APPLICATION_JSON);
        request.setEntity(new StringEntity(input, StandardCharsets.UTF_8));

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            return httpClient.execute(request, response -> {
                int statusCode = response.getCode();
                if (statusCode == HttpStatus.SC_OK) {
                    return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                } else {
                    LOGGER.info("Request unsuccessful: {}", response.getReasonPhrase());
                    throw new HttpException("Response code from Whitesource not OK");
                }
            });
        }
    }
}
