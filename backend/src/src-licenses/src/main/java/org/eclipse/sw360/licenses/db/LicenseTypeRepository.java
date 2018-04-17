/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 * With modifications by Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.licenses.db;

import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseRepository;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseType;
import org.ektorp.support.View;

/**
 * @author johannes.najjar@tngtech.com
 */
@View(name = "all", map = "function(doc) { if (doc.type == 'licenseType') emit(null, doc._id) }")
public class LicenseTypeRepository extends DatabaseRepository<LicenseType> {
    public LicenseTypeRepository(DatabaseConnector db) {
        super(LicenseType.class, db);

        initStandardDesignDocument();
    }
}
