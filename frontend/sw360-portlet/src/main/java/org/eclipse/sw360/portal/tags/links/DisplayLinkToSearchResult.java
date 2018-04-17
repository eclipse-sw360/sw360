/*
 * Copyright Siemens AG, 2015. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.tags.links;

import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.page.PortletDefaultPage;
import org.eclipse.sw360.portal.common.page.PortletReleasePage;
import org.eclipse.sw360.portal.portlets.LinkToPortletConfiguration;
import org.eclipse.sw360.portal.tags.urlutils.UrlWriter;

import javax.servlet.jsp.JspException;

import static org.eclipse.sw360.portal.tags.urlutils.UrlWriterImpl.renderUrl;

/**
 * @author daniele.fognini@tngtech.com
 */
public class DisplayLinkToSearchResult extends DisplayLinkAbstract {
    private SearchResult searchResult;

    public void setSearchResult(SearchResult searchResult) {
        this.searchResult = searchResult;
    }

    @Override
    protected String getTextDisplay() {
        return searchResult.getName();
    }

    @Override
    protected void writeUrl() throws JspException {
        String searchResultType = searchResult.getType();
        String searchResultId = searchResult.getId();
        UrlWriter writer;
        switch (searchResultType) {
            case SW360Constants.TYPE_RELEASE:
                writer = renderUrl(pageContext)
                        .toPortlet(LinkToPortletConfiguration.COMPONENTS, scopeGroupId)
                        .toPage(PortletDefaultPage.RELEASE_DETAIL)
                        .withParam(PortalConstants.RELEASE_ID, searchResultId);
                break;
            case SW360Constants.TYPE_PROJECT:
                writer =renderUrl(pageContext)
                    .toPortlet(LinkToPortletConfiguration.PROJECTS, scopeGroupId)
                    .toPage(PortletDefaultPage.DETAIL)
                    .withParam(PortalConstants.PROJECT_ID, searchResultId);
                break;
            case SW360Constants.TYPE_COMPONENT:
                writer =renderUrl(pageContext)
                    .toPortlet(LinkToPortletConfiguration.COMPONENTS, scopeGroupId)
                    .toPage(PortletDefaultPage.DETAIL)
                    .withParam(PortalConstants.COMPONENT_ID, searchResultId);
                break;
            case SW360Constants.TYPE_LICENSE:
                writer =renderUrl(pageContext)
                    .toPortlet(LinkToPortletConfiguration.LICENSES, scopeGroupId)
                    .toPage(PortletDefaultPage.DETAIL)
                    .withParam(PortalConstants.LICENSE_ID, searchResultId);
                break;
            default:
                throw new IllegalArgumentException("Unexpected searchResultType " + searchResultType);
        }
        writer.writeUrlToJspWriter();
    }
}
