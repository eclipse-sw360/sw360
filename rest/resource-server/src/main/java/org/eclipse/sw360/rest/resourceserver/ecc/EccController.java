/*
SPDX-FileCopyrightText: © 2023-2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/

package org.eclipse.sw360.rest.resourceserver.ecc;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
public class EccController implements RepresentationModelProcessor<RepositoryLinksResource> {
    
    private static final String TYPE_ECC = "ecc";

    public static final String ECC_URL = "/ecc";
    
    @NonNull
    private final RestControllerHelper restControllerHelper;
    
    @NonNull
    private final Sw360ReleaseService releaseService;

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(EccController.class).slash("api/ecc").withRel(TYPE_ECC));
        return resource;
    }
    
    @GetMapping(value = ECC_URL)
    public ResponseEntity<CollectionModel<?>> getEccDetails(HttpServletRequest request, Pageable pageable)
            throws TException, URISyntaxException {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        List<Release> releases = new ArrayList<>();
        try {
            releases = releaseService.getReleasesForUser(user);
            PaginationResult<Release> paginationResult = restControllerHelper.createPaginationResult(request, pageable,
                    releases, TYPE_ECC);
            final List<EntityModel<Release>> releaseResources = new ArrayList<>();
            for (Release rel : paginationResult.getResources()) {
                Release embeddedRelease = restControllerHelper.convertToEmbeddedRelease(rel);
                embeddedRelease.setEccInformation(rel.getEccInformation());
                final EntityModel<Release> releaseResource = EntityModel.of(embeddedRelease);
                releaseResources.add(releaseResource);
            }
            CollectionModel<EntityModel<Release>> resources;
            if (releases.isEmpty()) {
                resources = restControllerHelper.emptyPageResource(Release.class, paginationResult);
            } else {
                resources = restControllerHelper.generatePagesResource(paginationResult, releaseResources);
            }
            HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
            return new ResponseEntity<>(resources, status);
        } catch (Exception e) {
            throw new TException(e.getMessage());
        }
    }

}
