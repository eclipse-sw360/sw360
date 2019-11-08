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
include "sw360.thrift"
include "users.thrift"

namespace java org.eclipse.sw360.datahandler.thrift.vendors
namespace php sw360.thrift.vendors

typedef sw360.RequestStatus RequestStatus
typedef users.User User
typedef users.RequestedAction RequestedAction

struct Vendor {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "vendor",
    4: required string shortname,
    5: required string fullname,
    6: required string url

    200: optional map<RequestedAction, bool> permissions,
}

service VendorService {

    /**
     * return vendor specified by id
     **/
    Vendor getByID(1: string id);

    /**
     * return list of all vendors in database
     **/
    list<Vendor> getAllVendors();

    /**
     * return set of names of all vendors in database, no duplicates
     **/
    set<string> getAllVendorNames();

    /**
     * get lists of vendors whose fullname or shortname starts with searchText
     **/
    list<Vendor> searchVendors(1: string searchText);

    /**
     * get set of vendorIds whose fullname or shortname starts with searchText
     **/
    list<string> searchVendorIds(1: string searchText);

    /**
     * write vendor to database and return id
     **/
    string addVendor(1: Vendor vendor);

    /**
     * vendor specified by id is deleted from database if user has sufficient permissions, otherwise FAILURE is returned
     **/
    RequestStatus deleteVendor(1: string id, 2: User user);

    /**
     * vendor specified by id is updated in database if user has sufficient permissions, otherwise FAILURE is returned
     **/
    RequestStatus updateVendor(1: Vendor vendor, 2: User user);

    /**
     * merge vendor identified by vendorSourceId into vendor identified by vendorTargetId.
     * The vendorSelection shows which data has to be set on the target. The source will be deleted afterwards.
     * If user does not have permissions, RequestStatus.ACCESS_DENIED is returned
     * If any of the vendor has an active moderation request, it's a noop and RequestStatus.IN_USE is returned.
     * On any other error, REQUEST_FAILURE is returned.
     **/
    RequestStatus mergeVendors(1: string vendorTargetId, 2: string vendorSourceId, 3: Vendor vendorSelection, 4: User user);

}
