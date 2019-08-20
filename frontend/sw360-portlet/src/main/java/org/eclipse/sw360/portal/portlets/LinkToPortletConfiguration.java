/*
 * Copyright Siemens AG, 2015-2017. Part of the SW360 Portal Project,
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.Portlet;
import com.liferay.portal.kernel.service.LayoutLocalServiceUtil;
import com.liferay.portal.kernel.service.PortletLocalServiceUtil;

import org.eclipse.sw360.portal.portlets.components.ComponentPortlet;
import org.eclipse.sw360.portal.portlets.licenses.LicensesPortlet;
import org.eclipse.sw360.portal.portlets.moderation.ModerationPortlet;
import org.eclipse.sw360.portal.portlets.projects.ProjectPortlet;
import org.eclipse.sw360.portal.portlets.vulnerabilities.VulnerabilitiesPortlet;

import java.util.Optional;

/**
 * Maps Portlet classes to sw360 internal portlet names and layouts.
 * Portlet names are created internally
 * by concatenating data from the xml configuration file and deployment data.
 * To avoid update efforts (e.g. after increasing the version number of the war file, this is handled here.
 * If scopeGroupId is not null,
 * findPlid searches for a lower case version of the enum instance name to identify a Layout.
 * @author daniele.fognini@tngtech.com
 */
public enum LinkToPortletConfiguration {
    COMPONENTS(ComponentPortlet.class),
    PROJECTS(ProjectPortlet.class),
    LICENSES(LicensesPortlet.class),
    MODERATION(ModerationPortlet.class),
    VULNERABILITIES(VulnerabilitiesPortlet.class);

    private final Class<? extends Sw360Portlet> portletClass;

    LinkToPortletConfiguration(Class<? extends Sw360Portlet> portletClass) {
        this.portletClass = portletClass;
    }

    public Portlet findPortlet() {
        Optional<Portlet> portlet = PortletLocalServiceUtil.getPortlets().stream()
                .filter(p -> p.getPortletClass().equals(portletClass.getName())).findFirst();
        if (portlet.isPresent()) {
            return portlet.get();
        }
        throw new IllegalArgumentException("Could not find portlet name for " + this.portletClass);
    }

    public String portletName() {
        return findPortlet().getPortletId();
    }

    public long findPlid(Long portletGroupId) {
        if (portletGroupId == null ) {
            return 0;
        }
        try {
            Optional<Layout> layout = LayoutLocalServiceUtil.getLayouts(portletGroupId, true).stream()
            .filter(l -> ("/"+name().toLowerCase()).equals(l.getFriendlyURL()))
            .findFirst();
            if (layout.isPresent()) {
                return layout.get().getPlid();
            }
        } catch (SystemException e) {
            throw new IllegalStateException("Could not get layout for portlet " + portletClass, e);
        }
        return 0;
    }
}
