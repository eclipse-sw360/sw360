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
