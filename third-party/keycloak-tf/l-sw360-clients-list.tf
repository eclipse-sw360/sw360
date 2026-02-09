# Copyright (c) Siemens AG 2025.
# SPDX-License-Identifier: EPL-2.0
# Part of the SW360 Portal Project.
# Holds the list of clients for SW360

/*
Use these clients to generate KeyCloak keys for individual users to interact
with SW360 REST API.
The `sw360_read_clients` will have read-only access (GET).
The `sw360_write_clients` will have read-write access (ALL).

`user_email`:    User for home the token is to be generated.
`creator_email`: User who authorized the token generation.
`user_group`:    Group to which the user belongs.
`creation_date`: Date when the token was added.

These information are used in documentation in the KeyCloak OpenID Client as:
"<user_group>-<user_email>-<creator_email>-<creation_date>"
 */

locals {
  sw360_read_clients = [
    {
      user_email    = "mishra.gaurav@siemens.com"
      creator_email = "mishra.gaurav@siemens.com"
      user_group    = "FT"
      creation_date = "2025-05-07"
    }
  ]
  sw360_write_clients = [
    {
      user_email    = "amrit.verma@siemens.com"
      creator_email = "mishra.gaurav@siemens.com"
      user_group    = "FT"
      creation_date = "2025-05-07"
    }
  ]

  sw360_clients = concat(local.sw360_read_clients, local.sw360_write_clients)
}
