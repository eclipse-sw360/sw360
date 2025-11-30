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
# This script is for removing duplicate AL and ECCN field in eccInformation field of Release & making the field names to lower case
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

ECC_INFORMATION = 'eccInformation'
AL_IN_UPPERCASE = 'AL'
AL_IN_LOWERCASE = 'al'
ECCN_IN_UPPER_CASE = 'ECCN'
ECCN_IN_LOWER_CASE = 'eccn'
RELEASE_ADDITIONS = 'releaseAdditions'
RELEASE_DELETIONS = 'releaseDeletions'
COMPONENT_ADDITIONS = 'componentAdditions'
COMPONENT_DELETIONS = 'componentDeletions'

# ----------------------------------------
# queries
# ----------------------------------------

# get all releases having ecc information

all_releases_with_eccinformation = {"selector": {"type": {"$eq": "release"}, "eccInformation": {"$exists": True}}, "limit": 99999}

# get all release moderation requests having ecc information

all_release_moderations_with_eccinfo = {"selector": {"type": {"$eq": "moderation"},"documentType": {"$eq": "RELEASE"},"$or": [{"releaseAdditions": {"eccInformation":{"$exists":True}}},{"releaseDeletions": {"eccInformation": {"$exists": True}}}]},"limit": 99999}

all_component_moderations_with_eccinfo = {"selector": {"type": {"$eq": "moderation"},"documentType": {"$eq": "COMPONENT"},"$or": [{"componentAdditions": {"releases": {"$exists": True,"$elemMatch": {"eccInformation": {"$exists": True}}}}},{"componentDeletions": {"releases": {"$exists": True,"$elemMatch": {"eccInformation": {"$exists": True}}}}}]},"limit": 99999}

# ----------------------------------------
# functions
# ----------------------------------------

def cleanup_eccinfo_in_release(log, entities):
    log['Updated releases'] = []
    log['Releases not updated(with Error)'] = []

    for entity in entities:
        entity_ecc_info = ''
        if ECC_INFORMATION in entity:
            entity_ecc_info = entity[ECC_INFORMATION]
        updated_release_Id = {}
        release_with_error = {}

        updated_release_moderation_Id = {}
        release_moderation_with_error = {}

        if AL_IN_UPPERCASE in entity_ecc_info:
            if AL_IN_LOWERCASE in entity_ecc_info:
                if entity_ecc_info.get(AL_IN_UPPERCASE) == entity_ecc_info.get(AL_IN_LOWERCASE):
                    del entity[ECC_INFORMATION][AL_IN_UPPERCASE]
                    updated_release_Id['id'] = entity['_id']
                else:
                    print ("Warning!! Value of AL and al does not match for the release" + entity['_id'])
                    release_with_error['id'] = entity['_id']
            else:
                entity[ECC_INFORMATION][AL_IN_LOWERCASE] = entity[ECC_INFORMATION][AL_IN_UPPERCASE]
                del entity[ECC_INFORMATION][AL_IN_UPPERCASE]
                updated_release_Id['id'] = entity['_id']

        if ECCN_IN_UPPER_CASE in entity_ecc_info:
            if ECCN_IN_LOWER_CASE in entity_ecc_info:
                if entity_ecc_info.get(ECCN_IN_UPPER_CASE) == entity_ecc_info.get(ECCN_IN_LOWER_CASE):
                    del entity[ECC_INFORMATION][ECCN_IN_UPPER_CASE]
                    updated_release_Id['id'] = entity['_id']
                else:
                    print ("Warning!! Value of ECCN and eccn does not match for the release" + entity['_id'])
                    release_with_error['id'] = entity['_id']
            else:
                entity[ECC_INFORMATION][ECCN_IN_LOWER_CASE] = entity[ECC_INFORMATION][ECCN_IN_UPPER_CASE]
                del entity[ECC_INFORMATION][ECCN_IN_UPPER_CASE]
                updated_release_Id['id'] = entity['_id']

        log['Updated releases'].append(updated_release_Id)
        log['Releases not updated(with Error)'].append(release_with_error)

        if not DRY_RUN:
            db.save(entity)

