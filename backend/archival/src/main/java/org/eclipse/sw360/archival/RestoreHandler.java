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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.archival.bundle.BundleReader;
import org.eclipse.sw360.archival.bundle.RestoredEntity;
import org.eclipse.sw360.archival.bundle.ThriftJson;
import org.eclipse.sw360.archival.db.ArchivalDatabaseHandler;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.services.archival.ArchivalEntityType;
import org.eclipse.sw360.datahandler.services.archival.ArchivalRecord;
import org.eclipse.sw360.datahandler.services.archival.ArchivalStatus;
import org.eclipse.sw360.datahandler.services.archival.AttachmentMetadata;
import org.eclipse.sw360.datahandler.services.archival.ManifestEntry;
import org.eclipse.sw360.datahandler.services.archival.RestorePreview;
import org.eclipse.sw360.datahandler.services.archival.RestoreResult;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Restores entities from a previously produced archival bundle.
 *
 * <p>Restore is upload driven: the caller supplies the TAR.GZ they downloaded at
 * archive time. The decision for each entity is presence based — if it already
 * exists in the live database it is skipped, otherwise it is re-inserted with its
 * original id and a stripped revision. Entities are inserted in dependency order
 * (releases before the components/projects/packages that reference them).
 */
@Service
public class RestoreHandler {

    private static final Logger log = LogManager.getLogger(RestoreHandler.class);

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final ArchivalDatabaseHandler db;
    private DatabaseConnectorCloudant entityDb;
    private DatabaseConnectorCloudant attachmentDb;

    public RestoreHandler() throws IOException {
        this.db = new ArchivalDatabaseHandler(
                DatabaseSettings.getConfiguredClient(),
                ArchivalConstants.COUCH_DB_ARCHIVAL);
    }

    RestoreHandler(ArchivalDatabaseHandler db) {
        this.db = db;
    }

    /**
     * Dry run: reports, per entity, whether it would be restored or skipped
     * (already present), without changing anything.
     */
    public RestorePreview preview(InputStream bundle, User user) throws SW360Exception {
        BundleReader.Bundle read = readBundle(bundle);

        List<RestorePreview.Entry> entries = new ArrayList<>();
        for (RestoredEntity re : read.entities()) {
            ManifestEntry me = re.entry();
            RestorePreview.Entry e = new RestorePreview.Entry();
            e.setEntityId(me.getEntityId());
            e.setEntityName(me.getEntityName());
            e.setEntityType(me.getEntityType());
            e.setAttachmentCount(me.getAttachmentCount());
            e.setConflict(entityExists(me.getEntityId()));
            entries.add(e);
        }

        RestorePreview preview = new RestorePreview();
        preview.setBundleId(read.manifest().getBundleId());
        preview.setEntries(entries);
        return preview;
    }

    /**
     * Re-inserts the bundle's entities into the live database in dependency order,
     * skipping any that already exist, and flips the matching archival records to
     * RESTORED.
     */
    public RestoreResult restore(InputStream bundle, User user) throws SW360Exception {
        BundleReader.Bundle read = readBundle(bundle);

        List<RestoredEntity> ordered = new ArrayList<>(read.entities());
        ordered.sort(Comparator.comparingInt(re -> insertPriority(re.entry().getEntityType())));

        List<RestoreResult.Entry> results = new ArrayList<>();
        Set<String> restoredIds = new HashSet<>();
        int restored = 0;
        int skipped = 0;
        int failed = 0;

        for (RestoredEntity re : ordered) {
            ManifestEntry me = re.entry();
            RestoreResult.Entry res = new RestoreResult.Entry();
            res.setEntityId(me.getEntityId());
            res.setEntityType(me.getEntityType());
            try {
                RestoreResult.Outcome outcome = restoreOne(re);
                res.setOutcome(outcome);
                switch (outcome) {
                    case RESTORED -> { restored++; restoredIds.add(me.getEntityId()); }
                    case SKIPPED -> { skipped++; res.setReason("already exists in the live database"); }
                    case FAILED -> failed++;
                }
            } catch (Exception e) {
                log.error("restore failed for {} {}", me.getEntityType(), me.getEntityId(), e);
                res.setOutcome(RestoreResult.Outcome.FAILED);
                res.setReason(describe(e));
                failed++;
            }
            results.add(res);
        }

        markRecordsRestored(read.manifest().getBundleId(), restoredIds, user);

        RestoreResult result = new RestoreResult();
        result.setBundleId(read.manifest().getBundleId());
        result.setEntries(results);
        result.setRestoredCount(restored);
        result.setSkippedCount(skipped);
        result.setFailedCount(failed);
        return result;
    }

