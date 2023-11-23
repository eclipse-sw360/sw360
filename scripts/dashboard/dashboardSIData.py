#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# This script is for fetching all the data for global dashboard.
# ---------------------------------------------------------------------------------------------------------------------------------------------------------------
import couchdb
import pandas as pd
import json
import time
import csv
import re
from collections import defaultdict
from datetime import datetime
from collections import Counter
import os

#Constants
DRY_RUN = True
script_folder = os.path.dirname(os.path.abspath(__file__))
base_folder = os.path.basename(os.path.dirname(os.path.dirname(script_folder)))
# Generate the output folder path
output_folder = f"/{base_folder}/frontend/sw360-portlet/src/main/resources/META-INF/resources/html/dashboard/data/SI"

max_retries=3
retry_delay=2

def dbConnection(db):
    print('\nConnecting to local couchdb stage server....../')
    couch = couchdb.Server("http://localhost:5984/")
    return couch[db]

#--------Common methods---------------
def fetchResults(db, design_doc, view_name):
        url = f"{design_doc}/_view/{view_name}"
        result = []
        retries = 0
        while True:
            try:
                result = db.view(url, timeout=1000)
                break
            except couchdb.http.ServerError as e:
                if 'timeout' in str(e):
                    print(f"View execution timed out (retrying, attempt {retries + 1}/{max_retries}).")
                    retries += 1
                    if retries > max_retries:
                        print("Max retries reached. Unable to execute the view.")
                        return None
                    time.sleep(retry_delay)
                else:
                    print(f"Server error: {e}")
        return result

def saveNewView(db, design_doc, view, map_function):
     while True:
       try:
          
          design_docs = db[design_doc]
          design_docs['views'][view] = map_function
          if not DRY_RUN:
            db.save(design_docs)

          break  # Exit the loop if the update is successful
       except couchdb.http.ResourceConflict:
        # Handle the conflict by retrying the update
          pass

def write_to_csv(output_folder, filename, data, fieldnames):
    
    csv_file_path = os.path.join(output_folder, filename)
    with open(csv_file_path, mode="w", newline="") as csv_file:
        writer = csv.DictWriter(csv_file, fieldnames=fieldnames)
        writer.writeheader()
        for entry in data:
            writer.writerow(entry)
    return list(data)

def formatForTimeSeries(result, doc, keyStr, valueStr):
    upd_result = []
    for item in list(result):
            date_string = item[keyStr]
            date_object = datetime.strptime(date_string, "%Y-%m-%d")
            year = date_object.year
            if year>2015:
                upd_result.append({"key": year, "value": item[valueStr] })

    grouped_data = defaultdict(list)

    for entry in upd_result:
        key = entry["key"]
        grouped_data[key].append(entry["value"])

    data = [{"Year": key, doc: len(values)} for key, values in grouped_data.items()]

    return data

#---------------------Number of Components by Type-------------------------------------------
def queryExecutionComponentByType(db):
        print(f'\nExecuting the query for Number of Components by Type for SI.................../')
        design_doc = "_design/Component"
        map_function = {"map": "function(doc) {  if (doc.type == 'component' && doc.businessUnit == 'SI') {    emit(doc.componentType, doc._id);  } }"}
        #Temporary View
        saveNewView(db, design_doc, "bySIcomponenttype", map_function)
        
        result = fetchResults(db, design_doc, "bySIcomponenttype")
        
        grouped_data = defaultdict(list)
        for entry in list(result):
            if entry["key"] == "":
                entry["key"] = "empty"
            key = entry["key"]
            grouped_data[key].append(entry["value"])

        data = [{"type": key if key else "EMPTY", "value": len(values)} for key, values in grouped_data.items()]

        field_names = ["type", "value"]
        write_to_csv(output_folder,"componentType.csv", data, field_names  )

