/*
 * Copyright Siemens AG, 2024-2025.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.importexport;

import static org.eclipse.sw360.importer.ComponentImportUtils.getFlattenedView;
import static org.eclipse.sw360.importer.ComponentImportUtils.getReleasesById;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService.Iface;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.exporter.CSVExport;
import org.eclipse.sw360.importer.ComponentAttachmentCSVRecord;
import org.eclipse.sw360.importer.ComponentAttachmentCSVRecordBuilder;
import org.eclipse.sw360.importer.ComponentCSVRecord;
import org.eclipse.sw360.importer.ReleaseLinkCSVRecord;
import org.eclipse.sw360.importer.ReleaseLinkCSVRecordBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import com.google.common.collect.ImmutableList;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360ImportExportService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private static final String CONTENT_DISPOSITION = "Content-Disposition";

    public void getDownloadCsvComponentTemplate(User sw360User, HttpServletResponse response) throws IOException {
        if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
            throw new AccessDeniedException("User is not admin");
        }
        final Iterable<Iterable<String>> inputIterable = ImmutableList.of(ComponentCSVRecord.getSampleInputIterable());
        final Iterable<String> csvHeaderIterable = ComponentCSVRecord.getCSVHeaderIterable();

        ByteArrayInputStream byteArrayInputStream = CSVExport.createCSV(csvHeaderIterable, inputIterable);
        String filename = String.format("ComponentsReleasesVendorsSample_%s.csv", SW360Utils.getCreatedOn());
        response.setHeader(CONTENT_DISPOSITION, String.format("Components; filename=\"%s\"", filename));
        FileCopyUtils.copy(byteArrayInputStream, response.getOutputStream());
    }

    public void getDownloadAttachmentTemplate(User sw360User, HttpServletResponse response) throws IOException {
        if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
            throw new AccessDeniedException("User is not admin");
        }
        final Iterable<String> csvHeaderIterable = ComponentAttachmentCSVRecord.getCSVHeaderIterable();
        final Iterable<Iterable<String>> inputIterable = ImmutableList
                .of(ComponentAttachmentCSVRecord.getSampleInputIterable());

        ByteArrayInputStream byteArrayInputStream = CSVExport.createCSV(csvHeaderIterable, inputIterable);
        String filename = String.format("AttachmentInfo_Sample_%s.csv", SW360Utils.getCreatedOn());
        response.setHeader(CONTENT_DISPOSITION, String.format("Attachment; filename=\"%s\"", filename));
        FileCopyUtils.copy(byteArrayInputStream, response.getOutputStream());
    }

    public void getDownloadAttachmentInfo(User sw360User, HttpServletResponse response) throws IOException {
        List<Iterable<String>> csvRows = new ArrayList<>();
        if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
            throw new AccessDeniedException("User is not admin");
        }
        final List<Component> componentDetailedSummaryForExport = getComponentDetailedSummaryForExport();
        if (componentDetailedSummaryForExport != null) {
            for (Component component : componentDetailedSummaryForExport) {
                printComponentAttachments(component, csvRows);
                printReleasesAttachments(component, csvRows);
            }
        }
        ByteArrayInputStream byteArrayInputStream = CSVExport
                .createCSV(ComponentAttachmentCSVRecord.getCSVHeaderIterable(), csvRows);
        String filename = String.format("AttachmentInfo_%s.csv", SW360Utils.getCreatedOn());
        response.setHeader(CONTENT_DISPOSITION, String.format("Attachment; filename=\"%s\"", filename));
        FileCopyUtils.copy(byteArrayInputStream, response.getOutputStream());
    }

    private void printReleasesAttachments(Component component, List<Iterable<String>> csvRows) {
        final List<Release> releases = component.getReleases();
        if (releases != null && !releases.isEmpty()) {
            for (Release release : releases) {
                printReleaseAttachments(release, csvRows);
            }
        }
    }

    private void printReleaseAttachments(Release release, List<Iterable<String>> csvRows) {
        final Set<Attachment> attachments = release.getAttachments();
        printAttachments(attachments, csvRows, builder -> builder.fill(release));

    }

    private void printComponentAttachments(Component component, List<Iterable<String>> csvRows) {
        final Set<Attachment> attachments = component.getAttachments();
        printAttachments(attachments, csvRows, builder -> builder.fill(component));

    }

    private void printAttachments(Set<Attachment> attachments, List<Iterable<String>> csvRows,
            Consumer<ComponentAttachmentCSVRecordBuilder> containingObjectPrinter) {
        if (attachments != null && !attachments.isEmpty()) {
            for (Attachment attachment : attachments) {
                final ComponentAttachmentCSVRecordBuilder componentAttachmentCSVRecordBuilder = ComponentAttachmentCSVRecord
                        .builder();
                containingObjectPrinter.accept(componentAttachmentCSVRecordBuilder);
                componentAttachmentCSVRecordBuilder.fill(attachment);
                csvRows.add(componentAttachmentCSVRecordBuilder.build().getCSVIterable());
            }
        }
    }

    private List<Component> getComponentDetailedSummaryForExport() {
        final ComponentService.Iface componentClient = getThriftComponentClient();

        final List<Component> componentDetailedSummaryForExport;
        try {
            componentDetailedSummaryForExport = componentClient.getComponentDetailedSummaryForExport();
        } catch (TException e) {
            log.error("Problem fetching components", e);
            return null;
        }

        return componentDetailedSummaryForExport;
    }

    private Iface getThriftComponentClient() {
        return new ThriftClients().makeComponentClient();
    }

    public void getDownloadReleaseSample(User sw360User, HttpServletResponse response) throws TException, IOException {
        if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
            throw new AccessDeniedException("User is not admin");
        }
        final Iterable<String> csvHeaderIterable = ReleaseLinkCSVRecord.getCSVHeaderIterable();
        final Iterable<Iterable<String>> inputIterable = ImmutableList
                .of(ReleaseLinkCSVRecord.getSampleInputIterable());
        ByteArrayInputStream byteArrayInputStream = CSVExport.createCSV(csvHeaderIterable, inputIterable);
        String filename = String.format("ReleaseLinkInfo_Sample_%s.csv", SW360Utils.getCreatedOn());
        response.setHeader(CONTENT_DISPOSITION, String.format("Release; filename=\"%s\"", filename));
        FileCopyUtils.copy(byteArrayInputStream, response.getOutputStream());
    }

    public void getDownloadReleaseLink(User sw360User, HttpServletResponse response) throws TException, IOException {
        if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
            throw new AccessDeniedException("User is not admin");
        }
        List<Iterable<String>> csvRows = new ArrayList<>();
        final List<Component> componentDetailedSummaryForExport = getComponentDetailedSummaryForExport();
        if (componentDetailedSummaryForExport != null) {
            final Map<String, Component> componentsById = ThriftUtils.getIdMap(componentDetailedSummaryForExport);
            final Map<String, Release> releasesById = getReleasesById(componentDetailedSummaryForExport);

            for (Component component : componentDetailedSummaryForExport) {
                dealWithReleaseLinksContainedInComponent(componentsById, releasesById, component, csvRows);
            }
        }

        ByteArrayInputStream byteArrayInputStream = CSVExport.createCSV(ReleaseLinkCSVRecord.getCSVHeaderIterable(), csvRows);
        String filename = String.format("ReleaseLinkInfo_%s.csv", SW360Utils.getCreatedOn());
        response.setHeader(CONTENT_DISPOSITION, String.format("Release; filename=\"%s\"", filename));
        FileCopyUtils.copy(byteArrayInputStream, response.getOutputStream());
    }

    private void dealWithReleaseLinksContainedInComponent(Map<String, Component> componentsById,
            Map<String, Release> releasesById, Component component, List<Iterable<String>> csvRows) {
        final List<Release> releases = component.getReleases();
        if (releases != null && !releases.isEmpty()) {
            for (Release release : releases) {
                dealWithReleaseLinksContainedInRelease(componentsById, releasesById, component, release, csvRows);
            }
        }
    }

    private void dealWithReleaseLinksContainedInRelease(Map<String, Component> componentsById,
            Map<String, Release> releasesById, Component component, Release release, List<Iterable<String>> csvRows) {
        final Map<String, ReleaseRelationship> releaseIdToRelationship = release.getReleaseIdToRelationship();
        if (releaseIdToRelationship != null) {
            for (Map.Entry<String, ReleaseRelationship> idReleaseRelationshipEntry : releaseIdToRelationship
                    .entrySet()) {
                final Release linkedRelease = releasesById.get(idReleaseRelationshipEntry.getKey());
                if (linkedRelease != null) {
                    final ReleaseRelationship relationship = idReleaseRelationshipEntry.getValue();
                    final Component linkedComponent = componentsById.get(linkedRelease.getComponentId());
                    if (linkedComponent != null) {
                        printReleaseLinkEntry(component, release, linkedRelease, relationship, linkedComponent,
                                csvRows);
                    }
                }
            }
        }
    }

    private void printReleaseLinkEntry(Component component, Release release, Release linkedRelease,
            ReleaseRelationship relationship, Component linkedComponent, List<Iterable<String>> csvRows) {
        final ReleaseLinkCSVRecordBuilder releaseLinkCSVRecordBuilder = ReleaseLinkCSVRecord.builder();
        releaseLinkCSVRecordBuilder.fill(component);
        releaseLinkCSVRecordBuilder.fill(release);
        releaseLinkCSVRecordBuilder.fillLinking(linkedRelease);
        releaseLinkCSVRecordBuilder.fillLinking(linkedComponent);
        releaseLinkCSVRecordBuilder.setRelationship(relationship);
        csvRows.add(releaseLinkCSVRecordBuilder.build().getCSVIterable());
    }

    public void getComponentDetailedExport(User sw360User, HttpServletResponse response) throws TException, IOException {
        if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
            throw new AccessDeniedException("User is not admin");
        }

        final Iterable<String> csvHeaderIterable = ComponentCSVRecord.getCSVHeaderIterable();
        final List<Component> componentDetailedSummaryForExport = getComponentDetailedSummaryForExport();
        List<Iterable<String>> csvRows = getFlattenedView(componentDetailedSummaryForExport);

        ByteArrayInputStream byteArrayInputStream = CSVExport.createCSV(csvHeaderIterable, csvRows);
        String filename = String.format("ComponentsReleasesVendors_%s.csv", SW360Utils.getCreatedOn());
        response.setHeader(CONTENT_DISPOSITION, String.format("Components; filename=\"%s\"", filename));
        FileCopyUtils.copy(byteArrayInputStream, response.getOutputStream());
    }
}
