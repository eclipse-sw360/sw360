package org.eclipse.sw360.rest.resourceserver.projectopenapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.componentsApi.model.ComponentAPI;
import org.eclipse.sw360.datahandler.componentsApi.model.ProjectAPI;
import org.eclipse.sw360.datahandler.componentsApi.model.ReleaseAPI;
import org.eclipse.sw360.datahandler.postgres.ComponentPG;
import org.eclipse.sw360.datahandler.postgres.ProjectPG;
import org.eclipse.sw360.datahandler.postgres.ReleasePG;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.componentopenapi.ComponentsOpenAPIController;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelperPG;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@ConditionalOnProperty(name = "feature.experimental.enabled", havingValue = "true")
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
@Validated
public class ProjectsOpenAPIController
                implements RepresentationModelProcessor<RepositoryLinksResource> {

        public static final String PROJECTS_URL = "/projectsOpenAPI";

        @NonNull
        private final RestControllerHelper restControllerHelper;

        @NonNull
        private final RestControllerHelperPG restControllerHelperPG;

        private final ProjectServicePG projectServicePG = new ProjectServicePG();

        private final Logger logger = LogManager.getLogger(ComponentsOpenAPIController.class);

        @Override
        public RepositoryLinksResource process(RepositoryLinksResource resource) {
                resource.add(linkTo(ProjectsOpenAPIController.class).slash("api/projectsOpenAPI")
                                .withRel("projectsOpenAPI"));
                return resource;
        }

        @PreAuthorize("hasAuthority('WRITE')")
        @Operation(description = "Create a project.", tags = {"Projects"})
        @PostMapping(value = PROJECTS_URL)
        public ResponseEntity<EntityModel<ProjectPG>> createProject(@Parameter(
                        description = "The project to be created.") @RequestBody ProjectPG project) {

                User user = restControllerHelper.getSw360UserFromAuthentication();

                ProjectPG result = projectServicePG.createProject(project, user);
                URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                                .buildAndExpand(result.getId()).toUri();

                HalResource<ProjectPG> halResource = new HalResource<>(result);
                return ResponseEntity.created(location).body(halResource);
        }

        @Operation(description = "Get a single project.", tags = {"Projects"})
        @GetMapping(value = PROJECTS_URL + "/{id}")
        public ResponseEntity<EntityModel<ProjectPG>> getProject(
                        @Parameter(description = "Project ID",
                                        example = "376576") @PathVariable("id") String id)
                        throws Exception {

                ProjectPG project = projectServicePG.getProjectForUserById(id,
                                restControllerHelper.getSw360UserFromAuthentication());

                HalResource<ProjectPG> halProject = createHalProject(project,
                                restControllerHelper.getSw360UserFromAuthentication());
                return ResponseEntity.ok(halProject);
        }

        @Operation(summary = "List all of the service's projects.",
                        description = "List all of the service's projects with various filters.",
                        tags = {"Projects"})
        @RequestMapping(value = PROJECTS_URL, method = RequestMethod.GET)
        public ResponseEntity<CollectionModel<HalResource<ProjectPG>>> getProjectsForUser(
                        Pageable pageable,
                        @Parameter(description = "The name of the project") @RequestParam(
                                        value = "name", required = false) String name,
                        @Parameter(description = "The type of the project") @RequestParam(
                                        value = "type", required = false) String projectType,
                        @Parameter(description = "The group of the project") @RequestParam(
                                        value = "group", required = false) String group,
                        @Parameter(description = "The tag of the project") @RequestParam(
                                        value = "tag", required = false) String tag,
                        @Parameter(description = "Flag to get projects with all details.") @RequestParam(
                                        value = "allDetails", required = false) boolean allDetails,
                        @Parameter(description = "The version of the project") @RequestParam(
                                        value = "version", required = false) String version,
                        @Parameter(description = "The projectResponsible of the project") @RequestParam(
                                        value = "projectResponsible",
                                        required = false) String projectResponsible,
                        @Parameter(description = "The additionalData of the project") @RequestParam(
                                        value = "additionalData",
                                        required = false) String additionalData,
                        @Parameter(description = "List project by lucene search") @RequestParam(
                                        value = "luceneSearch",
                                        required = false) boolean luceneSearch,
                        HttpServletRequest request) throws Exception, URISyntaxException,
                        PaginationParameterException, ResourceClassNotFoundException {
                User sw360User = restControllerHelper.getSw360UserFromAuthentication();

                String queryString = request.getQueryString();
                Map<String, String> params = restControllerHelper.parseQueryString(queryString);

                List<HalResource<ProjectPG>> halProjects =
                                projectServicePG.getProjectsForUser(sw360User, pageable, params)
                                                .stream().map(project -> {
                                                        try {
                                                                return createHalProject(project,
                                                                                sw360User);
                                                        } catch (Exception e) {
                                                                throw new RuntimeException(
                                                                                "Error creating HalProject",
                                                                                e);
                                                        }
                                                }).toList();
                CollectionModel<HalResource<ProjectPG>> collectionModel =
                                CollectionModel.of(halProjects);
                collectionModel.add(linkTo(ProjectsOpenAPIController.class)
                                .slash("api/projectsOpenAPI").withRel("projectsOpenAPI"));
                return ResponseEntity.ok(collectionModel);

        }

        @Operation(description = "Delete a single project.", tags = {"Projects"}, responses = {
                        @ApiResponse(responseCode = "200", description = "Project deleted."),
                        @ApiResponse(responseCode = "202",
                                        description = "Request sent for moderation.",
                                        content = {@Content(mediaType = "application/json",
                                                        examples = @ExampleObject(
                                                                        value = "{\"message\": \"Moderation request is created\"}"))}),
                        @ApiResponse(responseCode = "409",
                                        description = "The project is used as a linked project. Cannot delete it."),
                        @ApiResponse(responseCode = "500",
                                        description = "Failed to delete project.")})
        @DeleteMapping(value = PROJECTS_URL + "/{id}")
        public ResponseEntity<Void> deleteProject(
                        @Parameter(description = "Project ID") @PathVariable("id") String id,
                        @Parameter(description = "Comment message") @RequestParam(value = "comment",
                                        required = false) String comment)
                        throws TException {
                User sw360User = restControllerHelper.getSw360UserFromAuthentication();

                RequestStatus requestStatus = projectServicePG.deleteProject(id, sw360User);
                if (requestStatus == RequestStatus.SUCCESS) {
                        return new ResponseEntity<>(HttpStatus.OK);
                } else if (requestStatus == RequestStatus.IN_USE) {
                        return new ResponseEntity<>(HttpStatus.CONFLICT);
                } else {
                        throw new SW360Exception("Something went wrong.");
                }
        }

        @PreAuthorize("hasAuthority('WRITE')")
        @Operation(description = "Update a project.", tags = {"Projects"})
        @PatchMapping(value = PROJECTS_URL + "/{id}")
        public ResponseEntity<EntityModel<ProjectPG>> patchProject(
                        @Parameter(description = "Project ID.") @PathVariable("id") String id,
                        @Parameter(description = "Updated values. Add `comment` field in the body for moderation request.") @RequestBody ProjectAPI sw360Project)
                        throws TException {
                User user = restControllerHelper.getSw360UserFromAuthentication();

                if (projectServicePG.getProjectForUserById(id, user) == null) {
                        throw new ResourceNotFoundException("Project not found");
                } else {
                        ProjectPG updatedproject =
                                        projectServicePG.updateProject(id, sw360Project, user);

                        return ResponseEntity.ok(new HalResource<>(updatedproject));
                }
        }

        @PreAuthorize("hasAuthority('WRITE')")
        @Operation(summary = "Link releases to the project.",
                        description = "Pass an array of release ids to be linked as request body.",
                        tags = {"Projects"})
        @PostMapping(value = PROJECTS_URL + "/{id}/releases")
        public ResponseEntity<Object> linkReleases(
                        @Parameter(description = "Project ID.") @PathVariable("id") String id,
                        @Parameter(description = "Array of release IDs to be linked.", examples = {
                                        @ExampleObject(value = "[\"3765276512\",\"5578999\",\"3765276513\"]"),
                                        @ExampleObject(value = "[\"/releases/5578999\"]")}) @RequestBody List<ReleaseAPI> releases,
                        @Parameter(description = "Comment message.") @RequestParam(
                                        value = "comment", required = false) String comment)
                        throws URISyntaxException, TException {
                return ResponseEntity.status(HttpStatus.OK)
                                .location(new URI(ServletUriComponentsBuilder.fromCurrentRequest()
                                                .path("/{id}").buildAndExpand(id).toString()))
                                .body(projectServicePG.linkReleases(id, releases, comment));
        }

        private HalResource<ProjectPG> createHalProject(ProjectPG sw360Project, User sw360User) {
                ProjectPG project = sw360Project;

                List<ComponentPG> components = sw360Project.getComponentsPG();
                HalResource<ProjectPG> halProject = new HalResource<>(project);

                restControllerHelperPG.addEmbeddedComponents(halProject, components);

                for (ComponentPG component : components) {
                        List<ReleasePG> releases = component.getReleasesPG();
                        logger.info("Project {} releases: {}", sw360Project.getId(), releases);
                        if (releases != null && !releases.isEmpty()) {
                                restControllerHelperPG.addEmbeddedReleasesPG(halProject, releases);

                                for (ReleasePG release : releases) {
                                        logger.info("Project releases vulnerabilities: {}", release
                                                        .getVulnerabilityRelation().stream()
                                                        .map(relation -> relation
                                                                        .getVulnerability())
                                                        .toList());
                                        if (release.getVulnerabilityRelation() != null) {
                                                restControllerHelperPG.addEmbeddedVulnerabilitiesPG(
                                                                halProject,
                                                                release.getVulnerabilityRelation()
                                                                                .stream()
                                                                                .map(relation -> relation
                                                                                                .getVulnerability())
                                                                                .toList());
                                        }

                                }

                        }
                }

                halProject.add(linkTo(ProjectsOpenAPIController.class)
                                .slash("api/projectsOpenAPI/" + sw360Project.getId())
                                .withRel("project"));

                return halProject;
        }

        @PreAuthorize("hasAuthority('WRITE')")
        @Operation(summary = "Add/link packages to the project.",
                        description = "Pass a set of package ids to be linked as request body.",
                        responses = {@ApiResponse(responseCode = "201",
                                        description = "Packages are linked to the project."),
                                        @ApiResponse(responseCode = "202",
                                                        description = "Moderation request is created.")},
                        tags = {"Projects"})
        @RequestMapping(value = PROJECTS_URL + "/{id}/link/components",
                        method = RequestMethod.PATCH)
        public ResponseEntity<?> linkComponents(
                        @Parameter(description = "Project ID.") @PathVariable("id") String id,
                        @RequestBody List<ComponentAPI> components,
                        @Parameter(description = "Comment message.") @RequestParam(
                                        value = "comment", required = false) String comment)
                        throws URISyntaxException, TException {
                return ResponseEntity.status(HttpStatus.OK)
                                .location(new URI(ServletUriComponentsBuilder.fromCurrentRequest()
                                                .path("/{id}").buildAndExpand(id).toString()))
                                .body(projectServicePG.linkComponents(id, components, comment));
        }

}
