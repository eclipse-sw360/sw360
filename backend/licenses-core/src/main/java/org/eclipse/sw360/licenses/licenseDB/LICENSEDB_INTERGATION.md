## LICENSEDB Rest Integration Layer

The integration layer with LicenseDB will be mainly through REST API integration with OAuth calls. The integration layer will built and implemented inside the backend/licenses-core servlet as a Rest client class, the reason for that is to ensure a complete separation of concerns and for ease of integration with the implemented licenses-core methods that provide a direct integration with couchdb. 

### 1. Components

For an efficient implementation of the LicenseDB integration layer the following components will be implemented:



- **backend/licenses-core/src/main/…/licensedb/config/LicenseDBConfig.java**: Class will contain the configuration for the Rest template and Container Configuration repository
- **backend/licenses-core/src/main/…/licensedb/config/LicenseDBRestConfig**: Class will contain the client configurations and integration with the Container Configuration repository to fetch the following configurations:
  - CONFIG_LICENSEDB_USERNAME: username for OAuth call
  - CONFIG_LICENSEDB_PASSWORD: password for OAuth call
  - CONFIG_LICENSEDB_BASE_URL: Base uri host for licenseDB
- **backend/licenses-core/src/main/…/licensedb/rest/LicenseDBRestClient.java**: Contains the full implementation for LicenseDB rest client methods for the following API endpoints:
  /licenses
  /license/id
  /obligations
  ….
- **backend/licenses-core/dtos**: Will contain the dto definitions for the request and response payloads for LicenseDB Rest API integration, sample DTO files will include:
  License_db
  Obligation_db

The LicenseDB rest client layer implementation follows the same paradigm as the Fossology rest client layer, where the rest client configuration will be resident inside the Configuration repository.

### 2. OAuth Integration

The machine-to-machine oAuth layer will be simply through OAuth calls implemented within the rest client implementation class for the following endpoints:
/login: Generate new token
/refresh-token: refresh existing token

The logic for checking the verification and expiry of a token would be implemented inside the rest client configuration class as a helper method.

Inside each request method implementation the token validation takes place through the helper methods and the implemented OAuth methods and accordingly the Auth token is inserted inside the Auth header for the request.

This REST API interface layer will be used across the license-core implemented methods to successfully sync between LicenseDB and the internal CouchDB.
