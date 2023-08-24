/*
 * Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.search;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
public class SearchController implements RepresentationModelProcessor<RepositoryLinksResource> {

    private static final Logger log = LogManager.getLogger(SearchController.class);

    public static final String SEARCH_URL = "/search";

    @Autowired
    private Sw360SearchService sw360SearchService;

    @NonNull
    private final RestControllerHelper restControllerHelper;


    @Operation(
            summary = "List all the search results based on the search text and type.",
            description = "List all the search results based on the search text and type.",
            tags = {"Search"}
    )
    @RequestMapping(value = SEARCH_URL, method = RequestMethod.GET)
    public ResponseEntity<CollectionModel<EntityModel<SearchResult>>> getSearchResult(
            Pageable pageable,
            @Parameter(description = "The search text.")
            @RequestParam(value = "searchText") String searchText,
            @Parameter(
                    description = "The type of resource.",
                    schema = @Schema(
                            allowableValues = {"project", "component", "license", "release", "obligation", "user",
                                    "vendor"},
                            type = "array"
                    )
            )
            @RequestParam Optional<List<String>> typeMasks,
            HttpServletRequest request
    ) throws TException, URISyntaxException, PaginationParameterException, ResourceClassNotFoundException {
        log.debug("SearchText = {} typeMasks = {}", searchText, typeMasks);
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<SearchResult> searchResults = sw360SearchService.search(searchText, sw360User, typeMasks);

        PaginationResult<SearchResult> paginationResult = restControllerHelper.createPaginationResult(request, pageable,
                searchResults, SW360Constants.TYPE_SEARCHRESULT);

        List<EntityModel<SearchResult>> searchResources = paginationResult.getResources().stream()
                .map(sr -> EntityModel.of(sr)).collect(Collectors.toList());

        CollectionModel resources = null;
        if (CommonUtils.isNotEmpty(searchResources)) {
            resources = restControllerHelper.generatePagesResource(paginationResult, searchResources);
        }

        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(resources, status);
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(SearchController.class).slash("api" + SEARCH_URL).withRel("searchs"));
        return resource;
    }

}
