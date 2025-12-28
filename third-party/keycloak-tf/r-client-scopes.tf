# Copyright (c) Siemens AG 2025.
# SPDX-License-Identifier: EPL-2.0
# Part of the SW360 Portal Project.
# Client scopes for SW360 realm clients

# 1. READ scope
resource "keycloak_openid_client_scope" "sw360_read" {
  realm_id               = keycloak_realm.sw360.id
  name                   = "READ"
  description            = "Read access to SW360"
  include_in_token_scope = true
}

# 2. WRITE scope
resource "keycloak_openid_client_scope" "sw360_write" {
  realm_id               = keycloak_realm.sw360.id
  name                   = "WRITE"
  description            = "Write access to SW360"
  include_in_token_scope = true
}

# 3. userGroup scope
resource "keycloak_openid_client_scope" "user_group" {
  realm_id               = keycloak_realm.sw360.id
  name                   = "userGroup"
  description            = "Get User Groups"
  include_in_token_scope = true
}

# 3.1. Add mapper for SW360 UI client
resource "keycloak_openid_group_membership_protocol_mapper" "user_group" {
  realm_id            = keycloak_realm.sw360.id
  claim_name          = "userGroup"
  name                = "userGroup"
  client_scope_id     = keycloak_openid_client_scope.user_group.id
  add_to_access_token = true
  add_to_id_token     = true
  add_to_userinfo     = true
}

# 4. Bind the scopes to SW360 UI realm. arc, basic, profile, email, roles &
# web-origins are used by KC by-default
resource "keycloak_openid_client_default_scopes" "client_read" {
  realm_id  = keycloak_realm.sw360.id
  client_id = keycloak_openid_client.sw360_ui.id
  default_scopes = [
    "acr",
    "basic",
    "profile",
    "email",
    "roles",
    "web-origins",
    keycloak_openid_client_scope.sw360_read.name,
    keycloak_openid_client_scope.sw360_write.name,
    keycloak_openid_client_scope.user_group.name,
  ]
}

# 5. Assign scopes for grafana client
resource "keycloak_openid_client_default_scopes" "grafana" {
  realm_id  = keycloak_realm.sw360.id
  client_id = keycloak_openid_client.grafana.id
  default_scopes = [
    "email",
    "profile",
    "roles",
  ]
}

# 5.1. Add group mapper for grafana client
resource "keycloak_openid_group_membership_protocol_mapper" "grafana_group_mapper" {
  realm_id   = keycloak_realm.sw360.id
  client_id  = keycloak_openid_client.grafana.id
  name       = "group-mapper"
  claim_name = "groups"
}
