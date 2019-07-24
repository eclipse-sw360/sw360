#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
#
# All rights reserved.   This program and the accompanying materials
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
# initial author: christoph.niehoff@tngtech.com
#
# -----------------------------------------------------------------------------
import couchdb

COUCHSERVER  = "http://localhost:5984/"
DBNAME       = 'sw360db'
DBNAME_USERS = 'sw360users'

couch    = couchdb.Server(COUCHSERVER)
db       = couch[DBNAME]
db_users = couch[DBNAME_USERS]


get_all_users_fun = '''function(doc){
    emit(doc._id, doc);
}'''

get_all_moderation_requests_fun = '''function(doc){
    if (doc.type=="moderation"){
        emit(doc._id, doc);
    }
}
'''


print "Retrieve list of all users"
all_users = db_users.query(get_all_users_fun)
map_users_to_department = {}
for user in all_users:
    map_users_to_department[user.value['email']] = user.value['department']


print "Update missing submitter's department in moderation requests"
all_moderation_requests = db.query(get_all_moderation_requests_fun)
for request in all_moderation_requests:
    document = request.value
    if not 'requestingUserGroup' in document.keys():
        try:
            requestingUserMail = document['requestingUser']
            document['requestingUserDepartment'] = map_users_to_department[requestingUserMail]
            db.save(document)
        except KeyError:
            print "no requesting user in moderation request with id=" + request.id

print "done."

