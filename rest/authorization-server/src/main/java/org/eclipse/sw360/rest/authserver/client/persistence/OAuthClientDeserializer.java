// Copyright (C) TOSHIBA CORPORATION, 2025. Part of the SW360 Frontend Project.
// Copyright (C) Toshiba Software Development (Vietnam) Co., Ltd., 2025. Part of the SW360 Frontend Project.

// This program and the accompanying materials are made
// available under the terms of the Eclipse Public License 2.0
// which is available at https://www.eclipse.org/legal/epl-2.0/

// SPDX-License-Identifier: EPL-2.0
// License-Filename: LICENSE
package org.eclipse.sw360.rest.authserver.client.persistence;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class OAuthClientDeserializer extends JsonDeserializer<OAuthClientEntity> {

    @Override
    public OAuthClientEntity deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        JsonNode node = p.getCodec().readTree(p);
        OAuthClientEntity client = new OAuthClientEntity();

        client.setId(node.get("_id").asText());
        client.setRev(node.get("_rev").asText());
        client.setClientId(node.get("client_id").asText());
        client.setClientSecret(node.get("client_secret").asText());
        client.setDescription(node.get("description").asText());

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

        return client;
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