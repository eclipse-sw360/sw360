# SPDX-License-Identifier: Siemens-ISL-1.5
# SW360 user groups used by roles

locals {
  sw360_groups = toset([
    "USER", "ADMIN", "CLEARING_ADMIN", "ECC_ADMIN", "SECURITY_ADMIN",
    "SW360_ADMIN", "CLEARING_EXPERT"
  ])
}

resource "keycloak_group" "sw360_groups" {
  for_each = local.sw360_groups
  realm_id = keycloak_realm.sw360.id
  name     = each.key
}
