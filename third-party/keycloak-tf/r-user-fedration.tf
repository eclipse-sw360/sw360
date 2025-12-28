# Copyright (c) Siemens AG 2025.
# SPDX-License-Identifier: EPL-2.0
# Part of the SW360 Portal Project.
# KeyCloak custom user federation to import CouchDB users to KeyCloak

# User federation to work with CouchDB and KeyCloak
resource "keycloak_custom_user_federation" "sw360_user_jpa" {
  realm_id    = keycloak_realm.sw360.id
  name        = "sw360-user-storage-jpa"
  provider_id = "sw360-user-storage-jpa"

  enabled = false

  changed_sync_period = "-1"
  full_sync_period    = "-1"
  cache_policy        = "DEFAULT"
}
