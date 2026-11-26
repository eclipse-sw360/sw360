/*
 * Copyright Siemens AG, 2026.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FileBasedResponseCacheTest {
    @TempDir
    Path directory;

    @Test
    void shouldReadLegacyMetadataAndPreserveCachePayload() throws Exception {
        CachedEndpoint endpoint = CachedEndpoint.COMPONENTS_ALL_DETAILS;
        CacheConfiguration config = CacheConfiguration.builder()
                .endpoint(endpoint).enabled(true).cacheDirectory(directory)
                .ttlSeconds(3600).maxStaleSeconds(300).build();
        String base = endpoint.getCacheKey() + "-role";
        byte[] body = "{\"content\":\"example\"}".getBytes(StandardCharsets.UTF_8);
        Files.write(directory.resolve(base + ".json"), body);
        FileBasedResponseCache.CacheMetadata metadata =
                new FileBasedResponseCache.CacheMetadata(Instant.now().toString(), body.length);
        String metadataJson = "{\"createdAt\":\"" + metadata.getCreatedAt() + "\",\"sizeBytes\":" + body.length + "}";
        Files.writeString(directory.resolve(base + ".meta"), metadataJson);
        FileBasedResponseCache<?> cache = new FileBasedResponseCache<>(
                endpoint, config, JsonMapper.builderWithJackson2Defaults().findAndAddModules().build(), "role");

        try (var stream = cache.getAsStream().orElseThrow()) {
            assertArrayEquals(body, stream.readAllBytes());
        }
        cache.putBytes(body);
        JsonMapper mapper = JsonMapper.builderWithJackson2Defaults().build();
        String expected = "{\"createdAt\":\"" + cache.getStatistics().getCreatedAt()
                + "\",\"sizeBytes\":" + body.length + "}";
        assertEquals(mapper.readTree(expected), mapper.readTree(Files.readString(directory.resolve(base + ".meta"))));
    }
}
