package org.eclipse.sw360.rest.resourceserver.componentopenapi;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.postgres.ReleasePG;
import org.eclipse.sw360.datahandler.postgres.VulnerabilityPG;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.MultiStatus;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelperPG;
import org.eclipse.sw360.rest.resourceserver.releasesopenapi.ReleaseServicePG;
import org.eclipse.sw360.rest.resourceserver.user.UserController;
import org.eclipse.sw360.rest.resourceserver.vendor.VendorController;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.eclipse.sw360.datahandler.postgres.ComponentPG;
import java.util.Set;
import org.eclipse.sw360.datahandler.componentsApi.model.VendorAPI;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.eclipse.sw360.datahandler.componentsApi.model.ComponentAPI;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@ConditionalOnProperty(name = "feature.experimental.enabled", havingValue = "true")
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
@Validated
public class ComponentsOpenAPIController
                implements RepresentationModelProcessor<RepositoryLinksResource> {

        public static final String COMPONENTS_URL = "/componentsOpenAPI";

        @NonNull
        private final RestControllerHelper restControllerHelper;

        @NonNull
        private final RestControllerHelperPG restControllerHelperPG;

        private ComponentServicePG componentService = new ComponentServicePG();

        private ReleaseServicePG releaseService = new ReleaseServicePG();

        private final Logger logger = LogManager.getLogger(ComponentsOpenAPIController.class);

        public static String urlDecode(String str) {
                if (str == null) {
                        return null;
                }
                try {
                        return URLDecoder.decode(str, StandardCharsets.UTF_8.name());
                } catch (UnsupportedEncodingException e) {
                        // This exception occurs if the specified encoding is not supported
                        throw new IllegalArgumentException(
                                        "Unsupported encoding: " + e.getMessage());
                }
        }

        public Map<String, String> parseQueryString(String queryString) {
                Map<String, String> parameters = new HashMap<>();
                if (queryString != null && !queryString.isEmpty()) {
                        UriComponentsBuilder builder =
                                        UriComponentsBuilder.newInstance().query(queryString);
                        builder.build().getQueryParams().forEach((key, values) -> parameters
                                        .put(key, urlDecode(values.get(0))));
                }
                return parameters;
        }

        @Override
        public RepositoryLinksResource process(RepositoryLinksResource resource) {
                resource.add(linkTo(ComponentsOpenAPIController.class)
                                .slash("api/componentsOpenAPI").withRel("componentsOpenAPI"));
                return resource;
        }

        private HalResource<ComponentPG> createHalComponent(ComponentPG sw360Component, User user)
                        throws TException {

                ComponentPG component = new ComponentPG(sw360Component);
                List<org.eclipse.sw360.datahandler.componentsApi.model.ReleaseAPI> releases =
                                new ArrayList<>();
                try {
                        releases = sw360Component.getReleases() != null
                                        ? sw360Component.getReleases().stream()
                                                        .collect(Collectors.toList())
                                        : new ArrayList<>();
                } catch (Exception e) {
                        throw new TException("Error while fetching releases for component: "
                                        + sw360Component.getId(), e);
                }
                List<VulnerabilityPG> vulnerabilities = new ArrayList<>();
                try {
                        if (releases != null && !releases.isEmpty()) {
                                for (ReleasePG release : sw360Component.getReleasesPG()) {
                                        if (release.getVulnerabilityRelation() != null) {
                                                vulnerabilities.addAll(release
                                                                .getVulnerabilityRelation().stream()
                                                                .map(vuln -> vuln
                                                                                .getVulnerability())
                                                                .toList());
                                        }
                                }
                        }
                } catch (Exception e) {
                        throw new TException("Error while fetching vulnerabilities for component: "
                                        + sw360Component.getId(), e);
                }

                HalResource<ComponentPG> halComponent = new HalResource<>(component);

                if (releases != null && !releases.isEmpty()) {
                        restControllerHelperPG.addEmbeddedReleasesPG(halComponent, releases);
                } else {
                        logger.info("Component has no releases.");
                }

                if (sw360Component.getVendors() != null)

                {
                        List<VendorAPI> vendors = sw360Component.getVendors();

                        Set<String> vendorNames = vendors.stream()
                                        .map(vendor -> vendor.getFullname().toLowerCase())
                                        .collect(Collectors.toSet());
                        restControllerHelperPG.addEmbeddedVendorsPG(halComponent, vendorNames);
                }

                if (!vulnerabilities.isEmpty()) {
                        restControllerHelperPG.addEmbeddedVulnerabilitiesPG(halComponent,
                                        vulnerabilities);
                }

                return halComponent;
        }

        private void addEmbeddedDefaultVendor(HalResource<ComponentPG> halComponent,
                        VendorAPI defaultVendor) {
                HalResource<VendorAPI> halDefaultVendor = new HalResource<>(defaultVendor);
                Link vendorSelfLink =
                                linkTo(UserController.class)
                                                .slash("api" + VendorController.VENDORS_URL + "/"
                                                                + defaultVendor.getId())
                                                .withSelfRel();
                halDefaultVendor.add(vendorSelfLink);
                halComponent.addEmbeddedResource("defaultVendor", halDefaultVendor);
        }

        private CollectionModel getFilteredComponentResources(List<String> fields,
                        boolean allDetails, User sw360User,
                        PaginationResult<ComponentPG> paginationResult)

                        throws URISyntaxException {
                List<EntityModel<ComponentPG>> componentResources = new ArrayList<>();

                paginationResult.getResources().stream().forEach(c -> {
                        try {
                                EntityModel<ComponentPG> embeddedComponentResource =
                                                createHalComponent(c, sw360User);
                                embeddedComponentResource
                                                .add(linkTo(ComponentsOpenAPIController.class)
                                                                .slash("api" + COMPONENTS_URL + "/"
                                                                                + c.getId())
                                                                .withSelfRel());
                                componentResources.add(embeddedComponentResource);
                        } catch (Exception e) {
                                throw new RuntimeException(e);
                        }
                });

                CollectionModel resources;
                if (componentResources.isEmpty()) {
                        resources = restControllerHelper.emptyPageResource(ComponentPG.class,
                                        paginationResult);
                } else {
                        resources = restControllerHelper.generatePagesResource(paginationResult,
                                        componentResources);
                }
                return resources;
        }

        @RequestMapping(value = COMPONENTS_URL, method = RequestMethod.GET)
        public ResponseEntity<CollectionModel<EntityModel<ComponentPG>>> getComponents(
                        HttpServletRequest request,
                        org.springframework.data.domain.Pageable pageable,
                        @Parameter(name = "name", description = "Name of the component to filter",
                                        in = ParameterIn.QUERY) @Valid @RequestParam(value = "name",
                                                        required = false) String name,
                        @Parameter(name = "categories",
                                        description = "Categories of the component to filter, as a comma-separated list.",
                                        in = ParameterIn.QUERY) @Valid @RequestParam(
                                                        value = "categories",
                                                        required = false) String categories,
                        @Parameter(name = "type", description = "Type of the component to filter",
                                        in = ParameterIn.QUERY) @Valid @RequestParam(value = "type",
                                                        required = false) String type,
                        @Parameter(name = "languages",
                                        description = "Component languages to filter, as a comma-separated list.",
                                        in = ParameterIn.QUERY) @Valid @RequestParam(
                                                        value = "languages",
                                                        required = false) String languages,
                        @Parameter(name = "softwarePlatforms",
                                        description = "Software Platforms to filter, as a comma-separated list.",
                                        in = ParameterIn.QUERY) @Valid @RequestParam(
                                                        value = "softwarePlatforms",
                                                        required = false) String softwarePlatforms,
                        @Parameter(name = "operatingSystems",
                                        description = "Operating Systems to filter, as a comma-separated list.",
                                        in = ParameterIn.QUERY) @Valid @RequestParam(
                                                        value = "operatingSystems",
                                                        required = false) String operatingSystems,
                        @Parameter(name = "vendors",
                                        description = "Vendors to filter, as a comma-separated list.",
                                        in = ParameterIn.QUERY) @Valid @RequestParam(
                                                        value = "vendors",
                                                        required = false) String vendors,
                        @Parameter(name = "mainLicenses",
                                        description = "Main Licenses to filter, as a comma-separated list.",
                                        in = ParameterIn.QUERY) @Valid @RequestParam(
                                                        value = "mainLicenses",
                                                        required = false) String mainLicenses,
                        @Parameter(name = "createdBy",
                                        description = "Created by user to filter (email).",
                                        in = ParameterIn.QUERY) @Valid @RequestParam(
                                                        value = "createdBy",
                                                        required = false) String createdBy,
                        @Parameter(name = "createdOn",
                                        description = "Date component was created on (YYYY-MM-DD).",
                                        in = ParameterIn.QUERY) @Valid @RequestParam(
                                                        value = "createdOn",
                                                        required = false) @DateTimeFormat(
                                                                        iso = DateTimeFormat.ISO.DATE) LocalDate createdOn,
                        @Parameter(name = "fields",
                                        description = "Properties which should be present for each component in the result",
                                        in = ParameterIn.QUERY) @Valid @RequestParam(
                                                        value = "fields",
                                                        required = false) List<String> fields,
                        @Parameter(name = "allDetails",
                                        description = "Flag to get components with all details.",
                                        in = ParameterIn.QUERY) @Valid @RequestParam(
                                                        value = "allDetails",
                                                        required = false) Boolean allDetails,
                        @Parameter(name = "luceneSearch",
                                        description = "Use lucenesearch to filter the components.",
                                        in = ParameterIn.QUERY) @Valid @RequestParam(
                                                        value = "luceneSearch",
                                                        required = false) Boolean luceneSearch)
                        throws TException, URISyntaxException, PaginationParameterException,
                        ResourceClassNotFoundException {
                String queryString = request.getQueryString();
                Map<String, String> params = restControllerHelper.parseQueryString(queryString);
                User sw360User = restControllerHelper.getSw360UserFromAuthentication();
                List<ComponentPG> allComponents = componentService.listOAPIComponents(params);
                PaginationResult<ComponentPG> paginationResult =
                                restControllerHelper.createPaginationResult(request, pageable,
                                                allComponents, SW360Constants.TYPE_COMPONENT);

                allDetails = allDetails == null ? false : allDetails;
                CollectionModel resources = getFilteredComponentResources(fields, allDetails,
                                sw360User, paginationResult);

                return new ResponseEntity<>(resources, HttpStatus.OK);
        }

        @Operation(summary = "Get a single component.",
                        description = "Get a single component by its id.", tags = {"Components"})
        @RequestMapping(value = COMPONENTS_URL + "/{id}", method = RequestMethod.GET)
        public ResponseEntity<EntityModel<ComponentPG>> getComponent(@Parameter(
                        description = "The id of the component to be retrieved.") @PathVariable("id") String id)
                        throws TException {
                User user = restControllerHelper.getSw360UserFromAuthentication();
                org.eclipse.sw360.datahandler.postgres.ComponentPG sw360Component =
                                componentService.getComponentById(id);

                HalResource<ComponentPG> userHalResource = createHalComponent(sw360Component, user);
                restControllerHelperPG.addEmbeddedDataToComponentOAPI(userHalResource,
                                sw360Component);
                return new ResponseEntity<>(userHalResource, HttpStatus.OK);
        }

        @Operation(summary = "Get recently created components.",
                        description = "Return 5 of the service's most recently created components.",
                        tags = {"Components"})
        @GetMapping(value = COMPONENTS_URL + "/recentComponents")
        public ResponseEntity<CollectionModel<EntityModel<org.eclipse.sw360.datahandler.postgres.ComponentPG>>> getRecentComponent() {
                List<org.eclipse.sw360.datahandler.postgres.ComponentPG> recentComponents =
                                componentService.getRecentComponents(5);
                List<EntityModel<org.eclipse.sw360.datahandler.postgres.ComponentPG>> componentResources =
                                recentComponents.stream().map(EntityModel::of).toList();
                CollectionModel<EntityModel<org.eclipse.sw360.datahandler.postgres.ComponentPG>> resources =
                                CollectionModel.of(componentResources);
                return new ResponseEntity<>(resources, HttpStatus.OK);
        }

        @Operation(summary = "Get components by external ID.",
                        description = "Get components by external ID.", tags = {"Components"})
        @GetMapping(value = COMPONENTS_URL + "/searchByExternalIds")
        public ResponseEntity<CollectionModel<EntityModel<ComponentPG>>> searchByExternalIds(
                        @Parameter(description = "The external IDs of the components to be retrieved.",
                                        example = "component-id-key=1831A3&component-id-key=c77321") HttpServletRequest request) {
                String queryString = request.getQueryString();
                Map<String, String> externalIds = parseQueryString(queryString);
                List<ComponentPG> components =
                                componentService.getComponentsByExternalIds(externalIds);
                List<EntityModel<ComponentPG>> componentResources =
                                components.stream().map(EntityModel::of).toList();
                CollectionModel<EntityModel<ComponentPG>> resources =
                                CollectionModel.of(componentResources);
                return new ResponseEntity<>(resources, HttpStatus.OK);
        }

        @PreAuthorize("hasAuthority('WRITE')")
        @Operation(summary = "Delete existing components.",
                        description = "Delete existing components by ids.", tags = {"Components"})
        @DeleteMapping(value = COMPONENTS_URL + "/{ids}")
        public ResponseEntity<List<MultiStatus>> deleteComponents(@Parameter(
                        description = "The ids of the components to be deleted.") @PathVariable("ids") List<String> idsToDelete) {
                User user = restControllerHelper.getSw360UserFromAuthentication();
                List<MultiStatus> results = new ArrayList<>();
                for (String id : idsToDelete) {
                        HttpStatus requestStatus = componentService.deleteComponent(id, user);
                        if (requestStatus == HttpStatus.OK) {
                                results.add(new MultiStatus(id, HttpStatus.OK));
                        } else {
                                results.add(new MultiStatus(id, HttpStatus.INTERNAL_SERVER_ERROR));
                        }
                }
                return new ResponseEntity<>(results, HttpStatus.MULTI_STATUS);
        }

        @PreAuthorize("hasAuthority('WRITE')")
        @Operation(summary = "Create a new component.", description = "Create a new component.",
                        tags = {"Components"})
        @PostMapping(value = COMPONENTS_URL)
        public ResponseEntity<EntityModel<ComponentPG>> createComponent(@Parameter(
                        description = "The component to be created.") @RequestBody ComponentAPI component)
                        throws TException {

                if (component.getName().isBlank() || component.getName().isEmpty()) {
                        throw new IllegalArgumentException("Component name is required");
                }

                User user = restControllerHelper.getSw360UserFromAuthentication();

                if (component.getReleases() == null) {
                        component.setReleases(new ArrayList<>());
                }

                ComponentPG createdComponent = componentService.createComponent(component, user);
                HalResource<ComponentPG> halResource = createHalComponent(createdComponent, user);

                URI location = UriComponentsBuilder.fromPath(COMPONENTS_URL).path("/{id}")
                                .buildAndExpand(createdComponent.getId().toString()).toUri();

                return ResponseEntity.created(location).body(halResource);
        }

        @Operation(summary = "Get all releases of a component.",
                        description = "Get all releases of a component.", tags = {"Components"})
        @GetMapping(value = COMPONENTS_URL + "/{id}/releases")
        public ResponseEntity<CollectionModel<ReleasePG>> getReleaseLinksByComponentId(@Parameter(
                        description = "The id of the component.") @PathVariable("id") String id) {
                final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
                List<ReleasePG> releases = releaseService.getReleasesByComponentId(id, sw360User);
                CollectionModel<ReleasePG> releaseResources = CollectionModel.of(releases);

                return new ResponseEntity<>(releaseResources, HttpStatus.OK);
        }
}
