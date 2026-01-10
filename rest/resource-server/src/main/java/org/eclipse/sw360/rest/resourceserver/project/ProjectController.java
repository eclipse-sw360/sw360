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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.apache.thrift.protocol.TSimpleJSONProtocol;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.ClearingRequestPriority;
import org.eclipse.sw360.datahandler.thrift.ClearingRequestState;
import org.eclipse.sw360.datahandler.thrift.ClearingRequestType;
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.ProjectPackageRelationship;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.ObligationStatus;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.attachments.UsageData;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfo;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoFile;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatInfo;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatVariant;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.thrift.projects.*;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationList;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationStatusInfo;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectLink;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectDTO;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.ProjectVulnerabilityRating;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityCheckStatus;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityDTO;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilitySummary;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityRatingForProject;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.component.Sw360ComponentService;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.OpenAPIPaginationHelper;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.license.Sw360LicenseService;
import org.eclipse.sw360.rest.resourceserver.licenseinfo.Sw360LicenseInfoService;
import org.eclipse.sw360.rest.resourceserver.moderationrequest.Sw360ModerationRequestService;
import org.eclipse.sw360.rest.resourceserver.packages.PackageController;
import org.eclipse.sw360.rest.resourceserver.packages.SW360PackageService;
import org.eclipse.sw360.rest.resourceserver.release.ReleaseController;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.eclipse.sw360.rest.resourceserver.vendor.Sw360VendorService;
import org.eclipse.sw360.rest.resourceserver.vulnerability.Sw360VulnerabilityService;
import org.eclipse.sw360.rest.resourceserver.vulnerability.VulnerabilityController;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.GsonJsonParser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.attachments.LicenseInfoUsage;
import org.eclipse.sw360.datahandler.thrift.attachments.SourcePackageUsage;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.sw360.datahandler.common.CommonUtils.isNullEmptyOrWhitespace;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyList;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;
import static org.eclipse.sw360.datahandler.common.CommonUtils.wrapThriftOptionalReplacement;
import static org.eclipse.sw360.datahandler.common.WrappedException.wrapTException;
import static org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer.REPORT_FILENAME_MAPPING;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
public class ProjectController implements RepresentationModelProcessor<RepositoryLinksResource> {
    private static final String CREATED_BY = "createdBy";
    private static final String ATTACHMENT_TYPE = "attachmentType";
    private static final String ATTACHMENTS = "attachments";
    public static final String PROJECTS_URL = "/projects";
    public static final String SW360_ATTACHMENT_USAGES = "sw360:attachmentUsages";
    private static final Logger log = LogManager.getLogger(ProjectController.class);
    private static final TSerializer THRIFT_JSON_SERIALIZER = getJsonSerializer();
    private static final ImmutableMap<Project._Fields, String> mapOfFieldsTobeEmbedded = ImmutableMap.<Project._Fields, String>builder()
            .put(Project._Fields.CLEARING_TEAM, "clearingTeam")
            .put(Project._Fields.EXTERNAL_URLS, "externalUrls")
            .put(Project._Fields.MODERATORS, "sw360:moderators")
            .put(Project._Fields.CONTRIBUTORS,"sw360:contributors")
            .put(Project._Fields.ATTACHMENTS,"sw360:attachments").build();
    private static final ImmutableMap<Project._Fields, String> mapOfProjectFieldsToRequestBody = ImmutableMap.<Project._Fields, String>builder()
            .put(Project._Fields.VISBILITY, "visibility")
            .put(Project._Fields.RELEASE_ID_TO_USAGE, "linkedReleases")
            .put(Project._Fields.RELEASE_RELATION_NETWORK, "dependencyNetwork").build();
    private static final ImmutableMap<String, String> RESPONSE_BODY_FOR_MODERATION_REQUEST = ImmutableMap.<String, String>builder()
            .put("message", "Moderation request is created").build();
    private static final ImmutableMap<String, String> RESPONSE_BODY_FOR_MODERATION_REQUEST_WITH_COMMIT = ImmutableMap.<String, String>builder()
            .put("message", "Unauthorized user or empty commit message passed.").build();
    private static final List<String> enumReleaseRelationshipValues = Stream.of(ReleaseRelationship.values())
            .map(ReleaseRelationship::name)
            .collect(Collectors.toList());
    private static final List<String> enumMainlineStateValues = Stream.of(MainlineState.values())
            .map(MainlineState::name)
            .collect(Collectors.toList());

    @NonNull
    private final Sw360ProjectService projectService;

    @NonNull
    private final SW360PackageService packageService;

    @NonNull
    private final Sw360UserService userService;

    @NonNull
    private final Sw360ReleaseService releaseService;

    @NonNull
    private final Sw360LicenseService licenseService;

    @NonNull
    private final Sw360VulnerabilityService vulnerabilityService;

    @NonNull
    private final Sw360AttachmentService attachmentService;

    @NonNull
    private final Sw360LicenseInfoService licenseInfoService;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    private final Sw360ComponentService componentService;

    @NonNull
    private final Sw360ModerationRequestService moderationRequestService;

    @NonNull
    private final Sw360VendorService vendorService;

    @NonNull
    private final com.fasterxml.jackson.databind.Module sw360Module;

    @Operation(
            summary = "List all of the service's projects.",
            description = "List all of the service's projects with various filters.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL, method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<Project>>> getProjectsForUser(
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable,
            @Parameter(description = "The name of the project")
            @RequestParam(value = "name", required = false) String name,
            @Parameter(description = "The type of the project")
            @RequestParam(value = "type", required = false) String projectType,
            @Parameter(description = "The group of the project")
            @RequestParam(value = "group", required = false) String group,
            @Parameter(description = "The tag of the project")
            @RequestParam(value = "tag", required = false) String tag,
            @Parameter(description = "Flag to get projects with all details.")
            @RequestParam(value = "allDetails", required = false) boolean allDetails,
            @Parameter(description = "The version of the project")
            @RequestParam(value = "version", required = false) String version,
            @Parameter(description = "The projectResponsible of the project")
            @RequestParam(value = "projectResponsible", required = false) String projectResponsible,
            @Parameter(description = "The state of the project")
            @RequestParam(value = "state", required = false) ProjectState projectState,
            @Parameter(description = "The clearingStatus of the project")
            @RequestParam(value = "clearingStatus", required = false) ProjectClearingState projectClearingState,
            @Parameter(description = "The additionalData of the project")
            @RequestParam(value = "additionalData", required = false) String additionalData,
            @Parameter(description = "List project by lucene search")
            @RequestParam(value = "luceneSearch", required = false) boolean luceneSearch,
            HttpServletRequest request
    ) throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<Project> sw360Projects = new ArrayList<>();
        Map<PaginationData, List<Project>> paginatedProjects = null;

        Map<String, Set<String>> filterMap = getFilterMap(tag, projectType, group, version, projectResponsible, projectState, projectClearingState,
                additionalData);
        if (CommonUtils.isNotNullEmptyOrWhitespace(name)) {
            Set<String> values = Collections.singleton(name);
            filterMap.put(Project._Fields.NAME.getFieldName(), values);
        }

