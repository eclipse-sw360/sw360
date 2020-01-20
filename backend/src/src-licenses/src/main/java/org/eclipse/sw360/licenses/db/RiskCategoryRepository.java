/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses.db;

import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseRepository;
import org.eclipse.sw360.datahandler.thrift.licenses.RiskCategory;
import org.ektorp.support.View;

/**
 * @author johannes.najjar@tngtech.com
 */
@View(name = "all", map = "function(doc) { if (doc.type == 'riskCategory') emit(null, doc._id) }")
public class RiskCategoryRepository extends DatabaseRepository<RiskCategory> {

    public RiskCategoryRepository(DatabaseConnector db) {
        super(RiskCategory.class, db);

        initStandardDesignDocument();
    }
}
