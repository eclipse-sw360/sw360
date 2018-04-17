/*
 * Copyright Siemens AG, 2015. Part of the SW360 Portal Project.
 * With modifications by Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.common.page;

import org.eclipse.sw360.portal.common.PortalConstants;

/**
 * @author daniele.fognini@tngtech.com
 */
public enum PortletDefaultPage implements PortletPage {
    DETAIL(PortalConstants.PAGENAME_DETAIL),
    RELEASE_DETAIL(PortalConstants.PAGENAME_RELEASE_DETAIL),
    EDIT(PortalConstants.PAGENAME_EDIT);

    private final String pagename;

    PortletDefaultPage(String pagename) {
        this.pagename = pagename;
    }

    @Override
    public String pagename() {
        return pagename;
    }
}
