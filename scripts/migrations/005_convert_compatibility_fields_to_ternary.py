#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright (c) Bosch Software Innovations GmbH 2017.
# Part of the SW360 Portal Project.
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
# -----------------------------------------------------------------------------

import couchdb

COUCHSERVER = "http://localhost:5984/"
DBNAME = 'sw360db'

couch=couchdb.Server(COUCHSERVER)
db = couch[DBNAME]

# -----------------------------------------------------------------------------
# helper functions
def convertToTernary(compatibilityBool):
    if isinstance(compatibilityBool, bool):
        if compatibilityBool:
            return "YES"
        else:
            return "UNDEFINED"
    return compatibilityBool

def convertLicenseToTernary(license):
    for field in ['GPLv2Compat','GPLv3Compat']:
        if field in license:
            license[field] = convertToTernary(license[field])
        else:
            license[field] = "UNDEFINED"
    if 'issetBitfield' in license:
        del license['issetBitfield']
    return license

# -----------------------------------------------------------------------------
# migrate licenses
licenses_by_id_fun = '''function(doc){
    if (doc.type=="license"){
        emit(doc._id, doc)
    }
}'''

licenses = db.query(licenses_by_id_fun)

print 'On raw licenses: converting boolean compatibility flags to ternary.'
for license_row in licenses:
    db.save(convertLicenseToTernary(license_row.value))
print 'Done.'

# -----------------------------------------------------------------------------
# migrate moderations related to licenses
moderations_with_license_stuff_fun = '''function(doc){
    if (doc.type=="moderation" && (doc.licenseAdditions || doc.licenseDeletions)){
        emit(doc._id, doc)
    }
}'''

moderations_with_license_stuff = db.query(moderations_with_license_stuff_fun)

print 'In moderations: converting boolean compatibility flags to ternary.'
for moderation_row in moderations_with_license_stuff:
    moderation = moderation_row.value
    for field in ['licenseAdditions','licenseDeletions']:
        moderation[field] = map(convertLicenseToTernary, moderation[field])
    db.save(moderation)
print 'Done.'
