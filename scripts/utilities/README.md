# Utilities

This folder contains utility scripts which implement the database schema changes.

The scripts are written in `python3` and depend on the `couchdb` library. The CouchDB server has to be running and accessible on `localhost:5984`.

To execute the scripts, follow these steps:
1. Ensure that CouchDB is accessible (try to open `http://localhost:5984/_utils/`).
2. Run the utility scripts (e.g., for each script call `python3 /PATH/TO/00?_some_utility_script.py`).
    * Be aware that some scripts use an internal dry-run switch which you have to change manually in the script's code.

## List and Description of Scripts
- `001_update_project_field_value_couchdb_1_x.py`
    - Script for CouchDB 1.x to update a field (String) in a project document to a new value.
      Following variables need to be updated based on the requirement:
      1. `fieldName` = *"field_to_be_migrated"*
      2. `oldValue` = *"old_value"*
      3. `newValue` = *"new_value"*
- `002_update_view.py`
    - Script to update a view for a design document.
      Following variables need to be updated based on the requirement:
      1. `doc_id` = *"document_id"*
      2. `nested_field_key` = *"field_to_be_updated"*
      3. `newValue` = *"new_value"*
- `003_update_project_field_value_couchdb_2_x.py`
    - Script for CouchDB 2.x to update a field (String) in a project document to a new value.
      Following variables need to be updated based on the requirement:
      1. `fieldName` = *"field_to_be_migrated"*
      2. `oldValue` = *"old_value"*
      3. `newValue` = *"new_value"*
      4. `isStartsWith` = *"False/True"*
- `004_recompute_clearing_state_of_release.py`
    - Script to recompute the clearing state of a release.

## Running the Scripts for a Database Not Running on Localhost
To run the scripts on a remote CouchDB instance, follow these steps:
1. Ensure that the remote CouchDB is accessible and you have the necessary credentials.
2. Update the script to point to the remote CouchDB URL.
3. Run the script with the necessary parameters.

Example:
```bash
export COUCHDB_URL=http://remote-couchdb-server:5984
python3 /PATH/TO/00?_some_utility_script.py
