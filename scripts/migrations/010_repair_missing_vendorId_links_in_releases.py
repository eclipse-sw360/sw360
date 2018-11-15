#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2018. Part of the SW360 Portal Project.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# This is a manual database migration script. It is assumed that a
# dedicated framework for automatic migration will be written in the
# future. When that happens, this script should be refactored to conform
# to the framework's prerequisites to be run by the framework. For
# example, server address and db name should be parametrized, the code
# reorganized into a single class or function, etc.
#
# The usage of this cleanup script is optional.
# It can be used to clean dead vendorId links in releases.
# As default the script runs in a dry mode and doesnt update releases.
#
# initial author: thomas.maier@evosoft.com
#
# -----------------------------------------------------------------------------
import couchdb

DRY_RUN = True
COUCHSERVER = "http://localhost:5984/"
DBNAME = 'sw360db'
RELEASENAME = 'name'
VENDORID = 'vendorId'

couch = couchdb.Server(COUCHSERVER)
db = couch[DBNAME]

get_all_vendors_func = '''function(doc){
        if (doc.type=='vendor') {
            emit(doc._id, doc)
        }
    }'''

get_all_releases_func = '''function(doc){
        if (doc.type=='release' && doc.vendorId != null) {
            emit(doc._id, doc)
        }
    }'''

print 'Retrieve all vendors...'
vendors = db.query(get_all_vendors_func)
print 'Retrieve all releases with vendors...'
releases = db.query(get_all_releases_func)

vendorsById = []
for vendor in vendors:
    vendorsById.append(vendor.id)

print 'got all vendors now, now comparing ...'

for release in releases:
    docRelease = release.value
    releaseVendorId = docRelease[VENDORID]

    if releaseVendorId and not releaseVendorId in vendorsById:
        if DRY_RUN:
            print 'WARN: not saving release ' + docRelease[RELEASENAME] + ' with vendor: ' + releaseVendorId + ' - DRY_RUN'
        else:
            docRelease[VENDORID] = None
            db.save(docRelease)
            print 'INFO: saving release ' + docRelease[RELEASENAME] + ' without vendor: ' + releaseVendorId + '.'
    else:
        print 'INFO: release ' + docRelease[RELEASENAME] + ' found vendorId ' + releaseVendorId + '.'
