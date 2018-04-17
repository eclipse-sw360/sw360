#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
#
# All rights reserved.   This program and the accompanying materials
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
# initial author: alex.borodin@evosoft.com
#
# -----------------------------------------------------------------------------

import couchdb

COUCHSERVER = "http://localhost:5984/"
DBNAME = 'sw360db'

couch=couchdb.Server(COUCHSERVER)
db = couch[DBNAME]

licenses_by_id_fun = '''function(doc){
    if (doc.type=="license"){
        emit(doc._id, doc)
    }
}'''

linking_objects_fun = '''function(doc){
    if ((doc.type=="release" || doc.type=="component") && doc.mainLicenseIds && doc.mainLicenseIds.length > 0){
        emit(doc._id, doc)
    }
}'''

licenses = db.query(licenses_by_id_fun)
linking_objects = db.query(linking_objects_fun)


print 'Updating external links to licenses'
for linking_obj_row in linking_objects:
    linking_obj = linking_obj_row.value
    linking_obj['mainLicenseIds'] = map(lambda license_id: licenses[license_id].rows[0].value['shortname'], linking_obj['mainLicenseIds'])
    if 'mainLicenseNames' in linking_obj:
        del linking_obj['mainLicenseNames']
    db.save(linking_obj)

print 'Copying licenses to new ids'
for license_row in licenses:
    license = license_row.value
    db.delete(license)
    del license['_rev']
    license['_id'] = license['shortname']
    del license['shortname']
    db.save(license)
