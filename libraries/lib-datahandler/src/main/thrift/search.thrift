/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
include "users.thrift"

namespace java org.eclipse.sw360.datahandler.thrift.search
namespace php sw360.thrift.search

typedef users.User User

struct ResultDetail {
    1: required string key,
    2: optional string value
}

struct SearchResult {
    1: required string id,
    2: required string type
    3: required string name,
    4: required double score,
    5: optional list<ResultDetail> details
}

service SearchService {

    /**
     * return all documents that have properties starting with text, user is ignored
     **/
    list<SearchResult> search(1: required string text, 2: User user);

    /**
     *  return all documents of a type that is in the typeMask list and that have properties starting with text,
     *  user is ignored
     **/
    list<SearchResult> searchFiltered(1: required string text, 2: User user, 3: list<string> typeMask);
}
