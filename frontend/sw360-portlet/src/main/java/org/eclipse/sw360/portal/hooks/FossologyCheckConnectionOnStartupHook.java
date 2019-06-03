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
package org.eclipse.sw360.portal.hooks;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.sw360.portal.common.FossologyConnectionHelper;

import com.liferay.portal.kernel.events.Action;
import com.liferay.portal.kernel.events.ActionException;

/**
 * Class to validate the fossology connectivity at the server startup time.
 *
 * @author smruti.sahoo@siemens.com
 *
 */

public class FossologyCheckConnectionOnStartupHook extends Action {

	private static final Logger log = Logger.getLogger(FossologyCheckConnectionOnStartupHook.class);

	private static boolean calledOnServerstartUp;

	@Override
	public void run(HttpServletRequest request, HttpServletResponse response) throws ActionException {
		if (!calledOnServerstartUp) {
			FossologyConnectionHelper.getInstance().checkFossologyConnection();
			calledOnServerstartUp = true;
		}

	}

}
