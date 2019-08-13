/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.importer;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentService;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static org.eclipse.sw360.datahandler.common.SW360Utils.printName;

/**
 * component importer utility class
 *
 * @author daniele.fognini@tngtech.com
 * @author johannes.najjar@tngtech.com
 */
public class ComponentImportUtils {

    private static final Logger log = Logger.getLogger(ComponentImportUtils.class);

    private ComponentImportUtils() {
        // Utility class with only static functions
    }

    public static URL getURL(String filename) throws MalformedURLException {
        return new File(System.getProperty("user.dir") + File.separator + filename).toURI().toURL();
    }

    public static RequestSummary writeReleaseLinksToDatabase(FluentIterable<ReleaseLinkCSVRecord> csvRecords, ComponentService.Iface componentClient, User user) throws TException {
        final List<Component> componentDetailedSummaryForExport = componentClient.getComponentDetailedSummaryForExport();
        final Map<String, Release> releasesByIdentifier = getReleasesByIdentifier(componentDetailedSummaryForExport);

        final Set<String> releasesIdentifiersToBeUpdated = new HashSet<>();

        for (ReleaseLinkCSVRecord csvRecord : csvRecords) {
            final String releaseIdentifier = csvRecord.getReleaseIdentifier();
            final String linkedReleaseIdentifier = csvRecord.getLinkedReleaseIdentifier();
            final ReleaseRelationship relationship = csvRecord.getRelationship();
            if (releaseIdentifier != null && linkedReleaseIdentifier != null && relationship != null) {
                final Release release = releasesByIdentifier.get(releaseIdentifier);
                final Release linkedRelease = releasesByIdentifier.get(linkedReleaseIdentifier);
                if (release != null && linkedRelease != null) {
                    final Map<String, ReleaseRelationship> releaseIdToRelationship = getRelationshipMap(release);
                    releaseIdToRelationship.put(linkedRelease.getId(), relationship);
                    release.setReleaseIdToRelationship(releaseIdToRelationship);
                    releasesIdentifiersToBeUpdated.add(releaseIdentifier);
                }
            }
        }

        final HashSet<Release> updatedReleases = getUpdatedReleases(releasesByIdentifier, releasesIdentifiersToBeUpdated);
        return componentClient.updateReleases(updatedReleases, user);
    }

