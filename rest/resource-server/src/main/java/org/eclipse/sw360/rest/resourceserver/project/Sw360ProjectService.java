/*
 * Copyright Siemens AG, 2017-2018.
 * Copyright Bosch Software Innovations GmbH, 2017.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.project;

import com.google.common.collect.ImmutableSet;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.common.WrappedException.WrappedTException;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.attachments.*;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStatusData;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoService;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseObligationsStatusInfo;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationParsingResult;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationList;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationStatusInfo;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectData;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectLink;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer;
import org.eclipse.sw360.rest.resourceserver.core.AwareOfRestServices;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.release.ReleaseController;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.Link;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import javax.annotation.PreDestroy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.nullToEmpty;
import static org.eclipse.sw360.datahandler.common.CommonUtils.arrayToList;
import static org.eclipse.sw360.datahandler.common.CommonUtils.getSortedMap;
import static org.eclipse.sw360.datahandler.common.CommonUtils.isNullEmptyOrWhitespace;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyList;
import static org.eclipse.sw360.datahandler.common.WrappedException.wrapTException;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360ProjectService implements AwareOfRestServices<Project> {

    private static final Logger log = LogManager.getLogger(Sw360ProjectService.class);

    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;

    @NonNull
    private RestControllerHelper rch;

    public static final ExecutorService releaseExecutor = Executors.newFixedThreadPool(10);

    public static final ImmutableSet<ObligationStatusInfo._Fields> SET_OF_LICENSE_OBLIGATION_FIELDS = ImmutableSet
            .of(ObligationStatusInfo._Fields.COMMENT, ObligationStatusInfo._Fields.STATUS);

    public Set<Project> getProjectsForUser(User sw360User, Pageable pageable) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        PaginationData pageData = new PaginationData()
                .setDisplayStart((int) pageable.getOffset())
                .setRowsPerPage(pageable.getPageSize())
                .setSortColumnNumber(0);
        Map<PaginationData, List<Project>> pageDtToProjects = sw360ProjectClient.getAccessibleProjectsSummaryWithPagination(sw360User, pageData);
        return new HashSet<>(pageDtToProjects.entrySet().iterator().next().getValue());
    }

    public Project getProjectForUserById(String projectId, User sw360User) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        try {
            Project project = sw360ProjectClient.getProjectById(projectId, sw360User);
            Map<String, String> sortedAdditionalData = getSortedMap(project.getAdditionalData(), true);
            project.setAdditionalData(sortedAdditionalData);
            return project;
        } catch (SW360Exception sw360Exp) {
            if (sw360Exp.getErrorCode() == 404) {
                throw new ResourceNotFoundException("Requested Project Not Found");
            } else if (sw360Exp.getErrorCode() == 403) {
                throw new AccessDeniedException(
                        "Project or its Linked Projects are restricted and / or not accessible");
            } else {
                throw sw360Exp;
            }
        }
    }

    public boolean validate(List<String> changedUsages, User sw360User, Sw360ReleaseService releaseService, Set<String> totalReleaseIds) throws TException {
		for (String data : changedUsages) {
			String releaseId;
			String usageData;
			String attachmentContentId;
			String[] parts = data.split("-");
			if (parts.length > 1) {
				String[] components = parts[1].split("_");
	            releaseId = components[0];
	            usageData = components[1];
	            attachmentContentId = components[2];
			}
			else {
				String[] components = data.split("_");
	            releaseId = components[0];
	            usageData = components[1];
	            attachmentContentId = components[2];
			}
			boolean relPresent = totalReleaseIds.contains(releaseId);
			if (!relPresent) {
				return false;
			}
            Release release = releaseService.getReleaseForUserById(releaseId, sw360User);
            Set<Attachment> attachments = release.getAttachments();
            if (usageData.equals("sourcePackage")) {
                for (Attachment attach : attachments) {
                    if (attach.getAttachmentContentId().equals(attachmentContentId) && (attach.getAttachmentType() != AttachmentType.SOURCE && attach.getAttachmentType() != AttachmentType.SOURCE_SELF)) {
                        return false;
                    }
                }
            }
            if (usageData.equals("licenseInfo")) {
                for (Attachment attach : attachments) {
                    if (attach.getAttachmentContentId().equals(attachmentContentId) && (attach.getAttachmentType() != AttachmentType.COMPONENT_LICENSE_INFO_COMBINED && attach.getAttachmentType() != AttachmentType.COMPONENT_LICENSE_INFO_XML)) {
                        return false;
                    }
                }
            }
        } return true;
    }

    public List<String> savedUsages(List<AttachmentUsage> allUsagesByProject) {
        List<String> selectedData = new ArrayList<>();
        for (AttachmentUsage usage : allUsagesByProject) {
            if (usage.getUsageData().getSetField().equals(UsageData._Fields.LICENSE_INFO)) {
                StringBuilder result = new StringBuilder();
                result.append(usage.getUsageData().getLicenseInfo().getProjectPath())
                      .append("-")
                      .append(usage.getOwner().getReleaseId())
                      .append("_")
                      .append(usage.getUsageData().getSetField().getFieldName())
                      .append("_")
                      .append(usage.getAttachmentContentId());
                String stringResult = result.toString();
                selectedData.add(stringResult);
            }
            else {
                StringBuilder result = new StringBuilder();
                result.append(usage.getOwner().getReleaseId())
                      .append("_")
                      .append(usage.getUsageData().getSetField().getFieldName())
                      .append("_")
                      .append(usage.getAttachmentContentId());
                String stringResult = result.toString();
                selectedData.add(stringResult);
            }
        }
        System.out.println("Resulting string: " + selectedData);
        return selectedData;
    }

    public List<AttachmentUsage> deselectedAttachmentUsagesFromRequest(List<String> deselectedUsages, List<String> selectedUsages, List<String> deselectedConcludedUsages, List<String> selectedConcludedUsages, String id) {
        return makeAttachmentUsagesFromParameters(deselectedUsages, selectedUsages, deselectedConcludedUsages, selectedConcludedUsages,  Sets::difference, true, id);
    }

    public List<AttachmentUsage> selectedAttachmentUsagesFromRequest(List<String> deselectedUsages, List<String> selectedUsages, List<String> deselectedConcludedUsages, List<String> selectedConcludedUsages, String id) {
        return makeAttachmentUsagesFromParameters(deselectedUsages, selectedUsages, deselectedConcludedUsages, selectedConcludedUsages, Sets::intersection, false, id);
    }

    private static List<AttachmentUsage> makeAttachmentUsagesFromParameters(List<String> deselectedUsages, List<String> selectedUsage,
            List<String> deselectedConcludedUsages, List<String> selectedConcludedUsages,
            BiFunction<Set<String>, Set<String>, Set<String>> computeUsagesFromCheckboxes, boolean deselectUsage, String projectId) {
        Set<String> selectedUsages = new HashSet<>(selectedUsage);
        Set<String> changedUsages = new HashSet<>(deselectedUsages);
        Set<String> changedIncludeConludedLicenses = new HashSet<>(deselectedConcludedUsages);
        changedUsages = Sets.union(changedUsages, new HashSet(changedIncludeConludedLicenses));
        List<String> includeConludedLicenses = new ArrayList<>(selectedConcludedUsages);
        Set<String> usagesSubset = computeUsagesFromCheckboxes.apply(changedUsages, selectedUsages);
        if (deselectUsage) {
            usagesSubset = Sets.union(usagesSubset, new HashSet(changedIncludeConludedLicenses));
        }
        return usagesSubset.stream()
                .map(s -> parseAttachmentUsageFromString(projectId, s, includeConludedLicenses))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static AttachmentUsage parseAttachmentUsageFromString(String projectId, String s, List<String> includeConludedLicense) {
        String[] split = s.split("_");
        if (split.length != 3) {
            log.warn(String.format("cannot parse attachment usage from %s for project id %s", s, projectId));
            return null;
        }

        String releaseId = split[0];
        String type = split[1];
        String attachmentContentId = split[2];
        String projectPath = null;
        if (UsageData._Fields.findByName(type).equals(UsageData._Fields.LICENSE_INFO)) {
            String[] projectPath_releaseId = split[0].split("-");
            if (projectPath_releaseId.length == 2) {
                releaseId = projectPath_releaseId[1];
                projectPath = projectPath_releaseId[0];
            }
        }

        AttachmentUsage usage = new AttachmentUsage(Source.releaseId(releaseId), attachmentContentId, Source.projectId(projectId));
        final UsageData usageData;
        switch (UsageData._Fields.findByName(type)) {
            case LICENSE_INFO:
                LicenseInfoUsage licenseInfoUsage = new LicenseInfoUsage(Collections.emptySet());
                licenseInfoUsage.setIncludeConcludedLicense(includeConludedLicense.contains(s));
                if (projectPath != null) {
                    licenseInfoUsage.setProjectPath(projectPath);
                }
                usageData = UsageData.licenseInfo(licenseInfoUsage);
                break;
            case SOURCE_PACKAGE:
                usageData = UsageData.sourcePackage(new SourcePackageUsage());
                break;
            case MANUALLY_SET:
                usageData = UsageData.manuallySet(new ManuallySetUsage());
                break;
            default:
                throw new IllegalArgumentException("Unexpected UsageData type: " + type);
        }
        usage.setUsageData(usageData);
        return usage;
    }

    /**
     * Here, "equivalent" means an AttachmentUsage should replace another one in the DB, not that they are equal.
     * I.e, they have the same attachmentContentId, owner, usedBy, and same UsageData type.
     */
    @NotNull
    public Predicate<AttachmentUsage> isUsageEquivalent(AttachmentUsage usage) {
        return equivalentUsage -> usage.getAttachmentContentId().equals(equivalentUsage.getAttachmentContentId()) &&
                usage.getOwner().equals(equivalentUsage.getOwner()) &&
                usage.getUsedBy().equals(equivalentUsage.getUsedBy()) &&
                usage.getUsageData().getSetField().equals(equivalentUsage.getUsageData().getSetField());
    }

    public void deleteAttachmentUsages(List<AttachmentUsage> usagesToDelete) throws TException {
        ThriftClients thriftClients = new ThriftClients();
        AttachmentService.Iface attachmentClient = thriftClients.makeAttachmentClient();
        attachmentClient.deleteAttachmentUsages(usagesToDelete);
	}

	public void makeAttachmentUsages(List<AttachmentUsage> usagesToCreate) throws TException {
		ThriftClients thriftClients = new ThriftClients();
		AttachmentService.Iface attachmentClient = thriftClients.makeAttachmentClient();
		attachmentClient.makeAttachmentUsages(usagesToCreate);
	}

	public List<AttachmentUsage> getUsedAttachments(Source usedBy, Object object) throws TException {
		ThriftClients thriftClients = new ThriftClients();
		AttachmentService.Iface attachmentClient = thriftClients.makeAttachmentClient();
		List<AttachmentUsage> allUsagesByProjectAfterCleanUp = attachmentClient.getUsedAttachments(usedBy, null);
		return allUsagesByProjectAfterCleanUp;
	}

    public String getCyclicLinkedProjectPath(Project project, User user) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        String cyclicLinkedProjectPath = sw360ProjectClient.getCyclicLinkedProjectPath(project, user);
        return cyclicLinkedProjectPath;
    }

    public Map<String, Set<Release>> getLicensesFromAttachmentUsage(
            Map<String, AttachmentUsage> licenseInfoAttachmentUsage, User user) {
        ThriftClients thriftClients = new ThriftClients();
        LicenseInfoService.Iface licenseInfoClient = thriftClients.makeLicenseInfoClient();
        ComponentService.Iface componentClient = thriftClients.makeComponentClient();
        Map<String, Release> attachmentIdToReleaseMap = new HashMap<String, Release>();
        Map<String, Set<Release>> licenseIdToReleasesMap = new HashMap<>();
        licenseInfoAttachmentUsage.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null).forEach(entry -> {
                    String releaseId = entry.getValue().getOwner().getReleaseId();
                    Release releaseById = null;
                    try {
                        releaseById = componentClient.getReleaseById(releaseId, user);
                    } catch (TException exp) {
                        log.warn("Error fetching Release from backend! Release Id-" + releaseId, exp.getMessage());
                        return;
                    }
                    if (CommonUtils.isNullOrEmptyCollection(releaseById.getAttachments()))
                        return;

                    Set<Attachment> attachmentFiltered = releaseById.getAttachments().stream().filter(Objects::nonNull)
                            .filter(att -> entry.getKey().equals(att.getAttachmentContentId()))
                            .filter(att -> att.getCheckStatus() != null && att.getCheckStatus() == CheckStatus.ACCEPTED)
                            .collect(Collectors.toSet());

                    if (CommonUtils.isNullOrEmptyCollection(attachmentFiltered))
                        return;
                    releaseById.setAttachments(attachmentFiltered);

                    attachmentIdToReleaseMap.put(entry.getKey(), releaseById);
                });

        attachmentIdToReleaseMap.entrySet().stream().filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .forEach(entry -> wrapTException(() -> {
                    List<LicenseInfoParsingResult> licenseInfoForAttachment = licenseInfoClient
                            .getLicenseInfoForAttachment(entry.getValue(), entry.getKey(), false, user);
                    Set<String> licenseIds = licenseInfoForAttachment.stream().filter(Objects::nonNull)
                            .filter(lia -> lia.getLicenseInfo() != null)
                            .filter(lia -> lia.getLicenseInfo().getLicenseNamesWithTexts() != null)
                            .flatMap(lia -> lia.getLicenseInfo().getLicenseNamesWithTexts().stream())
                            .filter(Objects::nonNull)
                            .map(licenseNamesWithTexts -> CommonUtils.isNotNullEmptyOrWhitespace(
                                    licenseNamesWithTexts.getLicenseSpdxId()) ? licenseNamesWithTexts.getLicenseSpdxId()
                                            : licenseNamesWithTexts.getLicenseName())
                            .filter(CommonUtils::isNotNullEmptyOrWhitespace).collect(Collectors.toSet());

                    licenseIds.stream().forEach(licenseId -> {
                        if (licenseIdToReleasesMap.containsKey(licenseId)) {
                            licenseIdToReleasesMap.get(licenseId).add(entry.getValue());
                        } else {
                            Set<Release> listOfRelease = new HashSet<>();
                            listOfRelease.add(entry.getValue());
                            licenseIdToReleasesMap.put(licenseId, listOfRelease);
                        }
                    });
                }));

        return licenseIdToReleasesMap;
    }

    public Map<String, AttachmentUsage> getLicenseInfoAttachmentUsage(String projectId) {
        Map<String, AttachmentUsage> licenseInfoUsages = new HashMap<>();
        try {
            ThriftClients thriftClients = new ThriftClients();
            AttachmentService.Iface attachmentClient = thriftClients.makeAttachmentClient();

            List<AttachmentUsage> attachmentUsages = wrapTException(
                    () -> attachmentClient.getUsedAttachments(Source.projectId(projectId), null));
            Collector<AttachmentUsage, ?, Map<String, AttachmentUsage>> attachmentUsageMapCollector = Collectors.toMap(
                    AttachmentUsage::getAttachmentContentId, Function.identity(),
                    Sw360ProjectService::mergeAttachmentUsages);
            BiFunction<List<AttachmentUsage>, UsageData._Fields, Map<String, AttachmentUsage>> filterAttachmentUsages = (
                    attUsages, type) -> attUsages.stream()
                            .filter(attUsage -> attUsage.getUsageData().getSetField().equals(type))
                            .collect(attachmentUsageMapCollector);

            licenseInfoUsages = filterAttachmentUsages.apply(attachmentUsages, UsageData._Fields.LICENSE_INFO);

        } catch (WrappedTException e) {
            log.error("Error fetching AttachmentUsage from backend!", e);
        }

        return licenseInfoUsages;
    }

    static AttachmentUsage mergeAttachmentUsages(AttachmentUsage u1, AttachmentUsage u2) {
        if (u1.getUsageData() == null) {
            if (u2.getUsageData() == null) {
                return u1;
            } else {
                throw new IllegalArgumentException("Cannot merge attachment usages of different usage types");
            }
        } else {
            if (!u1.getUsageData().getSetField().equals(u2.getUsageData().getSetField())) {
                throw new IllegalArgumentException("Cannot merge attachment usages of different usage types");
            }
        }
        AttachmentUsage mergedUsage = u1.deepCopy();
        switch (u1.getUsageData().getSetField()) {
            case LICENSE_INFO:
                mergedUsage.getUsageData().getLicenseInfo().setExcludedLicenseIds(
                        Sets.union(Optional.of(u1)
                                        .map(AttachmentUsage::getUsageData)
                                        .map(UsageData::getLicenseInfo)
                                        .map(LicenseInfoUsage::getExcludedLicenseIds)
                                        .orElse(Collections.emptySet()),
                                Optional.of(u2)
                                        .map(AttachmentUsage::getUsageData)
                                        .map(UsageData::getLicenseInfo)
                                        .map(LicenseInfoUsage::getExcludedLicenseIds)
                                        .orElse(Collections.emptySet())));
                break;
            case SOURCE_PACKAGE:
            case MANUALLY_SET:
                // do nothing
                // source package and manual usages do not have any information to be merged
                break;
            default:
                throw new IllegalArgumentException("Unexpected UsageData type: " + u1.getUsageData().getSetField());
        }

        return mergedUsage;
    }

    public Map<String, ObligationStatusInfo> getLicenseObligationData(Map<String, Set<Release>> licensesFromAttachmentUsage, User user) {
        ThriftClients thriftClients = new ThriftClients();
        LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();
        Map<String, ObligationStatusInfo> obligationStatusMap = new HashMap<String, ObligationStatusInfo>();
        licensesFromAttachmentUsage.entrySet().stream().forEach(entry -> wrapTException(() -> {
            License lic = null;
            Set<Release> releaseData = entry.getValue();
            Set<Release> limitedSet = new HashSet<>();
            Map<String, String> releaseIdToAcceptedCli = new HashMap<String, String>();
            for (Release rel : releaseData) {
                String releaseId = rel.getId();
                for (Attachment attachment : rel.getAttachments()) {
                    if (CheckStatus.ACCEPTED.equals(attachment.getCheckStatus())) {
                        String attachmentContentId = attachment.getAttachmentContentId();
                        releaseIdToAcceptedCli.put(releaseId, attachmentContentId);
                    }
                }
                Release limitedRelease = new Release();
                limitedRelease.setId(rel.getId());
                limitedRelease.setName(rel.getName());
                limitedRelease.setVersion(rel.getVersion());
                limitedSet.add(limitedRelease);
            }
            try {
                lic = licenseClient.getByID(entry.getKey(), user.getDepartment());
            } catch (TException exp) {
                log.warn("Error fetching license from backend! License Id-" + entry.getKey(), exp.getMessage());
                return;
            }
            if (lic == null || CommonUtils.isNullOrEmptyCollection(lic.getObligations()))
                return;

            lic.getObligations().stream().filter(Objects::nonNull).forEach(obl -> {
                String keyOfObl = CommonUtils.isNotNullEmptyOrWhitespace(obl.getTitle()) ? obl.getTitle()
                        : obl.getText();
                ObligationStatusInfo osi = null;
                if (obligationStatusMap.containsKey(keyOfObl)) {
                    osi = obligationStatusMap.get(keyOfObl);
                } else {
                    osi = new ObligationStatusInfo();
                    obligationStatusMap.put(keyOfObl, osi);
                }
                osi.setText(obl.getText());
                osi.setId(obl.getId());
                osi.setObligationType(obl.getObligationType());
                osi.setReleaseIdToAcceptedCLI(releaseIdToAcceptedCli);
                Set<String> licenseIds = osi.getLicenseIds();
                if (licenseIds == null) {
                    licenseIds = new HashSet<>();
                    osi.setLicenseIds(licenseIds);
                }
                licenseIds.add(entry.getKey());
                Set<Release> releases = osi.getReleases();
                if (releases == null) {
                    releases = new HashSet<>();
                    osi.setReleases(releases);
                }
                releases.addAll(limitedSet);
            });

        }));
        return obligationStatusMap;
    }

    public Map<String, ObligationStatusInfo> compareObligationStatusMap(
            User sw360User, Map<String, ObligationStatusInfo> obligationStatusMap, Map<String, ObligationStatusInfo> requestBodyObligationStatusInfo) {
        Map<String, ObligationStatusInfo> newResults = new HashMap<>();
        final String email = sw360User.getEmail();
        final String createdOn = SW360Utils.getCreatedOn();
        if (!CommonUtils.isNullOrEmptyMap(obligationStatusMap)) {

            for (String key : obligationStatusMap.keySet()) {
                if (requestBodyObligationStatusInfo.containsKey(key)) {
                    ObligationStatusInfo requestValue = requestBodyObligationStatusInfo.get(key);
                    ObligationStatusInfo databaseValue = obligationStatusMap.get(key);
                    for (ObligationStatusInfo._Fields field : ObligationStatusInfo._Fields.values()) {
                        Object fieldValue = requestValue.getFieldValue(field);
                        if (fieldValue != null && SET_OF_LICENSE_OBLIGATION_FIELDS.contains(field)) {
                            databaseValue.setFieldValue(field, fieldValue);
                            if (field == ObligationStatusInfo._Fields.STATUS) {
                                databaseValue.setModifiedBy(email);
                                databaseValue.setModifiedOn(createdOn);
                            }
                        }
                    }
                    newResults.put(key, obligationStatusMap.get(key));
                } else {
                    newResults.put(key, obligationStatusMap.get(key));
                }
            }
            return newResults;
        }
        throw new ResourceNotFoundException("Obligation Id not found for the given project");
    }

    public RequestStatus patchLinkedObligations(User sw360User, Map<String, ObligationStatusInfo> updatedObligationStatusMap, ObligationList obligation) {
        try {
            ThriftClients thriftClients = new ThriftClients();
            ProjectService.Iface client = thriftClients.makeProjectClient();
            obligation.unsetLinkedObligationStatus();
            obligation.setLinkedObligationStatus(updatedObligationStatusMap);
            return client.updateLinkedObligations(obligation, sw360User);
        } catch (TException exception) {
            log.error("Failed to update obligation for project: ");
        }
        return RequestStatus.FAILURE;
    }

    public RequestStatus addLinkedObligations(Project project, User user, Map<String, ObligationStatusInfo> licenseObligation) {
        try {
            ThriftClients thriftClients = new ThriftClients();
            ProjectService.Iface client = thriftClients.makeProjectClient();
            final boolean isObligationPresent = CommonUtils.isNotNullEmptyOrWhitespace(project.getLinkedObligationId());
            final String email = user.getEmail();
            final String createdOn = SW360Utils.getCreatedOn();
            final ObligationList obligation = isObligationPresent
                    ? client.getLinkedObligations(project.getLinkedObligationId(), user)
                    : new ObligationList().setProjectId(project.getId());

            Map<String, ObligationStatusInfo> obligationStatusInfo = isObligationPresent
                    && obligation.getLinkedObligationStatusSize() > 0 ? obligation.getLinkedObligationStatus() : Maps.newHashMap();

            for (Map.Entry<String, ObligationStatusInfo> entry : licenseObligation.entrySet()) {
                ObligationStatusInfo newOsi = entry.getValue();
                ObligationStatusInfo currentOsi = obligationStatusInfo.get(entry.getKey());
                if (newOsi.isSetModifiedOn()) {
                    newOsi.setModifiedBy(email);
                    newOsi.setModifiedOn(createdOn);
                    obligationStatusInfo.put(entry.getKey(), newOsi);
                } else if (null != currentOsi) {
                    if (newOsi.getReleaseIdToAcceptedCLISize() > 0)
                        currentOsi.setReleaseIdToAcceptedCLI(newOsi.getReleaseIdToAcceptedCLI());
                    obligationStatusInfo.put(entry.getKey(), currentOsi);
                }

                obligationStatusInfo.computeIfAbsent(entry.getKey(), e -> newOsi);
            }
            obligation.unsetLinkedObligationStatus();
            obligation.setLinkedObligationStatus(obligationStatusInfo);
            return isObligationPresent ? client.updateLinkedObligations(obligation, user) : client.addLinkedObligations(obligation, user);
        } catch (TException exception) {
            log.error("Failed to add/update obligation for project: " + project.getId(), exception);
        }
        return RequestStatus.FAILURE;
    }

    public ObligationList getObligationData(String linkedObligationId, User user) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        return sw360ProjectClient.getLinkedObligations(linkedObligationId, user);
    }

    public Map<String, ObligationStatusInfo> setLicenseInfoWithObligations(Map<String, ObligationStatusInfo> obligationStatusMap, Map<String, String> releaseIdToAcceptedCLI,
            List<Release> releases, User user) {

        final List<LicenseInfoParsingResult> licenseInfoWithObligations = Lists.newArrayList();
        ThriftClients thriftClients = new ThriftClients();
        LicenseInfoService.Iface licenseClient = thriftClients.makeLicenseInfoClient();

        for (Release release : releases) {
            List<Attachment> approvedCliAttachments = SW360Utils.getApprovedClxAttachmentForRelease(release);
            if (approvedCliAttachments.isEmpty()) {
                approvedCliAttachments = SW360Utils.getClxAttachmentForRelease(release);
            }
            final String releaseId = release.getId();

            if (approvedCliAttachments.size() == 1) {
                final Attachment filteredAttachment = approvedCliAttachments.get(0);
                final String attachmentContentId = filteredAttachment.getAttachmentContentId();

                if (releaseIdToAcceptedCLI.containsKey(releaseId) && releaseIdToAcceptedCLI.get(releaseId).equals(attachmentContentId)) {
                    releaseIdToAcceptedCLI.remove(releaseId);
                }

                try {
                    List<LicenseInfoParsingResult> licenseResults = licenseClient.getLicenseInfoForAttachment(release, attachmentContentId, false, user);

                    List<ObligationParsingResult> obligationResults = licenseClient.getObligationsForAttachment(release, attachmentContentId, user);

                    if (CommonUtils.allAreNotEmpty(licenseResults, obligationResults) && obligationResults.get(0).getObligationsAtProjectSize() > 0) {
                        licenseInfoWithObligations.add(licenseClient.createLicenseToObligationMapping(licenseResults.get(0), obligationResults.get(0)));
                    }
                } catch (TException exception) {
                    log.error(String.format("Error fetchinig license Information for attachment: %s in release: %s",
                            filteredAttachment.getFilename(), releaseId), exception);
                }
            }
        }

        try {
            LicenseObligationsStatusInfo licenseObligation = licenseClient.getProjectObligationStatus(obligationStatusMap,
                    licenseInfoWithObligations, releaseIdToAcceptedCLI);
            Map<String, String> releaseIdToAcceptedCli = new HashMap<String, String>();
            obligationStatusMap = licenseObligation.getObligationStatusMap();
            for (Map.Entry<String, ObligationStatusInfo> entry : obligationStatusMap.entrySet()) {
                ObligationStatusInfo details = entry.getValue();
                if (details.getReleaseIdToAcceptedCLI() == null) {
                    Set<Release> releaseData = details.getReleases();
                    for (Release rel : releaseData) {
                        String releaseId = rel.getId();
                        for (Attachment attachment : rel.getAttachments()) {
                            if (CheckStatus.ACCEPTED.equals(attachment.getCheckStatus())) {
                                String attachmentContentId = attachment.getAttachmentContentId();
                                releaseIdToAcceptedCli.put(releaseId, attachmentContentId);
                            }
                        }
                    }
                    details.setReleaseIdToAcceptedCLI(releaseIdToAcceptedCli);
                }
                details.unsetReleases();
            }
        } catch (TException e) {
            log.error("Failed to set obligation status for project!", e);
        }
        return obligationStatusMap;
    }

    public Set<Project> searchLinkingProjects(String projectId, User sw360User) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        return sw360ProjectClient.searchLinkingProjects(projectId, sw360User);
    }

    public Set<Project> getProjectsByReleaseIds(Set<String> releaseids, User sw360User)
            throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        return sw360ProjectClient.searchByReleaseIds(releaseids, sw360User);
    }

    public Set<Project> getProjectsByRelease(String releaseid, User sw360User)
            throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        return sw360ProjectClient.searchByReleaseId(releaseid, sw360User);
    }

    public Project createProject(Project project, User sw360User) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        rch.checkForCyclicOrInvalidDependencies(sw360ProjectClient, project, sw360User);
        AddDocumentRequestSummary documentRequestSummary = sw360ProjectClient.addProject(project, sw360User);
        if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.SUCCESS) {
            project.setId(documentRequestSummary.getId());
            project.setCreatedBy(sw360User.getEmail());
            Map<String, String> sortedAdditionalData = getSortedMap(project.getAdditionalData(), true);
            project.setAdditionalData(sortedAdditionalData);
            return project;
        } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.DUPLICATE) {
            throw new DataIntegrityViolationException("sw360 project with name '" + project.getName() + "' already exists.");
        } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.INVALID_INPUT) {
            throw new HttpMessageNotReadableException("Dependent document Id/ids not valid.");
        } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.NAMINGERROR) {
            throw new HttpMessageNotReadableException("Project name field cannot be empty or contain only whitespace character");
        }
        return null;
    }

    public RequestStatus updateProject(Project project, User sw360User) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        String cyclicLinkedProjectPath = null;
        rch.checkForCyclicOrInvalidDependencies(sw360ProjectClient, project, sw360User);

        // TODO: Move this logic to backend
        if (project.getReleaseIdToUsage() != null) {
            for (String releaseId : project.getReleaseIdToUsage().keySet()) {
                if (isNullEmptyOrWhitespace(releaseId)) {
                    throw new HttpMessageNotReadableException("Release Id can't be empty");
                }
            }
        }

        if (project.getVendor() != null && project.getVendorId() == null) {
            project.setVendorId(project.getVendor().getId());
        }

        RequestStatus requestStatus;
        if (Sw360ResourceServer.IS_FORCE_UPDATE_ENABLED) {
            requestStatus = sw360ProjectClient.updateProjectWithForceFlag(project, sw360User, true);
        } else {
            requestStatus = sw360ProjectClient.updateProject(project, sw360User);
        }
        if (requestStatus == RequestStatus.NAMINGERROR) {
            throw new HttpMessageNotReadableException("Project name field cannot be empty or contain only whitespace character");
        }

        if (requestStatus == RequestStatus.CLOSED_UPDATE_NOT_ALLOWED) {
            throw new RuntimeException("User cannot modify a closed project");
        } if (requestStatus == RequestStatus.INVALID_INPUT) {
            throw new HttpMessageNotReadableException("Dependent document Id/ids not valid.");
        } else if (requestStatus != RequestStatus.SENT_TO_MODERATOR && requestStatus != RequestStatus.SUCCESS) {
            throw new RuntimeException("sw360 project with name '" + project.getName() + " cannot be updated.");
        }
        return requestStatus;
    }

    public RequestStatus deleteProject(String projectId, User sw360User) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        if (Sw360ResourceServer.IS_FORCE_UPDATE_ENABLED) {
            return sw360ProjectClient.deleteProjectWithForceFlag(projectId, sw360User, true);
        } else {
            return sw360ProjectClient.deleteProject(projectId, sw360User);
        }
    }

    public void deleteAllProjects(User sw360User) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        List<Project> projects = sw360ProjectClient.getAccessibleProjectsSummary(sw360User);
        for (Project project : projects) {
            if (Sw360ResourceServer.IS_FORCE_UPDATE_ENABLED) {
                sw360ProjectClient.deleteProjectWithForceFlag(project.getId(), sw360User, true);
            } else {
                sw360ProjectClient.deleteProject(project.getId(), sw360User);
            }
        }
    }

    public Project getClearingInfo(Project sw360Project, User sw360User) throws TException {
    	ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
    	return sw360ProjectClient.fillClearingStateSummaryIncludingSubprojectsForSingleProject(sw360Project, sw360User);
    }

    public List<Project> searchProjectByName(String name, User sw360User) throws TException {
        final ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        return sw360ProjectClient.searchByName(name, sw360User);
    }

    public List<Project> searchProjectByGroup(String group, User sw360User) throws TException {
        final ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        ProjectData projectData = sw360ProjectClient.searchByGroup(group, sw360User);
        return getAllRequiredProjects(projectData, sw360User);
    }

    public List<Project> searchProjectByTag(String tag, User sw360User) throws TException {
        final ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        ProjectData projectData = sw360ProjectClient.searchByTag(tag, sw360User);
        return getAllRequiredProjects(projectData, sw360User);
    }

    public List<Project> searchProjectByType(String type, User sw360User) throws TException {
        final ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        ProjectData projectData = sw360ProjectClient.searchByType(type, sw360User);
        return getAllRequiredProjects(projectData, sw360User);
    }

    public Set<String> getReleaseIds(String projectId, User sw360User, boolean transitive) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        if (transitive) {
            List<ReleaseClearingStatusData> releaseClearingStatusData = sw360ProjectClient.getReleaseClearingStatuses(projectId, sw360User);
            return releaseClearingStatusData.stream().map(r -> r.release.getId()).collect(Collectors.toSet());
        } else {
            final Project project = getProjectForUserById(projectId, sw360User);
            if (project.getReleaseIdToUsage() == null) {
                return new HashSet<String>();
            }
            return project.getReleaseIdToUsage().keySet();
        }
    }

    public void addEmbeddedLinkedProject(Project sw360Project, User sw360User, HalResource<Project> projectResource, Set<String> projectIdsInBranch) throws TException {
        projectIdsInBranch.add(sw360Project.getId());
        Map<String, ProjectProjectRelationship> linkedProjects = sw360Project.getLinkedProjects();
		List<String> keys = new ArrayList<>(linkedProjects.keySet());
        if (keys != null) {
        	keys.forEach(linkedProjectId -> wrapTException(() -> {
                if (projectIdsInBranch.contains(linkedProjectId)) {
                    return;
                }
                Project linkedProject = getProjectForUserById(linkedProjectId, sw360User);
                Project embeddedLinkedProject = rch.convertToEmbeddedLinkedProject(linkedProject);
                HalResource<Project> halLinkedProject = new HalResource<>(embeddedLinkedProject);
                Link projectLink = linkTo(ProjectController.class)
                        .slash("api/projects/" + embeddedLinkedProject.getId()).withSelfRel();
                halLinkedProject.add(projectLink);
                addEmbeddedLinkedProject(linkedProject, sw360User, halLinkedProject,
                        projectIdsInBranch);
                projectResource.addEmbeddedResource("sw360:linkedProjects", halLinkedProject);
            }));
        }
        projectIdsInBranch.remove(sw360Project.getId());
    }

    public void addEmbeddedlinkedRelease(Release sw360Release, User sw360User, HalResource<Release> releaseResource,
            Sw360ReleaseService releaseService, Set<String> releaseIdsInBranch) throws TException {
        releaseIdsInBranch.add(sw360Release.getId());
        Map<String, ReleaseRelationship> releaseIdToRelationship = sw360Release.getReleaseIdToRelationship();
        if (releaseIdToRelationship != null) {
            releaseIdToRelationship.keySet().forEach(linkedReleaseId -> wrapTException(() -> {
                if (releaseIdsInBranch.contains(linkedReleaseId)) {
                    return;
                }
                Release linkedRelease = releaseService.getReleaseForUserById(linkedReleaseId, sw360User);
                Release embeddedLinkedRelease = rch.convertToEmbeddedRelease(linkedRelease);
                HalResource<Release> halLinkedRelease = new HalResource<>(embeddedLinkedRelease);
                Link releaseLink = linkTo(ReleaseController.class)
                        .slash("api/releases/" + embeddedLinkedRelease.getId()).withSelfRel();
                halLinkedRelease.add(releaseLink);
                addEmbeddedlinkedRelease(linkedRelease, sw360User, halLinkedRelease, releaseService,
                        releaseIdsInBranch);
                releaseResource.addEmbeddedResource("sw360:releases", halLinkedRelease);
            }));
        }
        releaseIdsInBranch.remove(sw360Release.getId());
    }

    @Override
    public Set<Project> searchByExternalIds(Map<String, Set<String>> externalIds, User user) throws TException {
        final ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        return sw360ProjectClient.searchByExternalIds(externalIds, user);
    }

    @Override
    public Project convertToEmbeddedWithExternalIds(Project sw360Object) {
        return rch.convertToEmbeddedProject(sw360Object).setExternalIds(sw360Object.getExternalIds());
    }

    public ProjectService.Iface getThriftProjectClient() throws TTransportException {
        ProjectService.Iface projectClient = new ThriftClients().makeProjectClient();
        return projectClient;
    }

    public Function<ProjectLink, ProjectLink> filterAndSortAttachments(Collection<AttachmentType> attachmentTypes) {
        Predicate<Attachment> filter = att -> attachmentTypes.contains(att.getAttachmentType());
        return createProjectLinkMapper(rl -> rl.setAttachments(nullToEmptyList(rl.getAttachments())
                .stream()
                .filter(filter)
                .sorted(Comparator
                        .comparing((Attachment a) -> nullToEmpty(a.getCreatedTeam()))
                        .thenComparing(Comparator.comparing((Attachment a) -> nullToEmpty(a.getCreatedOn())).reversed()))
                .collect(Collectors.toList())));
    }

    public Function<ProjectLink, ProjectLink> createProjectLinkMapper(Function<ReleaseLink, ReleaseLink> releaseLinkMapper){
        return (projectLink) -> {
            List<ReleaseLink> mappedReleaseLinks = nullToEmptyList(projectLink
                    .getLinkedReleases())
                    .stream()
                    .map(releaseLinkMapper)
                    .collect(Collectors.toList());
            projectLink.setLinkedReleases(mappedReleaseLinks);
            return projectLink;
        };
    }

    protected List<ProjectLink> createLinkedProjects(Project project,
            Function<ProjectLink, ProjectLink> projectLinkMapper, boolean deep, User user) {
        final Collection<ProjectLink> linkedProjects = SW360Utils
                .flattenProjectLinkTree(SW360Utils.getLinkedProjects(project, deep, new ThriftClients(), log, user));
        return linkedProjects.stream().map(projectLinkMapper).collect(Collectors.toList());
    }

    public Set<Release> getReleasesFromProjectIds(List<String> projectIds, boolean transitive, final User sw360User,
                                                  Sw360ReleaseService releaseService) {
        final List<Callable<List<Release>>> callableTasksToGetReleases = new ArrayList<Callable<List<Release>>>();

        projectIds.stream().forEach(id -> {
            Callable<List<Release>> getReleasesByProjectId = () -> {
                final Set<String> releaseIds = getReleaseIds(id, sw360User, transitive);

                List<Release> releases = releaseIds.stream().map(relId -> wrapTException(() -> {
                    final Release sw360Release = releaseService.getReleaseForUserById(relId, sw360User);
                    return sw360Release;
                })).collect(Collectors.toList());
                return releases;
            };
            callableTasksToGetReleases.add(getReleasesByProjectId);
        });

        List<Future<List<Release>>> releasesFuture = new ArrayList<Future<List<Release>>>();
        try {
            releasesFuture = releaseExecutor.invokeAll(callableTasksToGetReleases);
        } catch (InterruptedException e) {
            throw new RuntimeException("Error getting releases: " + e.getMessage());
        }

        List<List<Release>> listOfreleases = releasesFuture.stream().map(fut -> {
            List<Release> rels = new ArrayList<Release>();
            try {
                rels = fut.get();
            } catch (InterruptedException | ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof ResourceNotFoundException) {
                    throw (ResourceNotFoundException) cause;
                }

                if (cause instanceof AccessDeniedException) {
                    throw (AccessDeniedException) cause;
                }
                throw new RuntimeException("Error getting releases: " + e.getMessage());
            }
            return rels;
        }).collect(Collectors.toList());

        final Set<Release> relList = new HashSet<Release>();
        listOfreleases.stream().forEach(listOfRel -> {
            for(Release rel : listOfRel) {
                relList.add(rel);
            }
        });
        return relList;
    }

    public ProjectReleaseRelationship updateProjectReleaseRelationship(
            Map<String, ProjectReleaseRelationship> releaseIdToUsage,
            ProjectReleaseRelationship requestBodyProjectReleaseRelationship, String releaseId) {
        if (!CommonUtils.isNullOrEmptyMap(releaseIdToUsage)) {
            Optional<Entry<String, ProjectReleaseRelationship>> actualProjectReleaseRelationshipEntry = releaseIdToUsage
                    .entrySet().stream().filter(entry -> CommonUtils.isNotNullEmptyOrWhitespace(entry.getKey())
                            && entry.getKey().equals(releaseId))
                    .findFirst();
            if (actualProjectReleaseRelationshipEntry.isPresent()) {
                ProjectReleaseRelationship actualProjectReleaseRelationship = actualProjectReleaseRelationshipEntry
                        .get().getValue();
                rch.updateProjectReleaseRelationship(actualProjectReleaseRelationship,
                        requestBodyProjectReleaseRelationship);
                return actualProjectReleaseRelationship;
            }
        }
        throw new ResourceNotFoundException("Requested Release Not Found");
    }

    @PreDestroy
    public void shutDownThreadpool() {
        releaseExecutor.shutdown();
        try {
            if (!releaseExecutor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                releaseExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            releaseExecutor.shutdownNow();
        }
    }

    public List<Project> refineSearch(Map<String, Set<String>> filterMap, User sw360User) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        return sw360ProjectClient.refineSearch(null, filterMap, sw360User);
    }

    public void copyLinkedObligationsForClonedProject(Project createDuplicateProject, Project sw360Project, User user)
            throws TException {
        SW360Utils.copyLinkedObligationsForClonedProject(createDuplicateProject, sw360Project, getThriftProjectClient(),
                user);
    }

    private List<Project> getAllRequiredProjects(ProjectData projectData, User sw360User) throws TException {
        List<Project> listOfProjects = projectData.getFirst250Projects();
        List<String> projectIdsOfRemainingProject = projectData.getProjectIdsOfRemainingProject();
        if (CommonUtils.isNotEmpty(projectIdsOfRemainingProject)) {
            for (String id : projectIdsOfRemainingProject) {
                Project projectForUserById = getProjectForUserById(id, sw360User);
                listOfProjects.add(projectForUserById);
            }

        }
        return listOfProjects;
    }

    /**
     * From list of projects, filter projects based on their clearing state.
     * @param projects      List of projects to filter
     * @param clearingState Map of clearing states to filter projects for
     * @return List of filtered projects.
     */
    public List<Project> getWithFilledClearingStatus(List<Project> projects, Map<String, Boolean> clearingState) {
        if (!CommonUtils.isNullOrEmptyMap(clearingState)) {
            Boolean open = clearingState.getOrDefault(ProjectClearingState.OPEN.toString(), true);
            Boolean closed = clearingState.getOrDefault(ProjectClearingState.CLOSED.toString(), true);
            Boolean inProgress = clearingState.getOrDefault(ProjectClearingState.IN_PROGRESS.toString(), true);

            projects = projects.stream().filter(project -> {
                if (open != null && open && ProjectClearingState.OPEN.equals(project.getClearingState())) {
                    return true;
                } else if (closed != null && closed && ProjectClearingState.CLOSED.equals(project.getClearingState())) {
                    return true;
                } else if (inProgress != null && inProgress
                        && ProjectClearingState.IN_PROGRESS.equals(project.getClearingState())) {
                    return true;
                }
                return false;
            }).collect(Collectors.toList());
        }
        return projects;
    }

    /**
     * Get my projects from the thrift client.
     * @param user      User to get projects for
     * @param userRoles User roles to filter projects
     * @return List of projects
     * @throws TException
     */
    public List<Project> getMyProjects(User user, Map<String, Boolean> userRoles) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        return sw360ProjectClient.getMyProjects(user, userRoles);
    }

    /**
     * Get count of projects accessible by given user.
     * @param sw360User User to get the count for.
     * @return Total count of projects accessible by user.
     * @throws TException
     */
    public int getMyAccessibleProjectCounts(User sw360User) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        return sw360ProjectClient.getMyAccessibleProjectCounts(sw360User);
    }

    /**
     * Import SPDX SBOM using the method on the thrift client.
     * @param user                User uploading the SBOM
     * @param attachmentContentId Id of the attachment uploaded
     * @return RequestSummary
     * @throws TException
     */
    public RequestSummary importSPDX(User user, String attachmentContentId) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        return sw360ProjectClient.importBomFromAttachmentContent(user, attachmentContentId);
    }

    /**
     * Import CycloneDX SBOM using the method on the thrift client.
     * @param user                User uploading the SBOM
     * @param attachmentContentId Id of the attachment uploaded
     * @return RequestSummary
     * @throws TException
     */
    public RequestSummary importCycloneDX(User user, String attachmentContentId, String projectId) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        return sw360ProjectClient.importCycloneDxFromAttachmentContent(user, attachmentContentId, CommonUtils.nullToEmptyString(projectId));
    }

    /**
     * Get Projects are using release in dependencies (enable.flexible.project.release.relationship = true)
     * @param releaseId                Id of release
     * @return List<Project>
     */
    public List<Project> getProjectsUsedReleaseInDependencyNetwork(String releaseId) {
        return SW360Utils.getUsingProjectByReleaseIds(Collections.singleton(releaseId), null);
    }

    public void syncReleaseRelationNetworkAndReleaseIdToUsage(Project project, User user) throws TException {
        SW360Utils.syncReleaseRelationNetworkAndReleaseIdToUsage(project, user);
    }

    /**
     * Count the number of projects are using the releases that has releaseIds
     * @param releaseIds              Ids of Releases
     * @return int                    Number of projects
     * @throws TException
     */
    public int countProjectsByReleaseIds(Set<String> releaseIds) {
        try {
            ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
            return sw360ProjectClient.getCountByReleaseIds(releaseIds);
        } catch (TException e) {
            log.error(e.getMessage());
            return 0;
        }
    }

    public AddDocumentRequestSummary createClearingRequest(ClearingRequest clearingRequest, User sw360User, String baseUrl, String projectId) throws TException {
        ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
        String projectUrl = baseUrl + "/projects/-/project/detail/" + projectId;
        return sw360ProjectClient.createClearingRequest(clearingRequest, sw360User, projectUrl);
    }

    public Integer loadPreferredClearingDateLimit() {
        Integer limit = Optional.of(SW360Constants.PREFERRED_CLEARING_DATE_LIMIT).filter(s -> !s.isEmpty())
                .map(Integer::parseInt).orElse(0);
        // returning default value 7 (days) if variable is not set
        return limit < 1 ? 7 : limit;
    }
}

