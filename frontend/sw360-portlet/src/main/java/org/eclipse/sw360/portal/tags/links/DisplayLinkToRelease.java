/*
 * Copyright Siemens AG, 2015-2017. Part of the SW360 Portal Project.
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

import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.page.PortletReleasePage;
import org.eclipse.sw360.portal.portlets.LinkToPortletConfiguration;

import javax.servlet.jsp.JspException;

import static org.eclipse.sw360.datahandler.common.SW360Utils.printFullname;
import static org.eclipse.sw360.datahandler.common.SW360Utils.printName;
import static org.eclipse.sw360.portal.tags.urlutils.UrlWriterImpl.renderUrl;

/**
 * @author daniele.fognini@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class DisplayLinkToRelease extends DisplayLinkAbstract {
    private Release release;
    private PortletReleasePage page = PortletReleasePage.DETAIL;
    private Boolean showName = true;
    private String releaseId;
    private Boolean showFullname = false;

    public void setRelease(Release release) {
        this.release = release;
        releaseId=release.getId();
    }

    public void setPage(PortletReleasePage page) {
        this.page = page;
    }
    public void setReleaseId(String releaseId) {
        this.releaseId = releaseId;
        showName=false;
    }
    public void setShowName(Boolean showName) {
        this.showName = showName;
    }

    public void setShowFullname(Boolean showFullname) {
        this.showFullname = showFullname;
    }

    @Override
    protected String getTextDisplay() {
        return showName ? (showFullname ? printFullname(release) : printName(release)): null;
    }

    @Override
    protected void writeUrl() throws JspException {
        renderUrl(pageContext)
                .toPortlet(LinkToPortletConfiguration.COMPONENTS, scopeGroupId)
                .toPage(page)
                .withParam(PortalConstants.RELEASE_ID, releaseId)
                .writeUrlToJspWriter();
    }
}
