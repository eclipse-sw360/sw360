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

import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.Duration;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.PackageDatabaseHandler;
import org.eclipse.sw360.datahandler.db.ProjectDatabaseHandler;
import org.eclipse.sw360.datahandler.db.UserRepository;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.services.archival.ArchivalEntityType;
import org.eclipse.sw360.datahandler.services.archival.AttachmentMetadata;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Production EntityProvider that reads from the live SW360 databases.
 * Wired for RELEASE; PROJECT / COMPONENT / PACKAGE remain stubs for now.
 *
 * Planned wiring:
 *   PROJECT   -> ProjectDatabaseHandler.getProjectByIdIgnoringVisibility(id)
 *                + ChangeLogsDatabaseHandler.getChangeLogsByDocumentId(id)
 *                + ProjectObligationRepository, AttachmentUsageRepository
 *   COMPONENT -> ComponentDatabaseHandler.getComponent(id, user) (+ all linked Releases)
 *   PACKAGE   -> PackageDatabaseHandler.getPackageById(id)
 */
public class Sw360EntityProvider implements EntityProvider {

    private static final Duration ATTACHMENT_DOWNLOAD_TIMEOUT =
            Duration.durationOf(5, TimeUnit.MINUTES);

    private final boolean includeAttachments;
    private final boolean includeChangelogs;
    private final String userEmail;

    private ComponentDatabaseHandler componentHandler;
    private ProjectDatabaseHandler projectHandler;
    private PackageDatabaseHandler packageHandler;
    private AttachmentConnector attachmentConnector;
    private UserRepository userRepository;

    public Sw360EntityProvider(boolean includeAttachments, boolean includeChangelogs, String userEmail) {
        this.includeAttachments = includeAttachments;
        this.includeChangelogs = includeChangelogs;
        this.userEmail = userEmail;
    }

    @Override
    public boolean includeAttachments() { return includeAttachments; }

    @Override
    public boolean includeChangelogs() { return includeChangelogs; }

    @Override
    public CollectedEntity collect(ArchivalEntityType type, String entityId) throws Exception {
        return switch (type) {
            case RELEASE -> collectRelease(entityId, false);
            case PROJECT -> collectProject(entityId);
            case COMPONENT -> collectComponentOnly(entityId);
            case PACKAGE -> collectPackage(entityId);
        };
    }

    /**
     * Returns the full bundle for a Project archive: the Project document itself,
     * followed by one CollectedEntity per linked Release. Each Release is flagged
     * keepAlive=true when other live Projects still reference it.
     */
    public List<CollectedEntity> collectProjectBundle(String projectId) throws Exception {
        Project project = projectHandler().getProjectByIdIgnoringVisibility(projectId);
        if (project == null) {
            throw new SW360Exception("Project " + projectId + " not found");
        }

        List<CollectedEntity> bundle = new ArrayList<>();
        bundle.add(collectedFromProject(project));

        if (project.getReleaseIdToUsage() != null) {
            for (String releaseId : project.getReleaseIdToUsage().keySet()) {
                bundle.add(collectRelease(releaseId, isReleaseSharedWithOtherProjects(releaseId, projectId)));
            }
        }
        return bundle;
    }

    private CollectedEntity collectProject(String projectId) throws Exception {
        Project project = projectHandler().getProjectByIdIgnoringVisibility(projectId);
        if (project == null) {
            throw new SW360Exception("Project " + projectId + " not found");
        }
        return collectedFromProject(project);
    }

    private CollectedEntity collectedFromProject(Project project) throws IOException, SW360Exception {
        Map<String, byte[]> documents = new LinkedHashMap<>();
        documents.put("project.json", ThriftJson.toJsonBytes(project));

        List<AttachmentSource> attachments = new ArrayList<>();
        if (includeAttachments && project.getAttachments() != null) {
            for (Attachment att : project.getAttachments()) {
                AttachmentSource source = buildAttachmentSource(att);
                if (source != null) attachments.add(source);
            }
        }

        return new CollectedEntity(
                project.getId(),
                project.getName(),
                project.getVersion(),
                ArchivalEntityType.PROJECT,
                documents,
                attachments,
                false);
    }

    private CollectedEntity collectRelease(String releaseId, boolean keepAlive) throws Exception {
        User user = resolveUser();
        Release release = componentHandler().getRelease(releaseId, user);
        if (release == null) {
            throw new SW360Exception("Release " + releaseId + " not found");
        }

        Map<String, byte[]> documents = new LinkedHashMap<>();
        documents.put("release.json", ThriftJson.toJsonBytes(release));

        List<AttachmentSource> attachments = new ArrayList<>();
        if (includeAttachments && release.getAttachments() != null) {
            for (Attachment att : release.getAttachments()) {
                AttachmentSource source = buildAttachmentSource(att);
                if (source != null) attachments.add(source);
            }
        }

        return new CollectedEntity(
                release.getId(),
                release.getName(),
                release.getVersion(),
                ArchivalEntityType.RELEASE,
                documents,
                attachments,
                keepAlive);
    }

