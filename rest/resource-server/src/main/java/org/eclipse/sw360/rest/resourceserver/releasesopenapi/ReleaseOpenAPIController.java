package org.eclipse.sw360.rest.resourceserver.releasesopenapi;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.componentopenapi.ComponentServicePG;
import org.eclipse.sw360.rest.resourceserver.componentopenapi.ComponentsOpenAPIController;
import org.eclipse.sw360.datahandler.postgres.ComponentPG;
import org.eclipse.sw360.datahandler.postgres.ReleasePG;
import org.eclipse.sw360.datahandler.postgres.VulnerabilityPG;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelperPG;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import java.net.URISyntaxException;
import java.util.List;
import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@ConditionalOnProperty(name = "feature.experimental.enabled", havingValue = "true")
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
@Validated
public class ReleaseOpenAPIController
                implements RepresentationModelProcessor<RepositoryLinksResource> {

        @NonNull
        private final RestControllerHelper restControllerHelper;

        @NonNull
        private final RestControllerHelperPG restControllerHelperPG;

        private ReleaseServicePG releaseService = new ReleaseServicePG();

        private ComponentServicePG componentService = new ComponentServicePG();

        private final String RELEASES_URL = "/releasesOpenAPI";

        @Override
        public RepositoryLinksResource process(RepositoryLinksResource resource) {
                resource.add(linkTo(ComponentsOpenAPIController.class).slash("api/releasesOpenAPI")
                                .withRel("releasesOpenAPI"));
                return resource;
        }

        @PreAuthorize("hasAuthority('WRITE')")
        @Operation(summary = "Create a release.", description = "Create a new release.",
                        tags = {"Releases"})
        @RequestMapping(value = RELEASES_URL, method = RequestMethod.POST)
        public ResponseEntity<EntityModel<ReleasePG>> createRelease(@Parameter(
                        description = "The release to be created.") @RequestBody ReleasePG release)
                        throws URISyntaxException, TException {
                User sw360User = restControllerHelper.getSw360UserFromAuthentication();

                if (release.getComponentId() == null) {
                        throw new IllegalArgumentException("Component ID is required");
                }

                ReleasePG sw360Release = releaseService.createRelease(release, sw360User);

                URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                                .buildAndExpand(sw360Release.getId()).toUri();

                return ResponseEntity.created(location)
                                .body(EntityModel.of(sw360Release)
                                                .add(linkTo(ReleaseOpenAPIController.class)
                                                                .slash("api/releasesOpenAPI")
                                                                .withRel("releasesOpenAPI")));
        }

        private HalResource<ReleasePG> createHalReleaseResource(ReleasePG release, boolean verbose)
                        throws TException {
                ComponentPG component = release.getComponent() != null ? componentService
                                .getComponentById(release.getComponent().getId().toString()) : null;

                List<VulnerabilityPG> vulnerabilities = release.getVulnerabilityRelation() != null
                                ? release.getVulnerabilityRelation().stream()
                                                .map(vuln -> vuln.getVulnerability()).toList()
                                : List.of();

                HalResource<ReleasePG> halRelease = new HalResource<>(release);
                if (component != null) {
                        restControllerHelperPG.addEmbeddedComponentPG(halRelease, component);
                }
                if (vulnerabilities != null && !vulnerabilities.isEmpty()) {
                        restControllerHelperPG.addEmbeddedVulnerabilitiesPG(halRelease,
                                        vulnerabilities);
                }

                return halRelease;

        }

        @Operation(summary = "Get a release by ID.", description = "Get a single release by ID.",
                        tags = {"Releases"})
        @GetMapping(value = RELEASES_URL + "/{id}")
        public ResponseEntity<EntityModel<ReleasePG>> getRelease(@Parameter(
                        description = "The ID of the release to be retrieved.") @PathVariable("id") String id)
                        throws TException {
                User sw360User = restControllerHelper.getSw360UserFromAuthentication();
                ReleasePG sw360Release = releaseService.getReleaseForUserById(id, sw360User);
                HalResource<ReleasePG> halRelease = createHalReleaseResource(sw360Release, true);

                return new ResponseEntity<>(halRelease, HttpStatus.OK);
        }

        @PatchMapping(value = RELEASES_URL + "/{id}")
        @Operation(summary = "Update a release by ID.",
                        description = "Update a single release by ID.", tags = {"Releases"})
        public ResponseEntity<EntityModel<ReleasePG>> updateRelease(@Parameter(
                        description = "The ID of the release to be updated.") @PathVariable("id") String id,
                        @Parameter(description = "The release data to be updated.") @RequestBody ReleasePG release)
                        throws TException {
                User sw360User = restControllerHelper.getSw360UserFromAuthentication();
                if (release.getId() == null || !release.getId().toString().equals(id)) {
                        throw new IllegalArgumentException(
                                        "Release ID in the body must match the path ID");
                }
                ReleasePG updatedRelease = releaseService.updateRelease(id, release, sw360User);
                HalResource<ReleasePG> halRelease = createHalReleaseResource(updatedRelease, true);
                return new ResponseEntity<>(halRelease, HttpStatus.OK);
        }

        @Operation(summary = "List all of the service's releases.",
                        description = "List all of the service's releases.", tags = {"Releases"})
        @GetMapping(value = RELEASES_URL)
        public ResponseEntity<CollectionModel<HalResource<ReleasePG>>> getReleasesForUser(
                        @Parameter(description = "The page number to retrieve.") @RequestParam(
                                        defaultValue = "0") int page,
                        @Parameter(description = "The number of items per page.") @RequestParam(
                                        defaultValue = "10") int size)
                        throws PaginationParameterException, ResourceClassNotFoundException,
                        TException {
                User sw360User = restControllerHelper.getSw360UserFromAuthentication();
                List<ReleasePG> releases = releaseService.getReleasesForUser(sw360User, page, size);

                List<HalResource<ReleasePG>> halReleases = releases.stream().map(release -> {
                        try {
                                return createHalReleaseResource(release, true);
                        } catch (TException e) {
                                throw new RuntimeException(e);
                        }
                }).toList();

                CollectionModel<HalResource<ReleasePG>> collectionModel =
                                CollectionModel.of(halReleases);
                collectionModel.add(linkTo(ReleaseOpenAPIController.class)
                                .slash("api/releasesOpenAPI").withRel("releasesOpenAPI"));

                return new ResponseEntity<>(collectionModel, HttpStatus.OK);
        }
}
