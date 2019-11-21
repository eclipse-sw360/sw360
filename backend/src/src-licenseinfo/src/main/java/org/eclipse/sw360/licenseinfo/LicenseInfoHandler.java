/*
 * Copyright Siemens AG, 2016-2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.licenseinfo;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Enums;
import com.google.common.base.Predicate;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.common.WrappedException.WrappedTException;
import org.eclipse.sw360.datahandler.db.AttachmentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.ProjectDatabaseHandler;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.*;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationStatusInfo;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.licenseinfo.outputGenerators.*;
import org.eclipse.sw360.licenseinfo.parsers.*;
import org.eclipse.sw360.licenseinfo.util.LicenseNameWithTextUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import java.net.MalformedURLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.common.WrappedException.wrapTException;
import static org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatVariant.DISCLOSURE;
import static org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatVariant.REPORT;

/**
 * Implementation of the Thrift service
 */
public class LicenseInfoHandler implements LicenseInfoService.Iface {
    private static final Logger LOGGER = Logger.getLogger(LicenseInfoHandler.class);
    private static final int CACHE_TIMEOUT_MINUTES = 15;
    private static final int CACHE_MAX_ITEMS = 100;
    private static final String DEFAULT_LICENSE_INFO_HEADER_FILE = "/DefaultLicenseInfoHeader.txt";
    private static final String DEFAULT_LICENSE_INFO_TEXT = dropCommentedLine(DEFAULT_LICENSE_INFO_HEADER_FILE);
    private static final String DEFAULT_OBLIGATIONS_FILE = "/DefaultObligations.txt";
    private static final String DEFAULT_OBLIGATIONS_TEXT = dropCommentedLine(DEFAULT_OBLIGATIONS_FILE);
    private static final String MSG_NO_RELEASE_GIVEN = "No release given";

    protected List<LicenseInfoParser> parsers;
    protected List<OutputGenerator<?>> outputGenerators;
    protected ComponentDatabaseHandler componentDatabaseHandler;
    protected ProjectDatabaseHandler projectDatabaseHandler;
    protected Cache<String, List<LicenseInfoParsingResult>> licenseInfoCache;
    protected Cache<String, List<ObligationParsingResult>> obligationCache;
    protected Cache<String, LicenseInfoParsingResult> licenseObligationMappingCache;

    public LicenseInfoHandler() throws MalformedURLException {
        this(new AttachmentDatabaseHandler(DatabaseSettings.getConfiguredHttpClient(), DatabaseSettings.COUCH_DB_DATABASE, DatabaseSettings.COUCH_DB_ATTACHMENTS),
                new ComponentDatabaseHandler(DatabaseSettings.getConfiguredHttpClient(), DatabaseSettings.COUCH_DB_DATABASE, DatabaseSettings.COUCH_DB_ATTACHMENTS),
                new ProjectDatabaseHandler(DatabaseSettings.getConfiguredHttpClient(), DatabaseSettings.COUCH_DB_DATABASE, DatabaseSettings.COUCH_DB_ATTACHMENTS));
    }

