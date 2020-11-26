/*
 * Copyright (c) Bosch Software Innovations GmbH 2018.
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.client.adapter;

import org.eclipse.sw360.antenna.sw360.client.rest.MultiStatusResponse;
import org.eclipse.sw360.antenna.sw360.client.rest.PagingResult;
import org.eclipse.sw360.antenna.sw360.client.rest.SW360ComponentClient;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360HalResourceUtility;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.ComponentSearchParams;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360Component;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360SparseComponent;
import org.eclipse.sw360.antenna.sw360.client.utils.FutureUtils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.sw360.antenna.sw360.client.utils.FutureUtils.optionalFuture;

class SW360ComponentClientAdapterAsyncImpl implements SW360ComponentClientAdapterAsync {
    private final SW360ComponentClient componentClient;

    public SW360ComponentClientAdapterAsyncImpl(SW360ComponentClient client) {
        componentClient = client;
    }

    @Override
    public SW360ComponentClient getComponentClient() {
        return componentClient;
    }

    @Override
    public CompletableFuture<SW360Component> createComponent(SW360Component component) {
        return FutureUtils.wrapInFuture(() -> SW360ComponentAdapterUtils.validateComponent(component),
                "Cannot create invalid component for " + component.getName())
                .thenCompose(getComponentClient()::createComponent);
    }

    @Override
    public CompletableFuture<Optional<SW360Component>> getComponentById(String componentId) {
        return optionalFuture(getComponentClient().getComponent(componentId));
    }

    @Override
    public CompletableFuture<Optional<SW360Component>> getComponentByName(String componentName) {
        ComponentSearchParams searchParams = ComponentSearchParams.builder()
                .withName(componentName)
                .build();
        return getComponentClient().search(searchParams)
                .thenCompose(components ->
                        components.getResult().stream()
                                .filter(c -> c.getName().equals(componentName))
                                .map(c -> SW360HalResourceUtility.getLastIndexOfSelfLink(c.getLinks()).orElse(""))
                                .map(this::getComponentById)
                                .findFirst()
                                .orElse(CompletableFuture.completedFuture(Optional.empty()))
                );
    }

    @Override
    public CompletableFuture<List<SW360SparseComponent>> search(ComponentSearchParams searchParams) {
        return searchWithPaging(searchParams)
                .thenApply(PagingResult::getResult);
    }

    @Override
    public CompletableFuture<PagingResult<SW360SparseComponent>> searchWithPaging(ComponentSearchParams searchParams) {
        return getComponentClient().search(searchParams);
    }

    @Override
    public CompletableFuture<SW360Component> updateComponent(SW360Component component) {
        return FutureUtils.wrapInFuture(() -> SW360ComponentAdapterUtils.validateComponent(component),
                "Cannot update invalid component for " + component.getName())
                .thenCompose(getComponentClient()::patchComponent);
    }

    @Override
    public CompletableFuture<MultiStatusResponse> deleteComponents(Collection<String> idsToDelete) {
        return SW360DeleteUtils.deleteEntities(getComponentClient()::deleteComponents, idsToDelete);
    }

    @Override
    public CompletableFuture<Void> deleteComponent(String componentId) {
        return SW360DeleteUtils.deleteEntity(getComponentClient()::deleteComponents,
                componentId, "component");
    }
}
