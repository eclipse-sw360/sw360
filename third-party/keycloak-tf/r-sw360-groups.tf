# SPDX-License-Identifier: Siemens-ISL-1.5
# SW360 user groups used by roles

resource "keycloak_group" "sw360_admin" {
  realm_id = keycloak_realm.sw360.id
  name     = "ADMIN"
}

resource "keycloak_group" "sw360_clearing_admin" {
  realm_id = keycloak_realm.sw360.id
  name     = "CLEARING_ADMIN"
}

resource "keycloak_group" "sw360_clearing_expert" {
  realm_id = keycloak_realm.sw360.id
  name     = "CLEARING_EXPERT"
}

resource "keycloak_group" "sw360_ecc_admin" {
  realm_id = keycloak_realm.sw360.id
  name     = "ECC_ADMIN"
}

resource "keycloak_group" "sw360_security_admin" {
  realm_id = keycloak_realm.sw360.id
  name     = "SECURITY_ADMIN"
}

resource "keycloak_group" "sw360_sw360_admin" {
  realm_id = keycloak_realm.sw360.id
  name     = "SW360_ADMIN"
}

resource "keycloak_group" "sw360_user" {
  realm_id = keycloak_realm.sw360.id
  name     = "USER"
}
