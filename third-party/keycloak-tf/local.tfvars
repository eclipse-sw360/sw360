# Variable template for TF scripts
# SPDX-License-Identifier: Siemens-ISL-1.5

# Client ID and secret to use for connection to KeyCloak
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
# Azure IDP connection configuration
tenant              = "38ae3bcd-9579-4fd4-adda-b42e1495d55a"
azure_client_id     = "azure-idp-client-id"
azure_client_secret = "azure-idp-client-secret"
# Uncomment following to setup SMTP using smtp.siemens.com
# smtp_from     = "sw360.server.in@siemens.com"
# smtp_username = "my-smtp-user"
# smtp_password = "my-smtp-password"
# Grafana dashboard URL
dashboard_base_url = "http://localhost:3000/dashboard"
