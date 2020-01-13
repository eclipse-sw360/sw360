/*
 * Copyright Siemens AG, 2018-2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.users;

import com.liferay.portal.kernel.events.Action;
import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.events.LifecycleAction;
import com.liferay.portal.kernel.util.PropsUtil;

import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.google.common.base.Strings.isNullOrEmpty;


@Component(
    immediate = true,
    property = "key=login.events.post",
    service = LifecycleAction.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class LandingPageAction extends Action {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final String DEFAULT_LANDING_PAGE_PATH_PROPERTY = "default.landing.page.path";
    private static final String DEFAULT_LANDING_PAGE_PATH = "/group/guest/home";

    private String landingPage;

    @Activate
    protected void activate() {
        log.info("Component [" + getClass().getCanonicalName() + "] has been ENABLED.");

        landingPage = PropsUtil.get(DEFAULT_LANDING_PAGE_PATH_PROPERTY);
        if (landingPage == null){
            landingPage = DEFAULT_LANDING_PAGE_PATH;
        }
        log.info("loaded landing page path from properties: " + landingPage);
    }

    @Modified
    protected void modified() {
        log.info("Component [" + getClass().getCanonicalName() + "] has been MODIFIED.");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Component [" + getClass().getCanonicalName() + "] has been DISABLED.");
    }

    @Override
    public void run(HttpServletRequest request, HttpServletResponse response) throws ActionException {
        if (isNullOrEmpty(request.getContextPath())) {
            try {
                log.info("Redirect to [" + landingPage + "].");
                response.sendRedirect(landingPage);
            } catch (IOException e) {
                throw new ActionException(e);
            }
        }
    }
}
