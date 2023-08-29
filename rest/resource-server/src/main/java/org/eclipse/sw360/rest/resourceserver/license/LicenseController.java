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
package org.eclipse.sw360.rest.resourceserver.license;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LicenseController implements RepresentationModelProcessor<RepositoryLinksResource> {
    public static final String LICENSES_URL = "/licenses";

    @NonNull
    private final Sw360LicenseService licenseService;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @RequestMapping(value = LICENSES_URL, method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<License>>> getLicenses() throws TException {
        List<License> sw360Licenses = licenseService.getLicenses();

        List<EntityModel<License>> licenseResources = new ArrayList<>();
        for (License sw360License : sw360Licenses) {
            License embeddedLicense = restControllerHelper.convertToEmbeddedLicense(sw360License);
            EntityModel<License> licenseResource = EntityModel.of(embeddedLicense);
            licenseResources.add(licenseResource);
        }

        CollectionModel<EntityModel<License>> resources = CollectionModel.of(licenseResources);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @RequestMapping(value = LICENSES_URL + "/{id:.+}", method = RequestMethod.GET)
    public ResponseEntity<EntityModel<License>> getLicense(
            @PathVariable("id") String id) throws TException {
        License sw360License = licenseService.getLicenseById(id);
        HalResource<License> licenseHalResource = createHalLicense(sw360License);
        return new ResponseEntity<>(licenseHalResource, HttpStatus.OK);
    }
    
    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = LICENSES_URL + "/{id:.+}", method = RequestMethod.DELETE)
    public ResponseEntity deleteLicense(
            @PathVariable("id") String id) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        licenseService.deleteLicenseById(id, sw360User);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = LICENSES_URL, method = RequestMethod.POST)
    public ResponseEntity<EntityModel<License>> createLicense(
            @RequestBody License license) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        license = licenseService.createLicense(license, sw360User);
        HalResource<License> halResource = createHalLicense(license);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(license.getId()).toUri();

        return ResponseEntity.created(location).body(halResource);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = LICENSES_URL + "/{id}/obligations", method = RequestMethod.POST)
    public ResponseEntity linkObligation(
            @PathVariable("id") String id,
            @RequestBody Set<String> obligationIds) throws TException {
        updateLicenseObligations(obligationIds, id, false);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = LICENSES_URL + "/{id}/obligations", method = RequestMethod.PATCH)
    public ResponseEntity unlinkObligation(
            @PathVariable("id") String id,
            @RequestBody Set<String> obligationIds) throws TException {
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
                throw new HttpMessageNotReadableException("Obligation ids: " + obligationIdsIncorrect + " are not linked to license");
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

    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = LICENSES_URL + "/deleteAll", method = RequestMethod.DELETE)
    public ResponseEntity deleteAllLicense() throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        licenseService.deleteAllLicenseInfo(sw360User);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = LICENSES_URL + "/import/SPDX", method = RequestMethod.POST)
    public ResponseEntity importSPDX() throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        licenseService.importSpdxInformation(sw360User);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
