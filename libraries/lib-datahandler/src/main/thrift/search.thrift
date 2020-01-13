/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