    @NotNull
    private static HashSet<Release> getUpdatedReleases(Map<String, Release> releasesByIdentifier, final Set<String> releasesIdentifiersToBeUpdated) {
        return Sets.newHashSet(Maps.filterKeys(releasesByIdentifier, new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return releasesIdentifiersToBeUpdated.contains(input);
            }
        }).values());
    }


    private static Map<String, ReleaseRelationship> getRelationshipMap(Release release) {
        final Map<String, ReleaseRelationship> releaseIdToRelationship;
        if (release.isSetReleaseIdToRelationship()) {
            releaseIdToRelationship = release.getReleaseIdToRelationship();
        } else {
            releaseIdToRelationship = new HashMap<>();
        }
        return releaseIdToRelationship;
    }

    public static RequestSummary writeAttachmentsToDatabase(FluentIterable<ComponentAttachmentCSVRecord> compCSVRecords,
                                                            User user, ComponentService.Iface componentClient, AttachmentService.Iface attachmentClient) throws TException {

        final List<Component> componentDetailedSummaryForExport = componentClient.getComponentDetailedSummaryForExport();
        final ImmutableMap<String, Component> componentsByName = getComponentsByName(componentDetailedSummaryForExport);
        final Map<String, Release> releasesByIdentifier = getReleasesByIdentifier(componentDetailedSummaryForExport);

        final Set<String> usedAttachmentContentIds = componentClient.getUsedAttachmentContentIds();

        final Set<String> releaseIdentifiersToUpdate = new HashSet<>();
        final Set<String> componentsToUpdate = new HashSet<>();
        final Set<Attachment> attachmentStubsToDelete = new HashSet<>();

        for (ComponentAttachmentCSVRecord compCSVRecord : compCSVRecords) {
            if (compCSVRecord.isSaveableAttachment()) {
                final Attachment attachment = compCSVRecord.getAttachment();

                if (usedAttachmentContentIds.contains(attachment.getAttachmentContentId()))
                    continue;

                if (compCSVRecord.isForComponent()) {
                    final Component component = componentsByName.get(compCSVRecord.getComponentName());
                    if (component != null) {
                        component.addToAttachments(attachment);
                        componentsToUpdate.add(component.getName());
                    }
                } else if (compCSVRecord.isForRelease()) {
                    final Release release = releasesByIdentifier.get(compCSVRecord.getReleaseIdentifier());
                    if (release != null) {

                        attachmentStubsToDelete.addAll(removeAutogeneratedAttachments(attachmentClient, attachment, release));
                        release.addToAttachments(attachment);
                        releaseIdentifiersToUpdate.add(compCSVRecord.getReleaseIdentifier());

                    }
                }
            }
        }

        final HashSet<Release> updatedReleases = getUpdatedReleases(releasesByIdentifier, releaseIdentifiersToUpdate);
        final RequestSummary releaseRequestSummary = componentClient.updateReleases(updatedReleases, user);

        final HashSet<Component> updatedComponents = Sets.newHashSet(Maps.filterKeys(componentsByName, new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return componentsToUpdate.contains(input);
            }
        }).values());

        final RequestSummary componentRequestSummary = componentClient.updateComponents(updatedComponents, user);

        RequestSummary attachmentSummary = null;
        if (!attachmentStubsToDelete.isEmpty()) {
            attachmentSummary = attachmentClient.bulkDelete(Lists.transform(Lists.newArrayList(attachmentStubsToDelete), new Function<Attachment, String>() {
                @Override
                public String apply(Attachment input) {
                    return input.getAttachmentContentId();
                }
            }));
        }

        RequestSummary requestSummary = CommonUtils.addRequestSummaries(releaseRequestSummary, "release",
                componentRequestSummary, "component");

        if (attachmentSummary != null) {
            requestSummary = CommonUtils.addToMessage(requestSummary, attachmentSummary, "attachment deletion");
        }

        return requestSummary;
    }

    private static HashSet<Attachment> removeAutogeneratedAttachments(AttachmentService.Iface attachmentClient /*read value*/,
                                                                      Attachment attachment /*read value*/,
                                                                      Release release /*return value*/) throws TException {
        final HashSet<Attachment> attachmentsToRemove = new HashSet<>();
        if (release.isSetAttachments()) {

            final AttachmentContent attachmentContent
                    = attachmentClient.getAttachmentContent(attachment.getAttachmentContentId());
            final Set<Attachment> attachments = release.getAttachments();

            if (attachmentContent.isSetRemoteUrl()) {
                for (Attachment existentAttachment : attachments) {
                    final AttachmentContent existentAttachmentContent
                            = attachmentClient.getAttachmentContent(existentAttachment.getAttachmentContentId());

                    if (existentAttachmentContent.isSetRemoteUrl()) {
                        if (existentAttachmentContent.getRemoteUrl().equals(attachmentContent.getRemoteUrl())) {
                            attachmentsToRemove.add(existentAttachment);
                        }
                    }
                }
                //This changes the release and this is actually used.
                attachments.removeAll(attachmentsToRemove);
            }
        }
        return attachmentsToRemove;
    }

    @NotNull
    public static Map<String, Release> getReleasesById(List<Component> componentDetailedSummaryForExport) {
        final Map<String, Release> releasesById = new HashMap<>();

        for (Component component : componentDetailedSummaryForExport) {
            final List<Release> releases = component.getReleases();
            if (releases != null && !releases.isEmpty()) {
                releasesById.putAll(ThriftUtils.getIdMap(releases));
            }
        }
        return releasesById;
    }

    @NotNull
    public static Map<String, Release> getReleasesByIdentifier(List<Component> componentDetailedSummaryForExport) {
        final Map<String, Release> releasesByIdentifier = new HashMap<>();

        for (Component component : componentDetailedSummaryForExport) {
            final List<Release> releases = component.getReleases();
            if (releases != null && !releases.isEmpty()) {
                releasesByIdentifier.putAll(getMapEntryReleaseIdentifierToRelease(releases));
            }
        }
        return releasesByIdentifier;
    }

    private static Map<String, Release> getMapEntryReleaseIdentifierToRelease(List<Release> releases) {

        final HashMap<String, Release> mapEntries = new HashMap<>();
        for (Release release : releases) {
            final String releaseIdentifier = printName(release);

            if (releaseIdentifier != null && !mapEntries.containsKey(releaseIdentifier)) {
                mapEntries.put(releaseIdentifier, release);
            }
        }

        return mapEntries;
        /*  This looks nicer but throws if two identifiers are the same.

            return Maps.uniqueIndex(releases, new Function<Release, String>() {
            @Override
            public String apply(Release input) {
                return printName(input);
            }
        });

        */
    }

    private static ImmutableMap<String, Component> getComponentsByName(List<Component> componentDetailedSummaryForExport) {
        return Maps.uniqueIndex(componentDetailedSummaryForExport, new Function<Component, String>() {
            @Override
            public String apply(Component input) {
                return printName(input);
            }
        });
    }

    public static RequestSummary writeToDatabase(Iterable<ComponentCSVRecord> compCSVRecords,
                                                 ComponentService.Iface componentClient, VendorService.Iface vendorClient,
                                                 AttachmentService.Iface attachmentClient, User user) throws TException {

        Map<String, String> vendorNameToVendorId = getVendorNameToId(compCSVRecords, vendorClient);
        log.debug(format("Read vendors: (%d) %s ", vendorNameToVendorId.size(), vendorNameToVendorId));

        final RequestSummary componentRequestSummary = updateComponents(compCSVRecords, componentClient, user);


        Map<String, String> componentNameToId = new HashMap<>();
        final ArrayList<Release> releases = new ArrayList<>();
        for (Component component : componentClient.getComponentDetailedSummaryForExport()) {
            componentNameToId.put(component.getName(), component.getId());
            final List<Release> componentReleases = component.getReleases();
            if (componentReleases != null && componentReleases.size() > 0)
                releases.addAll(componentReleases);
        }
        Set<String> knownReleaseIdentifiers = Sets.newHashSet(getReleaseIdentifiers(releases));


        List<ComponentCSVRecord> relevantCSVRecords = new ArrayList<>();
        final HashMap<String, List<String>> releaseIdentifierToDownloadURL = new HashMap<>();


        List<AttachmentContent> attachmentContentsToUpdate = new ArrayList<>();

        filterRelevantCSVRecordsAndGetAttachmentContents(compCSVRecords, componentNameToId, knownReleaseIdentifiers, relevantCSVRecords,
                releaseIdentifierToDownloadURL, attachmentContentsToUpdate);

        attachmentContentsToUpdate = attachmentClient.makeAttachmentContents(attachmentContentsToUpdate);
        final ImmutableMap<String, AttachmentContent> URLtoAttachment = Maps.uniqueIndex(attachmentContentsToUpdate, new Function<AttachmentContent, String>() {
            @Override
            public String apply(AttachmentContent input) {
                return input.getRemoteUrl();
            }
        });

        Set<Release> releasesToUpdate = new HashSet<>();


        //I do not need so many checks here because I only iterate over the relevant CSV records
        for (ComponentCSVRecord componentCSVRecord : relevantCSVRecords) {
            String releaseIdentifier = componentCSVRecord.getReleaseIdentifier();
            String vendorName = componentCSVRecord.getVendorName();
            String vendorId = vendorNameToVendorId.get(vendorName);

            String componentId = componentNameToId.get(componentCSVRecord.getComponentName());
            List<AttachmentContent> attachmentContents = getAttachmentContents(releaseIdentifierToDownloadURL, URLtoAttachment, releaseIdentifier);

            Release releaseToAdd = componentCSVRecord.getRelease(vendorId, componentId, attachmentContents);
            knownReleaseIdentifiers.add(releaseIdentifier);
            if (releaseToAdd != null) {
                releasesToUpdate.add(releaseToAdd);
            }

        }

        final RequestSummary releaseRequestSummary = componentClient.updateReleases(releasesToUpdate, user);

        return CommonUtils.addRequestSummaries(componentRequestSummary, "component", releaseRequestSummary, "release");
    }

    @Nullable
    private static List<AttachmentContent> getAttachmentContents(HashMap<String, List<String>> releaseIdentifierToDownloadURL, ImmutableMap<String, AttachmentContent> URLtoAttachment, String releaseIdentifier) {
        List<AttachmentContent> attachmentContents = null;
        if (releaseIdentifierToDownloadURL.containsKey(releaseIdentifier)) {

            final List<String> URLs = releaseIdentifierToDownloadURL.get(releaseIdentifier);
            attachmentContents = new ArrayList<>(URLs.size());
            for (String url : URLs) {
                if (URLtoAttachment.containsKey(url)) {
                    final AttachmentContent attachmentContent = URLtoAttachment.get(url);
                    attachmentContents.add(attachmentContent);
                }
            }
        }
        return attachmentContents;
    }

    private static void filterRelevantCSVRecordsAndGetAttachmentContents(Iterable<ComponentCSVRecord> compCSVRecords, Map<String, String> componentNameToId, Set<String> knownReleaseIdentifiers, List<ComponentCSVRecord> relevantCSVRecords, HashMap<String, List<String>> releaseIdentifierToDownloadURL, List<AttachmentContent> attachmentContentsToUpdate) {
        for (ComponentCSVRecord componentCSVRecord : compCSVRecords) {
            String releaseIdentifier = componentCSVRecord.getReleaseIdentifier();
            if (knownReleaseIdentifiers.contains(releaseIdentifier) || !componentCSVRecord.isSetRelease()) {
                log.debug("skipping existing release " + releaseIdentifier);
            } else {
                String componentId = componentNameToId.get(componentCSVRecord.getComponentName());
                if (!isNullOrEmpty(componentId)) {
                    if (componentCSVRecord.isSetAttachmentContent()) {
                        List<AttachmentContent> attachmentContents = componentCSVRecord.getAttachmentContents();

                        final ImmutableList<String> attachmentURLs = CommonUtils.getAttachmentURLsFromAttachmentContents(attachmentContents);

                        releaseIdentifierToDownloadURL.put(releaseIdentifier, attachmentURLs);

                        attachmentContentsToUpdate.addAll(attachmentContents);
                    }
                    relevantCSVRecords.add(componentCSVRecord);
                    knownReleaseIdentifiers.add(releaseIdentifier);
                } else {
                    log.error("Broken component: " + componentCSVRecord);
                }
            }
        }
    }

    private static RequestSummary updateComponents(Iterable<ComponentCSVRecord> compCSVRecords, ComponentService.Iface componentClient, User user) throws TException {

        Set<String> componentNames = new HashSet<>();

        for (Component component : componentClient.getComponentSummaryForExport()) {
            componentNames.add(component.getName());
        }

        Set<Component> toBeUpdated = new HashSet<>();

        for (ComponentCSVRecord componentCSVRecord : compCSVRecords) {
            if (componentCSVRecord.isSetComponent()) {
                String componentName = componentCSVRecord.getComponentName();
                if (componentNames.add(componentName)) {
                    Component component = componentCSVRecord.getComponent();
                    toBeUpdated.add(component);
                }
            }
        }
        return componentClient.updateComponents(toBeUpdated, user);
    }


    public static FluentIterable<ComponentCSVRecord> convertCSVRecordsToCompCSVRecords(List<CSVRecord> in) {
        return FluentIterable.from(in).transform(new Function<CSVRecord, ComponentCSVRecord>() {
            @Override
            public ComponentCSVRecord apply(CSVRecord input) {
                ComponentCSVRecord componentCSVRecord = null;
                try {
                    componentCSVRecord = new ComponentCSVRecordBuilder(input).build();
                } catch (Exception e) {
                    log.error("Bad record " + input, e);
                }
                return componentCSVRecord;
            }
        }).filter(notNull());
    }

    public static FluentIterable<ComponentAttachmentCSVRecord> convertCSVRecordsToComponentAttachmentCSVRecords(List<CSVRecord> in) {
        return FluentIterable.from(in).transform(new Function<CSVRecord, ComponentAttachmentCSVRecord>() {
            @Override
            public ComponentAttachmentCSVRecord apply(CSVRecord input) {
                ComponentAttachmentCSVRecord componentAttachmentCSVRecord = null;
                try {
                    componentAttachmentCSVRecord = new ComponentAttachmentCSVRecordBuilder(input).build();
                } catch (Exception e) {
                    log.error("Bad record " + input, e);
                }
                return componentAttachmentCSVRecord;
            }
        }).filter(notNull());
    }

    public static FluentIterable<ReleaseLinkCSVRecord> convertCSVRecordsToReleaseLinkCSVRecords(List<CSVRecord> in) {
        return FluentIterable.from(in).transform(new Function<CSVRecord, ReleaseLinkCSVRecord>() {
            @Override
            public ReleaseLinkCSVRecord apply(CSVRecord input) {
                ReleaseLinkCSVRecord releaseLinkCSVRecord = null;
                try {
                    releaseLinkCSVRecord = new ReleaseLinkCSVRecordBuilder(input).build();
                } catch (Exception e) {
                    log.error("Bad record " + input, e);
                }
                return releaseLinkCSVRecord;
            }
        }).filter(notNull());
    }


    @NotNull
    public static Map<String, String> getVendorNameToId(Iterable<ComponentCSVRecord> compCSVRecords,
                                                        VendorService.Iface vendorClient) throws TException {
        Map<String, String> vendorNameToVendorId = getVendorNameToVendorId(vendorClient);


        for (ComponentCSVRecord componentCSVRecord : compCSVRecords) {
            if (componentCSVRecord.isSetVendor()) {
                String vendorName = componentCSVRecord.getVendorName();
                if (!vendorNameToVendorId.containsKey(vendorName)) {
                    Vendor vendor = componentCSVRecord.getVendor();
                    String vendorId = vendorClient.addVendor(vendor);

                    vendorNameToVendorId.put(vendorName, vendorId);
                    log.trace(format("created vendor with name '%s' as %s: %s", vendorName, vendorId, vendor));
                } else {
                    log.trace(format("recognized vendor with name '%s' as %s", vendorName, vendorNameToVendorId.get(vendorName)));
                }
            } else {
                log.info("invalid vendor in record " + componentCSVRecord);
            }
        }

        return vendorNameToVendorId;
    }

    @NotNull
    private static Map<String, String> getVendorNameToVendorId(VendorService.Iface vendorClient) throws TException {
        Map<String, String> vendorNameToVendorId = new HashMap<>();

            for (Vendor vendor : vendorClient.getAllVendors()) {
                if (!vendorNameToVendorId.containsKey(vendor.getShortname())) {
                    vendorNameToVendorId.put(vendor.getShortname(), vendor.getId());
                }
                else {
                    log.error("There is a clash between the shortname " + vendor.getShortname() + " and a name already present in the mapping.");
                }
                if (!vendorNameToVendorId.containsKey(vendor.getFullname())) {
                    vendorNameToVendorId.put(vendor.getFullname(), vendor.getId());
                }
                else {
                    log.error("There is a clash between the vendor fullname " + vendor.getFullname() + " and a name already present in the mapping.");
                }
            }

        return vendorNameToVendorId;
    }

    @NotNull
    public static List<String> getReleaseIdentifiers(List<Release> releaseSummary) throws TException {
        return Lists.transform(releaseSummary, new Function<Release, String>() {
            @Override
            public String apply(Release input) {
                return printName(input);
            }
        });
    }

    @NotNull
    public static List<Iterable<String>> getFlattenedView(List<Component> componentDetailedSummaryForExport) {
        List<Iterable<String>> csvRows = new ArrayList<>();

        Set<String> exportedReleaseIdentifiers = new HashSet<>();
        Set<String> exportedComponentIdentifiers = new HashSet<>();
        if (componentDetailedSummaryForExport != null) {
            for (Component component : componentDetailedSummaryForExport) {
                final List<Release> releases = component.getReleases();
                if (releases != null && releases.size() > 0) {
                    for (Release release : releases) {
                        if (isNotProcessed(exportedReleaseIdentifiers, release)) {
                            final ComponentCSVRecordBuilder componentCSVRecordBuilder = ComponentCSVRecord.builder();
                            componentCSVRecordBuilder.fill(component);
                            componentCSVRecordBuilder.fill(release);
                            csvRows.add(componentCSVRecordBuilder.build().getCSVIterable());
                        }
                    }
                } else {
                    if (isNotProcessed(exportedComponentIdentifiers, component)) {
                        final ComponentCSVRecordBuilder componentCSVRecordBuilder = ComponentCSVRecord.builder();
                        componentCSVRecordBuilder.fill(component);
                        csvRows.add(componentCSVRecordBuilder.build().getCSVIterable());
                    }
                }
            }
        }
        return csvRows;
    }

    public static boolean isNotProcessed(Set<String> exportedComponentIdentifiers, Component component) {
        return exportedComponentIdentifiers.add(printName(component));
    }

    public static boolean isNotProcessed(Set<String> exportedReleaseIdentifiers, Release release) {
        return exportedReleaseIdentifiers.add(printName(release));
    }
}
