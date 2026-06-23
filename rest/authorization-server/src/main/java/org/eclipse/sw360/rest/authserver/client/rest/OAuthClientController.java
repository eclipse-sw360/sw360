/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.client.rest;

import com.google.common.collect.Sets;
import jakarta.annotation.Nonnull;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientEntity;
import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientRepository;
import org.eclipse.sw360.rest.authserver.client.service.Sw360UserMirrorService;
import org.eclipse.sw360.rest.authserver.security.Sw360GrantedAuthority;
import org.eclipse.sw360.rest.authserver.security.key.KeyManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This REST controller can be accessed by users having the authority
 * {@link Sw360GrantedAuthority#ADMIN}. Such users can perform CRUD operations
 * on the configured {@link OAuthClientResource}s via these REST endpoints.
 */
@RestController
@RequestMapping(path = "/" + OAuthClientController.ENDPOINT_URL)
@PreAuthorize("hasAuthority('ADMIN')")
public class OAuthClientController {

    public static final String ENDPOINT_URL = "client-management";

    /**
     * Allowed scope values (case-insensitive); anything else is rejected.
     */
    private static final Set<String> ALLOWED_SCOPES = Set.of("READ", "WRITE");

    /**
     * Spring Authorization Server requires every {@code RegisteredClient} to
     * declare at least one redirect URI, even for {@code client_credentials}-only
     * registrations that will never invoke a redirect endpoint.
     */
    public static final String UNUSED_REDIRECT_URI = "https://localhost/unused-redirect";

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${security.oauth2.resource.id}")
    private String resourceId;

    @Autowired
    private KeyManager keyManager;

    @Autowired
    private OAuthClientRepository repo;

    @Autowired
    private Sw360UserMirrorService userMirrorService;

    /**
     * Normalize a caller-supplied {@code scope} set to the canonical
     * {@code READ} / {@code WRITE} values understood by the resource server,
     * rejecting anything outside that set.
     *
     * @throws IllegalArgumentException if any submitted value is unsupported.
     */
    @Nonnull
    private static Set<String> normalizeScope(@Nonnull Set<String> submitted) {
        if (CommonUtils.isNullOrEmptyCollection(submitted)) {
            return Collections.singleton("READ");
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : submitted) {
            if (CommonUtils.isNullEmptyOrWhitespace(value)) {
                continue;
            }
            String upper = value.trim().toUpperCase(Locale.ROOT);
            if (upper.startsWith("SCOPE_")) {
                upper = upper.substring("SCOPE_".length());
            }
            if (!ALLOWED_SCOPES.contains(upper)) {
                throw new IllegalArgumentException(
                        "Invalid scope '%s'. Allowed values: {%s}.".formatted(
                                upper, ALLOWED_SCOPES.toString()
                        )
                );
            }
            normalized.add(upper);
        }
        if (normalized.isEmpty()) {
            return Collections.singleton("READ");
        }
        return normalized;
    }

    /**
     * List all registered OAuth clientsReturns every client registration. The
     * `client_secret` field is replaced by `<hidden>` for every entry: the raw
     * secret is only ever disclosed once, in the response body of the creation
     * call.
     */
    @GetMapping(path = "")
    public ResponseEntity<List<OAuthClientResource>> getAllClients() {
        List<OAuthClientEntity> clients = repo.getAll();
        if (clients == null) {
            clients = new ArrayList<>();
        }
        List<OAuthClientResource> clientResources = clients.stream()
                .map(OAuthClientResource::new)
                .peek(r -> r.setClientSecret(OAuthClientResource.HIDDEN_SECRET))
                .collect(Collectors.toList());
        return new ResponseEntity<>(clientResources, HttpStatus.OK);
    }

    /**
     * Create or update an OAuth clientOn creation, the response body contains
     * the freshly generated `client_secret` in plaintext. **This is the only
     * opportunity to retrieve it** - subsequent calls will return `<hidden>`
     * for that field. The persisted secret is stored as a BCrypt hash. Updates
     * do not rotate the secret. The `scope` field is restricted to any subset
     * of `{READ, WRITE}` (case-insensitive; `SCOPE_READ` / `SCOPE_WRITE`
     * accepted); other values are rejected with HTTP 400. The `authorities`
     * field is accepted as-is.
     */
    @PostMapping(path = "", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> createOrUpdateClient(
            @RequestBody OAuthClientResource clientResource
    ) {
        Set<String> normalizedScope;
        try {
            normalizedScope = normalizeScope(clientResource.getScope());
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        OAuthClientEntity clientEntity;
        String rawSecret = null;
        if (CommonUtils.isNotNullEmptyOrWhitespace(clientResource.getClientId())) {
            clientEntity = repo.getByClientId(clientResource.getClientId());
            if (clientEntity == null) {
                return new ResponseEntity<>(
                        "No client found for given clientId " + clientResource.getClientId(),
                        HttpStatus.BAD_REQUEST);
            }
        } else {
            // On create, the owner must exist as a SW360 user. Default to the
            // calling admin when not supplied; reject early if no such user.
            String ownerEmail = CommonUtils.isNotNullEmptyOrWhitespace(clientResource.getOwnerEmail())
                    ? clientResource.getOwnerEmail().trim()
                    : currentAdminEmail();
            if (CommonUtils.isNullEmptyOrWhitespace(ownerEmail)) {
                return new ResponseEntity<>(
                        "owner_email is required and could not be derived from the caller.",
                        HttpStatus.BAD_REQUEST);
            }
            User owner = userMirrorService.getByEmail(ownerEmail);
            if (owner == null) {
                return new ResponseEntity<>(
                        "owner_email <" + ownerEmail + "> does not match any SW360 user.",
                        HttpStatus.BAD_REQUEST);
            }
            clientEntity = new OAuthClientEntity();
            clientEntity.setOwnerEmail(owner.getEmail());

            // store entity to get a new id
            try {
                repo.add(clientEntity);
            } catch (SW360Exception e) {
                return new ResponseEntity<>(
                        "Unable to add client " + clientResource.getClientId(),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }

            clientEntity.setClientId(clientEntity.getId());
            // Generate the raw secret first so we can return it to the caller exactly
            // once. The persisted value is BCrypt-hashed for storage at rest.
            rawSecret = UUID.randomUUID().toString();
            clientEntity.setClientSecret(passwordEncoder.encode(rawSecret));
        }
        updateClientEntityFromResource(clientEntity, clientResource, normalizedScope);
        repo.update(clientEntity);

        // Mirror the client_id -> ClientMetadata entry into the owner's
        // oidcClientInfos so the resource server can resolve the SW360 user
        // from the client_id JWT claim. We do this on both create and update
        // so name/access stay in sync with the latest description/scope.
        if (CommonUtils.isNotNullEmptyOrWhitespace(clientEntity.getOwnerEmail())) {
            User owner = userMirrorService.getByEmail(clientEntity.getOwnerEmail());
            if (owner != null) {
                userMirrorService.mirrorClient(owner, clientEntity.getClientId(),
                        clientEntity.getDescription(),
                        Sw360UserMirrorService.accessFromScope(normalizedScope));
            }
        }

        OAuthClientResource responseResource = new OAuthClientResource(
                repo.getByClientId(clientEntity.getClientId()));
        if (rawSecret != null) {
            // One-time disclosure of the plaintext secret.
            responseResource.setClientSecret(rawSecret);
        } else {
            // Update path: never re-disclose the stored hash.
            responseResource.setClientSecret(OAuthClientResource.HIDDEN_SECRET);
        }
        return new ResponseEntity<>(responseResource, HttpStatus.OK);
    }

    @DeleteMapping(path = "/{clientId}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> deleteClient(
            @PathVariable("clientId") String clientId
    ) {
        OAuthClientEntity clientEntity = null;

        if (CommonUtils.isNotNullEmptyOrWhitespace(clientId)) {
            clientEntity = repo.getByClientId(clientId);
            if (clientEntity == null) {
                return new ResponseEntity<>(
                        "No client found for given clientId " + clientId,
                        HttpStatus.NOT_FOUND);
            }

            // Unmirror from the owner user's oidcClientInfos before deleting
            // the client doc itself. Best-effort: log-only on failure so a
            // stale user mirror never blocks deletion.
            if (CommonUtils.isNotNullEmptyOrWhitespace(clientEntity.getOwnerEmail())) {
                User owner = userMirrorService.getByEmail(clientEntity.getOwnerEmail());
                userMirrorService.unmirrorClient(owner, clientEntity.getClientId());
            }

            repo.remove(clientEntity);
        } else {
            return new ResponseEntity<>(
                    "clientId must be provided in path",
                    HttpStatus.BAD_REQUEST
            );
        }
        OAuthClientResource responseResource = new OAuthClientResource(clientEntity);
        responseResource.setClientSecret(OAuthClientResource.HIDDEN_SECRET);
        return new ResponseEntity<>(responseResource, HttpStatus.OK);
    }

    /**
     * Returns the email of the currently authenticated admin (the
     * {@code /client-management} request is gated by HTTP Basic with
     * {@code hasAuthority('ADMIN')}; the principal name is the SW360 user
     * email per {@link org.eclipse.sw360.rest.authserver.client.service.Sw360UserDetailsService}).
     */
    private static String currentAdminEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    private void updateClientEntityFromResource(
            @Nonnull OAuthClientEntity clientEntity,
            @Nonnull OAuthClientResource clientResource,
            Set<String> normalizedScope
    ) {
        // updateable properties (clientId and clientSecret cannot be changed)
        clientEntity.setDescription(clientResource.getDescription());
        clientEntity.setAuthorities(clientResource.getAuthorities());
        clientEntity.setScope(normalizedScope);
        clientEntity.setAccessTokenValiditySeconds(clientResource.getAccessTokenValidity());
        clientEntity.setRefreshTokenValiditySeconds(clientResource.getRefreshTokenValidity());

        // static properties
        clientEntity.setAuthorizedGrantTypes(
                Stream.of("client_credentials", "password", "refresh_token").collect(Collectors.toSet()));
        clientEntity.setAutoApproveScopes(Collections.singleton("true"));
        clientEntity.setResourceIds(Sets.newHashSet(resourceId));
        clientEntity.setRegisteredRedirectUri(Collections.singleton(UNUSED_REDIRECT_URI));
    }
}
