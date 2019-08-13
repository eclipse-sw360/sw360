/*
 * Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.search;

import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.datahandler.thrift.search.SearchService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.portlet.*;

import com.google.common.base.Strings;
import com.liferay.portal.kernel.util.PortalUtil;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.portal.common.PortalConstants.*;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
        "/org/eclipse/sw360/portal/portlets/base.properties",
        "/org/eclipse/sw360/portal/portlets/default.properties"
    },
    property = {
        "javax.portlet.name=" + SEARCH_PORTLET_NAME,

        "javax.portlet.display-name=Search Results",
        "javax.portlet.info.short-title=Search Results",
        "javax.portlet.info.title=Search Results",

        "javax.portlet.init-param.view-template=/html/search/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class SearchPortlet extends Sw360Portlet {

    private static final Logger log = Logger.getLogger(SearchPortlet.class);

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        final User user = UserCacheHolder.getUserFromRequest(request);
        String searchtext = request.getParameter(KEY_SEARCH_TEXT);
        String[] typeMaskArray = request.getParameterValues(TYPE_MASK);
        String searchQuery = PortalUtil.getOriginalServletRequest(PortalUtil.getHttpServletRequest(request)).getParameter("q");

        List<String> typeMask;
        if (typeMaskArray != null) { // premature optimization would add && typeMaskArray.length<6
            typeMask = Arrays.asList(typeMaskArray);
        } else {
            typeMask = Collections.emptyList();
            log.info("typeMask set to emptyList");
        }

        if (isNullOrEmpty(searchtext)) {
            searchtext = searchQuery;
        }
        searchtext = Strings.nullToEmpty(searchtext);

        List<SearchResult> searchResults;
        try {
            SearchService.Iface client = thriftClients.makeSearchClient();
            searchResults = client.searchFiltered(searchtext, user, typeMask);
        } catch (TException e) {
            log.error("Search could not be performed!", e);
            searchResults = Collections.emptyList();
        }

        // Set the results
        request.setAttribute(KEY_SEARCH_TEXT, searchtext);
        request.setAttribute(KEY_SUMMARY, searchResults);
        request.setAttribute(TYPE_MASK, typeMask);

        // Proceed with page rendering
        super.doView(request, response);
    }

}
