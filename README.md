# Overview
Downloads a Swagger API specification and generates client API code and response schemas from it.
The project is compiled into a jar file that is stored in AzureDevops build artifact feed.

# Configuration
## pom.xml
Configure the following settings in the `pom.xml` file:

```xml
<project>
    <groupId>ORGANIZATION-NAME</groupId> <!-- AzureDevops organization name -->
    <artifactId>ARTIFACT-NAME</artifactId> <!-- AzureDevops artifact name -->
    <version>ARTIFACT-VERSION</version> <!-- AzureDevops artifact version -->

    <properties>
        <swagger.file.name>SWAGGER-FILE-NAME.json</swagger.file.name> <!-- Swagger file name -->
        <swagger.url>SWAGGER-URL.json</swagger.url> <!-- Swagger json -->
        <artifact.feed.name>ARTIFACT-FEED-NAME</artifact.feed.name> <!-- AzureDevops build artifact feed name -->
        <artifact.feed.url> <!-- AzureDevops build artifact feed url -->
            https://pkgs.dev.azure.com/ORGANIZATION-NAME/PROJECT-NAME/_packaging/ARTIFACT-FEED-NAME/maven/v1
        </artifact.feed.url>
        <project.domain.name>PROJECT-DOMAIN-NAME</project.domain.name> <!-- Directory path -->
        <maven.compiler.source>use-same-version-in-pipeline</maven.compiler.source> <!-- Java version -->
        <maven.compiler.target>use-same-version-in-pipeline</maven.compiler.target> <!-- Java version -->
        <!-- ... -->
    </properties>
    <!-- ... -->
</project>
```

## YAML File
In the YAML file, ensure that the Java versions are in sync with the Java versions in the pom.xml:

```yaml
variables:
  mavenJavaVersion: '1.17'
```

## Azure DevOps Pipeline Authorization
This config should be done only once. Either for this project or for the API client code and schema -generation project.
1. **Create a Personal Access Token (PAT) with read and write access to the Azure DevOps artifact feed:**
   1. Go to your Azure DevOps organization and click on your profile picture in the top right corner.
   2. Click on "Security".
   3. Under "Personal access tokens", click "New Token".
   4. Give your token a name, an expiration date, and the necessary permissions.
   5. Copy the generated token and keep it secure, as you won't be able to see it again.

2. **Create a `settings.xml` file with your PAT as follows:**
- Make sure to save this file locally to simplify configuration of the Test Automation project

```xml

<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
    <servers>
        <server>
            <id>ARTIFACT-FEED-NAME</id>
            <username>ORGANIZATION-NAME</username>
            <password>PERSONAL-ACCESS-TOKEN</password>
        </server>
    </servers>
</settings>
```

3. **Upload this `settings.xml` file as a secure file in your Azure DevOps pipeline.**

# Execution

1. Create a pipeline in Azure DevOps from the project YAML file.
2. Run the pipeline.
3. After the pipeline runs successfully, a new artifact containing the response schemas will be available in the Azure
   DevOps artifact feed.
