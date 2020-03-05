/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 * With contributions by Siemens Healthcare Diagnostics Inc, 2018.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags.links;

import static org.eclipse.sw360.portal.tags.urlutils.UrlWriterImpl.renderUrl;

import javax.servlet.jsp.JspException;

import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.page.PortletDefaultPage;
import org.eclipse.sw360.portal.portlets.LinkToPortletConfiguration;

/**
 * @author abdul.mannankapti@siemens.com
 */
public class DisplayLinkToClearingRequest extends DisplayLinkAbstract {
    private String clearingRequestId;

    public void setClearingRequestId(String clearingRequestId) {
        this.clearingRequestId = clearingRequestId;
    }

    @Override
    protected String getTextDisplay() {
        return clearingRequestId;
    }

    @Override
    protected void writeUrl() throws JspException {
        renderUrl(pageContext)
                .toPortlet(LinkToPortletConfiguration.MODERATION, scopeGroupId)
                .toPage(PortletDefaultPage.CLEARING_REQUEST_DETAIL)
                .withParam(PortalConstants.CLEARING_REQUEST_ID, clearingRequestId)
                .writeUrlToJspWriter();
    }
}
