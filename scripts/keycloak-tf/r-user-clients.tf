# Copyright (c) Siemens AG 2025.
# SPDX-License-Identifier: EPL-2.0
# Part of the SW360 Portal Project.
# Holds the information for clients created for SW360 users

# OpenID Clients which can connect with SW360 REST API, authenticated via KC
resource "keycloak_openid_client" "sw360_user_clients" {
  for_each = { for client in local.sw360_clients : client.user_email => client }

  realm_id  = keycloak_realm.sw360.id
  client_id = uuidv5("oid", each.key)

  name    = "${each.value.user_group}-${each.value.user_email}-${each.value.creator_email}-${each.value.creation_date}"
  enabled = true

  access_type         = "CONFIDENTIAL"
  valid_redirect_uris = []
  web_origins         = []

  client_authenticator_type    = "client-secret"
  standard_flow_enabled        = false
  implicit_flow_enabled        = false
  direct_access_grants_enabled = false
  service_accounts_enabled     = true
  frontchannel_logout_enabled  = false
  consent_required             = false
  use_refresh_tokens           = false
}

# Default scopes given to all clients
resource "keycloak_openid_client_default_scopes" "sw360_user_client_default_scope" {
  for_each = keycloak_openid_client.sw360_user_clients

  realm_id  = keycloak_realm.sw360.id
  client_id = each.value.id

  default_scopes = [
    "acr",
    "basic",
    "profile",
    "email",
    "roles",
    "web-origins",
    keycloak_openid_client_scope.sw360_read.name,
  ]
}

# Write scopes given to clients in `local.sw360_write_clients`
resource "keycloak_openid_client_optional_scopes" "sw360_user_client_optional_scope" {
  for_each = { for client in local.sw360_write_clients : client.user_email => client }

  realm_id  = keycloak_realm.sw360.id
  client_id = keycloak_openid_client.sw360_user_clients[each.key].id

  optional_scopes = [
    keycloak_openid_client_scope.sw360_write.name,
  ]
}

# Hardcoded email claim given to all clients as "client-secret" grant type does
# not tell bearer token belongs to which user. This claim maps the user's email
# as a hard coded "email" claim to the token which can then be read by SW360.
resource "keycloak_openid_hardcoded_claim_protocol_mapper" "user_client_email_claim" {
  for_each = keycloak_openid_client.sw360_user_clients

  realm_id  = keycloak_realm.sw360.id
  client_id = each.value.id
  name      = "hardcode-email"

  claim_name  = "email"
  claim_value = each.key

  add_to_access_token = true
  add_to_id_token     = true
  add_to_userinfo     = true
  claim_value_type    = "string"
}
