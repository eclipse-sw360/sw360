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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;
import com.liferay.portal.kernel.portlet.PortletResponseUtil;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.util.PortalUtil;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.commonIO.ConvertRecord;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentService;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenses.*;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.eclipse.sw360.exporter.CSVExport;
import org.eclipse.sw360.exporter.ZipTools;
import org.eclipse.sw360.importer.*;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.UsedAsLiferayAction;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.jetbrains.annotations.NotNull;

import javax.portlet.*;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.ZipOutputStream;

import static org.eclipse.sw360.commonIO.ConvertRecord.*;
import static org.eclipse.sw360.commonIO.TypeMappings.*;
import static org.eclipse.sw360.datahandler.common.ImportCSV.readAsCSVRecords;
import static org.eclipse.sw360.exporter.ZipTools.*;
import static org.eclipse.sw360.importer.ComponentImportUtils.*;

/**
 * @author daniele.fognini@tngtech.com
 * @author johannes.najjar@tngtech.com
 */
public class ComponentUploadPortlet extends Sw360Portlet {
    private static final Logger log = Logger.getLogger(ComponentUploadPortlet.class);

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
            case PortalConstants.DOWNLOAD_LICENSE_BACKUP:
                try {
                    backUpLicenses(request, response);
                } catch (IOException | TException e) {
                    log.error("Something went wrong with the license zip creation", e);
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

    public void backUpLicenses(ResourceRequest request, ResourceResponse response) throws IOException, TException {
        Map<String, InputStream> fileNameToStreams = getFilenameToCSVStreams();

        final ByteArrayOutputStream outB = new ByteArrayOutputStream();
        final ZipOutputStream zipOutputStream = new ZipOutputStream(outB);

        for (Map.Entry<String, InputStream> entry : fileNameToStreams.entrySet()) {
            ZipTools.addToZip(zipOutputStream, entry.getKey(), entry.getValue());
        }

        zipOutputStream.flush();
        zipOutputStream.close(); // this closes outB

        final ByteArrayInputStream zipFile = new ByteArrayInputStream(outB.toByteArray());
        PortletResponseUtil.sendFile(request, response, "LicensesBackup.lics", zipFile, "application/zip");
    }

    @NotNull
    private Map<String, InputStream> getFilenameToCSVStreams() throws TException, IOException {
        Map<String, InputStream> fileNameToStreams = new HashMap<>();

        final LicenseService.Iface client = thriftClients.makeLicenseClient();

        fileNameToStreams.put(RISK_CATEGORY_FILE, getCsvStream(serialize(client.getRiskCategories(), riskCategorySerializer())));

        fileNameToStreams.put(ZipTools.RISK_FILE, getCsvStream(serialize(client.getRisks(), riskSerializer())));
        fileNameToStreams.put(ZipTools.OBLIGATION_FILE, getCsvStream(serialize(client.getObligations(), obligationSerializer())));

        final List<Todo> todos = client.getTodos();
        List<PropertyWithValueAndId> customProperties = new ArrayList<>();
        SetMultimap<Integer, Integer> todoCustomPropertyMap = HashMultimap.create();
        ConvertRecord.fillTodoCustomPropertyInfo(todos, customProperties, todoCustomPropertyMap);
        fileNameToStreams.put(ZipTools.TODO_CUSTOM_PROPERTIES_FILE, getCsvStream(serialize(todoCustomPropertyMap, ImmutableList.of("T_ID", "P_ID"))));
        fileNameToStreams.put(ZipTools.CUSTOM_PROPERTIES_FILE, getCsvStream(serialize(customProperties, customPropertiesSerializer())));
        fileNameToStreams.put(ZipTools.OBLIGATION_TODO_FILE, getCsvStream(serialize(getTodoToObligationMap(todos), ImmutableList.of("O_ID", "T_ID"))));
        fileNameToStreams.put(ZipTools.TODO_FILE, getCsvStream(serialize(todos, todoSerializer())));

        fileNameToStreams.put(ZipTools.LICENSETYPE_FILE, getCsvStream(serialize(client.getLicenseTypes(), licenseTypeSerializer())));

        final List<License> licenses = client.getLicenses();
        fileNameToStreams.put(ZipTools.LICENSE_TODO_FILE, getCsvStream(serialize(getLicenseToTodoMap(licenses), ImmutableList.of("Identifier", "ID"))));
        fileNameToStreams.put(ZipTools.LICENSE_RISK_FILE, getCsvStream(serialize(getLicenseToRiskMap(licenses), ImmutableList.of("Identifier", "ID"))));
        fileNameToStreams.put(ZipTools.LICENSE_FILE, getCsvStream(serialize(licenses, licenseSerializer())));
        return fileNameToStreams;
    }

    @NotNull
    private ByteArrayOutputStream writeCsvStream(List<List<String>> listList) throws TException, IOException {
        final ByteArrayOutputStream riskCategoryCsvStream = new ByteArrayOutputStream();
        Writer out = new BufferedWriter(new OutputStreamWriter(riskCategoryCsvStream));
        CSVPrinter csvPrinter = new CSVPrinter(out, CommonUtils.sw360CsvFormat);
        csvPrinter.printRecords(listList);
        csvPrinter.flush();
        csvPrinter.close();
        return riskCategoryCsvStream;
    }

    private ByteArrayInputStream getCsvStream(List<List<String>> listList) throws TException, IOException {
        return new ByteArrayInputStream(writeCsvStream(listList).toByteArray());
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

    @UsedAsLiferayAction
    public void updateLicenses(ActionRequest request, ActionResponse response) throws PortletException, IOException, TException {
        final HashMap<String, InputStream> inputMap = new HashMap<>();
        User user = UserCacheHolder.getUserFromRequest(request);
        try {
            fillFilenameInputStreamMap(request, inputMap);
            if (ZipTools.isValidLicenseArchive(inputMap)) {

                final LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();

                log.debug("Parsing risk categories ...");
                Map<Integer, RiskCategory> riskCategoryMap = getIdentifierToTypeMapAndWriteMissingToDatabase(licenseClient,
                        inputMap.get(RISK_CATEGORY_FILE), RiskCategory.class, Integer.class, user);

                log.debug("Parsing risks ...");
                Map<Integer, Risk> riskMap = getIntegerRiskMap(licenseClient, riskCategoryMap, inputMap.get(RISK_FILE), user);

                log.debug("Parsing obligations ...");
                Map<Integer, Obligation> obligationMap = getIdentifierToTypeMapAndWriteMissingToDatabase(licenseClient,
                        inputMap.get(OBLIGATION_FILE), Obligation.class, Integer.class, user);

                log.debug("Parsing obligation todos ...");
                List<CSVRecord> obligationTodoRecords = readAsCSVRecords(inputMap.get(OBLIGATION_TODO_FILE));
                Map<Integer, Set<Integer>> obligationTodoMapping = convertRelationalTableWithIntegerKeys(obligationTodoRecords);

                log.debug("Parsing license types ...");
                Map<Integer, LicenseType> licenseTypeMap = getIdentifierToTypeMapAndWriteMissingToDatabase(licenseClient,
                        inputMap.get(LICENSETYPE_FILE), LicenseType.class, Integer.class, user);

                log.debug("Parsing todos ...");
                Map<Integer, Todo> todoMap = getTodoMapAndWriteMissingToDatabase(licenseClient, obligationMap, obligationTodoMapping, inputMap.get(TODO_FILE), user);

                if(inputMap.containsKey(CUSTOM_PROPERTIES_FILE)) {
                    log.debug("Parsing custom properties ...");
                    Map<Integer, PropertyWithValue> customPropertiesMap =
                            getCustomPropertiesWithValuesByIdAndWriteMissingToDatabase(licenseClient, inputMap.get(CUSTOM_PROPERTIES_FILE), user);

                    log.debug("Parsing todo custom properties relation ...");
                    List<CSVRecord> todoPropertiesRecord = readAsCSVRecords(inputMap.get(TODO_CUSTOM_PROPERTIES_FILE));
                    Map<Integer, Set<Integer>> todoPropertiesMap = convertRelationalTableWithIntegerKeys(todoPropertiesRecord);

                    todoMap = updateTodoMapWithCustomPropertiesAndWriteToDatabase(licenseClient, todoMap, customPropertiesMap, todoPropertiesMap, user);
                }

                log.debug("Parsing license todos ...");
                List<CSVRecord> licenseTodoRecord = readAsCSVRecords(inputMap.get(LICENSE_TODO_FILE));
                Map<String, Set<Integer>> licenseTodoMap = convertRelationalTable(licenseTodoRecord);


                log.debug("Parsing license risks ...");
                List<CSVRecord> licenseRiskRecord = readAsCSVRecords(inputMap.get(LICENSE_RISK_FILE));
                Map<String, Set<Integer>> licenseRiskMap = convertRelationalTable(licenseRiskRecord);

                log.debug("Parsing licenses ...");
                List<CSVRecord> licenseRecord = readAsCSVRecords(inputMap.get(LICENSE_FILE));

                final List<License> licensesToAdd = ConvertRecord.fillLicenses(licenseRecord, licenseTypeMap, todoMap, riskMap, licenseTodoMap, licenseRiskMap);
                addLicenses(licenseClient, licensesToAdd, log, user);

            } else {
                throw new SW360Exception("Invalid file format");
            }
        } finally {
            for (InputStream inputStream : inputMap.values()) {
                inputStream.close();
            }
        }
    }

    private void fillFilenameInputStreamMap(ActionRequest request, HashMap<String, InputStream> fileNameToStream) throws IOException {
        InputStream in = null;
        try {
            in = getInputStreamFromRequest(request, "file");
            ZipTools.extractZipToInputStreamMap(in, fileNameToStream);
        } finally {
            if (in != null) in.close();
        }
    }
}
