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
package org.eclipse.sw360.components.summary;

import org.eclipse.sw360.datahandler.permissions.DocumentPermissions;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by bodet on 17/02/15.
 *
 * @author cedric.bodet@tngtech.com
 *
 * This does some processing of the documents to trim unneeded fields away and fill computed fields.
 */
public abstract class DocumentSummary<T> {

    protected abstract T summary(SummaryType type, T document);

    public T makeSummary(SummaryType type, T document) {
        if (document == null) return null;
        return summary(type, document);
    }

    public List<T> makeSummary(SummaryType type, Collection<T> fullDocuments) {
        if (fullDocuments == null) return Collections.emptyList();

        List<T> documents = new ArrayList<>(fullDocuments.size());
        for (T fullDocument : fullDocuments) {
            T document = makeSummary(type, fullDocument);
            if (document != null) documents.add(document);
        }
        return documents;
    }

    public T makeSummaryWithPermissions(SummaryType type, T document, User user) {
        if (document == null) return null;
        DocumentPermissions<T> permissions = PermissionUtils.makePermission(document, user);
        T summary = makeSummary(type, document);
        permissions.fillPermissionsInOther(summary);
        return summary;
    }

    public List<T> makeSummaryWithPermissionsFromFullDocs(SummaryType type, Collection<T> docs, User user) {
        if (docs == null) {
            return Collections.emptyList();
        }

        List<T> documents = new ArrayList<>();
        for (T doc : docs) {
            T document = makeSummaryWithPermissions(type, doc, user);
            if (document != null) {
                documents.add(document);
            }
        }
        return documents;
    }
}
