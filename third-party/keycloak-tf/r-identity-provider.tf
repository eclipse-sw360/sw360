# SPDX-License-Identifier: Siemens-ISL-1.5
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
    syncMode = "INHERIT"
  }
}
