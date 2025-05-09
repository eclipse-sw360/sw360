# SPDX-License-Identifier: Siemens-ISL-1.5
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

# 3. Bind the scopes to SW360 UI realm. arc, basic, profile, email, roles &
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
  ]
}

# 4. Assign scopes for grafana client
resource "keycloak_openid_client_default_scopes" "grafana" {
  realm_id = keycloak_realm.sw360.id
  client_id = keycloak_openid_client.grafana.id
  default_scopes = [
    "email",
    "profile",
    "roles",
  ]
}
