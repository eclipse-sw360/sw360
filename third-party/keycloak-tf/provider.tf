# SPDX-License-Identifier: Siemens-ISL-1.5
# Keycloak TF provider

terraform {
  required_providers {
    keycloak = {
      source  = "keycloak/keycloak"
      version = "~> 5"
    }
  }

  required_version = "~> 1.7"

  backend "local" {}
}

provider "keycloak" {
  client_id     = var.kc_client_id
  client_secret = var.kc_client_secret
  url           = var.kc_base_url
  realm         = "master"
}
