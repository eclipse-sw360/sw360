<!--
Copyright (c) Siemens AG 2025.
SPDX-License-Identifier: EPL-2.0
Part of the SW360 Portal Project.
-->
# Setup KeyCloak to work with SW360 with all custom scopes and setup

This Terraform repo contains the scripts which help setup KeyCloak to be used
with SW360 as a simple script. The IaC mode of Terraform also helps keep both
the systems in sync and document each step in the process.

Following guide helps setup the repo to be sed with your KeyCloak installation.

#### Disclaimer

The Terraform scripts are provided here as reference to help speed up the setup
of KeyCloak for SW360. These scripts should be considered as complete or correct
and should only be used as a reference to scripts created to maintain your own
setup.

# Setup Terraform Repo

## 1. Create OIDC client for master realm

Follow instructions from in Client Credentials Grant Setup mode
https://registry.terraform.io/providers/keycloak/keycloak/latest/docs#client-credentials-grant-setup-recommended
and get the `client_id` and `client_secret`.

## 2. Setup prod.auto.tfvars

Copy the `local.tfvars` as `prod.auto.tfvars` and fill the variables. The files
`.auto.tfvars` are loaded by default by Terraform clients and provide better way
to control variables for different setup. The repo already ignores all
`*.auto.tfvars` in this folder to prevent accidental commit to git as it
contains your secrets.

### 2. a. Variables in tfvars

| Variable              | Example value                   | Description                                                                               |
|-----------------------|---------------------------------|-------------------------------------------------------------------------------------------|
| `kc_client_id`        | kc_client                       | Client ID created in step 1.                                                              |
| `kc_client_secret`    | kc_secret                       | Client secret created in step 1.                                                          |
| `kc_base_url`         | http://localhost:8083           | Base URL where KC is running (for accessing REST API).                                    |
| `redirect_uris`       | callback                        | Base URLs which are allowed for the realm to authenticate. Provided by the SW360-frontend |
| `frontend_base_url`   | http://localhost:3000           | Base URL where front-end is running.                                                      |
| `tenant`              | azure-id-123                    | Tenant ID of the Azure EntraID to be used as IdP.                                         |
| `azure_client_id`     | azure-IdP-client-id             | Client ID from Azure EntraID to be used for IdP setup.                                    |
| `azure_client_secret` | azure-IdP-client-secret         | Client Secret from Azure EntraID to be used for IdP setup.                                |
| `smtp_from`           | admin@sw360.org                 | Address from which the email should come from.                                            |
| `smtp_username`       | my-smtp-user                    | Username for authenticating with SMTP server.                                             |
| `smtp_password`       | my-smtp-password                | Password for authenticating with SMTP server.                                             |
| `smtp_host`           | smtp.sw360.org                  | Hostname of SMTP server to connect with.                                                  |
| `smtp_port`           | 25                              | SMTP port to be used.                                                                     |
| `dashboard_base_url`  | http://localhost:3000/dashboard | Base URL of Grafana Dashboard, if used, for authentication.                               |

## 3. Apply the Terraform scripts

1. `tofu init`
    - Initialize the Terraform modules.
2. `tofu plan -out plan.out`
    - Plan the actions. Make sure to review everything here.
3. `tofu apply plan.out`
    - If plan looks good, apply it.

## 4. Note

Some notes and tips on using these Terraform scripts for your setup.
1. Always make changes via Terraform if used once. This practice keeps your
   changes as part of IaC and prevents the Terraform state from going out of
   sync and overwriting your changes in the next sync.
2. The `*.auto.tfvars` file hold the variables required for the terraform
   scripts to run. This includes some secrets as well. The file name here does
   not matter as long as it ends with `.auto.tfvars`, TF will autoload it.
3. The `terraform.tfstate` is the state file used by Terraform to manage the
   known state of the configuration.
    - The state file should be maintained once setup on a KC server, preventing
      unnecessary creations/duplications and match the configuration of KC.
    - The file could also contain any secret in plain text!
    - The best practice is to store this file on a remote server which can be
      accessed by other team member. Follow the
      [official guide](https://developer.hashicorp.com/terraform/language/state/remote-state-data)
      from Terraform to find solution best suited for your need. This base repo
      only provides the default storage on local disk and is **not** the best
      solution.
4. The default script is created to use Microsoft Azure's EntraID as the
   identity provider. And thus, the KC flows are designed to by-pass the login
   window and use this IdP always.
    - The IdP claims are used in `r-identity-provider.tf` as following:
    - `OrgCodeToDepartmentMapper` uses `org_code` claim to fill `Department`
      attribute of the user.
    - `UidToExternalIdMapper` uses `uid` claim to fill `externalId` attribute
      of the user.
    - `MailToEmailMapper` uses `mail` claim to fill `email` attribute.
    - `MailToUsernameMapper` uses `mail` claim to fill `username` attribute.
    - These claims can be customized as per your EntraID tenant. But it is
      **highly recommended** to keep the `MailToEmailMapper` and
      `MailToUsernameMapper` as is to use mail in email & username fields.

## Creating new user clients

SW360 provides a robust REST API which users would like to access from their
own client, apart from the SW360 front-end project. The helper script
`l-sw360-clients-list.tf` provide easy way to create OpenID Clients in KeyCloak
with Client Credentials grant which can be used by the SW360 users to
communicate with SW360 REST API with authentication from KeyCloak.

1. Edit the `l-sw360-clients-list.tf` and create a new entry in either
   `sw360_read_clients` (for read only clients) or `sw360_write_clients` (for
   read/write clients).
2. Make sure each entry has the 4 keys, namely:
    1. `user_email`: for whom the client is being created.
    2. `creator_email`: admin who is creating the client.
    3. `user_group`: which department user belongs to (DEP1, etc.)
    4. `creation_date`: when the client creation was approved in `YYYY-MM-DD`.
3. Run `tofu fmt` and commit the file and run the script on corresponding server.
4. Goto KeyCloak > sw360 realm > Clients > Find the new client >
   Copy the "Client ID" > Goto Credentials > Copy the "Client Secret".
5. User can now generate the token with following config:
    ```
    Token Type: OAuth2
    Grant Type: Client Credentials
    Client ID: copied from step 4
    Client Secret: copied from step 4
    Token URL: https://<keycloak-server>/realms/sw360/protocol/openid-connect/token
    Scope to get read only token: "openid email READ"
    Scope to get read/write token: "openid email READ WRITE"
    ```

# Using KeyCloak to configure Frontend and Backend

After following the steps mentioned above and running `tofu apply`, your
KeyCloak server should be now ready with everything. The next steps is to
configure your SW360 frontend and backend to communicate with KC.

1. In your Frontend, create the `.env` file
    - In KC UI, find the "sw360" realm and look for client called "sw360ui".
    - Use the configurations here to setup your `.env` file.
2. In your Backend, modify the `/etc/sw360/sw360.properties`
    - From the "sw360" realm, find your configurations to be filled in:
      ```
      jwks.validation.enabled=true
      jwks.issuer.url=http://localhost:8083/kc/realms/sw360
      jwks.endpoint.url=http://localhost:8083/kc/realms/sw360/protocol/openid-connect/certs
      ```
