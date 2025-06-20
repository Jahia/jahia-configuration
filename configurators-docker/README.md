# Jahia Docker Configurator

A standalone application used in Docker images to configure Jahia instances. This is a streamlined version of the original configurator, optimized for Docker environments with simplified functionality.

## Features and Limitations

The Docker configurator:
- Supports Tomcat application server only
- Supports all database types compatible with Jahia
- Does not embed database drivers (they must be provided in the Tomcat `lib` directory)

## Usage

The configurator can be executed as a Java application with the following command:

```shell
java -cp "target/configurators-docker-6.13.0-SNAPSHOT-standalone.jar:/path/to/tomcat/lib/*" \
org.jahia.configuration.ConfigureMain \
--configure configure.properties
```

Replace `/path/to/tomcat/lib/*` with the path to your Tomcat library directory containing the required database drivers.

## Configuration

Configuration is managed through a properties file passed as an argument to the `ConfigureMain` class. The file should contain all necessary settings for your Jahia instance.

### Example Configuration

```properties
targetServerDirectory=/usr/local/tomcat
jahiaVarDiskPath=/var/jahia
externalizedConfigTargetPath=/etc/jahia
licenseFile=/var/jahia/license.xml
databaseType=mariadb
databaseUrl=jdbc:mariadb://localhost:3306/jahia?useUnicode=true&characterEncoding=UTF-8&useServerPrepStmts=false&useSSL=false
databaseUsername=system
databasePassword=jahia
storeFilesInAWS=false
storeFilesInDB=true
fileDataStorePath=
jahiaRootPassword=xxxxx
processingServer=true
operatingMode=development
overwritedb=true
jahiaProperties={"mvnPath":"/opt/apache-maven-3.9.9/bin/mvn","svnPath":"/usr/bin/svn","gitPath":"/usr/bin/git","karaf.remoteShell.host":"0.0.0.0"}
cluster_node_serverId=my-jahia-0
cluster_activated=false
```

### Configuration Properties

| Property | Description                                                      |
|----------|------------------------------------------------------------------|
| `targetServerDirectory` | Path to the Tomcat server directory                              |
| `jahiaVarDiskPath` | Path to Jahia's var directory for runtime data                   |
| `externalizedConfigTargetPath` | Path to externalized configuration directory                     |
| `databaseType` | Type of database (mysql, mariadb, postgresql, etc.)              |
| `databaseUrl` | JDBC connection URL for the database                             |
| `databaseUsername` | Database username                                                |
| `databasePassword` | Database password                                                |
| `storeFilesInAWS` | Whether to store files in AWS (true/false)                       |
| `storeFilesInDB` | Whether to store files in the database (true/false)              |
| `fileDataStorePath` | Path for file storage if not using DB or AWS                     |
| `jahiaRootPassword` | Password for the Jahia root user                                 |
| `processingServer` | Whether this instance is a processing server (true/false)        |
| `operatingMode` | Operating mode (development, production)                         |
| `overwritedb` | Whether to overwrite existing database (true/false/if-necessary) |
| `jahiaProperties` | JSON string of additional Jahia properties                       |
| `cluster_node_serverId` | Server ID for cluster configuration                              |
| `cluster_activated` | Whether clustering is activated (true/false)                     |
| `licenseFile` | Path to Jahia license file                                       |
