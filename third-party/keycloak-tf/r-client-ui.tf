# Copyright (c) Siemens AG 2025.
# SPDX-License-Identifier: EPL-2.0
# Part of the SW360 Portal Project.
# Create client for SW360 UI

# 1. Client for SW360 UI to authenticate
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

# 2. Client for grafana dashboard
resource "keycloak_openid_client" "grafana" {
  realm_id  = keycloak_realm.sw360.id
  client_id = "grafana"
  enabled   = true

  name        = "Grafana"
  description = "Login for Grafana Dashboard"
  base_url    = var.dashboard_base_url

  valid_redirect_uris             = ["${var.dashboard_base_url}/login/generic_oauth"]
  valid_post_logout_redirect_uris = ["+"]
  web_origins                     = [var.dashboard_base_url]

  access_type                  = "CONFIDENTIAL"
  client_authenticator_type    = "client-secret"
  standard_flow_enabled        = true
  direct_access_grants_enabled = true
  implicit_flow_enabled        = false

  frontchannel_logout_enabled = true
}
