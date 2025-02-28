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
package org.eclipse.sw360.rest.resourceserver.admin.releaseedit;

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
import org.springframework.web.bind.annotation.*;

import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

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
}
