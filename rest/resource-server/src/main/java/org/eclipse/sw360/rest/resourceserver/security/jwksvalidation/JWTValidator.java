/*
 * Copyright Siemens AG, 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.security.jwksvalidation;

import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer;

@Profile("!SECURITY_MOCK")
@Component
public class JWTValidator {
    private final JwtConsumer jwtConsumer;

    /**
     * Creates a validator for JWT access tokens issued by the configured JWKS endpoint.
     * This constructor is automatically called by Spring when JWKS validation is enabled.
     * The validator is created as a singleton bean for optimal performance.
     */
    @Autowired
    public JWTValidator() {
        if (!Sw360ResourceServer.IS_JWKS_VALIDATION_ENABLED) {
            this.jwtConsumer = null;
            return;
        }
        
        String issuerUrl = Sw360ResourceServer.JWKS_ISSUER_URL;
        String jwksUrl = Sw360ResourceServer.JWKS_ENDPOINT_URL;
        String aud = Sw360ResourceServer.JWT_CLAIM_AUD;
        
        HttpsJwks httpsJkws = new HttpsJwks(jwksUrl);
        HttpsJwksVerificationKeyResolver httpsJwksKeyResolver = new HttpsJwksVerificationKeyResolver(httpsJkws);
        JwtConsumerBuilder jwtConsumerBuilder = new JwtConsumerBuilder()
                .setRequireExpirationTime() // Enforce expiration time (exp claim)
                .setRequireIssuedAt() // Enforce issued at time (iat claim)
                .setAllowedClockSkewInSeconds(30) // Allow 30 seconds clock skew for time-based claims
                .setExpectedIssuer(issuerUrl)
                .setVerificationKeyResolver(httpsJwksKeyResolver);
        
        if (aud == null || aud.isEmpty()) {
            jwtConsumerBuilder.setSkipDefaultAudienceValidation();
        } else {
            jwtConsumerBuilder.setExpectedAudience(aud);
        }
        
        this.jwtConsumer = jwtConsumerBuilder.build();
    }

    /**
     * Validates the given JWT access token.
     *
     * @param jwt the JWT access token.
     * @return the claims associated with the given token.
     * @throws InvalidJwtException if the JWT could not be validated.
     */
    public JwtClaims validateJWT(String jwt) throws InvalidJwtException {
        return jwtConsumer.processToClaims(jwt);
    }
}
