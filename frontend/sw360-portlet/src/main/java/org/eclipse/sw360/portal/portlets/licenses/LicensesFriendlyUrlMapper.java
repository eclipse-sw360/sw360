/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets.licenses;

import com.liferay.portal.kernel.portlet.DefaultFriendlyURLMapper;
import com.liferay.portal.kernel.portlet.FriendlyURLMapper;

import org.osgi.service.component.annotations.Component;

import static org.eclipse.sw360.portal.common.PortalConstants.LICENSES_PORTLET_NAME;

@Component(
        property = {
            "com.liferay.portlet.friendly-url-routes=org/eclipse/sw360/portal/mapper/license-friendly-url-routes.xml",
            "javax.portlet.name=" + LICENSES_PORTLET_NAME,
        },
        service = FriendlyURLMapper.class
    )
public class LicensesFriendlyUrlMapper extends DefaultFriendlyURLMapper {
    @Override
    public String getMapping() {
        return "license";
    }
}
