/*
SPDX-FileCopyrightText: © 2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.resourceserver.security.jwt;

import jakarta.annotation.Nullable;
import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.security.TokenCapabilityAuthorities;
import org.eclipse.sw360.rest.resourceserver.security.basic.Sw360GrantedAuthoritiesCalculator;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Validate user and extracts the roles from the JWT token and convert into GrantedAuthority
 *
 * @author smruti.sahoo@siemens.com
 */
@Profile("!SECURITY_MOCK")
@Component
public class Sw360JWTAccessTokenConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	private static final Logger log = LogManager.getLogger(Sw360JWTAccessTokenConverter.class);
	public static final String USER_NAME = "user_name";
	public static final String USER_IS_DEACTIVATED_OR_NOT_AVAILABLE = "User is deactivated or not available.";
	public static final String SCOPE = "scope";
	private static final String CLIENT_ID = "client_id";

	private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

	private static final String JWT_EMAIL = "email";

	@Value("${jwt.auth.converter.principle-attribute:email}")
	private String principleAttribute;

	@Autowired
	private Sw360UserService userService;

	/**
	 * Converts the JWT token into an AbstractAuthenticationToken.
	 *
	 * @param jwt the JWT token
	 * @return an AbstractAuthenticationToken containing the JWT token and its authorities
	 */
	@Override
	public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
		User sw360User = loadSw360User(jwt);
		Collection<GrantedAuthority> tokenCapabilities = TokenCapabilityAuthorities.fromJwtScopeClaim(jwt.getClaim(SCOPE));
		Collection<GrantedAuthority> authorities = Stream
				.concat(jwtGrantedAuthoritiesConverter.convert(jwt).stream(), extractResourceRoles(sw360User, jwt, tokenCapabilities).stream())
				.collect(Collectors.toSet());
		JwtAuthenticationToken authenticationToken = new JwtAuthenticationToken(jwt, authorities, getPrincipleClaimName(jwt));
		authenticationToken.setDetails(sw360User);
		return authenticationToken;
	}

	/**
	 * Retrieves the principal claim name from the JWT token.
	 *
	 * @param jwt the JWT token
	 * @return the principal claim name
	 */
	private String getPrincipleClaimName(Jwt jwt) {
		String claimName = JwtClaimNames.SUB;
		if (principleAttribute != null) {
			claimName = principleAttribute;
		}
		return jwt.getClaim(claimName);
	}

	/**
	 * .Extracts the roles from the JWT token and converts them into GrantedAuthority.
	 *
	 * @param jwt the JWT token
	 * @return a collection of GrantedAuthority extracted from the JWT token
	 */
	private Collection<GrantedAuthority> extractResourceRoles(User sw360User, Jwt jwt,
			Collection<GrantedAuthority> tokenCapabilities) {
		String email = sw360User.getEmail();
		List<GrantedAuthority> grantedAuthorities = Sw360GrantedAuthoritiesCalculator.generateFromUser(sw360User);
		log.debug("User {} has group authorities {} and token capabilities {} for client {}", email,
				grantedAuthorities, tokenCapabilities, jwt.getClaim("client_id"));
		return TokenCapabilityAuthorities.mergeForTokenAuthentication(grantedAuthorities, tokenCapabilities);
	}

	private User loadSw360User(Jwt jwt) {
		String email = extractEmailFromJWT(jwt);
		String clientId = extractClientIdFromJWT(jwt);
		User sw360User = getUserFromService(email, clientId);
		validateUser(sw360User);
		return sw360User;
	}

	@Nullable
	private static String extractClientIdFromJWT(Jwt jwt) {
		Object clientIdClaim = jwt.getClaim(CLIENT_ID);
		if (clientIdClaim instanceof Collection<?> clientIdCollection) {
			for (Object candidate : clientIdCollection) {
				String value = Objects.toString(candidate, null);
				if (CommonUtils.isNotNullEmptyOrWhitespace(value)) {
					return value;
				}
			}
			return null;
		}
		String value = Objects.toString(clientIdClaim, null);
		return CommonUtils.isNotNullEmptyOrWhitespace(value) ? value : null;
	}

	/**
	 * First try to get User based on email from User Service. If not found, try
	 * with ClientID. If that fails as well, return {@code null}.
	 * @param email    Email to search user with.
	 * @param clientId Client ID to search user with.
	 * @return Return user if found from one of the parameter, null otherwise.
	 */
	@Nullable
	private User getUserFromService(@Nullable String email, @Nullable String clientId) {
		User sw360User = null;
		if (CommonUtils.isNotNullEmptyOrWhitespace(email)) {
			try {
				sw360User = userService.getUserByEmail(email);
			} catch (RuntimeException e) {
				log.debug("Could not resolve SW360 user by email claim {}", email, e);
			}
		}

		if (sw360User == null && CommonUtils.isNotNullEmptyOrWhitespace(clientId)) {
			try {
				sw360User = userService.getUserFromClientId(clientId);
			} catch (RuntimeException e) {
				log.debug("Could not resolve SW360 user by client_id claim {}", clientId, e);
			}
		}
		return sw360User;
	}

	/**
	 * Extracts the email from the JWT token.
	 *
	 * @param jwt the JWT token
	 * @return the email extracted from the JWT token
	 */
	private static String extractEmailFromJWT(Jwt jwt) {
		String email = jwt.getClaim(JWT_EMAIL);
		if(email == null) {
			email = jwt.getClaim(USER_NAME);
		}
		return email;
	}

	/**
	 * Validates the user based on its status.
	 *
	 * @param sw360User the user object fetched from the user service
	 * @throws BadCredentialsException if the user is deactivated or not available
	 */
	private static void validateUser(@Nullable User sw360User) {
		if (sw360User == null || sw360User.isDeactivated()) {
			throw new BadCredentialsException(USER_IS_DEACTIVATED_OR_NOT_AVAILABLE);
		}
	}
}
