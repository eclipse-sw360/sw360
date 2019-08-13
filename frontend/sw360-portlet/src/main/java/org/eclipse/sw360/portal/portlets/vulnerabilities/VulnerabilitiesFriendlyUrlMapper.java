/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.vulnerabilities;

import com.liferay.portal.kernel.portlet.DefaultFriendlyURLMapper;
import com.liferay.portal.kernel.portlet.FriendlyURLMapper;

import org.osgi.service.component.annotations.Component;

import static org.eclipse.sw360.portal.common.PortalConstants.VULNERABILITIES_PORTLET_NAME;

@Component(
        property = {
            "com.liferay.portlet.friendly-url-routes=org/eclipse/sw360/portal/mapper/vulnerability-friendly-url-routes.xml",
            "javax.portlet.name=" + VULNERABILITIES_PORTLET_NAME,
        },
        service = FriendlyURLMapper.class
    )
public class VulnerabilitiesFriendlyUrlMapper extends DefaultFriendlyURLMapper {
    @Override
    public String getMapping() {
        return "vulnerability";
    }
}
