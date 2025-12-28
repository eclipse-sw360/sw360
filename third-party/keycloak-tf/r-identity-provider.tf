# Copyright (c) Siemens AG 2025.
# SPDX-License-Identifier: EPL-2.0
# Part of the SW360 Portal Project.
# Generic OIDC identity providers for the realm, based on EntraID

# 1. Identity provider
resource "keycloak_oidc_identity_provider" "entra_id" {
  realm        = keycloak_realm.sw360.id
  alias        = "azure-foss360"
  display_name = "Login with EntraID"

  authorization_url  = "https://login.microsoftonline.com/${var.tenant}/oauth2/v2.0/authorize"
  token_url          = "https://login.microsoftonline.com/${var.tenant}/oauth2/v2.0/token"
  logout_url         = "https://login.microsoftonline.com/${var.tenant}/oauth2/v2.0/logout"
  user_info_url      = "https://graph.microsoft.com/oidc/userinfo"
  issuer             = "https://login.microsoftonline.com/${var.tenant}/v2.0"
  validate_signature = true
  jwks_url           = "https://login.microsoftonline.com/${var.tenant}/discovery/v2.0/keys"

  client_id     = var.azure_client_id
  client_secret = var.azure_client_secret
  extra_config = {
    clientAuthMethod = "client_secret_post"
  }

  trust_email                   = true
  first_broker_login_flow_alias = keycloak_authentication_flow.first_login.alias
  sync_mode                     = "IMPORT"
}

# 2. Map `org_code` from EntraID to `Department` of user
resource "keycloak_attribute_importer_identity_provider_mapper" "department" {
  realm                   = keycloak_realm.sw360.id
  name                    = "OrgCodeToDepartmentMapper"
  identity_provider_alias = keycloak_oidc_identity_provider.entra_id.alias
  user_attribute          = "Department"
  claim_name              = "org_code"

  extra_config = {
    syncMode = "FORCE"
  }
}

# 3. Map `uid` claim from EntraID to `externalId` of user
resource "keycloak_attribute_importer_identity_provider_mapper" "uid" {
  realm                   = keycloak_realm.sw360.id
  name                    = "UidToExternalIdMapper"
  identity_provider_alias = keycloak_oidc_identity_provider.entra_id.alias
  user_attribute          = "externalId"
  claim_name              = "uid"

  extra_config = {
    syncMode = "IMPORT"
  }
}

# 4. Map `mail` claim from EntraID to `email` of user
resource "keycloak_attribute_importer_identity_provider_mapper" "email" {
  realm                   = keycloak_realm.sw360.id
  name                    = "MailToEmailMapper"
  identity_provider_alias = keycloak_oidc_identity_provider.entra_id.alias
  user_attribute          = "email"
  claim_name              = "mail"

  extra_config = {
    syncMode = "IMPORT"
  }
}

# 5. Map `mail` claim from EntraID to `username` of user
resource "keycloak_attribute_importer_identity_provider_mapper" "username" {
  realm                   = keycloak_realm.sw360.id
  name                    = "MailToUsernameMapper"
  identity_provider_alias = keycloak_oidc_identity_provider.entra_id.alias
  user_attribute          = "username"
  claim_name              = "mail"

  extra_config = {
    syncMode = "IMPORT"
  }
}
