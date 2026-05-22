// Copyright (C) TOSHIBA CORPORATION, 2025. Part of the SW360 Frontend Project.
// Copyright (C) Toshiba Software Development (Vietnam) Co., Ltd., 2025. Part of the SW360 Frontend Project.

// This program and the accompanying materials are made
// available under the terms of the Eclipse Public License 2.0
// which is available at https://www.eclipse.org/legal/epl-2.0/

// SPDX-License-Identifier: EPL-2.0
// License-Filename: LICENSE
package org.eclipse.sw360.rest.authserver.client.persistence;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class OAuthClientDeserializer extends JsonDeserializer<OAuthClientEntity> {

    @Override
    public OAuthClientEntity deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = p.getCodec().readTree(p);
        OAuthClientEntity client = new OAuthClientEntity();

        client.setId(asText(node, "_id"));
        client.setRev(asText(node, "_rev"));
        client.setClientId(asText(node, "client_id"));
        client.setClientSecret(asText(node, "client_secret"));
        client.setDescription(asText(node, "description"));

        client.setSecretRequired(asBoolean(node, "secretRequired"));
        client.setScoped(asBoolean(node, "scoped"));

        client.setResourceIds(jsonNodeToSet(node.get("resource_ids")));
        client.setAuthorizedGrantTypes(jsonNodeToSet(node.get("authorized_grant_types")));
        client.setScope(jsonNodeToSet(node.get("scope")));
        client.setRegisteredRedirectUri(jsonNodeToSet(node.get("redirect_uri")));
        client.setAutoApproveScopes(jsonNodeToSet(node.get("autoapprove")));

        JsonNode accessTokenValidity = node.get("access_token_validity");
        if (accessTokenValidity != null && !accessTokenValidity.isNull()) {
            client.setAccessTokenValiditySeconds(accessTokenValidity.asInt());
        }
        JsonNode refreshTokenValidity = node.get("refresh_token_validity");
        if (refreshTokenValidity != null && !refreshTokenValidity.isNull()) {
            client.setRefreshTokenValiditySeconds(refreshTokenValidity.asInt());
        }

        client.setAuthorities(jsonNodeToSet(node.get("authorities")));

        JsonNode ownerEmail = node.get("owner_email");
        if (ownerEmail != null && !ownerEmail.isNull()) {
            client.setOwnerEmail(ownerEmail.asText());
        }

        return client;
    }

    /**
     * Return the textual value of {@code field} on {@code node}, or {@code null}
     * when the field is missing or explicitly JSON null. Guards against malformed
     * documents in the {@code sw360oauthclients} database (e.g. migration
     * artifacts) that would otherwise NPE during a bulk
     * {@link OAuthClientRepository#getAll()}.
     */
    private static String asText(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return (child == null || child.isNull()) ? null : child.asText();
    }

    private static boolean asBoolean(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return child != null && !child.isNull() && child.asBoolean();
    }

    private Set<String> jsonNodeToSet(JsonNode node) {
        if (node == null || !node.isArray()) {
            return new HashSet<>();
        }
        Set<String> result = new HashSet<>();
        for (JsonNode element : node) {
            if (element.isTextual()) {
                result.add(element.asText());
            } else if (element.isObject() && element.has("role")) {
                result.add(element.get("role").asText());
            }
        }
        return result;
    }
}
