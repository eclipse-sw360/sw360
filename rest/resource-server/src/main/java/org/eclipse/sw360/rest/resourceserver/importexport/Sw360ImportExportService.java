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
import static org.eclipse.sw360.datahandler.common.ImportCSV.readAsCSVRecords;
import static org.eclipse.sw360.importer.ComponentImportUtils.convertCSVRecordsToCompCSVRecords;
import static org.eclipse.sw360.importer.ComponentImportUtils.convertCSVRecordsToComponentAttachmentCSVRecords;
import static org.eclipse.sw360.importer.ComponentImportUtils.convertCSVRecordsToReleaseLinkCSVRecords;
import static org.eclipse.sw360.importer.ComponentImportUtils.writeAttachmentsToDatabase;
import static org.eclipse.sw360.importer.ComponentImportUtils.writeReleaseLinksToDatabase;
import static org.eclipse.sw360.importer.ComponentImportUtils.writeToDatabase;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.RequestContext;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentService;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService.Iface;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.eclipse.sw360.exporter.CSVExport;
import org.eclipse.sw360.importer.ComponentAttachmentCSVRecord;
import org.eclipse.sw360.importer.ComponentAttachmentCSVRecordBuilder;
import org.eclipse.sw360.importer.ComponentCSVRecord;
import org.eclipse.sw360.importer.ReleaseLinkCSVRecord;
import org.eclipse.sw360.importer.ReleaseLinkCSVRecordBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360ImportExportService {
    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private static final String CONTENT_DISPOSITION = "Content-Disposition";
    ThriftClients thriftClients = new ThriftClients();

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

    public void getDownloadAttachmentInfo(User sw360User, HttpServletResponse response) throws IOException, TTransportException {
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

    private List<Component> getComponentDetailedSummaryForExport() throws TTransportException{
        final ComponentService.Iface componentClient = thriftClients.makeComponentClient();

        final List<Component> componentDetailedSummaryForExport;
        try {
            componentDetailedSummaryForExport = componentClient.getComponentDetailedSummaryForExport();
        } catch (TException e) {
            log.error("Problem fetching components", e);
            return null;
        }

        return componentDetailedSummaryForExport;
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

        ByteArrayInputStream byteArrayInputStream = CSVExport.createCSV(ReleaseLinkCSVRecord.getCSVHeaderIterable(),
                csvRows);
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

    public void getComponentDetailedExport(User sw360User, HttpServletResponse response)
            throws TException, IOException {
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

    @JsonInclude
    public RequestSummary uploadComponent(User sw360User, MultipartFile file, HttpServletRequest request,
            HttpServletResponse response) throws IOException, TException, ServletException {
        if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
            throw new AccessDeniedException("Unable to upload component csv file. User is not admin");
        }
        List<CSVRecord> releaseRecords = getCSVFromRequest(request, "file");
        FluentIterable<ComponentCSVRecord> compCSVRecords = convertCSVRecordsToCompCSVRecords(releaseRecords);
        ComponentService.Iface sw360ComponentClient = thriftClients.makeComponentClient();
        VendorService.Iface sw360VendorClient = thriftClients.makeVendorClient();
        AttachmentService.Iface sw360AttachmentClient = thriftClients.makeAttachmentClient();
        RequestSummary requestSummary = writeToDatabase(compCSVRecords, sw360ComponentClient, sw360VendorClient,
                sw360AttachmentClient, sw360User);
        return requestSummary;
    }

    private List<CSVRecord> getCSVFromRequest(HttpServletRequest request, String fileUploadFormId)
            throws IOException, TException, ServletException {
        final InputStream stream = getInputStreamFromRequest(request, fileUploadFormId);
        return readAsCSVRecords(stream);
    }

    private InputStream getInputStreamFromRequest(HttpServletRequest request, String fileUploadFormId)
            throws IOException, ServletException {
        Collection<Part> parts = request.getParts();

        for (Part part : parts) {
            if (!part.getName().equals(fileUploadFormId)) {
                return part.getInputStream();
            }
        }
        throw new IOException("File not found in the request with the specified field name.");
    }

    public RequestSummary uploadReleaseLink(User sw360User, MultipartFile file, HttpServletRequest request)
            throws IOException, TException, ServletException {
        if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
            throw new AccessDeniedException("Unable to upload component csv file. User is not admin");
        }
        List<CSVRecord> releaseLinkRecords = getCSVFromRequest(request, "file");
        FluentIterable<ReleaseLinkCSVRecord> csvRecords = convertCSVRecordsToReleaseLinkCSVRecords(releaseLinkRecords);
        ComponentService.Iface sw360ComponentClient = thriftClients.makeComponentClient();
        final RequestSummary requestSummary = writeReleaseLinksToDatabase(csvRecords, sw360ComponentClient, sw360User);
        return requestSummary;
    }

    public RequestSummary uploadComponentAttachment(User sw360User, MultipartFile file, HttpServletRequest request)
            throws IOException, TException, ServletException {
        if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
            throw new AccessDeniedException("Unable to upload component attachment csv file. User is not admin");
        }
        List<CSVRecord> attachmentRecords = getCSVFromRequest(request, "file");
        FluentIterable<ComponentAttachmentCSVRecord> compCSVRecords = convertCSVRecordsToComponentAttachmentCSVRecords(
                attachmentRecords);
        ComponentService.Iface sw360ComponentClient = thriftClients.makeComponentClient();
        AttachmentService.Iface sw360AttachmentClient = thriftClients.makeAttachmentClient();
        final RequestSummary requestSummary = writeAttachmentsToDatabase(compCSVRecords, sw360User,
                sw360ComponentClient, sw360AttachmentClient);
        return requestSummary;
    }
}
