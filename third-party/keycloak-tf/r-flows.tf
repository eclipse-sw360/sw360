# SPDX-License-Identifier: Siemens-ISL-1.5
# Keycloak Authentication flows to be used

# 1. First login flow
resource "keycloak_authentication_flow" "first_login" {
  realm_id    = keycloak_realm.sw360.id
  alias       = "first-login-user-association"
  description = "Associate users based on email id on their first login, or create if not exists"
  provider_id = "basic-flow"
}

# 1.1. Subpath to split
resource "keycloak_authentication_subflow" "create_or_link" {
  realm_id          = keycloak_realm.sw360.id
  parent_flow_alias = keycloak_authentication_flow.first_login.alias
  alias             = "User create or linking"
  description       = "Create user on first login or link to existing user from DB"
  requirement       = "REQUIRED"
  provider_id       = "basic-flow"
}

# 1.1.1. Alternative subpath to link existing users
resource "keycloak_authentication_subflow" "link_existing_user" {
  realm_id          = keycloak_realm.sw360.id
  parent_flow_alias = keycloak_authentication_subflow.create_or_link.alias
  alias             = "Link existing user"
  description       = "If the user already exists, link to existing account"
  requirement       = "ALTERNATIVE"
  provider_id       = "basic-flow"
  priority          = 0
}

# 1.1.1.1. Get the existing user from idp
resource "keycloak_authentication_execution" "idp_detect_existing" {
  realm_id          = keycloak_realm.sw360.id
  parent_flow_alias = keycloak_authentication_subflow.link_existing_user.alias
  authenticator     = "idp-detect-existing-broker-user"
  requirement       = "REQUIRED"
  priority          = 0
}

# 1.1.2.2. Link the user with existing idp user
resource "keycloak_authentication_execution" "idp_auto_link" {
  realm_id          = keycloak_realm.sw360.id
  parent_flow_alias = keycloak_authentication_subflow.link_existing_user.alias
  authenticator     = "idp-auto-link"
  requirement       = "REQUIRED"
  priority          = 1
}

# 1.1.2. Alternative path to create user if new
resource "keycloak_authentication_execution" "idp_create_unique" {
  realm_id          = keycloak_realm.sw360.id
  parent_flow_alias = keycloak_authentication_subflow.create_or_link.alias
  authenticator     = "idp-create-user-if-unique"
  requirement       = "ALTERNATIVE"
  priority          = 1
}

# 2. Browser login flow to hide login form
resource "keycloak_authentication_flow" "browser_login" {
  realm_id    = keycloak_realm.sw360.id
  alias       = "browser-login"
  description = "Redirect to idp without login form on browser"
}

# 2.1. Alternative path, if browser cookie exists, use it
resource "keycloak_authentication_execution" "browser_cookie" {
  realm_id          = keycloak_realm.sw360.id
  parent_flow_alias = keycloak_authentication_flow.browser_login.alias
  authenticator     = "auth-cookie"
  requirement       = "ALTERNATIVE"
  priority          = 10
}

# 2.2. Alternative path, redirect to idp, no forms
resource "keycloak_authentication_execution" "browser_idp" {
  realm_id          = keycloak_realm.sw360.id
  parent_flow_alias = keycloak_authentication_flow.browser_login.alias
  authenticator     = "identity-provider-redirector"
  requirement       = "ALTERNATIVE"
  priority          = 20
}

# 2.2.1. Config path for idp redirect, sets which idp to use
resource "keycloak_authentication_execution_config" "browser_idp" {
  realm_id     = keycloak_realm.sw360.id
  execution_id = keycloak_authentication_execution.browser_idp.id
  alias        = "azure-sw360"
  config = {
    defaultProvider = keycloak_oidc_identity_provider.entra_id.alias
  }
}

# 3. Bind the (2) browser login flow to realm
resource "keycloak_authentication_bindings" "browser_binding" {
  realm_id     = keycloak_realm.sw360.id
  browser_flow = keycloak_authentication_flow.browser_login.alias
}
