/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.common;

import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;

/**
 * Helper class to validate the fossology connectivity.
 *
 * @author smruti.sahoo@siemens.com
 *
 */
public class FossologyConnectionHelper {

    private static final Logger log = LogManager.getLogger(FossologyConnectionHelper.class);
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
