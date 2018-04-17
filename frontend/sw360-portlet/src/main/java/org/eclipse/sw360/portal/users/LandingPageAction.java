/*
 * Copyright Siemens AG, 2018. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.users;

import com.liferay.portal.kernel.events.Action;
import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.util.PropsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * @author alex.borodin@evosoft.com
 */
public class LandingPageAction extends Action {

    private static final Logger log = LoggerFactory.getLogger(LandingPageAction.class);
    private static final String DEFAULT_LANDING_PAGE_PATH_PROPERTY = "default.landing.page.path";
    private static final String DEFAULT_LANDING_PAGE_PATH = "/group/guest/home";

    private static String landingPage;

    static {
        landingPage = PropsUtil.get(DEFAULT_LANDING_PAGE_PATH_PROPERTY);
        if (landingPage == null){
            landingPage = DEFAULT_LANDING_PAGE_PATH;
        }
        log.info("loaded landing page path from properties: " + landingPage);
    }

    @Override
    public void run(HttpServletRequest request, HttpServletResponse response) throws ActionException {
        if (isNullOrEmpty(request.getContextPath())) {
            try {
                response.sendRedirect(landingPage);
            } catch (IOException e) {
                throw new ActionException(e);
            }
        }
    }
}
