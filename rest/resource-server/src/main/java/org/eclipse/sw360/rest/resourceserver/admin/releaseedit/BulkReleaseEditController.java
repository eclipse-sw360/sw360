package org.eclipse.sw360.rest.resourceserver.admin.releaseedit;

import com.google.common.collect.ImmutableMap;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.eclipse.sw360.rest.resourceserver.admin.fossology.FossologyAdminController;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.datahandler.common.WrappedException.wrapTException;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class BulkReleaseEditController implements RepresentationModelProcessor<RepositoryLinksResource> {
    public static final String BULK_RELEASE_EDIT_URL = "/bulkReleaseEdit";

    ThriftClients thriftClients = new ThriftClients();
    ComponentService.Iface componentClient = thriftClients.makeComponentClient();
    VendorService.Iface vendorClient = thriftClients.makeVendorClient();

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    private Sw360ReleaseService releaseService;

    private static final ImmutableMap<String, String> RESPONSE_BODY_FOR_MODERATION_REQUEST = ImmutableMap.<String, String>builder()
            .put("message", "Moderation request is created").build();

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(FossologyAdminController.class).slash("api" + BULK_RELEASE_EDIT_URL).withRel("bulkReleaseEdit"));
        return resource;
    }

    @RequestMapping(value = BULK_RELEASE_EDIT_URL , method =RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<Release>>> getBulkReleaseEditList(HttpServletRequest request, Pageable pageable)
            throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        PaginationResult<Release> paginationResult = restControllerHelper.createPaginationResult(request, pageable,
                componentClient.getReleaseSummary(sw360User), SW360Constants.TYPE_RELEASE);
        final List<EntityModel<Release>> releaseResources = paginationResult.getResources().stream()
                .map(sw360Release -> wrapTException(() -> {
                    final Release embeddedRelease = restControllerHelper.convertToEmbeddedLinkedProjectsReleases(sw360Release);
                    embeddedRelease.setCpeid(sw360Release.getCpeid());
                    final HalResource<Release> releaseResource = new HalResource<>(embeddedRelease);
                    return releaseResource;
                })).collect(Collectors.toList());
        CollectionModel resources = restControllerHelper.generatePagesResource(paginationResult, releaseResources);;
        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    @RequestMapping(value = BULK_RELEASE_EDIT_URL+"/searchVendor" , method =RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<Vendor>>> getVendorList(
            @Parameter(description = "Search text") String searchText,
            HttpServletRequest request, Pageable pageable)
            throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        PaginationResult<Vendor> paginationResult = null;
        if (isNullOrEmpty(searchText)){
            paginationResult = restControllerHelper.createPaginationResult(request, pageable,
                    vendorClient.getAllVendors(), SW360Constants.TYPE_VENDOR);
        } else {
            paginationResult = restControllerHelper.createPaginationResult(request, pageable,
                    vendorClient.searchVendors(searchText), SW360Constants.TYPE_VENDOR);
        }
        final List<EntityModel<Vendor>> vendorResources = paginationResult.getResources().stream()
                .map(sw360Vendor -> wrapTException(() -> {
                    final HalResource<Vendor> vendorResource = new HalResource<>(sw360Vendor);
                    return vendorResource;
                })).collect(Collectors.toList());
        CollectionModel resources = restControllerHelper.generatePagesResource(paginationResult, vendorResources);;
        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Update a release.",
            description = "Update an existing release.",
            tags = {"Releases"}
    )
    @PatchMapping(value = BULK_RELEASE_EDIT_URL + "/{id}")
    public ResponseEntity<EntityModel<Release>> patchRelease(
            @Parameter(description = "The ID of the release to be updated.")
            @PathVariable("id") String id,
            @Parameter(description = "The release object to be updated.",
                    schema = @Schema(implementation = Release.class))
            @RequestBody Map<String, Object> reqBodyMap
    ) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        Release sw360Release = releaseService.getReleaseForUserById(id, user);
        Release updateRelease = releaseService.setBackwardCompatibleFieldsInRelease(reqBodyMap);
        updateRelease.setClearingState(sw360Release.getClearingState());
        sw360Release = this.restControllerHelper.updateRelease(sw360Release, updateRelease);
        releaseService.setComponentNameAsReleaseName(sw360Release, user);
        RequestStatus updateReleaseStatus = releaseService.updateRelease(sw360Release, user);
        sw360Release = releaseService.getReleaseForUserById(id, user);
        HalResource<Release> halRelease = releaseService.createHalReleaseResource(sw360Release, true);
        if (updateReleaseStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>(halRelease, HttpStatus.OK);
    }
}