def cleanup_eccinfo_in_release_moderation(log, entities):
    log['Updated release moderations'] = []
    log['Release moderations not updated(with Error)'] = []

    options = [RELEASE_ADDITIONS, RELEASE_DELETIONS]
    for entity in entities:
        for option in options:
            entity_ecc_info = ''
            if ECC_INFORMATION in entity[option]:
                entity_ecc_info = entity[option][ECC_INFORMATION]

            updated_release_moderation_Id = {}
            release_moderation_with_error = {}

            if AL_IN_UPPERCASE in entity_ecc_info:
                if AL_IN_LOWERCASE in entity_ecc_info:
                    if entity_ecc_info.get(AL_IN_UPPERCASE) == entity_ecc_info.get(AL_IN_LOWERCASE):
                        del entity[option][ECC_INFORMATION][AL_IN_UPPERCASE]
                        updated_release_moderation_Id['id'] = entity['_id']
                    else:
                        print ("Warning!! Value of AL and al does not match for the release" + entity['_id'])
                        release_moderation_with_error['id'] = entity['_id']
                else:
                    entity[option][ECC_INFORMATION][AL_IN_LOWERCASE] = entity[option][ECC_INFORMATION][AL_IN_UPPERCASE]
                    del entity[option][ECC_INFORMATION][AL_IN_UPPERCASE]
                    updated_release_moderation_Id['id'] = entity['_id']

            if ECCN_IN_UPPER_CASE in entity_ecc_info:
                if ECCN_IN_LOWER_CASE in entity_ecc_info:
                    if entity_ecc_info.get(ECCN_IN_UPPER_CASE) == entity_ecc_info.get(ECCN_IN_LOWER_CASE):
                        del entity[option][ECC_INFORMATION][ECCN_IN_UPPER_CASE]
                        updated_release_moderation_Id['id'] = entity['_id']
                    else:
                        print ("Warning!! Value of ECCN and eccn does not match for the release" + entity['_id'])
                        release_moderation_with_error['id'] = entity['_id']
                else:
                    entity[option][ECC_INFORMATION][ECCN_IN_LOWER_CASE] = entity[option][ECC_INFORMATION][ECCN_IN_UPPER_CASE]
                    del entity[option][ECC_INFORMATION][ECCN_IN_UPPER_CASE]
                    updated_release_moderation_Id['id'] = entity['_id']

            log['Updated release moderations'].append(updated_release_moderation_Id)
            log['Release moderations not updated(with Error)'].append(release_moderation_with_error);

        if not DRY_RUN:
            db.save(entity)

def cleanup_eccinfo_in_component_moderation(log, entities):
    log['Updated component moderations'] = []
    log['Component moderations not updated(with Error)'] = []

    options = [COMPONENT_ADDITIONS, COMPONENT_DELETIONS]
    for component_mod in entities:
        for option in options:
            if 'releases' in component_mod[option]:
                for entity in component_mod[option]['releases']:
                    entity_ecc_info = ''
                    if ECC_INFORMATION in entity:
                        entity_ecc_info = entity[ECC_INFORMATION]

                    updated_release_moderation_Id = {}
                    release_moderation_with_error = {}

                    if AL_IN_UPPERCASE in entity_ecc_info:
                        if AL_IN_LOWERCASE in entity_ecc_info:
                            if entity_ecc_info.get(AL_IN_UPPERCASE) == entity_ecc_info.get(AL_IN_LOWERCASE):
                                del entity[ECC_INFORMATION][AL_IN_UPPERCASE]
                                updated_release_moderation_Id['id'] = component_mod['_id']
                            else:
                                print ("Warning!! Value of AL and al does not match for the release" + entity['_id'])
                                release_moderation_with_error['id'] = component_mod['_id']
                        else:
                            entity[ECC_INFORMATION][AL_IN_LOWERCASE] = entity[ECC_INFORMATION][AL_IN_UPPERCASE]
                            del entity[ECC_INFORMATION][AL_IN_UPPERCASE]
                            updated_release_moderation_Id['id'] = component_mod['_id']

                    if ECCN_IN_UPPER_CASE in entity_ecc_info:
                        if ECCN_IN_LOWER_CASE in entity_ecc_info:
                            if entity_ecc_info.get(ECCN_IN_UPPER_CASE) == entity_ecc_info.get(ECCN_IN_LOWER_CASE):
                                del entity[ECC_INFORMATION][ECCN_IN_UPPER_CASE]
                                updated_release_moderation_Id['id'] = component_mod['_id']
                            else:
                                print ("Warning!! Value of ECCN and eccn does not match for the release" + entity['_id'])
                                release_moderation_with_error['id'] = component_mod['_id']
                        else:
                            entity[ECC_INFORMATION][ECCN_IN_LOWER_CASE] = entity[ECC_INFORMATION][ECCN_IN_UPPER_CASE]
                            del entity[ECC_INFORMATION][ECCN_IN_UPPER_CASE]
                            updated_release_moderation_Id['id'] = component_mod['_id']

                    log['Updated component moderations'].append(updated_release_moderation_Id)
                    log['Component moderations not updated(with Error)'].append(release_moderation_with_error);

        if not DRY_RUN:
            db.save(component_mod)

def run():
    log = {}
    logFile = open('050_cleanup_eccinformation_duplicate_attributes.log', 'w')

    print ('Getting all the releases with field ' + ECC_INFORMATION)
    releases_with_eccinfo_field = db.find(all_releases_with_eccinformation)
    cleanup_eccinfo_in_release(log, releases_with_eccinfo_field)

    print ('Getting all release moderation requests with field ' + ECC_INFORMATION)
    release_moderations_with_ecc_info = db.find(all_release_moderations_with_eccinfo)
    cleanup_eccinfo_in_release_moderation(log, release_moderations_with_ecc_info )

    print ('Getting all component moderation requests with field ' + ECC_INFORMATION)
    component_moderations_with_ecc_info = db.find(all_component_moderations_with_eccinfo)
    cleanup_eccinfo_in_component_moderation(log, component_moderations_with_ecc_info)

    json.dump(log, logFile, indent = 4, sort_keys = True)
    logFile.close()

    print ('\n')
    print ('------------------------------------------')
    print ('Please check log file "050_cleanup_eccinformation_duplicate_attributes.log" in this directory for details')
    print ('------------------------------------------')

# --------------------------------

startTime = time.time()
run()
print ('\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's')
