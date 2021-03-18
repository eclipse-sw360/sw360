#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2021. Part of the SW360 Portal Project.
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
# This script gets all release iterates over the attachment and recomputes clearing state of release.
# -----------------------------------------------------------------------------

import time
import couchdb
import json
from webbrowser import get

# ---------------------------------------
# constants
# ---------------------------------------

DRY_RUN = True
COUCHUSERNAME = "*****"
COUCHPWD = "*****"
COUCHSERVER = "http://" + COUCHUSERNAME + ":" + COUCHPWD + "@localhost:5984/"
DBNAME = 'sw360db'

couch = couchdb.Server(COUCHSERVER)
db = couch[DBNAME]

# ----------------------------------------
# queries
# ----------------------------------------

# get all releases
all_releases_query = {"selector": {"type": {"$eq": "release"}},"limit": 200000}

# ---------------------------------------
# functions
# ---------------------------------------

def run():
    log = {}
    log['updatedReleases'] = []
    print 'Getting all releases'
    all_releases = db.find(all_releases_query)
    print 'Received ' + str(len(all_releases)) + ' releases'

    for release in all_releases:
        attachmentTypeMatched = False
        attachmentAccepted = False
        if release.get("attachments") is not None:
            for attachmentInfo in release.get("attachments"):
                if attachmentInfo.get("attachmentType") == "COMPONENT_LICENSE_INFO_XML" or attachmentInfo.get("attachmentType") == "CLEARING_REPORT":
                    attachmentTypeMatched = True
                    if attachmentInfo.get("checkStatus") == "ACCEPTED":
                        attachmentAccepted = True
                        break

        updatedReleaseLog = {}
        isClearingStateUpdated = False;
        if attachmentTypeMatched and attachmentAccepted:
            isClearingStateUpdated = setClearingStateIfDifferent(release, "APPROVED", updatedReleaseLog)
        elif attachmentTypeMatched:
            isClearingStateUpdated = setClearingStateIfDifferent(release, "REPORT_AVAILABLE", updatedReleaseLog)
        elif release.get('clearingState') != "SENT_TO_CLEARING_TOOL" and release.get('clearingState') != "UNDER_CLEARING":
            isClearingStateUpdated = setClearingStateIfDifferent(release, "NEW_CLEARING", updatedReleaseLog)

        if isClearingStateUpdated: 
            print '\tUpdating release ID -> ' + release.get('_id') + ', Release Name -> ' + release.get('name') + ', Release Version -> ' + release.get('version') + ', Old Clearing State -> ' + str(updatedReleaseLog['oldClearingState']) + ', New Clearing State -> ' + str(updatedReleaseLog['newClearingState'])
            updatedReleaseLog['id'] = release.get('_id')
            updatedReleaseLog['name'] = release.get('name')
            updatedReleaseLog['version'] = release.get('version')
            log['updatedReleases'].append(updatedReleaseLog)
            if not DRY_RUN:
                db.save(release)

    resultFile = open('004_recompute_clearing_state_of_release.log', 'w')
    json.dump(log, resultFile, indent = 4, sort_keys = True)
    resultFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Total Releases updated : ' + str(len(log['updatedReleases']))
    print '------------------------------------------'
    print 'Please check log file "004_recompute_clearing_state_of_release.log" in this directory for details'
    print '------------------------------------------'

# --------------------------------

def setClearingStateIfDifferent(release, correctClearingState, updatedReleaseLog):
    if release.get('clearingState') != correctClearingState:
        updatedReleaseLog['oldClearingState'] = release.get('clearingState')
        release["clearingState"] = correctClearingState
        updatedReleaseLog['newClearingState'] = release.get('clearingState')
        return True
    return False


startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'
