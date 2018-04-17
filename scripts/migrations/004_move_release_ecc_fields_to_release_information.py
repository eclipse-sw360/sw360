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

ECC_INFORMATION = 'eccInformation'
CLEARING_INFORMATION = 'clearingInformation'

releases_with_clearing_info_fun = '''function(doc){
    if (doc.type=="release" && doc.clearingInformation){
        emit(doc._id, doc)
    }
}'''

releases_with_clearing_info = db.query(releases_with_clearing_info_fun)

print 'Moving release ecc fields to eccInformation'
for release_row in releases_with_clearing_info:
    release = release_row.value
    release[ECC_INFORMATION] = {}
    changed = False
    for field in ['AL', 'ECCN', 'assessorContactPerson', 'assessorDepartment', 'assessmentDate', 'eccComment', 'eccStatus', 'materialIndexNumber']:
        if field in release[CLEARING_INFORMATION]:
            release[ECC_INFORMATION][field] = release[CLEARING_INFORMATION][field]
            del release[CLEARING_INFORMATION][field]
            changed = True
    if changed:
        db.save(release)

print 'Done with releases.'
               
# --------------------------------
               
print 'Moving moderation release additions ecc fields to eccInformation'
 
moderations_with_release_additions_fun = '''function(doc){
    if (doc.type=="moderation" && doc.releaseAdditions && doc.releaseAdditions.clearingInformation){
        emit(doc._id, doc)
    }
}'''
 
moderations_with_release_additions = db.query(moderations_with_release_additions_fun)
 
for moderation_row in moderations_with_release_additions:
    moderation = moderation_row.value
    release_additions = moderation['releaseAdditions']
    release_additions[ECC_INFORMATION] = {}
    changed = False
    for field in ['AL', 'ECCN', 'assessorContactPerson', 'assessorDepartment', 'assessmentDate', 'eccComment', 'eccStatus', 'materialIndexNumber']:
        if field in release_additions[CLEARING_INFORMATION]:
            release_additions[ECC_INFORMATION][field] = release_additions[CLEARING_INFORMATION][field]
            del release_additions[CLEARING_INFORMATION][field]
            changed = True
    if changed:
        db.save(moderation)    
               
print 'Done with moderation release addition.'
 
# --------------------------------
 
print 'Moving moderation release deletions ecc fields to eccInformation'
 
moderations_with_release_deletions_fun = '''function(doc){
    if (doc.type=="moderation" && doc.releaseDeletions && doc.releaseDeletions.clearingInformation){
        emit(doc._id, doc)
    }
}'''
 
moderations_with_release_deletions = db.query(moderations_with_release_deletions_fun)
 
for moderation_row in moderations_with_release_deletions:
    moderation = moderation_row.value
    release_deletions = moderation['releaseDeletions']
    release_deletions[ECC_INFORMATION] = {}
    changed = False
    for field in ['AL', 'ECCN', 'assessorContactPerson', 'assessorDepartment', 'assessmentDate', 'eccComment', 'eccStatus', 'materialIndexNumber']:
        if field in release_deletions[CLEARING_INFORMATION]:
            release_deletions[ECC_INFORMATION][field] = release_deletions[CLEARING_INFORMATION][field]
            del release_deletions[CLEARING_INFORMATION][field]
            changed = True
    if changed:
        db.save(moderation)    
               
print 'Done with moderation release deletions.'
