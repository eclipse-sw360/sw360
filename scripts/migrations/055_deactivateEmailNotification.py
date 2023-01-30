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
# This script is for deactivating email notification for user not belonging to a domain.
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
NOTIFICATION = 'wantsMailNotification'
EMAIL = 'email'

####################################################################################################
#all emailIds from the domains NOT included in the below list (separated with '|' ) will be disabled for email notification
DOMAINS = "@<domain1>.com|@<domain2>.org" #relpace the <domain1> and <domain2> with your domain names
####################################################################################################

# ----------------------------------------
# queries
# ----------------------------------------

# query to get list of emailIds/users that doest not belong in the above DOMAINS list
user_query = {
   "selector": {
      "type": {
         "$eq": "user"
      },
      "email": {
         "$not": {
            "$regex": DOMAINS
         }
      }
   },
   "limit": 99999
}

# ---------------------------------------
# functions
# ---------------------------------------

def deactivateEmailNotification(log, users_data_list):
    log["Updated User's mail notification"] =[]

    for user in users_data_list:
        if (user[NOTIFICATION] == True):
            user[NOTIFICATION] = False
            print (user[EMAIL])
            log["Updated User's mail notification"].append(user)

        if not DRY_RUN:
            userDb.save(user)

def run():
    log = {}
    logFile = open('055_deactivateEmailNotification.log', 'w')

    print ('Updated Users detail')
    print ('\n')
    
    users_data = userDb.find(user_query)
    users_data_list = list(users_data)
    print ('size of users not belonging to that instance')
    print (len(users_data_list))
    print ('email of users whose emailNotification field set to false')
    deactivateEmailNotification(log, users_data_list)
    
    print ('\n')

    json.dump(log, logFile, indent = 4, sort_keys = True)
    logFile.close()

    print ('\n')
    print ('------------------------------------------')
    print ('Please check log file "055_deactivateEmailNotification.log" in this directory for details')

# --------------------------------

startTime = time.time()
run()
print ('\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's')
