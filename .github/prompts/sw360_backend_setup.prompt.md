---
agent: agent
---
Provide step-by-step instructions to deploy a SW360 Backend application using Apache Tomcat 11 and Keycloak 26 as an identity provider. This guide covers installation, configuration, building, and deployment.

Follow these steps:

1. **Confirm Installation Paths:**
  Before proceeding, ask the user to confirm whether the required software is already installed and obtain the actual installation paths. Use these paths throughout the deployment process.

  **Ask the user for each component:**
  - **Tomcat 11**: Is Tomcat already installed? If yes, provide the installation path (e.g., `/opt/tomcat`, `/opt/apache-tomcat-11.x.x`, or `/usr/local/tomcat`)
  - **Keycloak 26**: Is Keycloak already installed? If yes, provide the installation path (e.g., `/opt/keycloak`, `/opt/keycloak-26.x.x`, or `/usr/local/keycloak`)
  - **Maven**: Is Maven already installed? If yes, provide the path (e.g., `/opt/apache-maven`, `/usr/bin/mvn`, or system Maven via `mvn`)
  - **Application Path**: Confirm SW360 workspace path (default: `/home/$(whoami)/workspace/sw360`)
  - **SW360 Config Directory**: Confirm config directory (default: `/etc/sw360`)

  **Store these paths as variables for later use:**
  ```bash
# Example - adjust based on user responses:
export TOMCAT_HOME="/opt/tomcat"           # User's Tomcat installation path
export KEYCLOAK_HOME="/opt/keycloak"       # User's Keycloak installation path
export MAVEN_CMD="/opt/apache-maven/bin/mvn"  # Maven command (could be just "mvn" if system-installed)
export SW360_APP_PATH="/home/$(whoami)/workspace/sw360"  # SW360 application path
export SW360_CONFIG_DIR="/etc/sw360"       # SW360 config directory
  ```
  Note: `$(whoami)` resolves to the logged-in Ubuntu user; keep the home path consistent with that account.

  **Verify the provided paths exist:**
  ```bash
# Check Tomcat installation
ls -la "$TOMCAT_HOME" 2>/dev/null && echo "Tomcat found at $TOMCAT_HOME" || echo "ERROR: Tomcat not found at $TOMCAT_HOME"

# Check Keycloak installation
ls -la "$KEYCLOAK_HOME" 2>/dev/null && echo "Keycloak found at $KEYCLOAK_HOME" || echo "ERROR: Keycloak not found at $KEYCLOAK_HOME"

# Check Maven installation
$MAVEN_CMD -version 2>/dev/null && echo "Maven found" || echo "ERROR: Maven not found at $MAVEN_CMD"

# Check SW360 application path
ls -la "$SW360_APP_PATH" 2>/dev/null && echo "SW360 app found at $SW360_APP_PATH" || echo "ERROR: SW360 not found at $SW360_APP_PATH"

# Check SW360 config directory (will create later if missing)
ls -la "$SW360_CONFIG_DIR" 2>/dev/null && echo "SW360 config found at $SW360_CONFIG_DIR" || echo "SW360 config directory will be created"
  ```

  **Additional Configuration (use defaults if user doesn't specify otherwise):**
  - Keycloak Port: `8083`
  - Keycloak Realm: `sw360`
  - CouchDB Port: `5984`
  - PostgreSQL Port: `5432`

2. **Verify and Clone SW360 Source Code:**
  Check if the SW360 source code exists at the specified application path. If not, clone it from the GitHub repository.

  ```bash
# Check if SW360 source code exists at the specified path
if [ -d "$SW360_APP_PATH" ] && [ -f "$SW360_APP_PATH/pom.xml" ]; then
    echo "SW360 source code found at $SW360_APP_PATH"
    cd "$SW360_APP_PATH"
    # Optionally show current branch and status
    git branch --show-current 2>/dev/null && echo "Current branch: $(git branch --show-current)"
    git status -s 2>/dev/null || echo "Not a git repository or git not initialized"
else
    echo "SW360 source code not found. Cloning from GitHub..."

    # Create parent directory if it doesn't exist
    mkdir -p "$(dirname "$SW360_APP_PATH")"

    # Clone the SW360 repository
    git clone https://github.com/eclipse-sw360/sw360.git "$SW360_APP_PATH"

    # Navigate to the cloned directory
    cd "$SW360_APP_PATH"

    # Install pre-commit hooks (if Python and pip are available)
    if command -v pip &> /dev/null || command -v pip3 &> /dev/null; then
        pip install pre-commit 2>/dev/null || pip3 install pre-commit 2>/dev/null || echo "pre-commit installation skipped"
        pre-commit install 2>/dev/null || echo "pre-commit hook installation skipped"
    else
        echo "Python pip not found. Skipping pre-commit installation."
    fi

    echo "SW360 source code successfully cloned to $SW360_APP_PATH"
fi

# Verify pom.xml exists (indicates valid SW360 project)
if [ ! -f "$SW360_APP_PATH/pom.xml" ]; then
    echo "ERROR: pom.xml not found at $SW360_APP_PATH. This may not be a valid SW360 project directory."
    exit 1
fi
  ```

  **Note:** The SW360 repository will be cloned from the official Eclipse repository: `https://github.com/eclipse-sw360/sw360.git`

3. **Verify Required Software and Versions:**
  Ensure the required tools are installed and at compatible versions using the paths confirmed in step 1. If a tool is missing or the version is mismatched, proceed to step 4 for installation commands.

  ```bash
  # Java: must include JDK 21 (use alternatives if multiple)
  update-alternatives --list java || echo "No Java alternatives"
  java -version || echo "Java not found"

  # Maven (use the confirmed Maven command)
  $MAVEN_CMD -version || echo "Maven not found"

  # Git
  git --version || echo "Git not found"

  # Tomcat (use the confirmed Tomcat path)
  [ -f "$TOMCAT_HOME/version.txt" ] && cat "$TOMCAT_HOME/version.txt" || ls -la "$TOMCAT_HOME"

  # Keycloak (use the confirmed Keycloak path)
  [ -f "$KEYCLOAK_HOME/version.txt" ] && cat "$KEYCLOAK_HOME/version.txt" || ls -la "$KEYCLOAK_HOME"

  # CouchDB (expects 5984 with admin credentials)
  curl -s http://localhost:5984/ || echo "CouchDB not reachable on 5984"
  # Check CouchDB authentication
  curl -s http://admin:12345@localhost:5984/ 2>/dev/null && echo "CouchDB authenticated" || echo "CouchDB auth failed"

  # PostgreSQL (expects 5432)
  psql --version || echo "psql client missing"
  sudo -u postgres pg_lsclusters || echo "No Postgres clusters listed"
  ss -lntp | grep ":5432" || echo "No process listening on 5432"
  ```

  If Java has multiple versions, select JDK 21 for the build:
  ```bash
  export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
  echo "Using JAVA_HOME=$JAVA_HOME"
  $JAVA_HOME/bin/java -version
  ```

  If PostgreSQL is installed but the cluster is down, start it and ensure the `keycloak` DB and user exist (do NOT change credentials in Keycloak):
  ```bash
  # Start the default Ubuntu cluster (example for 14/main)
  sudo pg_ctlcluster 14 main start
  sudo -u postgres pg_lsclusters

  # Create user and DB if missing (credentials must match /opt/keycloak/conf/keycloak.conf)
  sudo -u postgres psql -tc "SELECT 1 FROM pg_roles WHERE rolname='keycloak'" | grep -q 1 || sudo -u postgres createuser -P keycloak
  sudo -u postgres psql -tc "SELECT 1 FROM pg_database WHERE datname='keycloak'" | grep -q 1 || sudo -u postgres createdb -O keycloak keycloak
  ```

  Keycloak providers hygiene (avoid duplicate jars causing split packages):
  ```bash
  # Remove known duplicates if present (keep newest versions) - use confirmed Keycloak path
  sudo rm -f "$KEYCLOAK_HOME/providers/commonIO-19.1.0.jar" || true
  sudo rm -f "$KEYCLOAK_HOME/providers/datahandler-19.1.0.jar" || true
  sudo rm -f "$KEYCLOAK_HOME/providers/spring-security-crypto-6.4.4.jar" || true
  sudo rm -f "$KEYCLOAK_HOME/providers/httpcore5-5.3.4.jar" || true
  sudo rm -f "$KEYCLOAK_HOME/providers/jakarta.xml.bind-api-4.0.2.jar" || true
  # Ensure providers directory ownership allows Maven copy
  sudo chown -R $USER:$USER "$KEYCLOAK_HOME/providers"
  ```

4. **Install Missing Software (If Not Present or Version Mismatched):**
  The following commands install or upgrade the required tools on Ubuntu. Adjust versions/paths as needed. When multiple Java versions are installed, always use Java 21 via `update-alternatives`.

  ```bash
  # Java 21 (OpenJDK)
  sudo apt update
  sudo apt install -y openjdk-21-jdk
  sudo update-alternatives --config java   # choose Java 21
  export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

  # Maven (apt or manual install)
  sudo apt install -y maven || true
  # If you prefer Apache Maven in /opt
  MAVEN_VERSION=3.9.9
  curl -fsSL https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz -o /tmp/maven.tgz
  sudo mkdir -p /opt/apache-maven
  sudo tar -xzf /tmp/maven.tgz -C /opt/apache-maven --strip-components=1
  /opt/apache-maven/bin/mvn -version

  # Git
  sudo apt install -y git

  # Tomcat 11 (extract to /opt/tomcat)
  TOMCAT_VERSION=11.0.2
  curl -fsSL https://downloads.apache.org/tomcat/tomcat-11/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz -o /tmp/tomcat.tgz
  sudo mkdir -p /opt/tomcat
  sudo tar -xzf /tmp/tomcat.tgz -C /opt/tomcat --strip-components=1
  sudo chown -R $USER:$USER /opt/tomcat
  /opt/tomcat/bin/version.sh || cat /opt/tomcat/version.txt || ls -la /opt/tomcat

  # Keycloak 26 (extract to /opt/keycloak)
  KEYCLOAK_VERSION=26.0.0
  curl -fsSL https://github.com/keycloak/keycloak/releases/download/${KEYCLOAK_VERSION}/keycloak-${KEYCLOAK_VERSION}.tar.gz -o /tmp/keycloak.tgz
  sudo mkdir -p /opt/keycloak
  sudo tar -xzf /tmp/keycloak.tgz -C /opt/keycloak --strip-components=1
  sudo chown -R $USER:$USER /opt/keycloak
  ls -la /opt/keycloak

  # PostgreSQL 14 (specifically version 14 for compatibility)
  sudo apt install -y postgresql-14 postgresql-contrib-14
  # Remove any old clusters if necessary
  sudo pg_dropcluster --stop 18 main 2>/dev/null || true
  sudo pg_dropcluster --stop 16 main 2>/dev/null || true
  # Create and start PostgreSQL 14 cluster
  sudo pg_createcluster 14 main --start || sudo pg_ctlcluster 14 main start
  sudo -u postgres pg_lsclusters
  # Create the Keycloak user/db to match /opt/keycloak/conf/keycloak.conf
  # Note: Use 'password' as per setup document (or 'keycloak' if already configured in keycloak.conf)
  sudo -u postgres psql -c "CREATE USER keycloak WITH ENCRYPTED PASSWORD 'password';" 2>/dev/null || echo "User already exists"
  sudo -u postgres psql -c "CREATE DATABASE keycloak;" 2>/dev/null || echo "Database already exists"
  sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;" 2>/dev/null || echo "Privileges already granted"
  ss -lntp | grep ":5432" || echo "Postgres not listening on 5432"

  # CouchDB 3.x and CouchDB Nouveau (install from Apache repository)
  sudo apt install -y curl apt-transport-https gnupg
  curl https://couchdb.apache.org/repo/keys.asc | gpg --dearmor | sudo tee /usr/share/keyrings/couchdb-archive-keyring.gpg >/dev/null 2>&1
  echo "deb [signed-by=/usr/share/keyrings/couchdb-archive-keyring.gpg] https://apache.jfrog.io/artifactory/couchdb-deb/ jammy main" | sudo tee /etc/apt/sources.list.d/couchdb.list >/dev/null
  echo "deb http://archive.ubuntu.com/ubuntu/ jammy main universe" | sudo tee /etc/apt/sources.list.d/jammy.list
  sudo apt update
  sudo apt install -y couchdb couchdb-nouveau
  # During CouchDB installation, provide:
  # - Standalone mode
  # - Bind address: 0.0.0.0 (or 127.0.0.1 for local only)
  # - Admin username: admin
  # - Admin password: 12345 (or your chosen password)
  #
  # Note: couchdb-nouveau provides full-text search capabilities required by SW360
  # It will be automatically configured to work with CouchDB
  #
  # Verify CouchDB is running
  curl -s http://localhost:5984/ || echo "CouchDB not reachable on 5984"
  # Access Fauxton UI at: http://localhost:5984/_utils/
  ```

5. **Verify Tomcat and Keycloak Directory Ownership:**
  **IMPORTANT:** The entire Tomcat and Keycloak installation directories must be owned by the current user. This is essential for Maven to deploy WAR files and Keycloak providers without permission issues.

  If you installed Tomcat and Keycloak in step 4, ownership should already be set correctly. Verify with:

  ```bash
  # Verify Tomcat directory ownership
  TOMCAT_OWNER=$(stat -c '%U' "$TOMCAT_HOME")
  if [ "$TOMCAT_OWNER" = "$USER" ]; then
      echo "✓ Tomcat directory is owned by $USER"
  else
      echo "✗ ERROR: Tomcat directory is owned by '$TOMCAT_OWNER', not '$USER'"
      echo "  Please fix ownership during installation or re-install Tomcat with correct ownership"
  fi

  # Verify Keycloak directory ownership
  KEYCLOAK_OWNER=$(stat -c '%U' "$KEYCLOAK_HOME")
  if [ "$KEYCLOAK_OWNER" = "$USER" ]; then
      echo "✓ Keycloak directory is owned by $USER"
  else
      echo "✗ ERROR: Keycloak directory is owned by '$KEYCLOAK_OWNER', not '$USER'"
      echo "  Please fix ownership during installation or re-install Keycloak with correct ownership"
  fi

  # Show directory details
  ls -la "$TOMCAT_HOME" | head -5
  ls -la "$KEYCLOAK_HOME" | head -5
  ```

  **Note:** If ownership is incorrect, go back to step 4 and ensure the `chown` commands were executed during installation. For pre-existing installations, you may need to run `sudo chown -R $USER:$USER <directory>` once to fix ownership.

6. **Create and Configure SW360 Config Directory:**
  Create the SW360 configuration directory and copy configuration files (use the confirmed config path):
  ```bash
  # Create directory with proper permissions (use confirmed SW360_CONFIG_DIR)
  sudo mkdir -p "$SW360_CONFIG_DIR"
  sudo chmod 777 "$SW360_CONFIG_DIR" -R

  # Copy configuration files to the config directory
  # Files needed: sw360.properties, couchdb.properties, couchdb-test.properties, application.yml
  # Update these files with correct credentials:
  # - sw360.properties: main SW360 configuration (backend settings, thrift ports, etc.)
  # - couchdb.properties: couchdb.user=admin, couchdb.password=12345
  # - couchdb-test.properties: same credentials for testing
  # - application.yml in authorization folder: update database credentials
  ```

7. **Verify JWT Configuration in application.yml:**
  Before building, ensure that [`rest/resource-server/src/main/resources/application.yml`](/home/$(whoami)/workspace/sw360/rest/resource-server/src/main/resources/application.yml) is configured to use Keycloak as the JWT issuer.

  **Required Configuration:**
  The JWT settings should point to your Keycloak server:
  ```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8083/realms/sw360
          jwk-set-uri: http://localhost:8083/realms/sw360/protocol/openid-connect/certs
  ```

  **⚠️ IMPORTANT:** If your application.yml currently has:
  ```yaml
issuer-uri: http://localhost:8080/authorization/oauth2/jwks
jwk-set-uri: http://localhost:8080/authorization/oauth2/jwks
  ```

  **You MUST change it to use Keycloak endpoints** because:
  - The current configuration points to a custom authorization server on port 8080
  - For Keycloak 26 authentication, the JWT tokens must be validated against Keycloak's JWK endpoint
  - The issuer-uri must match the realm issuer in Keycloak (http://localhost:8083/realms/sw360)
  - Without this change, JWT token validation will fail and authentication won't work

8. **Select Java 21 and Build with Maven:**
  Ensure you are using JDK 21 via alternatives, then build and deploy to Tomcat and Keycloak providers using the paths confirmed in step 1.

  ```bash
# List installed Java alternatives
update-alternatives --list java
# Export Java 21 for this shell
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
echo "Using JAVA_HOME=$JAVA_HOME"

# Navigate to SW360 application directory (use confirmed path)
cd "$SW360_APP_PATH"

# Build and deploy using confirmed paths for Tomcat and Keycloak
$MAVEN_CMD clean install -DskipTests \
  -Dbase.deploy.dir="$TOMCAT_HOME/" \
  -Dlistener.deploy.dir="$KEYCLOAK_HOME/providers" \
  -P deploy
  ```

9. **Start Keycloak Server (If Using Keycloak Authentication):**
  **Only perform this step if your JWT configuration points to Keycloak** (issuer-uri contains `/realms/sw360`).

  If you're using the custom Spring Boot authorization server (issuer-uri: `http://localhost:8080/authorization/oauth2/jwks`), skip this step.

  Set environment variables and start Keycloak using the confirmed installation path:
  ```bash
# Set Keycloak admin credentials
export KEYCLOAK_ADMIN=admin
export KEYCLOAK_ADMIN_PASSWORD=admin

# Start Keycloak in development mode (runs in background) - use confirmed path
$KEYCLOAK_HOME/bin/kc.sh start-dev --http-port=8083 &

# Wait for Keycloak to start (typically takes 30-60 seconds)
echo "Waiting for Keycloak to start..."
sleep 10
for i in {1..12}; do
  if curl -s http://localhost:8083/ > /dev/null 2>&1; then
    echo "Keycloak is up and running!"
    break
  fi
  echo "Waiting for Keycloak... ($i/12)"
  sleep 5
done

# Verify Keycloak admin console is accessible
curl -s -o /dev/null -w "Keycloak admin console HTTP status: %{http_code}\n" http://localhost:8083/admin/
  ```

  **Alternative: Start in production mode** (requires prior build with `kc.sh build`):
  ```bash
$KEYCLOAK_HOME/bin/kc.sh start --http-port=8083 &
  ```

10. **Verify and Configure Keycloak Realm:**
  After Keycloak starts, verify the sw360 realm exists and is properly configured. If not, create it using the Keycloak Admin API.

  **a. Get Admin Access Token:**
  ```bash
# Get admin access token for API calls
ADMIN_TOKEN=$(curl -s -X POST "http://localhost:8083/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

echo "Admin token obtained: ${ADMIN_TOKEN:0:20}..."
  ```

  **b. Check if sw360 Realm Exists:**
  ```bash
# Check if sw360 realm exists
REALM_EXISTS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X GET "http://localhost:8083/admin/realms/sw360" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

if [ "$REALM_EXISTS" = "200" ]; then
    echo "✓ SW360 realm exists"
else
    echo "✗ SW360 realm does not exist. Creating..."

    # Create sw360 realm
    curl -X POST "http://localhost:8083/admin/realms" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "realm": "sw360",
        "enabled": true,
        "displayName": "SW360",
        "registrationAllowed": false,
        "loginWithEmailAllowed": true,
        "duplicateEmailsAllowed": false,
        "resetPasswordAllowed": false,
        "editUsernameAllowed": false,
        "bruteForceProtected": false,
        "accessTokenLifespan": 300,
        "ssoSessionIdleTimeout": 1800,
        "ssoSessionMaxLifespan": 36000,
        "accessCodeLifespan": 60
      }'

    echo "✓ SW360 realm created"
fi
  ```

  **c. Verify and Create Groups:**
  ```bash
# Define required groups
GROUPS=("ADMIN" "CLEARING_ADMIN" "CLEARING_EXPERT" "ECC_ADMIN" "SECURITY_ADMIN" "SW360_ADMIN" "USER")

for GROUP_NAME in "${GROUPS[@]}"; do
    # Check if group exists
    GROUP_CHECK=$(curl -s -X GET "http://localhost:8083/admin/realms/sw360/groups" \
      -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r --arg name "$GROUP_NAME" '.[] | select(.name == $name) | .id')

    if [ -n "$GROUP_CHECK" ]; then
        echo "✓ Group $GROUP_NAME exists"
    else
        echo "✗ Group $GROUP_NAME missing. Creating..."
        curl -X POST "http://localhost:8083/admin/realms/sw360/groups" \
          -H "Authorization: Bearer $ADMIN_TOKEN" \
          -H "Content-Type: application/json" \
          -d "{\"name\": \"$GROUP_NAME\"}"
        echo "✓ Group $GROUP_NAME created"
    fi
done
  ```

  **d. Verify and Create Client Scopes:**
  ```bash
# Define required client scopes
declare -A SCOPES=(
    ["READ"]="Read access to SW360"
    ["WRITE"]="Write access to SW360"
)

for SCOPE_NAME in "${!SCOPES[@]}"; do
    SCOPE_DESC="${SCOPES[$SCOPE_NAME]}"

    # Check if scope exists
    SCOPE_CHECK=$(curl -s -X GET "http://localhost:8083/admin/realms/sw360/client-scopes" \
      -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r --arg name "$SCOPE_NAME" '.[] | select(.name == $name) | .id')

    if [ -n "$SCOPE_CHECK" ]; then
        echo "✓ Client scope $SCOPE_NAME exists"
    else
        echo "✗ Client scope $SCOPE_NAME missing. Creating..."
        curl -X POST "http://localhost:8083/admin/realms/sw360/client-scopes" \
          -H "Authorization: Bearer $ADMIN_TOKEN" \
          -H "Content-Type: application/json" \
          -d "{
            \"name\": \"$SCOPE_NAME\",
            \"description\": \"$SCOPE_DESC\",
            \"protocol\": \"openid-connect\",
            \"attributes\": {
              \"include.in.token.scope\": \"true\",
              \"display.on.consent.screen\": \"true\"
            }
          }"
        echo "✓ Client scope $SCOPE_NAME created"
    fi
done
  ```

  **e. Verify and Create Clients:**
  ```bash
# Check and create sw360ui client (for frontend)
SW360UI_CLIENT=$(curl -s -X GET "http://localhost:8083/admin/realms/sw360/clients?clientId=sw360ui" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id')

if [ "$SW360UI_CLIENT" != "null" ] && [ -n "$SW360UI_CLIENT" ]; then
    echo "✓ sw360ui client exists"
else
    echo "✗ sw360ui client missing. Creating..."
    curl -X POST "http://localhost:8083/admin/realms/sw360/clients" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "clientId": "sw360ui",
        "name": "SW360 Frontend",
        "enabled": true,
        "publicClient": false,
        "standardFlowEnabled": true,
        "directAccessGrantsEnabled": true,
        "serviceAccountsEnabled": false,
        "redirectUris": [
          "http://localhost:3000/api/auth/callback/keycloak",
          "https://localhost:3000/api/auth/callback/keycloak"
        ],
        "webOrigins": [
          "http://localhost:3000",
          "https://localhost:3000"
        ],
        "protocol": "openid-connect"
      }'
    echo "✓ sw360ui client created"
fi

# Check and create trusted-sw360-client (for REST API)
TRUSTED_CLIENT=$(curl -s -X GET "http://localhost:8083/admin/realms/sw360/clients?clientId=trusted-sw360-client" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id')

if [ "$TRUSTED_CLIENT" != "null" ] && [ -n "$TRUSTED_CLIENT" ]; then
    echo "✓ trusted-sw360-client exists"
else
    echo "✗ trusted-sw360-client missing. Creating..."
    curl -X POST "http://localhost:8083/admin/realms/sw360/clients" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "clientId": "trusted-sw360-client",
        "name": "Trusted SW360 REST Client",
        "enabled": true,
        "publicClient": false,
        "standardFlowEnabled": true,
        "directAccessGrantsEnabled": true,
        "serviceAccountsEnabled": true,
        "secret": "sw360-secret",
        "redirectUris": ["*"],
        "webOrigins": ["*"],
        "protocol": "openid-connect"
      }'
    echo "✓ trusted-sw360-client created"
fi
  ```

  **f. Verify User Federation (Custom SW360 User Storage):**
  ```bash
# Check if sw360-user-storage-jpa provider exists
USER_STORAGE=$(curl -s -X GET "http://localhost:8083/admin/realms/sw360/components?parent=\$(curl -s http://localhost:8083/admin/realms/sw360 -H \"Authorization: Bearer \$ADMIN_TOKEN\" | jq -r '.id')&type=org.keycloak.storage.UserStorageProvider" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[] | select(.name == "sw360-user-storage-jpa") | .id')

if [ -n "$USER_STORAGE" ]; then
    echo "✓ SW360 user storage provider (sw360-user-storage-jpa) is configured"
else
    echo "⚠ SW360 user storage provider not found. This should be automatically configured when Keycloak providers are deployed."
    echo "  The provider will be available after building SW360 with Keycloak providers enabled."
fi
  ```

  **g. Create Default Admin User (if needed):**
  ```bash
# Check if admin user exists
ADMIN_USER=$(curl -s -X GET "http://localhost:8083/admin/realms/sw360/users?username=admin@sw360.org" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id')

if [ "$ADMIN_USER" != "null" ] && [ -n "$ADMIN_USER" ]; then
    echo "✓ Admin user exists"
else
    echo "✗ Admin user missing. Creating..."

    # Create admin user
    curl -X POST "http://localhost:8083/admin/realms/sw360/users" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "username": "admin@sw360.org",
        "email": "admin@sw360.org",
        "enabled": true,
        "emailVerified": true,
        "attributes": {
          "Department": ["IT"]
        },
        "credentials": [{
          "type": "password",
          "value": "12345",
          "temporary": false
        }]
      }'

    # Get the created user ID
    ADMIN_USER=$(curl -s -X GET "http://localhost:8083/admin/realms/sw360/users?username=admin@sw360.org" \
      -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id')

    # Add user to ADMIN and SW360_ADMIN groups
    ADMIN_GROUP_ID=$(curl -s -X GET "http://localhost:8083/admin/realms/sw360/groups" \
      -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[] | select(.name == "ADMIN") | .id')

    SW360_ADMIN_GROUP_ID=$(curl -s -X GET "http://localhost:8083/admin/realms/sw360/groups" \
      -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[] | select(.name == "SW360_ADMIN") | .id')

    curl -X PUT "http://localhost:8083/admin/realms/sw360/users/$ADMIN_USER/groups/$ADMIN_GROUP_ID" \
      -H "Authorization: Bearer $ADMIN_TOKEN"

    curl -X PUT "http://localhost:8083/admin/realms/sw360/users/$ADMIN_USER/groups/$SW360_ADMIN_GROUP_ID" \
      -H "Authorization: Bearer $ADMIN_TOKEN"

    echo "✓ Admin user created and added to ADMIN and SW360_ADMIN groups"
fi
  ```

  **h. Configure Custom User Attribute Mapper (Department):**
  SW360 uses a custom user attribute called "Department" to store department information. This needs to be mapped to JWT tokens.

  ```bash
# Add Department attribute mapper to profile scope
PROFILE_SCOPE_ID=$(curl -s -X GET "http://localhost:8083/admin/realms/sw360/client-scopes" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[] | select(.name == "profile") | .id')

# Check if Department mapper already exists
DEPT_MAPPER=$(curl -s -X GET "http://localhost:8083/admin/realms/sw360/client-scopes/$PROFILE_SCOPE_ID/protocol-mappers/models" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[] | select(.name == "Department") | .id')

if [ -n "$DEPT_MAPPER" ]; then
    echo "✓ Department attribute mapper exists in profile scope"
else
    echo "✗ Department attribute mapper missing. Creating..."

    curl -X POST "http://localhost:8083/admin/realms/sw360/client-scopes/$PROFILE_SCOPE_ID/protocol-mappers/models" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "name": "Department",
        "protocol": "openid-connect",
        "protocolMapper": "oidc-usermodel-attribute-mapper",
        "config": {
          "user.attribute": "Department",
          "claim.name": "Department",
          "jsonType.label": "String",
          "id.token.claim": "true",
          "access.token.claim": "true",
          "userinfo.token.claim": "true",
          "multivalued": "false",
          "aggregate.attrs": "false"
        }
      }'

    echo "✓ Department attribute mapper created"
fi

# Verify: When creating users, you can add Department attribute like this:
# "attributes": {
#   "Department": ["IT"]
# }
  ```

  **Summary:**
  After running these verification steps, your Keycloak sw360 realm should have:
  - ✓ SW360 realm enabled
  - ✓ 7 user groups: ADMIN, CLEARING_ADMIN, CLEARING_EXPERT, ECC_ADMIN, SECURITY_ADMIN, SW360_ADMIN, USER
  - ✓ 2 custom client scopes: READ, WRITE
  - ✓ 2 clients: sw360ui (frontend), trusted-sw360-client (REST API)
  - ✓ User federation provider: sw360-user-storage-jpa (after building with providers)
  - ✓ Default admin user: admin@sw360.org
  - ✓ Custom user attribute: Department (mapped to JWT tokens)

  You can verify the configuration by accessing: `http://localhost:8083/admin/` (username: admin, password: admin)

11. **Start Apache Tomcat with JPDA:**
  Use the following commands to start and stop Apache Tomcat with JPDA enabled (using the confirmed Tomcat path):
  ```bash
# To start Tomcat with JPDA (use confirmed Tomcat path)
export JPDA_ADDRESS=8000
export JPDA_TRANSPORT=dt_socket
$TOMCAT_HOME/bin/catalina.sh jpda start

# Wait for Tomcat to deploy all WAR files (this takes 2-3 minutes)
echo "Waiting for Tomcat to start and deploy applications..."
sleep 10

# Check if Tomcat is responding
for i in {1..30}; do
  if curl -s http://localhost:8080/ > /dev/null 2>&1; then
    echo "Tomcat is up!"
    break
  fi
  echo "Waiting for Tomcat... ($i/30)"
  sleep 10
done

# Verify health endpoint is accessible
echo "Checking SW360 health endpoint..."
sleep 5
curl -s -o /dev/null -w "Health endpoint HTTP status: %{http_code}\n" http://localhost:8080/resource/api/health

# To stop Tomcat (use confirmed Tomcat path)
$TOMCAT_HOME/bin/catalina.sh stop
  ```

  **Note:** Tomcat deployment typically takes 2-3 minutes as it needs to extract and initialize all WAR files (20+ applications). The health check loop above ensures services are fully ready before testing.

---

## Verification and Testing

After deployment, verify the services are running:

**1. Check Service Status:**
```bash
# PostgreSQL 14
sudo -u postgres pg_lsclusters

# CouchDB
curl http://localhost:5984/

# Keycloak
curl http://localhost:8083/

# Tomcat (should return HTTP 200)
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost:8080/

# Check all listening ports
ss -tlnp | grep -E "(8083|8080|5984|5432)" | grep LISTEN
```

**2. Test SW360 Health Endpoint:**

The health endpoint requires JWT authentication from Keycloak. To test:

**Option A: Using Keycloak OAuth2 Token (Recommended)**
```bash
# Get access token from Keycloak
TOKEN=$(curl -s -X POST "http://localhost:8083/realms/sw360/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=trusted-sw360-client" \
  -d "client_secret=sw360-secret" \
  -d "username=setup@sw360.org" \
  -d "password=12345" \
  -d "grant_type=password" | jq -r '.access_token')

# Test health endpoint with token
curl -X GET "http://localhost:8080/resource/api/health" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

**Option B: Configure for Basic Authentication (Alternative)**
If you prefer Basic auth instead of Keycloak JWT, update [`rest/resource-server/src/main/resources/application.yml`](/home/$(whoami)/workspace/sw360/rest/resource-server/src/main/resources/application.yml):
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/authorization/oauth2/jwks
          jwk-set-uri: http://localhost:8080/authorization/oauth2/jwks
```
Then rebuild and deploy the resource-server module.

**Expected Health Response:**
```json
{
  "status": "UP",
  "components": {
    "diskSpace": { ... },
    "ping": { ... }
  }
}
```

---

## Summary

This deployment guide covers:
- **Prerequisites**: Git, Java 21, Maven 3.9.x, Tomcat 11, Keycloak 26, PostgreSQL 14, CouchDB 3.x
- **Configuration**: `/etc/sw360` with CouchDB credentials, JWT setup for Keycloak authentication
- **Build**: Maven build with deployment to Tomcat webapps and Keycloak providers
- **Services**: PostgreSQL (5432), CouchDB (5984), Keycloak (8083), Tomcat (8080 with JPDA 8000)
- **Authentication**: Keycloak OAuth2/JWT for resource server APIs

**Important Notes:**
- Ensure all services are running before accessing SW360 REST APIs
- CouchDB requires authentication (default: admin/12345)
- PostgreSQL keycloak database password must match `/opt/keycloak/conf/keycloak.conf` (default: password)
- JWT authentication is configured to use Keycloak realm `sw360` on port 8083
- For testing APIs, obtain a JWT token from Keycloak using client credentials or password grant
