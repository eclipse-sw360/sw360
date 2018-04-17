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
