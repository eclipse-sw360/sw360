# Copyright (c) Siemens AG 2025.
# SPDX-License-Identifier: EPL-2.0
# Part of the SW360 Portal Project.
# Variable template for TF scripts

# Client ID and secret to use for connection to KeyCloak as an KC Admin
kc_client_id     = "tofu"
kc_client_secret = "master-realm-tofu-secret"
# Where KeyCloak is running
kc_base_url = "http://localhost:8083"
# Redirect URIs to allow for SW360 client
redirect_uris = [
  "http://localhost:3000/api/auth/callback/keycloak"
]
# SW360 frontend URL
frontend_base_url = "http://localhost:3000"
# Azure IDP connection configuration (if used)
tenant              = "azure-ad-tenant"
azure_client_id     = "azure-idp-client-id"
azure_client_secret = "azure-idp-client-secret"
# Uncomment following to setup SMTP in KeyCloak for sending emails
# smtp_from     = "admin@sw360.org"
# smtp_username = "my-smtp-user"
# smtp_password = "my-smtp-password"
# smtp_host     = "smtp.sw360.org"
# smtp_port     = 25
# Grafana dashboard URL (if setup)
dashboard_base_url = "http://localhost:3000/dashboard"
