/*
 * Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.users;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.http.*;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.portal.components.LoggingComponent;
import org.osgi.service.component.annotations.*;

import com.liferay.portal.kernel.events.*;

/**
 * This class can be used to write any custom logic post logout such as
 * redirecting to a custom logout page, integrating and invalidating third party
 * auth cookies viz mod-mellon etc.
 *
 * @author smruti.sahoo@siemens.com
 *
 */
@Component(
    immediate = true,
    property = {"key=logout.events.post" },
    service = LifecycleAction.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    enabled = false
)
public class LogoutAction extends LoggingComponent implements LifecycleAction {

    private static final String PROPERTIES_FILE_PATH = "/sw360.properties";
    private static final String LOGOUT_REDIRECT_URL = "logout.redirect.url";

    private String LOGOUT_REDIRECT_URL_VALUE;

    @Override
    @Activate
    protected void activate() {
        super.activate();

        Properties props = CommonUtils.loadProperties(LogoutAction.class, PROPERTIES_FILE_PATH);
        LOGOUT_REDIRECT_URL_VALUE = props.getProperty(LOGOUT_REDIRECT_URL, "");
    }

    @Override
    public void processLifecycleEvent(LifecycleEvent lifecycleEvent) throws ActionException {
        HttpServletResponse response = lifecycleEvent.getResponse();
        response.setHeader("Clear-Site-Data", "\"cache\", \"storage\", \"cookies\"");

        if (CommonUtils.isNotNullEmptyOrWhitespace(LOGOUT_REDIRECT_URL_VALUE)) {
            try {
                response.sendRedirect(LOGOUT_REDIRECT_URL_VALUE);
            } catch (IOException e) {
                log.error("Error redirecting to custom logout page", e);
            }
        }
    }
}
