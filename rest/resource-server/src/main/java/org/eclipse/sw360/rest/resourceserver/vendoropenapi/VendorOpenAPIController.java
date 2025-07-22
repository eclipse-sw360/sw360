package org.eclipse.sw360.rest.resourceserver.vendoropenapi;

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;
import org.eclipse.sw360.datahandler.postgres.VendorPG;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@ConditionalOnProperty(name = "feature.experimental.enabled", havingValue = "true")
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
@Validated
public class VendorOpenAPIController
                implements RepresentationModelProcessor<RepositoryLinksResource> {

        @NonNull
        private final RestControllerHelper restControllerHelper;

        public static final String VENDORS_URL = "/vendorsOpenAPI";

        private final VendorServicePG vendorService = new VendorServicePG();

        @Override
        public RepositoryLinksResource process(RepositoryLinksResource resource) {
                resource.add(linkTo(VendorOpenAPIController.class).slash("api/vendorsOpenAPI")
                                .withRel("vendorsOpenAPI"));
                return resource;
        }

        @Operation(summary = "Create a new vendor.", description = "Create a new vendor.",
                        tags = {"Vendor"})
        @PreAuthorize("hasAuthority('WRITE')")
        @RequestMapping(value = VENDORS_URL, method = RequestMethod.POST)
        public ResponseEntity<?> createVendor(@Parameter(
                        description = "The vendor to be created.") @RequestBody org.eclipse.sw360.datahandler.componentsApi.model.VendorAPI vendor) {
                User user = restControllerHelper.getSw360UserFromAuthentication();
                VendorPG internalVendor = new VendorPG(vendor);

                VendorPG result = vendorService.createVendor(internalVendor, user);
                return ResponseEntity.ok(new HalResource<>(result));
        }

        @PreAuthorize("hasAuthority('WRITE')")
        @RequestMapping(value = VENDORS_URL + "/{id}", method = RequestMethod.PATCH)
        public ResponseEntity<?> updateVendor(@Parameter(
                        description = "The ID of the vendor to be updated.") @PathVariable("id") String id,
                        @RequestBody org.eclipse.sw360.datahandler.componentsApi.model.VendorAPI vendor) {
                User user = restControllerHelper.getSw360UserFromAuthentication();
                VendorPG internalVendor = new VendorPG(vendor);
                internalVendor.setId(UUID.fromString(id));
                VendorPG result = vendorService.updateVendor(internalVendor, user);
                return ResponseEntity.ok(new HalResource<>(result));
        }

        @Operation(summary = "Delete a vendor.", description = "Delete vendor by id.",
                        tags = {"Vendor"})
        @RequestMapping(value = VENDORS_URL + "/{id}", method = RequestMethod.DELETE)
        public ResponseEntity<?> deleteVendor(@Parameter(
                        description = "The id of the vendor to be deleted.") @PathVariable("id") String id) {
                User user = restControllerHelper.getSw360UserFromAuthentication();
                vendorService.deleteVendor(UUID.fromString(id), user);
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "List all of the service's vendors.",
                        description = "List all of the service's vendors.", tags = {"Vendor"})
        @RequestMapping(value = VENDORS_URL, method = RequestMethod.GET)
        public ResponseEntity<List<VendorPG>> getVendors(
                        @Parameter(description = "Search text") @RequestParam(value = "searchText",
                                        required = false) String searchText,
                        Pageable pageable, HttpServletRequest request) throws URISyntaxException,
                        PaginationParameterException, ResourceClassNotFoundException {
                List<VendorPG> vendors = null;

                vendors = vendorService.getVendors();

                return new ResponseEntity<>(vendors, HttpStatus.OK);
        }

        @Operation(summary = "Get a single vendor.", description = "Get a single vendor by id.",
                        tags = {"Vendor"})
        @RequestMapping(value = VENDORS_URL + "/{id}", method = RequestMethod.GET)
        public ResponseEntity<EntityModel<VendorPG>> getVendor(@Parameter(
                        description = "The id of the vendor to get.") @PathVariable("id") String id) {
                VendorPG sw360Vendor = vendorService.getVendorById(id);
                HalResource<VendorPG> halResource = createHalVendor(sw360Vendor);
                return new ResponseEntity<>(halResource, HttpStatus.OK);
        }

        private HalResource<VendorPG> createHalVendor(VendorPG sw360Vendor) {
                return new HalResource<>(sw360Vendor);
        }
}
