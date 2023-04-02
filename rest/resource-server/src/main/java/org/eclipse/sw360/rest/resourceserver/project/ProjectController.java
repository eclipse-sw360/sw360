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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TSimpleJSONProtocol;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage;
import org.eclipse.sw360.datahandler.thrift.attachments.UsageData;
import org.eclipse.sw360.datahandler.thrift.components.ClearingState;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfo;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoFile;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatInfo;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectLink;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.ProjectVulnerabilityRating;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityCheckStatus;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityDTO;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityRatingForProject;
import org.eclipse.sw360.rest.resourceserver.attachment.Sw360AttachmentService;
import org.eclipse.sw360.rest.resourceserver.component.Sw360ComponentService;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.license.Sw360LicenseService;
import org.eclipse.sw360.rest.resourceserver.licenseinfo.Sw360LicenseInfoService;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.eclipse.sw360.rest.resourceserver.vulnerability.Sw360VulnerabilityService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.GsonJsonParser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.sw360.datahandler.common.CommonUtils.wrapThriftOptionalReplacement;
import static org.eclipse.sw360.datahandler.common.WrappedException.wrapTException;
import static org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer.REPORT_FILENAME_MAPPING;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProjectController implements RepresentationModelProcessor<RepositoryLinksResource> {
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
            .put(Project._Fields.RELEASE_ID_TO_USAGE, "linkedReleases").build();
    private static final ImmutableMap<String, String> RESPONSE_BODY_FOR_MODERATION_REQUEST = ImmutableMap.<String, String>builder()
            .put("message", "Moderation request is created").build();

    @NonNull
    private final Sw360ProjectService projectService;

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
    private final com.fasterxml.jackson.databind.Module sw360Module;

    @RequestMapping(value = PROJECTS_URL, method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<Project>>> getProjectsForUser(
            Pageable pageable,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "type", required = false) String projectType,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "allDetails", required = false) boolean allDetails,
            @RequestParam(value = "luceneSearch", required = false) boolean luceneSearch,
            HttpServletRequest request) throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Map<String, Project> mapOfProjects = new HashMap<>();
        boolean isSearchByName = name != null && !name.isEmpty();
        boolean isSearchByTag = CommonUtils.isNotNullEmptyOrWhitespace(tag);
        boolean isSearchByType = CommonUtils.isNotNullEmptyOrWhitespace(projectType);
        boolean isSearchByGroup = CommonUtils.isNotNullEmptyOrWhitespace(group);
        List<Project> sw360Projects = new ArrayList<>();
        Map<String, Set<String>> filterMap = new HashMap<>();
        if (luceneSearch) {
            if (CommonUtils.isNotNullEmptyOrWhitespace(projectType)) {
                Set<String> values = CommonUtils.splitToSet(projectType);
                filterMap.put(Project._Fields.PROJECT_TYPE.getFieldName(), values);
            }
            if (CommonUtils.isNotNullEmptyOrWhitespace(group)) {
                Set<String> values = CommonUtils.splitToSet(group);
                filterMap.put(Project._Fields.BUSINESS_UNIT.getFieldName(), values);
            }
            if (CommonUtils.isNotNullEmptyOrWhitespace(tag)) {
                Set<String> values = CommonUtils.splitToSet(tag);
                filterMap.put(Project._Fields.TAG.getFieldName(), values);
            }

            if (CommonUtils.isNotNullEmptyOrWhitespace(name)) {
                Set<String> values = CommonUtils.splitToSet(name);
                values = values.stream().map(LuceneAwareDatabaseConnector::prepareWildcardQuery)
                        .collect(Collectors.toSet());
                filterMap.put(Project._Fields.NAME.getFieldName(), values);
            }

            sw360Projects.addAll(projectService.refineSearch(filterMap, sw360User));
        } else {
            if (isSearchByName) {
                sw360Projects.addAll(projectService.searchProjectByName(name, sw360User));
            } else if (isSearchByGroup) {
                sw360Projects.addAll(projectService.searchProjectByGroup(group, sw360User));
            } else if (isSearchByTag) {
                sw360Projects.addAll(projectService.searchProjectByTag(tag, sw360User));
            } else if (isSearchByType) {
                sw360Projects.addAll(projectService.searchProjectByType(projectType, sw360User));
            } else {
                sw360Projects.addAll(projectService.getProjectsForUser(sw360User));
            }
        }
        return getProjectResponse(pageable, projectType, group, tag, allDetails, luceneSearch, request, sw360User,
                mapOfProjects, isSearchByName, sw360Projects);
    }

    @NotNull
    private ResponseEntity<CollectionModel<EntityModel<Project>>> getProjectResponse(Pageable pageable,
            String projectType, String group, String tag, boolean allDetails, boolean luceneSearch,
            HttpServletRequest request, User sw360User, Map<String, Project> mapOfProjects, boolean isSearchByName,
            List<Project> sw360Projects) throws ResourceClassNotFoundException, PaginationParameterException, URISyntaxException {
        sw360Projects.stream().forEach(prj -> mapOfProjects.put(prj.getId(), prj));
        PaginationResult<Project> paginationResult = restControllerHelper.createPaginationResult(request, pageable,
                sw360Projects, SW360Constants.TYPE_PROJECT);

        List<EntityModel<Project>> projectResources = new ArrayList<>();
        Consumer<Project> consumer = p -> {
            EntityModel<Project> embeddedProjectResource = null;
            if (!allDetails) {
                Project embeddedProject = restControllerHelper.convertToEmbeddedProject(p);
                embeddedProjectResource = EntityModel.of(embeddedProject);
            } else {
                embeddedProjectResource = createHalProjectResourceWithAllDetails(p, sw360User, mapOfProjects,
                        !isSearchByName);
                if (embeddedProjectResource == null) {
                    return;
                }
            }
            projectResources.add(embeddedProjectResource);
        };

        if (luceneSearch) {
            paginationResult.getResources().stream().forEach(consumer);
        } else {
            paginationResult.getResources().stream()
                    .filter(project -> projectType == null || projectType.equals(project.projectType.name()))
                    .filter(project -> group == null || group.isEmpty() || group.equals(project.getBusinessUnit()))
                    .filter(project -> tag == null || tag.isEmpty() || tag.equals(project.getTag())).forEach(consumer);
        }
        CollectionModel resources;
        if (projectResources.size() == 0) {
            resources = restControllerHelper.emptyPageResource(Project.class, paginationResult);
        } else {
            resources = restControllerHelper.generatePagesResource(paginationResult, projectResources);
        }

        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    @RequestMapping(value = PROJECTS_URL + "/myprojects", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<Project>>> getProjectsFilteredForUser(
            Pageable pageable,
            @RequestParam(value = "createdBy", required = false, defaultValue = "true") boolean createdBy,
            @RequestParam(value = "moderator", required = false, defaultValue = "true") boolean moderator,
            @RequestParam(value = "contributor", required = false, defaultValue = "true") boolean contributor,
            @RequestParam(value = "projectOwner", required = false, defaultValue = "true") boolean projectOwner,
            @RequestParam(value = "leadArchitect", required = false, defaultValue = "true") boolean leadArchitect,
            @RequestParam(value = "projectResponsible", required = false, defaultValue = "true") boolean projectResponsible,
            @RequestParam(value = "securityResponsible", required = false, defaultValue = "true") boolean securityResponsible,
            @RequestParam(value = "stateOpen", required = false, defaultValue = "true") boolean stateOpen,
            @RequestParam(value = "stateClosed", required = false, defaultValue = "true") boolean stateClosed,
            @RequestParam(value = "stateInProgress", required = false, defaultValue = "true") boolean stateInProgress,
            @RequestParam(value = "allDetails", required = false) boolean allDetails,
            HttpServletRequest request) throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Map<String, Project> mapOfProjects = new HashMap<>();

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

        return getProjectResponse(pageable, null, null, null, allDetails, true, request, sw360User,
                mapOfProjects, true, sw360Projects);
    }

    @RequestMapping(value = PROJECTS_URL + "/{id}", method = RequestMethod.GET)
    public ResponseEntity<EntityModel<Project>> getProject(
            @PathVariable("id") String id) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Project sw360Project = projectService.getProjectForUserById(id, sw360User);
        HalResource<Project> userHalResource = createHalProject(sw360Project, sw360User);
        return new ResponseEntity<>(userHalResource, HttpStatus.OK);
    }

    @RequestMapping(value = PROJECTS_URL + "/{id}/linkedProjects", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel>> getLinkedProject(
            Pageable pageable,
            @PathVariable("id") String id,
            HttpServletRequest request) throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        Project sw360Proj = projectService.getProjectForUserById(id, sw360User);

        Map<String, ProjectProjectRelationship> linkedProjects = sw360Proj.getLinkedProjects();
        List<String> keys = new ArrayList<>(linkedProjects.keySet());
        List<Project> projects = keys.stream().map(projId -> wrapTException(() -> {
            final Project sw360Project = projectService.getProjectForUserById(projId, sw360User);
            return sw360Project;
        })).collect(Collectors.toList());

        PaginationResult<Project> paginationResult = restControllerHelper.createPaginationResult(request, pageable,
                projects, SW360Constants.TYPE_PROJECT);

        final List<EntityModel<Project>> projectResources = paginationResult.getResources().stream()
                .map(sw360Project -> wrapTException(() -> {
                    final Project embeddedProject = restControllerHelper.convertToEmbeddedProject(sw360Project);
                    final HalResource<Project> projectResource = new HalResource<>(embeddedProject);
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

    @RequestMapping(value = PROJECTS_URL + "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity deleteProject(@PathVariable("id") String id) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        projectService.deleteProject(id, sw360User);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = PROJECTS_URL, method = RequestMethod.POST)
    public ResponseEntity createProject(@RequestBody Map<String, Object> reqBodyMap)
            throws URISyntaxException, TException {
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

        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        project = projectService.createProject(project, sw360User);
        HalResource<Project> halResource = createHalProject(project, sw360User);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(project.getId()).toUri();

        return ResponseEntity.created(location).body(halResource);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = PROJECTS_URL + "/duplicate/{id}", method = RequestMethod.POST)
    public ResponseEntity<EntityModel<Project>> createDuplicateProject(@PathVariable("id") String id,
            @RequestBody Map<String, Object> reqBodyMap) throws TException {
        if (!reqBodyMap.containsKey("name") && !reqBodyMap.containsKey("version")) {
            throw new HttpMessageNotReadableException(
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
    @RequestMapping(value = PROJECTS_URL + "/{id}/releases", method = RequestMethod.POST)
    public ResponseEntity linkReleases(
            @PathVariable("id") String id,
            @RequestBody Object releasesInRequestBody) throws URISyntaxException, TException {
        RequestStatus linkReleasesStatus = addOrPatchReleasesToProject(id, releasesInRequestBody, false);
        HttpStatus status = HttpStatus.CREATED;
        if (linkReleasesStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity<>(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = PROJECTS_URL + "/{id}/releases", method = RequestMethod.PATCH)
    public ResponseEntity patchReleases(@PathVariable("id") String id, @RequestBody Object releaseURIs)
            throws URISyntaxException, TException {
        RequestStatus patchReleasesStatus = addOrPatchReleasesToProject(id, releaseURIs, true);
        if (patchReleasesStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity<>(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @RequestMapping(value = PROJECTS_URL + "/{id}/releases", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<Release>>> getProjectReleases(
            Pageable pageable,
            @PathVariable("id") String id,
            @RequestParam(value = "transitive", required = false) String transitive,HttpServletRequest request) throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {

        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Set<String> releaseIds = projectService.getReleaseIds(id, sw360User, transitive);
        final Set<String> releaseIdsInBranch = new HashSet<>();
        boolean isTransitive = Boolean.parseBoolean(transitive);

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
                    if (isTransitive) {
                        projectService.addEmbeddedlinkedRelease(sw360Release, sw360User, releaseResource,
                                releaseService, releaseIdsInBranch);
                    }
                    return releaseResource;
                })).collect(Collectors.toList());

        CollectionModel resources;
        if (releaseResources.size() == 0) {
            resources = restControllerHelper.emptyPageResource(Project.class, paginationResult);
        } else {
            resources = restControllerHelper.generatePagesResource(paginationResult, releaseResources);
        }

        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    @RequestMapping(value = PROJECTS_URL + "/releases", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<Release>>> getProjectsReleases(
            Pageable pageable,
            @RequestBody List<String> projectIds,
            @RequestParam(value = "transitive", required = false) String transitive,@RequestParam(value = "clearingState", required = false) String clState, HttpServletRequest request) throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {

        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Set<String> releaseIdsInBranch = new HashSet<>();
        boolean isTransitive = Boolean.parseBoolean(transitive);

        Set<Release> releases = projectService.getReleasesFromProjectIds(projectIds, transitive, sw360User,releaseService);

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
                    if (isTransitive) {
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

    @RequestMapping(value = PROJECTS_URL + "/{id}/releases/ecc", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<Release>>> getECCsOfReleases(
            @PathVariable("id") String id,
            @RequestParam(value = "transitive", required = false) String transitive) throws TException {

        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Set<String> releaseIds = projectService.getReleaseIds(id, sw360User, transitive);

        final List<EntityModel<Release>> releaseResources = new ArrayList<>();
        for (final String releaseId : releaseIds) {
            final Release sw360Release = releaseService.getReleaseForUserById(releaseId, sw360User);
            Release embeddedRelease = restControllerHelper.convertToEmbeddedRelease(sw360Release);
            embeddedRelease.setEccInformation(sw360Release.getEccInformation());
            final EntityModel<Release> releaseResource = EntityModel.of(embeddedRelease);
            releaseResources.add(releaseResource);
        }

        final CollectionModel<EntityModel<Release>> resources = restControllerHelper.createResources(releaseResources);
        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    @RequestMapping(value = PROJECTS_URL + "/{id}/vulnerabilities", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<VulnerabilityDTO>>> getVulnerabilitiesOfReleases(
            Pageable pageable,
            @PathVariable("id") String id, @RequestParam(value = "priority") Optional<String> priority,
            @RequestParam(value = "projectRelevance") Optional<String> projectRelevance,
            @RequestParam(value = "releaseId") Optional<String> releaseId,
            @RequestParam(value = "externalId") Optional<String> externalId,
            HttpServletRequest request) throws URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {
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

    @RequestMapping(value = PROJECTS_URL + "/{id}/vulnerabilities", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CollectionModel<EntityModel<VulnerabilityDTO>>> updateVulnerabilitiesOfReleases(
            @PathVariable("id") String id, @RequestBody List<VulnerabilityDTO> vulnDTOs) {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();

        List<VulnerabilityDTO> actualVDto = vulnerabilityService.getVulnerabilitiesByProjectId(id, sw360User);
        Set<String> actualExternalId = actualVDto.stream().map(VulnerabilityDTO::getExternalId).collect(Collectors.toSet());
        Set<String> externalIdsFromRequestDto = vulnDTOs.stream().map(VulnerabilityDTO::getExternalId).collect(Collectors.toSet());
        Set<String> commonExtIds = Sets.intersection(actualExternalId, externalIdsFromRequestDto);

        if(CommonUtils.isNullOrEmptyCollection(commonExtIds) || commonExtIds.size() != externalIdsFromRequestDto.size()) {
            throw new HttpMessageNotReadableException("External ID is not valid");
        }

        Set<String> actualReleaseIds = actualVDto.stream().map(VulnerabilityDTO::getIntReleaseId).collect(Collectors.toSet());
        Set<String> releaseIdsFromRequestDto = vulnDTOs.stream().map(VulnerabilityDTO::getIntReleaseId).collect(Collectors.toSet());
        Set<String> commonRelIds = Sets.intersection(actualReleaseIds, releaseIdsFromRequestDto);

        if(CommonUtils.isNullOrEmptyCollection(commonRelIds) || commonRelIds.size() != releaseIdsFromRequestDto.size()) {
            throw new HttpMessageNotReadableException("Release ID is not valid");
        }

        Optional<ProjectVulnerabilityRating> projectVulnerabilityRatings = wrapThriftOptionalReplacement(vulnerabilityService.getProjectVulnerabilityRatingByProjectId(id, sw360User));
        ProjectVulnerabilityRating link = updateProjectVulnerabilityRatingFromRequest(projectVulnerabilityRatings, vulnDTOs, id, sw360User);
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
    @RequestMapping(value = PROJECTS_URL + "/{id}/release/{releaseId}", method = RequestMethod.PATCH)
    public ResponseEntity<EntityModel<ProjectReleaseRelationship>> patchProjectReleaseUsage(
            @PathVariable("id") String id, @PathVariable("releaseId") String releaseId,
            @RequestBody ProjectReleaseRelationship requestBodyProjectReleaseRelationship) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Project sw360Project = projectService.getProjectForUserById(id, sw360User);
        Map<String, ProjectReleaseRelationship> releaseIdToUsage = sw360Project.getReleaseIdToUsage();
        ProjectReleaseRelationship updatedProjectReleaseRelationship = projectService
                .updateProjectReleaseRelationship(releaseIdToUsage, requestBodyProjectReleaseRelationship, releaseId);
        RequestStatus updateProjectStatus = projectService.updateProject(sw360Project, sw360User);
        if (updateProjectStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        EntityModel<ProjectReleaseRelationship> updatedProjectReleaseRelationshipResource = EntityModel.of(
                updatedProjectReleaseRelationship);
        return new ResponseEntity<>(updatedProjectReleaseRelationshipResource, HttpStatus.OK);
    }

    public ProjectVulnerabilityRating updateProjectVulnerabilityRatingFromRequest(Optional<ProjectVulnerabilityRating> projectVulnerabilityRatings, List<VulnerabilityDTO> vulDtoList, String projectId, User sw360User) {
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

    @RequestMapping(value = PROJECTS_URL + "/{id}/licenses", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<License>>> getLicensesOfReleases(@PathVariable("id") String id) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
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

    @RequestMapping(value = PROJECTS_URL + "/{id}/licenseinfo", method = RequestMethod.GET)
    public void downloadLicenseInfo(@PathVariable("id") String id,
                                    @RequestParam("generatorClassName") String generatorClassName,
                                    @RequestParam("variant") String variant,
                                    @RequestParam(value = "externalIds", required=false) String externalIds,
                                    @RequestParam(value = "template", required = false ) String template,
                                    HttpServletResponse response) throws TException, IOException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Project sw360Project = projectService.getProjectForUserById(id, sw360User);

        List<ProjectLink> mappedProjectLinks = projectService.createLinkedProjects(sw360Project,
                projectService.filterAndSortAttachments(SW360Constants.LICENSE_INFO_ATTACHMENT_TYPES), true, sw360User);

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
            Set<String> excludedLicenseIds = releaseIdToExcludedLicenses.get(Source.releaseId(releaseLinkId));

            if (!selectedReleaseAndAttachmentIds.containsKey(releaseLinkId)) {
                selectedReleaseAndAttachmentIds.put(releaseLinkId, new HashMap<>());
            }
            final List<Attachment> attachments = releaseLink.getAttachments();
            Release release = componentService.getReleaseById(releaseLinkId, sw360User);
            for (final Attachment attachment : attachments) {
                String attachemntContentId = attachment.getAttachmentContentId();
                if (usedAttachmentContentIds.containsKey(attachemntContentId)) {
                    boolean includeConcludedLicense = usedAttachmentContentIds.get(attachemntContentId);
                    List<LicenseInfoParsingResult> licenseInfoParsingResult = licenseInfoService
                            .getLicenseInfoForAttachment(release, sw360User, attachemntContentId, includeConcludedLicense);
                    excludedLicensesPerAttachments.put(attachemntContentId,
                            getExcludedLicenses(excludedLicenseIds, licenseInfoParsingResult));
                    selectedReleaseAndAttachmentIds.get(releaseLinkId).put(attachemntContentId,
                            includeConcludedLicense);
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

        final LicenseInfoFile licenseInfoFile = licenseInfoService.getLicenseInfoFile(sw360Project, sw360User, outputGeneratorClassNameWithVariant, selectedReleaseAndAttachmentIds, excludedLicensesPerAttachments, externalIds, fileName);
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

    @RequestMapping(value = PROJECTS_URL + "/{id}/attachments", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<Attachment>>> getProjectAttachments(
            @PathVariable("id") String id) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Project sw360Project = projectService.getProjectForUserById(id, sw360User);
        final CollectionModel<EntityModel<Attachment>> resources = attachmentService.getResourcesFromList(sw360Project.getAttachments());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = PROJECTS_URL + "/{id}/attachment/{attachmentId}", method = RequestMethod.PATCH)
    public ResponseEntity<EntityModel<Attachment>> patchProjectAttachmentInfo(@PathVariable("id") String id,
            @PathVariable("attachmentId") String attachmentId, @RequestBody Attachment attachmentData)
            throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Project sw360Project = projectService.getProjectForUserById(id, sw360User);
        Set<Attachment> attachments = sw360Project.getAttachments();
        Attachment updatedAttachment = attachmentService.updateAttachment(attachments, attachmentData, attachmentId, sw360User);
        RequestStatus updateProjectStatus = projectService.updateProject(sw360Project, sw360User);
        if (updateProjectStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        EntityModel<Attachment> attachmentResource = EntityModel.of(updatedAttachment);
        return new ResponseEntity<>(attachmentResource, HttpStatus.OK);
    }

    @RequestMapping(value = PROJECTS_URL + "/{projectId}/attachments/{attachmentId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadAttachmentFromProject(
            @PathVariable("projectId") String projectId,
            @PathVariable("attachmentId") String attachmentId,
            HttpServletResponse response) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Project project = projectService.getProjectForUserById(projectId, sw360User);
        this.attachmentService.downloadAttachmentWithContext(project, attachmentId, response, sw360User);
    }

    @RequestMapping(value = PROJECTS_URL + "/{projectId}/attachments/clearingReports", method = RequestMethod.GET, produces = "application/zip")
    public void downloadClearingReports(
            @PathVariable("projectId") String projectId,
            HttpServletResponse response) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Project project = projectService.getProjectForUserById(projectId, sw360User);
        final String filename = "Clearing-Reports-" + project.getName() + ".zip";


        final Set<Attachment> attachments = project.getAttachments();
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
            log.error(e.getMessage());
        }
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = PROJECTS_URL + "/{id}", method = RequestMethod.PATCH)
    public ResponseEntity<EntityModel<Project>> patchProject(
            @PathVariable("id") String id,
            @RequestBody Map<String, Object> reqBodyMap) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Project sw360Project = projectService.getProjectForUserById(id, user);
        Project updateProject = convertToProject(reqBodyMap);
        sw360Project = this.restControllerHelper.updateProject(sw360Project, updateProject, reqBodyMap, mapOfProjectFieldsToRequestBody);
        RequestStatus updateProjectStatus = projectService.updateProject(sw360Project, user);
        HalResource<Project> userHalResource = createHalProject(sw360Project, user);
        if (updateProjectStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(userHalResource, HttpStatus.OK);
    }

    @RequestMapping(value = PROJECTS_URL + "/{projectId}/attachments", method = RequestMethod.POST, consumes = {"multipart/mixed", "multipart/form-data"})
    public ResponseEntity<HalResource> addAttachmentToProject(@PathVariable("projectId") String projectId,
                                                              @RequestPart("file") MultipartFile file,
                                                              @RequestPart("attachment") Attachment newAttachment) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        final Project project = projectService.getProjectForUserById(projectId, sw360User);
        Attachment attachment = null;
        try {
            attachment = attachmentService.uploadAttachment(file, newAttachment, sw360User);
        } catch (IOException e) {
            log.error("failed to upload attachment", e);
            throw new RuntimeException("failed to upload attachment", e);
        }

        project.addToAttachments(attachment);
        RequestStatus updateProjectStatus = projectService.updateProject(project, sw360User);
        HttpStatus status = HttpStatus.OK;
        HalResource<Project> halResource = createHalProject(project, sw360User);
        if (updateProjectStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(halResource, status);
    }

    @RequestMapping(value = PROJECTS_URL + "/searchByExternalIds", method = RequestMethod.GET)
    public ResponseEntity searchByExternalIds(@RequestParam MultiValueMap<String, String> externalIdsMultiMap) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        return restControllerHelper.searchByExternalIds(externalIdsMultiMap, projectService, sw360User);
    }

    @RequestMapping(value = PROJECTS_URL + "/usedBy" + "/{id}", method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<Project>>> getUsedByProjectDetails(@PathVariable("id") String id) throws TException{
        User user = restControllerHelper.getSw360UserFromAuthentication();
        //Project sw360Project = projectService.getProjectForUserById(id, user);
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

    @RequestMapping(value = PROJECTS_URL + "/{id}/attachmentUsage", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<Map<String, Object>> getAttachmentUsage(@PathVariable("id") String id)
            throws TException, TTransportException {
        List<AttachmentUsage> attachmentUsages = attachmentService.getAllAttachmentUsage(id);
        String prefix = "{\"" + SW360_ATTACHMENT_USAGES + "\":[";
        String serializedUsages = attachmentUsages.stream()
                .map(usage -> wrapTException(() -> THRIFT_JSON_SERIALIZER.toString(usage)))
                .collect(Collectors.joining(",", prefix, "]}"));
        GsonJsonParser parser = new GsonJsonParser();
        Map<String, Object> attachmentUsageMap = parser.parseMap(serializedUsages);
        List<Map<String, Object>> listOfAttachmentUsages = (List<Map<String, Object>>) attachmentUsageMap
                .get(SW360_ATTACHMENT_USAGES);
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

        if (listOfAttachmentUsages.isEmpty()) {
            attachmentUsageMap = null;
        }

        HttpStatus status = attachmentUsageMap == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(attachmentUsageMap, status);
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ProjectController.class).slash("api" + PROJECTS_URL).withRel("projects"));
        return resource;
    }

    private HalResource<Project> createHalProject(Project sw360Project, User sw360User) throws TException {
        HalResource<Project> halProject = new HalResource<>(sw360Project);
        User projectCreator = restControllerHelper.getUserByEmail(sw360Project.getCreatedBy());
        restControllerHelper.addEmbeddedUser(halProject, projectCreator, "createdBy");

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
            HalResource<Vendor> vendorHalResource = restControllerHelper.addEmbeddedVendor(vendor.getFullname());
            halProject.addEmbeddedResource("sw360:vendors", vendorHalResource);
            sw360Project.setVendor(null);
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
            throw new HttpMessageNotReadableException(
                    "Request body should be List of valid release id or map of release id to usage");
        }
        project.setReleaseIdToUsage(releaseIdToUsage);
        return projectService.updateProject(project, sw360User);
    }

    private HalResource<Project> createHalProjectResourceWithAllDetails(Project sw360Project, User sw360User,
            Map<String, Project> mapOfProjects, boolean isAllAccessibleProjectFetched) {
        Map<String, ProjectProjectRelationship> linkedProjects = sw360Project.getLinkedProjects();
        if (!isLinkedProjectsVisible(linkedProjects, sw360User, mapOfProjects, isAllAccessibleProjectFetched)) {
            return null;
        }
        HalResource<Project> halProject = new HalResource<>(sw360Project);
        halProject.addEmbeddedResource("createdBy", sw360Project.getCreatedBy());

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

    private boolean isLinkedProjectsVisible(Map<String, ProjectProjectRelationship> linkedProjects, User sw360User,
            Map<String, Project> mapOfProjects, boolean isAllAccessibleProjectFetched) {
        if (isAllAccessibleProjectFetched && !CommonUtils.isNullOrEmptyMap(linkedProjects)) {
            for (String linkedProjectId : linkedProjects.keySet()) {
                if (!mapOfProjects.containsKey(linkedProjectId)) {
                    return false;
                }
            }
        } else if (!CommonUtils.isNullOrEmptyMap(linkedProjects)) {
            for (String linkedProjectId : linkedProjects.keySet()) {
                if (!mapOfProjects.containsKey(linkedProjectId)) {
                    try {
                        projectService.getProjectForUserById(linkedProjectId, sw360User);
                    } catch (TException exp) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private Project convertToProject(Map<String, Object> requestBody) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(sw360Module);

        if (requestBody.containsKey("linkedProjects")) {
            Map<String, Object> linkedProjects = (Map<String, Object>) requestBody.get("linkedProjects");
            linkedProjects.entrySet().stream().forEach(entry -> {
                if (entry.getValue() instanceof String) {
                    Map<String, Object> projectProjectRelationShip = new HashMap<String, Object>();
                    projectProjectRelationShip.put("projectRelationship", entry.getValue());
                    linkedProjects.put(entry.getKey(), projectProjectRelationShip);
                }
            });

        }
        return mapper.convertValue(requestBody, Project.class);
    }

    public static TSerializer getJsonSerializer() {
        try {
            return new TSerializer(new TSimpleJSONProtocol.Factory());
        } catch (TTransportException e) {
            log.error("Error creating TSerializer " + e);
        }
        return null;
    }
}