#-------------------Counting total number of comp, proj, rel-------------------------------------    
def queryExecutionCountAll(db):
        print(f'\nExecuting the query for counting total number of comp, proj, rel.................../')
        lengthArr = []

        #Counting total projects
        design_doc = "_design/Project"
        map_function = {"map": "function(doc) {  if (doc.type == 'project' && doc.businessUnit == 'SI') { if(doc.releaseIdToUsage) { for(var key in doc.releaseIdToUsage) { emit(key, doc._id); } } else {emit(doc._id, doc._id)} } }"}
        #Temporary View
        saveNewView(db, design_doc, "allSI", map_function)
        
        result = list(fetchResults(db, design_doc, "allSI"))

        unique_proj = list(set([row.value for row in result]))


        dataProj = {"key":"Projects", "value":len(unique_proj)}

        #Counting total releases
        id_list = [row.key for row in result]

        design_doc = "_design/Release"
        db_query = db.find({"selector":{"type": "release","_id": {"$in": id_list}} , "limit":999999999999999}) 
        result = list(db_query)
        dataRel = {"key":"Releases", "value":len(result)}

        #Counting total components
        design_doc = "_design/Component"
        id_list = [doc["componentId"] for doc in result]
        db_query = db.find({"selector":{"type": "component","_id": {"$in": id_list}} , "limit":999999999999999}) 
        result = list(db_query)
        dataComp = {"key":"Components", "value":len(result)}

        lengthArr.append(dataComp)
        lengthArr.append(dataProj)
        lengthArr.append(dataRel)

        grouped_data = defaultdict(list)

        for entry in lengthArr:
            key = entry["key"]
            grouped_data[key] = (entry["value"])

        data = [{"type": key, "value": values} for key, values in grouped_data.items()]

        field_names = ["type", "value"]
        write_to_csv(output_folder,"noOfProjCompRel.csv", data, field_names  )


#---------------------------Time Series by Year for Proj, Comp, Rel-----------------------------
def queryCompProjRelTimeSeriesExecution(db):
    print(f'\nExecuting the time-series query.................../')
    print(f'\n  Executing the time-series query for projects.................../')
    design_doc = "_design/Project"
    map_function = {"map": "function(doc) {  if (doc.type == 'project' && doc.businessUnit == 'SI') {  emit(doc.createdOn, doc._id) }}"}
    #Temporary View
    saveNewView(db, design_doc, "byCreatedOn", map_function)
    
    result = fetchResults(db, design_doc, "byCreatedOn")
    dataProj = formatForTimeSeries(result, "Project", "key", "value")
    
#-----------------------------------------ReleaseCreatedOn------------------------------------------------------------------
    print(f'\n  Executing the time-series query for release.................../')
    design_doc = "_design/Release"
    id_list = [row.value for row in result]
    db_query = db.find({"selector":{"type": "project","_id": {"$in": id_list}} , "limit":999999999999999}) 
    result = list(db_query)
    id_rel_list = []
    for doc in result:
        if(doc["releaseIdToUsage"]) :
            for key in doc["releaseIdToUsage"]:
                if key is not None:
                    id_rel_list.append(key)

    db_query = db.find({"selector":{"type": "release","_id": {"$in": id_rel_list}} , "limit":999999999999999}) 
    result = list(db_query)
    dataRel = formatForTimeSeries(result, "Release", "createdOn","_id")

#-----------------------------------------ComponentCreatedOn------------------------------------------------------------------
    print(f'\n  Executing the time-series query for component.................../')
    design_doc = "_design/Component"
    id_list = [doc["componentId"] for doc in result]
    db_query = db.find({"selector":{"type": "component","_id": {"$in": id_list}} , "limit":999999999999999}) 
    result = list(db_query)
    dataComp = formatForTimeSeries(result, "Component","createdOn","_id")


    combined_data = {}
    for item in dataProj + dataComp + dataRel:
        year = item["Year"]
        if year not in combined_data:
            combined_data[year] = {"Year": year}
        combined_data[year].update(item)

    data = list(combined_data.values())
    field_names = ["Year", "Component", "Project", "Release"]
    write_to_csv(output_folder,"TimeSeries.csv", data, field_names )

