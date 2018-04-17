/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
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

import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.portal.common.PortalConstants.*;

/**
 * Created by bodet on 03/12/14.
 *
 * @author cedric.bodet@tngtech.com
 */
public class SearchPortlet extends Sw360Portlet {

    private static final Logger log = Logger.getLogger(SearchPortlet.class);

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        final User user = UserCacheHolder.getUserFromRequest(request);
        String searchtext = request.getParameter(KEY_SEARCH_TEXT);
        String[] typeMaskArray = request.getParameterValues(TYPE_MASK);

        List<String> typeMask;
        if (typeMaskArray != null) { // premature optimization would add && typeMaskArray.length<6
            typeMask = Arrays.asList(typeMaskArray);
        } else {
            typeMask = Collections.emptyList();
            log.info("typeMask set to emptyList");
        }

        String usedsearchtext;
        if (isNullOrEmpty(searchtext)) {
            usedsearchtext = "";
            searchtext = "";
        } else {
            usedsearchtext = searchtext;
        }


        List<SearchResult> searchResults;
        try {
            SearchService.Iface client = thriftClients.makeSearchClient();
            searchResults = client.searchFiltered(usedsearchtext, user, typeMask);
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
