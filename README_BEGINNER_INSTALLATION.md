# How to install and run SW360
# These instructions worked on Ubuntu 20.04 and has detailed explanations for newcomers.

### This is a guide with detailed explanation of how to install and run SW360 natively on you local machine, it includes installation of all dependencies manually, which will not use docker or other container system during the installation or run.

SW360 is an Open Source project. The [SW360] repository and [SW360 website] repositories are published on GitHub.

## In this file you will find how to:
- Install SW360 and its dependencies
- Run SW360 and its dependencies
- Check all services are working
- Be aware of cautions and notes

## What does SW360 use to construct the UI.
- [Java] - Java is a class-based, object-oriented programming language.
- [Maven] - Maven is a build automation tool for Java projects. 
- [Liferay bundled with Tomcat] - Liferay is a Java-based web application platform for the development of customizable portals and websites.
                                  And Apache Tomcat provides a "pure Java" HTTP web server environment in which Java code can run. 
- [PostgreSQL] - PostgreSQL or Postgres, is a relational database management system.
- [Couchdb] - Apache CouchDB is a document-oriented NoSQL database, it uses JSON to store data, and provides HTTP for an API.
- [CVE-Search] - CVE-Search is a tool to perform local searches for known vulnerabilities (CVE - Common Vulnerabilities and Exposures).


## Install SW360 and its dependencies

### 1. Clone the SW360 Github repository and checkout to stable version.

```sh
$ git clone https://github.com/eclipse/sw360.git
$ cd sw360/
$ git checkout sw360-16.0.0-M1
```
> Check if you have correct repository version

```sh
$ git branch
```

### 2. Install Java, Maven

> Install java and maven:

```sh
$ sudo apt install openjdk-11-jre-headless
```
> You may use this "$ sudo apt install default-jdk" command instead.

> Check if java is installed:

```sh
$ java --version   [check]
```
> Install maven:

```sh
$ sudo apt update
$ sudo apt install maven
```
> Check if Maven is installed:

```sh
$ mvn --version   
```

### 3. Install Liferay portal and its dependencies

```sh
$ ./scripts/docker-config/download_dependencies.sh
$ ls -la ./deps     [check if all dependencies have proper sizes]
$ ./scripts/install-thrift.sh
$ thrift --version   [check]
```
> After this step, check whether the "./deps/jars/libthriftxxx.jar" has version at the end of its name instead of xxx, and has size of 345Kb. If no, download the correct jar from this link:

```sh
$ wget https://repo1.maven.org/maven2/org/apache/thrift/libthrift/0.16.0/libthrift-0.16.0.jar
$ mv libthrift-0.16.0.jar ./deps/jars

```
> Once the correct Thrift library is found, install Liferay and copy dependency ".jar" files under "liferay_xxx/osgi/modules" folder:

```sh
$ tar -xzvf liferay-ce-portal-tomcat-7.3.4-ga5-20200811154319029.tar.gz
$ cp ./deps/jars/* deps/liferay-ce-portal-7.3.4-ga5/osgi/modules/
```
> Now set all environment variables of SW360 path to your local ".bashrc":
> You may use other text editor instead of vim.
```sh
$ vim ~/.bashrc
```
> Scroll till the end of the .bashrc file and add following lines, make sure to put correct absolute paths of your local machine in the place of {absolute path to sw360 repository folder}.

```sh
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
export PATH=$PATH:$JAVA_HOME/bin
export LIFERAY_INSTALL="/{absolute path to sw360 repository folder}/sw360/deps/liferay-ce-portal-7.3.4-ga5"
export SW360_DIR_INSTALL="{absolute path to sw360 repository folder}/sw360"
```
> Save the .bashrc file and run it:

```sh
$ source ~/.bashrc
```

### 4. Make and build SW360

> Go to sw360 repository folder and type: 
> "sudo" might not be necessary, and this will take time, around 5 min]

```sh
$ mvn clean
$ sudo mvn install -DskipTests    
```
> If the installation was successful, then need to deploy the project to be able to run. 
> Check which tomcat version do you have and put that in the place of {existing version 9.0.33}, normally it should be just "tomcat-9.0.33".

```sh
sudo mvn package -P deploy -Dbase.deploy.dir=. -Dliferay.deploy.dir=${LIFERAY_INSTALL}/deploy -Dbackend.deploy.dir=${LIFERAY_INSTALL}/tomcat-{existing version 9.0.33}/webapps -Drest.deploy.dir=${LIFERAY_INSTALL}/tomcat-{existing version 9.0.33}/webapps -DskipTests      
```
> This will create /deploy under root, so sudo is necessary, however you can chmod /deploy.
> This will take time, around 5 - 10 min.