    private RestoreResult.Outcome restoreOne(RestoredEntity re) throws Exception {
        ManifestEntry me = re.entry();
        byte[] document = re.primaryDocument();
        if (document == null) {
            throw new SW360Exception("bundle entry " + me.getEntityId() + " has no entity document");
        }
        Object entity = deserializeStrippingRevision(me.getEntityType(), document);
        // add() preserves the pre-set id and returns false when the id already
        // exists, which is exactly the presence-based skip we want.
        boolean added = entityDb().add(entity);
        if (!added) {
            return RestoreResult.Outcome.SKIPPED;
        }
        restoreAttachmentContent(re);
        return RestoreResult.Outcome.RESTORED;
    }

    /**
     * Re-creates the AttachmentContent documents and their binary blobs for a
     * restored entity, preserving the original attachment content ids so the
     * entity's attachment references resolve. Content already present is left
     * untouched (add returns false), which also dedupes shared attachments.
     */
    private void restoreAttachmentContent(RestoredEntity re) {
        for (Map.Entry<String, byte[]> att : re.attachmentContent().entrySet()) {
            String attachmentContentId = att.getKey();
            byte[] binary = att.getValue();
            try {
                AttachmentMetadata meta = readMeta(re.attachmentMeta().get(attachmentContentId));
                String filename = meta != null && meta.getFilename() != null
                        ? meta.getFilename() : attachmentContentId;
                String contentType = meta != null && meta.getContentType() != null
                        ? meta.getContentType() : "application/octet-stream";

                AttachmentContent content = new AttachmentContent()
                        .setId(attachmentContentId)
                        .setFilename(filename)
                        .setContentType(contentType);

                // add() creates the doc with the preserved id (and a placeholder
                // blob); createAttachment then puts the real bytes under the same
                // name, replacing the placeholder. Skip if it already exists.
                if (attachmentDb().add(content)) {
                    attachmentDb().createAttachment(attachmentContentId, filename,
                            new ByteArrayInputStream(binary), contentType);
                }
            } catch (Exception e) {
                log.warn("could not restore attachment content {} for {}: {}",
                        attachmentContentId, re.entry().getEntityId(), describe(e));
            }
        }
    }

    private static AttachmentMetadata readMeta(byte[] metaJson) {
        if (metaJson == null) {
            return null;
        }
        try {
            return JSON.readValue(metaJson, AttachmentMetadata.class);
        } catch (IOException e) {
            return null;
        }
    }

    private static Object deserializeStrippingRevision(ArchivalEntityType type, byte[] json) throws IOException {
        return switch (type) {
            case PROJECT -> {
                Project p = ThriftJson.fromJson(json, Project.class);
                p.unsetRevision();
                yield p;
            }
            case COMPONENT -> {
                Component c = ThriftJson.fromJson(json, Component.class);
                c.unsetRevision();
                yield c;
            }
            case RELEASE -> {
                Release r = ThriftJson.fromJson(json, Release.class);
                r.unsetRevision();
                yield r;
            }
            case PACKAGE -> {
                Package pkg = ThriftJson.fromJson(json, Package.class);
                pkg.unsetRevision();
                yield pkg;
            }
        };
    }

    /** Releases first, then components, then projects, then packages. */
    private static int insertPriority(ArchivalEntityType type) {
        return switch (type) {
            case RELEASE -> 0;
            case COMPONENT -> 1;
            case PROJECT -> 2;
            case PACKAGE -> 3;
        };
    }

    private void markRecordsRestored(String bundleId, Set<String> restoredIds, User user) {
        if (restoredIds.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        for (ArchivalRecord r : db.getAll()) {
            if (!bundleId.equals(r.getBundleId()) || !restoredIds.contains(r.getEntityId())) {
                continue;
            }
            r.setStatus(ArchivalStatus.RESTORED);
            r.setRestoredBy(user.getEmail());
            r.setRestoredAt(now);
            if (!db.update(r)) {
                log.warn("archival record {} could not be flipped to RESTORED", r.getId());
            }
        }
    }

    private boolean entityExists(String id) {
        return entityDb().contains(id);
    }

    private synchronized DatabaseConnectorCloudant entityDb() {
        if (entityDb == null) {
            entityDb = new DatabaseConnectorCloudant(
                    DatabaseSettings.getConfiguredClient(),
                    DatabaseSettings.COUCH_DB_DATABASE);
        }
        return entityDb;
    }

    private synchronized DatabaseConnectorCloudant attachmentDb() {
        if (attachmentDb == null) {
            attachmentDb = new DatabaseConnectorCloudant(
                    DatabaseSettings.getConfiguredClient(),
                    DatabaseSettings.COUCH_DB_ATTACHMENTS);
        }
        return attachmentDb;
    }

    private BundleReader.Bundle readBundle(InputStream bundle) throws SW360Exception {
        try {
            return BundleReader.read(bundle);
        } catch (IOException e) {
            throw new SW360Exception("could not read the uploaded bundle: " + describe(e));
        }
    }

    private static String describe(Throwable e) {
        String message = e.getMessage();
        return CommonUtils.isNullEmptyOrWhitespace(message) ? e.getClass().getName() : message;
    }
}