    @VisibleForTesting
    protected LicenseInfoHandler(AttachmentDatabaseHandler attachmentDatabaseHandler,
                              ComponentDatabaseHandler componentDatabaseHandler, ProjectDatabaseHandler projectDatabaseHandler) throws MalformedURLException {
        this.componentDatabaseHandler = componentDatabaseHandler;
        this.projectDatabaseHandler = projectDatabaseHandler;
        this.licenseInfoCache = CacheBuilder.newBuilder().expireAfterWrite(CACHE_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                .maximumSize(CACHE_MAX_ITEMS).build();
        this.obligationCache = CacheBuilder.newBuilder().expireAfterWrite(CACHE_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                .maximumSize(CACHE_MAX_ITEMS).build();
        this.licenseObligationMappingCache = CacheBuilder.newBuilder().expireAfterWrite(CACHE_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                .maximumSize(CACHE_MAX_ITEMS).build();

        AttachmentContentProvider contentProvider = attachment -> attachmentDatabaseHandler.getAttachmentContent(attachment.getAttachmentContentId());

        // @formatter:off
        parsers = Lists.newArrayList(
            new SPDXParser(attachmentDatabaseHandler.getAttachmentConnector(), contentProvider),
            new CLIParser(attachmentDatabaseHandler.getAttachmentConnector(), contentProvider),
            new CombinedCLIParser(attachmentDatabaseHandler.getAttachmentConnector(), contentProvider, componentDatabaseHandler)
        );

        outputGenerators = Lists.newArrayList(
                new TextGenerator(DISCLOSURE, "License Disclosure as TEXT"),
                new XhtmlGenerator(DISCLOSURE, "License Disclosure as XHTML"),
                new DocxGenerator(DISCLOSURE, "License Disclosure as DOCX"),
                new DocxGenerator(REPORT, "Project Clearing Report as DOCX")
        );
        // @formatter:on
    }

    @Override
    public LicenseInfoFile getLicenseInfoFile(Project project, User user, String outputGenerator,
                                              Map<String, Set<String>> releaseIdsToSelectedAttachmentIds, Map<String, Set<LicenseNameWithText>> excludedLicensesPerAttachment, String externalIds)
            throws TException {
        assertNotNull(project);
        assertNotNull(user);
        assertNotNull(outputGenerator);
        assertNotNull(releaseIdsToSelectedAttachmentIds);
        assertNotNull(excludedLicensesPerAttachment);

        Map<Release, Set<String>> releaseToAttachmentId = mapKeysToReleases(releaseIdsToSelectedAttachmentIds, user);
        Collection<LicenseInfoParsingResult> projectLicenseInfoResults = getAllReleaseLicenseInfos(releaseToAttachmentId, user,
                excludedLicensesPerAttachment);
        Collection<ObligationParsingResult> obligationsResults = getAllReleaseObligations(releaseToAttachmentId, user);

        String[] outputGeneratorClassnameAndVariant = outputGenerator.split("::");
        if (outputGeneratorClassnameAndVariant.length != 2) {
            throw new TException("Unsupported output generator value: " + outputGenerator);
        }

        String outputGeneratorClassName = outputGeneratorClassnameAndVariant[0];
        OutputFormatVariant outputGeneratorVariant = Enums.getIfPresent(OutputFormatVariant.class, outputGeneratorClassnameAndVariant[1]).orNull();
        OutputGenerator<?> generator = getOutputGeneratorByClassnameAndVariant(outputGeneratorClassName, outputGeneratorVariant);
        LicenseInfoFile licenseInfoFile = new LicenseInfoFile();

        licenseInfoFile.setOutputFormatInfo(generator.getOutputFormatInfo());

        fillDefaults(project);

        Map<String,String> filteredExtIdMap = Collections.emptyMap();
        if(!StringUtils.isEmpty(externalIds)) {
            Map<String,String> extIdMap = project.getExternalIds();
            List<String> externalId = Arrays.asList(externalIds.split(","));
            filteredExtIdMap = extIdMap.entrySet().stream().filter(x->externalId.contains(x.getKey())).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        }

        Object output = generator.generateOutputFile(projectLicenseInfoResults, project, obligationsResults, user, filteredExtIdMap);
        if (output instanceof byte[]) {
            licenseInfoFile.setGeneratedOutput((byte[]) output);
        } else if (output instanceof String) {
            licenseInfoFile.setGeneratedOutput(((String) output).getBytes());
        } else {
            throw new TException("Unsupported output generator result: " + output.getClass().getSimpleName());
        }

        return licenseInfoFile;
    }

    private void fillDefaults(Project project) {
        if(!project.isSetLicenseInfoHeaderText()) {
            project.setLicenseInfoHeaderText(getDefaultLicenseInfoHeaderText());
        }
        if(!project.isSetObligationsText()) {
            project.setObligationsText(getDefaultObligationsText());
        }
    }

    @Override
    public List<OutputFormatInfo> getPossibleOutputFormats() {
        return outputGenerators.stream().map(OutputGenerator::getOutputFormatInfo).collect(Collectors.toList());
    }

    @Override
    public OutputFormatInfo getOutputFormatInfoForGeneratorClass(String generatorClassName) throws TException {
        OutputGenerator<?> generator = getOutputGeneratorByClassname(generatorClassName);
        return generator.getOutputFormatInfo();
    }

    @Override
    public List<LicenseInfoParsingResult> getLicenseInfoForAttachment(Release release, String attachmentContentId, User user)
            throws TException {
        if (release == null) {
            return Collections.singletonList(noSourceParsingResult(MSG_NO_RELEASE_GIVEN));
        }

        List<LicenseInfoParsingResult> cachedResults = licenseInfoCache.getIfPresent(attachmentContentId);
        if (cachedResults != null) {
            return cachedResults;
        }

        Attachment attachment = nullToEmptySet(release.getAttachments()).stream()
                .filter(a -> a.getAttachmentContentId().equals(attachmentContentId)).findFirst().orElseThrow(() -> {
                    String message = String.format(
                            "Attachment selected for license info generation is not found in release's attachments. Release id: %s. Attachment content id: %s",
                            release.getId(), attachmentContentId);
                    return new IllegalStateException(message);
                });

        try {

            List<LicenseInfoParser> applicableParsers = parsers.stream()
                    .filter(parser -> wrapTException(() -> parser.isApplicableTo(attachment, user, release))).collect(Collectors.toList());

            if (applicableParsers.size() == 0) {
                LOGGER.warn("No applicable parser has been found for the attachment selected for license information");
                return assignReleaseToLicenseInfoParsingResult(
                        assignFileNameToLicenseInfoParsingResult(
                                noSourceParsingResult("No applicable parser has been found for the attachment"), attachment.getFilename()),
                        release);
            } else if (applicableParsers.size() > 1) {
                LOGGER.info("More than one parser claims to be able to parse attachment with contend id " + attachmentContentId);
            }

            List<LicenseInfoParsingResult> results = applicableParsers.stream()
                    .map(parser -> wrapTException(() -> parser.getLicenseInfos(attachment, user, release))).flatMap(Collection::stream)
                    .collect(Collectors.toList());
            filterEmptyLicenses(results);

            results = assignReleaseToLicenseInfoParsingResults(results, release);
            results = assignComponentToLicenseInfoParsingResults(results, release, user);

            licenseInfoCache.put(attachmentContentId, results);
            return results;
        } catch (WrappedTException exception) {
            throw exception.getCause();
        }
    }

    @Override
    public Map<Project, List<LicenseInfoParsingResult>> setProjectObligationStatus(Project project, List<LicenseInfoParsingResult> licenseResults, Map<String, String> excludedReleaseIdToAcceptedCLI) {

        Map<String, ObligationStatusInfo> obligationStatusMap = project.isSetLinkedObligations() ? removeUnwantedObligations(project, excludedReleaseIdToAcceptedCLI) : Maps.newHashMap();
        Map<Project, List<LicenseInfoParsingResult>> projectObligationMap = Maps.newHashMap();

        // mapping obligations and it's status
        for (LicenseInfoParsingResult result : licenseResults) {
            Release release = result.getRelease();
            LicenseInfo licenseInfo = result.getLicenseInfo();
            licenseInfo.setLicenseNamesWithTexts(filterLicense(licenseInfo));
            for (LicenseNameWithText license : licenseInfo.getLicenseNamesWithTexts()) {
                String licenseName = license.getLicenseName();
                license.getObligations().stream().forEach(obl -> {
                    ObligationStatusInfo posi = obligationStatusMap.get(obl.getTopic());
                    release.setAttachments(release.getAttachments().stream()
                            .filter(a -> a.getAttachmentContentId().equals(result.getAttachmentContentId()))
                            .collect(Collectors.toSet()));
                    if (Objects.nonNull(posi)) {
                        posi.setText(obl.getText());
                        posi.addToLicenseIds(licenseName);
                        posi.addToReleases(release);
                        obl.setObligationStatusInfo(posi);
                    } else {
                        ObligationStatusInfo osi = new ObligationStatusInfo().setText(obl.getText())
                                .setReleases(Sets.newHashSet(release)).setLicenseIds(Sets.newHashSet(licenseName));
                        obligationStatusMap.put(obl.getTopic(), osi);
                    }
                });
            }
        }
        project.setLinkedObligations(obligationStatusMap);
        projectObligationMap.put(project, licenseResults);
        return projectObligationMap;
    }

    private Map<String, ObligationStatusInfo> removeUnwantedObligations(Project project, Map<String, String> excludedReleaseIdToAcceptedCLI) {
        if (!excludedReleaseIdToAcceptedCLI.isEmpty()) {
            Map<String, ObligationStatusInfo> obligationStatusMap = project.getLinkedObligations().entrySet().stream().filter(entry -> {
                ObligationStatusInfo osi = entry.getValue();
                Map<String, String> currentReleaseIdToAcceptedCLI = osi.getReleaseIdToAcceptedCLI();
                if (excludedReleaseIdToAcceptedCLI.equals(currentReleaseIdToAcceptedCLI)) {
                    return false;
                }
                if (osi.isSetReleaseIdToAcceptedCLI()) {
                    currentReleaseIdToAcceptedCLI.keySet().removeAll(excludedReleaseIdToAcceptedCLI.keySet());
                    if (currentReleaseIdToAcceptedCLI.isEmpty()) {
                        return false;
                    }
                }
                return true;
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            projectDatabaseHandler.updateProjectForObligations(project.getId(), obligationStatusMap);
            return obligationStatusMap;
        }
        return project.getLinkedObligations();
    }

    private Set<LicenseNameWithText> filterLicense(LicenseInfo licenseInfo) {
        // filtering all license without obligations and license name unknown or n/a
        Predicate<LicenseNameWithText> filterLicense = license -> (license.isSetObligations()
                && !(SW360Constants.LICENSE_NAME_UNKNOWN.equals(license.getLicenseName())
                        && SW360Constants.NA.equalsIgnoreCase(license.getLicenseName())));

        return licenseInfo.getLicenseNamesWithTexts().stream().filter(filterLicense).map(license -> {
            // changing non-global license type as Others an global to Global
            if (SW360Constants.LICENSE_TYPE_GLOBAL.equalsIgnoreCase(license.getType())) {
                license.setType(SW360Constants.LICENSE_TYPE_GLOBAL);
            } else {
                license.setType(SW360Constants.LICENSE_TYPE_OTHERS);
            }
            return license;
        }).collect(Collectors.toSet());
    }

    @Override
    public LicenseInfoParsingResult createLicenseToObligationMapping(LicenseInfoParsingResult licenseResult, ObligationParsingResult obligationResult) {

        LicenseInfoParsingResult cachedResults = licenseObligationMappingCache.getIfPresent(licenseResult.getAttachmentContentId());
        if (cachedResults != null) {
            return cachedResults;
        }

        Map<String, Set<Obligation>> licenseIdToObligations = obligationResult.getObligations().stream()
                // filtering obligations with unknown topic
                .filter(obligation -> !(SW360Constants.OBLIGATION_TOPIC_UNKNOWN.equals(obligation.getTopic())))
                // sort the obligations by topic in ascending order
                .sorted(Comparator.comparing(Obligation::getTopic, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList()).stream()
                // create a Map<licenseId, Set<Obligation>>
                .flatMap(obligation -> obligation.getLicenseIDs().stream()
                        .map(id -> new AbstractMap.SimpleEntry<>(obligation, id)))
                .collect(Collectors.groupingBy(Map.Entry::getValue,
                        Collectors.mapping(Map.Entry::getKey, Collectors.toSet())));

        LicenseInfo licenseInfo = licenseResult.getLicenseInfo();
        licenseInfo.getLicenseNamesWithTexts()
                .forEach(license -> license.setObligations(licenseIdToObligations.get(license.getLicenseName())));
        licenseInfo.setTotalObligations(obligationResult.getObligationsSize());
        licenseObligationMappingCache.put(licenseResult.getAttachmentContentId(), licenseResult);
        return licenseResult;
    }

    /* TODO: This method will used once project obligations approach is fixed */
    private Map<String, ObligationStatusInfo> createLicenseToObligationMappingForReport(Project project, List<LicenseInfoParsingResult> licenseResults,
            List<ObligationParsingResult> obligationResults, Map<Release, Set<String>> releaseToSelectedAttachmentIds) {

        Set<String> linkedReleaseIds = project.getReleaseIdToUsage().keySet();
        Map<String, String> releaseIdToAcceptedCLI = Maps.newHashMap();

        if (project.getLinkedObligationsSize() > 0) {
            releaseIdToAcceptedCLI
                    .putAll(SW360Utils.getReleaseIdtoAcceptedCLIMappings(project.getLinkedObligations()));
        }

        Map<Release, Set<String>> filteredRelToSelAttIds = releaseToSelectedAttachmentIds.entrySet().stream()
                .filter(e -> linkedReleaseIds.contains(e.getKey().getId()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (filteredRelToSelAttIds.isEmpty()) {
            LOGGER.info("Attachment from linked releases is not selected while downloading the report.");
            return null;
        }

        Map<String, LicenseInfoParsingResult> attachmentToLicenseMap = licenseResults.stream()
                .filter(LicenseInfoParsingResult::isSetAttachmentContentId)
                .filter(LicenseInfoParsingResult::isSetLicenseInfo).collect(Collectors.toList()).stream()
                .collect(Collectors.toMap(LicenseInfoParsingResult::getAttachmentContentId, Function.identity()));

        Map<String, ObligationParsingResult> attachmentToObligationMap = obligationResults.stream()
                .filter(ObligationParsingResult::isSetAttachmentContentId).filter(o -> o.getObligationsSize() > 0)
                .collect(Collectors.toList()).stream()
                .collect(Collectors.toMap(ObligationParsingResult::getAttachmentContentId, Function.identity()));

        List<LicenseInfoParsingResult> licenseParsingResults = new ArrayList<LicenseInfoParsingResult>();


        for (Entry<Release, Set<String>> entry : filteredRelToSelAttIds.entrySet()) {
            List<Attachment> filteredAttachments = SW360Utils.getApprovedClxAttachmentForRelease(entry.getKey());

            if (filteredAttachments.size() == 1) {
                final String approvedAttContentId = filteredAttachments.get(0).getAttachmentContentId();
                final String releaseId = entry.getKey().getId();

                if (releaseIdToAcceptedCLI.containsKey(releaseId)
                        && releaseIdToAcceptedCLI.get(releaseId).equals(approvedAttContentId)) {
                    releaseIdToAcceptedCLI.remove(releaseId);
                }

                for (String attachmentContentId : entry.getValue()) {
                    LicenseInfoParsingResult licenseResult = attachmentToLicenseMap.get(attachmentContentId);
                    ObligationParsingResult obligationResult = attachmentToObligationMap.get(attachmentContentId);
                    if (attachmentContentId.equals(approvedAttContentId)
                            && null != obligationResult && null != licenseResult) {
                        licenseParsingResults.add(createLicenseToObligationMapping(licenseResult, obligationResult));
                    }
                }
            }
        }
        if (licenseParsingResults.isEmpty()) {
            return null;
        }
        return setProjectObligationStatus(project, licenseParsingResults, releaseIdToAcceptedCLI).keySet().iterator().next().getLinkedObligations();
    }

    @Override
    public List<ObligationParsingResult> getObligationsForAttachment(Release release, String attachmentContentId, User user)
            throws TException {
        if (release == null) {
            return Collections.singletonList(new ObligationParsingResult()
                                                    .setStatus(ObligationInfoRequestStatus.NO_APPLICABLE_SOURCE)
                                                    .setMessage(MSG_NO_RELEASE_GIVEN));
        }

        List<ObligationParsingResult> cachedResults = obligationCache.getIfPresent(attachmentContentId);
        if (cachedResults != null) {
            return cachedResults;
        }

        Attachment attachment = nullToEmptySet(release.getAttachments()).stream()
                .filter(a -> a.getAttachmentContentId().equals(attachmentContentId)).findFirst().orElseThrow(() -> {
                    String message = String.format(
                            "Attachment selected for obligations info generation is not found in release's attachments. Release id: %s. Attachment content id: %s",
                            release.getId(), attachmentContentId);
                    return new IllegalStateException(message);
                });

        try {

            List<LicenseInfoParser> applicableParsers = parsers.stream()
                    .filter(parser -> wrapTException(() -> parser.isApplicableTo(attachment, user, release))).collect(Collectors.toList());

            if (applicableParsers.size() == 0) {
                LOGGER.warn("No applicable parser has been found for the attachment selected for license information");
                return Collections.singletonList(new ObligationParsingResult()
                        .setStatus(ObligationInfoRequestStatus.NO_APPLICABLE_SOURCE)
                        .setMessage("No applicable parser has been found for the attachment."));
            } else if (applicableParsers.size() > 1) {
                LOGGER.info("More than one parser claims to be able to parse attachment with contend id " + attachmentContentId);
            }

            List<ObligationParsingResult> results = applicableParsers.stream()
                    .map(parser -> wrapTException(() -> parser.getObligations(attachment, user, release)))
                    .collect(Collectors.toList());

            results = assignReleaseToObligationParsingResults(results, release);

            obligationCache.put(attachmentContentId, results);
            return results;
        } catch (WrappedTException exception) {
            throw exception.getCause();
        }
    }

    private LicenseInfoParsingResult assignFileNameToLicenseInfoParsingResult(LicenseInfoParsingResult licenseInfoParsingResult, String filename) {
        if (licenseInfoParsingResult.getLicenseInfo() == null) {
            licenseInfoParsingResult.setLicenseInfo(new LicenseInfo());
        }
        licenseInfoParsingResult.getLicenseInfo().addToFilenames(filename);
        return licenseInfoParsingResult;
    }

    @Override
    public String getDefaultLicenseInfoHeaderText() {
        return DEFAULT_LICENSE_INFO_TEXT;
    }

    @Override
    public String getDefaultObligationsText() {
        return DEFAULT_OBLIGATIONS_TEXT;
    }

    protected Map<Release, Set<String>> mapKeysToReleases(Map<String, Set<String>> releaseIdsToAttachmentIds, User user) throws TException {
        Map<Release, Set<String>> result = Maps.newHashMap();
        try {
            releaseIdsToAttachmentIds
                    .forEach((relId, attIds) -> wrapTException(() -> result.put(componentDatabaseHandler.getRelease(relId, user), attIds)));
        } catch (WrappedTException exception) {
            throw exception.getCause();
        }
        return result;
    }

    protected void filterEmptyLicenses(List<LicenseInfoParsingResult> results) {
        for (LicenseInfoParsingResult result : results) {
            if (result.isSetLicenseInfo() && result.getLicenseInfo().isSetLicenseNamesWithTexts()) {
                result.getLicenseInfo().setLicenseNamesWithTexts(
                        result.getLicenseInfo().getLicenseNamesWithTexts().stream().filter(licenseNameWithText -> {
                            return !LicenseNameWithTextUtils.isEmpty(licenseNameWithText);
                        }).collect(Collectors.toSet()));
            }
        }
    }

    protected Collection<LicenseInfoParsingResult> getAllReleaseLicenseInfos(Map<Release, Set<String>> releaseToSelectedAttachmentIds,
            User user, Map<String, Set<LicenseNameWithText>> excludedLicensesPerAttachment) throws TException {
        List<LicenseInfoParsingResult> results = Lists.newArrayList();

        for (Entry<Release, Set<String>> entry : releaseToSelectedAttachmentIds.entrySet()) {
            for (String attachmentContentId : entry.getValue()) {
                if (attachmentContentId != null) {
                    Set<LicenseNameWithText> licencesToExclude = excludedLicensesPerAttachment.getOrDefault(attachmentContentId,
                            Sets.newHashSet());
                    List<LicenseInfoParsingResult> parsedLicenses = getLicenseInfoForAttachment(entry.getKey(), attachmentContentId, user);

                    results.addAll(
                            parsedLicenses.stream().map(result -> filterLicenses(result, licencesToExclude)).collect(Collectors.toList()));
                }
            }
        }

        return results;
    }

    private Collection<ObligationParsingResult> getAllReleaseObligations(Map<Release, Set<String>> releaseToSelectedAttachmentIds, User user)
            throws TException {
        List<ObligationParsingResult> results = Lists.newArrayList();

        for (Entry<Release, Set<String>> entry : releaseToSelectedAttachmentIds.entrySet()) {
            for (String attachmentContentId : entry.getValue()) {
                if (attachmentContentId != null) {
                    results.addAll(getObligationsForAttachment(entry.getKey(), attachmentContentId, user));
                }
            }
        }

        return results;
    }

    protected LicenseInfoParsingResult filterLicenses(LicenseInfoParsingResult result, Set<LicenseNameWithText> licencesToExclude) {
        // make a deep copy to NOT change the original document that is cached
        LicenseInfoParsingResult newResult = result.deepCopy();

        if (result.getLicenseInfo() != null) {
            Set<LicenseNameWithText> filteredLicenses = nullToEmptySet(result.getLicenseInfo().getLicenseNamesWithTexts())
                    .stream()
                    .filter(license -> {
                        for (LicenseNameWithText excludeLicense : licencesToExclude) {
                            if (LicenseNameWithTextUtils.licenseNameWithTextEquals(license, excludeLicense)) {
                                return false;
                            }
                        }
                        return true;
                    }).collect(Collectors.toSet());
            newResult.getLicenseInfo().setLicenseNamesWithTexts(filteredLicenses);
        }
        return newResult;
    }

    protected LicenseInfoParsingResult noSourceParsingResult(String message) {
        return new LicenseInfoParsingResult().setStatus(LicenseInfoRequestStatus.NO_APPLICABLE_SOURCE).setMessage(message);
    }

    protected List<LicenseInfoParsingResult> assignReleaseToLicenseInfoParsingResult(LicenseInfoParsingResult licenseInfoParsingResult,
            Release release) {
        return assignReleaseToLicenseInfoParsingResults(Collections.singletonList(licenseInfoParsingResult), release);
    }

    protected List<LicenseInfoParsingResult> assignReleaseToLicenseInfoParsingResults(List<LicenseInfoParsingResult> parsingResults,
            Release release) {
        parsingResults.forEach(r -> {
            //override by given release only if the fields were not set by parser, because parser knows best
            if (!r.isSetVendor() && !r.isSetName() && !r.isSetVersion()) {
                r.setVendor(release.isSetVendor() ? release.getVendor().getShortname() : "");
                r.setName(release.getName());
                r.setVersion(release.getVersion());
                r.setRelease(release);
            }
        });
        return parsingResults;
    }

    protected List<ObligationParsingResult> assignReleaseToObligationParsingResults(List<ObligationParsingResult> parsingResults,
    Release release) {
        parsingResults.stream()
            .filter(r -> ! r.isSetRelease())
            .forEach(r -> r.setRelease(release));
        return parsingResults;
    }

    private List<LicenseInfoParsingResult> assignComponentToLicenseInfoParsingResults(List<LicenseInfoParsingResult> parsingResults, Release release, User user) throws TException {
        final ComponentService.Iface componentClient = new ThriftClients().makeComponentClient();
        final Component component = componentClient.getComponentById(release.getComponentId(), user);

        parsingResults.forEach(result -> {
            if(component != null) {
                if (component.getComponentType() != null) {
                    result.setComponentType(toString(component.getComponentType()));
                } else {
                    LOGGER.warn("Component with [" + component.getId() + ": " + component.getName() + "] has no type!");
                    result.setComponentType("");
                }
            } else {
                // just being extra defensive
                result.setComponentType("Unknown component.");
            }
        });
        return parsingResults;
    }

    protected String toString(ComponentType type) {
        switch(type) {
            case INTERNAL:
                return "Internal";
            case OSS:
                return "OSS";
            case COTS:
                return "COTS";
            case FREESOFTWARE:
                return  "Free software";
            case INNER_SOURCE:
                return "Inner source";
            case SERVICE:
                return "Service";
        }

        return "";
    }

    protected OutputGenerator<?> getOutputGeneratorByClassname(String generatorClassname) throws TException {
        assertNotNull(generatorClassname);
        return outputGenerators.stream()
                .filter(outputGenerator -> generatorClassname.equals(outputGenerator.getClass().getSimpleName()))
                .findFirst().orElseThrow(() -> new TException("Unknown output generator: " + generatorClassname));
    }

    protected OutputGenerator<?> getOutputGeneratorByClassnameAndVariant(String generatorClassname, OutputFormatVariant generatorVariant) throws TException {
        assertNotNull(generatorClassname);
        assertNotNull(generatorVariant);
        return outputGenerators.stream()
                .filter(outputGenerator -> generatorClassname.equals(outputGenerator.getClass().getSimpleName()))
                .filter(outputGenerator -> generatorVariant.equals(outputGenerator.getOutputVariant()))
                .findFirst().orElseThrow(() -> new TException("Unknown output generator: " + generatorClassname));
    }

    private static String dropCommentedLine(String TEMPLATE_FILE) {
        String text = new String( CommonUtils.loadResource(LicenseInfoHandler.class, TEMPLATE_FILE).orElse(new byte[0]) );
        return text.replaceAll("(?m)^#.*(?:\r?\n)?", ""); // ignore comments in template file
    }
}
