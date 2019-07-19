#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2018. Part of the SW360 Portal Project.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# This is a manual database migration script. It is assumed that a
# dedicated framework for automatic migration will be written in the
# future. When that happens, this script should be refactored to conform
# to the framework's prerequisites to be run by the framework. For
# example, server address and db name should be parametrized, the code
# reorganized into a single class or function, etc.
#
# The usage of this script is optional.
# It is intended to overwrite the release name with the parent component name.
# As default the script runs in a dry mode and doesnt update releases.
#
# initial author: thomas.maier@evosoft.com
#
# -----------------------------------------------------------------------------
import couchdb

DRY_RUN = True

COUCHSERVER = "http://localhost:5984/"
DBNAME = 'sw360db'
NAME = 'name'
COMPONENTID = 'componentId'
ID = "_id"

couch = couchdb.Server(COUCHSERVER)
db = couch[DBNAME]

get_all_components_fun = '''function(doc){
        if (doc.type=='component') {
            emit(doc._id, doc)
        }
    }'''

get_all_releases_fun = '''function(doc){
        if (doc.type=='release') {
            emit(doc._id, doc)
        }
    }'''

print 'Retrieve all components'
components = db.query(get_all_components_fun)
print 'Retrieve all releases'
releases = db.query(get_all_releases_fun)

componentNamesById = {}
for component in components:
    docComponent = component.value
    componentNamesById[component.id] = docComponent[NAME]

for release in releases:
    docRelease = release.value
    releaseId = docRelease[ID]
    releaseName = docRelease[NAME]
    componentId = docRelease[COMPONENTID]
    if componentId in componentNamesById:
        if releaseName != componentNamesById[componentId]:
            print (u'INFO: releaseId:%s, old name:"%s", new name: "%s"' % (
                releaseId, releaseName, componentNamesById[componentId])).encode('utf-8')
            if DRY_RUN:
                print 'INFO: not saving release - DRY_RUN\n'
            else:
                docRelease[NAME] = componentNamesById[componentId]
                db.save(docRelease)
                print 'INFO: saved release\n'
    else:
        print 'WARN: document for componentId ' + componentId + ' not found\n'