> After this step, you should be able to run Tomcat server and see the index page of SW360 portal. [Check SW360]

### 5. Install PostgreSQL

> Install PostgerSQL manually, you can install through "apt install" too:

```sh
$ sudo apt install zlib1g-dev -y
$ sudo apt install libreadline-dev -y
$ wget https://download.postgresql.org/pub/source/v10.14/postgresql-10.14.tar.gz
$ tar -xvf postgresql-10.14.tar.gz 
$ cd postgresql-10.14/
$ mkdir -p  /PATH_TO/sw360postgres
$ ./configure -prefix=/PATH_TO/sw360postgres
$ make
$ sudo make install
```
> Set the paths for Postgres in the .bashrc otherwise you have to export them each time. Use same procedure as before in 3rd step.

```sh
$ vim ~/.bashrc
```
> Got to the end of the .bashrc file and add following lines, make sure to add correct paths of previously configured sw360postgres. Here $HOME is the absolute path of your user, such as "/home/username":

```sh
$ export PATH=$HOME/sw360postgres/bin:$PATH
$ export PGDATA=$HOME/sw360postgres/data
$ export LD_LIBRARY_PATH=$HOME/sw360postgres/lib
$ export PGPORT=5432
```
> Check if paths have been set, result must be the absolute paths:

```sh
$ echo $PATH
$ echo $PGDATA
$ echo $LD_LIBRARY_PATH
$ echo $PGPORT
```
> After paths are set, postgres service can be run:

```sh
$  cd /PATH_TO/sw360postgres/bin
$ ./initdb --encoding=UTF8 --no-locale
$ ./pg_ctl start
```
> You will see that the server has started.

> Note: If you installed through "apt install" then start the postgres service by following command, where after @ comes the installed version, if postgres isn't running you won't be able to connect to the server, and the error message is not explaining well that server isn't actually running at the moment:

```sh
sudo systemctl status postgresql@12-main.service
sudo systemctl start postgresql@12-main.service
```

Normally, Default postgres creates user "postgres" with "postgres" password, use that to enter PostgreSQL terminal:
```sh
$ sudo -i -u postgres
$ psql
 ```
> You will be logged in as user named "postgres".
```sh
$ psql postgres
postgres=# \du
postgres=# create database lportal;
postgres=# ALTER USER postgres WITH PASSWORD 'sw360fossy';
postgres=# ALTER ROLE postgres with superuser;
postgres=# \q
```

> Connect to postgres shell, and check users information

```sh
$ psql -d lportal
# \du
# \dt
# \l
```
### 6. Install Couch DB

> To install from aptitute  type:

```sh
$ sudo apt update
$ sudo apt install -y couchdb
```

> After, run CouchDb service, check if it's working:

```sh
$ sudo systemctl start couchdb.service
```
> Check if CouchDB is responding:

```sh
$ curl localhost:5984
```
> This should return json containing version information
> You can use "start/stop/status/restart" command with systemctl for controlling CouchDB service.

### 7. Install CVE-Search

> Follow these detailed instructions:

```sh
[https://github.com/cve-search/cve-search/blob/master/docs/source/getting_started/installation.rst]
```

> To connect it to SW360, see following instructions:

```sh
https://www.eclipse.org/sw360/docs/deployment/deploy-cve-search/
```

##### Notes:
- In the instruction be careful with setting apt link for mongodb, if somehow it destroys your "sudo apt update" command, go to "/etc/apt/sources.list" file and comment out the broken line, that's probably the one you lately added at the end of the file. This happens because some PPA are outdated but remain in the instructions.

### 8. Configure SW360

> Before going to configuration page, need to start the Liferay Tomcat server:

```sh
$ {path to sw360 installation}/./deps_backup/liferay-ce-portal-7.3.4-ga5/tomcat-9.0.33/bin/startup.sh
```
> You can use ...bin/shutdown.sh script to stop the server.
> If startup.sh script responded "Tomcat started. Then you are close to see SW360 portal page:
> To do so, open this url from your browser:

```sh
http://127.0.0.1:8080
```
> This will take time, around 5 min.

> If you can see liferay page, then go to the following links to configure SW360 portal.

