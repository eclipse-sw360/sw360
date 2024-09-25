# STEPS TO INTEGRATE KEYCLOAK WITH SW360 NEW UI

## Install Java 17

* Update the package index: `sudo apt update`
* Install OpenJDK 17: `sudo apt install openjdk-17-jdk`

## Set JAVA_HOME

* Edit the ~/.bashrc file: `vim ~/.bashrc`
* Add the following line at the end of the file: `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`
* Save and exit the editor.
* Update the environment variables: `source ~/.bashrc`
* Or you can set environment variable in /etc/environment file

## Install postgresql:

* Install PostgreSQL 14 or higher.
```
sudo apt update && sudo apt upgrade -y
sudo apt install postgresql-14
```

## Set Username and Password in PostgreSQL:

* Switch to the PostgreSQL user: sudo su postgres
* Access the PostgreSQL console: psql
* Execute the following SQL commands:
```
CREATE USER keycloak WITH ENCRYPTED PASSWORD 'password';
CREATE DATABASE keycloak;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;
```

## Install Keycloak:

* Download Keycloak 24.0.2 from the official repository.
* Or download the tar file `wget https://github.com/keycloak/keycloak/releases/download/24.0.2/keycloak-24.0.2.tar.gz`
* Extract the downloaded file to the /opt folder, `sudo tar -xvf myfiles.tar -C /opt`
* Goto keycloak conf folder and uncomment the following from keycloak.conf file:
```
# Database

# The database vendor.
db=postgres

# The username of the database user.
db-username=keycloak

# The password of the database user.
db-password=password

# The full database JDBC URL. If not provided, a default URL is set based on the selected database vendor.
db-url=jdbc:postgresql://localhost/keycloak
```
* Set environment variables for Keycloak administration:
```
export KEYCLOAK_ADMIN=admin
export KEYCLOAK_ADMIN_PASSWORD=admin
```

## Start Keycloak:

* Navigate to the Keycloak directory, `cd /path/to/keycloak/bin`
* Run the start command with the necessary parameters:
```
./kc.sh start --log="console,file" --hostname-strict-backchannel=false --http-enabled=true --http-port=8083 --https-port=8533 --hostname-strict=false --hostname-strict-https=false
```
* Run the start command with the necessary parameters(with debug mode):
```
sudo ./kc.sh start  --log="console,file" --hostname-strict-backchannel=false --http-enabled=true --http-port=8083 --https-port=8533 --hostname-strict=false --debug  --log-level=INFO,org.eclipse.sw360.keycloak.spi:debug,org.eclipse.sw360.keycloak.event.listener:debug
```

## Build the Backend:

* Build the SW360 backend code using Maven, `mvn clean install -DskipTests`
* Copy the generated WAR files to the webapps folder of Apache Tomcat, `cp $(find . -name "*.war")  /opt/apache-tomcat-10.x.x/webapps`
* Start the Apache Tomcat server.

## Keycloak Providers and Libraries:
 Providers are used to read users from sw360 db and register users from keycloak to sw360 db
 
* After building the backend add the below files to providers folder in /opt/keycloak-24.0.2/providers/:
```
sudo cp sw360/keycloak/user-storage-provider/target/sw360-keycloak-user-storage-provider.jar /opt/keycloak-24.0.2/providers/
sudo cp sw360/keycloak/event-listner/target/sw360-keycloak-event-listener.jar /opt/keycloak-24.0.2/providers
sudo cp .m2/repository/org/eclipse/sw360/datahandler/18.99.1-SNAPSHOT/datahandler-18.99.1-SNAPSHOT.jar /opt/keycloak-24.0.2/providers/
sudo cp .m2/repository/org/eclipse/sw360/commonIO/18.99.1/commonIO-18.99.1.jar /opt/keycloak-24.0.2/providers/
add libthrift-0.19.0.jar file to providers folder
sudo wget https://repo1.maven.org/maven2/org/apache/httpcomponents/core5/httpcore5/5.2.4/httpcore5-5.2.4.jar
```

## Keycloak Admin Console:

