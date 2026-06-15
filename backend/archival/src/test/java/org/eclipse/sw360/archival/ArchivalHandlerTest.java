/*
 * Copyright Taanvi Khevaria, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.archival;

import org.eclipse.sw360.archival.bundle.ByteArrayAttachmentSource;
import org.eclipse.sw360.archival.bundle.CollectedEntity;
import org.eclipse.sw360.archival.bundle.InMemoryEntityProvider;
import org.eclipse.sw360.archival.db.ArchivalDatabaseHandler;
import org.eclipse.sw360.datahandler.services.archival.ArchivalEntityType;
import org.eclipse.sw360.datahandler.services.archival.ArchivalRecord;
import org.eclipse.sw360.datahandler.services.archival.ArchivalStatus;
import org.eclipse.sw360.datahandler.services.archival.ArchiveRequest;
import org.eclipse.sw360.datahandler.services.archival.AttachmentMetadata;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchivalHandlerTest {

    @Test
    void archive_writesOneRecordPerEntity_withSharedBundleIdAndArchivedStatus() throws Exception {
        FakeDb fakeDb = new FakeDb();
        ArchivalHandler handler = new ArchivalHandler(fakeDb);

        InMemoryEntityProvider provider = new InMemoryEntityProvider()
                .register(release("rel-1", "Release One"))
                .register(release("rel-2", "Release Two"))
                .register(release("rel-3", "Release Three"));

        ArchiveRequest req = new ArchiveRequest();
        req.setEntityType(ArchivalEntityType.RELEASE);
        req.setEntityIds(List.of("rel-1", "rel-2", "rel-3"));
        req.setComment("Q2 cleanup");

        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        ArchiveResult result = handler.archive(req, "taanvi@example.com", sink, provider);

        List<ArchivalRecord> records = result.records();
        assertEquals(3, records.size());

        String bundleId = records.get(0).getBundleId();
        assertNotNull(bundleId);
        assertTrue(bundleId.startsWith("bundle-"));
        assertEquals(bundleId, result.bundleId());

        for (ArchivalRecord r : records) {
            assertEquals(bundleId, r.getBundleId(), "all records share the bundle id");
            assertEquals(ArchivalStatus.ARCHIVED, r.getStatus());
            assertEquals(ArchivalEntityType.RELEASE, r.getEntityType());
            assertEquals("taanvi@example.com", r.getArchivedBy());
            assertEquals("Q2 cleanup", r.getComment());
            assertNotNull(r.getArchivedAt());
        }

        assertEquals(3, fakeDb.added.size(), "3 rows initially added");
        assertEquals(3, fakeDb.updated.size(), "3 rows flipped to ARCHIVED");
        assertTrue(sink.size() > 0, "TAR.GZ bytes were streamed to the sink");
    }

    private static CollectedEntity release(String id, String name) {
        AttachmentMetadata meta = new AttachmentMetadata();
        meta.setAttachmentId("att-" + id);
        meta.setFilename(id + "-source.zip");
        meta.setContentType("application/zip");

        Map<String, byte[]> docs = new HashMap<>();
        docs.put("release.json", ("{\"id\":\"" + id + "\"}").getBytes(StandardCharsets.UTF_8));

        byte[] body = ("fake-bytes-for-" + id).getBytes(StandardCharsets.UTF_8);
        return new CollectedEntity(id, name, "1.0", ArchivalEntityType.RELEASE, docs,
                List.of(new ByteArrayAttachmentSource(meta, body)));
    }

    /** Stand-in for ArchivalDatabaseHandler; the real one needs a Cloudant instance. */
    static class FakeDb extends ArchivalDatabaseHandler {
        final List<ArchivalRecord> added = new ArrayList<>();
        final List<ArchivalRecord> updated = new ArrayList<>();
        final Map<String, ArchivalRecord> byId = new HashMap<>();

        FakeDb() {
            super((org.eclipse.sw360.archival.db.ArchivalRepository) null);
        }

        @Override
        public ArchivalRecord add(ArchivalRecord r) {
            added.add(r);
            byId.put(r.getId(), r);
            return r;
        }

        @Override
        public boolean update(ArchivalRecord r) {
            if (!byId.containsKey(r.getId())) return false;
            updated.add(r);
            byId.put(r.getId(), r);
            return true;
        }
    }
}
