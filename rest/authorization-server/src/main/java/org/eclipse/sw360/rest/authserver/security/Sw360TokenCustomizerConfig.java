/*
SPDX-FileCopyrightText: © 2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.authserver.security;

import org.eclipse.sw360.rest.authserver.client.service.Sw360OidcUserInfoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.Arrays;

/**
 * Customizes the JWT token and ID token
 *
 * @author smruti.sahoo@siemens.com
 */
@Configuration
public class Sw360TokenCustomizerConfig {

	public static final String USER_NAME = "user_name";
	public static final String CLIENT_ID = "client_id";
	public static final String AUD = "aud";
	public static final String SUB = "sub";
	public static final String SW360_REST_API = "sw360-REST-API";
	private final Sw360OidcUserInfoService sw360OidcUserInfoService;

	public Sw360TokenCustomizerConfig(Sw360OidcUserInfoService sw360OidcUserInfoService) {
		this.sw360OidcUserInfoService = sw360OidcUserInfoService;
	}

	@Bean
	public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {

		return (context) -> {
			//JWT token customizer
			if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
				context.getClaims().claims((claims) -> {
					claims.put(USER_NAME, claims.get(SUB));
					claims.remove(SUB);
					claims.put(CLIENT_ID, claims.get(AUD));
					claims.remove(AUD);
					claims.put(AUD, Arrays.asList(SW360_REST_API));
				});
			}
			//ID token customizer
			if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
				OidcUserInfo userInfo = sw360OidcUserInfoService.loadUser(context.getPrincipal().getName());
				context.getClaims().claims(claims ->
						claims.putAll(userInfo.getClaims()));
			}
		};
	}
}