* Login to Keycloak admin console. ![loginPage](https://github.com/siemens/sw360/assets/58290634/178af032-2e21-48b3-a621-65cdde2dfdce)

  ```
  username: admin
  password: admin
  ```

* Create Realm and name it sw360. ![createRealm](https://github.com/siemens/sw360/assets/58290634/027539e3-5152-484f-ba8c-b625c81e59c0)

* Create Client in Keycloak. ![clientCreation](https://github.com/siemens/sw360/assets/58290634/c3d6e93c-554a-4050-b3ce-4bc6b9a3f346)

  * Follow the below steps for client creation:
    * Under *General settings*, enter Client ID which will be used in .env file(SW360 Frontend Repo) as well as in rest. ![Step1](https://github.com/siemens/sw360/assets/58290634/58ecefc7-d23e-4f3f-87f0-9fb16bae3e11)

	* In *Capability config* enable Client authentication. ![Step2](https://github.com/siemens/sw360/assets/58290634/9e597766-4af9-4364-b2d5-3d15a11e53c4)

    * Goto *Login settings* and enter below fields: ![Step3](https://github.com/siemens/sw360/assets/58290634/fad873fe-2bff-4ff5-9b2d-fa7e817eb4db)

	```
	Home URL: http://localhost:3000
	Valid redirect URIs: http://localhost:3000/api/auth/callback/keycloak, https://oauth.pstmn.io/v1/callback
	Valid post logout redirect URIs: +
	Web origins: *
	```

* Create Client Scopes. 
  * Create READ scope by clicking on Create client scope button. ![createScope2](https://github.com/siemens/sw360/assets/58290634/60769c25-cc10-4299-9a67-ce9a5f08ac28)

  * Similarly create WRITE scope.

* Add Scopes to our Client. 
  * Goto Clients, then select your newly created client in *Client lists* page.
  * Goto *Client scopes* page, click on Add client scope and there you will see your READ and WRITE scopes that you need to add.
  * Select both scopes and then click on Add(default). ![AddScopeToClient](https://github.com/siemens/sw360/assets/58290634/60e69e0d-0ef4-4dcf-9afd-2dd81b9a4dac)

* Create Groups.
  * Goto Groups and create different groups that we are going to use in sw360. ![createGroups](https://github.com/siemens/sw360/assets/58290634/fc5596e4-e901-4c59-9304-a2dca4949530)

  * Create 7 groups: ADMIN, CLEARING_ADMIN, CLEARING_EXPERT, ECC_ADMIN, SECURITY_ADMIN, SW360_ADMIN, USER. ![Create7Groups](https://github.com/siemens/sw360/assets/58290634/ca1d307c-4f3c-4d23-b809-003765a7f0f8)

* Create an Attribute.
  * Goto Realm settings then click on *User profile* page where we can create a new attribute. ![realmSettings](https://github.com/siemens/sw360/assets/58290634/6dba6596-1f19-4078-b223-01ada12e585f)

  * Create a new attribute by the name Department and give the required permissions as shown in screenshot. ![CreateDepartmentAttribute](https://github.com/siemens/sw360/assets/58290634/c2772795-4432-42ce-b589-949c937a8ae8)

* Add Event Listner.
  * Goto *Events* page in Realm settings.
  * Click on event listners dropdown and select *sw360-add-user-to-couchdb*. ![AddEventListner](https://github.com/siemens/sw360/assets/58290634/c799e25c-9651-4079-a4fd-c7e16544502f)

* Access to external Databases.
  * Goto User federation and select *sw360-user-storage-jpa providers*. ![UserFederation](https://github.com/siemens/sw360/assets/58290634/6f88d9e7-7c36-43e5-b186-58607e7f4dd4)

  * Give proper name and create the custom provider. ![AddUserProvider](https://github.com/siemens/sw360/assets/58290634/ab815f04-d535-476b-8f35-b9d6e9e9240d)

* Check Authentication Settings
  * Goto Authentication and apply the permissions in *Required actions* as shown in screenshot. ![authenticationSettings](https://github.com/siemens/sw360/assets/58290634/3c09616f-9aa6-451d-9a5b-55e5294b82ae)

* Create Users
  * To create a new user one can goto Users section. ![CreateUser](https://github.com/siemens/sw360/assets/58290634/76efe669-5881-4459-bff2-cedaebbc4e99)

  * Also check whether user is created in couchdb or not.
  * Set password for the newly created user by selecting the user and going to the *Credentials* page. ![passwordUser](https://github.com/siemens/sw360/assets/58290634/dacd0324-2b85-4d33-8443-ed1167055cf8)

## Clone SW360 Frontend Repository

* Run the git clone command, `git clone git@github.com:eclipse-sw360/sw360-frontend.git`
* Create .env file inside the repository and add the following data: 
```
NEXTAUTH_SECRET = 'secret'
NEXT_PUBLIC_SW360_API_URL = 'http://localhost:8080'
NEXTAUTH_URL='http://localhost:3000'
NEXT_PUBLIC_SW360_REST_CLIENT_ID='trusted-sw360-client'
NEXT_PUBLIC_SW360_REST_CLIENT_SECRET='sw360-secret'
NEXT_PUBLIC_ENABLE_SW360_OAUTH_PROVIDER='true'
#possible values are sw360basic, sw360oauth, keycloak
#NEXT_PUBLIC_SW360_AUTH_PROVIDER='keycloak'
SW360_KEYCLOAK_CLIENT_ID=
SW360_KEYCLOAK_CLIENT_SECRET=
AUTH_ISSUER=http://localhost:8083/realms/sw360

```
* Get SW360_KEYCLOAK_CLIENT_ID and SW360_KEYCLOAK_CLIENT_SECRET from Keycloak console
  * SW360_KEYCLOAK_CLIENT_ID will be present in your client's *Settings* page.
  * SW360_KEYCLOAK_CLIENT_SECRET will be present in your clients's *Credentials* page

## Install NVM

* Installs NVM (Node Version Manager)
`curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.4/install.sh | bash`
* Download and Install Node.js
`nvm install 20.5.1`
* Verifies the right Node.js version is in the environment
`node -v` # should print `v20.5.1`
* Verifies the right NPM version is in the environment
`npm -v` # should print `10.2.4`
* Installs next 
`npm install next@latest react@latest react-dom@latest`

## Build the Frontend
```
npm run build
npm run start
/usr/bin/google-chrome-stable --disable-web-security --user-data-dir="/home/${USER}/cors" --allow-file-access-from-files
```
## Token Creation for REST

* Type of authorization will be OAuth 2.0.
* Enter the below details while creating a new Bearer token: ![restCheck](https://github.com/siemens/sw360/assets/58290634/4c57305d-4dce-4b4f-99a3-12c283d4723d)

```
Clallback URL: https://oauth.pstmn.io/v1/callback
Auth URL: http://localhost:8083/realms/sw360/protocol/openid-connect/auth
Access Token URL: http://localhost:8083/realms/sw360/protocol/openid-connect/token
Get Client Id and Client Secret from Keycloak client
Scope: openid READ WRITE
```
