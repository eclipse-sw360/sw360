/*
 * Copyright Taanvi Khevaria, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.archival.bundle;

import org.eclipse.sw360.services.archival.ArchivalEntityType;

import java.util.HashMap;
import java.util.Map;

/**
 * Test implementation that stores CollectedEntity instances in memory.
 * Allows tests to prepopulate entities by ID without any SW360 or CouchDB dependencies.
 */
public final class InMemoryEntityProvider implements EntityProvider {

    private final Map<String, CollectedEntity> byId = new HashMap<>();

    public InMemoryEntityProvider register(CollectedEntity e) {
        byId.put(key(e.entityType(), e.entityId()), e);
        return this;
    }

    @Override
    public boolean includeAttachments() { return true; }

    @Override
    public boolean includeChangelogs() { return true; }

    @Override
    public CollectedEntity collect(ArchivalEntityType type, String entityId) {
        CollectedEntity hit = byId.get(key(type, entityId));
        if (hit == null) {
            throw new IllegalArgumentException("No entity registered for " + type + "/" + entityId);
        }
        return hit;
    }

    private static String key(ArchivalEntityType type, String id) {
        return type.name() + ":" + id;
    }
}
