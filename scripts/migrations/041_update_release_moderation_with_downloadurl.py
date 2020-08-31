#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
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
# example, server address and db name should be parameterized, the code
# reorganized into a single class or function, etc.
#
# This script renames the field "obligationType" to "obligationLevel" in Obligations
# ---------------------------------------------------------------------------------------------------------------------------------------------------------------

import time
import couchdb
import json
from webbrowser import get

# ---------------------------------------
# constants
# ---------------------------------------

DRY_RUN = True

COUCHSERVER = "http://localhost:5984/"
DBNAME = 'sw360db'

couch=couchdb.Server(COUCHSERVER)
db = couch[DBNAME]

# set fieldName
oldFieldName = "downloadurl"
newFieldName = "sourceCodeDownloadurl"

# ----------------------------------------
# queries
# ----------------------------------------

# get all release moderation with field "downloadurl"
release_moderation_with_downloadurl_query = {"selector": {"type": {"$eq": "moderation"},"documentType": {"$eq": "RELEASE"},"$or": [{"releaseAdditions": {oldFieldName:{"$exists":True}}},{"releaseDeletions": {oldFieldName: {"$exists": True}}}]},"limit": 20000}

# ---------------------------------------
# functions
# ---------------------------------------

def updateFieldNames(qryResult, oldName, newName, log):
    print 'updating field name from '+oldName+' to '+newName
    log['updated release moderation fields from '+oldName+' to '+newName] = []
    
    for entity in qryResult:
        if oldName in entity["releaseAdditions"]:
            entity["releaseAdditions"][newName] = entity["releaseAdditions"][oldName]
            del entity["releaseAdditions"][oldName]
        if oldName in entity["releaseDeletions"]:
            entity["releaseDeletions"][newName] = entity["releaseDeletions"][oldName]
            del entity["releaseDeletions"][oldName]
        if not DRY_RUN:
            db.save(entity)
        updatedDocId = {}
        updatedDocId['id'] = entity.get('_id')
        log['updated release moderation fields from '+oldName+' to '+newName].append(updatedDocId)
    print 'updation of field name from '+oldName+' to '+newName+' done'

def run():
    log = {}
    print 'Getting all release moderation with field downloadurl'
    release_moderation_with_downloadurl = db.find(release_moderation_with_downloadurl_query)
    print 'found ' + str(len(release_moderation_with_downloadurl)) + ' release moderation with field downloadurl in db!'
    log['totalCount'] = len(release_moderation_with_downloadurl)
    updateFieldNames(release_moderation_with_downloadurl, oldFieldName, newFieldName, log);

    resultFile = open('041_update_release_moderation_with_'+oldFieldName+'.log', 'w')
    json.dump(log, resultFile, indent = 4, sort_keys = True)
    resultFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Please check log file "041_update_release_moderation_with_'+oldFieldName+'.log" in this directory for details'
    print '------------------------------------------'

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'
