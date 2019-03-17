/*
 * Copyright Siemens AG, 2014-2018. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

namespace java org.eclipse.sw360.datahandler.thrift.codescoop
namespace php sw360.thrift.codescoop

struct CodescoopComponentIndex {
    1: required i32 interest,
    2: required i32 activity,
    3: required i32 health,
}

struct CodescoopComponentCategory {
    1: required string name,
    2: required string path,
}

struct CodescoopComponentOrigin {
    1: required string gitId,
    2: required string createdUtc,
    3: required string owner,
    4: required string name,
    5: required string title,
    6: required string license,
    7: required string url,
    8: required string logo,
    9: required string author,
    10: required string mirrorUrl,
    11: required string primaryLanguage,
    12: required string homepageUrl,
    13: required i32 diskUsage,
    14: required i32 rate,
    15: required bool forked,
    16: required bool isMirror,
    17: required string componentType,
    18: required set<string> languages,
    19: required set<string> topics,
    20: required set<string> keywords,
    21: required set<string> companies,
    22: required set<CodescoopComponentCategory> categories,
    23: required CodescoopComponentIndex index;
}

struct CodescoopSearchFilter {
    1: required list<i32> type;
    2: required list<string> languages;
    3: required list<string> licenses;
    4: required i32 minScore;
}

struct CodescoopComponentSearch {
    1: required string uuid;
    2: required string searchQuery,
    3: required string ownerQuery,
    4: required i32 limit,
    5: required i32 offset,
    6: required CodescoopSearchFilter filter;
}

struct CodescoopRelease {
    1: required string name,
    2: required string version,
    3: required i64 date,
    4: required string dateUTC,
    5: required string downloadUrl,
    6: required string license;
}

struct CodescoopAutocompleteRequest {
    1: required string search;
    2: required string by;
    3: required i32 limit;
}

struct CodescoopAutocompleteComponet {
    1: required string id;
    2: required string owner;
    3: required string name;
    4: required string title;
    5: required string url;
}
struct CodescoopAutocompleteResponse {
    1: required list<CodescoopComponent> repositories;
}

struct CodescoopComponent {
    1: required string id,
    2: required i32 rate,
    3: required string owner,
    4: required string name,
    5: required string purl,
    6: required i32 type,
    7: optional CodescoopComponentOrigin origin,
    8: string uuid;
}

service CodescoopService {

     bool isEnabled();

     list<CodescoopComponent> searchComponents(1: CodescoopComponentSearch request);

     string proceedComponentsJson(1: string json);

     list<CodescoopComponent> searchComponentsComposite(1: list<CodescoopComponentSearch> requestList);

     string proceedComponentsCompositeJson(1: string json);

     list<CodescoopComponent> searchComponentsPurl(1: list<string> purlList);

     string proceedComponentsPurlJson(1: string json);

     list<CodescoopRelease> searchComponentReleases(1: CodescoopComponentSearch request);

     string proceedComponentReleasesJson(1: string json);

     CodescoopAutocompleteResponse autocomplete(1: CodescoopAutocompleteRequest request);

     string proceedAutocompleteJson(1: string json);
}