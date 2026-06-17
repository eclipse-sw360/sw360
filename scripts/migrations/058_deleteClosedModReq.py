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
# This script is for deleting all closed moderation requests based on date/timestamp.
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

couch = couchdb.Server(COUCHSERVER)
sw360Db = couch[SW360_DB]

MR_id = '_id'
modState = "PENDING|INPROGRESS"

print("****** This script will delete all closed moderation requests that were created before the INPUT DATE ****** \n")

print("****** Enter the date ******")
start_YYYY = int(input("Enter the Year: "))
start_MM = int(input("Enter the Month: "))
start_DD = int(input("Enter the Date: "))
inputDate = int(datetime.datetime(start_YYYY, start_MM, start_DD, 0, 0, 0).timestamp()*1000)

# ----------------------------------------
# queries
# ----------------------------------------

MR_query = {
   "selector": {
      "type": {
         "$eq": "moderation"
      },
      "moderationState": {
         "$not": {
            "$regex": modState
         }
      },
      "timestamp": {
         "$lte": inputDate
      }
   },
   "limit": 999999
}

# ---------------------------------------
# functions
# ---------------------------------------

def deleteClosedModerationRequests(log, MR_data_list):
    log["Closed Moderation Requests Ids"] =[]

    for MR in MR_data_list:
        log['Closed Moderation Requests Ids'].append(MR[MR_id])

        if not DRY_RUN:
            sw360Db.delete(MR)

def run():
    log = {}
    logFile = open('modRequest.log', 'w')
    
    print ('\n')
    print ('All closed moderation requests older than the mentioned date will get deleted \n')

    MR_data = sw360Db.find(MR_query)
    MR_data_list = list(MR_data)

    total = len(MR_data_list)
    print ('Size of closed moderation requests: ' + str(total))
    deleteClosedModerationRequests(log, MR_data_list)

    json.dump(log, logFile, indent = 4, sort_keys = True)
    logFile.close()

    print ('\n')
    print ('------------------------------------------')
    print ('Please check log file "modRequest.log" in this directory for details')

# --------------------------------

startTime = time.time()
run()
print ('\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's')
