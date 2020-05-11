/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.components;

import com.liferay.portal.kernel.events.Action;
import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.events.LifecycleAction;

import org.eclipse.sw360.portal.common.FossologyConnectionHelper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.osgi.service.component.annotations.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Class to validate the fossology connectivity at the server startup time.
 */
@Component(
    immediate = true,
    property = {
        "key=servlet.service.events.pre"
    },
    service = LifecycleAction.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    enabled = false
)
public class FossologyCheckConnectionOnStartupHook extends Action {
    protected final Logger log = LogManager.getLogger(getClass());

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
