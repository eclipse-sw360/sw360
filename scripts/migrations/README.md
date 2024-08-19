# Migrations

This folder contains scripts which implement the database migrations.

The scripts are written in `python2` and depend on `python2-couchdb`. The couchdb has to run and be accessible on `localhost:5984`.

From release 16.0.0 onwards python2 is not supported.All migration scripts form release 16.0.0 onwards are in python3. To adapt the migrate scripts to be Python 3 compatible, you need to change the print statement to a print() function. Because in Python 2, print is a statement and can be used without parentheses. However, in Python 3, print is a function and therefore always requires parentheses. Try modifying print "CR Id: " + cr.get("_id") like this: print("CR Id: " + str(cr.get("_id"))).

To migrate it is recommended to do this in the following order:
1. stop SW360 (i.e. the tomcat)
2. ensure that couchdb is accessible (try to open `http://localhost:5984/_utils/`)
3. run the migration scripts (i.e. for each script call `python3 /PATH/TO/00?_some_migration_script.py`)
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
### 4.0.0 -> 5.0.0
- `013_migrate_releases_external_tool_requests.py`
### 5.x.x -> 6.0.0
- `014_migrate_fossology_rest.py`

### 6.0.0 -> 7.0.0
- `015_update_fullmyprojects_view.py`

### 7.0.1-M1 -> 7.1.0
- `016_update_byExternalIds_component_view.py`

### 8.2.0 -> 9.0.0
- `017_update_empty_release_clearing_state_to_default_NEW_CLEARING.py`

### 9.0.0 -> 10.0.0
- `018_remove_unwanted_field_from_clearing_request.py`

### 10.1.0 -> 11.0.0
- `019_update_byExternalIds_component_view.py`
- `020_update_byExternalIds_release_view.py`
- `021_update_byexternalids_project_view.py`
- `022_migrate_todo_to_obligation.py`

### 11.0.0 -> 12.0.0
- `023_rename_obligationType_to_obligationLevel.py`
- `024_update_type_from_obligations_to_obligation.py`
- `025_remove_old_obligations_view.py`
- `026_licenseObligation_populate_text_field.py`
- `027_licenseObligation_field_update.py`
- `028_update_type_from_licenseObligation_to_obligation.py`
- `029_remove_old_licenseobligation_view.py`
- `030_obligation_field_update.py`
- `031_update_obligationLevel_from_productObligation_to_projectObligation.py`
- `032_rename_linkedObligation_to_linkedObligationStatus_in_ProjectObligation.py`
- `033_update_type_from_projectObligation_to_obligationList.py`
- `034_remove_old_projectObligation_view.py`
- `035_risk_field_updates.py`
- `036_drop_old_views_and_license_field_update.py`
- `037_checkfor_project_todos_in_moderations.py`
- `038_convert_ObligationStatusInfo_type_to_obligationType.py`
- `039_projecttodo_to_obligationlist.py`
- `040_rename_downloadurl_to_sourceCodeDownloadurl_in_Release.py`
- `041_update_release_moderation_with_downloadurl.py`

### 12.0.0 -> 13.0.0

- `042_remove_validForProject_from_Obligation.py`

### 13.4.0 -> 14.0.0

- `043_migrate_project_homepage_wiki_to_externalUrls.py`
- `044_replace_gpl_compatibility_by_osi_approved.py`
- `045_migrate_project_linked_project_relation.py`
- `046_migrate_project_moderation_request_linked_project_relation.py`
- `047_migrate_obligation_status.py`

### 15.0.0 -> 16.0.0

- `048_add_component_businessunit.py`
- `049_migrate_admin_obligation.py`

### 16.0.0 -> 17.0.0

- `050_cleanup_eccinformation_duplicate_attributes.py`
- `051_change_eccStatus.py`
- `052_migrate_clearing_request_status.py`
- `053_remove_whitespace_component_name.py`
- `054_remove_inactiveUsers_from_moderators.py`
- `055_deactivateEmailNotification.py`
- `056_dataReduction.py`
- `057_deleteClosedModReq.py`
- `058_closedModReqStats.py`

### 17.0.0 -> 17.0.1

- `059_repair_broken_releasevulnerability.py`

### 17.0.1 -> 18.0.0

- `060_migrate_project_dependency_network.py`
- `061_add_modifiedBy_modifiedOn_project.py`
- `062_add_release_info_in_proj_doc.py`

## Optional usage
- `009_overwrite_release_name_with_component_name.py`
- `010_repair_missing_vendorId_links_in_releases.py`

## Run the scripts for a database not running on localhost
tbd.

## Run the scripts within docker
You can use the bash script `./dockerized-migration-runner.sh` to run the scripts wrapped within a docker container.
