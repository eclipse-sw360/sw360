/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.fossology.db;

import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseRepository;
import org.eclipse.sw360.datahandler.thrift.fossology.FossologyHostFingerPrint;

/**
 * @author daniele.fognini@tngtech.com
 */
public class FossologyFingerPrintRepository extends DatabaseRepository<FossologyHostFingerPrint> {
    public FossologyFingerPrintRepository(Class<FossologyHostFingerPrint> type, DatabaseConnector fossologyFingerPrintDatabaseConnector) {
        super(type, fossologyFingerPrintDatabaseConnector);
    }
}
