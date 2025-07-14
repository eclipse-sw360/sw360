package org.eclipse.sw360.rest.resourceserver.licenseopenapi;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.postgres.LicensePG;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@ConditionalOnProperty(name = "feature.experimental.enabled", havingValue = "true")
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
@Validated
public class LicenseOpenAPIController
                implements RepresentationModelProcessor<RepositoryLinksResource> {

        public static final String LICENSES_URL = "api/licensesOpenAPI";

        @Override
        public RepositoryLinksResource process(RepositoryLinksResource resource) {
                resource.add(linkTo(LicenseOpenAPIController.class).slash(LICENSES_URL)
                                .withRel("licensesOpenAPI"));
                return resource;
        }

        private final LicenseServicePG licenseServicePG = new LicenseServicePG();

        @Operation(summary = "Create a new license.", description = "Create a new license.",
                        tags = {"Licenses"})
        @PreAuthorize("hasAuthority('WRITE')")
        @PostMapping(value = LICENSES_URL)
        public ResponseEntity<EntityModel<LicensePG>> createLicense(@Parameter(
                        description = "The license to be created.") @RequestBody LicensePG license)
                        throws TException {
                LicensePG createdLicense = licenseServicePG.createLicense(license);
                EntityModel<LicensePG> entityModel = EntityModel.of(createdLicense);

                return ResponseEntity
                                .created(linkTo(LicenseOpenAPIController.class).slash(LICENSES_URL)
                                                .toUri())
                                .contentType(MediaTypes.HAL_JSON).body(entityModel);
        }
}
