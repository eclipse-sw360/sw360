/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
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

import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.page.PortletDefaultPage;
import org.eclipse.sw360.portal.portlets.LinkToPortletConfiguration;

/**
 * @author alex.borodin@evosoft.com
 */
public class DisplayLinkToModerationRequest extends DisplayLinkAbstract {
    private ModerationRequest moderationRequest;
    private Boolean showName = true;
    private String moderationRequestId;

    public void setModerationRequest(ModerationRequest moderationRequest) {
        this.moderationRequest = moderationRequest;
        moderationRequestId =moderationRequest.getId();
    }

    public void setModerationRequestId(String moderationRequestId) {
        this.moderationRequestId = moderationRequestId;
        showName=false;
    }
    public void setShowName(Boolean showName) {
        this.showName = showName;
    }

    @Override
    protected String getTextDisplay() {
        return showName ? moderationRequest.getDocumentName() : null;
    }

    @Override
    protected void writeUrl() throws JspException {
        renderUrl(pageContext)
                .toPortlet(LinkToPortletConfiguration.MODERATION, scopeGroupId)
                .toPage(PortletDefaultPage.EDIT)
                .withParam(PortalConstants.MODERATION_ID, moderationRequestId)
                .writeUrlToJspWriter();
    }
}
