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
# This script renames the field "linkedObligations" to "linkedObligationStatus" in ProjectObligation
# --------------------------------------------------------------------------------------------------

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
oldFieldName = "linkedObligations"
newFieldName = "linkedObligationStatus"

# ----------------------------------------
# queries
# ----------------------------------------

# get all ProjectObligations with field "linkedObligations"
projectObligations_with_linkedObligations_query = {"selector": {"type": {"$eq": "projectObligation"},oldFieldName: {"$exists": True}}}

# ---------------------------------------
# functions
# ---------------------------------------

def updateFieldNames(qryResult, oldName, newName, log):
    print 'updating field name from '+oldName+' to '+newName
    log['updated ProjectObligations fields from '+oldName+' to '+newName] = []
    for entity in qryResult:
        entity[''+newName+''] = entity[''+oldName+'']
        del entity[''+oldName+'']
        if not DRY_RUN:
            db.save(entity)
        updatedDocId = {}
        updatedDocId['id'] = entity.get('_id')
        log['updated ProjectObligations fields from '+oldName+' to '+newName].append(updatedDocId)
    print 'updation of field name from '+oldName+' to '+newName+' done'

def run():
    log = {}
    print 'Getting all ProjectObligations with field linkedObligations'
    projectObligations_with_linkedObligations = db.find(projectObligations_with_linkedObligations_query)
    print 'found ' + str(len(projectObligations_with_linkedObligations)) + ' ProjectObligations with field linkedObligations in db!'
    log['totalCount'] = len(projectObligations_with_linkedObligations)
    updateFieldNames(projectObligations_with_linkedObligations, oldFieldName, newFieldName, log);

    resultFile = open('032_projectObligations_migration_'+oldFieldName+'.log', 'w')
    json.dump(log, resultFile, indent = 4, sort_keys = True)
    resultFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Please check log file "032_projectObligations_migration_'+oldFieldName+'.log" in this directory for details'
    print '------------------------------------------'

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'