        if (luceneSearch && !filterMap.isEmpty()) {
            if (filterMap.containsKey(Project._Fields.NAME.getFieldName())) {
                Set<String> values = filterMap.get(Project._Fields.NAME.getFieldName()).stream()
                        .map(NouveauLuceneAwareDatabaseConnector::prepareWildcardQuery)
                        .collect(Collectors.toSet());
                filterMap.put(Project._Fields.NAME.getFieldName(), values);
            }

            paginatedProjects = projectService.refineSearch(filterMap, sw360User, pageable);
        } else {
            if (filterMap.isEmpty()) {
                paginatedProjects = projectService.getProjectsForUser(sw360User, pageable);
            } else {
                paginatedProjects = projectService.searchAccessibleProjectByExactValues(filterMap, sw360User, pageable);
            }
        }
        return getProjectResponse(pageable, allDetails, request, sw360User,
                sw360Projects, paginatedProjects);
    }

    private Map<String, Set<String>> getFilterMap(String tag, String projectType, String group, String version, String projectResponsible,
                                                  ProjectState projectState, ProjectClearingState projectClearingState, String additionalData) {
        Map<String, Set<String>> filterMap = new HashMap<>();
        if (CommonUtils.isNotNullEmptyOrWhitespace(tag)) {
            filterMap.put(Project._Fields.TAG.getFieldName(), CommonUtils.splitToSet(tag));
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(projectType)) {
            filterMap.put(Project._Fields.PROJECT_TYPE.getFieldName(), CommonUtils.splitToSet(projectType));
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(group)) {
            filterMap.put(Project._Fields.BUSINESS_UNIT.getFieldName(), CommonUtils.splitToSet(group));
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(version)) {
            filterMap.put(Project._Fields.VERSION.getFieldName(), CommonUtils.splitToSet(version));
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(projectResponsible)) {
            filterMap.put(Project._Fields.PROJECT_RESPONSIBLE.getFieldName(), CommonUtils.splitToSet(projectResponsible));
        }
        if (projectState!=null && CommonUtils.isNotNullEmptyOrWhitespace(projectState.name())) {
            filterMap.put(Project._Fields.STATE.getFieldName(), CommonUtils.splitToSet(projectState.name()));
        }
        if (projectClearingState!=null && CommonUtils.isNotNullEmptyOrWhitespace(projectClearingState.name())) {
            filterMap.put(Project._Fields.CLEARING_STATE.getFieldName(), CommonUtils.splitToSet(projectClearingState.name()));
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(additionalData)) {
            filterMap.put(Project._Fields.ADDITIONAL_DATA.getFieldName(), CommonUtils.splitToSet(additionalData));
        }
        return filterMap;
    }

    @NotNull
    private ResponseEntity<CollectionModel<EntityModel<Project>>> getProjectResponse(
            Pageable pageable, boolean allDetails, HttpServletRequest request, User sw360User,
            List<Project> sw360Projects, Map<PaginationData, List<Project>> paginatedProjects
    ) throws ResourceClassNotFoundException, PaginationParameterException, URISyntaxException {
        PaginationResult<Project> paginationResult;
        if (paginatedProjects != null) {
            sw360Projects.addAll(paginatedProjects.values().iterator().next());
            int totalCount = Math.toIntExact(paginatedProjects.keySet().stream()
                    .findFirst().map(PaginationData::getTotalRowCount).orElse(0L));
            paginationResult = restControllerHelper.paginationResultFromPaginatedList(
                    request, pageable, sw360Projects, SW360Constants.TYPE_PROJECT, totalCount);
        } else {
            paginationResult = restControllerHelper.createPaginationResult(request, pageable,
                    sw360Projects, SW360Constants.TYPE_PROJECT);
        }

        List<EntityModel<Project>> projectResources = new ArrayList<>();
        Consumer<Project> consumer = p -> {
            EntityModel<Project> embeddedProjectResource = null;
            if (!allDetails) {
                Project embeddedProject = restControllerHelper.convertToEmbeddedProject(p);
                embeddedProjectResource = EntityModel.of(embeddedProject);
            } else {
                embeddedProjectResource = createHalProjectResourceWithAllDetails(p, sw360User);
                if (embeddedProjectResource == null) {
                    return;
                }
            }
            projectResources.add(embeddedProjectResource);
        };

        paginationResult.getResources().forEach(consumer);

        CollectionModel resources;
        if (projectResources.isEmpty()) {
            resources = restControllerHelper.emptyPageResource(Project.class, paginationResult);
        } else {
            resources = restControllerHelper.generatePagesResource(paginationResult, projectResources);
        }

        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    @Operation(
            description = "List all projects associated to the user.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/myprojects", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<Project>>> getProjectsFilteredForUser(
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable,
            @Parameter(description = "Projects with current user as creator.")
            @RequestParam(value = CREATED_BY, required = false, defaultValue = "true") boolean createdBy,
            @Parameter(description = "Projects with current user as moderator.")
            @RequestParam(value = "moderator", required = false, defaultValue = "true") boolean moderator,
            @Parameter(description = "Projects with current user as contributor.")
            @RequestParam(value = "contributor", required = false, defaultValue = "true") boolean contributor,
            @Parameter(description = "Projects with current user as owner.")
            @RequestParam(value = "projectOwner", required = false, defaultValue = "true") boolean projectOwner,
            @Parameter(description = "Projects with current user as lead architect.")
            @RequestParam(value = "leadArchitect", required = false, defaultValue = "true") boolean leadArchitect,
            @Parameter(description = "Projects with current user as project responsible.")
            @RequestParam(value = "projectResponsible", required = false, defaultValue = "true") boolean projectResponsible,
            @Parameter(description = "Projects with current user as security responsible.")
            @RequestParam(value = "securityResponsible", required = false, defaultValue = "true") boolean securityResponsible,
            @Parameter(description = "Projects with state as open.")
            @RequestParam(value = "stateOpen", required = false, defaultValue = "true") boolean stateOpen,
            @Parameter(description = "Projects with state as closed.")
            @RequestParam(value = "stateClosed", required = false, defaultValue = "true") boolean stateClosed,
            @Parameter(description = "Projects with state as in progress.")
            @RequestParam(value = "stateInProgress", required = false, defaultValue = "true") boolean stateInProgress,
            @Parameter(description = "Flag to get projects with all details.")
            @RequestParam(value = "allDetails", required = false) boolean allDetails,
            HttpServletRequest request
    ) throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();

        ImmutableMap<String, Boolean> userRoles = ImmutableMap.<String, Boolean>builder()
                .put(Project._Fields.CREATED_BY.toString(), createdBy)
                .put(Project._Fields.MODERATORS.toString(), moderator)
                .put(Project._Fields.CONTRIBUTORS.toString(), contributor)
                .put(Project._Fields.PROJECT_OWNER.toString(), projectOwner)
                .put(Project._Fields.LEAD_ARCHITECT.toString(), leadArchitect)
                .put(Project._Fields.PROJECT_RESPONSIBLE.toString(), projectResponsible)
                .put(Project._Fields.SECURITY_RESPONSIBLES.toString(), securityResponsible)
                .build();

        ImmutableMap<String, Boolean> clearingState = ImmutableMap.<String, Boolean>builder()
                .put(ProjectClearingState.OPEN.toString(), stateOpen)
                .put(ProjectClearingState.CLOSED.toString(), stateClosed)
                .put(ProjectClearingState.IN_PROGRESS.toString(), stateInProgress)
                .build();

        List<Project> sw360Projects = projectService.getMyProjects(sw360User, userRoles);
        sw360Projects = projectService.getWithFilledClearingStatus(sw360Projects, clearingState);

        return getProjectResponse(pageable, allDetails, request, sw360User,
                sw360Projects, null);
    }

    @Operation(
            description = "Get all releases of license clearing.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/licenseClearing", method = RequestMethod.GET)
    public ResponseEntity<HalResource<Project>> licenseClearing(
            @Parameter(description = "Project ID", example = "376576")
            @PathVariable("id") String id,
            @Parameter(description = "Get the transitive releases.")
            @RequestParam(value = "transitive", required = true) boolean transitive,
            @Parameter(description = "Type of the component")
            @RequestParam(value = "componentType", required = false) List<ComponentType> componentType,
            @Parameter(description = "Type of release relation")
            @RequestParam(value = "releaseRelation", required = false) ReleaseRelationship releaseRelation,
            @Parameter(description = "Clearing state of the release")
            @RequestParam(value = "clearingState", required = false) List<ClearingState> clearingState
    ) throws TException {

        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        Project sw360Project = projectService.getProjectForUserById(id, sw360User);

        //check the below condition when releaseRelation is not null
        if (releaseRelation != null) {
            Map<String, ProjectReleaseRelationship> filteredReleaseIdToUsage = sw360Project.getReleaseIdToUsage().entrySet().stream()
                    .filter(entry -> entry.getValue().getReleaseRelation() == releaseRelation)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue
                    ));
            sw360Project.setReleaseIdToUsage(filteredReleaseIdToUsage);
        }

        final Set<String> releaseIds = projectService.getReleaseIds(id, sw360User, transitive);
        List<Release> releases = projectService.getFilteredReleases(releaseIds, sw360User, clearingState, componentType, releaseService);

        // Extract all release IDs from the provided list
        Set<String> validReleaseIds = releases.stream()
                .map(Release::getId)
                .collect(Collectors.toSet());

        // Filter the releaseIdToUsage map
        Map<String, ProjectReleaseRelationship> filteredReleaseIdData = sw360Project.getReleaseIdToUsage().entrySet().stream()
                .filter(entry -> validReleaseIds.contains(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
        sw360Project.setReleaseIdToUsage(filteredReleaseIdData);

        List<EntityModel<Release>> releaseList = releases.stream().map(sw360Release -> wrapTException(() -> {
            final Release embeddedRelease = restControllerHelper.convertToEmbeddedLinkedRelease(sw360Release);
            final HalResource<Release> releaseResource = restControllerHelper.addEmbeddedReleaseLinks(embeddedRelease);
            return releaseResource;
        })).collect(Collectors.toList());

        HalResource<Project> userHalResource = createHalLicenseClearing(sw360Project, releaseList);
        return new ResponseEntity<>(userHalResource, HttpStatus.OK);
    }

    @Operation(
            description = "Get a single project.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}", method = RequestMethod.GET)
    public ResponseEntity<EntityModel<Project>> getProject(
            @Parameter(description = "Project ID", example = "376576")
            @PathVariable("id") String id
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Project sw360Project = projectService.getProjectForUserById(id, sw360User);
        HalResource<Project> userHalResource = createHalProject(sw360Project, sw360User);
        return new ResponseEntity<>(userHalResource, HttpStatus.OK);
    }

    @Operation(
            description = "Get a package with project id.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/packages", method = RequestMethod.GET)
    public ResponseEntity<List<HalResource<Project>>> getPackagesByProjectId(
            @Parameter(description = "Project ID", example = "376576")
            @PathVariable("id") String id
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Project sw360Project = projectService.getProjectForUserById(id, sw360User);
        List<HalResource<Package>> halPackages = new ArrayList<>();
        if (sw360Project.getPackageIdsSize() > 0) {
            for (Map.Entry<String, ProjectPackageRelationship> entry : sw360Project.getPackageIds().entrySet()) {
                String packageId = entry.getKey();
                Package sw360Package = packageService.getPackageForUserById(packageId);
                HalResource<Package> halPackage = createHalPackage(sw360Package, sw360User);
                halPackages.add(halPackage);
            }
        }
        return new ResponseEntity(halPackages, HttpStatus.OK);
    }

    private HalResource<Package> createHalPackage(Package sw360Package, User sw360User) throws TException {
        HalResource<Package> halPackage = new HalResource<>(sw360Package);
        User packageCreator = restControllerHelper.getUserByEmail(sw360Package.getCreatedBy());
        String linkedRelease = sw360Package.getReleaseId();

        restControllerHelper.addEmbeddedUser(halPackage, packageCreator, "createdBy");
        if (CommonUtils.isNotNullEmptyOrWhitespace(linkedRelease)) {
            Release release = releaseService.getReleaseForUserById(linkedRelease, sw360User);

            if (release != null) {
                restControllerHelper.addEmbeddedSingleRelease(halPackage, release);
            } else {
                log.warn("Release not found for ID: {}", linkedRelease);
            }
        }
        if (sw360Package.getModifiedBy() != null) {
            restControllerHelper.addEmbeddedModifiedBy(halPackage, sw360User, "modifiedBy");
        }
        if (sw360Package.getVendorId() != null) {
            Vendor vendor = vendorService.getVendorById(sw360Package.getVendorId());
            if (vendor != null) {
                Vendor vendorHalResource = restControllerHelper.convertToEmbeddedVendor(vendor);
                halPackage.addEmbeddedResource("sw360:vendors", vendorHalResource);
            } else {
                log.warn("Vendor not found for ID: {}", sw360Package.getVendorId());
            }
        }
        return halPackage;
    }

    @Operation(
            description = "Get linked projects of a single project.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/linkedProjects", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel>> getLinkedProject(
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable,
            @Parameter(description = "Project ID", example = "376576")
            @PathVariable("id") String id,
            @RequestParam(value = "transitive", required = false) boolean isTransitive,
            HttpServletRequest request
    ) throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Project sw360Proj = projectService.getProjectForUserById(id, sw360User);
        final Set<String> projectIdsInBranch = new HashSet<>();

        Map<String, ProjectProjectRelationship> linkedProjects = sw360Proj.getLinkedProjects();
        List<String> keys = linkedProjects != null ? new ArrayList<>(linkedProjects.keySet()) : new ArrayList<>();
        List<Project> projects = keys.stream().map(projId -> wrapTException(() -> {
            final Project sw360Project = projectService.getProjectForUserById(projId, sw360User);
            return sw360Project;
        })).collect(Collectors.toList());

        PaginationResult<Project> paginationResult = restControllerHelper.createPaginationResult(request, pageable,
                projects, SW360Constants.TYPE_PROJECT);

        final List<EntityModel<Project>> projectResources = paginationResult.getResources().stream()
                .map(sw360Project -> wrapTException(() -> {
                    final Project embeddedProject = restControllerHelper.convertToEmbeddedLinkedProject(sw360Project);
                    final HalResource<Project> projectResource = new HalResource<>(embeddedProject);
                    if (isTransitive) {
                        projectService.addEmbeddedLinkedProject(sw360Project, sw360User, projectResource,
                                projectIdsInBranch);
                    }
                    return projectResource;
                })).collect(Collectors.toList());

        CollectionModel resources;
        if (projectResources.size() == 0) {
            resources = restControllerHelper.emptyPageResource(Project.class, paginationResult);
        } else {
            resources = restControllerHelper.generatePagesResource(paginationResult, projectResources);
        }

        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    @Operation(
            description = "Get releases of linked projects of a single project.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/linkedProjects/releases", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<Release>>> getReleasesOfLinkedProject(
            @Parameter(description = "Project ID", example = "376576")
            @PathVariable("id") String id,
            HttpServletRequest request,
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable
    ) throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<Map<String, String>> result = new ArrayList<>();
        final Set<String> directReleaseIds = projectService.getReleaseIds(id, sw360User, false);
        final Set<String> allReleaseIds = projectService.getReleaseIds(id, sw360User, true);
        allReleaseIds.removeAll(directReleaseIds);

        List<Release> releases = allReleaseIds.stream().map(relId -> wrapTException(() -> {
            final Release sw360Release = releaseService.getReleaseForUserById(relId, sw360User);
            return sw360Release;
        })).collect(Collectors.toList());

        if (releases.isEmpty()) {
            CollectionModel<EntityModel<Release>> emptyModel = CollectionModel.empty();
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(emptyModel);
        }
        PaginationResult<Release> paginationResult = restControllerHelper.createPaginationResult(request, pageable, releases, SW360Constants.TYPE_RELEASE);

        final List<EntityModel<Release>> releaseResources = paginationResult.getResources().stream()
                .map(sw360Release -> wrapTException(() -> {
                    final Release embeddedRelease = restControllerHelper.convertToEmbeddedLinkedProjectsReleases(sw360Release);
                    final HalResource<Release> releaseResource = new HalResource<>(embeddedRelease);
                    return releaseResource;
                })).collect(Collectors.toList());

        CollectionModel resources = restControllerHelper.generatePagesResource(paginationResult, releaseResources);;
        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    @Operation(
            description = "Delete a single project.",
            tags = {"Projects"},
            responses = {
                    @ApiResponse(
                            responseCode = "200", description = "Project deleted."
                    ),
                    @ApiResponse(
                            responseCode = "202", description = "Request sent for moderation.",
                            content = {
                                    @Content(mediaType = "application/json",
                                            examples = @ExampleObject(
                                                    value = "{\"message\": \"Moderation request is created\"}"
                                            ))
                            }
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "The project is used as a linked project. Cannot delete it."
                    ),
                    @ApiResponse(
                            responseCode = "500", description = "Failed to delete project."
                    )
            }
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity deleteProject(
            @Parameter(description = "Project ID")
            @PathVariable("id") String id,
            @Parameter(description = "Comment message")
            @RequestParam(value = "comment", required = false) String comment
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        boolean isWriteAllowed = restControllerHelper
                .isWriteActionAllowed(projectService.getProjectForUserById(id, sw360User), sw360User);
        sw360User.setCommentMadeDuringModerationRequest(comment);
        if (!isWriteAllowed && (comment == null || comment.isBlank())) {
            return ResponseEntity.badRequest().body(RESPONSE_BODY_FOR_MODERATION_REQUEST_WITH_COMMIT);
        }
        RequestStatus requestStatus = projectService.deleteProject(id, sw360User);
        if (requestStatus == RequestStatus.SUCCESS) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else if (requestStatus == RequestStatus.IN_USE) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        } else if (requestStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity<>(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        } else {
            throw new SW360Exception("Something went wrong.");
        }
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            description = "Create a project.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL, method = RequestMethod.POST)
    public ResponseEntity createProject(
            @Parameter(schema = @Schema(implementation = Project.class))
            @RequestBody Map<String, Object> reqBodyMap
    ) throws URISyntaxException, TException {
        Project project = convertToProject(reqBodyMap);
        if (project.getReleaseIdToUsage() != null) {

            Map<String, ProjectReleaseRelationship> releaseIdToUsage = new HashMap<>();
            Map<String, ProjectReleaseRelationship> oriReleaseIdToUsage = project.getReleaseIdToUsage();
            for (String releaseURIString : oriReleaseIdToUsage.keySet()) {
                URI releaseURI = new URI(releaseURIString);
                String path = releaseURI.getPath();
                String releaseId = path.substring(path.lastIndexOf('/') + 1);
                releaseIdToUsage.put(releaseId, oriReleaseIdToUsage.get(releaseURIString));
            }
            project.setReleaseIdToUsage(releaseIdToUsage);
        }
        project.unsetReleaseRelationNetwork();

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();

        if (SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            projectService.syncReleaseRelationNetworkAndReleaseIdToUsage(project, sw360User);
        }

        project = projectService.createProject(project, sw360User);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(project.getId()).toUri();

        HalResource<Project> halResource = createHalProject(project, sw360User);
        return ResponseEntity.created(location).body(halResource);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            description = "Create a duplicate project.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/duplicate/{id}", method = RequestMethod.POST)
    public ResponseEntity<EntityModel<Project>> createDuplicateProject(
            @Parameter(description = "Project ID to copy.")
            @PathVariable("id") String id,
            @Parameter(schema = @Schema(implementation = Project.class))
            @RequestBody Map<String, Object> reqBodyMap
    ) throws TException {
        if (!reqBodyMap.containsKey("name") && !reqBodyMap.containsKey("version")) {
            throw new BadRequestClientException(
                    "Field name or version should be present in request body to create duplicate of a project");
        }
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Project sw360Project = projectService.getProjectForUserById(id, user);
        Project updateProject = convertToProject(reqBodyMap);
        sw360Project = this.restControllerHelper.updateProject(sw360Project, updateProject, reqBodyMap,
                mapOfProjectFieldsToRequestBody);
        sw360Project.unsetId();
        sw360Project.unsetRevision();
        sw360Project.unsetAttachments();
        sw360Project.unsetClearingRequestId();
        sw360Project.setClearingState(ProjectClearingState.OPEN);
        String linkedObligationId = sw360Project.getLinkedObligationId();
        sw360Project.unsetLinkedObligationId();
        Project createDuplicateProject = projectService.createProject(sw360Project, user);
        sw360Project.setLinkedObligationId(linkedObligationId);
        projectService.copyLinkedObligationsForClonedProject(createDuplicateProject, sw360Project, user);

        HalResource<Project> halResource = createHalProject(createDuplicateProject, user);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(createDuplicateProject.getId()).toUri();

        return ResponseEntity.created(location).body(halResource);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Link releases to the project.",
            description = "Pass an array of release ids to be linked as request body.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/releases", method = RequestMethod.POST)
    public ResponseEntity linkReleases(
            @Parameter(description = "Project ID.")
            @PathVariable("id") String id,
            @Parameter(description = "Array of release IDs to be linked.",
                    examples = {
                            @ExampleObject(name = "Array of IDs", value = "[\"3765276512\",\"5578999\",\"3765276513\"]"),
                            @ExampleObject(name = "Array of paths", value = "[\"/releases/5578999\"]"),
                            @ExampleObject(name = "Map with usage", value = "{\"releaseId1\":{\"releaseRelation\":\"DYNAMICALLY_LINKED\",\"mainlineState\":\"MAINLINE\"},\"releaseId2\":{\"releaseRelation\":\"STATICALLY_LINKED\"}}")
                    }
            )
            @RequestBody Object releasesInRequestBody,
            @Parameter(description = "Comment message.")
            @RequestParam(value = "comment", required = false) String comment
    ) throws URISyntaxException, TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Project updateProject = projectService.getProjectForUserById(id, user);
        user.setCommentMadeDuringModerationRequest(comment);
        if (!restControllerHelper.isWriteActionAllowed(updateProject, user) && comment == null) {
            throw new BadRequestClientException(RESPONSE_BODY_FOR_MODERATION_REQUEST_WITH_COMMIT.toString());
        }
        RequestStatus linkReleasesStatus = addOrPatchReleasesToProject(id, releasesInRequestBody, false);
        if (linkReleasesStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity<>(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Link project to an array of projects.",
            description = "Pass an array of project ids to be linked as request body.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/linkProjects", method = RequestMethod.POST)
    public ResponseEntity linkToProjects(
            @Parameter(description = "Project ID.")
            @PathVariable("id") String id,
            @Parameter(description = "Array of project IDs",
                    examples = {
                            @ExampleObject(name = "Array of IDs", value = "[\"3765276512\",\"5578999\",\"3765276513\"]"),
                            @ExampleObject(name = "Map with relation", value = "{\"projectId1\":\"CONTAINED\",\"projectId2\":\"REFERRED\"}")
                    }
            )
            @RequestBody List<String> projectIdsInRequestBody,
            @Parameter(description = "Comment message.")
            @RequestParam(value = "comment", required = false) String comment
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Project sourceProj = projectService.getProjectForUserById(id, sw360User);
        Map<String, String> responseMap = new HashMap<>();
        HttpStatus status = null;
        Set<String> alreadyLinkedIds = new HashSet<>();
        Set<String> idsSentToModerator = new HashSet<>();
        Set<String> idsWithCyclicPath = new HashSet<>();
        Set<String> linkedProjectIds = new HashSet<>();
        int count = 0;

        try {

            for(String projId : projectIdsInRequestBody){
                Project proj = projectService.getProjectForUserById(projId, sw360User);
                Map<String, ProjectProjectRelationship> linkedProject = Optional.ofNullable(proj.getLinkedProjects())
                        .orElse(new HashMap<>());

                if (linkedProject.keySet().contains(id)) {
                    alreadyLinkedIds.add(projId);
                    continue;
                }

                sw360User.setCommentMadeDuringModerationRequest(comment);
                if (!restControllerHelper.isWriteActionAllowed(proj, sw360User) && comment == null) {
                    throw new BadRequestClientException(RESPONSE_BODY_FOR_MODERATION_REQUEST_WITH_COMMIT.toString());
                } else {
                    linkedProject.put(id, new ProjectProjectRelationship(ProjectRelationship.CONTAINED)
                            .setEnableSvm(sourceProj.isEnableSvm()));
                    proj.setLinkedProjects(linkedProject);
                    String cyclicLinkedProjectPath = projectService.getCyclicLinkedProjectPath(proj, sw360User);
                    if (!CommonUtils.isNullEmptyOrWhitespace(cyclicLinkedProjectPath)) {
                        idsWithCyclicPath.add(cyclicLinkedProjectPath);
                        continue;
                    }

                    RequestStatus updatedstatus = projectService.updateProject(proj, sw360User);
                    if (updatedstatus == RequestStatus.SUCCESS) {
                        linkedProjectIds.add(projId);
                    }

                    if (updatedstatus == RequestStatus.SENT_TO_MODERATOR) {
                        idsSentToModerator.add(projId);
                    }
                }
            }

            if (!alreadyLinkedIds.isEmpty()) {
                responseMap.put("Message regarding already linked project(s)", "Project ids are: " + alreadyLinkedIds);
                status = HttpStatus.CONFLICT;
                count++;
            }
            if (!idsWithCyclicPath.isEmpty()) {
                responseMap.put("Message regarding project(s) having cyclic path",
                        "Cyclic linked project path: " + idsWithCyclicPath);
                status = HttpStatus.CONFLICT;
                count++;
            }
            if (!idsSentToModerator.isEmpty()) {
                responseMap.put("Message regarding project(s) sent to Moderator", "Project ids are: " + idsSentToModerator);
                status = HttpStatus.ACCEPTED;
                count++;
            }
            if (!linkedProjectIds.isEmpty()) {
                responseMap.put("Message regarding successfully linked project(s)", "Project ids are: " + linkedProjectIds);
                status = HttpStatus.CREATED;
                count++;
            }
            if (count > 1) {
                status = HttpStatus.CONFLICT;
            }

        } catch (TException e) {
            log.error("Error: ", e);
            throw new SW360Exception("Error fetching project");
        }

        HalResource responseResource = new HalResource(responseMap);
        return new ResponseEntity<>(responseResource, status);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Append new releases to existing releases in a project.",
            description = "Pass an array of release ids or a map of release id to usage to be linked as request body.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/releases", method = RequestMethod.PATCH)
    public ResponseEntity patchReleases(
            @Parameter(description = "Project ID.")
            @PathVariable("id") String id,
            @Parameter(description = "Array of release IDs to be linked.",
                    examples = {
                            @ExampleObject(name = "Array of IDs", value = "[\"3765276512\",\"5578999\",\"3765276513\"]"),
                            @ExampleObject(name = "Array of paths", value = "[\"/releases/5578999\"]"),
                            @ExampleObject(name = "Map with usage", value = "{\"releaseId1\":{\"releaseRelation\":\"DYNAMICALLY_LINKED\",\"mainlineState\":\"MAINLINE\"},\"releaseId2\":{\"releaseRelation\":\"STATICALLY_LINKED\"}}")
                    }
            )
            @RequestBody Object releaseURIs,
            @Parameter(description = "Comment message.")
            @RequestParam(value = "comment", required = false) String comment
    ) throws URISyntaxException, TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Project project = projectService.getProjectForUserById(id, sw360User);
        if (!restControllerHelper.isWriteActionAllowed(project, sw360User) && comment == null) {
            throw new BadRequestClientException(RESPONSE_BODY_FOR_MODERATION_REQUEST_WITH_COMMIT.toString());
        } else {
            sw360User.setCommentMadeDuringModerationRequest(comment);
            RequestStatus patchReleasesStatus = addOrPatchReleasesToProject(id, releaseURIs, true);
            if (patchReleasesStatus == RequestStatus.SENT_TO_MODERATOR) {
                return new ResponseEntity<>(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
            }
            return new ResponseEntity<>(HttpStatus.CREATED);
        }
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Add/link packages to the project.",
            description = "Pass a set of package ids to be linked as request body.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Packages are linked to the project."),
                    @ApiResponse(responseCode = "202", description = "Moderation request is created.")
            },
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/link/packages", method = RequestMethod.PATCH)
    public ResponseEntity<?> linkPackages(
            @Parameter(description = "Project ID.", example = "376576")
            @PathVariable("id") String id,
            @Parameter(description = "Set of package IDs to be linked.",
                    example = "[\"3765276512\",\"5578999\",\"3765276513\"]"
            )
            @RequestBody Set<String> packagesInRequestBody,
            @Parameter(description = "Comment message.", example = "This is new MR.")
            @RequestParam(value = "comment", required = false) String comment
    ) throws TException {
        if (!packageService.validatePackageIds(packagesInRequestBody)) {
            throw new ResourceNotFoundException("Package ID invalid!");
        }
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Project project = projectService.getProjectForUserById(id, sw360User);
        sw360User.setCommentMadeDuringModerationRequest(comment);
        if (!restControllerHelper.isWriteActionAllowed(project, sw360User) && comment == null) {
            throw new BadRequestClientException(RESPONSE_BODY_FOR_MODERATION_REQUEST_WITH_COMMIT.toString());
        } else {
            RequestStatus linkPackageStatus = linkOrUnlinkPackages(id, packagesInRequestBody, true);
            if (linkPackageStatus == RequestStatus.SENT_TO_MODERATOR) {
                return new ResponseEntity<>(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
            }
            return new ResponseEntity<>(HttpStatus.CREATED);
        }
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Remove/unlink packages from the project.",
            description = "Pass a set of package ids to be unlinked as request body.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Packages are unlinked from the project."),
                    @ApiResponse(responseCode = "202", description = "Moderation request is created.")
            },
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/unlink/packages", method = RequestMethod.PATCH)
    public ResponseEntity<?> patchPackages(
            @Parameter(description = "Project ID.", example = "376576")
            @PathVariable("id") String id,
            @Parameter(description = "Set of package IDs to be linked.",
                    example = "[\"3765276512\",\"5578999\",\"3765276513\"]"
            )
            @RequestBody Set<String> packagesInRequestBody,
            @Parameter(description = "Comment message.")
            @RequestParam(value = "comment", required = false) String comment
    ) throws TException {
        if (!packageService.validatePackageIds(packagesInRequestBody)) {
            throw new ResourceNotFoundException("Package ID invalid!");
        }
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Project project = projectService.getProjectForUserById(id, sw360User);
        sw360User.setCommentMadeDuringModerationRequest(comment);
        if (!restControllerHelper.isWriteActionAllowed(project, sw360User) && comment == null) {
            throw new BadRequestClientException(RESPONSE_BODY_FOR_MODERATION_REQUEST_WITH_COMMIT.toString());
        } else {
            RequestStatus patchPackageStatus = linkOrUnlinkPackages(id, packagesInRequestBody, false);
            if (patchPackageStatus == RequestStatus.SENT_TO_MODERATOR) {
                return new ResponseEntity<>(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
            }
            return new ResponseEntity<>(HttpStatus.CREATED);
        }
    }

    @Operation(
            description = "Get releases of a single project.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/releases", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<Release>>> getProjectReleases(
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable,
            @Parameter(description = "Project ID.")
            @PathVariable("id") String id,
            @Parameter(description = "Get the transitive releases?")
            @RequestParam(value = "transitive", required = false, defaultValue = "false") boolean transitive,
            HttpServletRequest request
    ) throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {

        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Set<String> releaseIds = projectService.getReleaseIds(id, sw360User, transitive);
        final Set<String> releaseIdsInBranch = new HashSet<>();

        List<Release> releases = releaseIds.stream().map(relId -> wrapTException(() -> {
            final Release sw360Release = releaseService.getReleaseForUserById(relId, sw360User);
            return sw360Release;
        })).collect(Collectors.toList());

        PaginationResult<Release> paginationResult = restControllerHelper.createPaginationResult(request, pageable,
                releases, SW360Constants.TYPE_RELEASE);

        final List<EntityModel<Release>> releaseResources = paginationResult.getResources().stream()
                .map(sw360Release -> wrapTException(() -> {
                    final Release embeddedRelease = restControllerHelper.convertToEmbeddedRelease(sw360Release);
                    final HalResource<Release> releaseResource = new HalResource<>(embeddedRelease);
                    if (transitive) {
                        projectService.addEmbeddedlinkedRelease(sw360Release, sw360User, releaseResource,
                                releaseService, releaseIdsInBranch);
                    }
                    return releaseResource;
                })).collect(Collectors.toList());

        CollectionModel resources;
        if (releaseResources.size() == 0) {
            resources = restControllerHelper.emptyPageResource(Release.class, paginationResult);
        } else {
            resources = restControllerHelper.generatePagesResource(paginationResult, releaseResources);
        }

        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    @Operation(
            description = "Get releases of multiple projects.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/releases", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<Release>>> getProjectsReleases(
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable,
            @Parameter(description = "List of project IDs to get release for.", example = "[\"376576\",\"376570\"]")
            @RequestBody List<String> projectIds,
            @Parameter(description = "Get the transitive releases")
            @RequestParam(value = "transitive", required = false) boolean transitive,
            @Parameter(description = "The clearing state of the release.",
                    schema = @Schema(implementation = ClearingState.class))
            @RequestParam(value = "clearingState", required = false) String clState,
            HttpServletRequest request
    ) throws URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {

        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Set<String> releaseIdsInBranch = new HashSet<>();

        Set<Release> releases = projectService.getReleasesFromProjectIds(projectIds, transitive, sw360User, releaseService);

        if (null != clState) {
            ClearingState cls = ThriftEnumUtils.stringToEnum(clState, ClearingState.class);
            releases = releases.stream().filter(rel -> rel.getClearingState().equals(cls)).collect(Collectors.toSet());
        }

        List<Release> relList = releases.stream().collect(Collectors.toList());

        PaginationResult<Release> paginationResult = restControllerHelper.createPaginationResult(request, pageable, relList, SW360Constants.TYPE_RELEASE);
        final List<EntityModel<Release>> releaseResources = paginationResult.getResources().stream()
                .map(sw360Release -> wrapTException(() -> {
                    final Release embeddedRelease = restControllerHelper.convertToEmbeddedReleaseWithDet(sw360Release);
                    final HalResource<Release> releaseResource = new HalResource<>(embeddedRelease);
                    if (transitive) {
                        projectService.addEmbeddedlinkedRelease(sw360Release, sw360User, releaseResource,
                                releaseService, releaseIdsInBranch);
                    }
                    return releaseResource;
                })).collect(Collectors.toList());

        CollectionModel resources = null;
        if (CommonUtils.isNotEmpty(releaseResources)) {
            resources = restControllerHelper.generatePagesResource(paginationResult, releaseResources);
        }

        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    @Operation(
            description = "Get all releases with ECC information of a single project.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/releases/ecc", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<Release>>> getECCsOfReleases(
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable,
            HttpServletRequest request,
            @Parameter(description = "Project ID.")
            @PathVariable("id") String id,
            @Parameter(description = "Get the transitive ECC")
            @RequestParam(value = "transitive", required = false) boolean transitive
    ) throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {

        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        List<Release> releases = new ArrayList<>();
        final Set<String> releaseIds = projectService.getReleaseIds(id, sw360User, transitive);
        for (final String releaseId : releaseIds) {
            Release sw360Release = releaseService.getReleaseForUserById(releaseId, sw360User);
            releases.add(sw360Release);
        }

        PaginationResult<Release> paginationResult = restControllerHelper.createPaginationResult(request, pageable, releases, SW360Constants.TYPE_RELEASE);
        final List<EntityModel<Release>> releaseResources = new ArrayList<>();
        for (Release rel : paginationResult.getResources()) {
            Release embeddedRelease = restControllerHelper.convertToEmbeddedRelease(rel);
            embeddedRelease.setEccInformation(rel.getEccInformation());
            final EntityModel<Release> releaseResource = EntityModel.of(embeddedRelease);
            releaseResources.add(releaseResource);
        }

        CollectionModel<EntityModel<Release>> resources;
        if (releaseIds.size() == 0) {
            resources = restControllerHelper.emptyPageResource(Release.class, paginationResult);
        } else {
            resources = restControllerHelper.generatePagesResource(paginationResult, releaseResources);
        }

        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    @Operation(
            description = "Get vulnerabilities of all projects including parent and directly linked projects.",
            tags = {"Projects"}
    )

    @RequestMapping(value = PROJECTS_URL + "/{id}/vulnerabilitySummary", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<VulnerabilitySummary>>> getAllVulnerabilities(
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable,
            HttpServletRequest request,
            @PathVariable("id") String id
    ) throws TException, PaginationParameterException, ResourceClassNotFoundException, URISyntaxException {

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Project sw360Project = projectService.getProjectForUserById(id, sw360User);

        List<VulnerabilityDTO> parentProjectVulnerabilities = vulnerabilityService.getVulnerabilitiesByProjectId(id, sw360User);

        Map<String, List<VulnerabilityDTO>> vulnerabilitiesMap = new HashMap<>();
        String parentProjectKey = sw360Project.getName() + " " + sw360Project.getVersion();
        vulnerabilitiesMap.put(parentProjectKey, parentProjectVulnerabilities);

        if (sw360Project.getLinkedProjects() != null && !sw360Project.getLinkedProjects().isEmpty()) {
            for (String linkedProjectId : sw360Project.getLinkedProjects().keySet()) {
                Project linkedProject = projectService.getProjectForUserById(linkedProjectId, sw360User);
                List<VulnerabilityDTO> linkedProjectVulnerabilities = vulnerabilityService.getVulnerabilitiesByProjectId(linkedProjectId, sw360User);
                String linkedProjectKey = linkedProject.getName() + " " + linkedProject.getVersion();
                vulnerabilitiesMap.put(linkedProjectKey, linkedProjectVulnerabilities);
            }
        }

        List<VulnerabilitySummary> newList = new ArrayList<>();
        Optional<ProjectVulnerabilityRating> projectVulnerabilityRating = wrapThriftOptionalReplacement(vulnerabilityService.getProjectVulnerabilityRatingByProjectId(id, sw360User));
        Map<String, Map<String, List<VulnerabilityCheckStatus>>> vulnerabilityIdToStatusHistory = projectVulnerabilityRating
                .map(ProjectVulnerabilityRating::getVulnerabilityIdToReleaseIdToStatus).orElseGet(HashMap::new);


        for (Map.Entry<String, List<VulnerabilityDTO>> entry : vulnerabilitiesMap.entrySet()) {
            String projectName = entry.getKey();
            List<VulnerabilityDTO> vulnerabilities = entry.getValue();

            for (VulnerabilityDTO vulnerability : vulnerabilities) {
                String comment = "", action = "new";
                Map<String, Map<String, VulnerabilityRatingForProject>> vulRatingProj = vulnerabilityService.fillVulnerabilityMetadata(vulnerability, projectVulnerabilityRating);
                vulnerability.setProjectRelevance(vulRatingProj.get(vulnerability.externalId).get(vulnerability.intReleaseId).toString());
                Map<String, List<VulnerabilityCheckStatus>> relIdToCheckStatus = vulnerabilityIdToStatusHistory.get(vulnerability.externalId);
                if(null != relIdToCheckStatus && relIdToCheckStatus.containsKey(vulnerability.intReleaseId)) {
                    List<VulnerabilityCheckStatus> checkStatus = relIdToCheckStatus.get(vulnerability.intReleaseId);
                    comment = checkStatus.get(checkStatus.size()-1).getComment();
                    action = checkStatus.get(checkStatus.size()-1).getProjectAction();
                }
                vulnerability.setComment(comment);
                vulnerability.setAction(action);
                VulnerabilitySummary summ = new VulnerabilitySummary();
                summ.setProjectName(projectName);
                summ.setExternalId(vulnerability.getExternalId());
                summ.setDescription(vulnerability.getDescription());
                summ.setTitle(vulnerability.getTitle());
                summ.setPriority(vulnerability.getPriority());
                summ.setPriorityToolTip(vulnerability.getPriorityToolTip());
                summ.setAction(vulnerability.getAction());
                summ.setComment(vulnerability.getComment());
                summ.setMatchedBy(vulnerability.getMatchedBy());
                summ.setUsedNeedle(vulnerability.getUsedNeedle());
                summ.setProjectRelevance(vulnerability.getProjectRelevance());
                summ.setIntReleaseId(vulnerability.getIntReleaseId());
                summ.setIntReleaseName(vulnerability.getIntReleaseName());
                newList.add(summ);
            }
        }

        PaginationResult<VulnerabilitySummary> paginationResult = restControllerHelper.createPaginationResult(request, pageable, newList, SW360Constants.TYPE_VULNERABILITYSUMMARY);

        final List<EntityModel<VulnerabilitySummary>> vulResources = paginationResult.getResources().stream()
                .map(sw360Vul -> wrapTException(() -> {
                    final VulnerabilitySummary embeddedVul = restControllerHelper.convertToEmbeddedVulnerabilitySumm(sw360Vul);
                    final HalResource<VulnerabilitySummary> vulResource = new HalResource<>(embeddedVul);
                    Link projectLink = linkTo(VulnerabilityController.class)
                            .slash("api/vulnerabilities/" + sw360Vul.getExternalId()).withSelfRel();
                    vulResource.add(projectLink);
                    return vulResource;
                })).collect(Collectors.toList());

        CollectionModel resources;
        if (vulResources.size() == 0) {
            resources = restControllerHelper.emptyPageResource(Project.class, paginationResult);
        } else {
            resources = restControllerHelper.generatePagesResource(paginationResult, vulResources);
        }

        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    @Operation(
            description = "Get vulnerabilities of a single project.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/vulnerabilities", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<VulnerabilityDTO>>> getVulnerabilitiesOfReleases(
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable,
            @Parameter(description = "Project ID.")
            @PathVariable("id") String id,
            @Parameter(description = "The priority of vulnerability.",
                    examples = {@ExampleObject(value = "1 - critical"), @ExampleObject(value = "2 - major")}
            )
            @RequestParam(value = "priority") Optional<String> priority,
            @Parameter(description = "The relevance of project of the vulnerability.",
                    schema = @Schema(implementation = VulnerabilityRatingForProject.class)
            )
            @RequestParam(value = "projectRelevance") Optional<String> projectRelevance,
            @Parameter(description = "The release Id of vulnerability.")
            @RequestParam(value = "releaseId") Optional<String> releaseId,
            @Parameter(description = "The external Id of vulnerability.")
            @RequestParam(value = "externalId") Optional<String> externalId,
            HttpServletRequest request
    ) throws URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final List<VulnerabilityDTO> allVulnerabilityDTOs = vulnerabilityService.getVulnerabilitiesByProjectId(id, sw360User);

        Optional<ProjectVulnerabilityRating> projectVulnerabilityRating = wrapThriftOptionalReplacement(vulnerabilityService.getProjectVulnerabilityRatingByProjectId(id, sw360User));
        Map<String, Map<String, List<VulnerabilityCheckStatus>>> vulnerabilityIdToStatusHistory = projectVulnerabilityRating
                .map(ProjectVulnerabilityRating::getVulnerabilityIdToReleaseIdToStatus).orElseGet(HashMap::new);

        final List<EntityModel<VulnerabilityDTO>> vulnerabilityResources = new ArrayList<>();
        for (final VulnerabilityDTO vulnerabilityDTO : allVulnerabilityDTOs) {
            String comment = "", action = "";
            Map<String, Map<String, VulnerabilityRatingForProject>> vulRatingProj = vulnerabilityService.fillVulnerabilityMetadata(vulnerabilityDTO, projectVulnerabilityRating);
            vulnerabilityDTO.setProjectRelevance(vulRatingProj.get(vulnerabilityDTO.externalId).get(vulnerabilityDTO.intReleaseId).toString());
            Map<String, List<VulnerabilityCheckStatus>> relIdToCheckStatus = vulnerabilityIdToStatusHistory.get(vulnerabilityDTO.externalId);
            if(null != relIdToCheckStatus && relIdToCheckStatus.containsKey(vulnerabilityDTO.intReleaseId)) {
                List<VulnerabilityCheckStatus> checkStatus = relIdToCheckStatus.get(vulnerabilityDTO.intReleaseId);
                comment = checkStatus.get(checkStatus.size()-1).getComment();
                action = checkStatus.get(checkStatus.size()-1).getProjectAction();
            }
            vulnerabilityDTO.setComment(comment);
            vulnerabilityDTO.setAction(action);
            final EntityModel<VulnerabilityDTO> vulnerabilityDTOEntityModel = EntityModel.of(vulnerabilityDTO);
            vulnerabilityResources.add(vulnerabilityDTOEntityModel);
        }

        List<String> priorityList = priority.isPresent() ? Lists.newArrayList(priority.get().split(",")) : Lists.newArrayList();
        List<String> projectRelevanceList = projectRelevance.isPresent() ? Lists.newArrayList(projectRelevance.get().split(",")) : Lists.newArrayList();
        final List<EntityModel<VulnerabilityDTO>> vulnResources = vulnerabilityResources.stream()
                .filter(vulRes -> projectRelevance.isEmpty() || projectRelevanceList.contains(vulRes.getContent().getProjectRelevance()))
                .filter(vulRes -> priority.isEmpty() || priorityList.contains(vulRes.getContent().getPriority()))
                .filter(vulRes -> !releaseId.isPresent() || vulRes.getContent().getIntReleaseId().equals(releaseId.get()))
                .filter(vulRes -> !externalId.isPresent() || vulRes.getContent().getExternalId().equals(externalId.get()))
                .collect(Collectors.toList());

        List<VulnerabilityDTO> vulDtos = vulnResources.stream().map(res -> res.getContent()).collect(Collectors.toList());
        PaginationResult<VulnerabilityDTO> paginationResult = restControllerHelper.createPaginationResult(request, pageable, vulDtos, SW360Constants.TYPE_VULNERABILITYDTO);
        List<EntityModel<VulnerabilityDTO>> paginatedVulnResources = Lists.newArrayList();
        for (VulnerabilityDTO vd: paginationResult.getResources()) {
            EntityModel<VulnerabilityDTO> vDTOEntityModel = EntityModel.of(vd);
            paginatedVulnResources.add(vDTOEntityModel);
        }
        CollectionModel resources;
        if (vulnResources.size() == 0) {
            resources = restControllerHelper.emptyPageResource(VulnerabilityDTO.class, paginationResult);
        } else {
            resources = restControllerHelper.generatePagesResource(paginationResult, paginatedVulnResources);
        }

        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    @Operation(
            description = "Patch vulnerabilities of a single project.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/vulnerabilities", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CollectionModel<EntityModel<VulnerabilityDTO>>> updateVulnerabilitiesOfReleases(
            @Parameter(description = "Project ID.")
            @PathVariable("id") String id,
            @Parameter(description = "Vulnerability list")
            @RequestBody List<VulnerabilityDTO> vulnDTOs,
            @Parameter(description = "Comment message.")
            @RequestParam(value = "comment", required = false) String comment
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Project project = projectService.getProjectForUserById(id, sw360User);
        List<VulnerabilityDTO> actualVDto = vulnerabilityService.getVulnerabilitiesByProjectId(id, sw360User);
        Set<String> actualExternalId = actualVDto.stream().map(VulnerabilityDTO::getExternalId).collect(Collectors.toSet());
        Set<String> externalIdsFromRequestDto = vulnDTOs.stream().map(VulnerabilityDTO::getExternalId).collect(Collectors.toSet());
        Set<String> commonExtIds = Sets.intersection(actualExternalId, externalIdsFromRequestDto);

        if (CommonUtils.isNullOrEmptyCollection(commonExtIds) || commonExtIds.size() != externalIdsFromRequestDto.size()) {
            throw new BadRequestClientException("External ID is not valid");
        }

        Set<String> actualReleaseIds = actualVDto.stream().map(VulnerabilityDTO::getIntReleaseId).collect(Collectors.toSet());
        Set<String> releaseIdsFromRequestDto = vulnDTOs.stream().map(VulnerabilityDTO::getIntReleaseId).collect(Collectors.toSet());
        Set<String> commonRelIds = Sets.intersection(actualReleaseIds, releaseIdsFromRequestDto);

        if(CommonUtils.isNullOrEmptyCollection(commonRelIds) || commonRelIds.size() != releaseIdsFromRequestDto.size()) {
            throw new BadRequestClientException("Release ID is not valid");
        }

        Optional<ProjectVulnerabilityRating> projectVulnerabilityRatings = wrapThriftOptionalReplacement(vulnerabilityService.getProjectVulnerabilityRatingByProjectId(id, sw360User));
        ProjectVulnerabilityRating link = updateProjectVulnerabilityRatingFromRequest(projectVulnerabilityRatings, vulnDTOs, id, sw360User);
        if (!restControllerHelper.isWriteActionAllowed(project, sw360User) && comment == null) {
            throw new BadRequestClientException(RESPONSE_BODY_FOR_MODERATION_REQUEST_WITH_COMMIT.toString());
        }

        sw360User.setCommentMadeDuringModerationRequest(comment);
        final RequestStatus requestStatus = vulnerabilityService.updateProjectVulnerabilityRating(link, sw360User);
        final List<EntityModel<VulnerabilityDTO>> vulnerabilityResources = new ArrayList<>();
        vulnDTOs.forEach(dto->{
            final EntityModel<VulnerabilityDTO> vulnerabilityDTOEntityModel = EntityModel.of(dto);
            vulnerabilityResources.add(vulnerabilityDTOEntityModel);
        });

        CollectionModel<EntityModel<VulnerabilityDTO>> resources = null;
        if (RequestStatus.SUCCESS.equals(requestStatus)) {
            resources = restControllerHelper.createResources(vulnerabilityResources);
        }
        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        if (requestStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(resources, status);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Link releases to the project with usage.",
            description = "Pass a map of release id to usage to be linked as request body.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/release/{releaseId}", method = RequestMethod.PATCH)
    public ResponseEntity<EntityModel<ProjectReleaseRelationship>> patchProjectReleaseUsage(
            @Parameter(description = "Project ID.")
            @PathVariable("id") String id,
            @Parameter(description = "Release ID.")
            @PathVariable("releaseId") String releaseId,
            @Parameter(description = "Map of release id to usage.")
            @RequestBody ProjectReleaseRelationship requestBodyProjectReleaseRelationship,
            @Parameter(description = "Comment message.")
            @RequestParam(value = "comment", required = false) String comment
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Project sw360Project = projectService.getProjectForUserById(id, sw360User);
        Map<String, ProjectReleaseRelationship> releaseIdToUsage = sw360Project.getReleaseIdToUsage();
        ProjectReleaseRelationship updatedProjectReleaseRelationship = projectService
                .updateProjectReleaseRelationship(releaseIdToUsage, requestBodyProjectReleaseRelationship, releaseId);
        if (!restControllerHelper.isWriteActionAllowed(sw360Project, sw360User) && comment == null) {
            throw new BadRequestClientException(RESPONSE_BODY_FOR_MODERATION_REQUEST_WITH_COMMIT.toString());
        }
        sw360User.setCommentMadeDuringModerationRequest(comment);
        RequestStatus updateProjectStatus = projectService.updateProject(sw360Project, sw360User);
        if (updateProjectStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        EntityModel<ProjectReleaseRelationship> updatedProjectReleaseRelationshipResource = EntityModel.of(
                updatedProjectReleaseRelationship);
        return new ResponseEntity<>(updatedProjectReleaseRelationshipResource, HttpStatus.OK);
    }

    public ProjectVulnerabilityRating updateProjectVulnerabilityRatingFromRequest(
            Optional<ProjectVulnerabilityRating> projectVulnerabilityRatings, List<VulnerabilityDTO> vulDtoList,
            String projectId, User sw360User
    ) {
        Function<VulnerabilityDTO, VulnerabilityCheckStatus> fillVulnerabilityCheckStatus = vulDto -> {
            return new VulnerabilityCheckStatus().setCheckedBy(sw360User.getEmail())
                    .setCheckedOn(SW360Utils.getCreatedOn())
                    .setVulnerabilityRating(ThriftEnumUtils.stringToEnum(vulDto.getProjectRelevance(), VulnerabilityRatingForProject.class))
                    .setComment(vulDto.getComment() == null ? "" : vulDto.getComment()).setProjectAction(vulDto.getAction());

        };

        ProjectVulnerabilityRating projectVulnerabilityRating = projectVulnerabilityRatings.orElse(
                new ProjectVulnerabilityRating()
                        .setProjectId(projectId)
                        .setVulnerabilityIdToReleaseIdToStatus(new HashMap<>()));

        if (!projectVulnerabilityRating.isSetVulnerabilityIdToReleaseIdToStatus()) {
            projectVulnerabilityRating.setVulnerabilityIdToReleaseIdToStatus(new HashMap<>());
        }
        Map<String, Map<String, List<VulnerabilityCheckStatus>>> vulnerabilityIdToReleaseIdToStatus = projectVulnerabilityRating.getVulnerabilityIdToReleaseIdToStatus();

        String[] vulnerabilityIds = vulDtoList.stream().map(dto -> dto.getExternalId()).toArray(String[]::new);
        String[] releaseIds = vulDtoList.stream().map(dto -> dto.getIntReleaseId()).toArray(String[]::new);
        VulnerabilityCheckStatus[] vulStatusCheck = vulDtoList.stream().map(dt -> fillVulnerabilityCheckStatus.apply(dt)).toArray(VulnerabilityCheckStatus[]::new);

        for (int i = 0; i < vulnerabilityIds.length; i++) {
            String vulnerabilityId = vulnerabilityIds[i];
            String releaseId = releaseIds[i];

            Map<String, List<VulnerabilityCheckStatus>> releaseIdToStatus = vulnerabilityIdToReleaseIdToStatus.computeIfAbsent(vulnerabilityId, k -> new HashMap<>());
            List<VulnerabilityCheckStatus> vulnerabilityCheckStatusHistory = releaseIdToStatus.computeIfAbsent(releaseId, k -> new ArrayList<>());
            VulnerabilityCheckStatus statustoBeUpdated = vulStatusCheck[i];
            if (!vulnerabilityCheckStatusHistory.isEmpty()) {
                VulnerabilityCheckStatus latestStatusFromHistory = vulnerabilityCheckStatusHistory
                        .get(vulnerabilityCheckStatusHistory.size() - 1);
                if (StringUtils.isEmpty(statustoBeUpdated.getComment())) {
                    statustoBeUpdated.setComment(latestStatusFromHistory.getComment());
                }
                if (StringUtils.isEmpty(statustoBeUpdated.getProjectAction())) {
                    statustoBeUpdated.setProjectAction(latestStatusFromHistory.getProjectAction());
                }
                if (Objects.isNull(statustoBeUpdated.getVulnerabilityRating())) {
                    statustoBeUpdated.setVulnerabilityRating(latestStatusFromHistory.getVulnerabilityRating());
                }
            }
            vulnerabilityCheckStatusHistory.add(statustoBeUpdated);
        }

        return projectVulnerabilityRating;
    }

    @Operation(
            description = "Get license of releases of a single project.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/licenses", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<License>>> getLicensesOfReleases(
            @Parameter(description = "Project ID.")
            @PathVariable("id") String id
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        final Project project = projectService.getProjectForUserById(id, sw360User);
        final List<EntityModel<License>> licenseResources = new ArrayList<>();
        final Set<String> allLicenseIds = new HashSet<>();

        final Set<String> releaseIdToUsage = project.getReleaseIdToUsage().keySet();
        for (final String releaseId : releaseIdToUsage) {
            final Release sw360Release = releaseService.getReleaseForUserById(releaseId, sw360User);
            final Set<String> licenseIds = sw360Release.getMainLicenseIds();
            if (licenseIds != null && !licenseIds.isEmpty()) {
                allLicenseIds.addAll(licenseIds);
            }
        }
        for (final String licenseId : allLicenseIds) {
            final License sw360License = licenseService.getLicenseById(licenseId);
            final License embeddedLicense = restControllerHelper.convertToEmbeddedLicense(sw360License);
            final EntityModel<License> licenseResource = EntityModel.of(embeddedLicense);
            licenseResources.add(licenseResource);
        }

        final CollectionModel<EntityModel<License>> resources = restControllerHelper.createResources(licenseResources);
        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    @Operation(
            summary = "Download license info for the project.",
            description = """
                    Set the request parameter `&template=<TEMPLATE_NAME>` for variant `REPORT` to choose \
                    specific template.

                    Combination of `generatorClassName` and `variant` possible are:

                    When `variant` is `DISCLOSURE`, `generatorClassName` can be one of: \
                    `TextGenerator`, `XhtmlGenerator` or `DISCLOSURE`.
                    When `variant` is `REPORT`, `generatorClassName` can be one of: \
                    `DocxGenerator`.""",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/licenseinfo", method = RequestMethod.GET)
    public void downloadLicenseInfo(
            @Parameter(description = "Project ID.", example = "376576")
            @PathVariable("id") String id,
            @Parameter(description = "Output generator class",
                    schema = @Schema(type = "string",
                            allowableValues = {"DocxGenerator", "XhtmlGenerator",
                                    "TextGenerator"}
                    ))
            @RequestParam("generatorClassName") String generatorClassName,
            @Parameter(description = "Variant of the report",
                    schema = @Schema(implementation = OutputFormatVariant.class))
            @RequestParam("variant") String variant,
            @Parameter(description = "The external Ids of the project", example = "376577")
            @RequestParam(value = "externalIds", required = false) String externalIds,
            @RequestParam(value = "template", required = false) String template,
            @Parameter(description = "Generate license info including all attachments of the linked releases")
            @RequestParam(value = "includeAllAttachments", required = false ) boolean includeAllAttachments,
            @Parameter(description = "Exclude release version from the license info file")
            @RequestParam(value = "excludeReleaseVersion", required = false, defaultValue = "false") boolean excludeReleaseVersion,
            HttpServletResponse response
    ) throws TException, IOException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        final Project sw360Project = projectService.getProjectForUserById(id, sw360User);
        List<ProjectLink> mappedProjectLinks = new ArrayList<>();

        if (includeAllAttachments) {
            mappedProjectLinks = projectService.createLinkedProjects(sw360Project,
                    projectService.filterAndSortAllAttachments(SW360Constants.INITIAL_LICENSE_INFO_ATTACHMENT_TYPES), true, sw360User);
        } else {
            mappedProjectLinks = projectService.createLinkedProjects(sw360Project,
                    projectService.filterAndSortAttachments(SW360Constants.LICENSE_INFO_ATTACHMENT_TYPES), true, sw360User);
        }

        List<AttachmentUsage> attchmntUsg = attachmentService.getAttachemntUsages(id);

        Map<Source, Set<String>> releaseIdToExcludedLicenses = attchmntUsg.stream()
                .collect(Collectors.toMap(AttachmentUsage::getOwner,
                        x -> x.getUsageData().getLicenseInfo().getExcludedLicenseIds(), (li1, li2) -> li1));

        Map<String, Boolean> usedAttachmentContentIds = attchmntUsg.stream()
                .collect(Collectors.toMap(AttachmentUsage::getAttachmentContentId, attUsage -> {
                    if (attUsage.isSetUsageData()
                            && attUsage.getUsageData().getSetField().equals(UsageData._Fields.LICENSE_INFO)) {
                        return Boolean.valueOf(attUsage.getUsageData().getLicenseInfo().isIncludeConcludedLicense());
                    }
                    return Boolean.FALSE;
                }, (li1, li2) -> li1));

        final Map<String, Map<String, Boolean>> selectedReleaseAndAttachmentIds = new HashMap<>();
        final Map<String, Set<LicenseNameWithText>> excludedLicensesPerAttachments = new HashMap<>();


        mappedProjectLinks.forEach(projectLink -> wrapTException(() ->
                projectLink.getLinkedReleases().stream().filter(ReleaseLink::isSetAttachments).forEach(releaseLink -> {
                    String releaseLinkId = releaseLink.getId();
                    Set<String> excludedLicenseIds = releaseIdToExcludedLicenses.
                            getOrDefault(Source.releaseId(releaseLinkId),new HashSet<>());

                    if (!selectedReleaseAndAttachmentIds.containsKey(releaseLinkId)) {
                        selectedReleaseAndAttachmentIds.put(releaseLinkId, new HashMap<>());
                    }
                    final List<Attachment> attachments = nullToEmptyList(releaseLink.getAttachments());
                    Release release = componentService.getReleaseById(releaseLinkId, sw360User);
                    for (final Attachment attachment : attachments) {
                        String attachemntContentId = attachment.getAttachmentContentId();
                        if (includeAllAttachments) {
                            selectedReleaseAndAttachmentIds.get(releaseLinkId).put(attachemntContentId,
                                    false);
                        } else {
                            if (usedAttachmentContentIds.containsKey(attachemntContentId)) {
                                boolean includeConcludedLicense = usedAttachmentContentIds.get(attachemntContentId);
                                List<LicenseInfoParsingResult> licenseInfoParsingResult = licenseInfoService
                                        .getLicenseInfoForAttachment(release, sw360User, attachemntContentId, includeConcludedLicense);
                                excludedLicensesPerAttachments.put(attachemntContentId, getExcludedLicenses(excludedLicenseIds, licenseInfoParsingResult));
                                selectedReleaseAndAttachmentIds.get(releaseLinkId).put(attachemntContentId, includeConcludedLicense);
                            }
                        }
                    }
                })));

        final String projectName = sw360Project.getName();
        final String projectVersion = sw360Project.getVersion();
        final String timestamp = SW360Utils.getCreatedOnTime().replaceAll("\\s", "_").replace(":", "_");
        String outputGeneratorClassNameWithVariant = generatorClassName+"::"+variant;
        final OutputFormatInfo outputFormatInfo = licenseInfoService.getOutputFormatInfoForGeneratorClass(generatorClassName);
        final String filename = String.format("%s-%s%s-%s.%s", Strings.nullToEmpty(variant).equals("DISCLOSURE") ? "LicenseInfo" : "ProjectClearingReport", projectName,
                StringUtils.isBlank(projectVersion) ? "" : "-" + projectVersion, timestamp,
                outputFormatInfo.getFileExtension());

        String fileName = "";
        if (CommonUtils.isNotNullEmptyOrWhitespace(template) && CommonUtils.isNotNullEmptyOrWhitespace(REPORT_FILENAME_MAPPING)) {
            Map<String, String> orgToTemplate = Arrays.stream(REPORT_FILENAME_MAPPING.split(","))
                    .collect(Collectors.toMap(k -> k.split(":")[0], v -> v.split(":")[1]));
            fileName = orgToTemplate.get(template);
        }

        final LicenseInfoFile licenseInfoFile = licenseInfoService.getLicenseInfoFile(sw360Project, sw360User,
                outputGeneratorClassNameWithVariant, selectedReleaseAndAttachmentIds, excludedLicensesPerAttachments,
                externalIds, fileName, excludeReleaseVersion);
        byte[] byteContent = licenseInfoFile.bufferForGeneratedOutput().array();
        response.setContentType(outputFormatInfo.getMimeType());
        response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
        FileCopyUtils.copy(byteContent, response.getOutputStream());
    }

    private Set<LicenseNameWithText> getExcludedLicenses(Set<String> excludedLicenseIds,
                                                         List<LicenseInfoParsingResult> licenseInfoParsingResult) {

        Predicate<LicenseNameWithText> filteredLicense = licenseNameWithText -> excludedLicenseIds
                .contains(licenseNameWithText.getLicenseName());
        Function<LicenseInfo, Stream<LicenseNameWithText>> streamLicenseNameWithTexts = licenseInfo -> licenseInfo
                .getLicenseNamesWithTexts().stream();
        return licenseInfoParsingResult.stream().map(LicenseInfoParsingResult::getLicenseInfo)
                .flatMap(streamLicenseNameWithTexts).filter(filteredLicense).collect(Collectors.toSet());
    }

    @Operation(
            description = "Get all attachment information of a project.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/attachments", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<Attachment>>> getProjectAttachments(
            @Parameter(description = "Project ID.")
            @PathVariable("id") String id
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        final Project sw360Project = projectService.getProjectForUserById(id, sw360User);
        final CollectionModel<EntityModel<Attachment>> resources = attachmentService.getAttachmentResourcesFromList(sw360User, sw360Project.getAttachments(), Source.projectId(id));
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            description = "Update and attachment usage for project.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/attachment/{attachmentId}", method = RequestMethod.PATCH)
    public ResponseEntity<EntityModel<Attachment>> patchProjectAttachmentInfo(
            @Parameter(description = "Project ID.")
            @PathVariable("id") String id,
            @Parameter(description = "Attachment ID.")
            @PathVariable("attachmentId") String attachmentId,
            @RequestBody Attachment attachmentData,
            @Parameter(description = "Comment message.")
            @RequestParam(value = "comment", required = false) String comment
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Project sw360Project = projectService.getProjectForUserById(id, sw360User);
        Set<Attachment> attachments = nullToEmptySet(sw360Project.getAttachments());
        sw360User.setCommentMadeDuringModerationRequest(comment);
        Attachment updatedAttachment = attachmentService.updateAttachment(attachments, attachmentData, attachmentId, sw360User);
        if (!restControllerHelper.isWriteActionAllowed(sw360Project, sw360User) && comment == null) {
            throw new BadRequestClientException(RESPONSE_BODY_FOR_MODERATION_REQUEST_WITH_COMMIT.toString());
        }
        RequestStatus updateProjectStatus = projectService.updateProject(sw360Project, sw360User);
        if (updateProjectStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        EntityModel<Attachment> attachmentResource = EntityModel.of(updatedAttachment);
        return new ResponseEntity<>(attachmentResource, HttpStatus.OK);
    }

    @Operation(
            summary = "Download an attachment of a project",
            description = "Download an attachment of a project. Set the Accept-Header `application/*`. " +
                    "Only this Accept-Header is supported.",
            responses = @ApiResponse(
                    content = {@Content(mediaType = "application/*")}
            ),
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{projectId}/attachments/{attachmentId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadAttachmentFromProject(
            @Parameter(description = "Project ID.")
            @PathVariable("projectId") String projectId,
            @Parameter(description = "Attachment ID.")
            @PathVariable("attachmentId") String attachmentId,
            HttpServletResponse response
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        final Project project = projectService.getProjectForUserById(projectId, sw360User);
        this.attachmentService.downloadAttachmentWithContext(project, attachmentId, response, sw360User);
    }

    @Operation(
            description = "Download clearing reports as a zip.",
            responses = @ApiResponse(
                    content = {@Content(mediaType = "application/zip")}
            ),
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{projectId}/attachments/clearingReports", method = RequestMethod.GET, produces = "application/zip")
    public void downloadClearingReports(
            @Parameter(description = "Project ID.")
            @PathVariable("projectId") String projectId,
            HttpServletResponse response
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        final Project project = projectService.getProjectForUserById(projectId, sw360User);
        final String filename = "Clearing-Reports-" + project.getName() + ".zip";

        final Set<Attachment> attachments = nullToEmptySet(project.getAttachments());
        final Set<AttachmentContent> clearingAttachments = new HashSet<>();
        for (final Attachment attachment : attachments) {
            if (attachment.getAttachmentType().equals(AttachmentType.CLEARING_REPORT)) {
                clearingAttachments.add(attachmentService.getAttachmentContent(attachment.getAttachmentContentId()));
            }
        }

        try (InputStream attachmentStream = attachmentService.getStreamToAttachments(clearingAttachments, sw360User, project)) {
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
            FileCopyUtils.copy(attachmentStream, response.getOutputStream());
        } catch (final TException | IOException e) {
            log.error(e);
        }
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            description = "Update a project.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}", method = RequestMethod.PATCH)
    public ResponseEntity<EntityModel<Project>> patchProject(
            @Parameter(description = "Project ID.")
            @PathVariable("id") String id,
            @Parameter(description = "Updated values. Add `comment` field in the body for moderation request.", schema = @Schema(implementation = Project.class))
            @RequestBody Map<String, Object> reqBodyMap
    ) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Project sw360Project = projectService.getProjectForUserById(id, user);
        boolean editPermitted = PermissionUtils.checkEditablePermission(sw360Project.getClearingState().name(), user, reqBodyMap, sw360Project);
        if (!editPermitted) {
            log.error("No write permission for project");
            throw new AccessDeniedException("No write permission for project");
        }
        Project updateProject = convertToProject(reqBodyMap);
        updateProject.unsetReleaseRelationNetwork();
        String comment = (String) reqBodyMap.get("comment");
        user.setCommentMadeDuringModerationRequest(comment);
        if (!restControllerHelper.isWriteActionAllowed(sw360Project, user) && comment == null) {
            throw new BadRequestClientException(RESPONSE_BODY_FOR_MODERATION_REQUEST_WITH_COMMIT.toString());
        } else {
            sw360Project = this.restControllerHelper.updateProject(sw360Project, updateProject, reqBodyMap,
                    mapOfProjectFieldsToRequestBody);
            if (SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP
                    && updateProject.getReleaseIdToUsage() != null) {
                sw360Project.unsetReleaseRelationNetwork();
                projectService.syncReleaseRelationNetworkAndReleaseIdToUsage(sw360Project, user);
            }
            RequestStatus updateProjectStatus = projectService.updateProject(sw360Project, user);
            if (updateProjectStatus == RequestStatus.DUPLICATE_ATTACHMENT) {
                throw new RuntimeException("Duplicate attachment detected while updating project.");
            }
            HalResource<Project> userHalResource = createHalProject(sw360Project, user);
            if (updateProjectStatus == RequestStatus.SENT_TO_MODERATOR) {
                return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
            }
            return new ResponseEntity<>(userHalResource, HttpStatus.OK);
        }
    }

    @Operation(
            description = "Add attachments to a project.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{projectId}/attachments", method = RequestMethod.POST, consumes = {"multipart/mixed", "multipart/form-data"})
    public ResponseEntity<HalResource> addAttachmentToProject(
            @Parameter(description = "Project ID.")
            @PathVariable("projectId") String projectId,
            @Parameter(description = "Files to attach")
            @RequestParam("file") MultipartFile[] files,
            @Parameter(description = "Attachments descriptions")
            @RequestParam("attachments") String attachmentsJson,
            @Parameter(description = "Comment message.")
            @RequestParam(value = "comment", required = false) String comment,
            HttpServletRequest request
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Project project = projectService.getProjectForUserById(projectId, sw360User);

        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> attachmentsList;
        try {
            attachmentsList = objectMapper.readValue(attachmentsJson, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to parse attachments JSON", e);
            throw new BadRequestClientException("Failed to parse attachments JSON");
        }

        Set<String> uploadedFilenames = new HashSet<>();
        List<Attachment> uploadedAttachments = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            String filename = file.getOriginalFilename();

            if (uploadedFilenames.contains(filename)) {
                log.error("Duplicate file detected during upload: {}", filename);
                throw new DataIntegrityViolationException("Duplicate file detected during upload: " + filename);
            }
            uploadedFilenames.add(filename);

            try {
                Map<String, Object> attachmentMap = attachmentsList.get(i);
                Attachment attachment = new Attachment();
                attachment.setFilename(filename);
                attachment.setAttachmentContentId((String) attachmentMap.get("attachmentContentId"));
                attachment.setCreatedComment((String) attachmentMap.get("createdComment"));
                setAttachmentTypeAndCheckStatus(attachment, attachmentMap);

                attachment = attachmentService.uploadAttachment(file, attachment, sw360User);
                uploadedAttachments.add(attachment);
                project.addToAttachments(attachment);
            } catch (Exception e) {
                log.error("Failed to upload attachment: {}", filename, e);
                throw new SW360Exception("Failed to upload attachment: " + filename);
            }
        }

        Set<String> missingAttachments = projectService.verifyIfAttachmentsExist(projectId, sw360User, project);
        if (!missingAttachments.isEmpty()) {
            log.warn("Missing attachments detected: {}", missingAttachments);
            throw new DataIntegrityViolationException("Missing attachments detected");
        }

        try {
            if (!restControllerHelper.isWriteActionAllowed(project, sw360User) && comment == null) {
                throw new BadRequestClientException(RESPONSE_BODY_FOR_MODERATION_REQUEST_WITH_COMMIT.toString());
            }
            sw360User.setCommentMadeDuringModerationRequest(comment);
            RequestStatus updateStatus = projectService.updateProjectForAttachment(project, sw360User, request, null,
                    projectId);

            if (updateStatus == RequestStatus.DUPLICATE_ATTACHMENT) {
                log.error("Duplicate attachment detected while updating project: {}", projectId);

                Map<String, String> errorMessage = new HashMap<>();
                errorMessage.put("message", "Duplicate attachment detected while updating project.");
                errorMessage.put("projectId", projectId);

                return ResponseEntity.status(HttpStatus.CONFLICT).body(new HalResource<>(errorMessage));
            }

            HalResource<Project> halResource = createHalProject(project, sw360User);
            if (updateStatus == RequestStatus.SENT_TO_MODERATOR) {
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(halResource);
            }
            return ResponseEntity.ok(halResource);

        } catch (Exception e) {
            log.error("Error updating project attachments", e);
            throw new SW360Exception("Error updating project attachments");
        }
    }

    private void setAttachmentTypeAndCheckStatus(Attachment attachment, Map<String, Object> attachmentMap) throws SW360Exception {
        String attachmentTypeStr = (String) attachmentMap.get("attachmentType");
        String checkStatusStr = (String) attachmentMap.get("checkStatus");

        if (attachmentTypeStr != null && !attachmentTypeStr.isEmpty()) {
            try {
                attachment.setAttachmentType(AttachmentType.valueOf(attachmentTypeStr));
            } catch (IllegalArgumentException e) {
                throw new SW360Exception("Invalid attachmentType: " + attachmentTypeStr);
            }
        }
        if (checkStatusStr != null && !checkStatusStr.isEmpty()) {
            try {
                attachment.setCheckStatus(CheckStatus.valueOf(checkStatusStr));
            } catch (IllegalArgumentException e) {
                throw new SW360Exception("Invalid checkStatus: " + checkStatusStr);
            }
        }
    }

    @Operation(
            summary = "Get all projects corresponding to external ids.",
            description = "The request parameter supports MultiValueMap (allows to add duplicate keys with different " +
                    "values). It's possible to search for projects only by the external id key by leaving the value.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/searchByExternalIds", method = RequestMethod.GET)
    public ResponseEntity searchByExternalIds(
            @Parameter(description = "External ID map for filter.",
                    example = "{\"project-ext\": \"515432\", \"project-ext\": \"7657\", \"portal-id\": \"13319-XX3\"}"
            )
            HttpServletRequest request
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        String queryString = request.getQueryString();
        return restControllerHelper.searchByExternalIds(queryString, projectService, sw360User);
    }

    @Operation(
            description = "Get all the projects where the project is used.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/usedBy/{id}", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<Project>>> getUsedByProjectDetails(
            @Parameter(description = "Project ID to search.")
            @PathVariable("id") String id
    ) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Set<Project> sw360Projects = projectService.searchLinkingProjects(id, user);

        List<EntityModel<Project>> projectResources = new ArrayList<>();
        sw360Projects.forEach(p -> {
            Project embeddedProject = restControllerHelper.convertToEmbeddedProject(p);
            projectResources.add(EntityModel.of(embeddedProject));
        });

        CollectionModel<EntityModel<Project>> resources = restControllerHelper.createResources(projectResources);
        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Save attachment usages",
            description = "Pass an array of string in request body.",
            tags = {"Projects"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "AttachmentUsages Saved Successfully.",
                    content = {
                            @Content(mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = "{\"message\": \"AttachmentUsages Saved Successfully\"}"
                                    ))
                    }),
            @ApiResponse(
                    responseCode = "403", description = "No write permission for project.",
                    content = {
                            @Content(mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = "{\"message\": \"No write permission for project\"}"
                                    ))
                    }),
            @ApiResponse(
                    responseCode = "409", description = "Not a valid attachment type OR release does not belong to project.",
                    content = {
                            @Content(mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = "{\"message\": \"Not a valid attachment type OR release does not belong to project\"}"
                                    ))
                    }
            ),
            @ApiResponse(
                    responseCode = "500", description = "Internal Server Error.",
                    content = {
                            @Content(mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = "{\"message\": \"Saving attachment usages for project 123456 failed\"}"
                                    ))
                    }
            )
    })
    @RequestMapping(value = PROJECTS_URL + "/{id}/saveAttachmentUsages", method = RequestMethod.POST)
    public ResponseEntity<?> saveAttachmentUsages(
            @Parameter(description = "Project ID.")
            @PathVariable("id") String id,
            @Parameter(
                    description = "Map of key-value pairs where keys are one of `selected`, `deselected`, " +
                            "`selectedConcludedUsages` or `deselectedConcludedUsages`. The values are list of strings " +
                            "in format `<release_id>_<usage_type>_<attachment_content_id>`. If the usage is of type " +
                            "`licenseInfo`, you need to prepend `projectPath` with a `-`. Check the " +
                            "`selectedConcludedUsages` from example data.",
                    schema = @Schema(
                            example = """
                            {
                                "selected": [
                                    "4427a8e723ad405db63f75170ef240a2_sourcePackage_5c5d6f54ac6a4b33bcd3c5d3a8fefc43", "value2"
                                ],
                                "deselected": [
                                    "de213309ba0842ac8a7251bf27ea8f36_manuallySet_eec66c3465f64f0292dfc2564215c681", "value2"
                                ],
                                "selectedConcludedUsages": [
                                    "<project_id>-de213309ba0842ac8a7251bf27ea8f36_licenseInfo_eec66c3465f64f0292dfc2564215c681",
                                    "<parent_proj>:<sub_proj>:<sub_sub_proj>-<release_id>_licenseInfo_<attachment_content_id>",
                                    "value3"
                                ],
                                "deselectedConcludedUsages": [
                                    "b43a13409ba08b1ac8a7471bf27eb1f3c-ade213309ba0842ac8a7251bf27ea8f36_licenseInfo_aeec66c3465f64f0292dfc2564215c681", "value2"
                                ]
                            }
                            """
                    )
            )
            @RequestBody Map<String, List<String>> allUsages
    ) throws TException {
        final User user = restControllerHelper.getSw360UserFromAuthentication();
        final Project project = projectService.getProjectForUserById(id, user);
        try {
            if (PermissionUtils.makePermission(project, user).isActionAllowed(RequestedAction.WRITE)) {
                Source usedBy = Source.projectId(id);
                List<String> selectedUsages = new ArrayList<>();
                List<String> deselectedUsages = new ArrayList<>();
                List<String> selectedConcludedUsages = new ArrayList<>();
                List<String> deselectedConcludedUsages = new ArrayList<>();
                List<String> changedUsages = new ArrayList<>();
                for (Map.Entry<String, List<String>> entry : allUsages.entrySet()) {
                    String key = entry.getKey();
                    List<String> list = entry.getValue();
                    switch (key) {
                        case "selected" -> selectedUsages.addAll(list);
                        case "deselected" -> deselectedUsages.addAll(list);
                        case "selectedConcludedUsages" -> selectedConcludedUsages.addAll(list);
                        case "deselectedConcludedUsages" -> deselectedConcludedUsages.addAll(list);
                    }
                }
                Set<String> totalReleaseIds = projectService.getReleaseIds(id, user, true);
                changedUsages.addAll(selectedUsages);
                changedUsages.addAll(deselectedUsages);
                boolean valid = projectService.validate(changedUsages, user, releaseService, totalReleaseIds);
                if (!valid) {
                    throw new DataIntegrityViolationException("Not a valid attachment type OR release does not belong to project");
                }
                List<AttachmentUsage> allUsagesByProject = projectService.getUsedAttachments(usedBy, null);
                List<String> savedUsages = projectService.savedUsages(allUsagesByProject);
                savedUsages.removeAll(deselectedUsages);
                deselectedUsages.addAll(selectedUsages);
                selectedUsages.addAll(savedUsages);
                deselectedConcludedUsages.addAll(selectedConcludedUsages);
                List<AttachmentUsage> deselectedUsagesFromRequest = projectService.deselectedAttachmentUsagesFromRequest(deselectedUsages, selectedUsages, deselectedConcludedUsages, selectedConcludedUsages, id);
                List<AttachmentUsage> selectedUsagesFromRequest = projectService.selectedAttachmentUsagesFromRequest(deselectedUsages, selectedUsages, deselectedConcludedUsages, selectedConcludedUsages, id);
                List<AttachmentUsage> usagesToDelete = allUsagesByProject.stream()
                        .filter(usage -> deselectedUsagesFromRequest.stream()
                                .anyMatch(projectService.isUsageEquivalent(usage)))
                        .collect(Collectors.toList());
                if (!usagesToDelete.isEmpty()) {
                    projectService.deleteAttachmentUsages(usagesToDelete);
                }
                List<AttachmentUsage> allUsagesByProjectAfterCleanUp = projectService.getUsedAttachments(usedBy, null);
                List<AttachmentUsage> usagesToCreate = selectedUsagesFromRequest.stream()
                        .filter(usage -> allUsagesByProjectAfterCleanUp.stream()
                                .noneMatch(projectService.isUsageEquivalent(usage)))
                        .collect(Collectors.toList());

                if (!usagesToCreate.isEmpty()) {
                    projectService.makeAttachmentUsages(usagesToCreate);
                }
                return new ResponseEntity<>("AttachmentUsages Saved Successfully", HttpStatus.CREATED);
            } else {
                throw new AccessDeniedException("No write permission for project");
            }
        } catch (TException e) {
            log.error(e);
            throw new SW360Exception("Saving attachment usages for project " + id + " failed");
        }
    }

    public Map<String, Integer> countMap(Collection<AttachmentType> attachmentTypes, UsageData filter, Project project, User sw360User, String id) throws TException {
        boolean projectWithSubProjects = project.getLinkedProjects() != null && !project.getLinkedProjects().isEmpty();
        List<ProjectLink> mappedProjectLinks =
                (!SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP)
                        ? projectService.createLinkedProjects(project,
                        projectService.filterAndSortAttachments(attachmentTypes), true, sw360User)
                        : projectService.createLinkedProjectsWithAllReleases(project,
                        projectService.filterAndSortAttachments(attachmentTypes), true, sw360User);

        if (!projectWithSubProjects) {
            mappedProjectLinks = mappedProjectLinks.stream()
                    .filter(projectLink -> projectLink.getId().equals(id)).collect(Collectors.toList());
        }

        Map<String, Integer> countMap = projectService.storeAttachmentUsageCount(mappedProjectLinks, filter);
        return countMap;
    }

    @Operation(
            description = "Get all attachmentUsages of the projects.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/attachmentUsage", method = RequestMethod.GET)
    public ResponseEntity attachmentUsages(
            @Parameter(description = "Project ID.")
            @PathVariable("id") String id,
            @Parameter(
                    description = "filtering attachmentUsages",
                    schema = @Schema(allowableValues = {
                            "withSourceAttachment", "withoutSourceAttachment", "withoutAttachment",
                            "withAttachment", "withCliAttachment"})
            )
            @RequestParam(value = "filter", required = false) String filter,
            @Parameter(description = "Get the transitive releases.")
            @RequestParam(value = "transitive", required = false, defaultValue = "false") boolean transitive
    ) throws TException {

        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        Project sw360Project = projectService.getProjectForUserById(id, sw360User);
        final Set<String> releaseIds = projectService.getReleaseIds(id, sw360User, transitive);
        List<Release> releases = null;
        if (filter != null) {
            releases = filterReleases(sw360User, filter, releaseIds);
        } else {
            releases = releaseIds.stream().map(relId -> wrapTException(() -> {
                final Release sw360Release = releaseService.getReleaseForUserById(relId, sw360User);
                releaseService.setComponentDependentFieldsInRelease(sw360Release, sw360User);
                return sw360Release;
            })).collect(Collectors.toList());
        }
        List<EntityModel<Release>> releaseList = releases.stream().map(sw360Release -> wrapTException(() -> {
            final Release embeddedRelease = restControllerHelper.convertToEmbeddedReleaseAttachments(sw360Release);
            final HalResource<Release> releaseResource = restControllerHelper.addEmbeddedReleaseLinks(embeddedRelease);
            return releaseResource;
        })).collect(Collectors.toList());

        List<AttachmentUsage> attachmentUsages = attachmentService.getAllAttachmentUsage(id);
        String prefix = "{\"" + SW360_ATTACHMENT_USAGES + "\":[";
        String serializedUsages = attachmentUsages.stream()
                .map(usage -> wrapTException(() -> THRIFT_JSON_SERIALIZER.toString(usage)))
                .collect(Collectors.joining(",", prefix, "]}"));
        GsonJsonParser parser = new GsonJsonParser();
        Map<String, Object> attachmentUsageMap = parser.parseMap(serializedUsages);
        List<Map<String, Object>> listOfAttachmentUsages = (List<Map<String, Object>>) attachmentUsageMap
                .get(SW360_ATTACHMENT_USAGES);
        listOfAttachmentUsages.removeIf(Objects::isNull);
        for (Map<String, Object> attachmentUsage : listOfAttachmentUsages) {
            attachmentUsage.remove("revision");
            attachmentUsage.remove("type");
            Object usageData=attachmentUsage.get("usageData");
            if (usageData != null) {
                Map<String, Object> licenseInfo = ((Map<String, Map<String, Object>>) usageData).get("licenseInfo");
                if (licenseInfo != null) {
                    Object includeConcludedLicense = licenseInfo.get("includeConcludedLicense");
                    if (includeConcludedLicense != null) {
                        if ((Double)includeConcludedLicense == 0.0) {
                            licenseInfo.put("includeConcludedLicense", false);
                        } else {
                            licenseInfo.put("includeConcludedLicense", true);
                        }
                    }
                }
            }
        }

        Collection<AttachmentType> attachmentTypes;
        UsageData type;
        List<Map<String, Object>> releaseObjMap = new ArrayList<>();
        if ("withCliAttachment".equalsIgnoreCase(filter)) {
            attachmentTypes = SW360Constants.LICENSE_INFO_ATTACHMENT_TYPES;
            type = UsageData.licenseInfo(new LicenseInfoUsage(Sets.newHashSet()));
            Map<String, Integer> count = countMap(attachmentTypes, type, sw360Project, sw360User, id);
            releaseObjMap = getReleaseObjectMapper(releaseList, count);
        } else if ("withSourceAttachment".equalsIgnoreCase(filter)) {
            attachmentTypes = SW360Constants.SOURCE_CODE_ATTACHMENT_TYPES;
            type = UsageData.sourcePackage(new SourcePackageUsage());
            Map<String, Integer> count = countMap(attachmentTypes, type, sw360Project, sw360User, id);
            releaseObjMap = getReleaseObjectMapper(releaseList, count);
        } else {
            releaseObjMap = getReleaseObjectMapper(releaseList, null);
        }
        HalResource userHalResource = attachmentUsageReleases(sw360Project, releaseObjMap, listOfAttachmentUsages);
        return new ResponseEntity<>(userHalResource, HttpStatus.OK);
    }

    private List<Map<String, Object>> getReleaseObjectMapper(List<EntityModel<Release>> releaseList, Map<String, Integer> count) {
        ObjectMapper oMapper = new ObjectMapper();
        List<Map<String, Object>> modifiedList = new ArrayList<>();
        for (EntityModel<Release> rel : releaseList) {
            Map<String, Object> relMap = oMapper.convertValue(rel, Map.class);
            if (relMap.get(ATTACHMENTS) != null) {
                for (Object attach : (ArrayList<?>) relMap.get(ATTACHMENTS)) {
                    Map<String, Object> attachmentMap = (Map<String, Object>) attach;
                    AttachmentType type = AttachmentType.valueOf((String) attachmentMap.get(ATTACHMENT_TYPE));
                    attachmentMap.replace(ATTACHMENT_TYPE, attachmentMap.get(ATTACHMENT_TYPE),
                            ThriftEnumUtils.MAP_ATTACHMENT_TYPE_SHORT_STRING.get(type));
                }
            }
            final ImmutableSet<String> fieldsToKeep = ImmutableSet.of("name", "version", "componentType", "clearingState", ATTACHMENTS);
            Map<String, Object> valueToKeep = new LinkedHashMap<>();
            Link releaseLink = null;
            if (relMap != null) {
                for (Map.Entry<String, Object> entry : relMap.entrySet()) {
                    if (entry != null && entry.getKey().equals("id")) {
                        releaseLink = linkTo(ReleaseController.class).slash("api/releases/" + entry.getValue())
                                .withSelfRel();
                    } else if (entry != null && fieldsToKeep.contains(entry.getKey())) {
                        if (entry.getKey().equals(ATTACHMENTS) && null != entry.getValue()) {
                            List<Map<String, Object>> attList = new ArrayList<>();
                            for (LinkedHashMap att : ((List<LinkedHashMap>) entry.getValue())) {
                                Map<String, Object> map = new LinkedHashMap<>();
                                map.put("attachmentContentId", att.get("attachmentContentId"));
                                map.put("filename", att.get("filename"));
                                map.put("sha1", att.get("sha1"));
                                map.put(ATTACHMENT_TYPE, att.get(ATTACHMENT_TYPE));
                                map.put(CREATED_BY, att.get(CREATED_BY));
                                map.put("createdTeam", att.get("createdTeam"));
                                map.put("createdOn", att.get("createdOn"));
                                map.put("checkStatus", att.get("checkStatus"));
                                map.put("createdComment", att.get("createdComment"));
                                map.put("checkedBy", att.get("checkedBy"));
                                map.put("checkedTeam", att.get("checkedTeam"));
                                map.put("checkedComment", att.get("checkedComment"));
                                map.put("checkedOn", att.get("checkedOn"));
                                if (count != null) {
                                    Integer attachmentCount = 0;
                                    String releaseId = relMap.get("id").toString();
                                    String attachmentId = releaseId + "_" + att.get("attachmentContentId");
                                    if (count.containsKey(attachmentId)) {
                                        attachmentCount = count.get(attachmentId);
                                    }
                                    map.put("attachmentUsageCount",attachmentCount);
                                }
                                attList.add(map);
                            }
                            valueToKeep.put(entry.getKey(), attList);
                        } else {
                            valueToKeep.put(entry.getKey(), entry.getValue());
                        }
                    }
                }
            }
            Map<String, Object> linksValeToKeep = new LinkedHashMap<>();
            linksValeToKeep.put("self", releaseLink);
            valueToKeep.put("_links", linksValeToKeep);
            modifiedList.add(valueToKeep);
        }
        return modifiedList;
    }

    public List<Release> filterReleases(User sw360User, String filter, Set<String> releaseIds) {
        List<Release> releasesSrc = new ArrayList<>();

        switch (filter) {
            case "withSourceAttachment":
                releasesSrc = releaseIds.stream().map(relId -> wrapTException(() -> {
                            final Release sw360Release = releaseService.getReleaseForUserById(relId, sw360User);
                            releaseService.setComponentDependentFieldsInRelease(sw360Release, sw360User);
                            List<Attachment> sourceAttachments = nullToEmptySet(sw360Release.getAttachments()).stream()
                                    .filter(attachment ->
                                            attachment.getAttachmentType() == AttachmentType.SOURCE ||
                                                    attachment.getAttachmentType() == AttachmentType.SOURCE_SELF)
                                    .toList();
                            Set<Attachment> sourceAttachmentsSet = new HashSet<>(sourceAttachments);
                            sw360Release.setAttachments(sourceAttachmentsSet);
                            return sourceAttachmentsSet.isEmpty() ? null : sw360Release;
                        }))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                break;
            case "withoutSourceAttachment":
                releasesSrc = releaseIds.stream().map(relId -> wrapTException(() -> {
                            final Release sw360Release = releaseService.getReleaseForUserById(relId, sw360User);
                            releaseService.setComponentDependentFieldsInRelease(sw360Release, sw360User);
                            List<Attachment> withoutSourceAttachments = nullToEmptySet(sw360Release.getAttachments()).stream()
                                    .filter(attachment ->
                                            attachment.getAttachmentType() != AttachmentType.SOURCE &&
                                                    attachment.getAttachmentType() != AttachmentType.SOURCE_SELF)
                                    .toList();
                            Set<Attachment> withoutSourceAttachmentsSet = new HashSet<>(withoutSourceAttachments);
                            sw360Release.setAttachments(withoutSourceAttachmentsSet);
                            return withoutSourceAttachmentsSet.isEmpty() ? null : sw360Release;
                        }))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                break;
            case "withoutAttachment":
                releasesSrc = releaseIds.stream().map(relId -> wrapTException(() -> {
                            final Release sw360Release = releaseService.getReleaseForUserById(relId, sw360User);
                            releaseService.setComponentDependentFieldsInRelease(sw360Release, sw360User);
                            return nullToEmptySet(sw360Release.getAttachments()).isEmpty() ? sw360Release : null;
                        }))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                break;
            case "withAttachment":
                releasesSrc = releaseIds.stream().map(relId -> wrapTException(() -> {
                            final Release sw360Release = releaseService.getReleaseForUserById(relId, sw360User);
                            releaseService.setComponentDependentFieldsInRelease(sw360Release, sw360User);
                            return nullToEmptySet(sw360Release.getAttachments()).isEmpty() ? null : sw360Release;
                        }))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                break;
            case "withCliAttachment":
                releasesSrc = releaseIds.stream().map(relId -> wrapTException(() -> {
                            final Release sw360Release = releaseService.getReleaseForUserById(relId, sw360User);
                            releaseService.setComponentDependentFieldsInRelease(sw360Release, sw360User);
                            List<Attachment> cliAttachments = nullToEmptySet(sw360Release.getAttachments()).stream()
                                    .filter(attachment -> attachment.getAttachmentType() == AttachmentType.COMPONENT_LICENSE_INFO_XML || attachment.getAttachmentType() == AttachmentType.COMPONENT_LICENSE_INFO_COMBINED)
                                    .toList();
                            Set<Attachment> cliAttachmentsSet = new HashSet<>(cliAttachments);
                            sw360Release.setAttachments(cliAttachmentsSet);
                            return cliAttachmentsSet.isEmpty() ? null : sw360Release;
                        }))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                break;
            default:
                releasesSrc = releaseIds.stream().map(relId -> wrapTException(() -> {
                            final Release sw360Release = releaseService.getReleaseForUserById(relId, sw360User);
                            releaseService.setComponentDependentFieldsInRelease(sw360Release, sw360User);
                            return sw360Release;
                        }))
                        .collect(Collectors.toList());
                break;
        }

        return releasesSrc;
    }

    private HalResource attachmentUsageReleases(Project sw360Project, List<Map<String, Object>> releases, List<Map<String, Object>> attachmentUsageMap) {
        ObjectMapper oMapper = new ObjectMapper();
        Map<String, ProjectReleaseRelationship> releaseIdToUsages = sw360Project.getReleaseIdToUsage();
        Map<String, Object> projectMap = oMapper.convertValue(sw360Project, Map.class);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("linkedProjects", projectMap.get("linkedProjects"));
        resultMap.put("releaseIdToUsage", projectMap.get("releaseIdToUsage"));

        Map<String, Object> releaseIdToUsage = (Map<String, Object>) resultMap.get("releaseIdToUsage");
        final ImmutableSet<String> fieldsToRemove = ImmutableSet.of("setCreatedBy", "setCreatedOn", "setComment", "setReleaseRelation", "setMainlineState");
        if (releaseIdToUsage != null) {
            for (Map.Entry<String, Object> entry : releaseIdToUsage.entrySet()) {
                Map<String, Object> originalValue = (Map<String, Object>) entry.getValue();
                if (originalValue != null) {
                    for (String field : fieldsToRemove) {
                        originalValue.remove(field);
                    }
                }
            }
        }
        HalResource halProject = new HalResource<>(resultMap);

        if (releaseIdToUsages != null) {
            restControllerHelper.addEmbeddedProjectAttachmentUsage(halProject, releases, attachmentUsageMap);
        }
        return halProject;
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            description = "Import SBOM in SPDX format.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/import/SBOM", method = RequestMethod.POST, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> importSBOM(
            @Parameter(description = "Type of SBOM", example = "SPDX")
            @RequestParam(value = "type", required = true) String type,
            @Parameter(description = "SBOM file")
            @RequestBody MultipartFile file
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Attachment attachment = null;
        final RequestSummary requestSummary;
        String projectId = null;
        Map<String, String> messageMap = new HashMap<>();

        if (!(type.equalsIgnoreCase("SPDX") || type.equalsIgnoreCase("CycloneDX")) || !attachmentService.isValidSbomFile(file, type)) {
            throw new BadRequestClientException("Invalid SBOM file. Only SPDX(.rdf/.xml) and CycloneDX(.json/.xml) files are supported.");
        }

        try {
            attachment = attachmentService.uploadAttachment(file, new Attachment(), sw360User);
        } catch (IOException e) {
            log.error("failed to upload attachment", e);
            throw new RuntimeException("failed to upload attachment", e);
        }

        if (type.equalsIgnoreCase("SPDX")) {
            requestSummary = projectService.importSPDX(sw360User, attachment.getAttachmentContentId());

            if (!(requestSummary.getRequestStatus() == RequestStatus.SUCCESS)) {
                throw new BadRequestClientException((requestSummary.getMessage()!=null)? requestSummary.getMessage() : "Invalid SBOM file");
            }
            projectId = requestSummary.getMessage();
        } else {
            requestSummary = projectService.importCycloneDX(sw360User, attachment.getAttachmentContentId(), "", true);

            if (requestSummary.getRequestStatus() == RequestStatus.FAILURE) {
                throw new BadRequestClientException((requestSummary.getMessage()!=null)? requestSummary.getMessage() : "Invalid SBOM file");
            }
            else if (requestSummary.getRequestStatus() == RequestStatus.ACCESS_DENIED) {
                throw new BadCredentialsException("You do not have sufficient permissions.");
            }

            String jsonMessage = requestSummary.getMessage();
            messageMap = new Gson().fromJson(jsonMessage, Map.class);
            projectId = messageMap.get("projectId");

            if (requestSummary.getRequestStatus() == RequestStatus.DUPLICATE) {
                throw new DataIntegrityViolationException("A project with same name and version already exists. The projectId is: "
                        + projectId);
            }
        }
        Project project = projectService.getProjectForUserById(projectId, sw360User);
        HalResource<Project> halResource = createHalProject(project, sw360User);
        return new ResponseEntity<HalResource<Project>>(halResource, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Import SBOM on a project.",
            description = "Import a SBOM on a project. Currently only CycloneDX(.xml/" +
                    ".json) files are supported.",
            responses = {
                    @ApiResponse(
                            responseCode = "200", description = "Project successfully imported.",
                            content = {
                                    @Content(mediaType = "application/json",
                                            schema = @Schema(implementation = Project.class))
                            }
                    ),
                    @ApiResponse(
                            responseCode = "409", description = "A project with same name and version already exists.",
                            content = {
                                    @Content(mediaType = "application/json",
                                            examples = @ExampleObject(
                                                    value = "A project with same name and version already exists. " +
                                                            "The projectId is: 376576"
                                            ))
                            }
                    ),
            },
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/import/SBOM", method = RequestMethod.POST, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> importSBOMonProject(
            @Parameter(description = "Project ID", example = "376576")
            @PathVariable(value = "id", required = true) String id,
            @Parameter(description = "SBOM file")
            @RequestBody MultipartFile file,
            @Parameter(description = "Don't overwrite existing project releases and packages while re-importing SBOM")
            @RequestParam(value = "doNotReplacePackageAndRelease", required = false) boolean doNotReplacePackageAndRelease
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Attachment attachment = null;
        final RequestSummary requestSummary;
        String projectId = null;
        Map<String, String> messageMap = new HashMap<>();

        try {
            attachment = attachmentService.uploadAttachment(file, new Attachment(), sw360User);
        } catch (IOException e) {
            log.error("failed to upload attachment", e);
            throw new RuntimeException("failed to upload attachment", e);
        }

        requestSummary = projectService.importCycloneDX(sw360User, attachment.getAttachmentContentId(), id, doNotReplacePackageAndRelease);

        if (requestSummary.getRequestStatus() == RequestStatus.FAILURE) {
            throw new BadRequestClientException((requestSummary.getMessage()!=null)? requestSummary.getMessage() : "Invalid SBOM file");
        }else if(requestSummary.getRequestStatus() == RequestStatus.ACCESS_DENIED){
            throw new BadCredentialsException("You do not have sufficient permissions.");
        }

        String jsonMessage = requestSummary.getMessage();
        messageMap = new Gson().fromJson(jsonMessage, Map.class);
        projectId = messageMap.get("projectId");

        if (requestSummary.getRequestStatus() == RequestStatus.DUPLICATE) {
            throw new DataIntegrityViolationException("A project with same name and version already exists. The projectId is: "
                    + projectId);
        }else if (requestSummary.getRequestStatus() == RequestStatus.FAILED_SANITY_CHECK){
            throw new BadRequestClientException(
                    "Project name or version present in SBOM metadata tag is not same as the current SW360 project!");
        }

        Project project = projectService.getProjectForUserById(projectId, sw360User);
        HalResource<Project> halResource = createHalProject(project, sw360User);
        return new ResponseEntity<HalResource<Project>>(halResource, HttpStatus.OK);
    }

    @Operation(
            summary = "Get a single project with dependencies network.",
            responses = {
                    @ApiResponse(
                            responseCode = "200", description = "Project successfully imported.",
                            content = {
                                    @Content(mediaType = "application/json",
                                            schema = @Schema(implementation = ProjectDTO.class))
                            }
                    ),
                    @ApiResponse(
                            responseCode = "500", description = "Project release relationship is not enabled.",
                            content = {
                                    @Content(mediaType = "application/json",
                                            examples = @ExampleObject(
                                                    value = "Please enable flexible project release relationship " +
                                                            "configuration to use this function (enable.flexible.project.release.relationship = true)"
                                            ))
                            }
                    ),
            },
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/network/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getProjectWithNetwork(
            @Parameter(description = "Project ID", example = "376576")
            @PathVariable("id") String id
    ) throws TException {
        if (!SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            throw new SW360Exception(SW360Constants.PLEASE_ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP);
        }

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Project sw360Project = projectService.getProjectForUserById(id, sw360User);
        HalResource<ProjectDTO> projectDTOHalResource = createHalProjectDTO(sw360Project, sw360User);
        return new ResponseEntity<>(projectDTOHalResource, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Create a project with dependencies network.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL+ "/network", method = RequestMethod.POST)
    public ResponseEntity createProjectWithNetwork(
            @Parameter(description = "Project with `dependencyNetwork` set.",
                    schema = @Schema(implementation = Project.class))
            @RequestBody Map<String, Object> reqBodyMap
    ) throws TException {
        if (!SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            throw new SW360Exception(SW360Constants.PLEASE_ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP);
        }

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Project project = convertToProject(reqBodyMap);
        project.unsetReleaseIdToUsage();
        try {
            addOrPatchDependencyNetworkToProject(project, reqBodyMap, ProjectOperation.CREATE);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new BadRequestClientException("Invalid dependency network format");
        } catch (SW360Exception sw360Exception) {
            log.error(sw360Exception);
            throw sw360Exception;
        }

        projectService.syncReleaseRelationNetworkAndReleaseIdToUsage(project, sw360User);

        project = projectService.createProject(project, sw360User);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(project.getId()).toUri();

        HalResource<ProjectDTO> halResource = createHalProjectDTO(project, sw360User);
        return ResponseEntity.created(location).body(halResource);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Update a project with dependencies network.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/network/{id}", method = RequestMethod.PATCH)
    public ResponseEntity<?> patchProjectWithNetwork(
            @Parameter(description = "Project ID", example = "376576")
            @PathVariable("id") String id,
            @Parameter(description = "Project with `dependencyNetwork` set. Add `comment` field in the body for moderation request.",
                    schema = @Schema(implementation = Project.class))
            @RequestBody Map<String, Object> reqBodyMap
    ) throws TException {
        if (!SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            throw new SW360Exception(SW360Constants.PLEASE_ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP);
        }

        User user = restControllerHelper.getSw360UserFromAuthentication();
        Project sw360Project = projectService.getProjectForUserById(id, user);
        Project updateProject = convertToProject(reqBodyMap);
        updateProject.unsetReleaseIdToUsage();
        sw360Project.unsetReleaseIdToUsage();

        try {
            addOrPatchDependencyNetworkToProject(updateProject, reqBodyMap, ProjectOperation.UPDATE);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new BadRequestClientException("Invalid dependency network format");
        } catch (SW360Exception sw360Exception) {
            log.error(sw360Exception);
            throw sw360Exception;
        }
        String comment = (String) reqBodyMap.get("comment");
        if (!restControllerHelper.isWriteActionAllowed(updateProject, user) && comment == null) {
            throw new BadRequestClientException(RESPONSE_BODY_FOR_MODERATION_REQUEST_WITH_COMMIT.toString());
        }
        user.setCommentMadeDuringModerationRequest(comment);
        sw360Project = this.restControllerHelper.updateProject(sw360Project, updateProject, reqBodyMap, mapOfProjectFieldsToRequestBody);
        projectService.syncReleaseRelationNetworkAndReleaseIdToUsage(sw360Project, user);

        RequestStatus updateProjectStatus = projectService.updateProject(sw360Project, user);
        if (updateProjectStatus == RequestStatus.DUPLICATE_ATTACHMENT) {
            throw new RuntimeException("Duplicate attachment detected while updating project.");
        }
        if (updateProjectStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        HalResource<ProjectDTO> projectDTOHalResource = createHalProjectDTO(sw360Project, user);
        return new ResponseEntity<>(projectDTOHalResource, HttpStatus.OK);
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ProjectController.class).slash("api" + PROJECTS_URL).withRel("projects"));
        return resource;
    }

    private HalResource<Project> createHalLicenseClearing(Project sw360Project, List<EntityModel<Release>> releases) {
        Project sw360 = new Project();
        Map<String, ProjectReleaseRelationship> releaseIdToUsage = sw360Project.getReleaseIdToUsage();
        sw360.setReleaseIdToUsage(sw360Project.getReleaseIdToUsage());
        sw360.setLinkedProjects(sw360Project.getLinkedProjects());
        sw360.setId(sw360Project.getId());
        sw360.unsetState();
        sw360.unsetProjectType();
        sw360.unsetVisbility();
        sw360.unsetSecurityResponsibles();
        HalResource<Project> halProject = new HalResource<>(sw360);
        if (releaseIdToUsage != null) {
            restControllerHelper.addEmbeddedProjectReleases(halProject, releases);
        }
        return halProject;
    }

    private HalResource<Project> createHalProject(Project sw360Project, User sw360User) throws TException {
        HalResource<Project> halProject = new HalResource<>(sw360Project);
        User projectCreator = restControllerHelper.getUserByEmail(sw360Project.getCreatedBy());
        restControllerHelper.addEmbeddedUser(halProject, projectCreator, CREATED_BY);

        Map<String, ProjectReleaseRelationship> releaseIdToUsage = sw360Project.getReleaseIdToUsage();
        if (releaseIdToUsage != null) {
            restControllerHelper.addEmbeddedReleases(halProject, releaseIdToUsage.keySet(), releaseService, sw360User);
        }

        Map<String, ProjectProjectRelationship> linkedProjects = sw360Project.getLinkedProjects();
        if (linkedProjects != null) {
            restControllerHelper.addEmbeddedProject(halProject, linkedProjects.keySet(), projectService, sw360User);
        }

        if (sw360Project.getModerators() != null) {
            Set<String> moderators = sw360Project.getModerators();
            restControllerHelper.addEmbeddedModerators(halProject, moderators);
        }

        if (sw360Project.getAttachments() != null) {
            restControllerHelper.addEmbeddedAttachments(halProject, sw360Project.getAttachments());
        }

        if(sw360Project.getLeadArchitect() != null) {
            restControllerHelper.addEmbeddedLeadArchitect(halProject, sw360Project.getLeadArchitect());
        }

        if (sw360Project.getContributors() != null) {
            Set<String> contributors = sw360Project.getContributors();
            restControllerHelper.addEmbeddedContributors(halProject, contributors);
        }

        if (sw360Project.getVendor() != null) {
            Vendor vendor = sw360Project.getVendor();
            Vendor vendorHalResource = restControllerHelper.convertToEmbeddedVendor(vendor);
            halProject.addEmbeddedResource("sw360:vendors", vendorHalResource);
            sw360Project.setVendor(null);
        }

        if (sw360Project.getPackageIdsSize() > 0) {
            restControllerHelper.addEmbeddedPackages(halProject, sw360Project.getPackageIds().keySet(), packageService);
        }

        return halProject;
    }

    private RequestStatus addOrPatchReleasesToProject(String id, Object releasesInRequestBody, boolean patch)
            throws URISyntaxException, TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Project project = projectService.getProjectForUserById(id, sw360User);
        Map<String, ProjectReleaseRelationship> releaseIdToUsage = new HashMap<>();
        if (patch) {
            releaseIdToUsage = project.getReleaseIdToUsage();
        }

        if (releasesInRequestBody instanceof List) {
            List<String> releasesAsList = (List<String>) releasesInRequestBody;
            for (String release : releasesAsList) {
                URI releaseURI = new URI(release.toString());
                String path = releaseURI.getPath();
                String releaseId = path.substring(path.lastIndexOf('/') + 1);
                releaseIdToUsage.put(releaseId,
                        new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.OPEN));
            }
        } else if (releasesInRequestBody instanceof Map) {
            Map<String, Map> releaseAsMap = (Map<String, Map>) releasesInRequestBody;
            for (Entry<String, Map> entry : releaseAsMap.entrySet()) {
                String releaseId = entry.getKey();
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                mapper.registerModule(sw360Module);
                ProjectReleaseRelationship requestBodyProjectReleaseRelationship = mapper.convertValue(entry.getValue(),
                        ProjectReleaseRelationship.class);
                ProjectReleaseRelationship actualProjectReleaseRelationship = null;

                if (patch && releaseIdToUsage.containsKey(releaseId)) {
                    actualProjectReleaseRelationship = releaseIdToUsage.get(releaseId);
                } else {
                    actualProjectReleaseRelationship = new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED,
                            MainlineState.OPEN).setCreatedBy(sw360User.getEmail())
                            .setCreatedOn(SW360Utils.getCreatedOn());
                }

                restControllerHelper.updateProjectReleaseRelationship(actualProjectReleaseRelationship,
                        requestBodyProjectReleaseRelationship);
                releaseIdToUsage.put(releaseId, actualProjectReleaseRelationship);
            }
        } else {
            throw new BadRequestClientException(
                    "Request body should be List of valid release id or map of release id to usage");
        }
        project.setReleaseIdToUsage(releaseIdToUsage);
        return projectService.updateProject(project, sw360User);
    }

    private RequestStatus linkOrUnlinkPackages(String id, Set<String> packagesInRequestBody, boolean link)
            throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Project project = projectService.getProjectForUserById(id, sw360User);
        Set<String> packageIds = new HashSet<>();
        if (project.getPackageIds() != null && !CommonUtils.isNullOrEmptyCollection(project.getPackageIds().keySet())) {
            packageIds = new HashSet<>(project.getPackageIds().keySet());
        }
        if (link) {
            packageIds.addAll(packagesInRequestBody);
        } else {
            packageIds.removeAll(packagesInRequestBody);
        }

        project.setPackageIds(packageIds.stream()
                .collect(Collectors.toMap(pkgId -> pkgId, pkgId -> {
                    ProjectPackageRelationship existing = (project.getPackageIds() != null && project.getPackageIds().get(pkgId) != null)
                            ? project.getPackageIds().get(pkgId)
                            : new ProjectPackageRelationship();
                    if (existing != null && existing.getComment() != null) {
                        ProjectPackageRelationship rel = new ProjectPackageRelationship();
                        rel.setComment(existing.getComment());
                        return rel;
                    }
                    return new ProjectPackageRelationship();
                }))
        );
        return projectService.updateProject(project, sw360User);
    }


    private HalResource<Project> createHalProjectResourceWithAllDetails(Project sw360Project, User sw360User) {
        HalResource<Project> halProject = new HalResource<>(sw360Project);
        halProject.addEmbeddedResource(CREATED_BY, sw360Project.getCreatedBy());

        Set<String> packageIds = sw360Project.getPackageIds() != null ? sw360Project.getPackageIds().keySet() : new HashSet<>();
        if (packageIds != null && !packageIds.isEmpty()) {
            for (String id : sw360Project.getPackageIds().keySet()) {
                Link packageLink = linkTo(ProjectController.class)
                        .slash("api" + PackageController.PACKAGES_URL + "/" + id).withRel("packages");
                halProject.add(packageLink);
            }
        }

        List<String> obsoleteFields = List.of("homepage", "wiki");
        for (Entry<Project._Fields, String> field : mapOfFieldsTobeEmbedded.entrySet()) {
            if (Project._Fields.EXTERNAL_URLS.equals(field.getKey())) {
                Map<String, String> externalUrls = CommonUtils
                        .nullToEmptyMap((Map<String, String>) sw360Project.getFieldValue(field.getKey()));
                restControllerHelper.addEmbeddedFields(obsoleteFields.get(0),
                        externalUrls.get(obsoleteFields.get(0)) == null ? "" : externalUrls.get(obsoleteFields.get(0)),
                        halProject);
                restControllerHelper.addEmbeddedFields(obsoleteFields.get(1),
                        externalUrls.get(obsoleteFields.get(1)) == null ? "" : externalUrls.get(obsoleteFields.get(1)),
                        halProject);
            } else {
                restControllerHelper.addEmbeddedFields(field.getValue(), sw360Project.getFieldValue(field.getKey()),
                        halProject);
            }
        }

        return halProject;
    }

    private Project convertToProject(Map<String, Object> requestBody) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(sw360Module);

        if (requestBody.containsKey(mapOfProjectFieldsToRequestBody.get(Project._Fields.VISBILITY))) {
            try {
                String visibility = (String) requestBody
                        .get(mapOfProjectFieldsToRequestBody.get(Project._Fields.VISBILITY));
                requestBody.put(mapOfProjectFieldsToRequestBody.get(Project._Fields.VISBILITY),
                        visibility.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.error("Error processing visibility field", e);
                log.error("Failed requestBody: {}", requestBody);
                throw e;
            }
        }

        if (requestBody.containsKey("linkedProjects")) {
            Map<String, Object> linkedProjects = (Map<String, Object>) requestBody.get("linkedProjects");
            linkedProjects.entrySet().stream().forEach(entry -> {
                if (entry.getValue() instanceof String) {
                    Map<String, Object> projectRelationshipMap = new HashMap<>();
                    projectRelationshipMap.put("projectRelationship", entry.getValue());
                    linkedProjects.put(entry.getKey(), projectRelationshipMap);
                }
            });
        }
        Project projectFromRequest = mapper.convertValue(requestBody, Project.class);
        Set<Attachment> attachments = attachmentService.getAttachmentsFromRequest(requestBody.get(Project._Fields.ATTACHMENTS.getFieldName()), mapper);
        projectFromRequest.setAttachments(attachments);
        return projectFromRequest;
    }

    public static TSerializer getJsonSerializer() {
        try {
            return new TSerializer(new TSimpleJSONProtocol.Factory());
        } catch (TTransportException e) {
            log.error("Error creating TSerializer", e);
        }
        return null;
    }

    @Operation(
            description = "Get project count of a user.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/projectcount", method = RequestMethod.GET)
    public void getUserProjectCount(HttpServletResponse response) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        try {
            JsonObject resultJson = new JsonObject();
            resultJson.addProperty("status", "success");
            resultJson.addProperty("count", projectService.getMyAccessibleProjectCounts(sw360User));
            response.getWriter().write(resultJson.toString());
        } catch (IOException e) {
            throw new SW360Exception(e.getMessage());
        }
    }

    @Operation(
            description = "Get the default license info header text.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/licenseInfoHeader", method = RequestMethod.GET)
    public void getLicenseInfoheaderText(HttpServletResponse response) throws TException {
        try {
            response.setContentType("application/json; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            String licenseHeaderText = projectService.getLicenseInfoHeaderText();
            JsonObject resultJson = new JsonObject();
            resultJson.addProperty("licenseInfoHeaderText", licenseHeaderText);
            response.getWriter().write(resultJson.toString());
        } catch (IOException e) {
            throw new SW360Exception(e.getMessage());
        }
    }

    @Operation(
            description = "Get license clearing info for a project.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/licenseClearingCount", method = RequestMethod.GET)
    public void getlicenseClearingCount(
            HttpServletResponse response ,
            @Parameter(description = "Project ID", example = "376521")
            @PathVariable("id") String id
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        Project sw360Project = projectService.getProjectForUserById(id, sw360User);

        Project proj = projectService.getClearingInfo(sw360Project, sw360User);
        ReleaseClearingStateSummary clearingInfo = proj.getReleaseClearingStateSummary();
        int releaseCount = clearingInfo.newRelease + clearingInfo.sentToClearingTool + clearingInfo.underClearing + clearingInfo.reportAvailable + clearingInfo.scanAvailable + clearingInfo.approved;
        int approvedCount = clearingInfo.approved;
        try {
            JsonObject row = new JsonObject();
            row.addProperty("Release Count", releaseCount);
            row.addProperty("Approved Count", approvedCount);
            response.getWriter().write(row.toString());
        } catch (IOException e) {
            throw new SW360Exception(e.getMessage());
        }
    }

    @Operation(
            description = "Get license  clearing details counts for `Clearing Detail` field " +
                    "at Administration tab of project detail page.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/clearingDetailsCount", method = RequestMethod.GET)
    public void getlicenseClearingDetailsCount(
            HttpServletResponse response ,
            @Parameter(description = "Project ID", example = "376521")
            @PathVariable("id") String id
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        Project sw360Project = projectService.getProjectForUserById(id, sw360User);

        Project proj = projectService.getClearingInfo(sw360Project, sw360User);
        ReleaseClearingStateSummary clearingInfo = proj.getReleaseClearingStateSummary();
        int releaseCount = clearingInfo.newRelease + clearingInfo.sentToClearingTool + clearingInfo.underClearing + clearingInfo.reportAvailable + clearingInfo.approved;

        try {
            JsonObject row = new JsonObject();
            row.addProperty("newClearing",clearingInfo.newRelease);
            row.addProperty("underClearing",clearingInfo.underClearing);
            row.addProperty("sentToClearingTool",clearingInfo.sentToClearingTool);
            row.addProperty("reportAvailable",clearingInfo.reportAvailable);
            row.addProperty("approved",clearingInfo.approved);
            row.addProperty("totalReleases", releaseCount);
            response.getWriter().write(row.toString());
        } catch (IOException e) {
            throw new SW360Exception(e.getMessage());
        }
    }

    @Operation(
            description = "Get license obligations data from license database.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/licenseDbObligations", method = RequestMethod.GET)
    public ResponseEntity<?> getLicObligations(
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable,
            @Parameter(description = "Project ID.")
            @PathVariable("id") String id
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        final Project sw360Project = projectService.getProjectForUserById(id, sw360User);
        if (CommonUtils.isNullOrEmptyMap(sw360Project.getReleaseIdToUsage())) {
            return new ResponseEntity<String>("No release linked to the project", HttpStatus.NO_CONTENT);
        }
        Map<String, AttachmentUsage> licenseInfoAttachmentUsage = projectService.getLicenseInfoAttachmentUsage(id);
        if(licenseInfoAttachmentUsage.size() == 0) {
            return new ResponseEntity<String>("No approved CLI or licenseInfo attachment usage present for the project", HttpStatus.NO_CONTENT);
        }
        Map<String, Set<Release>> licensesFromAttachmentUsage = projectService.getLicensesFromAttachmentUsage(licenseInfoAttachmentUsage, sw360User);
        Map<String, ObligationStatusInfo> licenseObligation = projectService.getLicenseObligationData(licensesFromAttachmentUsage, sw360User);

        Map<String, Object> responseBody = createPaginationMetadata(pageable, licenseObligation);
        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }

    private Map<String, Object> createPaginationMetadata(Pageable pageable, Map<String, ObligationStatusInfo> licenseObligation) {
        List<Map.Entry<String, ObligationStatusInfo>> entries = new ArrayList<>(licenseObligation.entrySet());
        boolean isDefaultPaged = pageable != null && pageable.getPageSize() == 20 && pageable.getPageNumber() == 0;
        boolean isPaged = pageable != null && pageable.isPaged() && !isDefaultPaged;
        int pageSize = isPaged ? pageable.getPageSize() : entries.size();
        int pageNumber = isPaged ? pageable.getPageNumber() : 0;
        int start = pageNumber * pageSize;
        int end = Math.min(start + pageSize, entries.size());

        List<Map.Entry<String, ObligationStatusInfo>> paginatedEntries = entries.subList(start, end);
        int totalPages = (int) Math.ceil((double) licenseObligation.size()/ pageSize);

        Map<String, ObligationStatusInfo> paginatedMap = new LinkedHashMap<>();
        for (Map.Entry<String, ObligationStatusInfo> entry : paginatedEntries) {
            paginatedMap.put(entry.getKey(), entry.getValue());
        }
        Map<String, Integer> pagination =  Map.of(
                "size", pageSize,
                "totalElements", licenseObligation.size(),
                "totalPages", isPaged ? totalPages : 1,
                "number", pageNumber
        );
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("page", pagination);
        responseBody.put("obligations", paginatedMap);
        return responseBody;
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "delete orphan obligations",
            description = "Pass an array of orphan obligation titles in request body.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/orphanObligation", method = RequestMethod.PATCH)
    public ResponseEntity<?> removeOrphanObligation(
            @Parameter(description = "Project ID.")
            @PathVariable("id") String id,
            @Parameter(description = "Array of orphaned obligations title",
                    examples = {
                            @ExampleObject(name = "Array of titles", value = "[\"title1\",\"title2\",\"title3\"]"),
                            @ExampleObject(name = "Map format", value = "{\"title1\":\"obligationStatus1\",\"title2\":\"obligationStatus2\"}")
                    }
            )
            @RequestBody List<String> obligationTitlesInRequestBody
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Project sw360Project = projectService.getProjectForUserById(id, sw360User);

        ObligationList obligation = new ObligationList();
        RequestStatus status = null;
        Map<String, ObligationStatusInfo> obligationStatusMap = Maps.newHashMap();

        if (CommonUtils.isNotNullEmptyOrWhitespace(sw360Project.getLinkedObligationId())) {
            obligation = projectService.getObligationData(sw360Project.getLinkedObligationId(), sw360User);
            obligationStatusMap = CommonUtils.nullToEmptyMap(obligation.getLinkedObligationStatus());
            status = projectService.removeOrphanObligations(obligationStatusMap, obligationTitlesInRequestBody, sw360Project, sw360User, obligation);
        } else {
            throw new ResourceNotFoundException("No linked obligation found for the project");
        }
        if (status == RequestStatus.SUCCESS) {
            return new ResponseEntity<>("Orphaned Obligation Removed Successfully", HttpStatus.OK);
        }
        throw new ResourceNotFoundException("Failed to Remove Orphaned Obligation");
    }

    @Operation(
            description = "Get license obligation data of project tab.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/licenseObligations", method = RequestMethod.GET)
    public ResponseEntity<Object> getLicenseObligations(
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable,
            @Parameter(description = "Project ID.")
            @PathVariable("id") String id,
            @Parameter(description = "If true, returns the license obligation data in release view. "
                    + "Otherwise, returns it in project view.")
            @RequestParam(value = "view", defaultValue = "false") boolean releaseView
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        final Project sw360Project = projectService.getProjectForUserById(id, sw360User);
        final Map<String, String> releaseIdToAcceptedCLI = Maps.newHashMap();
        List<Release> releases = new ArrayList<>();
        ObligationList obligation = new ObligationList();
        Map<String, ObligationStatusInfo> obligationStatusMap = Maps.newHashMap();
        List<String> releaseIds = new ArrayList<>(sw360Project.getReleaseIdToUsage().keySet());
        for (final String releaseId : releaseIds) {
            Release sw360Release = releaseService.getReleaseForUserById(releaseId, sw360User);
            if (sw360Release.getAttachmentsSize() > 0) {
                releases.add(sw360Release);
            }
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(sw360Project.getLinkedObligationId())) {
            obligation = projectService.getObligationData(sw360Project.getLinkedObligationId(), sw360User);
            obligationStatusMap = filterObligationsByLevel(CommonUtils.nullToEmptyMap(obligation.getLinkedObligationStatus()), null);
            releaseIdToAcceptedCLI.putAll(SW360Utils.getReleaseIdtoAcceptedCLIMappings(obligationStatusMap));
        }

        if (releaseView) {
            final List<LicenseInfoParsingResult> licenseInfoWithObligations = new ArrayList<>();
            List<LicenseInfoParsingResult> processedLicenses = projectService.processLicenseInfoWithObligations(
                    licenseInfoWithObligations, releaseIdToAcceptedCLI, releases, sw360User);
            for (Map.Entry<String, ObligationStatusInfo> entry : obligationStatusMap.entrySet()) {
                ObligationStatusInfo statusInfo = entry.getValue();
                Set<Release> limitedSet = releaseService
                        .getReleasesForUserByIds(statusInfo.getReleaseIdToAcceptedCLI().keySet());
                statusInfo.setReleases(limitedSet);
            }

            // Include obligation status in the response
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("processedLicenses", processedLicenses);
            responseBody.put("obligationStatusMap", obligationStatusMap);
            return new ResponseEntity<>(responseBody, HttpStatus.OK);
        } else {
            obligationStatusMap = projectService.setLicenseInfoWithObligations(obligationStatusMap,
                    releaseIdToAcceptedCLI, releases, sw360User);
            for (Map.Entry<String, ObligationStatusInfo> entry : obligationStatusMap.entrySet()) {
                ObligationStatusInfo statusInfo = entry.getValue();
                if(statusInfo.getStatus() == null){
                    statusInfo.setStatus(ObligationStatus.OPEN);
                }
                Set<Release> limitedSet = releaseService
                        .getReleasesForUserByIds(statusInfo.getReleaseIdToAcceptedCLI().keySet());
                statusInfo.setReleases(limitedSet);
            }

            Map<String, Object> responseBody = createPaginationMetadata(pageable, obligationStatusMap);
            HalResource<Map<String, Object>> halObligation = new HalResource<>(responseBody);
            return new ResponseEntity<>(halObligation, HttpStatus.OK);
        }
    }

    @Operation(
            description = "Get obligation data of project tab.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/obligation", method = RequestMethod.GET)
    public ResponseEntity<HalResource> getObligations(
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable,
            @Parameter(description = "Project ID.")
            @PathVariable("id") String id,
            @Parameter(description = "Obligation Level",
                    schema = @Schema(allowableValues = {"license", "project", "organization", "component"}))
            @RequestParam(value = "obligationLevel", required = true) String oblLevel
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        final Project sw360Project = projectService.getProjectForUserById(id, sw360User);
        final Map<String, String> releaseIdToAcceptedCLI = Maps.newHashMap();
        List<Release> releases = new ArrayList<>();;
        ObligationList obligation = new ObligationList();
        Map<String, ObligationStatusInfo> obligationStatusMap = Maps.newHashMap();
        Map<String, ObligationStatusInfo> oblData = Maps.newHashMap();
        Map<String, ObligationStatusInfo> filterData = Maps.newHashMap();

        List<String> releaseIds = new ArrayList<>(sw360Project.getReleaseIdToUsage().keySet());
        for (final String releaseId : releaseIds) {
            Release sw360Release = releaseService.getReleaseForUserById(releaseId, sw360User);
            if (sw360Release.getAttachmentsSize() > 0) {
                releases.add(sw360Release);
            }
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(sw360Project.getLinkedObligationId())) {
            obligation = projectService.getObligationData(sw360Project.getLinkedObligationId(), sw360User);
            obligationStatusMap = CommonUtils.nullToEmptyMap(obligation.getLinkedObligationStatus());
            oblData = projectService.setObligationsFromAdminSection(sw360User, obligationStatusMap, sw360Project, oblLevel);
        } else {
            oblData = projectService.setObligationsFromAdminSection(sw360User, new HashMap(), sw360Project, oblLevel);
        }

        if (oblLevel.equalsIgnoreCase("License")) {
            filterData = filterObligationsByLevel(obligationStatusMap, null);
            releaseIdToAcceptedCLI.putAll(SW360Utils.getReleaseIdtoAcceptedCLIMappings(filterData));
            oblData = projectService.setLicenseInfoWithObligations(filterData, releaseIdToAcceptedCLI, releases, sw360User);

            for (Map.Entry<String, ObligationStatusInfo> entry : oblData.entrySet()) {
                ObligationStatusInfo statusInfo = entry.getValue();
                Set<Release> limitedSet = releaseService.getReleasesForUserByIds(statusInfo.getReleaseIdToAcceptedCLI().keySet());
                statusInfo.setReleases(limitedSet);
                statusInfo.setId(entry.getKey());
            }
        } else {
            ObligationLevel targetLevel;
            switch (oblLevel.toLowerCase()) {
                case "project":
                    targetLevel = ObligationLevel.PROJECT_OBLIGATION;
                    break;
                case "organization":
                    targetLevel = ObligationLevel.ORGANISATION_OBLIGATION;
                    break;
                case "component":
                    targetLevel = ObligationLevel.COMPONENT_OBLIGATION;
                    break;
                default:
                    throw new BadRequestClientException("Invalid Obligation Level");
            }
            oblData = filterObligationsByLevel(oblData, targetLevel);
        }


        Map<String, Object> responseBody = createPaginationMetadata(pageable, oblData);
        HalResource<Map<String, Object>> halObligation = new HalResource<>(responseBody);
        return new ResponseEntity<>(halObligation, HttpStatus.OK);
    }

    private Map<String, ObligationStatusInfo> filterObligationsByLevel(
            Map<String, ObligationStatusInfo> obligationStatusMap, ObligationLevel targetLevel) {
        return obligationStatusMap.entrySet().stream()
                .filter(entry -> {
                    ObligationLevel obligationLevel = entry.getValue().getObligationLevel();
                    return obligationLevel == null || obligationLevel == targetLevel;
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Add licenseObligations from license DB",
            description = "Pass an array of obligation ids in request body.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/licenseObligation", method = RequestMethod.POST)
    public ResponseEntity<?> addLicenseObligations(
            @Parameter(description = "License Obligation ID.")
            @PathVariable("id") String id,
            @Parameter(description = "Set of license obligation IDs to be added.",
                    example = "[\"3765276512\",\"5578999\",\"3765276513\"]"
            )
            @RequestBody List<String> obligationIds
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Project sw360Project = projectService.getProjectForUserById(id, sw360User);
        Map<String, AttachmentUsage> licenseInfoAttachmentUsage = projectService.getLicenseInfoAttachmentUsage(id);
        Map<String, Set<Release>> licensesFromAttachmentUsage = projectService.getLicensesFromAttachmentUsage(
                licenseInfoAttachmentUsage, sw360User);
        Map<String, ObligationStatusInfo> licenseObligation = projectService.getLicenseObligationData(licensesFromAttachmentUsage, sw360User);
        Map<String, ObligationStatusInfo> selectedLicenseObligation = new HashMap<String, ObligationStatusInfo>();

        ObligationList obligation = new ObligationList();
        Map<String, ObligationStatusInfo> obligationStatusMap = Maps.newHashMap();
        Map<String, ObligationStatusInfo> obligationStatusMapFromReport = Maps.newHashMap();
        final Map<String, String> releaseIdToAcceptedCLI = Maps.newHashMap();
        List<Release> releases = new ArrayList<>();
        List<String> releaseIds = new ArrayList<>(sw360Project.getReleaseIdToUsage().keySet());
        for (final String releaseId : releaseIds) {
            Release sw360Release = releaseService.getReleaseForUserById(releaseId, sw360User);
            if (sw360Release.getAttachmentsSize() > 0) {
                releases.add(sw360Release);
            }
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(sw360Project.getLinkedObligationId())) {
            obligation = projectService.getObligationData(sw360Project.getLinkedObligationId(), sw360User);
            obligationStatusMap = CommonUtils.nullToEmptyMap(obligation.getLinkedObligationStatus());
            releaseIdToAcceptedCLI.putAll(SW360Utils.getReleaseIdtoAcceptedCLIMappings(obligationStatusMap));
        } else {
            obligationStatusMapFromReport = projectService.setLicenseInfoWithObligations(obligationStatusMap, releaseIdToAcceptedCLI, releases, sw360User);
        }

        if (licenseObligation.size() == 0) {
            return new ResponseEntity<>("No License Obligations Present", HttpStatus.NO_CONTENT);
        }
        for (Map.Entry<String, ObligationStatusInfo> entry : licenseObligation.entrySet()) {
            String oblId = entry.getValue().getId();
            if (obligationIds.contains(oblId)) {
                selectedLicenseObligation.put(entry.getKey(), entry.getValue());
            }
        }
        selectedLicenseObligation.putAll(obligationStatusMapFromReport);
        RequestStatus requestStatus= projectService.addLinkedObligations(sw360Project, sw360User, selectedLicenseObligation);
        if (requestStatus == RequestStatus.SUCCESS) {
            return new ResponseEntity<>("License Obligation Added Successfully", HttpStatus.CREATED);
        }
        throw new ResourceNotFoundException("Failed to add/update obligation for project");
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Update License Obligations ",
            description = "Pass a map of obligations in request body.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/updateLicenseObligation", method = RequestMethod.PATCH)
    public ResponseEntity<?> patchLicenseObligations(
            @Parameter(description = "Project ID") @PathVariable("id") String id,
            @Parameter(description = "Map of obligation status info")
            @RequestBody Map<String, ObligationStatusInfo> requestBodyObligationStatusInfo
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        Map<String, ObligationStatusInfo> obligationStatusMap = new HashMap<>();
        try {
            obligationStatusMap = processLicenseObligations(id, sw360User, requestBodyObligationStatusInfo);
            Map<String, ObligationStatusInfo> updatedObligationStatusMap = projectService
                    .compareObligationStatusMap(sw360User, obligationStatusMap, requestBodyObligationStatusInfo);
            Project sw360Project = projectService.getProjectForUserById(id, sw360User);
            ObligationList obligationList = projectService.getObligationData(sw360Project.getLinkedObligationId(), sw360User);
            RequestStatus updateStatus = projectService
                    .patchLinkedObligations(sw360User, updatedObligationStatusMap, obligationList);
            if (updateStatus == RequestStatus.SUCCESS) {
                return ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body("License Obligation Updated Successfully");
            }

            throw new DataIntegrityViolationException("Cannot update License Obligation");
        } catch (Exception e) {
            log.error("Error updating license obligations: ", e);
            throw new DataIntegrityViolationException("Failed to update License Obligation: " + e.getMessage());
        }
    }

    /**
     * Processes the obligations for a project based on whether it has linked obligations or not.
     * If linked obligations exist, it processes them; otherwise, it updates the project obligations.
     *
     * @param sw360Project The SW360 project to process obligations for.
     * @param sw360User The user performing the operation.
     * @param requestBodyObligationStatusInfo The obligation status information from the request body.
     * @param obligationList The obligation list to be processed.
     * @return A map of obligation status information after processing.
     * @throws TException If there is an error during the Thrift operation.
     */
    private Map<String, ObligationStatusInfo> processLicenseObligations(
            String projectId,
            User sw360User,
            Map<String, ObligationStatusInfo> requestBodyObligationStatusInfo

    ) throws TException {
        Project sw360Project = projectService.getProjectForUserById(projectId, sw360User);
        if (hasLinkedObligations(sw360Project)) {
            return processExistingLicenseObligations(sw360Project, sw360User, requestBodyObligationStatusInfo);
        }
        return updateProjectLicenseObligations(sw360Project, sw360User, new HashMap<>());
    }

    private boolean hasLinkedObligations(Project project) {
        return CommonUtils.isNotNullEmptyOrWhitespace(project.getLinkedObligationId());
    }

    /**
     * Processes existing obligations for a project.
     * If the project has linked obligations, it retrieves them and checks if all obligations from the request body are present.
     * If not all obligations are present, it updates the project obligations with the existing ones.
     * If all obligations are present, it returns the existing obligation status map.
     *
     * @param sw360Project The SW360 project to process obligations for.
     * @param sw360User The user performing the operation.
     * @param requestBodyObligationStatusInfo The obligation status information from the request body.
     * @param obligationList The obligation list to be processed.
     * @return A map of obligation status information after processing.
     * @throws TException If there is an error during the Thrift operation.
     */
    private Map<String, ObligationStatusInfo> processExistingLicenseObligations(
            Project sw360Project,
            User sw360User,
            Map<String, ObligationStatusInfo> requestBodyObligationStatusInfo
    ) throws TException {
        ObligationList obligationList = projectService.getObligationData(sw360Project.getLinkedObligationId(), sw360User);
        Map<String, ObligationStatusInfo> obligationStatusMap = CommonUtils.nullToEmptyMap(obligationList.getLinkedObligationStatus());

        boolean allObligationsPresent = requestBodyObligationStatusInfo.keySet()
                .stream()
                .allMatch(obligationStatusMap::containsKey);

        if (!allObligationsPresent) {
            return updateProjectLicenseObligations(sw360Project, sw360User, obligationStatusMap);
        }

        return obligationStatusMap;
    }

    /**
     * Updates the project obligations by retrieving the releases and setting the license information with obligations.
     * It also adds linked obligations to the project.
     * This method is called when the project has no linked obligations or when the existing obligations need to be updated.
     *
     * @param sw360Project The SW360 project to update obligations for.
     * @param sw360User The user performing the operation.
     * @param existingObligationStatusMap The existing obligation status map to be updated.
     * @return A map of updated obligation status information.
     * @throws TException If there is an error during the Thrift operation.
     */
    private Map<String, ObligationStatusInfo> updateProjectLicenseObligations(
            Project sw360Project,
            User sw360User,
            Map<String, ObligationStatusInfo> existingObligationStatusMap
    ) throws TException {
        Map<String, String> releaseIdToAcceptedCLI = new HashMap<>();
        List<Release> releases = getReleasesWithAttachments(sw360Project, sw360User);

        Map<String, ObligationStatusInfo> updatedObligationStatusMap = projectService.setLicenseInfoWithObligations(
                existingObligationStatusMap,
                releaseIdToAcceptedCLI,
                releases,
                sw360User
        );


        projectService.addLinkedObligations(sw360Project, sw360User, updatedObligationStatusMap);
        return updatedObligationStatusMap;
    }

    /**
     * Retrieves releases with attachments for a given project and user.
     * It filters out releases that do not have any attachments.
     * This method is used to ensure that only relevant releases are processed, especially when dealing with license obligations.
     *
     *
     * @param project The project to retrieve releases from.
     * @param user The user for whom the releases are being retrieved.
     * @return A list of releases that have attachments.
     * @throws TException If there is an error during the Thrift operation.
     */
    private List<Release> getReleasesWithAttachments(Project project, User user) throws TException {
        return project.getReleaseIdToUsage().keySet().stream()
                .map(releaseId -> {
                    try {
                        Release release = releaseService.getReleaseForUserById(releaseId, user);
                        return release.getAttachmentsSize() > 0 ? release : null;
                    } catch (TException e) {
                        log.error("Error fetching release: " + releaseId, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Update project Obligations other than License Obligations",
            description = "Pass a map of obligations in request body.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/updateObligation", method = RequestMethod.PATCH)
    public ResponseEntity<?> patchObligations(
            @Parameter(description = "Project ID") @PathVariable("id") String id,
            @Parameter(description = "Map of obligation status info")
            @RequestBody Map<String, ObligationStatusInfo> requestBodyObligationStatusInfo ,
            @Parameter(description = "Obligation Level",
                    schema = @Schema(allowableValues = {"project", "organization", "component"}))
            @RequestParam(value = "obligationLevel", required = true) String oblLevel
    ) throws TException {

        Map<String, ObligationStatusInfo> obligationStatusMap = new HashMap<>();
        try {
            final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
            restControllerHelper.throwIfSecurityUser(sw360User);

            obligationStatusMap = processObligations(id, sw360User, requestBodyObligationStatusInfo, oblLevel);
            Map<String, ObligationStatusInfo> updatedObligationStatusMap = projectService
                    .compareObligationStatusMap(sw360User, obligationStatusMap, requestBodyObligationStatusInfo);
            Project sw360Project = projectService.getProjectForUserById(id, sw360User);
            ObligationList obligationList = projectService.getObligationData(sw360Project.getLinkedObligationId(), sw360User);
            RequestStatus updateStatus = projectService
                    .patchLinkedObligations(sw360User, updatedObligationStatusMap, obligationList);
            if (updateStatus == RequestStatus.SUCCESS) {
                return ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(oblLevel + " Obligation Updated Successfully");
            }

            throw new DataIntegrityViolationException("Cannot update "+oblLevel+" Obligation");
        } catch (Exception e) {
            log.error("Error updating {0} obligations: ", oblLevel ,e);
            throw new DataIntegrityViolationException("Failed to update "+oblLevel+"  Obligation: " + e.getMessage());
        }
    }

    private Map<String, ObligationStatusInfo> processObligations(
            String projectId,
            User sw360User,
            Map<String, ObligationStatusInfo> requestBodyObligationStatusInfo,
            String oblLevel) throws TException {
        Project sw360Project = projectService.getProjectForUserById(projectId, sw360User);
        if (hasLinkedObligations(sw360Project)) {
            return processExistingObligations(sw360Project, sw360User, requestBodyObligationStatusInfo ,oblLevel);
        }
        return updateProjectObligations(sw360Project, sw360User, new HashMap<>(), oblLevel);
    }

     private Map<String, ObligationStatusInfo> processExistingObligations(
            Project sw360Project,
            User sw360User,
            Map<String, ObligationStatusInfo> requestBodyObligationStatusInfo,
            String oblLevel) throws TException {
        ObligationList obligationList = projectService.getObligationData(sw360Project.getLinkedObligationId(), sw360User);
        Map<String, ObligationStatusInfo> obligationStatusMap = CommonUtils.nullToEmptyMap(obligationList.getLinkedObligationStatus());

        boolean allObligationsPresent = requestBodyObligationStatusInfo.keySet()
                .stream()
                .filter(entry -> {
                    ObligationStatusInfo statusInfo = requestBodyObligationStatusInfo.get(entry);
                    return statusInfo.getObligationLevel() == null || statusInfo.getObligationLevel().toString().equalsIgnoreCase(oblLevel);
                })
                .distinct()
                .collect(Collectors.toSet())
                .stream()
                .allMatch(obligationStatusMap::containsKey);

        if (!allObligationsPresent) {
            return updateProjectObligations(sw360Project, sw360User, obligationStatusMap , oblLevel);
        }

        return obligationStatusMap;
    }

    private Map<String, ObligationStatusInfo> updateProjectObligations(
            Project sw360Project,
            User sw360User,
            Map<String, ObligationStatusInfo> existingObligationStatusMap,
            String oblLevel) throws TException {

          Map<String, ObligationStatusInfo> updatedObligationStatusMap = projectService.setObligationsFromAdminSection(
                sw360User, existingObligationStatusMap, sw360Project, oblLevel);

        projectService.addLinkedObligations(sw360Project, sw360User, updatedObligationStatusMap);
        return updatedObligationStatusMap;
    }

    @Operation(
            description = "Get summary and administration page of project tab.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/summaryAdministration", method = RequestMethod.GET)
    public ResponseEntity<EntityModel<Project>> getAdministration(
            @Parameter(description = "Project ID", example = "376576")
            @PathVariable("id") String id
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Project sw360Project = projectService.getProjectForUserById(id, sw360User);
        if (isNullEmptyOrWhitespace(sw360Project.getLicenseInfoHeaderText())) {
            sw360Project.setLicenseInfoHeaderText(projectService.getLicenseInfoHeaderText());
        }
        Map<String, String> sortedExternalURLs = CommonUtils.getSortedMap(sw360Project.getExternalUrls(), true);
        sw360Project.setExternalUrls(sortedExternalURLs);
        sw360Project.setReleaseIdToUsage(null);
        sw360Project.setLinkedProjects(null);
        sw360Project.setAttachments(null);
        sw360Project.setPackageIds(null);
        HalResource<Project> userHalResource = createHalProject(sw360Project, sw360User);
        setAdditionalFieldsToHalResource(sw360Project,userHalResource);
        sw360Project.unsetLinkedProjects();
        sw360Project.unsetReleaseIdToUsage();
        sw360Project.unsetProjectResponsible();
        sw360Project.unsetSecurityResponsibles();

        return new ResponseEntity<>(userHalResource, HttpStatus.OK);
    }

    @Operation(
            summary = "Get a list view of dependency network for a project.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/network/{id}/listView", method = RequestMethod.GET)
    public ResponseEntity<?> getListViewDependencyNetwork(
            @Parameter(description = "Project ID", example = "376576")
            @PathVariable("id") String projectId
    ) throws TException {
        if (!SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            throw new SW360Exception(SW360Constants.PLEASE_ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP);
        }

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<Map<String, String>> clearingStatusList = projectService.serveDependencyNetworkListView(projectId, sw360User);
        return new ResponseEntity<>(clearingStatusList, HttpStatus.OK);
    }

    @Operation(
            description = "Get linked resources (projects, releases) of a project",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/network/{id}/linkedResources", method = RequestMethod.GET)
    public ResponseEntity<ProjectLink> getLinkedResourcesOfProjectForDependencyNetwork(
            @Parameter(description = "Project ID", example = "376576")
            @PathVariable("id") String id,
            @Parameter(description = "Get linked releases transitively (default is false)", example = "true")
            @RequestParam(value = "transitive", required = false, defaultValue = "false") boolean transitive
    ) throws TException {
        if (!SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            throw new SW360Exception(SW360Constants.PLEASE_ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP);
        }
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        ProjectLink projectLink = projectService.serveLinkedResourcesOfProjectInDependencyNetwork(id, transitive, sw360User);
        return new ResponseEntity<>(projectLink, HttpStatus.OK);
    }

    @Operation(
            description = "Get indirect linked releases of a project in dependency network by release's index path",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/network/{id}/releases", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<ReleaseLink>> getLinkedReleasesInDependencyNetworkByIndexPath(
            @Parameter(description = "Project ID", example = "376576")
            @PathVariable("id") String projectId,
            @Parameter(description = "Index path", example = "0->1")
            @RequestParam(value = "path", required = false) String releaseIndexPath
    ) throws TException {
        if (!SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            throw new SW360Exception(SW360Constants.PLEASE_ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP);
        }

        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        if (!CommonUtils.isNotNullEmptyOrWhitespace(releaseIndexPath)) {
            ProjectLink projectLink = projectService.serveLinkedResourcesOfProjectInDependencyNetwork(projectId, false, sw360User);
            return new ResponseEntity<>(CollectionModel.of(projectLink.getLinkedReleases()), HttpStatus.OK);
        }

        List<String> indexPath = Arrays.asList(releaseIndexPath.split("->"));
        try {
            List<ReleaseLink> releaseLinks = projectService.serveLinkedReleasesInDependencyNetworkByIndexPath(projectId, indexPath, sw360User);
            CollectionModel<ReleaseLink> resources = CollectionModel.of(releaseLinks);
            return new ResponseEntity<>(resources, HttpStatus.OK);
        } catch (SW360Exception exception) {
            if (exception.getErrorCode() == 404) {
                throw new ResourceNotFoundException("Requested project not found: " + projectId);
            }
            throw new RuntimeException(exception.getWhy());
        }
    }

    private void setAdditionalFieldsToHalResource(Project sw360Project, HalResource<Project> userHalResource) throws SW360Exception {
        try {
            String modifiedByEmail = sw360Project.getModifiedBy();
            if (modifiedByEmail != null) {
                User projectModifier = restControllerHelper.getUserByEmail(modifiedByEmail);
                if (projectModifier != null) {
                    restControllerHelper.addEmbeddedUser(userHalResource, projectModifier, "modifiedBy");
                }
            }
            String projectOwnerEmail = sw360Project.getProjectOwner();
            if (projectOwnerEmail != null) {
                User projectOwner = restControllerHelper.getUserByEmail(sw360Project.getProjectOwner());
                if (projectOwner != null) {
                    restControllerHelper.addEmbeddedUser(userHalResource, projectOwner, "projectOwner");
                }
            }
            if (sw360Project.getSecurityResponsibles() == null || sw360Project.getSecurityResponsibles().isEmpty()) {
                sw360Project.setSecurityResponsibles(new HashSet<String>(){{add("");}});
            }
            Set<String> securityResponsibles = sw360Project.getSecurityResponsibles();
            restControllerHelper.addEmbeddedSecurityResponsibles(userHalResource, securityResponsibles);

            String clearingTeam = sw360Project.getClearingTeam();
            if (clearingTeam != null) {
                restControllerHelper.addEmbeddedClearingTeam(userHalResource, clearingTeam, "clearingTeam");
            }
            if (sw360Project.getProjectResponsible() != null) {
                restControllerHelper.addEmbeddedProjectResponsible(userHalResource,sw360Project.getProjectResponsible());
            }
        } catch (Exception e) {
            throw new SW360Exception(e.getMessage());
        }
    }

    private HalResource<ProjectDTO> createHalProjectDTO(Project sw360Project, User sw360User) throws TException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ProjectDTO projectDTO = objectMapper.convertValue(sw360Project,ProjectDTO.class);

        List<ReleaseNode> dependencyNetwork = new ArrayList<>();
        if (CommonUtils.isNotNullEmptyOrWhitespace(sw360Project.getReleaseRelationNetwork())) {
            try {
                dependencyNetwork = objectMapper.readValue(sw360Project.getReleaseRelationNetwork(), new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                log.error(e);
            }
        }
        projectDTO.setDependencyNetwork(dependencyNetwork);
        HalResource<ProjectDTO> halProject = new HalResource<>(projectDTO);

        User projectCreator = restControllerHelper.getUserByEmail(projectDTO.getCreatedBy());
        restControllerHelper.addEmbeddedUser(halProject, projectCreator, CREATED_BY);

        Map<String, ProjectProjectRelationship> linkedProjects = projectDTO.getLinkedProjects();
        if (linkedProjects != null) {
            restControllerHelper.addEmbeddedProjectDTO(halProject, linkedProjects.keySet(), projectService, sw360User);
        }

        if (projectDTO.getModerators() != null) {
            Set<String> moderators = projectDTO.getModerators();
            restControllerHelper.addEmbeddedModerators(halProject, moderators);
        }

        if (projectDTO.getAttachments() != null) {
            restControllerHelper.addEmbeddedAttachments(halProject, projectDTO.getAttachments());
        }

        if(projectDTO.getLeadArchitect() != null) {
            restControllerHelper.addEmbeddedLeadArchitect(halProject, projectDTO.getLeadArchitect());
        }

        if (projectDTO.getContributors() != null) {
            Set<String> contributors = projectDTO.getContributors();
            restControllerHelper.addEmbeddedContributors(halProject, contributors);
        }

        if (projectDTO.getVendor() != null) {
            Vendor vendor = sw360Project.getVendor();
            HalResource<Vendor> vendorHalResource = restControllerHelper.addEmbeddedVendor(vendor.getFullname());
            halProject.addEmbeddedResource("sw360:vendors", vendorHalResource);
            projectDTO.setVendor(null);
        }

        return halProject;
    }

    private void addOrPatchDependencyNetworkToProject(Project project, Map<String, Object> requestBody, ProjectOperation operation)
            throws JsonProcessingException, SW360Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<ReleaseNode> releaseNodes = null;
        String dependencyNetwork = objectMapper.writeValueAsString(requestBody.get("dependencyNetwork"));

        List<ReleaseNode> uniqueDependencyNetwork = new ArrayList<>();

        if (dependencyNetwork != null && !dependencyNetwork.equals("null")) {
            releaseNodes = objectMapper.readValue(dependencyNetwork, new TypeReference<>() {
            });

            if (releaseNodes != null) {
                List<String> releaseWithSameLevel = new ArrayList<>();
                for (ReleaseNode releaseNode : releaseNodes) {
                    if (CommonUtils.isNullEmptyOrWhitespace(releaseNode.getReleaseId())) {
                        throw new SW360Exception("releaseId cannot be null or empty");
                    }

                    if (releaseWithSameLevel.contains(releaseNode.getReleaseId())) {
                        continue;
                    }
                    releaseWithSameLevel.add(releaseNode.getReleaseId());
                    updateReleaseNodeData(releaseNode, operation);
                    if (releaseNode.getReleaseLink() == null) {
                        releaseNode.setReleaseLink(Collections.emptyList());
                    }
                    else {
                        List<String> loadedReleases = new ArrayList<>();
                        loadedReleases.add(releaseNode.getReleaseId());
                        releaseNode.setReleaseLink(checkAndUpdateSubNodes(releaseNode.getReleaseLink(), operation, loadedReleases));
                    }
                    uniqueDependencyNetwork.add(releaseNode);
                }
            }
            project.setReleaseRelationNetwork(new Gson().toJson(uniqueDependencyNetwork));
        }
        else {
            project.setReleaseRelationNetwork(null);
        }
    }

    private List<ReleaseNode> checkAndUpdateSubNodes(List<ReleaseNode> releaseNodes, ProjectOperation operation, List<String> loadedReleases) throws SW360Exception {
        List<ReleaseNode> uniqueDependencyNetwork = new ArrayList<>();
        List<String> releaseIdsWithSameLevel = new ArrayList<>();
        for (ReleaseNode releaseNode : releaseNodes) {
            if (CommonUtils.isNullEmptyOrWhitespace(releaseNode.getReleaseId())) {
                throw new SW360Exception("releaseId cannot be null or empty");
            }

            if (releaseIdsWithSameLevel.contains(releaseNode.getReleaseId())) {
                continue;
            }
            releaseIdsWithSameLevel.add(releaseNode.getReleaseId());

            if (loadedReleases.contains(releaseNode.getReleaseId())) {
                loadedReleases.add(releaseNode.getReleaseId());
                String cyclicHierarchy = String.join(" -> ", loadedReleases);
                throw new SW360Exception("Cyclic hierarchy in dependency network: " + cyclicHierarchy);
            }

            loadedReleases.add(releaseNode.getReleaseId());
            updateReleaseNodeData(releaseNode, operation);
            if (releaseNode.getReleaseLink() == null) {
                releaseNode.setReleaseLink(Collections.emptyList());
                loadedReleases.remove(loadedReleases.size() - 1);
            } else {
                releaseNode.setReleaseLink(checkAndUpdateSubNodes(releaseNode.getReleaseLink(), operation, loadedReleases));
                loadedReleases.remove(loadedReleases.size() - 1);
            }
            uniqueDependencyNetwork.add(releaseNode);
        }
        return uniqueDependencyNetwork;
    }

    private void updateReleaseNodeData(ReleaseNode releaseNode, ProjectOperation operation) throws SW360Exception {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        String mainLineStateUpper = (releaseNode.getMainlineState() != null) ? releaseNode.getMainlineState().toUpperCase() : MainlineState.OPEN.toString();
        String releaseRelationShipUpper = (releaseNode.getReleaseRelationship() != null) ? releaseNode.getReleaseRelationship().toUpperCase() : ReleaseRelationship.CONTAINED.toString();

        if (!enumMainlineStateValues.contains(mainLineStateUpper)) {
            throw new SW360Exception("mainLineState of release " + releaseNode.getReleaseId() + " must be in Enum " + enumMainlineStateValues);
        }
        if (!enumReleaseRelationshipValues.contains(releaseRelationShipUpper)) {
            throw new SW360Exception("releaseRelationShip of release " + releaseNode.getReleaseId() + " must be in Enum " + enumReleaseRelationshipValues);
        }

        releaseNode.setReleaseRelationship(releaseRelationShipUpper);
        releaseNode.setMainlineState(mainLineStateUpper);
        releaseNode.unsetComponentId();
        releaseNode.unsetReleaseName();
        releaseNode.unsetReleaseVersion();

        if (operation.equals(ProjectOperation.CREATE)) {
            releaseNode.setCreateOn(SW360Utils.getCreatedOn());
            releaseNode.setCreateBy(sw360User.getEmail());
        } else if (operation.equals(ProjectOperation.UPDATE)) {
            if (CommonUtils.isNullEmptyOrWhitespace(releaseNode.getCreateOn())) {
                releaseNode.setCreateOn(SW360Utils.getCreatedOn());
            }
            if (CommonUtils.isNullEmptyOrWhitespace(releaseNode.getCreateBy())) {
                releaseNode.setCreateBy(sw360User.getEmail());
            }
        }
    }

    private ClearingRequest convertToClearingRequest(Map<String, Object> requestBody) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ClearingRequest clearingRequest = mapper.convertValue(requestBody, ClearingRequest.class);
        return clearingRequest;
    }

    /**
     * Creates a clearing request for a project.
     *
     * @param id          The project ID.
     * @param reqBodyMap  The clearing request
     * @return            The response entity containing the result of the operation.
     * @throws TException If an error occurs during the operation.
     */
    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Create a clearing request for a project.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/clearingRequest", method = RequestMethod.POST)
    public ResponseEntity<?> createClearingRequest(
            @Parameter(description = "Project ID", example = "376576")
            @PathVariable("id") String id,
            @Parameter(description = "Clearing request",
                    schema = @Schema(implementation = ClearingRequest.class))
            @RequestBody Map<String, Object> reqBodyMap,
            HttpServletRequest request
    ) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Project sw360Project = projectService.getProjectForUserById(id, user);
        ClearingRequest clearingRequest = convertToClearingRequest(reqBodyMap);
        clearingRequest.setProjectId(id);
        clearingRequest.setRequestingUser(user.getEmail());
        clearingRequest.setClearingState(ClearingRequestState.NEW);

        if (clearingRequest.getRequestingUserComment() == null) {
            clearingRequest.setRequestingUserComment("");
        }

        if (clearingRequest.getClearingType() == null) {
            throw new BadRequestClientException("clearingType is a mandatory field. Possible values are "
                    + Arrays.asList(ClearingRequestType.values()));
        }

        Integer criticalCount = moderationRequestService.getOpenCriticalCrCountByGroup(user.getDepartment());
        clearingRequest.setPriority(criticalCount > 1 ? null : clearingRequest.getPriority());
        Integer dateLimit = projectService.loadPreferredClearingDateLimit();
        dateLimit = (ClearingRequestPriority.CRITICAL.equals(clearingRequest.getPriority()) && criticalCount < 2) ? 0 : (dateLimit < 1) ? 7 : dateLimit;
        if (!SW360Utils.isValidDate(clearingRequest.getRequestedClearingDate(), DateTimeFormatter.ISO_LOCAL_DATE, Long.valueOf(dateLimit))) {
            log.warn("Invalid requested clearing date: " + clearingRequest.getRequestedClearingDate() + " is entered, by user: "+ user.getEmail());
            throw new BadRequestClientException("Invalid clearing date requested");
        }

        if (clearingRequest.getClearingTeam() != null) {
            User clearingTeam = restControllerHelper.getUserByEmailOrNull(clearingRequest.getClearingTeam());
            if (clearingTeam == null) {
                throw new BadRequestClientException("clearingTeam is not a valid user");
            }
        }
        String baseURL = restControllerHelper.getBaseUrl(request);
        AddDocumentRequestSummary addDocumentRequestSummary = projectService.createClearingRequest(clearingRequest, user, baseURL, id);

        if (addDocumentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.DUPLICATE) {
            throw new DataIntegrityViolationException(addDocumentRequestSummary.getMessage());
        } else if (addDocumentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.FAILURE) {
            throw new BadRequestClientException(addDocumentRequestSummary.getMessage());
        }
        clearingRequest.setId(addDocumentRequestSummary.getId());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(clearingRequest.getId()).toUri();

        HalResource<ClearingRequest> halResource = new HalResource<>(clearingRequest);

        return ResponseEntity.created(location).body(halResource);
    }

    @Operation(
            description = "Get linked releases information in project's dependency network.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/network/{id}/linkedReleases", method = RequestMethod.GET)
    public ResponseEntity<?> getLinkedReleasesInNetwork(
            @Parameter(description = "Project ID.")
            @PathVariable("id") String projectId
    ) throws TException {
        if (!SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            throw new SW360Exception(SW360Constants.PLEASE_ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP);
        }

        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        return new ResponseEntity<>(projectService.getLinkedReleasesInDependencyNetworkOfProject(projectId, sw360User), HttpStatus.OK);
    }

    @Operation(
            description = "Get linked releases information of linked projects.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/subProjects/releases", method = RequestMethod.GET)
    public ResponseEntity<?> getLinkedReleasesOfLinkedProjects(
            @Parameter(description = "Project ID.") @PathVariable("id") String projectId
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<Release> linkedReleases = projectService.getLinkedReleasesOfSubProjects(projectId, sw360User);
        List<HalResource> halResources = linkedReleases.stream()
                .map(rel -> restControllerHelper.createHalReleaseResourceWithAllDetails(rel))
                .collect(Collectors.toList());
        CollectionModel<HalResource> collectionModel = CollectionModel.of(halResources);
        return new ResponseEntity<>(collectionModel, HttpStatus.OK);
    }

    @Operation(
            description = "Compare dependency network with default network (relationships between releases).",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/network/compareDefaultNetwork", method = RequestMethod.POST)
    public ResponseEntity<?> compareDependencyNetworkWithDefaultNetwork(
            @RequestBody List<ReleaseNode> dependencyNetwork
    ) throws SW360Exception {
        if (!SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            throw new SW360Exception(SW360Constants.PLEASE_ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP);
        }

        if (CommonUtils.isNullOrEmptyCollection(dependencyNetwork)) {
            return new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK);
        }
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<Map<String, Object>> comparedNetwork = projectService.compareWithDefaultNetwork(dependencyNetwork, sw360User);
        return new ResponseEntity<>(comparedNetwork, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            description = "Create a duplicate project with dependency network.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/network/duplicate/{id}", method = RequestMethod.POST)
    public ResponseEntity<?> createDuplicateProjectWithDependencyNetwork(
            @Parameter(description = "Project ID to copy.")
            @PathVariable("id") String id,
            @Parameter(schema = @Schema(implementation = Project.class))
            @RequestBody Map<String, Object> reqBodyMap
    ) throws TException {
        if (!SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            throw new SW360Exception(SW360Constants.PLEASE_ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP);
        }

        if (!reqBodyMap.containsKey("name") && !reqBodyMap.containsKey("version")) {
            throw new BadRequestClientException(
                    "Field name or version should be present in request body to create duplicate of a project");
        }
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Project duplicatedProject = projectService.getProjectForUserById(id, sw360User);
        Project projectFromRequest = convertToProject(reqBodyMap);
        duplicatedProject.unsetReleaseIdToUsage();
        projectFromRequest.unsetReleaseIdToUsage();
        duplicatedProject = this.restControllerHelper.updateProject(duplicatedProject, projectFromRequest, reqBodyMap,
                mapOfProjectFieldsToRequestBody);

        if (reqBodyMap.get("dependencyNetwork") != null) {
            try {
                addOrPatchDependencyNetworkToProject(duplicatedProject, reqBodyMap, ProjectOperation.CREATE);
            } catch (JsonProcessingException | NoSuchElementException e) {
                log.error(e.getMessage(), e);
                throw new BadRequestClientException(e.getMessage());
            }
        }

        projectService.syncReleaseRelationNetworkAndReleaseIdToUsage(duplicatedProject, sw360User);
        duplicatedProject.unsetId();
        duplicatedProject.unsetRevision();
        duplicatedProject.unsetAttachments();
        duplicatedProject.unsetClearingRequestId();
        duplicatedProject.setClearingState(ProjectClearingState.OPEN);
        String linkedObligationId = duplicatedProject.getLinkedObligationId();
        duplicatedProject.unsetLinkedObligationId();

        Project createdProject = projectService.createProject(duplicatedProject, sw360User);
        createdProject.setLinkedObligationId(linkedObligationId);
        projectService.copyLinkedObligationsForClonedProject(createdProject, duplicatedProject, sw360User);

        HalResource<ProjectDTO> projectDTOHalResource = createHalProjectDTO(createdProject, sw360User);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(createdProject.getId()).toUri();

        return ResponseEntity.created(location).body(projectDTOHalResource);
    }

    @Operation(
            summary = "Add licenses to linked releases of a project.",
            description = "This API adds license information to linked releases of a project by processing the approved" +
                    " CLI attachments for each release. It categorizes releases based on the number of CLI attachments" +
                    " (single, multiple, or none) and updates their main and other licenses accordingly.",
            tags = {"Projects"},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "License information successfully added to linked releases. " +
                                    "Response contains a map with keys `[MULTIPLE_ATTACHMENTS, NOT_UPDATED, UPDATED]`" +
                                    ", each containing a list of release IDs (releases available as embedded objects).",
                            content = @Content(
                                    schema = @Schema(type = "object", implementation = Map.class),
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "MULTIPLE_ATTACHMENTS": [
                                                        "rel1"
                                                      ],
                                                      "NOT_UPDATED": [
                                                        "rel2"
                                                      ],
                                                      "UPDATED": [
                                                        "rel3",
                                                        "rel4"
                                                      ],
                                                      "_embedded": {
                                                        "sw360:releases": [
                                                          {
                                                            "id": "rel1",
                                                            "name": "release 1",
                                                            "version": "1",
                                                            "externalIds": {},
                                                            "clearingState": "APPROVED"
                                                          }
                                                        ]
                                                      }
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Error occurred while processing license information for linked releases.",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = "{\n  \"error\": \"Error adding license info to linked releases.\"\n}"
                                    )
                            )
                    )
            }
    )
    @RequestMapping(value = PROJECTS_URL + "/{id}/addLinkedReleasesLicenses", method = RequestMethod.POST)
    public ResponseEntity<HalResource<EntityModel<Map<String, List<String>>>>> addLicenseToLinkedReleases(
            @Parameter(description = "Project ID", example = "376576")
            @PathVariable("id") String projectId
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();

        Map<Sw360ProjectService.ReleaseCLIInfo, List<Release>> releaseUpdates =
                projectService.addLicenseToLinkedReleases(projectId, sw360User);

        // Make sure all types are set for response, irrespective of what service returns.
        for (Sw360ProjectService.ReleaseCLIInfo key : Sw360ProjectService.ReleaseCLIInfo.values()) {
            try {
                releaseUpdates.putIfAbsent(key, new ArrayList<>());
            } catch (UnsupportedOperationException e) {
                releaseUpdates = new HashMap<>(releaseUpdates);
                releaseUpdates.putIfAbsent(key, new ArrayList<>());
            }
        }

        Map<String, List<String>> responseContent = releaseUpdates.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toString(),
                        entry -> entry.getValue().stream().map(Release::getId)
                                .collect(Collectors.toList())
                ));

        HalResource<EntityModel<Map<String, List<String>>>> resources =
                new HalResource<>(EntityModel.of(responseContent));

        releaseUpdates.values().stream()
                .flatMap(List::stream)
                .forEach(release -> resources.addEmbeddedResource(
                        "sw360:releases",
                        releaseService.convertToEmbeddedWithExternalIds(release)
                ));

        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Operation(
            summary = "Get all project groups.",
            description = "Get all the unique groups used by projects.",
            tags = {"Projects"}
    )
    @RequestMapping(value = PROJECTS_URL + "/groups", method = RequestMethod.GET)
    public Set<String> getAllProjectGroups() {
        Set<String> groups;
        try {
            groups = projectService.getGroups();
        } catch (TException e) {
            groups = Collections.emptySet();
        }
        return groups;
    }
}
