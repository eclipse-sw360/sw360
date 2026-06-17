#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens Healthineers, 2022. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# This script is for removing Moderation requests of a particular user.
# ---------------------------------------------------------------------------------------------------------------------------------------------------------------

import time
import couchdb
import json
from webbrowser import get

# ---------------------------------------
# constants
# ---------------------------------------

DRY_RUN = True

COUCHSERVER = "http://user:pwd@localhost:5984/"
DBNAME = 'sw360db'

couch = couchdb.Server(COUCHSERVER)
db = couch[DBNAME]

EMAIL = "user_email"
MODERATION_STATE = ""

# ----------------------------------------
# queries
# ----------------------------------------

# remove the Moderation Requests created by a particular user
all_Moderation_Requests = {
    "selector": {
        "type": {"$eq": "moderation"},
        "requestingUser": {"$eq": EMAIL},
    },
    "limit": 99999
}

if MODERATION_STATE:
    all_Moderation_Requests["selector"]["moderationState"] = {"$eq": MODERATION_STATE}

# ---------------------------------------
# functions
# ---------------------------------------
def removeModerationRequests(logFile, docs):
    log = {}
    log['total MR'] = len(list(docs))
    log['MR'] = []
    log['MR(Dry run)'] = []
    for mr in docs:
        print ("MR Id: " + mr.get("_id"))
        if DRY_RUN:
            deleteMR_Dry_Run = {}
            deleteMR_Dry_Run['MRid'] = mr.get('_id')
            log['MR(Dry run)'].append(deleteMR_Dry_Run)
        if not DRY_RUN:
            db.delete(mr)
            deleteMR = {}
            deleteMR['id'] = mr.get('_id')
            log['MR'].append(deleteMR)

    json.dump(log, logFile, indent = 4, sort_keys = True)


def run():
    logFile = open('RemoveMR.log', 'w')
    print ('Getting all the Moderation Requests')
    Moderation_Requests = list(db.find(all_Moderation_Requests))
    print ('found ' + str(len(Moderation_Requests)) + ' Moderation Requests\n')
    removeModerationRequests(logFile, Moderation_Requests)
    logFile.close()
    print ('------------------------------------------')
    print ('Please check log file "RemoveMR.log" in this directory for details')
    print ('------------------------------------------')

# --------------------------------

startTime = time.time()
run()
print ('\nTime of deletion: ' + "{0:.2f}".format(time.time() - startTime) + 's')
