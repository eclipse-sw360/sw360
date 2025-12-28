# Copyright (c) Siemens AG 2025.
# SPDX-License-Identifier: EPL-2.0
# Part of the SW360 Portal Project.
# The main SW360 realm

# 1. sw360 realm in KeyCloak
resource "keycloak_realm" "sw360" {
  realm        = "sw360"
  enabled      = true
  display_name = "SW360 UI"

  ssl_required = "external"

  # Login tab
  registration_allowed           = false
  edit_username_allowed          = false
  registration_email_as_username = true
  login_with_email_allowed       = false
  duplicate_emails_allowed       = false

  # Email tab if smtp_password is configured
  dynamic "smtp_server" {
    for_each = var.smtp_password != null ? [1] : []
    content {
      host              = var.smtp_host
      port              = var.smtp_port
      from              = var.smtp_from
      from_display_name = "SW360 - Keycloak"
      starttls          = true
      ssl               = false

      auth {
        username = var.smtp_username
        password = var.smtp_password
      }
    }
  }

  # Themes tab - leave default (user should not see)

  # Events tab - resource

  # Localization tab
  internationalization {
    supported_locales = [
      "en",
      "de",
      "es"
    ]
    default_locale = "en"
  }

  # Security defenses tab
  security_defenses {
    headers {
      x_frame_options                     = "SAMEORIGIN"
      content_security_policy             = "frame-src 'self'; frame-ancestors 'self'; object-src 'none';"
      content_security_policy_report_only = ""
      x_content_type_options              = "nosniff"
      x_robots_tag                        = "none"
      x_xss_protection                    = "1; mode=block"
      strict_transport_security           = "max-age=31536000; includeSubDomains"
    }
  }

  # Sessions tab
  sso_session_idle_timeout    = "3h"
  sso_session_max_lifespan    = "10h"
  client_session_max_lifespan = "2h"

  # Tokens tab
  access_token_lifespan = "1h"
}

# 2. Events setting to setup sw360-add-user-to-couchdb listener
# This listener takes changes from KC to CouchDB
resource "keycloak_realm_events" "sw360_events" {
  realm_id = keycloak_realm.sw360.id

  events_enabled    = true
  events_expiration = 3600

  admin_events_enabled         = true
  admin_events_details_enabled = false

  events_listeners = [
    "jboss-logging",
    "sw360-add-user-to-couchdb"
  ]
}

# 3. User profiles with attributes username, email, firstName, lastName,
# Department and externalId
resource "keycloak_realm_user_profile" "sw360_profiles" {
  realm_id = keycloak_realm.sw360.id

  unmanaged_attribute_policy = "ENABLED"

  attribute {
    name         = "username"
    display_name = "$${username}"
    permissions {
      edit = ["admin", "user"]
      view = ["admin", "user"]
    }
    validator {
      name   = "length"
      config = { "min" : 3, "max" : 255 }
    }
    validator {
      name   = "username-prohibited-characters"
      config = {}
    }
    validator {
      name   = "up-username-not-idn-homograph"
      config = {}
    }
  }

  attribute {
    name         = "email"
    display_name = "$${email}"
    permissions {
      edit = ["admin", "user"]
      view = ["admin", "user"]
    }
    required_for_roles = ["admin", "user"]
    validator {
      name   = "length"
      config = { "max" : 255 }
    }
    validator {
      name   = "email"
      config = {}
    }
  }

  attribute {
    name         = "firstName"
    display_name = "$${firstName}"
    permissions {
      edit = ["admin", "user"]
      view = ["admin", "user"]
    }
    required_for_roles = ["admin", "user"]
    validator {
      name   = "length"
      config = { "max" : 255 }
    }
    validator {
      name   = "person-name-prohibited-characters"
      config = {}
    }
  }

  attribute {
    name         = "lastName"
    display_name = "$${lastName}"
    permissions {
      edit = ["admin", "user"]
      view = ["admin", "user"]
    }
    required_for_roles = ["admin", "user"]
    validator {
      name   = "length"
      config = { "max" : 255 }
    }
    validator {
      name   = "person-name-prohibited-characters"
      config = {}
    }
  }

  attribute {
    name         = "Department"
    display_name = "Department"
    permissions {
      edit = ["admin", "user"]
      view = ["admin", "user"]
    }
  }

  attribute {
    name         = "externalId"
    display_name = "externalId"
    permissions {
      edit = ["admin", "user"]
      view = ["admin", "user"]
    }
  }
}

# 4. Default role of users for the realm
resource "keycloak_default_groups" "default" {
  realm_id = keycloak_realm.sw360.id
  group_ids = [
    keycloak_group.sw360_groups["USER"].id
  ]
}