- https://qiita.com/K-Hama/items/1582b4e1bf248025eabb#liferaygui%E8%A8%AD%E5%AE%9A - instructions in Japanese. 
- https://www.eclipse.org/sw360/docs/deployment/legacy/deploy-liferay7.3 - instrutions in English

##### Notes:
- Probably your postgres user and password are different, then replace the configurations "deps/liferay-ce-portal-7.3.4-ga5/portal-setup-wizard.properties" file or add the new user into postgres with required credentials.
- After creating user, if you can't sign in to SW360 portal https://www.eclipse.org/sw360/img/sw360screenshots/deploy73/2020-08-13_20.09.26.png try to login with "test" password and the same email  "setup@sw360.org" as you set in during configuration.

## Run SW360 and its dependencies

### 1. Run dependencies

> Turn on the CouchDB and Postgres services

```sh
$ sudo systemctl start couchdb.service
$ sudo systemctl start postgres@@12-main.service
```
> Check if both are running:

```sh
$ sudo systemctl status couchdb.service
$ sudo systemctl status postgres@@12-main.service
```
> You should be able to see something like this:

```sh
... systemd[1]: Started PostgreSQL Cluster 12-main.
...
... halt systemd[1]: Started Apache CouchDB.
```
> Run Liferay portal

```sh
$ ./deps/liferay-ce-portal-7.3.4-ga5/tomcat-9.0.33/bin/startup.sh
```
> Make sure to type correct path to the startup.sh file.

### 3. Run SW360

> Open the localhost:8080 page from the browser
If all the previous steps were successfuly done you will be able to see this page:
https://www.eclipse.org/sw360/img/sw360screenshots/deploy73/2020-08-13_20.24.21.png


Now enjoy SW360 portal!


## Check all services are working

To fully use SW360 you need to have following services running, please check one by one by opening your browser and typing url, or using curl from command line:
| Service | URL/Port | Notes
| ------ | ------ | ------
| Tomcat | http://127.0.0.1:8080 | When Tomcat is installed without liferay it uses same 8080 port |
| Liferay | http://127.0.0.1:8080 | If Liferay version is correct you will see Liferay white-blue index page not Tomcat yellow-green page.
| PostgreSQL | http://127.0.0.1:5432 | 
| CouchDB | http://127.0.0.1:5984/_utils |
| CVE-Search | http://127.0.0.1:5000/admin |


## Be aware of cautions and notes

> There are various versions of Tomcat with or without Liferay, however here we use Liferay which has already bundled Tomcat inside it's installation archive, that means you don't have to install Tomcat separately. In this case, when script liferay- xxx / tomcat- yyy/start.sh is run, the 8080 page will be visible, and will be overwritten by Liferay.

> If the service has problem with Liferay then you will not see Liferay blue-white page. If you see other than that then you need to go through 3rd step of Liferay installation, check it's version and reinstall it.

> If you still face the problem with Thrift or Liferay page isn't responding properly, type this command in the shell, to set the missing Thrift version environment variable, and run the ./scripts/install-thrift.sh again, then start from 3rd step of installation again:

```sh
THRIFT_VERSION=${THRIFT_VERSION:-0.16.0}
```

## References for more information
- [SW360]
- [CVE-Search]
- [Java]
- [Maven]
- [Thrift]
- [Liferay bundled with Tomcat]
- [PostgreSQL]
- [CouchDB]

## License
[SPDX-License-Identifier: EPL-2.0]

[//]: # (These are reference links used in the body of this instructions markdown file.)
   [Check SW360]: <http://localhost:8080>
   [Check CouchDB]: <http://localhost:5984>
   [Check PostgreSQL]: <http://localhost:5432>
   [SW360]: <https://www.eclipse.org/sw360/docs/>
   [SW360 website]: <https://github.com/eclipse/sw360.website>
   [CVE-Search]: <https://github.com/cve-search/cve-search>
   [Java]: <https://docs.oracle.com/en/java/javase/11/install/installation-jdk-linux-platforms.html#GUID-79FBE4A9-4254-461E-8EA7-A02D7979A161>
   [Maven]: <https://maven.apache.org/install.html>
   [Thrift]: <https://thrift.apache.org/>
   [Liferay bundled with Tomcat]: <https://learn.liferay.com/dxp/latest/en/installation-and-upgrades/installing-liferay/installing-a-liferay-tomcat-bundle.html>
   [PostgreSQL]: <https://www.postgresql.org/download/linux/ubuntu/>
   [CouchDB]: <https://docs.couchdb.org/en/stable/install/unix.html>
