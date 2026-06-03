/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientEntity;
import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Component;

/**
 * Synchronously upgrades a legacy raw-UUID client secret to its BCrypt
 * representation immediately after the OAuth2 client authentication
 * succeeds. The raw secret is captured by {@link Sw360ClientSecretEncoder}
 * via {@link Sw360ClientSecretEncoder#UPGRADE_CTX} during {@code matches},
 * and consumed here.
 *
 * <p>Spring publishes {@link AuthenticationSuccessEvent} synchronously by
 * default, so the upgrade completes inside the same request thread before
 * the token response is returned to the client. The entity is re-fetched
 * from CouchDB right before {@code update} to avoid 409 {@code _rev}
 * conflicts; on any persistence failure we log a warning and let the next
 * login retry the upgrade - the token response itself is unaffected.</p>
 */
@Component
public class LegacyClientSecretUpgrader implements ApplicationListener<AuthenticationSuccessEvent> {

    private static final Logger log = LogManager.getLogger(LegacyClientSecretUpgrader.class);

    private final OAuthClientRepository repo;
    private final PasswordEncoder encoder;

    public LegacyClientSecretUpgrader(OAuthClientRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Override
    public void onApplicationEvent(@NonNull AuthenticationSuccessEvent event) {
        String rawSecret = Sw360ClientSecretEncoder.UPGRADE_CTX.get();
        if (rawSecret == null) {
            return;
        }
        try {
            Authentication authentication = event.getAuthentication();
            if (!(authentication instanceof OAuth2ClientAuthenticationToken clientAuth)) {
                return;
            }
            RegisteredClient registeredClient = clientAuth.getRegisteredClient();
            if (registeredClient == null) {
                return;
            }
            String clientId = registeredClient.getClientId();
            if (CommonUtils.isNullEmptyOrWhitespace(clientId)) {
                return;
            }

            // Re-fetch to get a fresh _rev: another node / a parallel admin
            // call could have updated this doc between login start and now,
            // and a stale _rev would fail with 409 Conflict in CouchDB.
            OAuthClientEntity fresh = repo.getByClientId(clientId);
            if (fresh == null) {
                return;
            }
            if (Sw360ClientSecretEncoder.looksLikeBcrypt(fresh.getClientSecret())) {
                // A concurrent upgrade already happened - nothing to do.
                return;
            }

            fresh.setClientSecret(encoder.encode(rawSecret));
            repo.update(fresh);
            log.info("Upgraded legacy plaintext client_secret to BCrypt for client_id={}", clientId);
        } catch (RuntimeException e) {
            log.warn("Failed to upgrade legacy client_secret for current authentication; "
                    + "will retry on next successful login.", e);
        } finally {
            Sw360ClientSecretEncoder.UPGRADE_CTX.remove();
        }
    }
}
