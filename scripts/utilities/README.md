# Utilities

This folder contains utility scripts which implement the database schema changes.

The scripts are written in `python2` and depend on `python2-couchdb`. The couchdb has to run and be accessible on `localhost:5984`.

To execute it is recommended to do this in the following order:
1. ensure that couchdb is accessible (try to open `http://localhost:5984/_utils/`)
2. run the utility scripts (i.e. for each script call `python2 /PATH/TO/00?_some_utility_script.py`)
    * be aware that some scripts are using an internal dry-run switch which you have to change manually in the script's code

## List and Description of script
- `001_update_project_field_value_couchdb_1_x.py`
    - Script for couchdb 1.x to update a field*(String)* in project document to a new value.
      Following variables needs to be updated based on the requirement
      1. `fieldName` = *"field_to_be_migrated"*
      2. `oldValue` = *"old_value"*
      3. `newValue` = *"new_value"*
- `002_update_view.py`
    - Script to update a view for a design document.
      Following variables needs to be updated based on the requirement
      1. `doc_id` = *"document_id"*
      2. `nested_field_key` = *"field_to_be_updated"*
      3. `newValue` = *"new_value"*
- `003_update_project_field_value_couchdb_2_x.py`
    - Script for couchdb 2.x to update a field*(String)* in project document to a new value.
      Following variables needs to be updated based on the requirement
      1. `fieldName` = *"field_to_be_migrated"*
      2. `oldValue` = *"old_value"*
      3. `newValue` = *"new_value"*
- `004_recompute_clearing_state_of_release.py`
    - Script to recompute clearing state of release.

## Run the scripts for a database not running on localhost
tbd.
