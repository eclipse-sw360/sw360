# SPDX-License-Identifier: Siemens-ISL-1.5
# Holds the list of clients for SW360

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
