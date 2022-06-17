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
# This script removes "validForProject" field from Obligation
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

# ----------------------------------------
# queries
# ----------------------------------------

# get all obligation with field "validForProject"
all_obligation_with_validForProject = {"selector": {"type": {"$eq": "obligation"}, "validForProject": {"$exists": True}}, "limit": 20000}

# ---------------------------------------
# functions
# ---------------------------------------

def removeFieldName(resultFile, qryResult, fieldToBeRemoved):
    log = {}
    log['totalCount'] = len(qryResult)
    print 'Removing field name '+fieldToBeRemoved
    log['Updated obligation fields '+fieldToBeRemoved] = []
    for entity in qryResult:
        del entity[fieldToBeRemoved]
        if not DRY_RUN:
            db.save(entity)
            print 'Removing field name '+fieldToBeRemoved+' done for '+entity.get('_id')
        updatedDocId = {}
        updatedDocId['id'] = entity.get('_id')
        log['Updated obligation fields '+fieldToBeRemoved].append(updatedDocId)
    
    json.dump(log, resultFile, indent = 4, sort_keys = True)


def run():
    logFile = open('042_remove_validForProject_from_Obligation.log', 'w')
    print 'Getting all obligations with field "validForProject"'
    obligations_with_validForProject = db.find(all_obligation_with_validForProject)
    print 'found ' + str(len(obligations_with_validForProject)) + ' obligations in db!'
    removeFieldName(logFile, obligations_with_validForProject, "validForProject");
    logFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Please check log file "042_remove_validForProject_from_Obligation.log" in this directory for details'
    print '------------------------------------------'

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'
