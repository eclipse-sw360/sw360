/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.common.security.jwt;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class Sw360JwtIssuerPropertiesTest {

    @Test
    void shouldReturnEmptyMap_whenNoIssuersConfigured() {
        Sw360JwtIssuerProperties props = new Sw360JwtIssuerProperties();

        Map<String, JwtIssuer> result = props.getEffectiveIssuers();

        assertThat(result).isEmpty();
    }

    @Test
    void shouldMapStructuredIssuers_andNormalizeJwkSetUri() {
        Sw360JwtIssuerProperties props = new Sw360JwtIssuerProperties();
        JwtIssuer first = new JwtIssuer();
        first.setIssuerUri("https://sw360.example.com/kc/realms/sw360");
        first.setJwkSetUri("  http://localhost:8083/kc/realms/sw360/protocol/openid-connect/certs  ");
        JwtIssuer second = new JwtIssuer();
        second.setIssuerUri("http://localhost:8090/authorization");
        second.setJwkSetUri("   ");
        props.setIssuers(Arrays.asList(first, second));

        Map<String, JwtIssuer> result = props.getEffectiveIssuers();

        assertThat(result).hasSize(2);
        assertThat(result.get("https://sw360.example.com/kc/realms/sw360").getJwkSetUri())
                .isEqualTo("http://localhost:8083/kc/realms/sw360/protocol/openid-connect/certs");
        assertThat(result.get("http://localhost:8090/authorization").getJwkSetUri()).isNull();
    }

    @Test
    void shouldSkipStructuredEntries_withBlankOrNullIssuerUri() {
        Sw360JwtIssuerProperties props = new Sw360JwtIssuerProperties();
        JwtIssuer valid = new JwtIssuer();
        valid.setIssuerUri("https://sw360.example.com/kc/realms/sw360");
        JwtIssuer blank = new JwtIssuer();
        blank.setIssuerUri("   ");
        JwtIssuer nullIssuer = new JwtIssuer();
        props.setIssuers(Arrays.asList(valid, blank, nullIssuer, null));

        Map<String, JwtIssuer> result = props.getEffectiveIssuers();

        assertThat(result).hasSize(1).containsKey("https://sw360.example.com/kc/realms/sw360");
    }

    @Test
    void shouldReturnEmpty_whenLegacyAuthserverNotConfigured() {
        Sw360JwtIssuerProperties props = new Sw360JwtIssuerProperties();

        assertThat(props.getEffectiveLegacyAuthserver()).isEmpty();
    }

    @Test
    void shouldReturnEmpty_whenLegacyAuthserverJwkSetUriIsBlank() {
        Sw360JwtIssuerProperties props = new Sw360JwtIssuerProperties();
        Sw360JwtIssuerProperties.LegacyAuthserver legacy = new Sw360JwtIssuerProperties.LegacyAuthserver();
        legacy.setJwkSetUri("   ");
        legacy.setExpectedAudience("sw360-REST-API");
        props.setLegacyAuthserver(legacy);

        assertThat(props.getEffectiveLegacyAuthserver()).isEmpty();
    }

    @Test
    void shouldNormalizeLegacyAuthserverValues() {
        Sw360JwtIssuerProperties props = new Sw360JwtIssuerProperties();
        Sw360JwtIssuerProperties.LegacyAuthserver legacy = new Sw360JwtIssuerProperties.LegacyAuthserver();
        legacy.setJwkSetUri("  http://localhost:8080/authorization/oauth2/jwks  ");
        legacy.setExpectedAudience("  sw360-REST-API  ");
        props.setLegacyAuthserver(legacy);

        Sw360JwtIssuerProperties.LegacyAuthserver effective =
                props.getEffectiveLegacyAuthserver().orElseThrow();

        assertThat(effective.getJwkSetUri()).isEqualTo("http://localhost:8080/authorization/oauth2/jwks");
        assertThat(effective.getExpectedAudience()).isEqualTo("sw360-REST-API");
    }

    @Test
    void shouldTreatBlankExpectedAudienceAsUnset() {
        Sw360JwtIssuerProperties props = new Sw360JwtIssuerProperties();
        Sw360JwtIssuerProperties.LegacyAuthserver legacy = new Sw360JwtIssuerProperties.LegacyAuthserver();
        legacy.setJwkSetUri("http://localhost:8080/authorization/oauth2/jwks");
        legacy.setExpectedAudience("   ");
        props.setLegacyAuthserver(legacy);

        Sw360JwtIssuerProperties.LegacyAuthserver effective =
                props.getEffectiveLegacyAuthserver().orElseThrow();

        assertThat(effective.getExpectedAudience()).isNull();
    }
}
