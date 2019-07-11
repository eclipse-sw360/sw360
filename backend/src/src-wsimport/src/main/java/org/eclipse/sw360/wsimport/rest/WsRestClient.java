/*
 * Copyright (c) Verifa Oy, 2018-2019. Part of the SW360 Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.wsimport.rest;


import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.eclipse.sw360.datahandler.thrift.projectimport.TokenCredentials;
import org.eclipse.sw360.wsimport.utility.TranslationConstants;
import org.eclipse.sw360.wsimport.utility.WsTokenType;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.IOException;

/**
 * @author: ksoranko@verifa.io
 */
public class WsRestClient {

    private static final Logger LOGGER = Logger.getLogger(WsRestClient.class);

    WsRestClient() {
    }

    private String generateRequestBody(String requestType, String userKey, WsTokenType tokenType, String token) {
        JSONObject json = new JSONObject();
        json.put("requestType", requestType);
        json.put("userKey", userKey);
        json.put(tokenType, token);
        return json.toString();
    }

    private HttpClient getConfiguredHttpClient() {
        return HttpClientBuilder
                .create()
                .build();
    }

    private HttpResponse getWsConnection(String input, HttpClient client, String serverUrl) throws IOException{
        HttpPost request = new HttpPost(serverUrl);
        request.addHeader(HttpHeaders.CONTENT_TYPE, TranslationConstants.APPLICATION_JSON);
        StringEntity stringEntity = new StringEntity(input, ContentType.create(TranslationConstants.APPLICATION_JSON));
        request.setEntity(stringEntity);
        return client.execute(request);
    }

    String getData(String requestString, String token, WsTokenType type, TokenCredentials tokenCredentials) throws IOException, HttpException {
        LOGGER.info("Making REST call to " + tokenCredentials.getServerUrl() + " with request: " + requestString + " and token: " + token + " and userKey: " + tokenCredentials.getUserKey());
        String input = generateRequestBody(requestString, tokenCredentials.getUserKey(), type, token);
        HttpClient httpClient = getConfiguredHttpClient();
        HttpResponse response = getWsConnection(input, httpClient, tokenCredentials.getServerUrl());
        int statusCode = response.getStatusLine().getStatusCode();
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            return IOUtils.toString(response.getEntity().getContent(), "UTF-8");
        } else {
            LOGGER.info("Request unsuccessful: " + response.getStatusLine().getReasonPhrase());
            throw new HttpException("Response code from Whitesource not OK");
        }
    }
}
