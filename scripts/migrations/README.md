# Migrations

This folder contains scripts which implement the database migrations.

The scripts are written in `python2` and depend on `python2-couchdb`. The couchdb has to run and be accessible on `localhost:5984`.

To migrate it is recommended to do this in the following order:
1. stop SW360 (i.e. the tomcat)
2. ensure that couchdb is accessible (try to open `http://localhost:5984/_utils/`)
3. run the migration scripts (i.e. for each script call `python2 /PATH/TO/00?_some_migration_script.py`)
    * be aware that some scripts are using an internal dry-run switch which you have to change manually in the script's code
4. deploy the new `.war` files
5. start SW360 again

## Which scripts are necessary
### 1.3.0 -> 1.4.0
- `001_migrate_license_shortname_to_id.py`
- `002_remove_project_comoderators.py`
- `003_rename_release_contacts_to_contributors.py`
### 1.4.0 -> 1.5.0
### 1.5.0 -> 1.6.0
- `004_move_release_ecc_fields_to_release_information.py`
### 1.6.0 -> 1.7.0
### 1.7.0 -> 2.0.0
- `005_convert_compatibility_fields_to_ternary.py`
- `006_convert_project_release_relationship_to_enums.py`
### 2.0.0 -> 2.2.0
### 2.2.0 -> 3.0.0
- `007_add_submitters_usergroup_to_moderation_request.py`
- `008_add_component_type_to_moderation_requests.py`
### 3.3.0 -> 4.0.0
- `011_migrate_attachment_usages_license_info.py`
- `012_migrate_todoid_to_title.py`

## Optional usage
- `009_overwrite_release_name_with_component_name.py`
- `010_repair_missing_vendorId_links_in_releases.py`

## Run the scripts for a database not running on localhost
tbd.

## Run the scripts within docker
You can use the bash script `./dockerized-migration-runner.sh` to run the scripts wrapped within a docker container.