#------------------------------------------------------------------CLI Size ------------------------------------------
def getCLIList(db,dbattach):
    print(f'Executing the project  query.................../')
    design_doc = "_design/Project"
    viewsList = None
    #function(doc) {  if (doc.type == 'project') {    for(var i in doc.releaseIdToUsage) {      emit(i, {id:doc._id,businessUnit:doc.tag});    }  }}
    #function(doc) {  if (doc.type == 'project') {    for(var i in doc.releaseIdToUsage) {      emit(i, {businessUnit:doc.tag, group:doc.group, id:doc._id});    }  }}
    view = "byReleaseAndSIBusinessUnit"
    map_function = {
    "map": "function(doc) {  if (doc.type == 'project' && doc.businessUnit == 'SI') {  {   for(var i in doc.releaseIdToUsage) {      emit(i, {businessUnit:doc.tag, group:doc.businessUnit, id:doc._id});  }  }  }}"
    }


    #Creating temporary view byReleaseAndBusinessUnit for project
    saveNewView(db, design_doc, view, map_function)
    
    #Executing the view such that it returns projects that has releases
    result = fetchResults(db, design_doc, "byReleaseAndSIBusinessUnit")

    for entry_dict in list(result):
        if "value" in entry_dict and "businessUnit" in entry_dict["value"]:
            if entry_dict["value"]["businessUnit"] == "":
                entry_dict["value"]["businessUnit"] = "empty"
            pattern = r'[^A-Za-z0-9\s]+'
            cleaned_string = re.sub(pattern, ' ', entry_dict["value"]["businessUnit"])
            words = cleaned_string.split()
            
            if words[0] == 'SI':
                first_two_words = ' '.join(words[:2])
                entry_dict["value"]["businessUnit"] = first_two_words

            first_word = words[0]
            if first_word[0:2] == 'RC' and 'SI' in words:
                entry_dict["value"]["businessUnit"] = 'SI RSS'


    unique_dict_map = {}  # Dictionary to track unique combinations
    unique_dicts = []     # List to store unique dictionaries

    for item in list(result):
        if "value" in item and "businessUnit" in item["value"]:
            key = item["key"]
            group = item["value"]["businessUnit"]
            unique_key = (key, group)  # Combine key and business unit for uniqueness

            if unique_key not in unique_dict_map:
                unique_dict_map[unique_key] = True
                unique_dicts.append(item)

    result = unique_dicts
    id_list = [row.key for row in result]
    
    #Executing the release query such that it returns releases that has CLI accepted status attachments...................
    db_query = db.find({"selector":{"type": "release","_id": {"$in": id_list},  "$and": [  {"attachments": {"$exists": True, "$type": "array", "$elemMatch": {"attachmentType": "COMPONENT_LICENSE_INFO_XML","checkStatus": "ACCEPTED" } } } ]}
                                                                                            , "limit":999999999999999}) 
    db_list = list(db_query)

    filtered_data = [
        {
            "_id": item["_id"],
            "type": item["type"],
            "attachments": [
                attachment for attachment in item["attachments"]
                if attachment["attachmentType"] == "COMPONENT_LICENSE_INFO_XML" and  "checkStatus" in attachment and
                    attachment["checkStatus"] is not None and attachment["checkStatus"] == "ACCEPTED"
            ]
        }
        for item in db_list
    ]      
    

    #Update the first list and remove releaseId not present based on second list
    ids_to_keep = {doc["_id"] for doc in filtered_data}
    filtered_list = [doc for doc in list(result) if doc.key in ids_to_keep]

    attach_id_list = []
    attach_values_list = []
    for doc in filtered_data:
        attach_list = doc.get("attachments", []) #Getting nested attachments list from release documents
        attach_values_list.extend(attach_list)
        content_ids = [attach.get("attachmentContentId") for attach in attach_list] #Getting the attachmentContentId and storing it in a list of the same
        attach_id_list.extend(content_ids)

    #Executing the attachment release query such that it returns the attachment doc list that has the actual length.................../')
    db_query = dbattach.find({"selector":{"type": "attachment","_id": {"$in": attach_id_list}}, "limit":999999999999999})
    db_attach_list = list(db_query)

    # Create a new list of dictionaries with the specified key-value pairs
    new_list = {} 
    length_list = []
    for doc in db_attach_list:
        
        for key, value in doc.get("_attachments", {}).items():
                length = value.get("length")
                new_list[doc["_id"]] = length
                length_list.append(length)

    content_id_map = {}
    for doc in filtered_data:
        attach_list = doc.get("attachments", []) #Getting the nested attachment content
        for attach in attach_list:
            content_id_map[attach["attachmentContentId"]] = {"releaseId": doc["_id"], "filename": attach["filename"]} #Saving it in a key value pair dicitonary for the lookup 
    
    
    # Merge data based on 'attachid' and 'contentId'
    merged_list = []
    for doc in db_attach_list:
        attachid = doc["_id"]
        if attachid in content_id_map:                   #Comparing attachmentId from attachment doc with attachmentContent from release doc
            content_data = content_id_map[attachid]
            length = None
            for key, value in doc.get("_attachments", {}).items():
                length = value.get("length")
            #Creating a single document which has releaseId, attachmentId, filename and length
            merged_doc = {
                "releaseId": content_data["releaseId"] ,
                "attachid":  attachid,
                "filename": content_data["filename"],   
                "length": length
            }
            merged_list.append(merged_doc)

    for entry_dict in merged_list:
        if "length" in entry_dict and entry_dict["length"] is None:
            entry_dict["length"] = 0

    length_data = {}
    
