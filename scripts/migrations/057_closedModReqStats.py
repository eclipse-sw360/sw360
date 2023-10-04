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
# This script is for collecting closed moderation requests statistics.
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

modState = "PENDING|INPROGRESS"
state = "moderationState"
timestamp = "timestampOfDecision"

print("This script will collect closed moderation requests statistics that falls between startDate and endDate \n")
print("************ NOTE: START_DATE should be less than the END_DATE ************ \n")

print("****** Enter the start date ******")
start_YYYY = int(input("Enter the Year: "))
start_MM = int(input("Enter the Month: "))
start_DD = int(input("Enter the Date: "))

print("****** Enter the end date ******")
end_YYYY = int(input("Enter the Year: "))
end_MM = int(input("Enter the Month: "))
end_DD = int(input("Enter the Date: "))

startDate = int(datetime.datetime(start_YYYY, start_MM, start_DD, 0, 0, 0).timestamp()*1000)
endDate = int(datetime.datetime(end_YYYY, end_MM, end_DD, 0, 0, 0).timestamp()*1000)

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
         "$gte": startDate,
         "$lte": endDate
      }
   },
    "fields": [
      "_id",
      "requestingUser",
      "requestingUserDepartment",
      "reviewer",
      "timestampOfDecision",
      "moderationState"  
    ],
   "limit": 999999
}

# ---------------------------------------
# functions
# ---------------------------------------

def closedModerationRequestsStatistics(log, MR_data_list):
    log["Closed Moderation Requests Statistics"] =[]
    log["Closed Moderation Requests TOTAL/APPROVED/REJECTED"] =[]
    rejectedCount = 0
    approvedCount = 0

    for MR in MR_data_list:
        epoch_time = MR[timestamp]/1000
        date_time = datetime.datetime.fromtimestamp(epoch_time)
        MR[timestamp] = str(date_time)
        log['Closed Moderation Requests Statistics'].append(MR)
        if (MR[state] == "APPROVED"):
            approvedCount = approvedCount + 1
        else:
            rejectedCount = rejectedCount + 1

    log["Closed Moderation Requests TOTAL/APPROVED/REJECTED"].append(len(MR_data_list))
    log["Closed Moderation Requests TOTAL/APPROVED/REJECTED"].append(approvedCount)
    log["Closed Moderation Requests TOTAL/APPROVED/REJECTED"].append(rejectedCount)

    print("Approved MRs " + str(approvedCount))
    print("Rejected MRs " + str(rejectedCount))

def run():
    log = {}
    logFile = open('modRequestStats2.log', 'w')
    
    print ('\n')
    MR_data = sw360Db.find(MR_query)
    MR_data_list = list(MR_data)

    print ('****** Statistics of closed moderation requests ******')
    total = len(MR_data_list)
    print ("Total Closed MRs: " + str(total))
    closedModerationRequestsStatistics(log, MR_data_list)

    json.dump(log, logFile, indent = 4, sort_keys = True)
    logFile.close()

    print ('\n')
    print ('------------------------------------------')
    print ('Please check log file "modRequestStats.log" in this directory for details')

# --------------------------------

startTime = time.time()
run()
print ('\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's')