    /**
     * True if this release is referenced by any Project other than the one being archived.
     * Backed by ProjectRepository's existing searchByReleaseId view.
     */
    private boolean isReleaseSharedWithOtherProjects(String releaseId, String archivedProjectId) throws Exception {
        User user = resolveUser();
        return projectHandler().searchByReleaseId(releaseId, user).stream()
                .anyMatch(p -> p.getId() != null && !p.getId().equals(archivedProjectId));
    }

    /**
     * Removes the Project from the live database using SW360's existing
     * deleteProject pipeline. Linked Releases are deliberately untouched —
     * SW360's Project delete does not cascade to Releases.
     */
    public RequestStatus deleteProject(String projectId) throws Exception {
        return projectHandler().deleteProject(projectId, resolveUser(), true);
    }

    // ---------------- COMPONENT ----------------

    /**
     * Returns the full bundle for a Component archive: the Component document itself
     * followed by one CollectedEntity per Release under it. Each Release is flagged
     * keepAlive=true when a live Project still references it.
     */
    public List<CollectedEntity> collectComponentBundle(String componentId) throws Exception {
        User user = resolveUser();
        Component component = componentHandler().getComponent(componentId, user);
        if (component == null) {
            throw new SW360Exception("Component " + componentId + " not found");
        }

        List<CollectedEntity> bundle = new ArrayList<>();
        bundle.add(collectedFromComponent(component));

        if (component.getReleaseIds() != null) {
            for (String releaseId : component.getReleaseIds()) {
                bundle.add(collectRelease(releaseId, isReleaseSharedWithLiveProjects(releaseId)));
            }
        }
        return bundle;
    }

    private CollectedEntity collectComponentOnly(String componentId) throws Exception {
        User user = resolveUser();
        Component component = componentHandler().getComponent(componentId, user);
        if (component == null) {
            throw new SW360Exception("Component " + componentId + " not found");
        }
        return collectedFromComponent(component);
    }

    private CollectedEntity collectedFromComponent(Component component) throws IOException, SW360Exception {
        Map<String, byte[]> documents = new LinkedHashMap<>();
        documents.put("component.json", ThriftJson.toJsonBytes(component));

        List<AttachmentSource> attachments = new ArrayList<>();
        if (includeAttachments && component.getAttachments() != null) {
            for (Attachment att : component.getAttachments()) {
                AttachmentSource source = buildAttachmentSource(att);
                if (source != null) attachments.add(source);
            }
        }

        return new CollectedEntity(
                component.getId(),
                component.getName(),
                null,
                ArchivalEntityType.COMPONENT,
                documents,
                attachments,
                false);
    }

    /**
     * True if this release is referenced by any live Project (any project — no exclusion).
     * Used by Component archive where there is no "the project we're archiving" to exclude.
     */
    private boolean isReleaseSharedWithLiveProjects(String releaseId) throws Exception {
        User user = resolveUser();
        return !projectHandler().searchByReleaseId(releaseId, user).isEmpty();
    }

    /**
     * Custom Component deletion that respects the keep-alive rule.
     * For each Release under the Component:
     *   - if shared with a live Project → unlink it from the Component's releaseIds so
     *     the Release survives on its own.
     *   - otherwise → deleteRelease normally.
     * Then removes the Component itself. Uses only existing SW360 building blocks.
     * Returns SUCCESS on happy path; if any step failed the caller inspects the
     * remaining live Releases via the returned status list.
     */
    public RequestStatus deleteComponentRespectingSharedReleases(String componentId,
                                                                 Set<String> sharedReleaseIds) throws Exception {
        User user = resolveUser();
        Component component = componentHandler().getComponent(componentId, user);
        if (component == null) return RequestStatus.FAILURE;

        Set<String> allReleaseIds = component.getReleaseIds() == null
                ? Collections.emptySet()
                : new HashSet<>(component.getReleaseIds());

        // Unlink shared Releases so Component delete doesn't cascade into them.
        if (!sharedReleaseIds.isEmpty()) {
            Component patched = componentHandler().getComponent(componentId, user);
            Set<String> keep = new HashSet<>(patched.getReleaseIds());
            keep.removeAll(sharedReleaseIds);
            patched.setReleaseIds(keep);
            componentHandler().updateComponent(patched, user, true);
        }

        // Delete non-shared Releases individually first (belt and suspenders — the
        // Component delete would delete them anyway, but doing it here lets us
        // report per-release failures).
        for (String releaseId : allReleaseIds) {
            if (sharedReleaseIds.contains(releaseId)) continue;
            RequestStatus rs = componentHandler().deleteRelease(releaseId, user, true);
            if (rs != RequestStatus.SUCCESS) {
                return rs; // surface the specific failure
            }
        }

        // The Component now only owns Releases that were already deleted above (via
        // its refreshed releaseIds). Delete the Component itself.
        return componentHandler().deleteComponent(componentId, user, true);
    }

    // ---------------- PACKAGE ----------------

