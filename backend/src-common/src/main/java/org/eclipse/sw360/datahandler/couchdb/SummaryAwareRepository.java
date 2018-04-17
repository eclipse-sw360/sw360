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
package org.eclipse.sw360.datahandler.couchdb;

import org.eclipse.sw360.components.summary.DocumentSummary;
import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by bodet on 17/02/15.
 *
 * @author cedric.bodet@tngtech.com
 */
public class SummaryAwareRepository<T> extends DatabaseRepository<T> {

    protected final DocumentSummary<T> summary;

    public SummaryAwareRepository(Class<T> type, DatabaseConnector databaseConnector, DocumentSummary<T> summary) {
        super(type, databaseConnector);

        this.summary = summary;
    }

    public List<T> makeSummary(SummaryType type, Collection<String> ids) {
        if (ids == null) {
            return Collections.emptyList();
        }

        List<T> documents = get(ids);

        return makeSummaryFromFullDocs(type, documents);
    }

    public List<T> makeSummaryFromFullDocs(SummaryType type, Collection<T> docs) {
        return summary.makeSummary(type, docs);
    }


    public List<T> makeSummaryWithPermissions(SummaryType type, Collection<String> ids, User user) {
        if (ids == null) {
            return Collections.emptyList();
        }

        List<T> documents = get(ids);
        return makeSummaryWithPermissionsFromFullDocs(type, documents, user);
    }

    public List<T> makeSummaryWithPermissionsFromFullDocs(SummaryType type, Collection<T> docs, User user) {
        return summary.makeSummaryWithPermissionsFromFullDocs(type,docs,user);
    }

}
