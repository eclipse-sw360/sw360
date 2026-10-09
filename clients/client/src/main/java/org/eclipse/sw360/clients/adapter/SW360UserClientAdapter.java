/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.clients.adapter;

import org.eclipse.sw360.clients.rest.SW360UserClient;
import org.eclipse.sw360.clients.rest.resource.users.SW360User;
import org.eclipse.sw360.clients.utils.SW360ClientException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * Service interface for an adapter supporting operations on SW360 user
 * entities.
 * </p>
 */
public interface SW360UserClientAdapter {
    SW360UserClient getUserClient();

    /**
     * Returns a list with all users known to the system.
     *
     * @return a list with all the users known
     * @throws SW360ClientException if an error occurs
     */
    List<SW360User> getUsers();

    /**
     * Returns the profile of the currently authenticated user.
     *
     * @return the profile of the current user
     * @throws SW360ClientException if an error occurs
     */
    SW360User getUserProfile();

    /**
     * Queries a user from SW360 by email. If the server responds with a 404
     * status, result is an empty {@code Optional}.
     *
     * @param email the email of the desired user
     * @return an {@code Optional} with the user fetched from the server
     * @throws SW360ClientException if an error occurs
     */
    Optional<SW360User> getUserByEmail(String email);

    /**
     * Creates a REST API token for the authenticated user and returns the
     * generated token value.
     *
     * @param name           the name of the token
     * @param authorities    the authorities granted to the token
     * @param expirationDate the expiration date of the token (yyyy-MM-dd)
     * @return the generated token value
     * @throws SW360ClientException if an error occurs
     */
    String createToken(String name, Set<String> authorities, String expirationDate);

    /**
     * Revokes the REST API token with the given name for the authenticated
     * user.
     *
     * @param name the name of the token to revoke
     * @return the status code returned by the server
     * @throws SW360ClientException if an error occurs
     */
    Integer revokeToken(String name);
}
