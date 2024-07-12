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

public class JWTValidator {
    JwtConsumer jwtConsumer;

    /**
     * Creates a validator for JWT access tokens issued by the given PF instance.
     * 
     * @param pfBaseUrl the base URL of the PF instance including the trailing
     *                  slash.
     */
    public JWTValidator(String issuerUrl, String jwksurl) {
        HttpsJwks httpsJkws = new HttpsJwks(jwksurl);
        HttpsJwksVerificationKeyResolver httpsJwksKeyResolver = new HttpsJwksVerificationKeyResolver(httpsJkws);
        jwtConsumer = new JwtConsumerBuilder()
                //TODO:Recheck
//                .setRequireExpirationTime()
//                .setAllowedClockSkewInSeconds(30)
                .setExpectedIssuer(issuerUrl)
                .setVerificationKeyResolver(httpsJwksKeyResolver).build();
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