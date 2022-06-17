/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets.components;

import com.liferay.portal.kernel.portlet.DefaultFriendlyURLMapper;
import com.liferay.portal.kernel.portlet.FriendlyURLMapper;

import org.osgi.service.component.annotations.Component;

import static org.eclipse.sw360.portal.common.PortalConstants.COMPONENT_PORTLET_NAME;

@Component(
        property = {
            "com.liferay.portlet.friendly-url-routes=org/eclipse/sw360/portal/mapper/component-friendly-url-routes.xml",
            "javax.portlet.name=" + COMPONENT_PORTLET_NAME,
        },
        service = FriendlyURLMapper.class
    )
public class ComponentFriendlyUrlMapper extends DefaultFriendlyURLMapper {
    @Override
    public String getMapping() {
        return "component";
    }
}
