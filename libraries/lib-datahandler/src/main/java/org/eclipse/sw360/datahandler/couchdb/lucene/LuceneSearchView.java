/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.couchdb.lucene;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 *
 * @author cedric.bodet@tngtech.com
 */
public class LuceneSearchView {

    final String searchView;
    final String searchFunction;
    final String searchBody;

    public LuceneSearchView(String searchView, String searchFunction, String searchBody) {
        if (isNullOrEmpty(searchView) || isNullOrEmpty(searchFunction) || isNullOrEmpty(searchBody)) {
            throw new IllegalArgumentException("Invalid search functions, provided strings cannot be empty!");
        }

        this.searchView = searchView;
        this.searchFunction = searchFunction;
        this.searchBody = searchBody;
    }

}
