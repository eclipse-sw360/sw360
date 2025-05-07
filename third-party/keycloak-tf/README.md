# 1. Create OIDC client for master realm

Follow instructions from
https://registry.terraform.io/providers/keycloak/keycloak/latest/docs#client-credentials-grant-setup-recommended
and get the `client_id` and `client_secret`.

# 2. Setup prod.auto.tfvars

Copy the `local.tfvars` as `prod.auto.tfvars` and fill the variables.

# 3. Apply

1. `tofu init`
2. `tofu plan -out plan.out`
3. `tofu apply plan.out`

# 4. Note

The script requires following files which contains secret and should **never**
be commited to the repository:
1. The `*.auto.tfvars` file hold the variables required for the terraform
   scripts to run. This includes some secrets as well. The file name here does
   not matter as long as it ends with `.auto.tfvars`, TF will autoload it.
2. The `terraform.tfstate` is the state file used by Terraform to manage the
   known state of the configuration. It should be maintained once setup on a KC
   server, preventing unnecessary creations/duplications and match the
   configuration of KC. The file also contains any secret in plain text!

# Creating new user clients

1. Edit the `l-sw360-clients-list.tf` and create a new entry in either
   `sw360_read_clients` (for read only clients) or `sw360_write_clients` (for
   read/write clients).
2. Make sure each entry has the 4 keys, namely:
    1. `user_email`: for whom the client is being created.
    2. `creator_email`: admin who is creating the client.
    3. `user_group`: which BU user belongs to (FT, BT, etc.)
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
