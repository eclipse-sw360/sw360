#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# This is a manual database migration script. It is assumed that a
# dedicated framework for automatic migration will be written in the
# future. When that happens, this script should be refactored to conform
# to the framework's prerequisites to be run by the framework. For
# example, server address and db name should be parametrized, the code
# reorganized into a single class or function, etc.
#
# initial author: alex.borodin@evosoft.com
#
# -----------------------------------------------------------------------------

import couchdb

COUCHSERVER = "http://localhost:5984/"
DBNAME = 'sw360db'

couch=couchdb.Server(COUCHSERVER)
db = couch[DBNAME]

releases_with_contacts_fun = '''function(doc){
    if (doc.type=="release" && doc.contacts){
        emit(doc._id, doc)
    }
}'''

releases_with_contacts = db.query(releases_with_contacts_fun)

print 'Renaming release.contacts to release.contributors'
for release_row in releases_with_contacts:
    release = release_row.value
    release['contributors'] = release['contacts']
    del release['contacts']
    db.save(release)

print 'Done'
