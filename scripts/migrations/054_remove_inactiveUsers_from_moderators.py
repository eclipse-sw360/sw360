#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2022. Part of the SW360 Portal Project.
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
# This script is for removing deactivated users from moderators list.
# ---------------------------------------------------------------------------------------------------------------------------------------------------------------

import time
import datetime
import couchdb
import json
from webbrowser import get

# ---------------------------------------
# constants
# ---------------------------------------

DRY_RUN = True

COUCHSERVER = 'http://username:pwd@localhost:5984/'
SW360_DB = 'sw360db'
USERS_DB = 'sw360users'

couch = couchdb.Server(COUCHSERVER)
sw360db = couch[SW360_DB]
userDb = couch[USERS_DB]

MODS = "moderators"
EMAIL = "email"
BOOL_TRUE = True
ID = "_id"
STATE = "moderationState"

# ----------------------------------------
# queries
# ----------------------------------------

# get all the deactivated users
inactive_users_query = {"selector": {"type": {"$eq": "user"},"deactivated": {"$eq": BOOL_TRUE}},"limit": 99999}

# get all the moderation requests
mr_query = {"selector": {"type": "moderation"}, "limit": 999999}


# ---------------------------------------
# functions
# ---------------------------------------

#Remove_InactiveUsers_From_Moderators
def activeUsersInModerators(log, inactive_users, moderation_list):
    log["Inactive Users email id in Moderators with moderation id"] =[]
    user_list = list(inactive_users)

    for moderation in moderation_list:
        for user in user_list:
            for iteration, item in enumerate(moderation[MODS]):
                if (item == user[EMAIL]):
                    moderation[MODS].remove(item)
                    print (moderation[ID])
                    print (moderation[STATE])
                    log['Inactive Users email id in Moderators with moderation id'].append(moderation[ID])
                    log['Inactive Users email id in Moderators with moderation id'].append(item)

        if not DRY_RUN:
            sw360db.save(moderation)

def run():
    log = {}
    logFile = open('054_moderationReq.log', 'w')

    print ('Updated Users detail in log file:')
    print ('\n')
    inactive_users = userDb.find(inactive_users_query)
    mod_requests = sw360db.find(mr_query)
    moderation_list = list(mod_requests)

    activeUsersInModerators(log, inactive_users, moderation_list)
    print ('\n')

    json.dump(log, logFile, indent = 4, sort_keys = True)
    logFile.close()

    print ('\n')
    print ('------------------------------------------')
    print ('Please check log file "054_moderationReq.log" in this directory for details')

# --------------------------------

startTime = time.time()
run()
print ('\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's')
