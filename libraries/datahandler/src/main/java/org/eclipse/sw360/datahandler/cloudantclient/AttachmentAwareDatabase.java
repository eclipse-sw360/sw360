/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.cloudantclient;

import com.google.gson.reflect.TypeToken;
import com.ibm.cloud.cloudant.common.SdkCommon;
import com.ibm.cloud.cloudant.v1.Cloudant;
import com.ibm.cloud.cloudant.v1.model.DocumentResult;
import com.ibm.cloud.cloudant.v1.model.GetAttachmentOptions;
import com.ibm.cloud.cloudant.v1.model.PutAttachmentOptions;
import com.ibm.cloud.sdk.core.http.RequestBuilder;
import com.ibm.cloud.sdk.core.http.ResponseConverter;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.cloud.sdk.core.util.ResponseConverterUtils;
import com.ibm.cloud.sdk.core.util.Validator;
import okhttp3.HttpUrl;
import org.jspecify.annotations.NonNull;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * A Cloudant client that is aware of attachments with special characters in
 * their names. This class provides a custom implementation of `getAttachment`
 * and `putAttachment` methods to handle URL encoding of attachment names
 * containing '+' or similar characters.<br />
 * https://github.com/IBM/cloudant-java-sdk/blob/51b7da64dea925dc1dd0b2a980dba93e0c899297/KNOWN_ISSUES.md#path-elements-containing-the--character
 */
public class AttachmentAwareDatabase extends Cloudant {

    public AttachmentAwareDatabase(@NonNull Cloudant client) {
        super(client.getName(), client.getAuthenticator());
        this.setServiceUrl(client.getServiceUrl());
    }

    private String urlEncodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @Override
    public ServiceCall<InputStream> getAttachment(GetAttachmentOptions getAttachmentOptions) {
        Validator.notNull(getAttachmentOptions, "getAttachmentOptions cannot be null");

        HttpUrl baseUrl = HttpUrl.parse(this.getServiceUrl());
        if (baseUrl == null) {
            throw new RuntimeException("Unable to get Base Service URL");
        }

        HttpUrl.Builder urlBuilder = baseUrl.newBuilder()
                .addPathSegment(getAttachmentOptions.db())
                .addPathSegment(getAttachmentOptions.docId())
                .addEncodedPathSegment(urlEncodePathSegment(getAttachmentOptions.attachmentName()));

        if (getAttachmentOptions.rev() != null) {
            urlBuilder.addQueryParameter("rev", getAttachmentOptions.rev());
        }

        RequestBuilder builder = RequestBuilder.get(urlBuilder.build());

        Map<String, String> sdkHeaders = SdkCommon.getSdkHeaders("cloudant", "v1", "getAttachment");
        for (Map.Entry<String, String> header : sdkHeaders.entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }

        if (getAttachmentOptions.accept() != null) {
            builder.header("Accept", getAttachmentOptions.accept());
        }
        if (getAttachmentOptions.ifMatch() != null) {
            builder.header("If-Match", getAttachmentOptions.ifMatch());
        }
        if (getAttachmentOptions.ifNoneMatch() != null) {
            builder.header("If-None-Match", getAttachmentOptions.ifNoneMatch());
        }
        if (getAttachmentOptions.range() != null) {
            builder.header("Range", getAttachmentOptions.range());
        }

        ResponseConverter<InputStream> responseConverter = ResponseConverterUtils.getInputStream();
        return this.createServiceCall(builder.build(), responseConverter);
    }

    @Override
    public ServiceCall<DocumentResult> putAttachment(PutAttachmentOptions putAttachmentOptions) {
        Validator.notNull(putAttachmentOptions, "putAttachmentOptions cannot be null");

        HttpUrl baseUrl = HttpUrl.parse(this.getServiceUrl());
        if (baseUrl == null) {
            throw new RuntimeException("Unable to get Base Service URL");
        }

        HttpUrl.Builder urlBuilder = baseUrl.newBuilder()
                .addPathSegment(putAttachmentOptions.db())
                .addPathSegment(putAttachmentOptions.docId())
                .addEncodedPathSegment(urlEncodePathSegment(putAttachmentOptions.attachmentName()));

        if (putAttachmentOptions.rev() != null) {
            urlBuilder.addQueryParameter("rev", putAttachmentOptions.rev());
        }

        RequestBuilder builder = RequestBuilder.put(urlBuilder.build());

        Map<String, String> sdkHeaders = SdkCommon.getSdkHeaders("cloudant", "v1", "putAttachment");
        for (Map.Entry<String, String> header : sdkHeaders.entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }

        builder.header("Accept", "application/json");
        builder.header("Content-Type", putAttachmentOptions.contentType());

        if (putAttachmentOptions.ifMatch() != null) {
            builder.header("If-Match", putAttachmentOptions.ifMatch());
        }

        builder.bodyContent(putAttachmentOptions.contentType(), null,
                null, putAttachmentOptions.attachment());
        ResponseConverter<DocumentResult> responseConverter = ResponseConverterUtils.getValue((new TypeToken<DocumentResult>() {
        }).getType());
        return this.createServiceCall(builder.build(), responseConverter);
    }
}
