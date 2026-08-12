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

import org.eclipse.sw360.archival.bundle.ArchiveBuilder;
import org.eclipse.sw360.archival.bundle.CollectedEntity;
import org.eclipse.sw360.archival.db.ArchivalDatabaseHandler;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.services.archival.ArchivalEntityType;
import org.eclipse.sw360.datahandler.services.archival.ArchivalRecord;
import org.eclipse.sw360.datahandler.services.archival.ArchivalStatus;
import org.eclipse.sw360.datahandler.services.archival.RestorePreview;
import org.eclipse.sw360.datahandler.services.archival.RestoreResult;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RestoreHandlerTest {

    private final User user = new User().setEmail("taanvi@example.com");

    @Test
    void restore_reinsertsNewEntity_andFlipsMatchingRecord() throws Exception {
        FakeDb db = new FakeDb();
        db.seed(record("bundle-1", "proj-1", ArchivalStatus.ARCHIVED));

        DatabaseConnectorCloudant entityDb = mock(DatabaseConnectorCloudant.class);
        DatabaseConnectorCloudant attachmentDb = mock(DatabaseConnectorCloudant.class);
        when(entityDb.add(any())).thenReturn(true); // not present -> inserted

        RestoreHandler handler = new RestoreHandler(db, entityDb, attachmentDb);
        byte[] bundle = bundle("bundle-1", project("proj-1", "Empty Alpha"));

        RestoreResult result = handler.restore(new ByteArrayInputStream(bundle), user);

        assertEquals(1, result.getRestoredCount());
        assertEquals(0, result.getSkippedCount());
        assertEquals(0, result.getFailedCount());
        assertEquals(RestoreResult.Outcome.RESTORED, result.getEntries().get(0).getOutcome());

        // the entity was re-inserted, and its archival record was flipped to RESTORED
        verify(entityDb, times(1)).add(any(Project.class));
        ArchivalRecord flipped = db.byId.get("rec-proj-1");
        assertEquals(ArchivalStatus.RESTORED, flipped.getStatus());
        assertEquals("taanvi@example.com", flipped.getRestoredBy());
    }

    @Test
    void restore_skipsEntityThatAlreadyExists_andLeavesRecordUntouched() throws Exception {
        FakeDb db = new FakeDb();
        db.seed(record("bundle-1", "proj-1", ArchivalStatus.ARCHIVED));

        DatabaseConnectorCloudant entityDb = mock(DatabaseConnectorCloudant.class);
        DatabaseConnectorCloudant attachmentDb = mock(DatabaseConnectorCloudant.class);
        when(entityDb.add(any())).thenReturn(false); // already present -> skipped

        RestoreHandler handler = new RestoreHandler(db, entityDb, attachmentDb);
        byte[] bundle = bundle("bundle-1", project("proj-1", "Empty Alpha"));

        RestoreResult result = handler.restore(new ByteArrayInputStream(bundle), user);

        assertEquals(0, result.getRestoredCount());
        assertEquals(1, result.getSkippedCount());
        assertEquals(RestoreResult.Outcome.SKIPPED, result.getEntries().get(0).getOutcome());

        // a skipped entity must not upload attachment content, nor flip the record
        verify(attachmentDb, never()).createAttachment(anyString(), anyString(), any(), anyString());
        assertEquals(ArchivalStatus.ARCHIVED, db.byId.get("rec-proj-1").getStatus());
    }

    @Test
    void restore_insertsReleasesBeforeProjects() throws Exception {
        FakeDb db = new FakeDb();
        DatabaseConnectorCloudant entityDb = mock(DatabaseConnectorCloudant.class);
        DatabaseConnectorCloudant attachmentDb = mock(DatabaseConnectorCloudant.class);
        when(entityDb.add(any())).thenReturn(true);

        RestoreHandler handler = new RestoreHandler(db, entityDb, attachmentDb);
        // deliberately list the project first so ordering has to reorder it
        byte[] bundle = bundle("bundle-1",
                project("proj-1", "Web Stack"),
                release("rel-1", "Flask"));

        handler.restore(new ByteArrayInputStream(bundle), user);

        ArgumentCaptor<Object> inserted = ArgumentCaptor.forClass(Object.class);
        verify(entityDb, times(2)).add(inserted.capture());
        List<Object> order = inserted.getAllValues();
        assertInstanceOf(Release.class, order.get(0), "releases restore before projects");
        assertInstanceOf(Project.class, order.get(1));
    }

    @Test
    void preview_reportsConflictOnlyForEntitiesAlreadyPresent() throws Exception {
        FakeDb db = new FakeDb();
        DatabaseConnectorCloudant entityDb = mock(DatabaseConnectorCloudant.class);
        DatabaseConnectorCloudant attachmentDb = mock(DatabaseConnectorCloudant.class);
        when(entityDb.contains("proj-1")).thenReturn(true);  // present -> conflict
        when(entityDb.contains("rel-1")).thenReturn(false);  // absent  -> restorable

        RestoreHandler handler = new RestoreHandler(db, entityDb, attachmentDb);
        byte[] bundle = bundle("bundle-1",
                project("proj-1", "Web Stack"),
                release("rel-1", "Flask"));

        RestorePreview preview = handler.preview(new ByteArrayInputStream(bundle), user);

        Map<String, Boolean> conflictById = new HashMap<>();
        for (RestorePreview.Entry e : preview.getEntries()) {
            conflictById.put(e.getEntityId(), e.isConflict());
        }
        assertTrue(conflictById.get("proj-1"), "present entity is a conflict");
        assertTrue(!conflictById.get("rel-1"), "absent entity is not a conflict");
    }

    @Test
    void restore_withNoMatchingRecords_stillRestoresWithoutError() throws Exception {
        FakeDb db = new FakeDb(); // empty registry: bundle came from another instance
        DatabaseConnectorCloudant entityDb = mock(DatabaseConnectorCloudant.class);
        DatabaseConnectorCloudant attachmentDb = mock(DatabaseConnectorCloudant.class);
        when(entityDb.add(any())).thenReturn(true);

        RestoreHandler handler = new RestoreHandler(db, entityDb, attachmentDb);
        byte[] bundle = bundle("bundle-1", project("proj-1", "Orphan"));

        RestoreResult result = handler.restore(new ByteArrayInputStream(bundle), user);

        assertEquals(1, result.getRestoredCount());
        assertTrue(db.updated.isEmpty(), "no records to flip when the registry has none");
    }

    // --- helpers -----------------------------------------------------------

    private static byte[] bundle(String bundleId, CollectedEntity... entities) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new ArchiveBuilder(bundleId, "taanvi@example.com", "20.1.0", "test")
                .writeTo(out, List.of(entities));
        return out.toByteArray();
    }

    private static CollectedEntity project(String id, String name) {
        return entity(id, name, ArchivalEntityType.PROJECT, "project.json");
    }

    private static CollectedEntity release(String id, String name) {
        return entity(id, name, ArchivalEntityType.RELEASE, "release.json");
    }

    private static CollectedEntity entity(String id, String name, ArchivalEntityType type, String docName) {
        Map<String, byte[]> docs = new HashMap<>();
        docs.put(docName, ("{\"id\":\"" + id + "\",\"name\":\"" + name + "\"}")
                .getBytes(StandardCharsets.UTF_8));
        return new CollectedEntity(id, name, "1.0", type, docs, List.of());
    }

    private static ArchivalRecord record(String bundleId, String entityId, ArchivalStatus status) {
        ArchivalRecord r = new ArchivalRecord();
        r.setId("rec-" + entityId);
        r.setBundleId(bundleId);
        r.setEntityId(entityId);
        r.setStatus(status);
        return r;
    }

    /** In-memory stand-in for ArchivalDatabaseHandler (the real one needs Cloudant). */
    static class FakeDb extends ArchivalDatabaseHandler {
        final Map<String, ArchivalRecord> byId = new HashMap<>();
        final List<ArchivalRecord> updated = new ArrayList<>();

        FakeDb() {
            super((org.eclipse.sw360.archival.db.ArchivalRepository) null);
        }

        void seed(ArchivalRecord r) {
            byId.put(r.getId(), r);
        }

        @Override
        public List<ArchivalRecord> getAll() {
            return new ArrayList<>(byId.values());
        }

        @Override
        public boolean update(ArchivalRecord r) {
            if (!byId.containsKey(r.getId())) {
                return false;
            }
            updated.add(r);
            byId.put(r.getId(), r);
            return true;
        }
    }
}
