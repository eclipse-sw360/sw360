/*
 * Copyright Siemens AG, 2017-2018.
 * Copyright Bosch Software Innovations GmbH, 2019.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.rest.resourceserver.core;

import org.apache.thrift.TBase;
import org.apache.thrift.TFieldIdEnum;
import org.eclipse.sw360.datahandler.resourcelists.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class RestPaginationHelper {

    private static final String PAGINATION_KEY_FIRST = "first";
    private static final String PAGINATION_KEY_PREVIOUS = "previous";
    private static final String PAGINATION_KEY_NEXT = "next";
    private static final String PAGINATION_KEY_LAST = "last";
    private static final String PAGINATION_PARAM_PAGE = "page";
    public static final String PAGINATION_PARAM_PAGE_ENTRIES = "page_entries";

    private RestPaginationHelper() {
        // only static methods
    }

    public static <T extends TBase<?, ? extends TFieldIdEnum>> PaginationResult<T> createPaginationResult(HttpServletRequest request,
                                                                                                          Pageable pageable,
                                                                                                          List<T> resources,
                                                                                                          String resourceType)
            throws ResourceClassNotFoundException, PaginationParameterException {
        PaginationResult<T> paginationResult;
        if (requestContainsPaging(request)) {
            PaginationOptions<T> paginationOptions = paginationOptionsFromPageable(pageable, resourceType);
            paginationResult = ResourceListController.applyPagingToList(resources, paginationOptions);
        } else {
            paginationResult = new PaginationResult<>(resources);
        }
        return paginationResult;
    }

    private static <T extends TBase<?, ? extends TFieldIdEnum>> boolean requestContainsPaging(HttpServletRequest request) {
        return request.getParameterMap().containsKey(PAGINATION_PARAM_PAGE) || request.getParameterMap().containsKey(PAGINATION_PARAM_PAGE_ENTRIES);
    }

    public static <T extends TBase<?, ? extends TFieldIdEnum>> Resources<Resource<T>> generatePagesResource(PaginationResult paginationResult, List<Resource<T>> resources) throws URISyntaxException {
        if (paginationResult.isPagingActive()) {
            PagedResources.PageMetadata pageMetadata = createPageMetadata(paginationResult);
            List<Link> pagingLinks = RestPaginationHelper.getPaginationLinks(paginationResult, RestPaginationHelper.getAPIBaseUrl());
            return new PagedResources<>(resources, pageMetadata, pagingLinks);
        } else {
            return new Resources<>(resources);
        }
    }

    public static <T extends TBase<?, ? extends TFieldIdEnum>> Resources<Resource<T>> emptyPageResource(Class<T> tClass, PaginationResult<T> paginationResult) {
        if (paginationResult.isPagingActive()) {
            PagedResources.PageMetadata pageMetadata = createPageMetadata(paginationResult);
            // TODO: does this break empty responses again
            // EmbeddedWrappers embeddedWrappers = new EmbeddedWrappers(true);
            // EmbeddedWrapper embeddedWrapper = embeddedWrappers.emptyCollectionOf(tClass);
            // List<EmbeddedWrapper> list = Collections.singletonList(embeddedWrapper);
            // return new PagedResources<Resource<T>>(list, pageMetadata, new ArrayList<>());
            return new PagedResources<>(new ArrayList<>(), pageMetadata, new ArrayList<>());
        } else {
            return new Resources<>(new ArrayList<>());
        }
    }

    private static PagedResources.PageMetadata createPageMetadata(PaginationResult paginationResult) {
        PaginationOptions paginationOptions = paginationResult.getPaginationOptions();
        return new PagedResources.PageMetadata(
                paginationOptions.getPageSize(),
                paginationOptions.getPageNumber(),
                paginationResult.getTotalCount(),
                paginationResult.getTotalPageCount());
    }

    private static List<Link> getPaginationLinks(PaginationResult paginationResult, String baseUrl) {
        PaginationOptions paginationOptions = paginationResult.getPaginationOptions();
        List<Link> paginationLinks = new ArrayList<>();

        paginationLinks.add(new Link(createPaginationLink(baseUrl, 0, paginationOptions.getPageSize()),PAGINATION_KEY_FIRST));
        if(paginationOptions.getPageNumber() > 0) {
            paginationLinks.add(new Link(createPaginationLink(baseUrl, paginationOptions.getPageNumber() - 1, paginationOptions.getPageSize()),PAGINATION_KEY_PREVIOUS));
        }
        if(paginationOptions.getOffset() + paginationOptions.getPageSize() < paginationResult.getTotalCount()) {
            paginationLinks.add(new Link(createPaginationLink(baseUrl, paginationOptions.getPageNumber() + 1, paginationOptions.getPageSize()),PAGINATION_KEY_NEXT));
        }
        paginationLinks.add(new Link(createPaginationLink(baseUrl, paginationResult.getTotalPageCount() - 1, paginationOptions.getPageSize()),PAGINATION_KEY_LAST));

        return paginationLinks;
    }

    private static String createPaginationLink(String baseUrl, int page, int pageSize) {
        return baseUrl + "?" + PAGINATION_PARAM_PAGE + "=" + page + "&" + PAGINATION_PARAM_PAGE_ENTRIES + "=" + pageSize;
    }

    private static String getAPIBaseUrl() throws URISyntaxException {
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return new URI(uri.getScheme(),
                uri.getAuthority(),
                uri.getPath(),
                null,
                uri.getFragment()).toString();
    }

    private static <T extends TBase<?, ? extends TFieldIdEnum>> PaginationOptions<T> paginationOptionsFromPageable(Pageable pageable, String resourceClassName) throws ResourceClassNotFoundException {
        Comparator<T> comparator = RestPaginationHelper.comparatorFromPageable(pageable, resourceClassName);
        return new PaginationOptions<T>(pageable.getPageNumber(), pageable.getPageSize(), comparator);
    }

    private static <T extends TBase<?, ? extends TFieldIdEnum>> Comparator<T> comparatorFromPageable(Pageable pageable, String resourceClassName) throws ResourceClassNotFoundException {
        Sort.Order order = firstOrderFromPageable(pageable);
        if(order == null) {
            return ResourceComparatorGenerator.generateComparator(resourceClassName);
        }
        Comparator<T> comparator = ResourceComparatorGenerator.generateComparator(resourceClassName, order.getProperty());
        if(order.isDescending()) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    private static Sort.Order firstOrderFromPageable(Pageable pageable) {
        Sort sort = pageable.getSort();
        if(sort == null) {
            return null;
        }
        Iterator<Sort.Order> orderIterator = sort.iterator();
        if(orderIterator.hasNext()) {
            return orderIterator.next();
        } else {
            return null;
        }
    }
}
