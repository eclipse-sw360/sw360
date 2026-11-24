// Copyright (C) TOSHIBA CORPORATION, 2025. Part of the SW360 Frontend Project.
// Copyright (C) Toshiba Software Development (Vietnam) Co., Ltd., 2025. Part of the SW360 Frontend Project.

// This program and the accompanying materials are made
// available under the terms of the Eclipse Public License 2.0
// which is available at https://www.eclipse.org/legal/epl-2.0/

// SPDX-License-Identifier: EPL-2.0
// License-Filename: LICENSE
package org.eclipse.sw360.rest.authserver.client.persistence;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.Set;

public class OAuthClientDeserializer extends ValueDeserializer<OAuthClientEntity> {

    @Override
    public OAuthClientEntity deserialize(JsonParser p, DeserializationContext ctxt) {

        JsonNode node = ctxt.readTree(p);
        OAuthClientEntity client = new OAuthClientEntity();

        client.setId(asLegacyText(node.get("_id")));
        client.setRev(asLegacyText(node.get("_rev")));
        client.setClientId(asLegacyText(node.get("client_id")));
        client.setClientSecret(asLegacyText(node.get("client_secret")));
        client.setDescription(asLegacyText(node.get("description")));

        client.setSecretRequired(node.get("secretRequired").asBoolean());
        client.setScoped(node.get("scoped").asBoolean());

        client.setResourceIds(jsonNodeToSet(node.get("resource_ids")));
        client.setAuthorizedGrantTypes(jsonNodeToSet(node.get("authorized_grant_types")));
        client.setScope(jsonNodeToSet(node.get("scope")));
        client.setRegisteredRedirectUri(jsonNodeToSet(node.get("redirect_uri")));
        client.setAutoApproveScopes(jsonNodeToSet(node.get("autoapprove")));

        if (node.has("access_token_validity")) {
            client.setAccessTokenValiditySeconds(node.get("access_token_validity").asInt());
        }
        if (node.has("refresh_token_validity")) {
            client.setRefreshTokenValiditySeconds(node.get("refresh_token_validity").asInt());
        }

        Set<String> authorities = jsonNodeToSet(node.get("authorities"));
        client.setAuthorities(authorities);

        if (node.has("owner_email") && !node.get("owner_email").isNull()) {
            client.setOwnerEmail(asLegacyText(node.get("owner_email")));
        }

        return client;
    }

    private Set<String> jsonNodeToSet(JsonNode node) {
        if (node == null || !node.isArray()) {
            return new HashSet<>();
        }
        Set<String> result = new HashSet<>();
        for (JsonNode element : node) {
            if (element.isTextual()) {
                result.add(asLegacyText(element));
            } else if (element.isObject() && element.has("role")) {
                result.add(asLegacyText(element.get("role")));
            }
        }
        return result;
    }

    private static String asLegacyText(JsonNode node) {
        return node.isNull() ? "null" : node.asText();
    }
}
