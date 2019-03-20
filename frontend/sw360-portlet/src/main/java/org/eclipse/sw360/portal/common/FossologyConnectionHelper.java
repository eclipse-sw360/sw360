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
package org.eclipse.sw360.portal.common;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;

/**
 * Helper class to validate the fossology connectivity.
 *
 * @author smruti.sahoo@siemens.com
 *
 */

public class FossologyConnectionHelper {

	private static final Logger log = Logger.getLogger(FossologyConnectionHelper.class);
	private static FossologyConnectionHelper instance;
	private boolean fossologyConnectionEnabled;

	private FossologyConnectionHelper() {
	}

	public RequestStatus checkFossologyConnection() {
		RequestStatus checkConnection = RequestStatus.FAILURE;
		try {
			checkConnection = new ThriftClients().makeFossologyClient().checkConnection();
		} catch (TException e) {
			fossologyConnectionEnabled = false;
			log.error("Error connecting to backend", e);
		}
		fossologyConnectionEnabled = checkConnection.equals(RequestStatus.SUCCESS);
		return checkConnection;
	}

	public static synchronized FossologyConnectionHelper getInstance() {
		if (instance == null) {
			instance = new FossologyConnectionHelper();
		}
		return instance;
	}

	public boolean isFossologyConnectionEnabled() {
		return fossologyConnectionEnabled;
	}
}
