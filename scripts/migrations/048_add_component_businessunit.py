#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Toshiba corporation, 2021. Part of the SW360 Portal Project.
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
# example, server address and db name should be parametrized, the code
# reorganized into a single class or function, etc.
#
# This script adds a business unit field to all components and set the creator organization.
# -----------------------------------------------------------------------------

import couchdb
import json
import time

# ---------------------------------------
# constants
# ---------------------------------------
DRY_RUN = True

COUCHSERVER = "http://localhost:5984/"
DBNAME = 'sw360db'
DBNAME_USERS = 'sw360users'

# set admin name and password for couchdb3
DB_USER_NAME = ''
DB_USER_PASSWORD = ''

DEFAULT_VISIBILITY = 'EVERYONE'
DEFAULT_BUSINESS_UNIT = ''

couch = couchdb.Server(COUCHSERVER)
# set credentials for couchdb3
if DB_USER_NAME:
    couch.resource.credentials=(DB_USER_NAME, DB_USER_PASSWORD)

db = couch[DBNAME]
db_users = couch[DBNAME_USERS]

# ----------------------------------------
# queries
# ----------------------------------------

# get all components
all_components = {"selector": {"type": {"$eq": "component"}}, "limit": 100000}
# get all users
all_users = {"selector": {"type": {"$eq": "user"}}, "limit": 10000}

# ---------------------------------------
# functions
# ---------------------------------------

def run():
    log = {}

    print('Add a business unit field to the component and set the creator organization.')
    # create a user primary department map
    user_departments = {}
    users = db_users.find(all_users)
    for user in users:
        email = user['email']
        department = user['department']
        if email:
            user_departments[email] = department
        else:
            print('Failed to get a user email. _id=' + user['_id'])
    
    # migrate all components
    components = db.find(all_components)
    component_len = len(components)
    print('Found ' + str(component_len) + ' components in db!')

    component_log = {}
    component_log['totalCount'] = component_len
    component_log['addFieldsList'] = []

    for component in components:
        addFieldsList = {}
        addFieldsList['id'] = component['_id']
        addFieldsList['name'] = component['name']

        # set default component visibility.
        if not 'visbility' in component:
            component['visbility'] = DEFAULT_VISIBILITY
            addFieldsList['visibility'] = component['visbility']

        # set the creator organization.
        if not 'businessUnit' in component:
            creator = component.get('createdBy')
            if creator in user_departments:
                department = user_departments[creator]
                component['businessUnit'] = department
            else:
                component['businessUnit'] = DEFAULT_BUSINESS_UNIT
                addFieldsList['warn'] = 'Failed to get ' + str(creator) + ' department.'

            addFieldsList['businessUnit'] = component['businessUnit']

        if not DRY_RUN:
            db.save(component)
        
        component_log['addFieldsList'].append(addFieldsList)


    log['components'] = component_log

    logFile = open('048_add_component_businessunit.log', 'w')
    json.dump(log, logFile, indent = 4, sort_keys = True)
    logFile.close()

    print ('\n')
    print ('------------------------------------------')
    print ('Please check log file "048_add_component_businessunit.log" in this directory for details')
    print ('------------------------------------------')

# --------------------------------


startTime = time.time()
run()
print('\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's')
