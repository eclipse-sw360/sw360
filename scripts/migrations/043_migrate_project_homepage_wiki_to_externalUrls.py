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
# This script is for migrating project's homepage and wiki to externalUrls. And removing those fields after migration.
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

# get all projects with field "homepage" or "wiki"
all_projects_with_homepage_or_wiki = {"selector": {"type": {"$eq": "project"}, "$or":[{"homepage": {"$exists": True}}, {"wiki": {"$exists": True}}]}, "limit": 20000}

# ---------------------------------------
# functions
# ---------------------------------------
    
def migrateAndDeleteHomepageWiki(logFile, docs):
    log = {}
    log['totalProjectsWitHomePageOrWiki'] = len(docs)
    log['Updated projects'] = []
    log['Projects to be updated(Dry run)'] = []
    for entity in docs:
        homepage = entity.get("homepage")
        wiki = entity.get("wiki");
        externalUrls = {}
        if homepage is not None:
            isEmptyHomepage = not (homepage and homepage.strip())
            if  not isEmptyHomepage:
                externalUrls["homepage"] = homepage
            del entity["homepage"]
        if wiki is not None:
            isEmptyWiki = not (wiki and wiki.strip())
            if not isEmptyWiki:
                externalUrls["wiki"] = wiki
            del entity["wiki"]

        if externalUrls:
            entity["externalUrls"] = externalUrls

        updateDocId_Dry_Run = {}
        updateDocId_Dry_Run['id'] = entity.get('_id')
        log['Projects to be updated(Dry run)'].append(updateDocId_Dry_Run)
        if not DRY_RUN:
            db.save(entity);
            updatedDocId = {}
            updatedDocId['id'] = entity.get('_id')
            log['Updated projects'].append(updatedDocId)

    
    json.dump(log, logFile, indent = 4, sort_keys = True)

def run():
    logFile = open('043_migrate_project_homepage_wiki_to_externalUrls.log', 'w')
    print 'Getting all projects with homepage or wiki'
    projects_with_homepage_or_wiki = db.find(all_projects_with_homepage_or_wiki);
    print 'found ' + str(len(projects_with_homepage_or_wiki)) + ' projects with homepage or wiki'
    migrateAndDeleteHomepageWiki(logFile, projects_with_homepage_or_wiki);
    logFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Please check log file "043_migrate_project_homepage_wiki_to_externalUrls.log" in this directory for details'
    print '------------------------------------------'

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'
