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
# This script is for removing the trailing and leading whitespaces in component's name
# ---------------------------------------------------------------------------------------------------------------------------------------------------------------

from logging import exception
import couchdb
import pandas as pd
import json
import time

#Constants
DRY_RUN = True

COUCHSERVER = "http://localhost:5984/"
DBNAME = 'sw360db'

def dbConnection():
    print('Connecting to local couchdb stage server....../')
    couch = couchdb.Server(COUCHSERVER)
    return couch[DBNAME]

def queryExecution(db):
    print('Executing the query.................../')
    db_query = db.find({"selector":{"type": "component","name": {"$regex": "(^\\s.+)|(.+\\s$)"}},"limit":999999999999999})
    if bool(db_query):
        return list(db_query)
    else:
        return []

def updateDB(db,db_copy_list):
    if not DRY_RUN:
        print('Correcting the component names and updating the database......../')

        df = pd.DataFrame(db_copy_list)
        df['name'] =  df['name'].str.strip()   
        df = df.fillna('')    
        db_df_list = df.to_dict('records')

        #Update call
        db.update(db_df_list)

def generateLogFile(db_copy_list):
    print('Generating log file......../')
    logFile = open('_050_remove_whitespace_component_name.log', 'w')
    json.dump(db_copy_list, logFile, indent = 4, sort_keys = True)
    logFile.close()

def run():
 
    #DB connection
    db = dbConnection()
    
    #Query to fetch the component names
    db_copy_list = queryExecution(db)

    #Generate log file
    generateLogFile(db_copy_list)

    #Converting document data to dataframe and removing the whitespaces
    
    if len(db_copy_list)!=0:
        updateDB(db,db_copy_list)
    else:
        print('No Records found!')
    
    print('Execution completed....')

if __name__ == '__main__':
    try:
        start_time = time.time()
        run()
        print('\nExecution time: ' + "{0:.2f}".format(time.time() - start_time) + 's')

    except Exception as e:
        print('Exception message ',e)



