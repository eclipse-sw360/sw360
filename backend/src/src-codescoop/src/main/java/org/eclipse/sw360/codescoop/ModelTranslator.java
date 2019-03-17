/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.codescoop;

import com.codescoop.client.model.autocomplete.AutocompleteRequest;
import com.codescoop.client.model.autocomplete.AutocompleteResponse;
import com.codescoop.client.model.component.*;
import com.codescoop.client.model.search.SearchRequest;
import org.eclipse.sw360.datahandler.thrift.codescoop.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.mid;

public class ModelTranslator {

    public static List<SearchRequest> translateSearchRequest(List<CodescoopComponentSearch> sw360Request) {
        return sw360Request.stream().map(ModelTranslator::translateSearchRequest).collect(Collectors.toList());
    }

    public static SearchRequest translateSearchRequest(CodescoopComponentSearch sw360Request) {
        SearchRequest.Builder builder = SearchRequest
                .build()
                .searchQuery(sw360Request.searchQuery)
                .ownerQuery(sw360Request.ownerQuery);

        if (isNotBlank(sw360Request.getUuid())) {
            builder.uuid(sw360Request.getUuid());
        }
        if (sw360Request.getLimit() > 1) {
            builder.searchLimit(sw360Request.getLimit());
        }
        if (sw360Request.getOffset() > 0) {
            builder.searchLimit(sw360Request.getOffset());
        }

        if (sw360Request.getFilter() != null) {
            builder.filterType(sw360Request.getFilter().getType());
            builder.filterLanguages(sw360Request.getFilter().getLanguages());
            builder.filterLicenses(sw360Request.getFilter().getLicenses());
            builder.filterMinScore((float) sw360Request.getFilter().getMinScore());
        }

        return builder.create();
    }

    public static List<CodescoopComponent> translateComponent(List<Component> external) {
        return external.stream().map(ModelTranslator::translateComponent).collect(Collectors.toList());
    }

    public static CodescoopComponent translateComponent(Component external) {
        CodescoopComponent c = new CodescoopComponent(
                external.getId(),
                external.getRate(),
                external.getOwner(),
                external.getName(),
                external.getPurl(),
                external.getType(),
                external.getUuid());
        if (external.getOrigin() != null) {
            c.setOrigin(translateOrigin(external.getOrigin()));
        }
        return c;
    }

    public static CodescoopComponentOrigin translateOrigin(ComponentOrigin external) {
        return new CodescoopComponentOrigin(
                external.getGitId(),
                external.getCreatedUTC(),
                external.getOwner(),
                external.getName(),
                external.getTitle(),
                external.getLicense(),
                external.getUrl(),
                external.getLogo(),
                external.getAuthor(),
                external.getMirrorUrl(),
                external.getPrimaryLanguage(),
                external.getHomepageUrl(),
                external.getDiskUsage(),
                external.getRate(),
                external.isForked(),
                external.isMirror(),
                external.getComponentType(),
                external.getLanguages(),
                external.getTopics(),
                external.getCsKeywords(),
                external.getCsCompanies(),
                translateCategory(external.getHierarchicalCategories()),
                translateIndex(external.getCompositeIndex()));
    }

    public static CodescoopComponentCategory translateCategory(HierarchicalCategory external) {
        return new CodescoopComponentCategory(external.getName(), external.getPath());
    }

    public static Set<CodescoopComponentCategory> translateCategory(Set<HierarchicalCategory> external) {
        return external.stream().map(ModelTranslator::translateCategory).collect(Collectors.toSet());
    }

    public static CodescoopComponentIndex translateIndex(ComponentIndex external) {
        return new CodescoopComponentIndex(
                external.getInterestPercent(),
                external.getActivityPercent(),
                external.getHealthPercent());
    }

    public static CodescoopRelease translateRelease(Release external) {
        return new CodescoopRelease(
                external.getName(),
                external.getVersion(),
                external.getDate(),
                external.getDateUTC(),
                external.getDownloadUrl(),
                external.getLicense());
    }

    public static List<CodescoopRelease> translateRelease(List<Release> external) {
        return external.stream().map(ModelTranslator::translateRelease).collect(Collectors.toList());
    }

    public static AutocompleteRequest translateAutocompleteRequest(CodescoopAutocompleteRequest sw360Request) {
        return AutocompleteRequest
                .build()
                .search(sw360Request.getSearch())
                .limit(sw360Request.getLimit())
                .by(sw360Request.getBy())
                .create();
    }

    public static CodescoopAutocompleteComponet translateAutocompleteRepository(ComponentOrigin external) {
        return new CodescoopAutocompleteComponet(
                external.getId(),
                external.getOwner(),
                external.getName(),
                external.getTitle(),
                external.getUrl()
        );
    }

    public static CodescoopAutocompleteResponse translateAutocompleteResponse(AutocompleteResponse external) {
        CodescoopAutocompleteResponse response = new CodescoopAutocompleteResponse();
        List<CodescoopComponent> repositories = external
                .getRepositories()
                .stream()
                .map(repo -> translateComponent(repo))
                .collect(Collectors.toList());
        response.setRepositories(repositories);
        return response;
    }
}
