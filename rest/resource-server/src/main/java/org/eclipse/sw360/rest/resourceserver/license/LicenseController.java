/*
 * Copyright Siemens AG, 2017-2018.
 * Copyright Bosch Software Innovations GmbH, 2017.
 * Copyright Ritankar Saha <ritankar.saha786@gmail.com>, 2025.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.license;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseType;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.OpenAPIPaginationHelper;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus.Series;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
public class LicenseController implements RepresentationModelProcessor<RepositoryLinksResource> {
    public static final String LICENSES_URL = "/licenses";
    public static final String LICENSE_TYPES_URL = "/licenseTypes";

    @NonNull
    private final Sw360LicenseService licenseService;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    private final LicenseDbIntegrationService licenseDbIntegrationService;

    private static final ImmutableMap<String, String> RESPONSE_BODY_FOR_MODERATION_REQUEST = ImmutableMap.<String, String>builder()
            .put("message", "Moderation request is created").build();

    @Operation(
            summary = "List all of the service's licenses.",
            description = "List all of the service's licenses. Supports quick filtering.",
            tags = {"Licenses"}
    )
    @GetMapping(value = LICENSES_URL)
    public ResponseEntity<CollectionModel<License>> getLicenses(
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable,
            @Parameter(description = "Search text to filter licenses.")
            @RequestParam(value = "searchText", required = false) String searchText,
            HttpServletRequest request
    ) throws TException, ResourceClassNotFoundException, PaginationParameterException, URISyntaxException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        List<License> sw360Licenses;

        if (CommonUtils.isNotNullEmptyOrWhitespace(searchText)) {
            sw360Licenses = licenseService.searchLicenses(searchText);
        } else {
            sw360Licenses = licenseService.getLicenses();
        }

        PaginationResult<License> paginationResult = restControllerHelper.createPaginationResult(request, pageable, sw360Licenses, SW360Constants.TYPE_LICENSE);
        List<EntityModel<License>> licenseResources = new ArrayList<>();
        paginationResult.getResources()
                .forEach(license -> {
                    License embeddedLicense = restControllerHelper.convertToEmbeddedLicense(license);
                    EntityModel<License> licenseResource = EntityModel.of(embeddedLicense);
                    licenseResources.add(licenseResource);
                });
        CollectionModel<License> resources;
        if (licenseResources.isEmpty()) {
            resources = restControllerHelper.emptyPageResource(License.class, paginationResult);
        } else {
            resources = restControllerHelper.generatePagesResource(paginationResult, licenseResources);
        }
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Operation(
            summary = "List obligations of license.",
            description = "List all obligations of a license.",
            tags = {"Licenses"}
    )
    @GetMapping(value = LICENSES_URL + "/{id}/obligations")
    public ResponseEntity<CollectionModel<EntityModel<Obligation>>> getObligationsByLicenseId(
            @PathVariable("id") String id
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        List<Obligation> obligations = licenseService.getObligationsByLicenseId(id);
        List<EntityModel<Obligation>> obligationResources = new ArrayList<>();
        obligations.forEach(o -> {
            Obligation embeddedObligation = restControllerHelper.convertToEmbeddedObligation(o);
            obligationResources.add(EntityModel.of(embeddedObligation));
        });
        CollectionModel<EntityModel<Obligation>> resources = CollectionModel.of(obligationResources);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Operation(
            summary = "List all of the service's licenseTypes.",
            description = "List all of the service's licenseTypes.",
            tags = {"Licenses"}
    )
    @GetMapping(value = LICENSE_TYPES_URL)
    public ResponseEntity<CollectionModel<EntityModel<LicenseType>>> getLicenseTypes(
            @Parameter(description = "The search license type text.")
            @RequestParam(value = "search", required = false) String searchElem) throws TException {
        List<LicenseType> sw360LicenseTypes;

        if (searchElem != null && !searchElem.isEmpty()) {
            sw360LicenseTypes = licenseService.quickSearchLicenseType(searchElem);
        } else {
            sw360LicenseTypes = licenseService.getLicenseTypes();
        }

        List<EntityModel<LicenseType>> licenseTypeResources = new ArrayList<>();
        for (LicenseType sw360LicenseType : sw360LicenseTypes) {
            LicenseType embeddedLicenseType = restControllerHelper.convertToEmbeddedLicenseType(sw360LicenseType);
            EntityModel<LicenseType> licenseResource = EntityModel.of(embeddedLicenseType);
            licenseTypeResources.add(licenseResource);
        }
        CollectionModel<EntityModel<LicenseType>> resources = CollectionModel.of(licenseTypeResources);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Operation(
            summary = "Get a specific license.",
            description = "Get a specific license.",
            tags = {"Licenses"}
    )
    @GetMapping(value = LICENSES_URL + "/{id:.+}")
    public ResponseEntity<EntityModel<License>> getLicense(
            @Parameter(description = "The id of the license.")
            @PathVariable("id") String id
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        License sw360License = licenseService.getLicenseById(id);
        HalResource<License> licenseHalResource = createHalLicense(sw360License);
        return new ResponseEntity<>(licenseHalResource, HttpStatus.OK);
    }

    @Operation(
            summary = "Delete a specific license.",
            description = "Delete a specific license.",
            tags = {"Licenses"}
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @DeleteMapping(value = LICENSES_URL + "/{id:.+}")
    public ResponseEntity deleteLicense(
            @Parameter(description = "The id of the license.")
            @PathVariable("id") String id
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        licenseService.deleteLicenseById(id, sw360User);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(
            summary = "Create a new license.",
            description = "Create a new license.",
            tags = {"Licenses"}
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @PostMapping(value = LICENSES_URL)
    public ResponseEntity<EntityModel<License>> createLicense(
            @Parameter(description = "The license to be created.")
            @RequestBody License license
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<License> sw360Licenses = licenseService.getLicenses();
        if(restControllerHelper.checkDuplicateLicense(sw360Licenses, license.shortname)) {
            return new ResponseEntity("sw360 license with name " + license.shortname + " already exists.", HttpStatus.CONFLICT);
        }
        license = licenseService.createLicense(license, sw360User);
        HalResource<License> halResource = createHalLicense(license);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(license.getId()).toUri();

        return ResponseEntity.created(location).body(halResource);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Update a license.",
            description = "Update a service's license.",
            tags = {"Licenses"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "License updated successfully."),
            @ApiResponse(
                    responseCode = "202", description = "Request sent for moderation.",
                    content = {
                            @Content(mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = "{\"message\": \"Moderation request is created\"}"
                                    ))
                    }
            ),
            @ApiResponse(responseCode = "405",
                    description = "Reject license update due to: an already checked license is not allowed" +
                            " to become unchecked again")
    })
    @PatchMapping(value = LICENSES_URL + "/{id}")
    public ResponseEntity<EntityModel<License>> updateLicense(
            @Parameter(description = "The id of the license.")
            @PathVariable("id") String id,
            @Parameter(description = "Updated license body.", schema = @Schema(implementation = License.class))
            @RequestBody Map<String, Object> reqBodyMap
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        License licenseUpdate = licenseService.getLicenseById(id);
        License licenseRequestBody = restControllerHelper.convertLicenseFromRequest(reqBodyMap, licenseUpdate);
        if (licenseUpdate.isChecked() && !licenseRequestBody.isChecked()) {
            return new ResponseEntity("Reject license update due to: an already checked license is not allowed to" +
                    " become unchecked again", HttpStatus.METHOD_NOT_ALLOWED);
        }
        licenseUpdate = restControllerHelper.mapLicenseRequestToLicense(licenseRequestBody, licenseUpdate);
        RequestStatus requestStatus = licenseService.updateLicense(licenseUpdate, sw360User);
        if (requestStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        }
        HalResource<License> halResource = createHalLicense(licenseUpdate);
        return new ResponseEntity<>(halResource, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Update whitelist for license's obligations.",
            description = "Update whitelist for license's obligations.",
            tags = {"Licenses"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "License updated successfully."),
            @ApiResponse(
                    responseCode = "202", description = "Request sent for moderation.",
                    content = {
                            @Content(mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = "{\"message\": \"Moderation request is created\"}"
                                    ))
                    }
            ),
            @ApiResponse(responseCode = "500", description = "Update Whitelist to Obligation Fail!")
    })
    @PatchMapping(value = LICENSES_URL+ "/{id}/whitelist")
    public ResponseEntity<EntityModel<License>> updateWhitelist(
            @Parameter(description = "ID of the license.")
            @PathVariable("id") String licenseId,
            @Parameter(description = "Obligations to whitelist as key and true/false as value",
                    schema = @Schema(example = """
                            {
                              "ob001": true,
                              "ob002": true
                            }""")
            )
            @RequestBody Map<String, Boolean> reqBodyMaps
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        License license = licenseService.getLicenseById(licenseId);
        Set<String> obligationIdsByLicense = new HashSet<>();
        if (!CommonUtils.isNullOrEmptyCollection(license.getObligationDatabaseIds())) {
            obligationIdsByLicense = license.getObligationDatabaseIds();
        }
        Map<String, Boolean> obligationIdsRequest = reqBodyMaps.entrySet().stream()
                .collect(Collectors.toMap(reqBodyMap-> reqBodyMap.getKey(), reqBodyMap -> reqBodyMap.getValue()));
        Set<String> obligationIds = obligationIdsRequest.keySet();

        Set<String> commonExtIds = Sets.intersection(obligationIdsByLicense, obligationIds);
        Set<String> diffIds = Sets.difference(obligationIdsByLicense, obligationIds);
        if (commonExtIds.size() != obligationIds.size()) {
            throw new BadRequestClientException("Obligation Ids not in license!" + license.getShortname());
        }

        Set<String> obligationIdTrue = licenseService.getIdObligationsContainWhitelist(sw360User, licenseId, diffIds);
        obligationIdTrue.addAll(restControllerHelper.getObligationIdsFromRequestWithValueTrue(reqBodyMaps));

        RequestStatus requestStatus = licenseService.updateWhitelist(obligationIdTrue, licenseId, sw360User);
        HalResource<License> halResource;
        if (requestStatus == RequestStatus.SENT_TO_MODERATOR) {
            return new ResponseEntity(RESPONSE_BODY_FOR_MODERATION_REQUEST, HttpStatus.ACCEPTED);
        } else if (requestStatus == RequestStatus.SUCCESS) {
            License licenseUpdate = licenseService.getLicenseById(licenseId);
            halResource = createHalLicense(licenseUpdate);
            return new ResponseEntity<>(halResource, HttpStatus.OK);
        } else {
            throw new SW360Exception("Update Whitelist to Obligation Fail!");
        }
    }

    @Operation(
            summary = "Link obligations to a license.",
            description = "Link a set of obligations to a license.",
            tags = {"Licenses"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Obligations linked to license."),
            @ApiResponse(
                    responseCode = "400", description = "Obligation ids which failed to linked with license.",
                    content = {
                            @Content(mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = "{\"message\": \"Obligation ids: ob001 are not linked to license\"}"
                                    ))
                    }
            )
    })
    @PreAuthorize("hasAuthority('WRITE')")
    @PostMapping(value = LICENSES_URL + "/{id}/obligations")
    public ResponseEntity linkObligation(
            @Parameter(description = "The id of the license.")
            @PathVariable("id") String id,
            @Parameter(description = "The ids of the obligations to be linked.",
                    example = "[\"ob001\",\"ob002\"]")
            @RequestBody Set<String> obligationIds
    ) throws TException {
        updateLicenseObligations(obligationIds, id, false);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Operation(
            summary = "Unlink obligations from a license.",
            description = "Unlink a set of obligations from a license.",
            tags = {"Licenses"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Obligations unlinked from license."),
            @ApiResponse(
                    responseCode = "400", description = "Obligation ids which failed to unlinked from license.",
                    content = {
                            @Content(mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = "{\"message\": \"Obligation ids: ob001 are not linked to license\"}"
                                    ))
                    }
            )
    })
    @PreAuthorize("hasAuthority('WRITE')")
    @PatchMapping(value = LICENSES_URL + "/{id}/obligations")
    public ResponseEntity unlinkObligation(
            @Parameter(description = "The id of the license.")
            @PathVariable("id") String id,
            @Parameter(description = "The ids of the obligations to be unlinked.")
            @RequestBody Set<String> obligationIds
    ) throws TException {
        updateLicenseObligations(obligationIds, id, true);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private void updateLicenseObligations(Set<String> obligationIds, String licenseId, boolean unLink) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        License license = licenseService.getLicenseById(licenseId);
        licenseService.checkObligationIds(obligationIds);
        Set<String> obligationIdsLink = obligationIds;
        if (unLink) {
            Set<String> licenseObligationIds = license.getObligationDatabaseIds();
            List<String> obligationIdsIncorrect = new ArrayList<>();
            for (String obligationId : obligationIds) {
                if (!licenseObligationIds.contains(obligationId)) {
                    obligationIdsIncorrect.add(obligationId);
                }
            }
            if (!obligationIdsIncorrect.isEmpty()) {
                throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,
                        "Obligation ids: " + obligationIdsIncorrect + " are not linked to license");
            }
            licenseObligationIds.removeAll(obligationIds);
            obligationIdsLink = licenseObligationIds;
        }
        licenseService.updateLicenseToDB(license, obligationIdsLink, sw360User);
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(LicenseController.class).slash("api/licenses").withRel("licenses"));
        return resource;
    }

    private HalResource<License> createHalLicense(License sw360License) {
        HalResource<License> halLicense = new HalResource<>(sw360License);
        if (sw360License.getObligations() != null) {
            List<Obligation> obligations = sw360License.getObligations();
            restControllerHelper.addEmbeddedObligations(halLicense, obligations);
        }
        return halLicense;
    }

    @Operation(
            summary = "Delete all licenses.",
            description = "Delete all licenses of the service.",
            tags = {"Licenses"}
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @DeleteMapping(value = LICENSES_URL + "/deleteAll")
    public ResponseEntity deleteAllLicense() throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        licenseService.deleteAllLicenseInfo(sw360User);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(
            summary = "Import SPDX information.",
            description = "Import SPDX information. DEPRECATED: Use LicenseDB integration instead.",
            tags = {"Licenses"},
            deprecated = true
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @PostMapping(value = LICENSES_URL + "/import/SPDX")
    public ResponseEntity<RequestSummary> importSPDX() throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestSummary requestSummary = licenseService.importSpdxInformation(sw360User);
        requestSummary.setMessage("SPDX license has imported successfully. DEPRECATED: Please use /import/LicenseDB instead.");
        HttpStatus status = HttpStatus.OK;
        return new ResponseEntity<>(requestSummary, status);
    }

    @Operation(
            summary = "Download license archive.",
            description = "Download license archive.",
            tags = {"Licenses"},
            parameters = {
                    @Parameter(name = "Accept", in = ParameterIn.HEADER, required = true, example = "application/zip"),
            }
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @GetMapping(value = LICENSES_URL + "/downloadLicenses", produces = "application/zip")
    public void downloadLicenseArchive(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws TException, IOException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        licenseService.getDownloadLicenseArchive(sw360User,request,response);

    }

    @Operation(
            summary = "Upload license archive.",
            description = "Upload license archive.",
            tags = {"Licenses"}
    )
    @PostMapping(value = LICENSES_URL + "/upload", consumes = {MediaType.MULTIPART_MIXED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> uploadLicenses(
            @Parameter(description = "The license archive file to be uploaded.")
            @RequestParam("licenseFile") MultipartFile file,
            @Parameter(description = "Overwrite if external id matches.")
            @RequestParam(value = "overwriteIfExternalIdMatches", required = false) boolean overwriteIfExternalIdMatches,
            @Parameter(description = "Overwrite if id matches even without external id match.")
            @RequestParam(value = "overwriteIfIdMatchesEvenWithoutExternalIdMatch", required = false) boolean overwriteIfIdMatchesEvenWithoutExternalIdMatch
    ) throws SW360Exception {
        try {
            User sw360User = restControllerHelper.getSw360UserFromAuthentication();
            licenseService.uploadLicense(sw360User, file, overwriteIfExternalIdMatches,
                    overwriteIfIdMatchesEvenWithoutExternalIdMatch);
        } catch (Exception e) {
            throw new SW360Exception(e.getMessage());
	    }
       return ResponseEntity.ok(Series.SUCCESSFUL);
     }

    @Operation(
            summary = "Import OSADL information.",
            description = "Import OSADL information. DEPRECATED: Use LicenseDB integration instead.",
            tags = {"Licenses"},
            deprecated = true
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @PostMapping(value = LICENSES_URL + "/import/OSADL")
    public ResponseEntity<RequestSummary> importOsadlInfo() throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestSummary requestSummary = licenseService.importOsadlInformation(sw360User);
        requestSummary.setMessage(requestSummary.getRequestStatus() == RequestStatus.SUCCESS ? 
            "OSADL information imported successfully. DEPRECATED: Please use /import/LicenseDB instead." : 
            "Failed to import OSADL information");
        HttpStatus status = requestSummary.getRequestStatus() == RequestStatus.SUCCESS ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
        return new ResponseEntity<>(requestSummary, status);
    }

    @Operation(
            summary = "Import LicenseDB information.",
            description = "Import licenses and obligations from LicenseDB.",
            tags = {"Licenses"}
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = LICENSES_URL + "/import/LicenseDB", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> importLicenseDb() throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        
        Map<String, Object> result = licenseDbIntegrationService.fullSync();
        result.put("user", sw360User.getEmail());
        
        HttpStatus status = HttpStatus.OK;
        if ("FAILED".equals(result.get("status"))) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new ResponseEntity<>(result, status);
    }

    @Operation(
            summary = "Get LicenseDB sync status.",
            description = "Get the current sync status from LicenseDB.",
            tags = {"Licenses"}
    )
    @PreAuthorize("hasAuthority('READ')")
    @RequestMapping(value = LICENSES_URL + "/sync/LicenseDB/status", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getLicenseDbSyncStatus() {
        Map<String, Object> result = licenseDbIntegrationService.getSyncStatus();
        
        HttpStatus status = HttpStatus.OK;
        return new ResponseEntity<>(result, status);
    }

    @Operation(
            summary = "Test LicenseDB connection.",
            description = "Test the connection to LicenseDB server.",
            tags = {"Licenses"}
    )
    @PreAuthorize("hasAuthority('ADMIN')")
    @RequestMapping(value = LICENSES_URL + "/sync/LicenseDB/test", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> testLicenseDbConnection() {
        Map<String, Object> result = licenseDbIntegrationService.testConnection();
        
        HttpStatus status = HttpStatus.OK;
        if (!Boolean.TRUE.equals(result.get("connected"))) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
        }
        return new ResponseEntity<>(result, status);
    }

    @Operation(
            summary = "Create license type.",
            description = "Create license type.",
            tags = {"Licenses"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "License Type created successfully."),
            @ApiResponse(
                    responseCode = "400", description = "Bad request if license type is empty or user is not admin.",
                    content = {
                            @Content(mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = "{\"message\": \"Unable to create License Type. User is not admin\"}"
                                    ))
                    }
            )
    })
    @PostMapping(value = LICENSES_URL + "/addLicenseType")
    public ResponseEntity<RequestStatus> createLicenseType(
            @Parameter(description = "The license type name.")
            @RequestParam(value = "licenseType", required = true) String licenseType,
            HttpServletRequest request
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestStatus requestStatus = licenseService.addLicenseType(sw360User, licenseType, request);
        HttpStatus status = HttpStatus.OK;
        return new ResponseEntity<>(requestStatus, status);
    }

    @Operation(
            summary = "Delete a specific license type.",
            description = "Delete a specific license type.",
            tags = {"Licenses"},
            responses = {
                    @ApiResponse(
                            responseCode = "200", description = "License type deleted successfully.",
                            content = {
                                    @Content(mediaType = "application/json",
                                            schema = @Schema(
                                                    example = """
                                                    {
                                                        "status": "SECCESS",
                                                        "message": "License type deleted successfully."
                                                    }
                                                    """
                                            ))
                            }
                    ),
                    @ApiResponse(
                            responseCode = "403", description = "User does not have permission to delete license type."
                    ),
                    @ApiResponse(
                            responseCode = "404", description = "License type with the given ID was not found."
                    ),
                    @ApiResponse(
                            responseCode = "409", description = "Cannot delete license type because it is currently in use."
                    ),
                    @ApiResponse(
                            responseCode = "500", description = "Unexpected error occurred while deleting license type."
                    )
            }
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @DeleteMapping(value = LICENSE_TYPES_URL + "/{id}")
    public ResponseEntity deleteLicenseType(
            @Parameter(description = "The id of the license type.")
            @PathVariable("id") String id
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestStatus status = licenseService.deleteLicenseType(id, sw360User);

        switch (status) {
            case SUCCESS:
                Map<String, String> successResponse = new HashMap<>();
                successResponse.put("status", status.name());
                successResponse.put("message", "License type deleted successfully.");
                return ResponseEntity.ok(successResponse);

            case IN_USE:
                throw new HttpClientErrorException(HttpStatus.CONFLICT,
                        "Cannot delete license type because it is currently in use.");

            case ACCESS_DENIED:
                throw new AccessDeniedException("User does not have permission to delete license type.");

            case INVALID_INPUT:
                throw new ResourceNotFoundException("License type with the given ID was not found.");

            default:
                throw new RuntimeException("Unexpected error occurred while deleting license type.");
        }
    }

    @Operation(
            summary = "Check if a license type is being used and get the count.",
            description = "Returns whether the license type is being used and the total count of such licenses.",
            tags = {"License Types"},
            responses = {
                    @ApiResponse(
                            responseCode = "200", description = "LicenseType usage information",
                            content = {
                                    @Content(mediaType = "application/json",
                                            schema = @Schema(
                                                    example = """
                                                            {
                                                              isUsed: true,
                                                              count: 5
                                                            }
                                                            """
                                            )
                                    )
                            }
                    ),
                    @ApiResponse(
                            responseCode = " 403", description = "User is not an admin"
                    )
            }
    )
    @GetMapping(value = LICENSE_TYPES_URL + "/{licenseTypeId}/usage", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getLicenseTypeUsage(
            @PathVariable("licenseTypeId") String licenseTypeId
    ) throws  TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        int count;
        count = licenseService.getLicenseTypeUsageCount(licenseTypeId, sw360User);
        boolean isUsed = count > 0;
        Map<String, Object> response = new HashMap<>();
        response.put("isUsed", isUsed);
        response.put("count", count);
        return ResponseEntity.ok(response);
    }
}
