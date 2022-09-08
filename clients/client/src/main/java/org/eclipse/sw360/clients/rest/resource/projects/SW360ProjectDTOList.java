/*
 * Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2022. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.clients.rest.resource.projects;

import org.eclipse.sw360.clients.rest.resource.LinkObjects;
import org.eclipse.sw360.clients.rest.resource.SW360HalResource;


public class SW360ProjectDTOList extends SW360HalResource<LinkObjects, SW360ProjectDTOListEmbedded> {

    @Override
    public LinkObjects createEmptyLinks() {
        return new LinkObjects();
    }

    @Override
    public SW360ProjectDTOListEmbedded createEmptyEmbedded() {
        return new SW360ProjectDTOListEmbedded();
    }
}

