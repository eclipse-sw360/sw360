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
package org.eclipse.sw360.portal.components;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.events.Action;
import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.events.LifecycleAction;

import org.apache.log4j.Logger;
import org.eclipse.sw360.portal.common.FossologyConnectionHelper;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

/**
 * Class to validate the fossology connectivity at the server startup time.
 */
@Component(
    immediate = true,
    property = {
        "key=servlet.service.events.pre"
    },
    service = LifecycleAction.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class FossologyCheckConnectionOnStartupHook extends Action {
    protected final Logger log = Logger.getLogger(getClass());

	private static boolean calledOnServerstartUp;

	@Override
	public void run(HttpServletRequest request, HttpServletResponse response) throws ActionException {
		if (!calledOnServerstartUp) {
            FossologyConnectionHelper.getInstance().checkFossologyConnection();
            log.info("Fossology connection state: " +
                (FossologyConnectionHelper.getInstance().isFossologyConnectionEnabled() ? "SUCCESS" : "FAILED"));

			calledOnServerstartUp = true;
		}
	}

	@Activate
    protected void activate() {
        log.info("Component [" + getClass().getCanonicalName() + "] has been ENABLED.");
    }

    @Modified
    protected void modified() {
        log.info("Component [" + getClass().getCanonicalName() + "] has been MODIFIED.");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Component [" + getClass().getCanonicalName() + "] has been DISABLED.");
    }
}