# Iterate through the second list to calculate total length for each relId
    for entry in merged_list:
        rel_id = entry["releaseId"]
        length = entry["length"]
        if rel_id in length_data:
            length_data[rel_id] += length
        else:
            length_data[rel_id] = length

    # Combine data 
    combined_data = []
    for entry in filtered_list:
        key = entry["key"]
        value = entry["value"]
        id = value["id"]
        
        if key in length_data:
            combined_data.append({"key": key, "value": value, "length": length_data[key]})
            if "businessUnit" in value and value["businessUnit"]=="CT":
                temp_dict = {"key": key, "value": value, "length": length_data[key]}
        else:
            combined_data.append({"key": key, "value": value, "length": 0})  #Adding another tag to the document returned by the view byReleaseAndBusinessUnit
                                    #updated_list.append(dictionary.copy())
            if value["businessUnit"]=="CT":
                temp_dict = {"key": key, "value": value, "length": length_data[key]}
    

    combined_data = nullCheckCLI(combined_data)
            
    
    business_unit_info = defaultdict(lambda: {"count": 0, "total_length": 0})
    business_unit_info = appendCLISize(business_unit_info, combined_data, "businessUnit")
    # Convert the defaultdict to a regular dictionary
    resultdict = dict(business_unit_info)


    csv_file_path = os.path.join(output_folder,'attachmentUsageDept.csv')

    # Extract keys and values from the dictionary
    keys = list(resultdict.keys())
    values = [item['total_length'] for item in resultdict.values()]
    data = [{'Key': key, 'Value': value} for key, value in zip(resultdict.keys(), (item['total_length'] for item in resultdict.values()))]
    field_names = ['Key', 'Value']  
    write_to_csv(output_folder,"attachmentUsageDept.csv", data, field_names )


    group_info = defaultdict(lambda: {"count": 0, "total_length": 0})
    group_info = appendCLISize(group_info, combined_data, "group") 

    # Convert the defaultdict to a regular dictionary
    resultdict = dict(group_info)

    data = [{'Key': key, 'Value': value} for key, value in zip(resultdict.keys(), (item['total_length'] for item in resultdict.values()))]
    field_names = ['Key', 'Value']  
    write_to_csv(output_folder,"attachmentUsage.csv", data, field_names )

    return db_attach_list
    

def appendCLISize(organizational_unit, combined_data, type):
    # Iterate through the documents and aggregate the lengths
    for doc in combined_data:
        if "value" in doc and "group" in doc["value"] and "length" in doc and "businessUnit" in doc["value"]:
            business_unit = doc["value"][type]
            length = doc["length"]
            organizational_unit[business_unit]["count"] += 1
            organizational_unit[business_unit]["total_length"] += length
    return organizational_unit
 
