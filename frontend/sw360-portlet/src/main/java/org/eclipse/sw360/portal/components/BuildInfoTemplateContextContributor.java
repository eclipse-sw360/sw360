/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.components;

import com.liferay.portal.kernel.template.TemplateContextContributor;

import org.eclipse.sw360.datahandler.common.CommonUtils;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

/**
 * Class to inject custom variables into the Velocity context to be used in the sw360-theme template
 *
 * @author: alex.borodin@evosoft.com
 */
@Component(
    immediate = true,
    property = {
        "type=" + TemplateContextContributor.TYPE_THEME
    },
    service = TemplateContextContributor.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class BuildInfoTemplateContextContributor extends LoggingComponent implements TemplateContextContributor {
    private static final String BUILD_INFO_PROPERTIES_FILE = "/buildInfo.properties";
    private static Map<Object, Object> buildInfo;

    @Override
    public void prepare(Map<String, Object> contextObjects, HttpServletRequest request) {
        contextObjects.put("sw360_build_info", getBuildInfo());
    }

    private Map<Object, Object> getBuildInfo() {
        if (buildInfo == null) {
            loadBuildInfo();
        }

        return buildInfo;
    }

    private synchronized void loadBuildInfo() {
        buildInfo = new HashMap<>();
        Properties properties = CommonUtils.loadProperties(BuildInfoTemplateContextContributor.class, BUILD_INFO_PROPERTIES_FILE, false);
        properties.forEach((s, value)-> {
            log.info(String.format("Build Info Context Contributor: attribute %s=%s", s, value));
            buildInfo.put(s, value);
        });
    }
}
