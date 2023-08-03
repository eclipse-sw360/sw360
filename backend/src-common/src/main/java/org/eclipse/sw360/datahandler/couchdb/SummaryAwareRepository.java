/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.couchdb;

import org.eclipse.sw360.components.summary.DocumentSummary;
import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by bodet on 17/02/15.
 *
 * @author cedric.bodet@tngtech.com
 */
public class SummaryAwareRepository<T> extends DatabaseRepositoryCloudantClient<T> {

    protected final DocumentSummary<T> summary;

    public SummaryAwareRepository(Class<T> type, DatabaseConnectorCloudant databaseConnector, DocumentSummary<T> summary) {
        super(databaseConnector, type);

        this.summary = summary;
    }

    public List<T> makeSummary(SummaryType type, Collection<String> ids) {
        if (ids == null) {
            return Collections.emptyList();
        }

        List<T> documents = get(ids);

        return makeSummaryFromFullDocs(type, documents);
    }

    public List<T> makeSummary(SummaryType type, Collection<String> ids, boolean ignoreNotFound) {
        if (ids == null) {
            return Collections.emptyList();
        }

        List<T> documents = get(ids, ignoreNotFound);

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

    public Set<T> getFullDocsById(Set<String> docIds) {
        Set<T> docs = new HashSet<>();
        if (CommonUtils.isNullOrEmptyCollection(docIds)) {
            return docs;
        }
        List<T> listOfDocs = get(docIds);
        if (CommonUtils.isNotEmpty(listOfDocs)) {
            docs.addAll(listOfDocs);
        }
        return docs;
    }

    public List<T> getFullDocsByListIds(SummaryType type, Collection<String> ids) {
        if (ids == null) {
            return Collections.emptyList();
        }

        List<T> documents = getDocsByListIds(ids);

        return makeSummaryFromFullDocs(type, documents);
    }
}