def nullCheckCLI(combined_data):
    for entry_dict in combined_data:
        if "value" in entry_dict and "businessUnit" in entry_dict["value"]:
            if entry_dict["value"]["businessUnit"] == "":
                entry_dict["value"]["businessUnit"] = "empty"
            if "length" in entry_dict and entry_dict["length"] is None:
                entry_dict["length"] = 0
        if "value" in entry_dict and "group" in entry_dict["value"]:
            if entry_dict["value"]["group"] == "":
                entry_dict["value"]["group"] = "empty"
    return combined_data

#-----------------------------Cleared/Not Cleared Releases----------------------------------
def queryExecutionReleasesECCClearedStatus(db):
        print(f'\nExecuting the query for release clearing status.................../')
        design_doc = "_design/Release"

        #Existing view
        result = fetchResults(db, design_doc, "byStatus")
        
        grouped_data = defaultdict(list)

        for entry in list(result):
            if entry["key"] == "":
                entry["key"] = "empty"
            key = entry["key"]
            grouped_data[key].append(entry["value"])

        data = [{"type": key if key else "EMPTY", "value": len(values)} for key, values in grouped_data.items()]
        field_names = ["type", "value"]
        write_to_csv(output_folder,"releaseStatus.csv", data, field_names )

#---------------------------Cleared/Not Cleared Release status based on Type-------------------------
def queryExecutionReleasesECCClearedStatus(db):
        print(f'\nExecuting the query for release clearing status.................../')
        design_doc = "_design/Project"
        result = fetchResults(db, design_doc, "allSI")
        id_list = [row.key for row in result]


        db_query = db.find({"selector":{"type": "release","_id": {"$in": id_list}} , "limit":999999999999999}) 
        result = list(db_query)

        design_doc = "_design/Release"
       
        grouped_data = defaultdict(list)

        # Extract all  IDs
        id_list = [doc['componentId'] for doc in result]

        db_query = db.find({"selector":{"type": "component","_id": {"$in": id_list}}, "limit":999999999999999})

        db_list = list(db_query)                

        comp_data = [{"id": doc["_id"], "componentType": doc.get("componentType")} for doc in db_list]

        comp_lookup = {data["id"]: data for data in comp_data}


        # Merge data from CouchDB into the original documents
        merged_documents = [
            {"id": doc["componentId"], "status": None if "eccInformation" not in doc or "eccStatus" not in doc["eccInformation"] else  doc["eccInformation"]["eccStatus"], "type": comp_lookup[doc["componentId"]]["componentType"] if doc["componentId"] in comp_lookup else None}
            for doc in result
        ]

        # Count the occurrences of each type and status combination
        type_status_count = defaultdict(int)
        for doc in merged_documents:
            type_status_count[(doc["type"] if doc["type"] else "EMPTY", doc["status"] if doc["status"] else "EMPTY")] += 1


        # Convert the count into the desired format
        grouped_data = [{"type": key[0], "status": key[1], "count": value} for key, value in type_status_count.items()]

        field_names = ["type", "status", "count"]
        write_to_csv(output_folder,"releaseStatusBasedOnCompType.csv", grouped_data, field_names )

#-----------------------------Attachment Disk Usage Total----------------------------------


def queryExecutionAttachmentUsageAll(db,db_attach_list):
        print(f'\n Executing the query for getting total attachment disk usage.................../')
        design_doc = "_design/AttachmentContent"

        length_list = []
        total_length = 0
        item_count = 0
        for doc in db_attach_list:
            
            for key, value in doc.get("_attachments", {}).items():
                    length = value.get("length")
                    total_length+=length
                    item_count+=1
                    length_list.append(length)

        data = [{"total_length":total_length, "count": item_count} ]
        field_names = ["total_length", "count"]
        write_to_csv(output_folder,"attachmentDiskUsageTotal.csv", data, field_names )

