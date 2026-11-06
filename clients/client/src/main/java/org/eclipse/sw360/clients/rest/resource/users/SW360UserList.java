/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.clients.rest.resource.users;

import org.eclipse.sw360.clients.rest.resource.LinkObjects;
import org.eclipse.sw360.clients.rest.resource.SW360HalResource;

public class SW360UserList extends SW360HalResource<LinkObjects, SW360UserListEmbedded> {

    @Override
    public LinkObjects createEmptyLinks() {
        return new LinkObjects();
    }

    @Override
    public SW360UserListEmbedded createEmptyEmbedded() {
        return new SW360UserListEmbedded();
    }
}
