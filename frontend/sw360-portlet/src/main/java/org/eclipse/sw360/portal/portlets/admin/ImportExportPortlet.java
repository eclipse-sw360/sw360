/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 * With modifications by Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.admin;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.kernel.portlet.PortletResponseUtil;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.PortalUtil;

import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentService;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.eclipse.sw360.exporter.CSVExport;
import org.eclipse.sw360.importer.*;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.UsedAsLiferayAction;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;

import javax.portlet.*;

import static org.eclipse.sw360.datahandler.common.ImportCSV.readAsCSVRecords;
import static org.eclipse.sw360.importer.ComponentImportUtils.*;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
            "/org/eclipse/sw360/portal/portlets/base.properties",
            "/org/eclipse/sw360/portal/portlets/admin.properties"
    },
    property = {
        "javax.portlet.name=" + PortalConstants.IMPORT_EXPORT_PORTLET_NAME,

        "javax.portlet.display-name=Import & Export",
        "javax.portlet.info.short-title=Import & Export",
        "javax.portlet.info.title=Import & Export",

        "javax.portlet.init-param.view-template=/html/admin/importexport/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class ImportExportPortlet extends Sw360Portlet {
    private static final Logger log = Logger.getLogger(ImportExportPortlet.class);

    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String action = request.getParameter(PortalConstants.ACTION);

        if (action == null) {
            log.error("Invalid action 'null'");
            return;
        }

        switch (action) {
            case PortalConstants.DOWNLOAD:
                try {
                    backUpComponents(request, response);
                } catch (IOException e) {
                    log.error("Something went wrong with the user backup", e);
                }
                break;
            case PortalConstants.DOWNLOAD_SAMPLE:
                try {
                    generateSampleFile(request, response);
                } catch (IOException e) {
                    log.error("Something went wrong with the CSV creation", e);
                }
                break;
            case PortalConstants.DOWNLOAD_SAMPLE_ATTACHMENT_INFO:
                    try {
                        generateSampleAttachmentsFile(request, response);
                    } catch (IOException e) {
                        log.error("Something went wrong with the CSV creation", e);
                    }
                break;
            case PortalConstants.DOWNLOAD_ATTACHMENT_INFO:
                try {
                    generateAttachmentsFile(request, response);
                } catch (IOException e) {
                    log.error("Something went wrong with the CSV creation", e);
                }
                break;
            case PortalConstants.DOWNLOAD_RELEASE_LINK_INFO:
                try {
                    generateReleaseLinksFile(request, response);
                } catch (IOException e) {
                    log.error("Something went wrong with the CSV creation", e);
                }
                break;
            case PortalConstants.DOWNLOAD_SAMPLE_RELEASE_LINK_INFO:
                try {
                    generateSampleReleaseLinksFile(request, response);
                } catch (IOException e) {
                    log.error("Something went wrong with the CSV creation", e);
                }
                break;
        }
    }

    private void generateSampleReleaseLinksFile(ResourceRequest request, ResourceResponse response) throws IOException {
        final Iterable<String> csvHeaderIterable = ReleaseLinkCSVRecord.getCSVHeaderIterable();
        final Iterable<Iterable<String>> inputIterable = ImmutableList.of(ReleaseLinkCSVRecord.getSampleInputIterable());
        ByteArrayInputStream byteArrayInputStream = CSVExport.createCSV(csvHeaderIterable, inputIterable);
        PortletResponseUtil.sendFile(request, response, "ReleaseLinkInfo_Sample.csv", byteArrayInputStream, "text/csv");
    }

    private void generateReleaseLinksFile(ResourceRequest request, ResourceResponse response) throws IOException {

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
        PortletResponseUtil.sendFile(request, response, "ReleaseLinkInfo.csv", byteArrayInputStream, "text/csv");
    }

    private void dealWithReleaseLinksContainedInComponent(Map<String, Component> componentsById,
                                                          Map<String, Release> releasesById, Component component, List<Iterable<String>> csvRows) throws IOException {
        final List<Release> releases = component.getReleases();
        if (releases != null && !releases.isEmpty()) {
            for (Release release : releases) {
                dealWithReleaseLinksContainedInRelease(componentsById, releasesById, component, release, csvRows);
            }
        }
    }

    private void dealWithReleaseLinksContainedInRelease(Map<String, Component> componentsById, Map<String, Release> releasesById,
                                                        Component component, Release release, List<Iterable<String>> csvRows) throws IOException {
        final Map<String, ReleaseRelationship> releaseIdToRelationship = release.getReleaseIdToRelationship();
        if (releaseIdToRelationship != null) {
            for (Map.Entry<String, ReleaseRelationship> idReleaseRelationshipEntry : releaseIdToRelationship.entrySet()) {
                final Release linkedRelease = releasesById.get(idReleaseRelationshipEntry.getKey());
                if (linkedRelease != null) {
                    final ReleaseRelationship relationship = idReleaseRelationshipEntry.getValue();
                    final Component linkedComponent = componentsById.get(linkedRelease.getComponentId());
                    if (linkedComponent != null) {
                        printReleaseLinkEntry(component, release, linkedRelease, relationship, linkedComponent, csvRows);
                    }
                }
            }
        }
    }

    private void printReleaseLinkEntry(Component component, Release release, Release linkedRelease,
                                       ReleaseRelationship relationship, Component linkedComponent, List<Iterable<String>> csvRows) throws IOException {
        final ReleaseLinkCSVRecordBuilder releaseLinkCSVRecordBuilder = ReleaseLinkCSVRecord.builder();
        releaseLinkCSVRecordBuilder.fill(component);
        releaseLinkCSVRecordBuilder.fill(release);
        releaseLinkCSVRecordBuilder.fillLinking(linkedRelease);
        releaseLinkCSVRecordBuilder.fillLinking(linkedComponent);
        releaseLinkCSVRecordBuilder.setRelationship(relationship);
        csvRows.add(releaseLinkCSVRecordBuilder.build().getCSVIterable());
    }

    private void generateAttachmentsFile(ResourceRequest request, ResourceResponse response) throws IOException {
        List<Iterable<String>> csvRows = new ArrayList<>();
        final List<Component> componentDetailedSummaryForExport = getComponentDetailedSummaryForExport();
        if (componentDetailedSummaryForExport != null) {
            for (Component component : componentDetailedSummaryForExport) {
                printComponentAttachments(component, csvRows);
                printReleasesAttachments(component, csvRows);
            }
        }

        ByteArrayInputStream byteArrayInputStream = CSVExport.createCSV(ComponentAttachmentCSVRecord.getCSVHeaderIterable(), csvRows);
        PortletResponseUtil.sendFile(request, response, "AttachmentInfo.csv", byteArrayInputStream, "text/csv");
    }

    private void printReleasesAttachments(Component component, List<Iterable<String>> csvRows) throws IOException {
        final List<Release> releases = component.getReleases();
        if (releases != null && !releases.isEmpty()) {
            for (Release release : releases) {
                printReleaseAttachments(release, csvRows);
            }
        }
    }

    private void printComponentAttachments(Component component, List<Iterable<String>> csvRows) throws IOException {
        final Set<Attachment> attachments = component.getAttachments();

        printAttachments(attachments, csvRows, builder -> builder.fill(component));
    }

    private void printReleaseAttachments(Release release, List<Iterable<String>> csvRows) throws IOException {
        final Set<Attachment> attachments = release.getAttachments();

        printAttachments(attachments, csvRows, builder -> builder.fill(release));
    }

    private void printAttachments(Set<Attachment> attachments, List<Iterable<String>> csvRows, Consumer<ComponentAttachmentCSVRecordBuilder> containingObjectPrinter) {
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

    public List<Component> getComponentDetailedSummaryForExport() {

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

    private void generateSampleAttachmentsFile(ResourceRequest request, ResourceResponse response) throws IOException {
        final Iterable<String> csvHeaderIterable = ComponentAttachmentCSVRecord.getCSVHeaderIterable();
        final Iterable<Iterable<String>> inputIterable = ImmutableList.of(ComponentAttachmentCSVRecord.getSampleInputIterable());

        ByteArrayInputStream byteArrayInputStream = CSVExport.createCSV(csvHeaderIterable, inputIterable);
        PortletResponseUtil.sendFile(request, response, "AttachmentInfo_Sample.csv", byteArrayInputStream, "text/csv");
    }

    public void generateSampleFile(ResourceRequest request, ResourceResponse response) throws IOException {
        final Iterable<Iterable<String>> inputIterable = ImmutableList.of(ComponentCSVRecord.getSampleInputIterable());
        final Iterable<String> csvHeaderIterable = ComponentCSVRecord.getCSVHeaderIterable();

        ByteArrayInputStream byteArrayInputStream = CSVExport.createCSV(csvHeaderIterable, inputIterable);
        PortletResponseUtil.sendFile(request, response, "ComponentsReleasesVendorsSample.csv", byteArrayInputStream, "text/csv");
    }

    public void backUpComponents(ResourceRequest request, ResourceResponse response) throws IOException {
        final Iterable<String> csvHeaderIterable = ComponentCSVRecord.getCSVHeaderIterable();
        final List<Component> componentDetailedSummaryForExport = getComponentDetailedSummaryForExport();
        List<Iterable<String>> csvRows = getFlattenedView(componentDetailedSummaryForExport);

        ByteArrayInputStream byteArrayInputStream = CSVExport.createCSV(csvHeaderIterable, csvRows);
        PortletResponseUtil.sendFile(request, response, "ComponentsReleasesVendors.csv", byteArrayInputStream, "text/csv");
    }

    @UsedAsLiferayAction
    public void updateComponents(ActionRequest request, ActionResponse response) throws PortletException, IOException, TException {
        List<CSVRecord> releaseRecords = getCSVFromRequest(request, "file");
        FluentIterable<ComponentCSVRecord> compCSVRecords = convertCSVRecordsToCompCSVRecords(releaseRecords);
        log.trace("read records <" + Joiner.on("\n").join(compCSVRecords) + ">");

        final ComponentService.Iface componentClient = thriftClients.makeComponentClient();
        final VendorService.Iface vendorClient = thriftClients.makeVendorClient();
        final AttachmentService.Iface attachmentClient = thriftClients.makeAttachmentClient();

        User user = UserCacheHolder.getUserFromRequest(request);
        final RequestSummary requestSummary = writeToDatabase(compCSVRecords, componentClient, vendorClient, attachmentClient, user);
        renderRequestSummary(request, response, requestSummary);
    }

    private List<CSVRecord> getCSVFromRequest(PortletRequest request, String fileUploadFormId) throws IOException, TException {
        final InputStream stream = getInputStreamFromRequest(request, fileUploadFormId);
        return readAsCSVRecords(stream);
    }

    private InputStream getInputStreamFromRequest(PortletRequest request, String fileUploadFormId) throws IOException {
        final UploadPortletRequest uploadPortletRequest = PortalUtil.getUploadPortletRequest(request);
        return uploadPortletRequest.getFileAsStream(fileUploadFormId);
    }

    @UsedAsLiferayAction
    public void updateComponentAttachments(ActionRequest request, ActionResponse response) throws PortletException, IOException, TException {
        List<CSVRecord> attachmentRecords = getCSVFromRequest(request, "file");
        FluentIterable<ComponentAttachmentCSVRecord> compCSVRecords = convertCSVRecordsToComponentAttachmentCSVRecords(attachmentRecords);
        log.trace("read records <" + Joiner.on("\n").join(compCSVRecords) + ">");

        final ComponentService.Iface componentClient = thriftClients.makeComponentClient();
        final AttachmentService.Iface attachmentClient = thriftClients.makeAttachmentClient();
        User user = UserCacheHolder.getUserFromRequest(request);
        final RequestSummary requestSummary = writeAttachmentsToDatabase(compCSVRecords, user, componentClient, attachmentClient);
        renderRequestSummary(request, response, requestSummary);
    }

    @UsedAsLiferayAction
    public void updateReleaseLinks(ActionRequest request, ActionResponse response) throws PortletException, IOException, TException {
        List<CSVRecord> releaseLinkRecords = getCSVFromRequest(request, "file");
        FluentIterable<ReleaseLinkCSVRecord> csvRecords = convertCSVRecordsToReleaseLinkCSVRecords(releaseLinkRecords);
        log.trace("read records <" + Joiner.on("\n").join(csvRecords) + ">");

        final ComponentService.Iface componentClient = thriftClients.makeComponentClient();

        User user = UserCacheHolder.getUserFromRequest(request);
        final RequestSummary requestSummary = writeReleaseLinksToDatabase(csvRecords, componentClient, user);

        renderRequestSummary(request, response, requestSummary);
    }
}
