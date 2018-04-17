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
package org.eclipse.sw360.portal.hooks;

import com.liferay.portal.kernel.events.Action;
import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.util.WebKeys;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Class to inject custom variables into the Velocity context to be used in the sw360-theme template
 *
 * @author: alex.borodin@evosoft.com
 */
public class BuildInfoForVelocityProviderHook extends Action {
    private static final String BUILD_INFO_PROPERTIES_FILE = "/buildInfo.properties";
    private static Map<Object, Object> buildInfo;
    private static Logger log = getLogger(BuildInfoForVelocityProviderHook.class);

    @Override
    public void run(HttpServletRequest request, HttpServletResponse response) throws ActionException {
        request.setAttribute(WebKeys.VM_VARIABLES, getBuildInfo());
    }

    private static Map<Object, Object> getBuildInfo() {
        if (buildInfo == null) {
            loadBuildInfo();
        }

        return buildInfo;
    }

    private static synchronized void loadBuildInfo() {
        buildInfo = new HashMap<>();
        Properties properties = CommonUtils.loadProperties(BuildInfoForVelocityProviderHook.class, BUILD_INFO_PROPERTIES_FILE, false);
        properties.forEach((s, value)-> {
            log.info(String.format("VELOCITYHOOK: attribute %s=%s", s, value));
            buildInfo.put(s, value);
        });
    }
}
