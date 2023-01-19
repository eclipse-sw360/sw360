#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2023. Part of the SW360 Portal Project.
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
# This script is for removing null from user's lastname.
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

COUCHSERVER = 'http://username:password@localhost:5984/'
USERS_DB = 'sw360users'

couch = couchdb.Server(COUCHSERVER)
userDb = couch[USERS_DB]

#Users
LASTNAME = 'lastname'
FULLNAME = 'fullname'
NULL = '(null)'
DOT = '.'

# ----------------------------------------
# queries
# ----------------------------------------

# get all the users with empty lastname
user_query = {"selector": {"type": {"$eq": "user"},"lastname": {"$eq": "(null)"}},"limit": 99999}

# ---------------------------------------
# functions
# ---------------------------------------

#UsersData
def usersWithLastnameNull(log,users_data_list):
    log["Updated User's Lastname"] =[]

    for user in users_data_list:
        user[LASTNAME] = "."
        user[FULLNAME] = user[FULLNAME].replace(NULL, DOT)
        print (user[FULLNAME])
        log["Updated User's Lastname"].append(user)

        if not DRY_RUN:
            userDb.save(user)

def run():
    log = {}
    logFile = open('055_updated_users_lastname.log', 'w')

    print ('Updated Users detail')
    print ('\n')
    users_data = userDb.find(user_query)
    users_data_list = list(users_data)
    usersWithLastnameNull(log,users_data_list)
    print ('\n')

    json.dump(log, logFile, indent = 4, sort_keys = True)
    logFile.close()

    print ('\n')
    print ('------------------------------------------')
    print ('Please check log file "055_updated_users_lastname.log" in this directory for details')

# --------------------------------

startTime = time.time()
run()
print ('\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's')