    private CollectedEntity collectPackage(String packageId) throws Exception {
        Package pkg = packageHandler().getPackageById(packageId);
        if (pkg == null) {
            throw new SW360Exception("Package " + packageId + " not found");
        }

        Map<String, byte[]> documents = new LinkedHashMap<>();
        documents.put("package.json", ThriftJson.toJsonBytes(pkg));

        return new CollectedEntity(
                pkg.getId(),
                pkg.getName(),
                pkg.getVersion(),
                ArchivalEntityType.PACKAGE,
                documents,
                new ArrayList<>(),
                false);
    }

    /**
     * True if the Package's parent Release still exists in the live DB.
     * Used to refuse Package archival when the parent Release is still live.
     */
    public boolean packageHasLiveParentRelease(String packageId) throws Exception {
        Package pkg = packageHandler().getPackageById(packageId);
        if (pkg == null) return false;
        String parentReleaseId = pkg.getReleaseId();
        if (parentReleaseId == null || parentReleaseId.isBlank()) return false;
        try {
            User user = resolveUser();
            Release release = componentHandler().getRelease(parentReleaseId, user);
            return release != null;
        } catch (SW360Exception notFound) {
            return false;
        }
    }

    public RequestStatus deletePackage(String packageId) throws Exception {
        return packageHandler().deletePackage(packageId, resolveUser());
    }

    private AttachmentSource buildAttachmentSource(Attachment att) throws SW360Exception, IOException {
        AttachmentContent content = attachmentConnector().getAttachmentContent(att.getAttachmentContentId());
        if (content == null || Boolean.TRUE.equals(content.isOnlyRemote())) {
            return null;
        }

        long size;
        try (var probe = attachmentConnector().unsafeGetAttachmentStream(content)) {
            size = probe == null ? 0L : probe.transferTo(java.io.OutputStream.nullOutputStream());
        }

        AttachmentMetadata metadata = new AttachmentMetadata();
        metadata.setAttachmentId(content.getId());
        metadata.setFilename(content.getFilename());
        metadata.setContentType(content.getContentType());
        metadata.setSizeBytes(size);
        metadata.setSha1(att.getSha1());
        metadata.setAttachmentType(att.getAttachmentType() == null ? null : att.getAttachmentType().name());
        metadata.setUploadedBy(att.getCreatedBy());

        return new Sw360AttachmentSource(attachmentConnector(), content, metadata, size);
    }

    private User resolveUser() throws SW360Exception, MalformedURLException {
        if (userEmail == null || userEmail.isBlank() || "unknown".equals(userEmail)) {
            throw new SW360Exception("archive request has no user email; cannot satisfy permission-checked reads");
        }
        User user = userRepository().getByEmail(userEmail);
        if (user == null) {
            throw new SW360Exception("user " + userEmail + " not found in SW360");
        }
        return user;
    }

    private synchronized ComponentDatabaseHandler componentHandler() throws MalformedURLException {
        if (componentHandler == null) {
            componentHandler = new ComponentDatabaseHandler(
                    DatabaseSettings.getConfiguredClient(),
                    DatabaseSettings.COUCH_DB_DATABASE,
                    DatabaseSettings.COUCH_DB_CHANGE_LOGS,
                    DatabaseSettings.COUCH_DB_ATTACHMENTS);
        }
        return componentHandler;
    }

    private synchronized ProjectDatabaseHandler projectHandler() throws MalformedURLException {
        if (projectHandler == null) {
            projectHandler = new ProjectDatabaseHandler(
                    DatabaseSettings.getConfiguredClient(),
                    DatabaseSettings.COUCH_DB_DATABASE,
                    DatabaseSettings.COUCH_DB_CHANGE_LOGS,
                    DatabaseSettings.COUCH_DB_ATTACHMENTS);
        }
        return projectHandler;
    }

    private synchronized PackageDatabaseHandler packageHandler() throws MalformedURLException {
        if (packageHandler == null) {
            packageHandler = new PackageDatabaseHandler(
                    DatabaseSettings.getConfiguredClient(),
                    DatabaseSettings.COUCH_DB_DATABASE,
                    DatabaseSettings.COUCH_DB_CHANGE_LOGS,
                    DatabaseSettings.COUCH_DB_ATTACHMENTS);
        }
        return packageHandler;
    }

    private synchronized AttachmentConnector attachmentConnector() throws MalformedURLException {
        if (attachmentConnector == null) {
            attachmentConnector = new AttachmentConnector(
                    DatabaseSettings.getConfiguredClient(),
                    DatabaseSettings.COUCH_DB_ATTACHMENTS,
                    ATTACHMENT_DOWNLOAD_TIMEOUT);
        }
        return attachmentConnector;
    }

    private synchronized UserRepository userRepository() throws MalformedURLException {
        if (userRepository == null) {
            DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(
                    DatabaseSettings.getConfiguredClient(),
                    DatabaseSettings.COUCH_DB_USERS);
            userRepository = new UserRepository(db);
        }
        return userRepository;
    }
}
