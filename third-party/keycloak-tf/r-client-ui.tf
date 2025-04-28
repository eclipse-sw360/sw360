# SPDX-License-Identifier: Siemens-ISL-1.5
# Create client for SW360 UI

resource "keycloak_openid_client" "sw360_ui" {
  realm_id  = keycloak_realm.sw360.id
  client_id = "sw360ui"
  enabled   = true

  name        = "SW360 Frontend"
  description = "NextJS based SW360 frontend"
  base_url    = var.frontend_base_url

  valid_redirect_uris             = var.redirect_uris
  valid_post_logout_redirect_uris = ["+"]
  web_origins                     = ["*"]

  access_type                  = "CONFIDENTIAL"
  client_authenticator_type    = "client-secret"
  standard_flow_enabled        = true
  direct_access_grants_enabled = false

  frontchannel_logout_enabled = true
}
