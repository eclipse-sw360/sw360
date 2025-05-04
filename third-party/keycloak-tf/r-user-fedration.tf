# SPDX-License-Identifier: Siemens-ISL-1.5
# KeyCloak custom user federation to import CouchDB users to KeyCloak

resource "keycloak_custom_user_federation" "sw360_user_jpa" {
  realm_id    = keycloak_realm.sw360.id
  name        = "sw360-user-storage-jpa"
  provider_id = "sw360-user-storage-jpa"

  enabled = true

  changed_sync_period = "-1"
  full_sync_period    = "-1"
  cache_policy        = "DEFAULT"
}
