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
# This script renames the field "name" to "title" in license obligation and delete field "obligationId"
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
oldFieldName = "name"
newFieldName = "title"
deleteFieldName = "obligationId"

# ----------------------------------------
# queries
# ----------------------------------------

# get all license obligation with field "name"
licenseObligation_with_name_query = {"selector": {"type": {"$eq": "licenseObligation"},oldFieldName: {"$exists": True}}}

# get all license obligation with field "obligationId"
licenseObligation_with_obligationId_query = {"selector": {"type": {"$eq": "licenseObligation"},deleteFieldName: {"$exists": True}}}

# ---------------------------------------
# functions
# ---------------------------------------

def updateFieldNames(qryResult, oldName, newName, log):
    print 'updating field name from '+oldName+' to '+newName
    log['updated licenseObligation fields from '+oldName+' to '+newName] = []
    for entity in qryResult:
        entity[''+newName+''] = entity[''+oldName+'']
        del entity[''+oldName+'']
        if not DRY_RUN:
            db.save(entity)
            print 'updation of field name from '+oldName+' to '+newName+' done'
        updatedDocId = {}
        updatedDocId['id'] = entity.get('_id')
        log['updated licenseObligation fields from '+oldName+' to '+newName].append(updatedDocId)

def removeFieldName(qryResult, fieldToBeRemoved, log):
    print 'Removing field name '+fieldToBeRemoved
    log['Updated licenseObligation fields '+fieldToBeRemoved] = []
    for entity in qryResult:
        del entity[''+fieldToBeRemoved+'']
        if not DRY_RUN:
            db.save(entity)
            print 'Removing field name '+fieldToBeRemoved+' done for '+entity.get('_id')
        updatedDocId = {}
        updatedDocId['id'] = entity.get('_id')
        log['Updated licenseObligation fields '+fieldToBeRemoved].append(updatedDocId)

def run():
    log = {}
    print 'Getting all licenseObligation with field name'
    licenseObligation_with_name = db.find(licenseObligation_with_name_query)
    print 'found ' + str(len(licenseObligation_with_name)) + ' licenseObligation with field name in db!'
    log['totalCount'] = len(licenseObligation_with_name)
    updateFieldNames(licenseObligation_with_name, oldFieldName, newFieldName, log);
    print 'Getting all licenseObligation with field obligationId'
    licenseObligation_with_obligationId = db.find(licenseObligation_with_obligationId_query)
    removeFieldName(licenseObligation_with_obligationId, deleteFieldName, log);


    resultFile = open('027_licenseObligation_migration_'+oldFieldName+'.log', 'w')
    json.dump(log, resultFile, indent = 4, sort_keys = True)
    resultFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Please check log file "027_licenseObligation_migration_'+oldFieldName+'.log" in this directory for details'
    print '------------------------------------------'

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'
