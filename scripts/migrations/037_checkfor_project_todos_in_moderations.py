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
# This script checks if any project moderation requests contains todos with data and show a warning message to work on the same
# before proceeding with further migration scriptsor else delete the empty todos field from the moderation request.
# ---------------------------------------------------------------------------------------------------------------------------

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

couch = couchdb.Server(COUCHSERVER)
db = couch[DBNAME]

# ----------------------------------------
# queries
# ----------------------------------------

# get all project moderations with todos
all_project_moderations = {"selector": {"type": {"$eq": "moderation"},"documentType": {"$eq": "PROJECT"},"$or": [{"projectAdditions": {"todos":{"$exists":True}}},{"projectDeletions": {"todos": {"$exists": True}}}]},"limit": 20000}

# ---------------------------------------
# functions
# ---------------------------------------

def checkEmptyTodosAnddeleteFromModeration(logFile, all_moderations):
    log = {}
    count = {}
    count["total"] = str(len(all_moderations))
    log['projectModerationRequestsHavingEmptyTodos'] = []
    log['projectModerationRequestsHavingEmptyTodos'].append(count)
    todo = "todos"
    
    for moderation in all_moderations:

        projectAdditions = moderation.get("projectAdditions")
        projectDeletions = moderation.get("projectDeletions")

        todoslenghtinprojAddn = None
        todoslenghtinprojDeln = None

        if todo in projectAdditions:
            todoslenghtinprojAddn = len(projectAdditions["todos"])

        if todo in projectDeletions:
            todoslenghtinprojDeln = len(projectDeletions["todos"])


        if todoslenghtinprojAddn == 0 and todoslenghtinprojDeln == 0:
            del(moderation["projectAdditions"]["todos"])
            del(moderation["projectDeletions"]["todos"])
        elif todoslenghtinprojAddn == 0 and todoslenghtinprojDeln is None:
            del(moderation["projectAdditions"]["todos"])
        elif todoslenghtinprojAddn is None and todoslenghtinprojDeln == 0:
            del(moderation["projectDeletions"]["todos"])

        if not DRY_RUN:
            db.save(moderation)


        openModRequestList = {}
        openModRequestList['id'] = moderation.get('_id')
        log['projectModerationRequestsHavingEmptyTodos'].append(openModRequestList)

    json.dump(log, logFile, indent = 4, sort_keys = True)

def checkForFilledTodosInModeration(logFile, all_moderations):
    log = {}
    todo = "todos"
    log["moderation_requests_with_todos_having_data"] = []
    moderationList = []
    mods = {}
    for moderation in all_moderations:
        projectAdditions = moderation.get("projectAdditions")
        projectDeletions = moderation.get("projectDeletions")

        todoslenghtinprojAddn = None
        todoslenghtinprojDeln = None

        if todo in projectAdditions:
            todoslenghtinprojAddn = len(projectAdditions["todos"])

        if todo in projectDeletions:
            todoslenghtinprojDeln = len(projectDeletions["todos"])

        if (todoslenghtinprojAddn is not None and todoslenghtinprojAddn > 0) or (todoslenghtinprojDeln is not None and todoslenghtinprojDeln > 0):
            moderationList.append(moderation.get("_id"))

    if len(moderationList) > 0:
        for moderation in moderationList:
            mods["id"] = moderation

    log["moderation_requests_with_todos_having_data"].append(mods)
    json.dump(log, logFile, indent = 4, sort_keys = True)

    if len(moderationList) > 0:
        print "\nWarning!! Following moderation requests contains todos with data. Please work on them before proceeding further to execute other migration scripts or else the moderation view might break"
        print(str(moderationList)[1:-1])
        return True



def run():
    logFile = open('037_checkfor_project_todos_in_moderations.log', 'w')

    print 'Getting all project moderations with todos'
    all_moderations = db.find(all_project_moderations)
    print 'found ' + str(len(all_moderations)) + ' project moderations with todos in db!'

    print 'Checking if there is any moderation request having todos with data'
    isExists = checkForFilledTodosInModeration(logFile, all_moderations)

    if isExists is None:
        print 'Found zero moderation request having todos with data'
        checkEmptyTodosAnddeleteFromModeration(logFile, all_moderations)

    logFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Please check log file "037_checkfor_project_todos_in_moderations.log" in this directory for details'
    print '------------------------------------------'

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'