#------------------------Most Used Components-------------------------------------
def queryExecutionMostUsedComp(db):
    print(f'\n Executing the query for most used components.................../')
    design_doc = "_design/Project"
    #Existing view
    result = fetchResults(db, design_doc, "allSI")

    id_list = [row.key for row in result]


    db_query = db.find({"selector":{"type": "release","_id": {"$in": id_list}} , "limit":999999999999999}) 
    result = list(db_query)

    key_counts = {}
    for item in result:
        key = item["componentId"]
        name = item["name"]
        if key in key_counts:
            key_counts[key]["count"] += 1
        else:
            key_counts[key] = {"key": key, "name": name, "count": 1}

    # Create the new list of dictionaries
    result_list = list(key_counts.values())
    sorted_list = sorted(result_list, key=lambda x: x["count"], reverse=True)
    field_names = ["key", "name", "count"]
    write_to_csv(output_folder,"mostUsedComp.csv", sorted_list, field_names )

#------------------Most Used Licenses------------------
def queryExecutionMostUsedLicenses(db):
    print(f'\n Executing the query for most used licenses.................../')
    design_doc = "_design/Project"
    result = fetchResults(db, design_doc, "allSI")

    id_list = [row.key for row in result]


    db_query = db.find({"selector":{"type": "release","_id": {"$in": id_list}} , "limit":999999999999999}) 
    result = list(db_query)

    design_doc = "_design/Release"
    
    grouped_data = defaultdict(list)

    # Extract all  IDs
    id_list = [doc['componentId'] for doc in result]

    db_query = db.find({"selector":{"type": "component","_id": {"$in": id_list}}, "limit":999999999999999})

    result = list(db_query)

    license_list = []
    for doc in result:
        if doc["mainLicenseIds"]:
            for value in doc["mainLicenseIds"]:
                license_list.append(value)

    
    license_count = Counter(license_list)

    license_list = [{'license': lic, 'count': count} for lic, count in license_count.items()]
    sorted_license_list = sorted(license_list, key=lambda x: x["count"], reverse=True)

    field_names = ["license", "count"]
    write_to_csv(output_folder,"mostUsedLicenses.csv", sorted_license_list, field_names )
    
#------------------------Components that are not used-------------------------------------
def queryExecutionCompNotUsed(db):
    print(f'\n Executing the query for components not being used.................../')
    design_doc = "_design/Project"
    #Existing view
    result = fetchResults(db, design_doc, "byreleaseid")
    proj_rel_id_list = [row.key for row in result]

    #Executing the view for all the releases
    design_doc = "_design/Release"
    result = list(fetchResults(db, design_doc, "all"))

    rel_id_list = [row.value for row in result]

    proj_rel_id_list = set(proj_rel_id_list)
    rel_id_list = set(rel_id_list)

    # Find values in the original list that are not in the input list
    result_id_list = list(rel_id_list - proj_rel_id_list)


    db_query = list(db.find({"selector":{"type": "release","_id": {"$in": result_id_list}}, "limit":999999999999999}))

    comp_result = {}
    for item in db_query:
        key = item["componentId"]
        name = item["name"]
        comp_result[key] = {"key": key, "name": name}

    result_list = list(comp_result.values())

    field_names = ["key", "name"]
    write_to_csv(output_folder,"notUsedComp.csv", result_list, field_names )    


def run():
    start_time = time.time()
    
    #DB connection
    db = dbConnection("sw360db")
    dbAttach = dbConnection("sw360attachments")

    #Query execution
    queryExecutionComponentByType(db)
    queryExecutionCountAll(db)
    queryCompProjRelTimeSeriesExecution(db)
    queryExecutionReleasesECCClearedStatus(db)
    queryExecutionMostUsedComp(db)
    queryExecutionMostUsedLicenses(db)
    queryExecutionCompNotUsed(db)
    db_attach_list = getCLIList(db, dbAttach)
    queryExecutionAttachmentUsageAll(dbAttach,db_attach_list)

if __name__ == '__main__':
    try:
        start_time = time.time()
        run()
        print('\nExecution time: ' + "{0:.2f}".format(time.time() - start_time) + 's')

    except Exception as e:
        print('Exception message ',e)

