/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Obligation.
 *
  * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.obligation;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.MultiStatus;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ObligationController implements ResourceProcessor<RepositoryLinksResource> {
    public static final String OBLIGATION_URL = "/obligations";

    @NonNull
    private final Sw360ObligationService obligationService;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @RequestMapping(value = OBLIGATION_URL, method = RequestMethod.GET)
    public ResponseEntity<Resources<Resource<Obligation>>> getObligations() {
        List<Obligation> obligations = obligationService.getObligations();

        List<Resource<Obligation>> obligationResources = new ArrayList<>();
        obligations.forEach(o -> {
            Obligation embeddedObligation = restControllerHelper.convertToEmbeddedObligation(o);
            obligationResources.add(new Resource<>(embeddedObligation));
        });

        Resources<Resource<Obligation>> resources = new Resources<>(obligationResources);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @RequestMapping(value = OBLIGATION_URL + "/{id}", method = RequestMethod.GET)
    public ResponseEntity<Resource<Obligation>> getObligation(
            @PathVariable("id") String id) {
        try {
            Obligation sw360Obligation = obligationService.getObligationById(id);
            HalResource<Obligation> halResource = createHalObligation(sw360Obligation);
            return new ResponseEntity<>(halResource, HttpStatus.OK);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Obligation does not exists! id=" + id);
        }
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = OBLIGATION_URL, method = RequestMethod.POST)
    public ResponseEntity createObligation(
            @RequestBody Obligation obligation) {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        obligation = obligationService.createObligation(obligation, sw360User);
        HalResource<Obligation> halResource = createHalObligation(obligation);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(obligation.getId()).toUri();

        return ResponseEntity.created(location).body(halResource);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = OBLIGATION_URL + "/{ids}", method = RequestMethod.DELETE)
    public ResponseEntity<List<MultiStatus>> deleteObligations(
            @PathVariable("ids") List<String> idsToDelete) throws TException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        List<MultiStatus> results = new ArrayList<>();
        for(String id : idsToDelete) {
            try {
                Obligation obligation = obligationService.getObligationById(id);
                RequestStatus requestStatus = obligationService.deleteObligation(obligation.getId(), user);
                if(requestStatus == RequestStatus.SUCCESS) {
                    results.add(new MultiStatus(id, HttpStatus.OK));
                } else {
                    results.add(new MultiStatus(id, HttpStatus.INTERNAL_SERVER_ERROR));
                }
            } catch (Exception e) {
                results.add(new MultiStatus(id, HttpStatus.BAD_REQUEST));
            }
        }
        return new ResponseEntity<>(results, HttpStatus.MULTI_STATUS);
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ObligationController.class).slash("api" + OBLIGATION_URL).withRel("obligations"));
        return resource;
    }

    private HalResource<Obligation> createHalObligation(Obligation sw360Obligation) {
        return new HalResource<>(sw360Obligation);
    }
}
