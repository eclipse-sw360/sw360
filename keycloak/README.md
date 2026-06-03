# SW360 Keycloak Integration

SW360 integrates with Keycloak to provide a robust Identity and Access
Management (IAM) solution. The solution directly communicates with CouchDB
using the SW360 `datahandler` library.

## Architecture Overview

The integration consists of two primary custom Service Provider Interface (SPI)
components that interact directly with the SW360 user database (CouchDB) using
the SW360 `datahandler` library:

1. **User Storage Provider (Federation)**: Allows Keycloak to look up users and
   validate credentials directly against the SW360 CouchDB.
2. **Custom Event Listener (Synchronization)**: Listens for user-related events
   in Keycloak (e.g., registration, profile updates) and synchronizes them to
   the SW360 database.

### Synchronization Flow
* **Keycloak to CouchDB**: Handled by the Event Listener (triggers on `REGISTER`,
  `UPDATE`, `ADMIN_EVENT`).
* **CouchDB to Keycloak**: Handled by the User Storage Provider (triggers on
  login or user lookup).

---

## 1. User Storage Provider
The `sw360-keycloak-user-storage-provider` allows Keycloak to federate users
from an existing SW360 instance.

* **User Lookup**: Supports finding users by ID (CouchDB document ID), Email, or
  Username.
* **Credential Validation**: Validates passwords stored in CouchDB using the
  same cryptographic logic as the SW360 backend.
* **Role Mapping**: Maps Keycloak group memberships (e.g., `ADMIN`,
  `CLEARING_ADMIN`) to the internal `UserGroup` model in SW360.

## 2. Custom Event Listener
The `sw360-keycloak-event-listener` ensures that any lifecycle events occurring
within Keycloak are propagated to SW360.

* **Registration**: When a new user registers via Keycloak, the listener
  automatically creates a corresponding user record in CouchDB with default
  permissions.
* **Profile Updates**: Changes to user attributes (First Name, Last Name, Email,
  Department) are instantly updated in the SW360 database.
* **Group Membership**: Modification of user groups in Keycloak results in the
  update of the `UserGroup` field in CouchDB.

---

## Configuration

The SPI providers share configuration with the SW360 system. They read
connection details directly from the standard SW360 configuration path:

* **Primary Path**: `/etc/sw360/couchdb.properties`

Ensure that the Keycloak process has read access to this file and that the
CouchDB credentials provided also have write access to the `users` database.

---

## Build and Deployment

### Building the Providers
To build the Keycloak SPI JARs, use the following Maven command from the root of
the SW360 repository:

```bash
mvn clean install -DskipTests -Pdeploy -Dlistener.deploy.dir=/path/to/keycloak/providers
```

### Deployment
Once built, the following JARs (and their dependencies) must be present in the
Keycloak `providers/` directory (e.g., `/opt/keycloak/providers/`):

* `sw360-keycloak-event-listener.jar`
* `sw360-keycloak-user-storage-provider.jar`
* Dependency JARs (e.g., `datahandler-*.jar`, `backend-common-*.jar`)

After copying the JARs, Keycloak must be restarted to recognize the new providers.

---

## Automated Deployment Note
In modern SW360 deployments, the entire Keycloak configuration including Realm
setup, Client creation, Group definitions, and SPI registration is automated via
**Terraform**.

For detailed installation instructions, please refer to:
* [Keycloak Terraform README](../ui/sw360-frontend/config/keycloak/README.md)
* [SW360 Documentation - Keycloak Authentication](https://www.eclipse.org/sw360/docs/deployment/deploy-keycloak-authentication/)
